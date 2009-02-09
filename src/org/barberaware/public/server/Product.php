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

class Product extends FromServer {
	public function __construct () {
		parent::__construct ( "Product" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "category", "OBJECT::Category" );
		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "shipping_price", "FLOAT" );
		$this->addAttribute ( "unit_price", "FLOAT" );
		$this->addAttribute ( "surplus", "PERCENTAGE" );
		$this->addAttribute ( "measure", "OBJECT::Measure" );
		$this->addAttribute ( "minimum_order", "INTEGER" );
		$this->addAttribute ( "multiple_order", "INTEGER" );
		$this->addAttribute ( "stock_size", "INTEGER" );
		$this->addAttribute ( "mutable_price", "INTEGER" );
		$this->addAttribute ( "available", "BOOLEAN" );
	}

	public function get ( $request ) {
		$ret = array ();

		if ( isset ( $request->supplier ) ) {
			$ret = array ();

			$query = sprintf ( "SELECT id FROM product
						WHERE supplier = %d", $request->supplier->id );

			if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
				$ids = join ( ',', $request->has );
				$query .= sprintf ( " AND id NOT IN ( %s ) ", $ids );
			}

			$query .= sprintf ( " ORDER BY id" );
			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti product" );

			while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
				$obj = new Product ();
				$obj->readFromDB ( $row [ 'id' ] );
				array_push ( $ret, $obj->exportable () );
			}

			return $ret;
		}
		else
			return parent::get ( $request );
	}
}

?>
