<?

/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

function users_data ( $user, $supplier, $startdate, $enddate ) {
	$query = sprintf ( "SELECT COUNT(OrderUser.id) FROM OrderUser, Orders
				WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
					Orders.startdate > '%s' AND Orders.enddate < '%s'",
						$user, $supplier, $startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
	$tot = $returned->fetchAll ( PDO::FETCH_NUM );
	unset ( $query );
	unset ( $returned );

	$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity)
				FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
				WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
					OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND
					Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
						$user, $supplier, $startdate, $enddate );
	$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
	$price = $returned->fetchAll ( PDO::FETCH_NUM );
	unset ( $query );
	unset ( $returned );

	return array ( $tot, $price );
}

function products_data ( $supplier, $startdate, $enddate ) {
	$query = sprintf ( "SELECT name, id FROM Product WHERE supplier = %d AND previous_description = 0 ORDER BY name", $supplier );
	$returned = query_and_check ( $query, "Impossibile recuperare lista prodotti" );
	$products = $returned->fetchAll ( PDO::FETCH_NUM );

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

		while ( true ) {
			$query = sprintf ( "SELECT COUNT(DISTINCT(OrderUser.baseuser)) FROM OrderUser, Orders, OrderUser_products, ProductUser
						WHERE OrderUser.baseorder = Orders.id AND OrderUser_products.parent = OrderUser.id AND
							ProductUser.id = OrderUser_products.target AND ProductUser.product = %d AND
							Orders.startdate > '%s' AND Orders.enddate < '%s'",
								$id, $startdate, $enddate );

			$returned = query_and_check ( $query, "Impossibile recuperare numero utenti per prodotto" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			$tot += $array [ 0 ] [ 0 ];

			unset ( $query );
			unset ( $returned );
			unset ( $array );

			$query = sprintf ( "SELECT SUM(ProductUser.quantity) * Product.unit_price FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
						WHERE Product.id = %d AND OrderUser.baseorder = Orders.id AND OrderUser_products.parent = OrderUser.id AND
							ProductUser.id = OrderUser_products.target AND ProductUser.product = Product.id AND
							Orders.startdate > '%s' AND Orders.enddate < '%s' GROUP BY Product.unit_price",
								$id, $startdate, $enddate );

			$returned = query_and_check ( $query, "Impossibile recuperare valore per prodotto" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			if ( count ( $array ) == 1 )
				$val += $array [ 0 ] [ 0 ];

			unset ( $query );
			unset ( $returned );
			unset ( $array );

			$query = sprintf ( "FROM Product WHERE previous_description = %d", $id );
			if ( db_row_count ( $query ) <= 0 )
				break;

			$returned = query_and_check ( "SELECT id " . $query, "Impossibile recuperare prodotto successivo" );
			$array = $returned->fetchAll ( PDO::FETCH_NUM );
			$id = $array [ 0 ] [ 0 ];

			unset ( $query );
			unset ( $returned );
			unset ( $array );
		}

		$ret [] = array ( $p [ 0 ], ( string ) $tot, ( string ) $val );
	}

	unset ( $query );
	unset ( $returned );
	unset ( $products );
	return $ret;
}

function list_suppliers () {
	$query = sprintf ( "SELECT id, name FROM Supplier ORDER BY name ASC" );
	$returned = query_and_check ( $query, "Impossibile recuperare fornitori" );
	$rows_suppliers = $returned->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $query );
	unset ( $returned );
	return $rows_suppliers;
}

function list_users () {
	$query = sprintf ( "SELECT id, firstname, surname FROM Users ORDER BY surname, firstname DESC" );
	$returned = query_and_check ( $query, "Impossibile recuperare utenti" );
	$rows_users = $returned->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $query );
	unset ( $returned );
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
			$rows_suppliers = list_suppliers ();
			$rows_users = list_users ();

			for ( $i = 0; $i < count ( $rows_suppliers ); $i++ )
				$ret .= ( $rows_suppliers [ $i ] [ "name" ] ) . ';';

			$ret .= "Totale\n";

			$supplier_total_orders = array ();
			$supplier_total_price = array ();

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
				$supplier_total_orders [] = 0;
				$supplier_total_price [] = 0;
			}

			foreach ( $rows_users as $user ) {
				$ret .= ( $user [ 'surname' ] ) . ' ' . ( $user [ 'firstname' ] ) . ';';
				$total_price = 0;

				for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
					list ( $tot, $price ) = users_data ( $user [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );

					if ( $price [ 0 ] [ 0 ] != "" ) {
						$ret .= ( ( $tot [ 0 ] [ 0 ] ) . ' ordini / ' . ( format_price ( $price [ 0 ] [ 0 ], false ) ) . ' euro;' );
						$total_price += $price [ 0 ] [ 0 ];

						$supplier_total_orders [ $a ] = $supplier_total_orders [ $a ] + $tot [ 0 ] [ 0 ];
						$supplier_total_price [ $a ] = $supplier_total_price [ $a ] + $price [ 0 ] [ 0 ];
					}
					else {
						$ret .= ';';
					}

					unset ( $tot );
					unset ( $price );
				}

				$ret .= ( format_price ( $total_price, false ) ) . " euro\n";
			}

			$ret .= ';';

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ )
				$ret .= ( $supplier_total_orders [ $a ] ) . ' ordini / ' . ( format_price ( $supplier_total_price [ $a ], false ) ) . ' euro;';

			$ret .= "\n";
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

			$ret .= "Totale Utenti;Totale Valore\n";
			$products = products_data ( $supplier, $startdate, $enddate );

			for ( $i = 0; $i < count ( $products ); $i++ ) {
				$r = $products [ $i ];
				$ret .= ( $r [ 0 ] ) . ';' . ( $r [ 1 ] ) . ';' . ( format_price ( $r [ 2 ], false ) ) . " euro\n";
			}

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
			$rows_suppliers = list_suppliers ();
			$rows_users = list_users ();

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
				$s = '';
				$n = '';

				if ( isset ( $user [ 'surname' ] ) ) {
					$s = sprintf ( "%s", $user [ 'surname' ] );
					if ( strlen ( $s ) > 12 ) {
						$s = substr ( $s, 0, 10 );
						$s .= '...';
					}
				}

				if ( isset ( $user [ 'firstname' ] ) ) {
					$n = sprintf ( "%s", $user [ 'firstname' ] );
					if ( strlen ( $n ) > 12 ) {
						$n = substr ( $n, 0, 10 );
						$n .= '...';
					}
				}

				$row [] = $s . '<br />' . $n;

				for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
					list ( $tot, $price ) = users_data ( $user [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );

					if ( $price [ 0 ] [ 0 ] != "" ) {
						$row [] = ( $tot [ 0 ] [ 0 ] ) . ' ordini /<br />' . ( format_price ( $price [ 0 ] [ 0 ] ) );
						$total_price += $price [ 0 ] [ 0 ];

						$supplier_total_orders [ $a ] = $supplier_total_orders [ $a ] + $tot [ 0 ] [ 0 ];
						$supplier_total_price [ $a ] = $supplier_total_price [ $a ] + $price [ 0 ] [ 0 ];
					}
					else {
						$row [] = '<br />';
					}

					unset ( $tot );
					unset ( $price );
				}

				$row [] = format_price ( $total_price );
				$data [] = $row;
			}

			$row = array ();
			$row [] = "";

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
				$row [] = ( $supplier_total_orders [ $a ] ) . ' ordini /<br />' . ( format_price ( $supplier_total_price [ $a ] ) );
				unset ( $rows_suppliers [ $a ] );
			}

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

			$header [] = "Totale Utenti";
			$header [] = "Totale Valore";
			$data = products_data ( $supplier, $startdate, $enddate );

			for ( $i = 0; $i < count ( $data ); $i++ )
				$data [ $i ] [ 2 ] = ( format_price ( $data [ $i ] [ 2 ], false ) ) . " euro";

			$file_title = 'Statistiche Prodotti/Fornitori';
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

		if ( $type == 'users' ) {
			$rows_suppliers = list_suppliers ();

			for ( $i = 0; $i < count ( $rows_suppliers ); $i++ ) {
				$query = sprintf ( "SELECT COUNT(DISTINCT(OrderUser.baseuser)) FROM OrderUser, Orders
							WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
								Orders.startdate > '%s' AND Orders.enddate < '%s'",
									$rows_suppliers [ $i ] [ "id" ], $startdate, $enddate );
				$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
				$tot = $returned->fetchAll ( PDO::FETCH_NUM );
				unset ( $query );
				unset ( $returned );

				$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity)
							FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
							WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND
								OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND
								Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
									$rows_suppliers [ $i ] [ "id" ], $startdate, $enddate );
				$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
				$price = $returned->fetchAll ( PDO::FETCH_NUM );
				unset ( $query );
				unset ( $returned );

				if ( count ( $price ) == 0 )
					$p = 0;
				else
					$p = $price [ 0 ] [ 0 ];

				if ( count ( $tot ) == 0 )
					$t = 0;
				else
					$t = $tot [ 0 ] [ 0 ];

				$suppname = $rows_suppliers [ $i ] [ "name" ];
				if ( strlen ( $suppname ) > 25 )
					$suppname = substr ( $suppname, 0, 23 ) . '...';

				$array [] = array ( $suppname, $t, $p );

				unset ( $tot );
				unset ( $price );
			}
		}
		else if ( $type == 'products' ) {
			$supplier = $_GET [ 'extra' ];
			if ( isset ( $supplier ) == false )
				error_exit ( "Richiesta non specificata, manca fornitore di riferimento" );

			$array = products_data ( $supplier, $startdate, $enddate );

			for ( $i = 0; $i < count ( $array ); $i++ ) {
				$prodname = $array [ $i ] [ 0 ];
				if ( strlen ( $prodname ) > 25 )
					$array [ $i ] [ 0 ] = substr ( $prodname, 0, 23 ) . '...';
			}
		}

		$ret->data = $array;
		$json = new Services_JSON ();
		echo $json->encode ( $ret ) . "\n";
	}
}

?>
