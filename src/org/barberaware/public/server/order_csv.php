<?

/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

$id = $_GET [ 'id' ];
if ( isset ( $id ) == false )
	error_exit ( "Richiesta non specificata" );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$order = new Order ();
$order->readFromDB ( $id );

$supplier = $order->getAttribute ( 'supplier' )->value;
$supplier_name = $supplier->getAttribute ( 'name' )->value;
$shipping_date = $order->getAttribute ( 'shippingdate' )->value;

$products = $order->getAttribute ( "products" )->value;
usort ( $products, "sort_product_by_name" );

$products_names = array ();
$products_prices = array ();
for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$products_names [] = sprintf ( "\"%s\"", $prod->getAttribute ( "name" )->value );
	$products_prices [] = format_price ( $prod->getTotalPrice (), false );
}

$products_names [] = "Totale";
$output = ";" . join ( ";", $products_names ) . "\n;" . join ( ";", $products_prices ) . "\n\n";

$products_sums = array ();
$quantities_sums = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$products_sums [] = 0;
	$quantities_sums [] = 0;
}

$request = new stdClass ();
$request->baseorder = $id;
$order_user_proxy = new OrderUser ();
$contents = $order_user_proxy->get ( $request, false );
usort ( $contents, "sort_orders_by_user" );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$output .= sprintf ( "\"%s %s\";", $order_user->baseuser->surname, $order_user->baseuser->firstname );

	$user_total = 0;
	usort ( $user_products, "sort_product_user_by_name" );

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product->id ) {
			$decimal = strlen ( strstr ( $prod_user->quantity, '.' ) );
			if ( $decimal != 0 )
				$output .= number_format ( $prod_user->quantity, $decimal - 1, ',', '' );
			else
				$output .= sprintf ( "%d", $prod_user->quantity );

			$output .= ";";

			$sum = $prod_user->quantity * $prod->getTotalPrice ();
			$products_sums [ $a ] += $sum;
			$quantities_sums [ $a ] += $prod_user->quantity;
			$user_total += $sum;
			$e++;
		}
		else
			$output .= sprintf ( ";" );
	}

	$output .= format_price ( round ( $user_total, 2 ), false ) . "\n";
}

$output .= "\n";

$gran_total = 0;
$output .= "Totale Prezzo";
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	$r = round ( $products_sums [ $i ], 2 );
	$p = format_price ( $r, false );
	$output .= ";" . $p;
	$gran_total += $r;
}
$output .= ";" . format_price ( round ( $gran_total, 2 ), false ) . "\n";

$output .= "Quantita' Totali;" . join ( ";", $quantities_sums );

header ( "Content-Type: plain/text" );
header ( 'Content-Disposition: inline; filename="' . 'consegne_' . $supplier_name . '_' . $shipping_date . '.csv' . '";' );
echo $output;

function sort_product_by_name ( $first, $second ) {
	return strcmp ( $first->getAttribute ( "name" )->value, $second->getAttribute ( "name" )->value );
}

function sort_product_user_by_name ( $first, $second ) {
	return strcmp ( $first->product->name, $second->product->name );
}

function sort_orders_by_user ( $first, $second ) {
	return strcmp ( $first->baseuser->surname, $second->baseuser->surname );
}
