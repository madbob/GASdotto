<?php

/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class BankMovement extends FromServer {
	public function __construct () {
		parent::__construct ( "BankMovement" );

		/*
			Questi due sono ID numerici anziche' riferimenti ad
			oggetti in quanto possono e devono poter assumere valore
			-1, dunque non rispettono alcuna constraint imposta sul
			DB
		*/
		$this->addAttribute ( "payuser", "INTEGER" );
		$this->addAttribute ( "paysupplier", "INTEGER" );

		$this->addAttribute ( "date", "DATE" );
		$this->addAttribute ( "registrationdate", "DATE" );
		$this->addAttribute ( "registrationperson", "OBJECT::User" );
		$this->addAttribute ( "amount", "FLOAT" );
		$this->addAttribute ( "movementtype", "INTEGER" );
		$this->addAttribute ( "method", "INTEGER" );
		$this->addAttribute ( "cro", "STRING" );
		$this->addAttribute ( "notes", "STRING" );

		$this->preserveAttribute ( "registrationperson" );
		$this->noBackDestroy ();
	}

	private function manageSums ( $type, $method, $amount, $user, $supplier, $revert = false ) {
		if ( $amount == 0 )
			return;
			
		global $current_gas;

		$add = array ();
		$sub = array ();

		switch ( $method ) {
			/*
				Dal conto
			*/
			case 0:
				switch ( $type ) {
					case 0:
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_deposit_balance', $current_gas );
						break;

					case 1:
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_deposit_balance', $current_gas );
						break;

					case 2:
						$sub [] = array ( 'Users', 'current_balance', $user );
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						break;

					case 3:
						$add [] = array ( 'Supplier', 'current_balance', $supplier );
						$sub [] = array ( 'Users', 'current_balance', $user );
						$add [] = array ( 'GAS', 'current_orders_balance', $current_gas );
						break;

					case 4:
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_orders_balance', $current_gas );
						$sub [] = array ( 'Supplier', 'current_balance', $supplier );
						break;

					case 5:
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$add [] = array ( 'Users', 'current_balance', $user );
						break;

					case 6:
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						break;

					case 7:
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;
				}

				break;

			/*
				In contanti
			*/
			case 1:
				switch ( $type ) {
					case 0:
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_deposit_balance', $current_gas );
						break;

					case 1:
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_deposit_balance', $current_gas );
						break;

					case 2:
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;

					case 3:
						$add [] = array ( 'Supplier', 'current_balance', $supplier );
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_orders_balance', $current_gas );
						break;

					case 4:
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_orders_balance', $current_gas );
						$sub [] = array ( 'Supplier', 'current_balance', $supplier );
						break;

					case 5:
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$add [] = array ( 'Users', 'current_balance', $user );
						break;

					case 6:
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;

					case 7:
						$add [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;
				}

				break;
		}

		if ( $revert == true ) {
			$tmp = $add;
			$add = $sub;
			$sub = $tmp;
		}

		foreach ( $add as $a ) {
			$query = sprintf ( "UPDATE %s SET %s = %s + %f WHERE id = %d", $a [ 0 ], $a [ 1 ], $a [ 1 ], $amount, $a [ 2 ] );
			query_and_check ( $query, "Impossibile eseguire somma in " . $this->classname );
		}

		foreach ( $sub as $s ) {
			$query = sprintf ( "UPDATE %s SET %s = %s - %f WHERE id = %d", $s [ 0 ], $s [ 1 ], $s [ 1 ], $amount, $s [ 2 ] );
			query_and_check ( $query, "Impossibile eseguire somma in " . $this->classname );
		}

		unset ( $add );
		unset ( $sub );
	}

	private function getObjectProperty ( $name ) {
		$obj = $this->getAttribute ( $name )->value;

		if ( is_object ( $obj ) )
			return $obj->getAttribute ( 'id' )->value;
		else
			return $obj;
	}

	private function revertById ( $id ) {
		$query = sprintf ( "SELECT * FROM %s WHERE id = %d", $this->tablename, $id );
		$returned = query_and_check ( $query, "Impossibile riallineare movimento in " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
		$row = $rows [ 0 ];
		$this->manageSums ( $row [ 'movementtype' ], $row [ 'method' ], $row [ 'amount' ], $row [ 'payuser' ], $row [ 'paysupplier' ], true );
		unset ( $returned );
		unset ( $rows );
	}

	private function assignMovement ( $type, $payuser, $id ) {
		/*
			Questo e' per evitare che vengano creati nuovi utenti a
			vuoto, soprattutto quando non sono abilitate le quote
			per i soci ma comunque transitano dei BankMovement di
			tal fatta (ovviamente senza alcun payuser settato)
		*/
		if ( $payuser <= 0 )
			return;

		switch ( $type ) {
			case 0:
				$tmp = new User ();
				$tmp->readFromDB ( $payuser );

				if ( $tmp->getAttribute ( 'deposit' )->value != $id ) {
					$tmp->getAttribute ( 'deposit' )->value = $id;
					$tmp->save ( $tmp->exportable () );
				}

				break;

			case 2:
				$tmp = new User ();
				$tmp->readFromDB ( $payuser );

				if ( $tmp->getAttribute ( 'paying' )->value != $id ) {
					$tmp->getAttribute ( 'paying' )->value = $id;
					$tmp->save ( $tmp->exportable () );
				}

				break;

			default:
				break;
		}
	}

	public function save ( $obj ) {
		global $current_user;

		if ( $obj instanceof FromServer )
			$this->dupBy ( $obj );
		else
			$this->from_object_to_internal ( $obj );

		/*
			Per scaramanzia...
		*/
		$registrationperson = $this->getAttribute ( "registrationperson" )->value;
		if ( $registrationperson <= 0 )
			$this->getAttribute ( "registrationperson" )->value = $current_user;

		$id = $this->getAttribute ( "id" )->value;
		if ( $id != -1 )
			$this->revertById ( $id );

		$id = parent::save ( $obj );

		if ( $id != -1 ) {
			$obj->payuser = $this->getObjectProperty ( 'payuser' );
			$obj->paysupplier = $this->getObjectProperty ( 'paysupplier' );

			$this->manageSums ( $obj->movementtype, $obj->method, $obj->amount, $obj->payuser, $obj->paysupplier, false );
		}

		$this->assignMovement ( $obj->movementtype, $obj->payuser, $id );

		return $id;
	}

	/*
		I movimenti non vengono mai eliminati, ma solo messi a 0.
		Questo per non rompere le numerose ed eterogenee relazioni nel
		database
	*/
	public function destroy ( $obj ) {
		$obj->amount = 0;
		return $this->save ( $obj );
	}

	public function get ( $request, $compress ) {
		$query = array ();

		if ( $request != null ) {
			if ( property_exists ( $request, 'startdate' ) )
				$query [] = sprintf ( "date >= '%s'", $request->startdate );
			if ( property_exists ( $request, 'enddate' ) )
				$query [] = sprintf ( "date <= '%s'", $request->enddate );
			if ( property_exists ( $request, 'payuser' ) )
				$query [] = sprintf ( "payuser = %d", $request->payuser );
			if ( property_exists ( $request, 'paysupplier' ) )
				$query [] = sprintf ( "paysupplier = %d", $request->paysupplier );

			/*
				Non filtrare sempre per amount != 0: buona parte dei BankMovement
				registrati potrebbero essere semplici placeholders creati salvando
				l'oggetto di riferimento, dunque appunto avere amount = 0, se non
				vengono restituiti al client questo continuera' a crearne di nuovi
				ogni volta
			*/
			$query [] = 'amount != 0';
		}

		$query = join ( ' AND ', $query );

		return parent::getByQuery ( $request, $compress, $query );
	}

	public function fix ( $offset ) {
		if ( $offset == -1 ) {
			$page = -1;
			$query_limit = '';
		}
		else {
			$slice = 1000;
			$page = $offset * $slice;
			$query_limit = " LIMIT $slice OFFSET $page";
		}

		if ( $offset == 0 || $offset == -1 ) {
			$query = sprintf ( "UPDATE Users SET current_balance = 0" );
			query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );

			$query = sprintf ( "UPDATE Supplier SET current_balance = 0" );
			query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );

			$query = sprintf ( "UPDATE GAS SET current_balance = 0, current_cash_balance = 0, current_bank_balance = 0, current_orders_balance = 0" );
			query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );
		}

		$query = sprintf ( "SELECT * FROM %s WHERE amount != 0 ORDER BY date $query_limit", $this->tablename );
		$returned = query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );

		if ( $returned->rowCount () == 0 ) {
			return 'done';
		}
		else {
			while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
				$this->manageSums ( $row [ 'movementtype' ], $row [ 'method' ], $row [ 'amount' ], $row [ 'payuser' ], $row [ 'paysupplier' ], false );
				unset ( $row );
			}

			return $offset + 1;
		}
	}
}

?>
