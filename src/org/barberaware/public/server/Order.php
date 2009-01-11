<?

/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class Order extends FromServer {
	public function __construct () {
		parent::__construct ( "Order", "orders" );

		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "reference", "OBJECT::User" );
		$this->addAttribute ( "products", "ARRAY::Product" );
		$this->addAttribute ( "startdate", "DATE" );
		$this->addAttribute ( "enddate", "DATE" );
		$this->addAttribute ( "status", "INTEGER" );
		$this->addAttribute ( "shippingdate", "DATE" );
		$this->addAttribute ( "nextdate", "STRING" );
		$this->addAttribute ( "anticipated", "STRING" );
	}

	public function readFromDB ( $id ) {
		parent::readFromDB ( $id );

		$products = $this->getAttribute ( "products" );

		$query = sprintf ( "SELECT * FROM orderdetails WHERE parent = %d", $id );
		$returned = query_and_check ( $query, "Impossibile recuperare lista prodotti per ordine " . $id );

		while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
			$obj = new Product ();
			$obj->readFromDB ( $row [ "product" ] );
			array_push ( $products->value, $obj );
		}
	}

	public function save ( $obj ) {
		$this->from_object_to_internal ( $obj );

		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		if ( $id == -1 ) {
			$names = array ();
			$values = array ();

			for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
				$attr = $this->attributes [ $i ];

				if ( $attr->name == "id" || $attr->name == "products" )
					continue;

				$value = $this->attr_to_db ( $attr );
				if ( $value == null )
					continue;

				array_push ( $names, $attr->name );
				array_push ( $values, $value );
			}

			$query = sprintf ( "INSERT INTO %s ( %s )
						VALUES ( %s )",
							$this->tablename, join ( ", ", $names ), join ( ", ", $values ) );

			query_and_check ( $query, "Impossibile salvare oggetto " . $this->classname );
			$order_id = last_id ( $this->tablename );

			for ( $i = 0; $i < count ( $obj->products ); $i++ ) {
				$prod = $obj->products [ $i ];

				$query = sprintf ( "INSERT INTO orderdetails ( parent, product )
							VALUES ( %d, %d )",
								$order_id, $prod->id );

				query_and_check ( $query, "Impossibile salvare prodotto in ordine" );
			}
		}
		else {
			$values = array ();

			for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
				$attr = $this->attributes [ $i ];

				if ( $attr->name == "id" || $attr->name == "products" )
					continue;

				$value = $this->attr_to_db ( $attr );
				if ( $value == null )
					continue;

				array_push ( $values, ( $attr->name . " = " . $value ) );
			}

			$query = sprintf ( "UPDATE %s SET %s
						WHERE id = %d",
							$this->tablename, join ( ", ", $values ), $id );
			query_and_check ( $query, "Impossibile salvare oggetto " . $this->classname );

			/* Allineamento prodotti aggiunti e prodotti rimossi */

			$query = sprintf ( "SELECT product FROM orderdetails
						WHERE parent = %d",
							$id );
			$existing = query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );

			while ( $row = $existing->fetch ( PDO::FETCH_ASSOC ) ) {
				$found = false;

				for ( $i = 0; $i < count ( $obj->products ); $i++ ) {
					$prod = $obj->products [ $i ];

					if ( $prod->id == $row [ 'product' ] ) {
						$prod->id = -1;
						$found = true;
						break;
					}
				}

				if ( $found == false ) {
					$query = sprintf ( "DELETE FROM orderdetails
								WHERE parent = %d
								AND product = %d",
									$id, $row [ 'product' ] );
					query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );
				}
			}

			for ( $i = 0; $i < count ( $obj->products ); $i++ ) {
				$prod = $obj->products [ $i ];

				if ( $prod->id != -1 ) {
					$query = sprintf ( "INSERT INTO orderdetails ( parent, product )
								VALUES ( %d, %d )",
									$id, $prod->id );
					query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );
				}
			}

			$order_id = $id;
		}

		return $order_id;
	}
}

?>
