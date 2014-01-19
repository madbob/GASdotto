<?php

/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

function format_order ( $order, $is_friend ) {
	global $products;
	global $products_sums;
	global $quantities_sums;
	global $shipping_price;
	global $existing_products;

	global $string_begin;
	global $string_end;
	global $content_sep;
	global $onelinepadding;
	global $emptycell;

	$user_products = $order->products;

	if ( is_array ( $user_products ) == false )
		return null;
	else
		$user_products = sort_products_on_products ( $products, $user_products );

	$row = array ();

	$user_total = 0;
	$user_total_ship = 0;
	$shipped_total = 0;

	if ( $is_friend )
		$row [] = sprintf ( "$string_begin%s$string_end", $order->friendname );
	else
		$row [] = sprintf ( "$string_begin%s %s$string_end", $order->baseuser->surname, $order->baseuser->firstname );

	for ( $a = 0, $e = 0; $a < count ( $products ) && $e < count ( $user_products ); $a++ ) {
		if ( $existing_products [ $a ] == false )
			continue;

		$prod = $products [ $a ];

		$prodid = $prod->getAttribute ( 'id' )->value;
		$quantity = 0;

		$prod_user = $user_products [ $e ];

		if ( $prodid == $prod_user->product ) {
			$quantity = $prod_user->quantity;
			$e++;
		}

		if ( $quantity != 0 ) {
			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			$vars = '';

			if ( $unit <= 0.0 ) {
				$q = $quantity;

				if ( property_exists ( $prod_user, 'variants' ) && is_array ( $prod_user->variants ) ) {
					list ( $variants, $quantities ) = aggregate_variants ( $prod_user->variants );

					for ( $j = 0; $j < count ( $variants ); $j++ )
						$vars .= $content_sep . ( $quantities [ $j ] ) . ' ' . ( $variants [ $j ] );
				}
			}
			else {
				$q = round ( $quantity / $unit );
			}

			$q = comma_format ( $q );
			$row [] = $q . $vars;

			$sum = $quantity * $uprice;
			$products_sums [ $a ] += $sum;
			$user_total += $sum;

			$sum = ( $quantity * $sprice );
			$shipping_price [ $a ] += $sum;
			$user_total_ship += $sum;

			$quantities_sums [ $a ] += $quantity;
		}
		else {
			$row [] = $emptycell;
		}
	}

	$row [] = format_price ( round ( $user_total, 2 ), false ) . $onelinepadding;
	$row [] = format_price ( round ( $user_total_ship, 2 ), false ) . $onelinepadding;
	$row [] = format_price ( round ( $user_total + $user_total_ship, 2 ), false ) . $onelinepadding;

	return $row;
}

function search_existing ( $order ) {
	global $products;
	global $existing_products;

	$user_products = $order->products;

	if ( is_array ( $user_products ) == false )
		return;
	else
		$user_products = sort_products_on_products ( $products, $user_products );

	for ( $a = 0, $e = 0; $a < count ( $products ) && $e < count ( $user_products ); $a++ ) {
		$prod = $products [ $a ];

		$prodid = $prod->getAttribute ( 'id' )->value;
		$prod_user = $user_products [ $e ];

		if ( $prodid == $prod_user->product ) {
			if ( $prod_user->quantity > 0 )
				$existing_products [ $a ] = true;

			$e++;
		}
	}
}

global $products;
global $products_sums;
global $quantities_sums;
global $shipping_price;
global $existing_products;

global $string_begin;
global $string_end;
global $onelinepadding;
global $emptycell;

$id = require_param ( 'id' );
$format = require_param ( 'format' );
$user = require_param ( 'user' );
$is_aggregate = get_param ( 'aggregate', false );

formatting_entities ( $format );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

list ( $orders, $supplier_name, $supplier_ships, $shipping_date ) = details_about_order ( $id, $is_aggregate );

$all_contents = array ();
$all_products = array ();

foreach ( $orders as $order ) {
	$contents = get_orderuser_by_order ( $order, $user );
	if ( count ( $contents ) == 0 )
		continue;

	$all_contents = merge_order_users ( $all_contents, $contents, true );

	$products = $order->getAttribute ( "products" )->value;
	usort ( $products, "sort_product_by_name" );
	$all_products = array_merge ( $all_products, $products );
}

/*
	n.b.: in search_existing() si assume che la variabile $products contenga
	tutti i prodotti da contemplate
*/
$products = $all_products;

$order_user = $all_contents [ 0 ];

$data = array ();

$products_sums = array_fill ( 0, count ( $all_products ), 0 );
$quantities_sums = array_fill ( 0, count ( $all_products ), 0 );
$shipping_price = array_fill ( 0, count ( $all_products ), 0 );
$existing_products = array_fill ( 0, count ( $all_products ), false );

/*
	Verifico quali prodotti sono contemplati tra tutti gli
	ordini. Serve a sapere quali casella saltare in toto e
	quali lasciare in bianco per incolonnare il tutto
	(bug #189: contemplare solo i prodotti ordinati)
*/

search_existing ( $order_user );

if ( property_exists ( $order_user, 'friends' ) ) {
	$contents = $order_user->friends;
	usort ( $contents, "sort_friend_orders_by_user" );

	for ( $i = 0; $i < count ( $contents ); $i++ ) {
		$order_friend = $contents [ $i ];
		search_existing ( $order_friend );
	}
}

$row = format_order ( $order_user, false );
if ( $row != null )
	$data [] = $row;

if ( property_exists ( $order_user, 'friends' ) ) {
	$contents = $order_user->friends;
	usort ( $contents, "sort_friend_orders_by_user" );

	for ( $i = 0; $i < count ( $contents ); $i++ ) {
		$order_friend = $contents [ $i ];

		$row = format_order ( $order_friend, true );
		if ( $row != null )
			$data [] = $row;
	}
}

/*
	Creo l'header, includendo solo i prodotti che sono stati ordinati da qualcuno
*/

$headers = array ();
$headers [] = $emptycell;

for ( $i = 0; $i < count ( $all_products ); $i++ ) {
	if ( $existing_products [ $i ] == false )
		continue;

	$prod = $all_products [ $i ];
	$name = sprintf ( "$string_begin%s$string_end", $prod->getAttribute ( "name" )->value );

	$measure = $prod->getAttribute ( "measure" )->value;
	if ( $measure != null )
		$symbol = " / " . $measure->getAttribute ( "name" )->value;
	else
		$symbol = "";

	$price = ( format_price ( $prod->getAttribute ( "unit_price" )->value, false ) ) . $symbol;
	$headers [] = sprintf ( "%s%s%s", $name, $content_sep, $price );
}

$headers [] = "Totale Prezzo Prodotti";
$headers [] = "Totale Prezzo Trasporto";
$headers [] = "Totale";

/*
	Somme complessive
*/

$row = array ();
$row [] = "Quantita' Totali";
for ( $i = 0; $i < count ( $quantities_sums ); $i++ ) {
	if ( $existing_products [ $i ] == false )
		continue;

	$qs = $quantities_sums [ $i ];
	$q = comma_format ( $qs );
	$row [] = $q . $onelinepadding;
}
$data [] = $row;

$row = array ();
$gran_total = 0;
$row [] = "Totale Prezzo Prodotti";
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	if ( $existing_products [ $i ] == false )
		continue;

	$ps = $products_sums [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . $onelinepadding;
	$gran_total += $r;
}
$row [] = format_price ( round ( $gran_total, 2 ), false ) . $onelinepadding;
$data [] = $row;

$row = array ();
$gran_total = 0;
$row [] = "Totale Prezzo Trasporto";
for ( $i = 0; $i < count ( $shipping_price ); $i++ ) {
	if ( $existing_products [ $i ] == false )
		continue;

	$ps = $shipping_price [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . $onelinepadding;
	$gran_total += $r;
}
$row [] = $emptycell;
$row [] = format_price ( round ( $gran_total, 2 ), false ) . $onelinepadding;
$data [] = $row;

$row = array ();
$gran_total = 0;
$row [] = "Totale Prodotti + Trasporto";
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	if ( $existing_products [ $i ] == false )
		continue;

	$ps = $products_sums [ $i ] + $shipping_price [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p . $onelinepadding;
	$gran_total += $r;
}
$row [] = $emptycell;
$row [] = $emptycell;
$row [] = format_price ( round ( $gran_total, 2 ), false ) . $onelinepadding;
$data [] = $row;

output_formatted_document ( 'Ordine per ' . $order->getAttribute ( 'supplier' )->value->getAttribute ( 'name' )->value, $headers, $data, $format );

?>
