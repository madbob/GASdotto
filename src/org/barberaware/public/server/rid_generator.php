<?php

/*  GASdotto
 *  Copyright (C) 2011/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

function filler ( $length ) {
	$ret = '';

	for ( $i = 0; $i < $length; $i++ )
		$ret .= ' ';

	return $ret;
}

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$id = require_param ( 'id' );
$is_aggregate = get_param ( 'aggregate', false );

list ( $orders, $supplier_name, $supplier_ships, $shipping_date ) = details_about_order ( $id, $is_aggregate );

$all_products = array ();
$all_contents = array ();

foreach ( $orders as $order ) {
	$products = $order->getAttribute ( "products" )->value;
	usort ( $products, "sort_product_by_name" );

	$contents = get_orderuser_by_order ( $order );

	$all_products = array_merge ( $all_products, $products );
	$all_contents = merge_order_users ( $all_contents, $contents );
}

usort ( $all_contents, "sort_orders_by_user_and_date" );

$gas = current_gas ();
$ridconf = $gas->getAttribute ( 'rid_conf' )->value;
list ( $name, $account, $code ) = explode ( '::', $ridconf );
list ( $abi, $cab, $account ) = explode ( ' ', $account );

$stream_id = $code . $abi . date ( 'dmy' ) . strtoupper ( random_string ( 20 ) );
$output = ' IR' . $stream_id . filler ( 67 ) . '00000  E ' . $abi . "\n";

/*
	I pagamenti scadono dopo 30 giorni
*/
$expiry = date ( 'dmy', time () + ( 30 * 24 * 60 * 60 ) );

$gran_total = 0;
$rows = 1;
$block = 0;

for ( $i = 0; $i < count ( $all_contents ); $i++ ) {
	$order_user = $all_contents [ $i ];
	$user = $order_user->baseuser;

	/*
		Dal 22/02/2014 viene generate il RID in formato SEPA, che
		richiede l'IBAN completo degli utenti. Se nel database non
		vengono trovate tutte le informazioni, il record viene saltato
	*/
	if ( property_exists ( $user, 'bank_account' ) == false || $user->bank_account == '' || strlen ( $user->bank_account ) < 32 )
		continue;

	if ( property_exists ( $user, 'sepa_subscribe' ) == false )
		continue;

	if ( property_exists ( $user, 'first_sepa' ) == false || $user->first_sepa == null || $user->first_sepa == '' ) {
		$seq_id = 'FRST';
		$user->first_sepa = date ( 'Y-m-d' );

		$useless_user = new User ();
		$useless_user->save ( $user );
		unset ( $useless_user );
	}
	else {
		$seq_id = "RCUR";
	}

	list ( $y, $m, $d ) = explode ( '-', $user->sepa_subscribe );
	$subscribe_date = $d . $m . ( $y - 2000 );

	$bank_account = $user->bank_account;
	list ( $user_country, $user_checkdigit, $user_cin, $user_abi, $user_cab, $user_account ) = explode ( ' ', $bank_account );
	$user_iban = $user_country . $user_checkdigit . $user_cin . $user_abi . $user_cab . $user_account;

	$user_name = $user->surname . ' ' . $user->firstname;
	$user_address = $user->address;

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$user_products = sort_products_on_products ( $all_products, $user_products );
	$user_total = 0;

	for ( $a = 0, $e = 0; $a < count ( $all_products ); $a++ ) {
		$prod = $all_products [ $a ];
		$prodid = $prod->getAttribute ( 'id' )->value;
		$quantity = 0;

		if ( $e < count ( $user_products ) ) {
			$prod_user = $user_products [ $e ];

			if ( $prodid == $prod_user->product ) {
				$quantity = $prod_user->delivered;
				$e++;
			}
		}

		if ( property_exists ( $order_user, 'friends' ) && count ( $order_user->friends ) != 0 ) {
			foreach ( $order_user->friends as $friend ) {
				foreach ( $friend->products as $fprod ) {
					if ( $fprod->product == $prodid ) {
						$quantity += $fprod->delivered;
						break;
					}
				}
			}
		}

		if ( $quantity != 0 ) {
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			$quprice = ( $quantity * $uprice );
			$qsprice = ( $quantity * $sprice );

			$user_total += sum_percentage ( $quprice, $prod->getAttribute ( "surplus" )->value ) + $qsprice;
		}
	}

	$block++;

	$output .= ' 10' . sprintf ( '%07d', $block ) . filler ( 12 ) . $expiry . '50000' . sprintf ( '%013d', $user_total * 100 ) . '-' . $abi . $cab . $account . $user_abi . $user_cab . filler ( 12 ) . $code . '4' . str_pad ( $user->id, 16, '0', STR_PAD_LEFT ) . filler ( 6 ) . 'E' . "\n";
	$output .= ' 17' . sprintf ( '%07d', $block ) . str_pad ( strtoupper ( $user_iban ), 27 ) . $seq_id . $subscribe_date . filler ( 73 ) . "\n";
	$output .= ' 20' . sprintf ( '%07d', $block ) . str_pad ( strtoupper ( $name ), 110 ) . "\n";
	$output .= ' 30' . sprintf ( '%07d', $block ) . str_pad ( strtoupper ( $user_name ), 110 ) . "\n";
	$output .= ' 40' . sprintf ( '%07d', $block ) . str_pad ( strtoupper ( $user_address->street ), 30 ) . str_pad ( strtoupper ( $user_address->cap ), 5 ) . str_pad ( strtoupper ( $user_address->city ), 25 ) . filler ( 50 ) . "\n";
	$output .= ' 50' . sprintf ( '%07d', $block ) . str_pad ( 'PAGAMENTO ORDINE ' . strtoupper ( $supplier_name ), 110 ) . "\n";
	$output .= ' 70' . sprintf ( '%07d', $block ) . filler ( 110 ) . "\n";

	$gran_total += $user_total;
	$rows += 6;
}

$output .= ' EF' . $stream_id . filler ( 6 ) . sprintf ( '%07d', $block ) . sprintf ( '%015d', $gran_total * 100 ) . sprintf ( '%022d', $rows + 1 ) . filler ( 24 ) . 'E' . filler ( 6 ) . "\n";

header ( "Content-Type: plain/text" );
header ( 'Content-Disposition: inline; filename="' . 'RID_' . $supplier_name . '.txt' . '";' );
echo $output;

?>

