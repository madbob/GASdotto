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
			for ( $i = $offset; $i < $end; $i++ )
				$html .= '<td>' . ( $header [ $i ] ) . '</td>';
			$html .= '</tr>';

			foreach ( $data as $row ) {
				$html .= '<tr>';

				for ( $i = $offset; $i < $end; $i++ ) {
					$val = $row [ $i ];
					$html .= '<td>' . $val . '</td>';
				}

				$html .= '</tr>';
			}

			$html .= '</table>';

			$this->writeHTML ( $html, true, false, false, false, 'C' );

			$offset = $end;

		} while ( $end < $tot );
	}
}

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$perm = current_permissions ();
if ( $perm != 1 && $perm != 2 )
	error_exit ( "Permessi non sufficienti" );

$document = $_GET [ 'document' ];
if ( isset ( $document ) == false )
	error_exit ( "Richiesta non specificata, manca tipo documento" );

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
	$query = sprintf ( "SELECT id, name FROM Supplier ORDER BY name DESC" );
	$returned = query_and_check ( $query, "Impossibile recuperare fornitori" );
	$rows_suppliers = $returned->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $query );
	unset ( $returned );

	$query = sprintf ( "SELECT id, surname FROM Users ORDER BY surname, firstname DESC" );
	$returned = query_and_check ( $query, "Impossibile recuperare utenti" );
	$rows_users = $returned->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $query );
	unset ( $returned );

	if ( $document == 'csv' ) {
		$ret = ';';

		for ( $i = 0; $i < count ( $rows_suppliers ); $i++ )
			$ret .= ( $rows_suppliers [ $i ] [ "name" ] ) . ';';

		$ret .= "Totale\n";

		for ( $i = 0; $i < count ( $rows_users ); $i++ ) {
			$ret .= ( $rows_users [ $i ] [ 'surname' ] ) . ';';
			$total_price = 0;

			for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
				$query = sprintf ( "SELECT COUNT(OrderUser.id) FROM OrderUser, Orders
							WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
								$rows_users [ $i ] [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );
				$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
				$tot = $returned->fetchAll ( PDO::FETCH_NUM );
				unset ( $query );
				unset ( $returned );

				$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity) FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
							WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
								$rows_users [ $i ] [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );
				$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
				$price = $returned->fetchAll ( PDO::FETCH_NUM );

				unset ( $query );
				unset ( $returned );

				if ( $price [ 0 ] [ 0 ] != "" ) {
					$ret .= ( ( $tot [ 0 ] [ 0 ] ) . ' ordini / ' . ( $price [ 0 ] [ 0 ] ) . ' euro;' );
					$total_price += $price [ 0 ] [ 0 ];
				}
				else {
					$ret .= ';';
				}

				unset ( $tot );
				unset ( $price );
			}

			$ret .= $total_price . " euro\n";
		}

		unset ( $rows_users );
		unset ( $rows_suppliers );

		header ( "Content-Type: plain/text" );
		header ( 'Content-Disposition: inline; filename="' . 'statistiche_utenti_fornitori.csv' . '";' );
		echo $ret;
	}
	else if ( $document == 'pdf' ) {
		$header = array ();
		$header [] = "";

		for ( $i = 0; $i < count ( $rows_suppliers ); $i++ ) {
			$header [] = $rows_suppliers [ $i ] [ "name" ];
			unset ( $rows_suppliers [ $i ] [ "name" ] );
		}

		$header [] = "Totale";
		$data = array ();

		for ( $i = 0; $i < count ( $rows_users ); $i++ ) {
			$row = array ();
			$total_price = 0;

			if ( isset ( $rows_users [ $i ] [ 'surname' ] ) ) {
				$n = sprintf ( "%s", $rows_users [ $i ] [ 'surname' ] );
				if ( strlen ( $n ) > 12 ) {
					$n = substr ( $n, 0, 10 );
					$n .= '...';
				}
				$row [] = $n;

				for ( $a = 0; $a < count ( $rows_suppliers ); $a++ ) {
					$query = sprintf ( "SELECT COUNT(OrderUser.id) FROM OrderUser, Orders
								WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
									$rows_users [ $i ] [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );
					$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
					$tot = $returned->fetchAll ( PDO::FETCH_NUM );
					unset ( $query );
					unset ( $returned );

					$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity) FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
								WHERE OrderUser.baseuser = %d AND OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
									$rows_users [ $i ] [ "id" ], $rows_suppliers [ $a ] [ "id" ], $startdate, $enddate );
					$returned = query_and_check ( $query, "Impossibile recuperare somma spesa" );
					$price = $returned->fetchAll ( PDO::FETCH_NUM );
					unset ( $query );
					unset ( $returned );

					if ( $price [ 0 ] [ 0 ] != "" ) {
						$row [] = ( $tot [ 0 ] [ 0 ] ) . ' ordini /<br />' . ( $price [ 0 ] [ 0 ] ) . ' €';
						$total_price += $price [ 0 ] [ 0 ];
					}
					else {
						$row [] = '<br />';
					}

					unset ( $tot );
					unset ( $price );
				}

				$row [] = $total_price . ' €';
				$data [] = $row;
			}

			unset ( $rows_users [ $i ] );
		}

		unset ( $rows_users );
		unset ( $rows_suppliers );

		$pdf = new GraphReport ( 'P', 'mm', 'A4', true, 'UTF-8', false );
		$pdf->SetCreator ( 'TCPDF' );
		$pdf->SetAuthor ( 'GASdotto' );
		$pdf->SetTitle ( 'Statistiche Utenti/Fornitori' );
		$pdf->SetSubject ( 'Statistiche Utenti/Fornitori' );
		$pdf->SetKeywords ( 'statistiche, fornitori, GASdotto, GAS' );
		$pdf->SetHeaderData ( '', 0, 'Statistiche Utenti/Fornitori' );
		$pdf->setHeaderFont ( Array ( 'helvetica', '', 10 ) );
		$pdf->setFooterFont ( Array ( 'helvetica', '', 8 ) );
		$pdf->SetDefaultMonospacedFont ( 'courier' );
		$pdf->SetMargins ( 15, 27, 25, 15 );
		$pdf->SetHeaderMargin ( 5 );
		$pdf->SetFooterMargin ( 10 );
		$pdf->SetAutoPageBreak ( true, 25 );
		$pdf->setImageScale ( 1 );
		$pdf->ColoredTable ( $header, $data );
		$pdf->Output ( 'statistiche_utenti_fornitori.pdf', 'I' );
	}
	else if ( $document == 'visual' ) {
		$ret = new stdClass ();

		for ( $i = 0; $i < count ( $rows_suppliers ); $i++ ) {
			$query = sprintf ( "SELECT COUNT(DISTINCT(OrderUser.baseuser)) FROM OrderUser, Orders
						WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
							$rows_suppliers [ $i ] [ "id" ], $startdate, $enddate );
			$returned = query_and_check ( $query, "Impossibile recuperare numero ordini" );
			$tot = $returned->fetchAll ( PDO::FETCH_NUM );
			unset ( $query );
			unset ( $returned );

			$query = sprintf ( "SELECT SUM(Product.unit_price * ProductUser.quantity) FROM OrderUser, Orders, OrderUser_products, ProductUser, Product
						WHERE OrderUser.baseorder = Orders.id AND Orders.supplier = %d AND OrderUser_products.parent = OrderUser.id AND ProductUser.id = OrderUser_products.target AND Product.id = ProductUser.product AND Orders.startdate > '%s' AND Orders.enddate < '%s'",
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

			$array [] = array ( $rows_suppliers [ $i ] [ "name" ], $t, $p );

			unset ( $tot );
			unset ( $price );
		}

		$ret->data = $array;
		$json = new Services_JSON ();
		echo $json->encode ( $ret ) . "\n";
	}
}

?>
