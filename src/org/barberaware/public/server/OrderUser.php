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

		$this->addAttribute ( "baseorder", "OBJECT::Order" );
		$this->addAttribute ( "baseuser", "OBJECT::User" );
		$this->addAttribute ( "products", "ARRAY::ProductUser" );

		$this->enforceUserCheck ( "baseuser" );
	}

	public function get ( $request ) {
		/*
			Al momento questo viene usato solo per order_csv.php, dunque non viene
			contemplato il parametro "has" come nelle altre ricerche
		*/
		if ( isset ( $request->order ) ) {
			$ret = array ();

			$query = sprintf ( "SELECT id FROM %s WHERE baseorder = %d ORDER BY id",
						$this->tablename, $request->order );

			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );

			while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
				$obj = new $this->classname;
				$obj->readFromDB ( $row [ 'id' ] );
				array_push ( $ret, $obj->exportable () );
			}
		}
		else
			$ret = parent::get ( $request );

		return $ret;
	}
}

?>
