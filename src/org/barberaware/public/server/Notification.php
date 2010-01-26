<?

/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class Notification extends FromServer {
	public function __construct () {
		parent::__construct ( "Notification" );

		parent::addAttribute ( "alert_type", "INTEGER" );
		parent::addAttribute ( "description", "STRING" );
		parent::addAttribute ( "startdate", "DATE" );
		parent::addAttribute ( "enddate", "DATE" );
		parent::addAttribute ( "recipent", "ARRAY::User" );
	}

	public function get ( $request, $compress ) {
		global $current_user;

		/*
			Elimino le notifiche oramai scadute
		*/
		$query = sprintf ( "DELETE FROM %s WHERE enddate < DATE('%s')",
					$this->tablename, date ( "Y-m-d" ) );
		query_and_check ( $query, "Impossibile eliminare vecchie notifiche" );

		$ret = array ();

		$u = new User ();
		$references = $this->tablename . "_recipent";
		$query = sprintf ( "SELECT %s.id FROM %s, %s WHERE ", $this->tablename, $this->tablename, $references );

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query .= sprintf ( " %s.id NOT IN ( %s ) AND ", $this->tablename, $ids );
		}

		$query .= sprintf ( " %s.target = %d AND %s.id = %s.parent ORDER BY startdate DESC", $references, $current_user, $this->tablename, $references );
		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
		}

		return $ret;
	}
}

?>
