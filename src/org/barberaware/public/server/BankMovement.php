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

		$this->addAttribute ( "payuser", "OBJECT::User" );
		$this->addAttribute ( "paysupplier", "OBJECT::Supplier" );
		$this->addAttribute ( "date", "DATE" );
		$this->addAttribute ( "registrationdate", "DATE" );
		$this->addAttribute ( "registrationperson", "OBJECT::User" );
		$this->addAttribute ( "amount", "FLOAT" );
		$this->addAttribute ( "movementtype", "INTEGER" );
		$this->addAttribute ( "method", "INTEGER" );
		$this->addAttribute ( "cro", "STRING" );
		$this->addAttribute ( "notes", "STRING" );
	}

	/*

	+-----------------+--------------------------------------+-------------------+-------------------+
	| FUNZIONE        | TIPO                                 | AUMENTA           | DIMINUISCE        |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Consegne        | Pagamento Ordine in Contanti         | GAS / Saldo Cassa |                   |
	|                 |                                      | Fornitore / Saldo |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Consegne        | Pagamento Ordine da Credito          | Fornitore / Saldo | Socio / Saldo     |
	|                 |                                      |                   |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Utenti | Versamento Cauzione                  | GAS / Saldo Cassa |                   |
	|                 |                                      |                   |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Utenti | Restituzione Cauzione                |                   | GAS / Saldo Cassa |
	|                 |                                      |                   |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Utenti | Versamento Quota Annuale in Contanti | GAS / Saldo Cassa |                   |
	|                 |                                      | GAS / Saldo       |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Utenti | Versamento Quota Annuale da Credito  | GAS / Saldo       | Socio / Saldo     |
	|                 |                                      |                   |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Utenti | Bonifico da Socio                    | GAS / Saldo Conto |                   |
	|                 |                                      | Socio / Saldo     |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Utenti | Versamento Socio in Contanti         | GAS / Saldo Cassa |                   |
	|                 |                                      | Socio / Saldo     |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Fornitori       | Pagamento Fattura via Bonifico       |                   | GAS / Saldo Conto |
	|                 |                                      |                   | Fornitore / Saldo |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Fornitori       | Pagamento Fattura in Contanti        |                   | GAS / Saldo Cassa |
	|                 |                                      |                   | Fornitore / Saldo |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Cassa  | Acquisto con Bonifico                |                   | GAS / Saldo Conto |
	|                 |                                      |                   | GAS / Saldo       |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Cassa  | Acquisto in Contanti                 |                   | GAS / Saldo Cassa |
	|                 |                                      |                   | GAS / Saldo       |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Cassa  | Versamento Contanti                  | GAS / Saldo Conto | GAS / Saldo Cassa |
	|                 |                                      |                   |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+
	| Gestione Cassa  | Prelievo Contanti                    | GAS / Saldo Cassa | GAS / Saldo Conto |
	|                 |                                      |                   |                   |
	+-----------------+--------------------------------------+-------------------+-------------------+

	*/
	private function manageSums ( $type, $method, $amount, $user, $supplier, $revert = false ) {
		global $current_gas;

		if ( $amount == 0 )
			return;

		if ( $user != null && is_object ( $user ) )
			$user = $user->id;
		if ( $supplier != null && is_object ( $supplier ) )
			$supplier = $supplier->id;

		$add = array ();
		$sub = array ();

		switch ( $method ) {
			/*
				Dal conto
			*/
			case 0:
				switch ( $type ) {
					case 0:
						$add [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						break;

					case 1:
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						break;

					case 2:
						$sub [] = array ( 'Users', 'current_balance', $user );
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						break;

					case 3:
						$add [] = array ( 'Supplier', 'current_balance', $supplier );
						$sub [] = array ( 'Users', 'current_balance', $user );
						break;

					case 4:
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$sub [] = array ( 'Supplier', 'current_balance', $supplier );
						break;

					case 5:
						$add [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$add [] = array ( 'Users', 'current_balance', $user );
						break;

					case 6:
						$sub [] = array ( 'GAS', 'current_bank_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
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
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;

					case 1:
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;

					case 2:
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$add [] = array ( 'GAS', 'current_balance', $current_gas );
						break;

					case 3:
						$add [] = array ( 'Supplier', 'current_balance', $supplier );
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						break;

					case 4:
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$sub [] = array ( 'Supplier', 'current_balance', $supplier );
						break;

					case 5:
						$add [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$add [] = array ( 'Users', 'current_balance', $user );
						break;

					case 6:
						$sub [] = array ( 'GAS', 'current_cash_balance', $current_gas );
						$sub [] = array ( 'GAS', 'current_balance', $current_gas );
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

	public function save ( $obj ) {
		if ( $obj instanceof FromServer )
			$this->dupBy ( $obj );
		else
			$this->from_object_to_internal ( $obj );

		$id = $this->getAttribute ( "id" )->value;

		if ( $id != -1 ) {
			$query = sprintf ( "SELECT * FROM %s WHERE id = %d", $this->tablename, $id );
			$returned = query_and_check ( $query, "Impossibile riallineare movimento in " . $this->classname );
			$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
			$row = $rows [ 0 ];
			$this->manageSums ( $row [ 'movementtype' ], $row [ 'method' ], $row [ 'amount' ], $row [ 'payuser' ], $row [ 'paysupplier' ], true );
			unset ( $returned );
			unset ( $rows );
		}

		$id = parent::save ( $obj );

		if ( $id != -1 ) {
			$obj->payuser = $this->getObjectProperty ( 'payuser' );
			$obj->paysupplier = $this->getObjectProperty ( 'paysupplier' );

			$this->manageSums ( $obj->movementtype, $obj->method, $obj->amount, $obj->payuser, $obj->paysupplier, false );
		}

		return $id;
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

			$query [] = 'amount != 0';
		}

		$query = join ( ' AND ', $query );

		return parent::getByQuery ( $request, $compress, $query );
	}
}

?>
