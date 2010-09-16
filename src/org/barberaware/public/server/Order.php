<?php

/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
 *
 *  This is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

require_once ( "utils.php" );

class Order extends FromServer {
	public function __construct () {
		parent::__construct ( "Order", "Orders" );

		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "products", "ARRAY::Product" );
		$this->addAttribute ( "startdate", "DATE" );
		$this->addAttribute ( "enddate", "DATE" );
		$this->addAttribute ( "status", "INTEGER" );
		$this->addAttribute ( "shippingdate", "DATE" );
		$this->addAttribute ( "nextdate", "STRING" );
		$this->addAttribute ( "anticipated", "STRING" );
	}

	public function get ( $request, $compress ) {
		/*
			Non e' particolarmente efficiente fare il check sullo stato degli ordini
			ad ogni interrogazione, ma l'alternativa sarebbe piazzare uno script in
			cron rendendo piu' problematico il deploy dell'applicazione.
		*/
		/*
			La data su cui viene confrontato l'ordine per la chiusura e' "oggi + 1",
			per far quadrare i conti sui timestamp calcolati sulle ore
		*/
		$query = sprintf ( "UPDATE %s SET status = 1 WHERE status = 0 AND enddate %s < NOW()", $this->tablename, db_date_plus_one () );
		query_and_check ( $query, "Impossibile chiudere vecchi ordini" );

		/*
			Questo e' per aprire gli ordini che sono stati creati con data di inizio
			nel futuro
		*/
		$query = sprintf ( "UPDATE %s SET status = 0 WHERE status = 2 AND startdate %s < NOW() AND enddate %s > NOW()",
		                    $this->tablename, db_date_plus_one (), db_date_plus_one () );
		query_and_check ( $query, "Impossibile aprire ordini sospesi" );

		/**
			TODO	Settare status ordini con ciclicita'
		*/

		if ( isset ( $request->status ) ) {
			if ( ( string ) $request->status == "any" ) {
				/*
					Aggiungo una condizione sempre vera giusto per
					concatenare poi gli altri pezzi della query correttamente
				*/
				$query = sprintf ( "SELECT id FROM %s WHERE id > 0 ", $this->tablename );
			}
			else {
				$query = sprintf ( "SELECT id FROM %s WHERE status = %d ", $this->tablename, $request->status );
			}
		}
		else {
			/*
				Gli ordini con status = 3 ("consegnato") non sono piu' esposti
				all'applicazione, vengono conservati solo ad uso statistico
			*/
			$query = sprintf ( "SELECT id FROM %s WHERE status != 3 ", $this->tablename );
		}

		$ret = array ();

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query .= sprintf ( "AND id NOT IN ( %s ) ", $ids );
		}

		if ( isset ( $request->supplier ) )
			$query .= sprintf ( "AND supplier = %d ", $request->supplier );

		if ( isset ( $request->startdate ) )
			$query .= sprintf ( "AND startdate > DATE('%s') ", $request->startdate );

		if ( isset ( $request->enddate ) )
			$query .= sprintf ( "AND enddate < DATE('%s') ", $request->enddate );

		$query .= "ORDER BY id DESC";

		if ( isset ( $request->query_limit ) )
			$query .= sprintf ( " LIMIT %d", $request->query_limit );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
		}

		return $ret;
	}

	private function archiveProduct ( $prod, $product ) {
		/*
			Creo una copia, non assegnata a nessun ordine, che
			restera' in attesa per il prossimo giro
		*/
		$dup_prod = $product->exportable ();
		$dup_prod->previous_description = $product->getAttribute ( 'id' )->value;
		$dup_prod->id = -1;
		$prod->save ( $dup_prod );

		/*
			Il prodotto che finisce in quest'ordine viene marcato
			come archiviato: e' accessibile solo se esplicitamente
			chiesto dal pannello degli ordini
		*/
		$original_prod = $product->exportable ();
		$original_prod->archived = "true";
		$prod->save ( $original_prod );
	}

	public function save ( $obj ) {
		$ref = new Product ();

		if ( $obj->id == -1 ) {
			$obj->products = array ();

			/*
				Quando salvo un nuovo ordine prendo per buoni i prodotti correntemente
				ordinabili presso il fornitore di riferimento, e li scrivo nell'apposita
				tabella
			*/
			$query = sprintf ( "SELECT id FROM %s
						WHERE supplier = %d
						AND available = true
						AND archived = false
						ORDER BY id",
							$ref->tablename, $obj->supplier );

			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $ref->classname );
			$rows = $returned->fetchAll ( PDO::FETCH_NUM );

			/*
				Se sto creando un nuovo ordine, duplico tutti i prodotti
				affinche' essi siano disponibili per futuri riferimenti ma quelli
				attuali restino comunque indipendenti. Questo per permettere di
				cambiare i prezzi dei singoli elementi sul singolo ordine, e
				tenere traccia dello storico
			*/

			foreach ( $rows as $row ) {
				$product = new $ref->classname;
				$product->readFromDB ( $row [ 0 ] );

				/*
					Questo e' per risolvere un vecchio bug di GASdotto, per cui i prodotti non
					venivano effettivamente duplicati alla creazione di un nuovo ordine.
					Ma si decide comunque di lasciare qui questo codice, affinche' intervenga
					qualora per qualche stravagante motivo ci si trovi nella situazione di non
					aver archiviato un prodotto incluso in un ordine e per evitare che due ordini
					puntino dunque allo stesso elemento
				*/
				$query = sprintf ( "FROM Orders_products WHERE target = %d", $product->getAttribute ( 'id' )->value );
				if ( db_row_count ( $query ) != 0 ) {
					/*
						Archivio il prodotto originale, linkato dagli
						ordini precedentemente aperti. Presumibilmente e'
						gia' archiviato, ma non si sa mai
					*/
					$old_prod = $product->exportable ();
					$old_prod->archived = "true";
					$ref->save ( $old_prod );

					/*
						Creo una copia, che e' quella che sara' inclusa
						nell'ordine aperto adesso. Sara' marcata come
						archiviata alla fine
					*/
					$buggy_prod = $product->exportable ();
					$buggy_prod->previous_description = $product->getAttribute ( 'id' )->value;
					$buggy_prod->id = -1;
					$buggy_id = $ref->save ( $buggy_prod );

					$product = new $ref->classname;
					$product->readFromDB ( $buggy_id );
				}

				self::archiveProduct ( $ref, $product );
				array_push ( $obj->products, $product->exportable () );
			}
		}
		else {
			foreach ( $obj->products as $product ) {
				/*
					Questo interviene quando aggiungo un prodotto all'interno
					di un ordine, e dunque devo comunque duplicarlo
				*/
				if ( $product->archived != 'true' ) {
					$query = sprintf ( "FROM %s WHERE previous_description = %d", $ref->tablename, $product->id );
					if ( db_row_count ( $query ) == 0 ) {
						$p = new Product ();
						$p->readFromDB ( $product->id );
						self::archiveProduct ( $ref, $p );
					}
				}
			}
		}

		/*
			Gli ordini con data di apertura nel futuro vengono marcati come "sospesi"
		*/
		$startdate = $obj->startdate . " 23:59:59";
		$now = date ( "Y-m-d" ) . " 23:59:59";
		if ( strtotime ( $startdate ) > strtotime ( $now ) )
			$obj->status = 2;

		return parent::save ( $obj );
	}
}

?>
