<?php

/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class DeliveryReport extends TCPDF {
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

/*
	First checks
*/

$id = $_GET [ 'id' ];
if ( isset ( $id ) == false )
	error_exit ( "Richiesta non specificata" );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

/*
	Questo serve a sapere se dovro' aggiungere la riga con il riassunto delle confezioni
	complete o meno al fondo del file
*/
$has_stocks = false;

$order = new Order ();
$order->readFromDB ( $id );
$supplier = $order->getAttribute ( 'supplier' )->value;
$supplier_name = $supplier->getAttribute ( 'name' )->value;
$shipping_date = $order->getAttribute ( 'shippingdate' )->value;

/*
	Init headers
*/

$products = $order->getAttribute ( "products" )->value;
usort ( $products, "sort_product_by_name" );

$header = array ( 'Utenti' );

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$name = $prod->getAttribute ( "name" )->value;
	$price = format_price ( $prod->getAttribute ( "unit_price" )->value );

	$measure = $prod->getAttribute ( "measure" )->value;
	if ( $measure != null )
		$symbol = " / " . $measure->getAttribute ( "name" )->value;
	else
		$symbol = "";

	array_push ( $header, $name . "<br />(" . $price . $symbol . ")" );

	if ( $prod->getAttribute ( "stock_size" )->value > 0 )
		$has_stocks = true;
}

array_push ( $header, 'Totale Prezzo Prodotti' );
array_push ( $header, 'Totale Prezzo Trasporto' );
array_push ( $header, 'Totale' );

if ( $_GET [ 'type' ] == 'saved' )
	array_push ( $header, 'Prezzato' );
else
	array_push ( $header, 'Totale Pagato' );

array_push ( $header, 'Stato Consegna' );
array_push ( $header, 'Data' );
array_push ( $header, 'Referente' );
array_push ( $header, 'Utenti' );

/*
	Format data
*/

$data = array ();

$products_sums = array_fill ( 0, count ( $products ), 0 );
$quantities_sums = array_fill ( 0, count ( $products ), 0 );
$delivery_sums = array_fill ( 0, count ( $products ), 0 );
$shipped_sums = array_fill ( 0, count ( $products ), 0 );
$shipping_price = array_fill ( 0, count ( $products ), 0 );
$shipped_sums_by_date = array ();

$contents = get_orderuser_by_order ( $order );
usort ( $contents, "sort_orders_by_user_and_date" );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$row = array ();
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;

	if ( is_array ( $user_products ) == false ) {
		if ( is_array ( $order_user->friends ) == false )
			continue;
	}
	else {
		$user_products = sort_products_on_products ( $products, $user_products );
	}

	$user_total = 0;
	$user_total_ship = 0;
	$shipped_total = 0;

	$surname = ellipse_string ( $order_user->baseuser->surname, 12 );
	$firstname = ellipse_string ( $order_user->baseuser->firstname, 12 );

	$user_name = sprintf ( "%s<br />%s", $surname, $firstname );
	$row [] = $user_name;

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prodid = $prod->getAttribute ( 'id' )->value;
		$quantity = 0;
		$delivered = 0;

		if ( is_array ( $user_products ) ) {
			$prod_user = $user_products [ $e ];

			if ( $prodid == $prod_user->product ) {
				$quantity = $prod_user->quantity;
				$delivered = $prod_user->delivered;
				$e++;
			}
		}

		if ( count ( $order_user->friends ) != 0 ) {
			foreach ( $order_user->friends as $friend ) {
				foreach ( $friend->products as $fprod ) {
					if ( $fprod->product == $prodid ) {
						$quantity += $fprod->quantity;
						$delivered += $fprod->delivered;
						break;
					}
				}
			}
		}

		if ( $quantity != 0 ) {
			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			if ( $unit <= 0.0 )
				$q = $quantity;
			else
				$q = round ( $quantity / $unit );

			$q = comma_format ( $q );

			if ( $delivered != 0 ) {
				$d = comma_format ( $delivered );
				$q .= ' ( ' . $d . ' )';
				$quantity = $delivered;
			}

			$row [] = $q . '<br />';

			$sum = ( $quantity * $sprice );
			$shipping_price [ $a ] += $sum;
			$user_total_ship += $sum;

			$sum = $quantity * $uprice;
			$products_sums [ $a ] += $sum;
			$user_total += $sum;

			if ( ( $_GET [ 'type' ] == 'saved' && $order_user->status == 3 ) || ( $_GET [ 'type' ] != 'saved' && $order_user->status != 3 ) ) {
				$sum = ( $delivered * $uprice ) + ( $delivered * $sprice );
				$shipped_sums [ $a ] += $sum;
				$shipped_total += $sum;

				if ( $order_user->deliverydate != null ) {
					if ( isset ( $shipped_sums_by_date [ $order_user->deliverydate ] ) == false ) {
						$arr = array_fill ( 0, count ( $products ), 0 );
						$shipped_sums_by_date [ $order_user->deliverydate ] = $arr;
					}

					$shipped_sums_by_date [ $order_user->deliverydate ] [ $a ] += $sum;
				}
			}

			$quantities_sums [ $a ] += $quantity;
			$delivery_sums [ $a ] += $delivered;
		}
		else {
			$row [] = '<br />';
		}

		unset ( $prod_user );
	}

	$row [] = ( format_price ( round ( $user_total, 2 ), false ) ) . '<br />';
	$row [] = ( format_price ( round ( $user_total_ship, 2 ), false ) ) . '<br />';
	$row [] = ( format_price ( round ( $user_total + $user_total_ship, 2 ), false ) ) . '<br />';
	$row [] = ( format_price ( round ( $shipped_total, 2 ), false ) ) . '<br />';

	if ( $order_user->status == 1 )
		$row [] = 'Parzialmente Consegnato<br />';
	else if ( $order_user->status == 2 )
		$row [] = 'Consegnato<br />';
	else if ( $order_user->status == 3 )
		$row [] = 'Prezzato<br />';
	else
		$row [] = '<br />';

	if ( $order_user->deliverydate != null && $order_user->deliverydate != '' )
		$row [] = format_date ( $order_user->deliverydate );
	else
		$row [] = '';

	$reference = $order_user->deliveryperson;
	if ( property_exists ( $reference, 'surname' ) )
		$row [] = sprintf ( "%s<br />%s", $reference->surname, $reference->firstname );
	else
		$row [] = '';

	/*
		Il nome dell'utente viene messo sia all'inizio che alla fine della riga per
		facilitare la lettura. Richiesto collateralmente a bug #146
	*/
	$row [] = $user_name;

	$data [] = $row;

	unset ( $user_products );
	unset ( $order_user );
}

unset ( $contents );

$row = array ();
$row [] = 'Quantita\' Totali';
for ( $i = 0; $i < count ( $quantities_sums ); $i++ ) {
	$qs = $quantities_sums [ $i ];
	$q = comma_format ( $qs );

	$ds = $delivery_sums [ $i ];
	if ( $ds != 0 ) {
		$d = comma_format ( $ds );
		$q .= ' ( ' . $d . ' )';
	}

	$row [] = $q . '<br />';
}
$data [] = $row;

if ( $has_stocks == true ) {
	$row = array ();
	$row [] = 'Numero Confezioni<br />';

	for ( $i = 0; $i < count ( $quantities_sums ); $i++ ) {
		$prod = $products [ $i ];
		$stock = $prod->getAttribute ( "stock_size" )->value;

		if ( $stock <= 0.0 ) {
			$row [] = '<br />';
		}
		else {
			$quantity = $quantities_sums [ $i ];
			$boxes = ceil ( $quantity / $stock );
			$q = $boxes . ' confezioni da ' . $stock;

			$missing = ( $stock * $boxes ) - $quantity;
			if ( $missing > 0 )
				$q .= ',<br />' . $missing . ' non assegnati';

			$row [] = $q . '<br />';
		}
	}

	$data [] = $row;
}

unset ( $products );

$gran_total = 0;
$row = array ();
$row [] = 'Totale Prezzo Prodotti';
foreach ( $products_sums as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . '<br />';
	$gran_total += $r;
}
$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . '<br />';
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = 'Totale Prezzo Trasporto';
foreach ( $shipping_price as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . '<br />';
	$gran_total += $r;
}
$row [] = '<br />';
$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . '<br />';
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = 'Totale<br />';
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	$ps = $products_sums [ $i ] + $shipping_price [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . '<br />';
	$gran_total += $r;
}
$row [] = '<br />';
$row [] = '<br />';
$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . '<br />';
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = 'Totale Pagato<br />';
foreach ( $shipped_sums as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . '<br />';
	$gran_total += $r;
}
$row [] = '<br />';
$row [] = '<br />';
$row [] = '<br />';
$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . '<br />';
$data [] = $row;

if ( count ( $shipped_sums_by_date ) > 1 ) {
	foreach ( $shipped_sums_by_date as $date => $values ) {
		$gran_total = 0;
		$row = array ();
		$row [] = 'Totale Pagato<br />il' . format_date ( $date );

		foreach ( $values as $ps ) {
			$r = round ( $ps, 2 );
			$p = format_price ( $r, false );
			$row [] = $p . '<br />';
			$gran_total += $r;
		}

		$row [] = '<br />';
		$row [] = '<br />';
		$row [] = '<br />';
		$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . '<br />';
		$data [] = $row;
	}
}

/*
	Output
*/

$pdf = new DeliveryReport ( 'P', 'mm', 'A4', true, 'UTF-8', false );
$pdf->SetCreator ( 'TCPDF' );
$pdf->SetAuthor ( 'GASdotto' );
$pdf->SetTitle ( 'Consegne ordine a ' . $supplier_name . ' del ' . $shipping_date );
$pdf->SetSubject ( 'Consegne ordine a ' . $supplier_name . ' del ' . $shipping_date );
$pdf->SetKeywords ( 'consegne, ordini, GASdotto, GAS, ' . $supplier_name );
$pdf->SetHeaderData ( '', 0, 'Consegne ordine a ' . $supplier_name . ' del ' . $shipping_date, '' );
$pdf->setHeaderFont ( Array ( 'helvetica', '', 10 ) );
$pdf->setFooterFont ( Array ( 'helvetica', '', 8 ) );
$pdf->SetDefaultMonospacedFont ( 'courier' );
$pdf->SetMargins ( 15, 27, 25, 15 );
$pdf->SetHeaderMargin ( 5 );
$pdf->SetFooterMargin ( 10 );
$pdf->SetAutoPageBreak ( true, 25 );
$pdf->setImageScale ( 1 );
$pdf->ColoredTable ( $header, $data );
$pdf->Output ( 'consegne_' . $supplier_name . '_' . $shipping_date . '.pdf', 'I' );

?>
