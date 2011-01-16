<?php

/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

		$user = new User ();
		if ( $current_user != -1 )
			$user->readFromDB ( $current_user );
		$this->user = $user->exportable ( null, $compress );

		$gas = new GAS ();
		$gas->readFromDB ( 1 );
		$this->gas = $gas->exportable ( null, $compress );

		$conf = new SystemConf ();
		$this->system = $conf->exportable ( null, $compress );

		return $this;
	}
}

?>
