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
$order = $order->exportable ();

$products = $order->products;
usort ( $products, "sort_product_by_name" );

$output = ",";
for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$output .= sprintf ( "%s,", $prod->name );
}
$output .= "\n";

$request = new stdClass ();
$request->order = $id;
$order_user_proxy = new OrderUser ();
$contents = $order_user_proxy->get ( $request );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$output .= sprintf ( "%s %s,", $order_user->baseuser->firstname, $order_user->baseuser->surname );

	$user_products = $order_user->products;
	usort ( $user_products, "sort_product_user_by_name" );

	for ( $a = 0, $e = 0; $a < count ( $products ) && $e < count ( $user_products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->id == $prod_user->product->id ) {
			$output .= sprintf ( "%d,", $prod_user->quantity );
			$e++;
		}
		else
			$output .= sprintf ( "," );
	}

	$output .= "\n";
}

echo $output;

function sort_product_by_name ( $first, $second ) {
	return strcmp ( $first->name, $second->name );
}

function sort_product_user_by_name ( $first, $second ) {
	return strcmp ( $first->product->name, $second->product->name );
}
