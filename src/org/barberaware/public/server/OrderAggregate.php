<?php

/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class OrderAggregate extends SharableFromServer {
	public function __construct () {
		parent::__construct ( "OrderAggregate" );

		$this->addAttribute ( "orders", "ARRAY::Order" );

		$this->setPublic ( false );
	}

	public function get ( $request, $compress ) {
		$query = parent::arrayRelationCustomQuery ( 'orders', 'status', '!=', 3 );
		return parent::getByQuery ( $request, $compress, $query );
	}

	/*
		TODO	In assenza di controlli vengono pescati anche gli
			OrderAggregate gia' chiusi e consegnati!
	*/

	public function export ( $options ) {
		$ret = array ();
		$name = array ();

		foreach ( self::getAttribute ( 'orders' )->value as $order ) {
			list ( $single_name, $single_ret ) = $order->export ( array () );
			$name [] = $single_name;
			$ret = array_merge ( $ret, $single_ret );
		}

		return array ( join ( ' - ', $name ), $ret );
	}

	public static function import ( &$ref, $contents ) {
		/*
			dummy
		*/
	}
}

?>
