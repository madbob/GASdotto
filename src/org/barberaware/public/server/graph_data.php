<?php

/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
require_once ( "tcpdf/tcpdf.php" );

class GraphReport extends TCPDF {
	public function ColoredTable ( $header, $data ) {
		$this->SetFillColor ( 224, 235, 255 );
		$this->SetTextColor ( 0 );
		$this->SetDrawColor ( 128, 0, 0 );
		$this->SetLineWidth ( 0.3 );
		$this->SetFont ( 'helvetica', '', 7 );

		$offset = 0;
		$end = 0;
		$tot = count ( $header );

		do {
			$this->AddPage ();

			if ( $end + 10 > $tot )
				$end = $tot;
			else
				$end = $end + 10;

			if ( $tot <= 10 )
				$width = 100;
			else
				$width = 10 * ( $end - $offset );

			$html = '<table cellspacing="0" cellpadding="1" border="1" width="' . $width . '%"><tr>';
			for ( $i = $offset; $i < $end; $i++ ) {
				$html .= '<td>' . ( $header [ $i ] ) . '</td>';
				unset ( $header [ $i ] );
			}
			$html .= '</tr>';

			foreach ( $data as $row ) {
				$html .= '<tr>';

				for ( $i = $offset; $i < $end; $i++ ) {
					$val = $row [ $i ];
					$html .= '<td>' . $val . '</td>';
					unset ( $row [ $i ] );
				}

				$html .= '</tr>';
			}

			$html .= '</table>';

			$this->writeHTML ( $html, true, false, false, false, 'C' );

			$offset = $end;
			unset ( $html );

		} while ( $end < $tot );
	}
}

function suppliers_data ( $supplier, $startdate, $enddate ) {
	$query = sprintf ( "SELECT COUNT(DISTINCT(OrderUser.baseuser)) FROM OrderUser, Orders
				WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
					Orders.startdate > '%s' AND Orders.enddate < '%s'",
						$supplier [ "id" ], $startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
	$tot = $returned->fetchAll ( PDO::FETCH_NUM );
	unset ( $returned );
	unset ( $query );

	$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity)
				FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
				WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
					OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND
					Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
						$supplier [ "id" ], $startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
	$price = $returned->fetchAll ( PDO::FETCH_NUM );
	unset ( $returned );
	unset ( $query );

	$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity)
				FROM OrderUser, Orders, OrderUser_friends, OrderUserFriend, OrderUserFriend_products, ProductUser, Product
				WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
					OrderUser_friends.parent = OrderUser.id AND OrderUser_friends.target = OrderUserFriend.id AND
					OrderUserFriend_products.parent = OrderUserFriend.id AND ProductUser.id = OrderUserFriend_products.target AND
					Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
						$supplier [ "id" ], $startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
	$price += $returned->fetchAll ( PDO::FETCH_NUM );
	unset ( $returned );
	unset ( $query );

	if ( count ( $price ) == 0 )
		$p = 0;
	else
		$p = $price [ 0 ] [ 0 ];

	if ( count ( $tot ) == 0 )
		$t = 0;
	else
		$t = $tot [ 0 ] [ 0 ];

	$suppname = ellipse_string ( $supplier [ "name" ], 25 );
	return array ( $suppname, $t, $p );
}

function users_data ( $user, $supplier, $startdate, $enddate ) {
	/*
		Attenzione: per le statistiche si contano solo gli ordini che sono stati
		consegnati, ovvero quelli chiusi definitivamente
	*/

	$query = sprintf ( "SELECT COUNT(OrderUser.id) FROM OrderUser, Orders
				WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
					OrderUser.status = 2 AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
						$user, $supplier, $startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
	$tot = $returned->fetchColumn ();
	unset ( $returned );
	unset ( $query );

	if ( $tot == 0 ) {
		$price = 0;
	}
	else {
		$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity)
					FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
					WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
						OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND
						Product.id = ProductUser.product AND OrderUser.status = 2 AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
							$user, $supplier, $startdate, $enddate );
		$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
		$price = $returned->fetchColumn ();
		unset ( $returned );
		unset ( $query );

		$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity)
					FROM OrderUser, Orders, OrderUser_friends, OrderUserFriend, OrderUserFriend_products, ProductUser, Product
					WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
						OrderUser_friends.parent = OrderUser.id AND OrderUser_friends.target = OrderUserFriend.id AND
						OrderUserFriend_products.parent = OrderUserFriend.id AND ProductUser.id = OrderUserFriend_products.target AND
						Product.id = ProductUser.product AND OrderUser.status = 2 AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
							$user, $supplier, $startdate, $enddate );
		$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
		$price += $returned->fetchColumn ();
		unset ( $returned );
		unset ( $query );
	}

	return array ( $tot, $price );
}

function products_data ( $supplier, $startdate, $enddate ) {
	$query = sprintf ( "SELECT name, id FROM Product WHERE supplier = %d AND previous_description = 0 ORDER BY name", $supplier );
	$returned = query_and_check ( $query, "Impossibile recuperare lista prodotti" );
	$products = $returned->fetchAll ( PDO::FETCH_NUM );
	unset ( $returned );

	$ret = array ();

	foreach ( $products as $p ) {
		/*
			La query ricorsiva per ricostruire la lista di prodotti logicamente
			concatenati potrebbe essere risolta con una query WITH
			( http://www.postgresql.org/docs/8.4/static/queries-with.html ), che
			pero' e' supportata solo a partire da PostgreSQL 8.4 . Si rimanda il
			perfezionamento a quando tale versione di database server sara'
			abbastanza diffusa
		*/

		$id = $p [ 1 ];
		$tot = 0;
		$val = 0;
		$managed_users = array ();

		while ( true ) {
			/*
				Devo badare a scartare dal conteggio gli utenti che ho gia'
				conteggiato: mi tengo l'array di tutti gli ID e poi li escludo
				nella query SQL
			*/
			if ( count ( $managed_users ) > 0 )
				$excluded = sprintf ( 'OrderUser.baseuser NOT IN (%s)', join ( ', ', $managed_users ) );
			else
				$excluded = 'OrderUser.baseuser > -1';

			$query = sprintf ( "SELECT DISTINCT(OrderUser.baseuser) FROM OrderUser, Orders, OrderUser_products, ProductUser
						WHERE OrderUser.baseorder = Orders.id AND OrderUser_products.parent = OrderUser.id AND
							ProductUser.id = OrderUser_products.target AND ProductUser.product = %d AND
							Orders.startdate > '%s' AND Orders.enddate < '%s' AND %s",
								$id, $startdate, $enddate, $excluded );

			$returned = query_and_check ( $query, "Impossibile recuperare numero utenti per prodotto" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			unset ( $returned );
			$tot += count ( $array );

			foreach ( $array as $u )
				$managed_users [] = $u [ 0 ];

			unset ( $query );
			unset ( $array );

			$query = sprintf ( "SELECT DISTINCT(OrderUser.baseuser) FROM OrderUser, Orders, OrderUserFriend, OrderUser_friends, OrderUserFriend_products, ProductUser
						WHERE OrderUser.baseorder = Orders.id AND OrderUser_friends.parent = OrderUser.id AND
							OrderUser_friends.target = OrderUserFriend.id AND OrderUserFriend_products.parent = OrderUserFriend.id AND
							ProductUser.id = OrderUserFriend_products.target AND ProductUser.product = %d AND
							OrderUser.baseuser NOT IN (%s) AND Orders.startdate > '%s' AND Orders.enddate < '%s' AND %s",
								$id, count ( $managed_users ) == 0 ? '0' : join ( ', ', $managed_users ),
								$startdate, $enddate, $excluded );

			$returned = query_and_check ( $query, "Impossibile recuperare numero utenti per prodotto" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			unset ( $returned );
			$tot += count ( $array );

			foreach ( $array as $u )
				$managed_users [] = $u [ 0 ];

			unset ( $query );
			unset ( $array );

			$query = sprintf ( "SELECT SUM(ProductUser.quantity) * Product.unit_price FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
						WHERE Product.id = %d AND OrderUser.baseorder = Orders.id AND OrderUser_products.parent = OrderUser.id AND
							ProductUser.id = OrderUser_products.target AND ProductUser.product = Product.id AND
							Orders.startdate > '%s' AND Orders.enddate < '%s' GROUP BY Product.unit_price",
								$id, $startdate, $enddate );

			$returned = query_and_check ( $query, "Impossibile recuperare valore per prodotto" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			unset ( $returned );

			if ( count ( $array ) == 1 )
				$val += $array [ 0 ] [ 0 ];

			unset ( $query );
			unset ( $array );

			$query = sprintf ( "SELECT SUM(ProductUser.quantity) * Product.unit_price FROM OrderUser, Orders, OrderUser_friends, OrderUserFriend, OrderUserFriend_products, ProductUser, Product
						WHERE Product.id = %d AND OrderUser.baseorder = Orders.id AND OrderUser_friends.parent = OrderUser.id AND
							OrderUser_friends.target = OrderUserFriend.id AND OrderUserFriend_products.parent = OrderUserFriend.id AND
							ProductUser.id = OrderUserFriend_products.target AND ProductUser.product = Product.id AND
							Orders.startdate > '%s' AND Orders.enddate < '%s' GROUP BY Product.unit_price",
								$id, $startdate, $enddate );

			$returned = query_and_check ( $query, "Impossibile recuperare valore per prodotto" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			unset ( $returned );

			if ( count ( $array ) == 1 )
				$val += $array [ 0 ] [ 0 ];

			unset ( $query );
			unset ( $array );

			$query = sprintf ( "FROM Product WHERE previous_description = %d", $id );
			if ( db_row_count ( $query ) <= 0 )
				break;

			$returned = query_and_check ( "SELECT id " . $query, "Impossibile recuperare prodotto successivo" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			unset ( $returned );
			$id = $array [ 0 ] [ 0 ];

			unset ( $query );
			unset ( $array );
		}

		$ret [] = array ( $p [ 0 ], ( string ) $tot, ( string ) $val );
	}

	unset ( $query );
	unset ( $products );
	return $ret;
}

function list_suppliers ( $startdate, $enddate ) {
	$query = sprintf ( "SELECT id, name FROM Supplier
				WHERE id IN (SELECT DISTINCT(supplier) FROM Orders WHERE startdate > '%s' AND enddate < '%s')
				ORDER BY name ASC",
					$startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare fornitori" );
	$rows_suppliers = $returned->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $returned );
	unset ( $query );
	return $rows_suppliers;
}

function list_users ( $startdate, $enddate ) {
	$query = sprintf ( "SELECT id, firstname, surname FROM Users
				WHERE id IN (SELECT DISTINCT(OrderUser.baseuser) FROM OrderUser, Orders
						WHERE OrderUser.baseorder = Orders.id AND Orders.startdate > '%s' AND Orders.enddate < '%s')
				ORDER BY surname, firstname DESC",
					$startdate, $enddate );

	$returned = query_and_check ( $query, "Impossibile recuperare utenti" );
	$rows_users = $returned->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $returned );
	unset ( $query );
	return $rows_users;
}

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$perm = current_permissions ();
if ( $perm != 1 && $perm != 2 )
	error_exit ( "Permessi non sufficienti" );

$document = $_GET [ 'document' ];
if ( isset ( $document ) == false )
	error_exit ( "Richiesta non specificata, manca tipo documento" );

$type = $_GET [ 'type' ];
if ( isset ( $type ) == false )
	error_exit ( "Richiesta non specificata, manca tipo dato" );

$graph = $_GET [ 'graph' ];
if ( isset ( $graph ) == false )
	error_exit ( "Richiesta non specificata, manca tipo statistiche" );

$startdate = $_GET [ 'startdate' ];
if ( isset ( $startdate ) == false )
	error_exit ( "Richiesta non specificata, manca data inizio" );

$enddate = $_GET [ 'enddate' ];
if ( isset ( $enddate ) == false )
	error_exit ( "Richiesta non specificata, manca data fine" );

if ( $graph == 0 ) {

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////// CSV

	if ( $document == 'csv' ) {
		$ret = ';';

		if ( $type == 'users' ) {
			$rows_suppliers = list_suppliers ( $startdate, $enddate );
			$rows_users = list_users ( $startdate, $enddate );

			$supplier_total_orders = array ();
			$supplier_total_price = array ();

			for ( $i = 0; $i < count ( $rows_suppliers ); $i++ ) {
				$ret .= ( $rows_suppliers [ $i ] [ "name" ] ) . ' - Ordini;' . ( $rows_suppliers [ $i ] [ "name" ] ) . ' - Valore (in euro);';
				$supplier_total_orders [] = 0;
				$supplier_total_price [] = 0;
			}

			$ret .= "Totale Ordini;Totale Valore\n";

			foreach ( $rows_users as $user ) {
				$ret .= ( $user [ 'surname' ] ) . ' ' . ( $user [ 'firstname' ] ) . ';';
				$total_orders = 0;
				$total_price = 0;

				for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
					list ( $tot, $price ) = users_data ( $user [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );

					if ( $price != 0 && $price != '' ) {
						$ret .= ( $tot . ';' . ( format_price ( $price, false ) ) . ';' );
						$total_orders += $tot;
						$total_price += $price;

						$supplier_total_orders [ $a ] = $supplier_total_orders [ $a ] + 1;
						$supplier_total_price [ $a ] = $supplier_total_price [ $a ] + $price;
					}
					else {
						$ret .= ';;';
					}

					unset ( $tot );
					unset ( $price );
				}

				$ret .= ( $total_orders . ';' . ( format_price ( $total_price, false ) ) . "\n" );
			}

			$ret .= ';';
			$total_orders = 0;
			$total_price = 0;

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
				$ret .= ( $supplier_total_orders [ $a ] ) . ';' . ( format_price ( $supplier_total_price [ $a ], false ) ) . ';';
				$total_orders += $supplier_total_orders [ $a ];
				$total_price += $supplier_total_price [ $a ];
			}

			$ret .= ( $total_orders . ';' . ( format_price ( $total_price, false ) ) . "\n" );
			unset ( $supplier_total_orders );
			unset ( $supplier_total_price );

			unset ( $rows_users );
			unset ( $rows_suppliers );

			header ( "Content-Type: plain/text" );
			header ( 'Content-Disposition: inline; filename="' . 'statistiche_utenti_fornitori.csv' . '";' );
		}
		else if ( $type == 'products' ) {
			$supplier = $_GET [ 'extra' ];
			if ( isset ( $supplier ) == false )
				error_exit ( "Richiesta non specificata, manca fornitore di riferimento" );

			$ret .= "Totale Utenti;Totale Valore (in euro)\n";
			$products = products_data ( $supplier, $startdate, $enddate );
			$total = 0;

			for ( $i = 0; $i < count ( $products ); $i++ ) {
				$r = $products [ $i ];
				$ret .= ( $r [ 0 ] ) . ';' . ( $r [ 1 ] ) . ';' . ( format_price ( $r [ 2 ], false ) ) . "\n";
				$total += $r [ 2 ];
			}

			$ret .= ';;' . ( format_price ( $total, false ) ) . "\n";

			unset ( $products );

			header ( "Content-Type: plain/text" );
			header ( 'Content-Disposition: inline; filename="' . 'statistiche_prodotti_fornitori.csv' . '";' );
		}

		echo $ret;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////// PDF

	else if ( $document == 'pdf' ) {
		$header = array ();
		$header [] = "";

		if ( $type == 'users' ) {
			$rows_suppliers = list_suppliers ( $startdate, $enddate );
			$rows_users = list_users ( $startdate, $enddate );

			for ( $i = 0; $i < count ( $rows_suppliers ); $i++ ) {
				$header [] = $rows_suppliers [ $i ] [ "name" ];
				unset ( $rows_suppliers [ $i ] [ "name" ] );
			}

			$header [] = "Totale";
			$data = array ();

			$supplier_total_orders = array ();
			$supplier_total_price = array ();

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
				$supplier_total_orders [] = 0;
				$supplier_total_price [] = 0;
			}

			foreach ( $rows_users as $user ) {
				$row = array ();
				$total_price = 0;
				$total_orders = 0;

				if ( isset ( $user [ 'surname' ] ) )
					$s = ellipse_string ( $user [ 'surname' ], 12 );
				else
					$s = '';

				if ( isset ( $user [ 'firstname' ] ) )
					$n = ellipse_string ( $user [ 'firstname' ], 12 );
				else
					$n = '';

				$row [] = $s . '<br />' . $n;

				for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
					list ( $tot, $price ) = users_data ( $user [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );

					if ( $price [ 0 ] [ 0 ] != "" ) {
						$row [] = ( $tot [ 0 ] [ 0 ] ) . ' ordini /<br />' . ( format_price ( $price [ 0 ] [ 0 ] ) );
						$total_price += $price [ 0 ] [ 0 ];
						$total_orders += $tot [ 0 ] [ 0 ];

						$supplier_total_orders [ $a ] = $supplier_total_orders [ $a ] + 1;
						$supplier_total_price [ $a ] = $supplier_total_price [ $a ] + $price [ 0 ] [ 0 ];
					}
					else {
						$row [] = '<br />';
					}

					unset ( $tot );
					unset ( $price );
				}

				$row [] = $total_orders . ' ordini /<br />' . ( format_price ( $total_price ) );
				$data [] = $row;
			}

			$row = array ();
			$row [] = "";
			$gran_total = 0;

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
				$row [] = ( $supplier_total_orders [ $a ] ) . ' utenti /<br />' . ( format_price ( $supplier_total_price [ $a ] ) );
				$gran_total += $supplier_total_price [ $a ];
			}

			$row [] = format_price ( $gran_total );
			$data [] = $row;
			unset ( $supplier_total_orders );
			unset ( $supplier_total_price );

			unset ( $rows_users );
			unset ( $rows_suppliers );

			$file_title = 'Statistiche Utenti/Fornitori';
			$file_name = 'statistiche_utenti_fornitori.pdf';
		}
		else if ( $type == 'products' ) {
			$supplier = $_GET [ 'extra' ];
			if ( isset ( $supplier ) == false )
				error_exit ( "Richiesta non specificata, manca fornitore di riferimento" );

			$header [] = 'Totale Utenti';
			$header [] = 'Totale Valore';
			$data = products_data ( $supplier, $startdate, $enddate );
			$total = 0;

			for ( $i = 0; $i < count ( $data ); $i++ ) {
				$data [ $i ] [ 2 ] = ( format_price ( $data [ $i ] [ 2 ], false ) ) . ' euro';
				$total += $data [ $i ] [ 2 ];
			}

			$data [] = array ( '', '', format_price ( $total, false ) . ' euro' );

			$supp = new Supplier ();
			$supp->readFromDB ( $supplier );

			$file_title = 'Statistiche Prodotti/Fornitore ' . ( $supp->getAttribute ( 'name' )->value );
			$file_name = 'statistiche_prodotti_fornitori.pdf';
		}

		$pdf = new GraphReport ( 'P', 'mm', 'A4', true, 'UTF-8', false );
		$pdf->SetCreator ( 'TCPDF' );
		$pdf->SetAuthor ( 'GASdotto' );
		$pdf->SetTitle ( $file_title );
		$pdf->SetSubject ( $file_title );
		$pdf->SetKeywords ( 'statistiche, fornitori, GASdotto, GAS' );
		$pdf->SetHeaderData ( '', 0, $file_title );
		$pdf->setHeaderFont ( Array ( 'helvetica', '', 10 ) );
		$pdf->setFooterFont ( Array ( 'helvetica', '', 8 ) );
		$pdf->SetDefaultMonospacedFont ( 'courier' );
		$pdf->SetMargins ( 15, 27, 25, 15 );
		$pdf->SetHeaderMargin ( 5 );
		$pdf->SetFooterMargin ( 10 );
		$pdf->SetAutoPageBreak ( true, 25 );
		$pdf->setImageScale ( 1 );
		$pdf->ColoredTable ( $header, $data );
		$pdf->Output ( $file_name, 'I' );
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////// GRAFICI

	else if ( $document == 'visual' ) {
		$ret = new stdClass ();
		$array = array ();

		if ( $type == 'users' ) {
			$rows_suppliers = list_suppliers ( $startdate, $enddate );
			for ( $i = 0; $i < count ( $rows_suppliers ); $i++ )
				$array [] = suppliers_data ( $rows_suppliers [ $i ], $startdate, $enddate );
		}
		else if ( $type == 'products' ) {
			$supplier = $_GET [ 'extra' ];
			if ( isset ( $supplier ) == false )
				error_exit ( "Richiesta non specificata, manca fornitore di riferimento" );

			$array = products_data ( $supplier, $startdate, $enddate );

			for ( $i = 0; $i < count ( $array ); $i++ )
				$array [ $i ] [ 0 ] = ellipse_string ( $array [ $i ] [ 0 ], 25 );
		}

		$ret->data = $array;
		echo json_encode ( $ret ) . "\n";
	}
}

?>
