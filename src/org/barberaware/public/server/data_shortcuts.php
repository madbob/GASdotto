<?php

/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

if ( !isset ( $_GET [ 'type' ] ) )
	error_exit ( "Richiesta non valida" );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$ret = null;

switch ( $_GET [ 'type' ] ) {
	case 'order_products_diff':
		$order = $_GET [ 'order' ];
		if ( !isset ( $order ) )
			error_exit ( "Richiesta non valida" );

		$ord = new Order ();
		$ord->readFromDB ( $order );

		$tmp = new Product ();

		$query = sprintf ( "SELECT id FROM %s
					WHERE archived = false AND supplier = %d AND available = true AND
					id NOT IN ( SELECT target FROM orders_products WHERE parent = %d ) AND
					previous_description NOT IN ( SELECT target FROM orders_products WHERE parent = %d )",
						$tmp->tablename, $ord->getAttribute ( 'supplier' )->value->getAttribute ( 'id' )->value, $order, $order );

		$result = query_and_check ( $query, "Impossibile recuperare differenza tra prodotti e ordine" );
		$rows = $result->fetchAll ( PDO::FETCH_ASSOC );
		$ret = array ();

		foreach ( $rows as $row ) {
			$product = new $tmp->classname;
			$product->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $product->exportable () );
		}

		break;

	case 'available_quantity_yet':
		$product = $_GET [ 'product' ];
		if ( !isset ( $product ) )
			error_exit ( "Richiesta non valida" );

		$ret = new stdClass ();
		$ret->index = $_GET [ 'index' ];

		$prod = new Product ();
		$prod->readFromDB ( $product );
		$max_quantity = $prod->getAttribute ( 'total_max_order' )->value;

		if ( $max_quantity == 0 ) {
			$ret->quantity = "-1";
		}
		else {
			$query = sprintf ( "SELECT SUM(quantity)
						FROM ProductUser
						WHERE product = %d AND
							id IN (SELECT target FROM OrderUser_products)", $product );
			$result = query_and_check ( $query, "Impossibile recuperare quantità sinora ordinata" );
			$row = $result->fetchAll ( PDO::FETCH_NUM );
			$quantity = $row [ 0 ] [ 0 ];

			if ( $quantity > $max_quantity )
				$ret->quantity = "0";
			else
				$ret->quantity = ( string ) ( $max_quantity - $quantity );
		}

		break;

	default:
		error_exit ( "Richiesta non valida" );
		break;
}

if ( $ret != null ) {
	$json = new Services_JSON ();
	echo $json->encode ( $ret ) . "\n";
}

exit ( 0 );

?>
