<?php

/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class Session {
	public $user		= null;
	public $gas		= null;

	public function get ( $request, $compress ) {
		global $current_user;
		global $current_gas;

		$user = new User ();
		if ( $current_user != -1 )
			$user->readFromDB ( $current_user );
		$this->user = $user->exportable ( null, $compress );

		if ( $current_gas != -1 ) {
			/*
				Orrore e raccapriccio...
				Qui non viene usata la funzione readFromDB() come negli altri casi in quanto solo get()
				provvede ad arricchire l'oggetto GAS con i parametri extra (che sono trattati fuori dal
				database). Sarebbe cosa buona gestire tutto nello stesso modo
			*/
			$r = new stdClass ();
			$r->id = $current_gas;
			$gas = new GAS ();
			$gass = $gas->get ( $r, $compress );
			$gas = $gass [ 0 ];
		}
		else {
			$gas = GAS::getMasterGAS ();
			$gas = $gas->exportable ( null, $compress );
		}

		/*
			TODO	Se il GAS non e' selezionato, far riportare (in
				qualche modo...) il nome di tutti nella
				schermata di login
		*/

		$this->gas = $gas;

		$conf = new SystemConf ();
		$this->system = $conf->exportable ( null, $compress );

		return $this;
	}
}

?>
