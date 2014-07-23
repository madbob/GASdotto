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

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$ret = null;
$type = require_param ( 'type' );

switch ( $type ) {
	case 'fix':
		$offset = require_param ( 'offset' );
		$tmp = new BankMovement ();
		echo $tmp->fix ( $offset );
		break;

	case 'close':
		$tmp = new BankMovement ();
		echo $tmp->close ();
		break;
}

exit ( 0 );

