<?php

/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

function missing_products ( $order ) {
	$ord = new Order ();
	$ord->readFromDB ( $order );

	$tmp = new Product ();

	$query = sprintf ( "SELECT id FROM %s
				WHERE supplier = %d AND
					available = true AND
					archived = false AND
					previous_description = 0 AND
					id NOT IN ( SELECT target FROM Orders_products )",
				$tmp->tablename, $ord->getAttribute ( 'supplier' )->value->getAttribute ( 'id' )->value );

	$result = query_and_check ( $query, "Impossibile recuperare differenza tra prodotti e ordine" );
	$rows = $result->fetchAll ( PDO::FETCH_ASSOC );
	unset ( $result );
	$ret = array ();

	foreach ( $rows as $row ) {
		$product = new $tmp->classname;
		$product->readFromDB ( $row [ 'id' ] );
		array_push ( $ret, $product->exportable () );
	}

	return $ret;
}

$type = require_param ( 'type' );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$ret = null;

switch ( $type ) {
	case 'order_products_diff':
		$order = require_param ( 'order' );
		$aggregate = get_param ( 'aggregate', 'false' );

		if ( $aggregate == 'true' ) {
			$ord = new OrderAggregate ();
			$ord->readFromDB ( $order );

			$ret = array ();
			$orders = $ord->getAttribute ( 'orders' )->value;

			foreach ( $orders as $o )
				$ret = array_merge ( $ret, missing_products ( $o->getAttribute ( 'id' )->value ) );
		}
		else {
			$ret = missing_products ( $order );
		}

		break;

	case 'available_quantity_yet':
		$product = require_param ( 'product' );

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
			unset ( $result );
			$quantity = $row [ 0 ] [ 0 ];

			if ( $quantity > $max_quantity )
				$ret->quantity = "0";
			else
				$ret->quantity = ( string ) ( $max_quantity - $quantity );
		}

		break;

	case 'unique_user':
		$name = require_param ( 'username' );
		$id = require_param ( 'id' );

		/*
			Lo username "master" e' riservato per il gestore del
			Multi-GAS
		*/
		if ( $name == 'master' ) {
			$ret = 'Username riservato';
		}
		else {
			$tmp = new User ();
			$query = sprintf ( "FROM %s WHERE login = '$name' AND id != $id", $tmp->tablename );
			if ( db_row_count ( $query ) != 0 )
				$ret = 'Username già assegnato';
		}

		break;

	default:
		error_exit ( "Richiesta non valida" );
		break;
}

if ( $ret != null )
	echo json_encode ( $ret ) . "\n";

exit ( 0 );

?>
