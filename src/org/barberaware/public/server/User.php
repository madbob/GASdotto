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

class User extends FromServer {
	public function __construct () {
		parent::__construct ( "User", "Users" );

		$this->addAttribute ( "login", "STRING" );
		$this->addAttribute ( "firstname", "STRING" );
		$this->addAttribute ( "surname", "STRING" );
		$this->addAttribute ( "birthday", "DATE" );
		$this->addAttribute ( "join_date", "DATE" );
		$this->addAttribute ( "card_number", "STRING" );
		$this->addAttribute ( "phone", "STRING" );
		$this->addAttribute ( "mobile", "STRING" );
		$this->addAttribute ( "mail", "STRING" );
		$this->addAttribute ( "mail2", "STRING" );
		$this->addAttribute ( "address", "ADDRESS" );
		$this->addAttribute ( "paying", "OBJECT::BankMovement" );
		$this->addAttribute ( "privileges", "INTEGER", "1" );
		$this->addAttribute ( "family", "INTEGER" );
		$this->addAttribute ( "photo", "STRING" );
		$this->addAttribute ( "codfisc", "STRING" );
		$this->addAttribute ( "lastlogin", "DATE" );
		$this->addAttribute ( "leaving_date", "DATE" );
		$this->addAttribute ( "bank_account", "STRING" );
		$this->addAttribute ( "current_balance", "FLOAT" );
		$this->addAttribute ( "deposit", "OBJECT::BankMovement" );
		$this->addAttribute ( "sepa_subscribe", "DATE" );
		$this->addAttribute ( "first_sepa", "DATE" );
		$this->addAttribute ( "shipping", "OBJECT::ShippingPlace", ShippingPlace::getDefault () );
		$this->addAttribute ( "suppliers_notification", "ARRAY::Supplier" );

		$this->setSorting ( "surname" );
		$this->setPublic ( false );
	}

	public function get ( $request, $compress ) {
		if ( $request != null && property_exists ( $request, 'password' ) ) {
			$query = sprintf ( "SELECT password FROM accounts
						WHERE username = (SELECT id FROM %s WHERE login = '%s')", $this->tablename, $request->username );
			$returned = query_and_check ( $query, "Impossibile validare utente" );
			$row = $returned->fetchAll ( PDO::FETCH_ASSOC );

			if ( md5 ( $request->password ) == $row [ 0 ] [ 'password' ] )
				return "OK";
			else
				return "NO";
		}

		if ( $request != null && property_exists ( $request, 'privileges' ) )
			$query = sprintf ( "privileges = %d", $request->privileges );
		else
			$query = sprintf ( "privileges != 3" );

		return parent::getByQuery ( $request, $compress, $query );
	}

	public function save ( $obj ) {
		$newly_created = ( $obj->id == -1 );
		$id = parent::save ( $obj );

		if ( property_exists ( $obj, 'password' ) && ( $obj->password != "" || $newly_created ) ) {
			$password = md5 ( $obj->password );

			if ( $newly_created )
				$query = sprintf ( "INSERT INTO accounts ( username, password )
							VALUES ( %d, '%s' )",
								$id, $password );
			else
				$query = sprintf ( "UPDATE accounts SET password = '%s'
							WHERE username = %d",
								$password, $id );

			query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );
		}

		if ( $obj->privileges == 3 ) {
			/*
				L'atto di cessazione della partecipazione di un utente ha effetto
				immediato, percui invalido tutte le sessioni aperte/salvate
			*/
			$query = sprintf ( "DELETE FROM current_sessions WHERE username = %d", $obj->id );
			query_and_check ( $query, "Impossibile eliminare sessioni aperte per utente sospeso" );
		}

		return $id;
	}

	public function destroy ( $obj ) {
		/*
			Prima viene rimosso l'account in quanto la tabella contiene una
			constraint verso Users
		*/
		$query = sprintf ( "DELETE FROM accounts WHERE username = %d", $obj->id );
		query_and_check ( $query, "Impossibile eliminare oggetto " . $this->classname );

		parent::destroy ( $obj );
		return $obj->id;
	}

	public function registerLogin () {
		$query = sprintf ( "UPDATE %s SET lastlogin = DATE('%s') WHERE id = %d",
		                   $this->tablename, date ( "Y-m-d", time () ), $this->getAttribute ( "id" )->value );
		query_and_check ( $query, "Impossibile salvare data login" );
	}

	public function getBySupplierSubscription ( $supplier_id ) {
		$query = parent::arrayRelationQuery ( 'suppliers_notification', $supplier_id );
		return parent::getByQuery ( null, false, $query );
	}
}

?>
