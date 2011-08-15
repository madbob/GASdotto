<?php

/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class ACL extends FromServer {
	public function __construct () {
		parent::__construct ( "ACL" );

		$this->addAttribute ( "gas", "OBJECT::GAS" );
		$this->addAttribute ( "target_type", "STRING" );
		$this->addAttribute ( "target_id", "INTEGER" );
		$this->addAttribute ( "privileges", "INTEGER" );
	}

	public function get ( $request, $compress ) {
		$ret = array ();

		if ( isset ( $request->target_type ) && isset ( $request->target_id ) ) {
			$query = sprintf ( "SELECT id, gas FROM %s WHERE target_type = '%s' AND target_id = %d", $this->tablename, $request->target_type, $request->target_id );
			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
			$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
			unset ( $returned );

			$managed_gas = array ();

			foreach ( $rows as $row ) {
				$obj = new $this->classname;
				$obj->readFromDB ( $row [ 'id' ] );
				array_push ( $ret, $obj->exportable ( $request, $compress ) );
				array_push ( $managed_gas, $row [ 'gas' ] );
			}

			$tmp = new GAS ();
			$query = sprintf ( "SELECT id FROM %s WHERE id NOT IN ( %s )", $tmp->tablename, join ( ', ', $managed_gas ) );
			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
			$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
			unset ( $returned );

			foreach ( $rows as $row ) {
				$gas = new GAS ();
				$gas->readFromDB ( $row [ 'id' ] );

				$obj = new $this->classname;
				$obj->getAttribute ( 'gas' )->value = $gas;
				$obj->getAttribute ( 'target_type' )->value = $request->target_type;
				$obj->getAttribute ( 'target_id' )->value = $request->target_id;
				$obj->getAttribute ( 'privileges' )->value = 3;

				array_push ( $ret, $obj->exportable ( $request, $compress ) );
			}
		}

		return $ret;
	}

	public function save ( $obj ) {
		/*
			TODO	Fare un controllo serio sull'autorizzazione
				della richiesta
		*/
		return parent::save ( $obj );
	}
}

?>
