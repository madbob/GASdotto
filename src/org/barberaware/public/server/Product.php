<?

/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class Product extends FromServer {
	public function __construct () {
		parent::__construct ( "Product" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "category", "OBJECT::Category" );
		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "shipping_price", "FLOAT" );
		$this->addAttribute ( "unit_price", "FLOAT" );
		$this->addAttribute ( "surplus", "PERCENTAGE" );
		$this->addAttribute ( "measure", "OBJECT::Measure" );
		$this->addAttribute ( "minimum_order", "FLOAT" );
		$this->addAttribute ( "multiple_order", "FLOAT" );
		$this->addAttribute ( "stock_size", "FLOAT" );
		$this->addAttribute ( "unit_size", "FLOAT" );
		$this->addAttribute ( "mutable_price", "BOOLEAN" );
		$this->addAttribute ( "available", "BOOLEAN" );
		$this->addAttribute ( "archived", "BOOLEAN" );
		$this->addAttribute ( "previous_description", "INTEGER" );
	}

	public function get ( $request, $compress ) {
		$ret = array ();

		if ( isset ( $request->supplier ) )
			$tuning = sprintf ( " AND supplier = %d ", $request->supplier );
		else
			$tuning = "";

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query = sprintf ( "SELECT id FROM %s WHERE id NOT IN ( %s ) AND archived = false %s ORDER BY id",
						$this->tablename, $ids, $tuning );
		}
		else {
			$query = sprintf ( "SELECT id FROM %s WHERE archived = false %s ORDER BY id",
						$this->tablename, $tuning );
		}

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
		}

		return $ret;
	}

	public function save ( $obj ) {
		/*
			Immenso trigo per il salvataggio dei prodotti, dovuto al fatto che essi
			devono essere allineati con gli ordini aperti.

			Quando aggiungo un prodotto nuovo (ed ordinabile) devo vedere se e' per
			un fornitore per il quale c'e' un ordine attivo, e nel caso introdurlo
			nella lista di quelli ordinabili.

			Quando modifico un prodotto esistente
				- se non c'e' alcun ordine (aperto o chiuso che sia) che lo
					contempla tiro dritto
				- se c'e' un ordine (aperto o chiuso che sia), il prodotto viene
					duplicato in modo che i vecchi riferimenti continuino a
					puntare a quello vecchio ed i successivi vadano a quello
					nuovo
				- ma se e' stata modificata solo la disponibilita', mi limito ad
					allineare eventuali ordini aperti rimuovendone o
					aggiungendone il riferimento

			Questa modalita' permette di garantire un certo allineamento automatico
			tra ordini e prodotti, senza avere pannelli a livello utente che
			gestiscono questa cosa, sebbene ponga molti limiti all'interazione
			(appunto perche' e' costruita su un sacco di assunzioni)
		*/

		$align_existing_orders = false;

		if ( $obj->id != -1 ) {
			$prod = new Product ();
			$prod->readFromDB ( $obj->id );

			if ( $prod->getAttribute ( "available" )->value != $obj->available ) {
				$align_existing_orders = true;
			}
			else {
				if ( ( $obj->archived == false ) && ( db_row_count ( sprintf ( "FROM Orders_products WHERE target = %d", $obj->id ) ) != 0 ) ) {
					$query = sprintf ( "UPDATE %s SET archived = true WHERE id = %d", $this->tablename, $obj->id );
					query_and_check ( $query, "Impossibile sincronizzare " . $this->classname );

					/*
						Se il Product e' gia' contemplato in un Order, ne valuto la
						duplicazione. Se gia' esiste un duplicato cronologicamente successivo
						lascio perdere, altrimenti forzo l'ID a -1 per ingannare
						FromServer::save() e appunto setto l'ID corrente come antecedente,
						per evitare di ripetere nuovamente l'operazione
					*/

					$query = sprintf ( "FROM %s WHERE previous_description = %d", $this->tablename, $obj->id );
					if ( db_row_count ( $query ) == 0 ) {
						$obj->previous_description = $obj->id;
						$obj->id = -1;
					}
				}
			}
		}
		else {
			$align_existing_orders = true;
		}

		$id = parent::save ( $obj );

		if ( $align_existing_orders == true ) {
			$query = sprintf ( "SELECT id FROM Orders WHERE supplier = %d AND status = 0",
			                   $obj->supplier->id, date ( "Y-m-d" ) );
			$returned = query_and_check ( $query, "Impossibile verificare lista oggetti " . $this->classname );
			$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

			if ( $obj->available == "true" ) {
				foreach ( $rows as $row ) {
					$query = sprintf ( "FROM Orders_products WHERE parent = %d AND target = %d", $row [ "id" ], $id );
					if ( db_row_count ( $query ) == 0 ) {
						$query = sprintf ( "INSERT INTO Orders_products ( parent, target ) VALUES ( %d, %d )",
								$row [ "id" ], $id );
						query_and_check ( $query, "Impossibile aggiungere prodotto ora ordinabile" );
					}
				}
			}
			else {
				foreach ( $rows as $row ) {
					$query = sprintf ( "DELETE FROM Orders_products WHERE parent = %d AND target = %d",
					                   $row [ "id" ], $id );
					query_and_check ( $query, "Impossibile eliminare prodotto non piu' ordinabile" );
				}
			}
		}

		/*
		$query = sprintf ( "UPDATE %s SET archived = false WHERE id = %d", $this->tablename, $id );
		$returned = query_and_check ( $query, "Impossibile sincronizzare " . $this->classname );
		*/

		return $id;
	}

	public function getTotalPrice () {
	    $tot = 0;

	    $tot = $this->getAttribute ( "unit_price" )->value;

	    $s = $this->getAttribute ( "shipping_price" )->value;
	    if ( $s != "" )
		$tot += $s;

	    $s = $this->getAttribute ( "surplus" )->value;
	    if ( $s != "" && $s != 0 ) {
		if ( $s [ strlen ( $s ) - 1 ] == '%' )
		    $tot += ( $tot * rtrim ( $s, "%" ) ) / 100;
		else
		    $tot += $s;
	    }

	    return $tot;
	}
}

?>
