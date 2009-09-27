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

$products_names = "";
$products_prices = "";
for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$products_names .= sprintf ( ",\"%s\"", $prod->getAttribute ( "name" )->value );
	$products_prices .= sprintf ( ",%.02f €", $prod->getTotalPrice () );
}

$output = $products_names . "\n" . $products_prices . "\n";

$products_sums = array ();
for ( $i = 0; $i < count ( $product ); $i++ )
    $products_sums [] = 0;

$request = new stdClass ();
$request->order = $id;
$order_user_proxy = new OrderUser ();
$contents = $order_user_proxy->get ( $request );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$output .= sprintf ( "\"%s %s\",", $order_user->baseuser->firstname, $order_user->baseuser->surname );

	$user_total = 0;
	$user_products = $order_user->products;
	usort ( $user_products, "sort_product_user_by_name" );

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product->id ) {
			$output .= sprintf ( "%d,", $prod_user->quantity );
			$sum = $prod_user->quantity * $prod->getTotalPrice ();
			$products_sums [ $a ] += $sum;
			$user_total += $sum;
			$e++;
		}
		else
			$output .= sprintf ( "," );
	}

	$output .= sprintf ( "%.02f €\n", round ( $user_total, 2 ) );
}

for ( $i = 0; $i < count ( $products_sums ); $i++ )
    $output .= sprintf ( ",%.02f €", round ( $products_sums [ $i ], 2 ) );

header("Content-Type: plain/text");
header('Content-Disposition: inline; filename="' . 'consegne_' . $supplier_name . '_' . $shipping_date . '.csv' . '";');
echo $output;

function sort_product_by_name ( $first, $second ) {
	return strcmp ( $first->getAttribute ( "name" )->value, $second->getAttribute ( "name" )->value );
}

function sort_product_user_by_name ( $first, $second ) {
	return strcmp ( $first->product->name, $second->product->name );
}
