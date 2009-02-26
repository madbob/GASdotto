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

	public function get ( $request ) {
		if ( isset ( $request->status ) ) {
			$ret = array ();

			$query = sprintf ( "SELECT id FROM %s WHERE status = %d ", $this->tablename, $request->status );

			if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
				$ids = join ( ',', $request->has );
				$query .= sprintf ( "AND id NOT IN ( %s ) ", $ids );
			}

			$query .= "ORDER BY id";

			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );

			while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
				$obj = new $this->classname;
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
