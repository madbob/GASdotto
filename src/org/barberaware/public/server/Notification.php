<?

/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		parent::addAttribute ( "recipent", "INTEGER" );
	}

	public function get ( $request ) {
		global $current_user;

		/*
			Elimino le notifiche oramai scadute
		*/
		$query = sprintf ( "DELETE FROM %s WHERE enddate < DATE('%s')",
					$this->tablename, date ( "Y-m-d" ) );
		query_and_check ( $query, "Impossibile eliminare vecchie notifiche" );

		$ret = array ();

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query = sprintf ( "SELECT id FROM %s WHERE id NOT IN ( %s )", $this->tablename, $ids );
		}
		else {
			/*
				Per far quagliare la concatenazione di altri frammenti di query
				forzo l'esistenza di uno statement WHERE cui accodare gli altri
				in AND
			*/
			$query = sprintf ( "SELECT id FROM %s WHERE true", $this->tablename, $check_query );
		}

		if ( !isset ( $request->all ) ) {
			/*
				Il valore -1 per il campo recipent indica che la notifica e' destinata a
				tutti gli utenti
			*/
			$query .= sprintf ( " AND recipent = %d OR recipent = %d", $current_user, -1 );
		}

		$query .= sprintf ( " ORDER BY startdate DESC" );
		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );

		while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable () );
		}

		return $ret;
	}
}

?>
