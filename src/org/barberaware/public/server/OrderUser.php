<?php

/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		$this->addAttribute ( "friends", "ARRAY::OrderUserFriend" );
		$this->addAttribute ( "deliverydate", "DATE" );
		$this->addAttribute ( "deliveryperson", "OBJECT::User" );
		$this->addAttribute ( "status", "INTEGER" );

		$this->enforceUserCheck ( "baseuser" );
	}

	public function get ( $request, $compress ) {
		global $current_user;

		$ret = array ();
		$query = sprintf ( "SELECT id FROM %s WHERE ", $this->tablename );

		if ( isset ( $request->id ) ) {
			$query .= sprintf ( "id = %d ", $request->id );
		}
		else {
			$ord = new Order ();

			if ( isset ( $request->baseorder ) ) {
				$query .= sprintf ( "baseorder = %d ", $request->baseorder );
			}
			else if ( isset ( $request->supplier ) ) {
				$query .= sprintf ( "baseorder IN ( SELECT id FROM %s WHERE supplier = %d ) ", $ord->tablename, $request->supplier );
			}
			else {
				if ( !isset ( $request->all ) ) {
					/*
						Per le richieste generiche, vengono scartati i dati per ordini
						che sono gia' stati consegnati (status = 3)
					*/
					$query .= sprintf ( "baseorder NOT IN (SELECT id FROM %s WHERE status = %d) ", $ord->tablename, 3 );
				}
				else {
					/*
						Riempitivo...
					*/
					$query .= sprintf ( "id > -1 " );
				}
			}

			if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
				$ids = join ( ',', $request->has );
				$query .= sprintf ( "AND id NOT IN ( %s ) ", $ids );
			}
		}

		if ( current_permissions () == 0 )
			$query .= sprintf ( "AND baseuser = %d ", $current_user );
		else if ( isset ( $request->baseuser ) )
			$query .= sprintf ( "AND baseuser = %d ", $request->baseuser );

		$query .= "ORDER BY id";

		if ( isset ( $request->query_limit ) )
			$query .= sprintf ( " LIMIT %d", $request->query_limit );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
		}

		return $ret;
	}

	public function save ( $obj ) {
		/*
			Questo e' per evitare di salvare nel DB ordini vuoti, per cui non e'
			stata settata alcuna quantita' di prodotti
		*/
		if ( count ( $obj->products ) == 0 && count ( $obj->friends ) == 0 ) {
			if ( $obj->id == -1 )
				return -1;
			else if ( $obj->id != -1 )
				return parent::destroy ( $obj );
		}
		else {
			return parent::save ( $obj );
		}
	}
}

?>
