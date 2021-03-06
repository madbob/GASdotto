<?php

/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class ProductUser extends FromServer {
	public function __construct () {
		parent::__construct ( "ProductUser" );

		$this->addAttribute ( "product", "OBJECT::Product" );
		$this->addAttribute ( "variants", "ARRAY::ProductUserVariant" );
		$this->addAttribute ( "quantity", "FLOAT" );
		$this->addAttribute ( "delivered", "FLOAT" );
		$this->addAttribute ( "orderdate", "DATE" );
		$this->addAttribute ( "orderperson", "OBJECT::User" );

		$this->preserveAttribute ( "product" );
		$this->preserveAttribute ( "orderperson" );

		// $this->setPublic ( false, 'asc', 'OrderUser', 'products' );
	}

	public function save ( $obj ) {
		if ( property_exists ( $obj, 'variants' ) && is_array ( $obj->variants ) && count ( $obj->variants ) > 1 ) {
			if ( is_numeric ( $obj->product ) ) {
				$prod = new Product ();
				$prod->readFromDB ( $obj->product );
			}
			else {
				$prod = $obj->product;
			}

			if ( $prod->getAttribute ( 'mutable_price' )->value == true ) {
				$tmp = array ();
				$tmp [] = $obj->variants [ 0 ];
				$obj->variants = $tmp;
			}
		}

		return parent::save ( $obj );
	}
}

?>
