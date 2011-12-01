<?php

/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class Order extends SharableFromServer {
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
		$this->addAttribute ( "mail_summary_sent", "DATE" );
		$this->addAttribute ( "mail_summary_text", "STRING" );
		$this->addAttribute ( "parent_aggregate", "BOOLEAN" );

		$this->setPublic ( false );
	}

	public function get ( $request, $compress ) {
		$ret = array ();

		if ( isset ( $request->id ) ) {
			$query = sprintf ( "SELECT id FROM %s WHERE id = %d", $this->tablename, $request->id );
		}
		else {
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

			$query .= $this->filter_by_current_gas ();

			$query .= "ORDER BY id DESC";

			if ( isset ( $request->query_limit ) )
				$query .= sprintf ( " LIMIT %d", $request->query_limit );
		}

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
		unset ( $returned );

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

		if ( $obj->id == -1 && ( property_exists ( $obj, 'products' ) == false || count ( $obj->products ) == 0 ) ) {
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
			unset ( $returned );

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

			if ( isset ( $obj->mail_summary_sent ) )
				self::send_summary ( $obj );
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

	public function export ( $options ) {
		$order = "\t\t<orders>\n";
		$order .= "\t\t\t<order>\n";
		$order .= "\t\t\t\t<openDate>" . exportable_date ( self::getAttribute ( 'startdate' )->value ) . "</openDate>\n";
		$order .= "\t\t\t\t<closeDate>" . exportable_date ( self::getAttribute ( 'enddate' )->value ) . "</closeDate>\n";

		$ship = self::getAttribute ( 'shippingdate' )->value;
		if ( $ship != '' )
			$order .= "\t\t\t\t<deliveryDate>" . exportable_date ( self::getAttribute ( 'shippingdate' )->value ) . "</deliveryDate>\n";

		$order .= "\t\t\t</order>\n";
		$order .= "\t\t</orders>\n";

		$products = exportable_products ( FromServer::exportable_array ( self::getAttribute ( 'products' )->value ) );

		$supplierid = self::getAttribute ( 'supplier' )->value->getAttribute ( 'id' )->value;
		$supplier = new Supplier ();
		$supplier->readFromDB ( $supplierid );
		return $supplier->export ( array ( 'products' => $products, 'child' => $order ) );
	}

	public static function import ( $ref, $contents ) {
		$ret = array ();

		$elements = $contents->xpath ( '//orders/order' );

		foreach ( $elements as $el ) {
			$final = new Order ();

			foreach ( $el->children () as $child ) {
				$name = $child->getName ();
				$value = $child;

				switch ( $name ) {
					case 'openDate':
						$final->getAttribute ( 'startdate' )->value = import_date ( $value );
						break;

					case 'closeDate':
						$final->getAttribute ( 'enddate' )->value = import_date ( $value );
						break;

					case 'deliveryDate':
						$final->getAttribute ( 'shippingdate' )->value = import_date ( $value );
						break;
				}
			}

			$ret [] = $final->exportable ();
		}

		$ref->orders = $ret;
	}

	private function send_summary ( $obj ) {
		$test = new Order ();
		$test->readFromDB ( $obj->id );

		if ( format_date ( $test->getAttribute ( 'mail_summary_sent' )->value ) != format_date ( $obj->mail_summary_sent ) ) {
			$message = $obj->mail_summary_text;
			$products = $test->getAttribute ( 'products' )->value;
			usort ( $products, 'sort_product_by_name' );

			$supplier = $test->getAttribute ( 'supplier' )->value;

			$gas = current_gas ();
			$orderusers = get_orderuser_by_order ( $test );

			foreach ( $orderusers as $ou ) {
				/*
					Feature #170: non si invia mail di
					riepilogo a chi ha gia' ritirato l'ordine
				*/
				if ( $ou->status == 2 )
					continue;

				$user_products = $ou->products;
				if ( is_array ( $user_products ) == false )
					continue;

				$dests = array ();

				if ( isset ( $ou->baseuser->mail ) && $ou->baseuser->mail != '' )
					$dests [] = $ou->baseuser->mail;
				if ( isset ( $ou->baseuser->mail2 ) && $ou->baseuser->mail2 != '' )
					$dests [] = $ou->baseuser->mail2;

				if ( count ( $dests ) == 0 )
					continue;

				$user_products = sort_products_on_products ( $products, $user_products );
				$user_total = 0;

				$text = sprintf ( "Di seguito il riassunto dei prodotti che hai ordinato presso %s insieme agli altri membri di %s.\n\n",
							$supplier->getAttribute ( 'name' )->value, $gas->getAttribute ( 'name' )->value );

				$extra_notify = '';

				$html = '<html><head><meta http-equiv="Content-Type" content="text/html"; charset="UTF-8" /></head>';
				$html .= '<body><p>' . $text . '</p><br /><table><tr><td>Prodotto</td><td>Quantità</td><td>Prezzo</td><td>Prezzo Trasporto</td></tr>';

				if ( $ou->status == 3 )
					$param = 'delivered';
				else
					$param = 'quantity';

				for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
					$prod = $products [ $a ];
					$prodid = $prod->getAttribute ( "id" )->value;
					$prod_user = $user_products [ $e ];
					$quantity = 0;

					$variants = array ();

					if ( $prodid == $prod_user->product ) {
						$quantity = $prod_user->$param;
						$e++;

						if ( is_array ( $prod_user->variants ) )
							$variants = $prod_user->variants;
					}

					if ( count ( $ou->friends ) != 0 ) {
						foreach ( $ou->friends as $friend ) {
							foreach ( $friend->products as $fprod ) {
								if ( $fprod->product == $prodid ) {
									$quantity += $fprod->$param;

									if ( is_array ( $fprod->variants ) )
										$variants = array_merge ( $variants, $fprod->variants );

									break;
								}
							}
						}
					}

					if ( $quantity != 0 ) {
						$unit = $prod->getAttribute ( "unit_size" )->value;
						$uprice = $prod->getAttribute ( "unit_price" )->value;
						$sprice = $prod->getAttribute ( "shipping_price" )->value;

						if ( $unit <= 0.0 ) {
							$q = comma_format ( $quantity );
						}
						else {
							if ( $ou->status == 0 ) {
								$q = ( $quantity / $unit );
								$q = ( comma_format ( $q ) ) . ' pezzi';
								$extra_notify = "l'importo reale di questo ordine dipende dal peso effettivo dei prodotti consegnati; il totale qui riportato è solo indicativo";
							}
							else {
								$measure = $prod->getAttribute ( "measure" )->value;
								$q = ( comma_format ( $quantity ) ) . ' ' . ( $measure->getAttribute ( "name" )->value );
								$extra_notify = "quantità ed importo indicati sono quelli effettivi";
							}
						}

						$quprice = ( $quantity * $uprice );
						$qsprice = ( $quantity * $sprice );

						$user_total += sum_percentage ( $quprice, $prod->getAttribute ( "surplus" )->value ) + $qsprice;

						$quprice = format_price ( round ( $quprice, 2 ) );
						$qsprice = format_price ( round ( $qsprice, 2 ) );

						$text .= ( $prod->getAttribute ( "name" )->value ) . "\n";
						$text .= "\t\t" . $q . "\t" . $quprice . "\t" . $qsprice . "\n";

						$html .= '<tr><td>' . ( $prod->getAttribute ( "name" )->value );
						$html .= '</td><td>' . $q . '</td><td>' . $quprice . '</td><td>' . $qsprice . '</td></tr>';

						if ( ( $unit <= 0.0 ) && ( count ( $variants ) != 0 ) ) {
							list ( $variants, $quantities ) = aggregate_variants ( $variants );

							for ( $j = 0; $j < count ( $variants ); $j++ ) {
								$r = ( $quantities [ $j ] ) . ' ' . ( $variants [ $j ] );
								$text .= "\t" . $r . "\n";
								$html .= '<tr><td colspan="2">&nbsp;&nbsp;&nbsp;' . $r . '</td><td>&nbsp;</td><td>&nbsp;</td></tr>';
							}
						}
					}
				}

				$user_total = format_price ( round ( $user_total, 2 ) );

				$text .= "\nPer un totale di " . $user_total;
				if ( $extra_notify != '' )
					$text .= ' (' . $extra_notify . ')';
				if ( isset ( $message ) && $message != '' )
					$text .= "\n\n\nMessaggio dai Referenti:\n" . $message;
				$text .= ".\n\n\nIn caso di problemi su questo ordine NON rispondere a questo messaggio ma contatta il Referente del Fornitore.\n";

				$html .= '</table><p>Per un totale di ' . $user_total;
				if ( $extra_notify != '' )
					$html .= ' (' . $extra_notify . ')';
				if ( isset ( $message ) && $message != '' )
					$html .= "</p><br /><p>Messaggio dai Referenti:<br />" . $message;
				$html .= '.</p><br /><p>In caso di problemi su questo ordine NON rispondere a questo messaggio ma contatta il Referente del Fornitore.</p><ul>';

				foreach ( $supplier->getAttribute ( 'references' )->value as $ref ) {
					if ( ( $ref->getAttribute ( 'mail' )->value != "" ) )
						$mail = $ref->getAttribute ( 'mail' )->value;
					else
						$mail = $ref->getAttribute ( 'mail2' )->value;

					$text .= "\t" . ( $ref->getAttribute ( 'firstname' )->value ) . " " . ( $ref->getAttribute ( 'surname' )->value ) . " - " . $mail . "\n";
					$html .= "<li>" . ( $ref->getAttribute ( 'firstname' )->value ) . " " . ( $ref->getAttribute ( 'surname' )->value ) . " - <a href=\"mailto:" . $mail . "\">" . $mail . "</a></li>";
				}

				$html .= '</ul></body></html>';

				my_send_mail ( $dests, 'Riassunto dell\'ordine a ' . $supplier->getAttribute ( 'name' )->value, true, $text, $html );
				unset ( $dests );
			}

			unset ( $orderusers );
		}
	}
}

?>
