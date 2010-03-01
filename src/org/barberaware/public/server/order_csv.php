<?

/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

$products_names [] = "Totale Prezzo Prodotti";
$products_names [] = "Totale Prezzo Trasporto";
$products_names [] = "Totale";
$products_names [] = "Pagato";
$products_names [] = "Stato Consegna";
$output = ";" . join ( ";", $products_names ) . "\n;" . join ( ";", $products_prices ) . "\n\n";

$products_sums = array ();
$quantities_sums = array ();
$delivery_sums = array ();
$shipped_sums = array ();
$shipping_price = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$products_sums [] = 0;
	$quantities_sums [] = 0;
	$delivery_sums [] = 0;
	$shipped_sums [] = 0;
	$shipping_price [] = 0;
}

$request = new stdClass ();
$request->baseorder = $id;

/*
	Questo e' per evitare che lo script ricarichi per intero l'ordine di riferimento e tutti
	i prodotti per ogni singolo OrderUser
*/
$request->has_Order = array ( $id );
$request->has_Product = array ();
for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$request->has_Product [] = $prod->getAttribute ( "id" )->value;
}

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
	$user_total_ship = 0;
	$shipped_total = 0;

	/*
		Devo ordinare l'array di ProductUser per nome del prodotto, in modo da essere
		allineato all'array di Products, ma i ProductUser non contengono il nome. Percui
		mi tocca ordinarlo prendendo come traccia l'array di Products, e ricostruirlo da
		un'altra parte
	*/

	$proxy = array ();

	for ( $e = 0; $e < count ( $products ); $e++ ) {
		$prod = $products [ $e ];

		for ( $a = 0; $a < count ( $user_products ); $a++ ) {
			$prod_user = $user_products [ $a ];

			if ( $prod->getAttribute ( "id" )->value == $prod_user->product ) {
				$proxy [] = $prod_user;
				break;
			}
		}
	}

	unset ( $user_products );
	$user_products = $proxy;

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product ) {
			/*
				Se l'ordine e' stato solo salvato e non consegnato per davvero (status == 3) ignoro
				le quantita' marcate come consegnate. Semplicemente azzero qui la variabile relativa
				onde evitare di piazzara if() qua e la'
			*/
			if ( $order_user->status == 3 )
				$prod_user->delivered = 0;

			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			if ( $unit <= 0.0 )
				$q = $prod_user->quantity;
			else
				$q = ( $prod_user->quantity / $unit );

			$q = comma_format ( $q );

			if ( $prod_user->delivered != 0 ) {
				$d = comma_format ( $prod_user->delivered );
				$q .= ' ( ' . $d . ' )';
			}

			$output .= $q . ";";

			$sum = $prod_user->quantity * $uprice;
			$products_sums [ $a ] += $sum;
			$user_total += $sum;

			$sum = ( $prod_user->quantity * $sprice );
			$shipping_price [ $a ] += $sum;
			$user_total_ship += $sum;

			$sum = ( $prod_user->delivered * $uprice ) + ( $prod_user->delivered * $sprice );
			$shipped_sums [ $a ] += $sum;
			$shipped_total += $sum;

			$quantities_sums [ $a ] += $prod_user->quantity;
			$delivery_sums [ $a ] += $prod_user->delivered;

			$e++;
		}
		else
			$output .= sprintf ( ";" );
	}

	$output .= format_price ( round ( $user_total, 2 ), false ) . ';';
	$output .= format_price ( round ( $user_total_ship, 2 ), false ) . ';';
	$output .= format_price ( round ( $user_total + $user_total_ship, 2 ), false ) . ';';
	$output .= format_price ( round ( $shipped_total, 2 ), false ) . ';';

	if ( $order_user->status == 1 )
		$output .= 'Parzialmente Consegnato';
	else if ( $order_user->status == 2 )
		$output .= 'Consegnato';

	$output .= "\n";
}

$output .= "\n";

$output .= "Quantita' Totali";
for ( $i = 0; $i < count ( $quantities_sums ); $i++ ) {
	$qs = $quantities_sums [ $i ];
	$q = comma_format ( $qs );

	$ds = $delivery_sums [ $i ];
	if ( $ds != 0 ) {
		$d = comma_format ( $ds );
		$q .= ' ( ' . $d . ' )';
	}

	$output .= ";" . $q;
}
$output .= "\n";

$gran_total = 0;
$output .= "Totale Prezzo Prodotti";
foreach ( $products_sums as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$output .= ";" . $p;
	$gran_total += $r;
}
$output .= ";" . format_price ( round ( $gran_total, 2 ), false ) . "\n";

$gran_total = 0;
$output .= "Totale Prezzo Trasporto";
foreach ( $shipping_price as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$output .= ";" . $p;
	$gran_total += $r;
}
$output .= ";;" . format_price ( round ( $gran_total, 2 ), false ) . "\n";

$gran_total = 0;
$output .= "Totale Prodotti + Trasporto";
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	$ps = $products_sums [ $i ] + $shipping_price [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$output .= ";" . $p;
	$gran_total += $r;
}
$output .= ";;;" . format_price ( round ( $gran_total, 2 ), false ) . "\n";

$gran_total = 0;
$output .= "Totale Pagato";
foreach ( $shipped_sums as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$output .= ";" . $p;
	$gran_total += $r;
}
$output .= ";;;;" . format_price ( round ( $gran_total, 2 ), false ) . "\n";

header ( "Content-Type: plain/text" );
header ( 'Content-Disposition: inline; filename="' . 'consegne_' . $supplier_name . '_' . $shipping_date . '.csv' . '";' );
echo $output;

?>
