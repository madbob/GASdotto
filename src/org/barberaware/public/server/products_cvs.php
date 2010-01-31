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
	$products_prices [] = format_price ( $prod->getAttribute ( "unit_price" )->value, false );
}

$products_sums = array ();
$quantities_sums = array ();
$shipping_sum = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$products_sums [] = 0;
	$quantities_sums [] = 0;
	$shipping_sum [] = 0;
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

	$user_total = 0;
	usort ( $user_products, "sort_product_user_by_name" );

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product->id ) {
			$unit = $prod_user->product->unit_size;

			if ( $unit <= 0.0 )
				$q = $prod_user->quantity;
			else
				$q = ( $prod_user->quantity / $unit );

			$quantities_sums [ $a ] += $q;

			$sum = $q * $prod_user->product->unit_price;
			$products_sums [ $a ] += $sum;

			$sum = $q * $prod_user->product->shipping_price;
			$shipping_sum [ $a ] += $sum;

			$e++;
		}
	}
}

$output = "Prodotto;Quantità;Prezzo Totale;Prezzo Trasporto\n";

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$q = comma_format ( round ( $quantities_sums [ $i ], 2 ), false );
	$p = format_price ( round ( $products_sums [ $i ], 2 ), false );
	$s = format_price ( round ( $shipping_sum [ $i ], 2 ), false );
	$output .= ( $prod->getAttribute ( "name" )->value ) . ';' . $q . ';' . $p . ';' . $s . "\n";
}

header ( "Content-Type: plain/text" );
header ( 'Content-Disposition: inline; filename="' . 'ordinazioni_' . $supplier_name . '_' . $shipping_date . '.csv' . '";' );
echo $output;

?>
