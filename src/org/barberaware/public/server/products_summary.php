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

function manage_product ( $prod, $prod_user, &$references, $a ) {
	$unit = $prod->getAttribute ( "unit_size" )->value;
	$uprice = $prod->getAttribute ( "unit_price" )->value;
	$sprice = $prod->getAttribute ( "shipping_price" )->value;

	if ( $unit <= 0.0 )
		$q = $prod_user->quantity;
	else
		$q = ( $prod_user->quantity / $unit );

	if ( is_array ( $references [ $a ] [ 1 ] ) == false ) {
		$references [ $a ] [ 3 ] += $q;

		$sum = $prod_user->quantity * $uprice;
		$references [ $a ] [ 4 ] += $sum;

		$sum = $prod_user->quantity * $sprice;
		$references [ $a ] [ 5 ] += $sum;
	}
	else {
		/*
			Questo macello cosmico e' per tenere il conto di quante volte una variante e'
			stata ordinata.
			In ($references[$q][1]) e ($references[$q][2]) ci sono gli array con le
			varianti ed i valori sinora trovati. Se uno di questi array (il primo) e'
			vuoto, vuol dire che la combinazione che sto testando non e' ancora stata
			trovata e dunque la immetto. Altrimenti verifico che la combinazione
			varianti-valori sia la stessa della componente che sto maneggiando
		*/
		foreach ( $prod_user->variants as $v ) {
			$found = false;

			for ( $q = $a; $q < count ( $references ); $q++ ) {
				if ( is_array ( $references [ $q ] [ 1 ] ) == false )
					break;

				if ( count ( $references [ $q ] [ 1 ] ) == 0 ) {
					foreach ( $v->components as $c ) {
						$references [ $q ] [ 1 ] [] = $c->variant->id;
						$references [ $q ] [ 2 ] [] = $c->value->id;
						$found = true;
					}
				}
				else {
					$found = true;
					$comps = $v->components;

					for ( $z = 0; $z < count ( $comps ); $z++ ) {
						$c = $comps [ $z ];

						if ( $references [ $q ] [ 1 ] [ $z ] != $c->variant->id ||
								$references [ $q ] [ 2 ] [ $z ] != $c->value->id ) {
							$found = false;
							break;
						}
					}
				}

				if ( $found == true ) {
					$references [ $q ] [ 3 ] += 1;
					$references [ $q ] [ 4 ] += $uprice;
					$references [ $q ] [ 5 ] += $sprice;
					break;
				}
			}
		}

		$a--;
	}

	return $a;
}

global $emptycell;

$id = $_GET [ 'id' ];
if ( isset ( $id ) == false )
	error_exit ( "Richiesta non specificata" );

$format = $_GET [ 'format' ];
if ( isset ( $format ) == false )
	error_exit ( "Richiesta non specificata" );

formatting_entities ( $format );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$order = new Order ();
$order->readFromDB ( $id );

$supplier = $order->getAttribute ( 'supplier' )->value;
$supplier_name = $supplier->getAttribute ( 'name' )->value;
$shipping_date = $order->getAttribute ( 'shippingdate' )->value;

$products = $order->getAttribute ( "products" )->value;
usort ( $products, "sort_product_by_name" );

/*
	L'array "references" e' fatto per contenere altri array, uno per ogni possibile prodotto o variante di un
	prodotto. Dunque se ci sono due prodotti, uno senza varianti ed uno con tre possibili valori di "colore", in
	tutti si trovano quattro array. In ogni sotto-array ci sono le informazioni relative a quello specifico
	elemento:

	0: il Product di riferimento
	1: l'array di varianti allegate al prodotto (o null se non ce ne sono)
	2: l'array di valori assunti dalle varianti per questa specifica combinazione (o null se non ce ne sono)
	3: la quantita' raccolta durante le varie iterazioni
	4: il totale del prezzo dei prodotti in questa combinazione
	5: il totale del prezzo di trasporto in questa combinazione
*/

$references = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$p = $products [ $i ];
	$vars = $p->getAttribute ( 'variants' )->value;

	if ( count ( $vars ) == 0 ) {
		$references [] = array ( $p, null, null, 0, 0, 0 );
	}
	else {
		$tot = 1;

		foreach ( $vars as $var )
			$tot = $tot * count ( $var->getAttribute ( 'values' )->value );

		for ( $j = 0; $j < $tot; $j++ )
			$references [] = array ( $p, array (), array (), 0, 0, 0 );
	}
}

$contents = get_orderuser_by_order ( $order );
usort ( $contents, "sort_orders_by_user" );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$user_total = 0;

	/*
		Devo ordinare l'array di ProductUser per nome del prodotto, in modo da essere
		allineato all'array di Products, ma i ProductUser non contengono il nome. Percui
		mi tocca ordinarlo prendendo come traccia l'array di Products, e ricostruirlo da
		un'altra parte
	*/

	$proxy = array ();
	$prev_id = -1;

	for ( $e = 0; $e < count ( $references ); $e++ ) {
		$prod = $references [ $e ] [ 0 ];
		$test_id = $prod->getAttribute ( "id" )->value;

		/*
			Quando uso le varianti, in $references ci sono piu' elementi in sequenza
			che fatto capo allo stesso prodotto. Li devo saltare, altrimenti in
			$proxy ci finisce piu' volte lo stesso $prod_user
		*/
		if ( $prev_id == $test_id )
			continue;

		for ( $a = 0; $a < count ( $user_products ); $a++ ) {
			$prod_user = $user_products [ $a ];

			if ( $test_id == $prod_user->product ) {
				$proxy [] = $prod_user;
				break;
			}
		}

		$prev_id = $test_id;
	}

	unset ( $user_products );
	$user_products = $proxy;

	for ( $a = 0, $e = 0; $a < count ( $references ); $a++ ) {
		$prod = $references [ $a ] [ 0 ];
		$prodid = $prod->getAttribute ( "id" )->value;
		$prod_user = $user_products [ $e ];

		if ( $prodid == $prod_user->product ) {
			$a = manage_product ( $prod, $prod_user, $references, $a );
			$e++;
		}
	}

	if ( count ( $order_user->friends ) != 0 ) {
		$a = 0;
		$prod = $references [ $a ] [ 0 ];
		$prodid = $prod->getAttribute ( "id" )->value;

		while ( $a < count ( $references ) ) {
			foreach ( $order_user->friends as $friend ) {
				foreach ( $friend->products as $fprod ) {
					if ( $fprod->product == $prodid ) {
						manage_product ( $prod, $fprod, $references, $a );
						break;
					}
				}
			}

			$prev_prodid = $prodid;

			do {
				$a++;

				if ( $a >= count ( $references ) )
					break;

				$prod = $references [ $a ] [ 0 ];
				$prodid = $prod->getAttribute ( "id" )->value;
			} while ( $prev_prodid == $prodid );
		}
	}
}

$tot_price = 0;
$tot_transport = 0;
$data = array ();

$headers = array ( 'Prodotto', 'Quantità', 'Unità Misura', 'Prezzo Totale', 'Prezzo Trasporto' );

for ( $i = 0; $i < count ( $references ); $i++ ) {
	if ( $references [ $i ] [ 3 ] == 0 )
		continue;

	if ( is_array ( $references [ $i ] [ 1 ] ) ) {
		$tot = count ( $references [ $i ] [ 1 ] );
		if ( $tot == 0 )
			continue;

		$vars_str = array ();
		$name = get_product_name ( $references [ $i ] [ 0 ] ) . ' ( ';

		for ( $a = 0; $a < $tot; $a++ ) {
			$var = new ProductVariant ();
			$var->readFromDB ( $references [ $i ] [ 1 ] [ $a ] );

			$val = new ProductVariantValue ();
			$val->readFromDB ( $references [ $i ] [ 2 ] [ $a ] );

			$vars_str [] = $var->getAttribute ( 'name' )->value . ': ' . $val->getAttribute ( 'name' )->value;

			unset ( $var );
			unset ( $val );
		}

		$name .= join ( ', ', $vars_str ) . ' )';
	}
	else {
		$name = get_product_name ( $references [ $i ] [ 0 ] );
	}

	$q = get_product_quantity_stocks ( $references [ $i ] [ 0 ], $references [ $i ] [ 3 ] );
	$u = get_product_measure_symbol ( $references [ $i ] [ 0 ] );
	$p = format_price ( round ( $references [ $i ] [ 4 ], 2 ), false );
	$s = format_price ( round ( $references [ $i ] [ 5 ], 2 ), false );

	$data [] = array ( $name, $q, $u, $p, $s );

	$tot_price += $references [ $i ] [ 4 ];
	$tot_transport += $references [ $i ] [ 5 ];
}

$p = format_price ( round ( $tot_price, 2 ), false );
$s = format_price ( round ( $tot_transport, 2 ), false );
$data [] = array ( $emptycell, $emptycell, $emptycell, $p, $s );

output_formatted_document ( 'Ordinazioni ' . $supplier_name . ' ' . $shipping_date, $headers, $data, $format );

?>
