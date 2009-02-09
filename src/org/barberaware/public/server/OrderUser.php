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

class OrderUser extends FromServer {
	public function __construct () {
		parent::__construct ( "OrderUser" );

		$this->addAttribute ( "order", "OBJECT::Order" );
		$this->addAttribute ( "user", "OBJECT::User" );
		$this->addAttribute ( "products", "ARRAY::ProductUser" );
	}

	public function readFromDB ( $id ) {
		parent::readFromDB ( $id );

		$products = $this->getAttribute ( "products" );

		$query = sprintf ( "SELECT * FROM orderuser_products WHERE parent = %d", $id );
		$returned = query_and_check ( $query, "Impossibile recuperare lista prodotti per ordine " . $id );

		while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
			$obj = new ProductUser ();
			$obj->readFromDB ( $row [ "target" ] );
			array_push ( $products->value, $obj );
		}
	}

	public function get ( $request ) {
		$ret = array ();

		if ( isset ( $request->order ) ) {
			if ( isset ( $request->user ) )
				$query = sprintf ( "SELECT id FROM orderuser
							WHERE order = %d AND user = %d",
								$request->order->id, $request->user->id );

			else
				$query = sprintf ( "SELECT id FROM orderuser
							WHERE order = %d",
								$request->order->id );

			if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
				$ids = join ( ',', $request->has );
				$query .= sprintf ( " AND id NOT IN ( %s ) ", $ids );
			}

			$query .= sprintf ( " ORDER BY id" );
			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti product" );

			while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
				$obj = new OrderUser ();
				$obj->readFromDB ( $row [ 'id' ] );
				array_push ( $ret, $obj->exportable () );
			}

			return $ret;
		}
		else
			return $this->get ( $request );
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

				$query = sprintf ( "INSERT INTO orderuser_products ( parent, target )
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

			$query = sprintf ( "SELECT target FROM orderuser_products
						WHERE parent = %d",
							$id );
			$existing = query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );

			while ( $row = $existing->fetch ( PDO::FETCH_ASSOC ) ) {
				$found = false;

				for ( $i = 0; $i < count ( $obj->products ); $i++ ) {
					$prod = $obj->products [ $i ];

					if ( $prod->id == $row [ 'target' ] ) {
						$prod->id = -1;
						$found = true;
						break;
					}
				}

				if ( $found == false ) {
					$query = sprintf ( "DELETE FROM orderuser_products
								WHERE parent = %d
								AND target = %d",
									$id, $row [ 'target' ] );
					query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );
				}
			}

			for ( $i = 0; $i < count ( $obj->products ); $i++ ) {
				$prod = $obj->products [ $i ];

				if ( $prod->id != -1 ) {
					$query = sprintf ( "INSERT INTO orderuser_products ( parent, target )
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
