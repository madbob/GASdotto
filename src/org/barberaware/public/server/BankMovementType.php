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

/*
	Questa classe *non* viene usata da nessuna parte, esiste solo nella
	misura in cui fornisce una legenda interna al database per decodificare
	gli identificativi dei movimenti contabili
*/

class BankMovementType extends FromServer {
	public function __construct () {
		parent::__construct ( "BankMovementType" );

		$this->addAttribute ( "identifier", "INTEGER" );
		$this->addAttribute ( "name", "STRING" );
	}

	public function install () {
		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 0;
		$a->getAttribute ( 'name' )->value = "Deposito cauzione socio del GAS";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 1;
		$a->getAttribute ( 'name' )->value = "Restituzione cauzione socio del GAS";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 2;
		$a->getAttribute ( 'name' )->value = "Versamento della quota annuale da parte di un socio";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 3;
		$a->getAttribute ( 'name' )->value = "Pagamento di un ordine da parte di un socio";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 4;
		$a->getAttribute ( 'name' )->value = "Pagamento dell'ordine presso il fornitore";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 5;
		$a->getAttribute ( 'name' )->value = "Deposito di credito da parte di un socio";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 6;
		$a->getAttribute ( 'name' )->value = "Acquisto o spesa generica da parte del GAS";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 7;
		$a->getAttribute ( 'name' )->value = "Trasferimento interno al GAS, dalla cassa al conto o viceversa";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 8;
		$a->getAttribute ( 'name' )->value = "Prelievo generico";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 9;
		$a->getAttribute ( 'name' )->value = "Versamento generico";
		$a->save ( $a->exportable () );
		unset ( $a );

		$a = new BankMovementType ();
		$a->getAttribute ( 'identifier' )->value = 10;
		$a->getAttribute ( 'name' )->value = "Arrotondamento/sconto fornitore";
		$a->save ( $a->exportable () );
		unset ( $a );
	}
}

?>
