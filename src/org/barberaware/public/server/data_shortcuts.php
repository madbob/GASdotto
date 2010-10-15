<?php

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

if ( !isset ( $_GET [ 'type' ] ) )
	exit ( 0 );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

switch ( $_GET [ 'type' ] ) {
	case 'order_products_diff':
		$order = $_GET [ 'order' ];
		if ( !isset ( $order ) )
			exit ( 0 );

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

		$json = new Services_JSON ();
		echo $json->encode ( $ret ) . "\n";
		break;
}

exit ( 0 );

?>
