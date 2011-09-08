<?php

/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

global $string_begin;
global $string_end;
global $content_sep;
global $double_line_sep;
global $double_line_end;
global $onelinepadding;
global $emptycell;

function total_row ( $title, $values, $empty ) {
	global $double_line_end;
	global $emptycell;

	$gran_total = 0;
	$row = array ();
	$row [] = $title;

	foreach ( $values as $ps ) {
		$r = round ( $ps, 2 );
		$p = format_price ( $r, false );
		$row [] = $p . $double_line_end;
		$gran_total += $r;
	}

	for ( $i = 0; $i < $empty; $i++ )
		$row [] = $emptycell;

	$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . $double_line_end;
	return $row;
}

$id = require_param ( 'id' );
$format = require_param ( 'format' );
$is_aggregate = get_param ( 'aggregate', false );

formatting_entities ( $format );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

list ( $orders, $supplier_name, $shipping_date ) = details_about_order ( $id, $is_aggregate );

/*
	Init headers
*/

$headers = array ( 'Utenti' );
$tot_prod_num = 0;
$all_products = array ();

foreach ( $orders as $order ) {
	$products = $order->getAttribute ( "products" )->value;
	usort ( $products, "sort_product_by_name" );
	$prod_num = count ( $products );

	for ( $i = 0; $i < $prod_num; $i++ ) {
		$prod = $products [ $i ];
		$name = $prod->getAttribute ( "name" )->value;
		$price = format_price ( $prod->getAttribute ( "unit_price" )->value );

		$measure = $prod->getAttribute ( "measure" )->value;
		if ( $measure != null )
			$symbol = " / " . $measure->getAttribute ( "name" )->value;
		else
			$symbol = "";

		array_push ( $headers, $name . $content_sep . "(" . $price . $symbol . ")" );

		if ( $prod->getAttribute ( "stock_size" )->value > 0 )
			$has_stocks = true;
	}

	$tot_prod_num += $prod_num;
	$all_products = array_merge ( $all_products, $products );
}

array_push ( $headers, 'Totale Prezzo Prodotti' );
array_push ( $headers, 'Totale Prezzo Trasporto' );
array_push ( $headers, 'Totale' );

if ( $_GET [ 'type' ] == 'saved' )
	array_push ( $headers, 'Prezzato' );
else
	array_push ( $headers, 'Totale Pagato' );

array_push ( $headers, 'Stato Consegna' );
array_push ( $headers, 'Data' );
array_push ( $headers, 'Referente' );
array_push ( $headers, 'Utenti' );

/*
	Format data
*/

$data = array ();

$products_sums = array_fill ( 0, $tot_prod_num, 0 );
$quantities_sums = array_fill ( 0, $tot_prod_num, 0 );
$delivery_sums = array_fill ( 0, $tot_prod_num, 0 );
$shipped_sums = array_fill ( 0, $tot_prod_num, 0 );
$shipping_price = array_fill ( 0, $tot_prod_num, 0 );
$shipped_sums_by_date = array ();
$all_contents = array ();

foreach ( $orders as $order ) {
	$contents = get_orderuser_by_order ( $order );
	$all_contents = merge_order_users ( $all_contents, $contents );
}

usort ( $all_contents, "sort_orders_by_user_and_date" );

for ( $i = 0; $i < count ( $all_contents ); $i++ ) {
	$row = array ();
	$order_user = $all_contents [ $i ];
	$user_products = $order_user->products;

	if ( is_array ( $user_products ) == false ) {
		if ( is_array ( $order_user->friends ) == false )
			continue;
	}
	else {
		$user_products = sort_products_on_products ( $all_products, $user_products );
	}

	$user_total = 0;
	$user_total_ship = 0;
	$shipped_total = 0;

	$surname = ellipse_string ( $order_user->baseuser->surname, 12 );
	$firstname = ellipse_string ( $order_user->baseuser->firstname, 12 );

	$user_name = sprintf ( "%s%s%s", $surname, $double_line_sep, $firstname );
	$row [] = $user_name;

	for ( $a = 0, $e = 0; $a < count ( $all_products ); $a++ ) {
		$prod = $all_products [ $a ];
		$prodid = $prod->getAttribute ( 'id' )->value;
		$quantity = 0;
		$delivered = 0;

		if ( is_array ( $user_products ) && $e < count ( $user_products ) ) {
			$prod_user = $user_products [ $e ];

			if ( $prodid == $prod_user->product ) {
				$quantity = $prod_user->quantity;
				$delivered = $prod_user->delivered;
				$e++;
			}
		}

		if ( property_exists ( $order_user, "friends" ) && count ( $order_user->friends ) != 0 ) {
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

			$row [] = $q . $double_line_end;

			$sum = ( $quantity * $sprice );
			$shipping_price [ $a ] += $sum;
			$user_total_ship += $sum;

			$sum = $quantity * $uprice;
			$products_sums [ $a ] += $sum;
			$user_total += $sum;

			if ( ( $_GET [ 'type' ] == 'saved' && property_exists ( $order_user, 'status' ) && $order_user->status == 3 ) ||
					( $_GET [ 'type' ] != 'saved' && ( property_exists ( $order_user, 'status' ) == false || $order_user->status != 3 ) ) ) {
				$sum = ( $delivered * $uprice ) + ( $delivered * $sprice );
				$shipped_sums [ $a ] += $sum;
				$shipped_total += $sum;

				if ( property_exists ( $order_user, 'deliverydate' ) ) {
					if ( isset ( $shipped_sums_by_date [ $order_user->deliverydate ] ) == false ) {
						$arr = array_fill ( 0, $tot_prod_num, 0 );
						$shipped_sums_by_date [ $order_user->deliverydate ] = $arr;
					}

					$shipped_sums_by_date [ $order_user->deliverydate ] [ $a ] += $sum;
				}
			}

			$quantities_sums [ $a ] += $quantity;
			$delivery_sums [ $a ] += $delivered;
		}
		else {
			$row [] = $emptycell . $emptycell;
		}

		unset ( $prod_user );
	}

	$row [] = ( format_price ( round ( $user_total, 2 ), false ) ) . $double_line_end;
	$row [] = ( format_price ( round ( $user_total_ship, 2 ), false ) ) . $double_line_end;
	$row [] = ( format_price ( round ( $user_total + $user_total_ship, 2 ), false ) ) . $double_line_end;
	$row [] = ( format_price ( round ( $shipped_total, 2 ), false ) ) . $double_line_end;

	if ( property_exists ( $order_user, 'status' ) ) {
		if ( $order_user->status == 1 )
			$row [] = 'Parzialmente Consegnato' . $double_line_end;
		else if ( $order_user->status == 2 )
			$row [] = 'Consegnato' . $double_line_end;
		else if ( $order_user->status == 3 )
			$row [] = 'Prezzato' . $double_line_end;
		else
			$row [] = $emptycell;
	}
	else {
		$row [] = $emptycell;
	}

	if ( property_exists ( $order_user, 'deliverydate' ) )
		$row [] = format_date ( $order_user->deliverydate );
	else
		$row [] = $emptycell;

	if ( property_exists ( $order_user, 'deliveryperson' ) ) {
		$reference = $order_user->deliveryperson;
		if ( property_exists ( $reference, 'surname' ) )
			$row [] = sprintf ( "%s%s%s", $reference->surname, $double_line_sep, $reference->firstname );
		else
			$row [] = $emptycell;
	}
	else {
		$row [] = $emptycell;
	}

	/*
		Il nome dell'utente viene messo sia all'inizio che alla fine della riga per
		facilitare la lettura. Richiesto collateralmente a bug #146
	*/
	$row [] = $user_name;

	$data [] = $row;

	unset ( $user_products );
	unset ( $order_user );
}

unset ( $all_contents );

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

	$row [] = $q . $double_line_end;
}
$data [] = $row;

if ( $has_stocks == true ) {
	$row = array ();
	$row [] = 'Numero Confezioni' . $double_line_end;

	for ( $i = 0; $i < count ( $quantities_sums ); $i++ ) {
		$prod = $all_products [ $i ];
		$stock = $prod->getAttribute ( "stock_size" )->value;

		if ( $stock <= 0.0 ) {
			$row [] = $emptycell;
		}
		else {
			$quantity = $quantities_sums [ $i ];
			$boxes = ceil ( $quantity / $stock );
			$q = $boxes . ' confezioni da ' . $stock;

			$missing = ( $stock * $boxes ) - $quantity;
			if ( $missing > 0 )
				$q .= ', ' . $double_line_sep . $missing . ' non assegnati';

			$row [] = $q . $double_line_end;
		}
	}

	$data [] = $row;
}

unset ( $all_products );

$data [] = total_row ( 'Totale Prezzo Prodotti', $products_sums, 0 );
$data [] = total_row ( 'Totale Prezzo Trasporto', $shipping_price, 1 );

$gran_total = 0;
$row = array ();
$row [] = 'Totale' . $double_line_end;
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	$ps = $products_sums [ $i ] + $shipping_price [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . $double_line_end;
	$gran_total += $r;
}
$row [] = $emptycell;
$row [] = $emptycell;
$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . $double_line_end;
$data [] = $row;

$data [] = total_row ( 'Totale Pagato', $shipped_sums, 3 );

if ( count ( $shipped_sums_by_date ) > 1 ) {
	foreach ( $shipped_sums_by_date as $date => $values ) {
		$gran_total = 0;
		$row = array ();
		$row [] = 'Totale Pagato' . $double_line_sep . 'il ' . format_date ( $date, true );

		foreach ( $values as $ps ) {
			$r = round ( $ps, 2 );
			$p = format_price ( $r, false );
			$row [] = $p . $double_line_end;
			$gran_total += $r;
		}

		$row [] = $emptycell;
		$row [] = $emptycell;
		$row [] = $emptycell;
		$row [] = ( format_price ( round ( $gran_total, 2 ), false ) ) . $double_line_end;
		$data [] = $row;
	}
}

output_formatted_document ( 'Ordine per ' . $supplier_name, $headers, $data, $format );

?>
