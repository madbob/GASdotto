<?php

/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class ShippingPlace extends FromServer {
	public function __construct () {
		parent::__construct ( "ShippingPlace" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "address", "ADDRESS" );
	}

	public static function getDefault () {
		/*
			TODO	Implementazione temporanea, occorre un criterio
				piu' furbo per determinare il luogo di consegna
				di default
		*/

		$tmp = new ShippingPlace ();
		$places = $tmp->get ( null, false );

		if ( count ( $places ) != 0 ) {
			$ret = $places [ 0 ];
			$tmp->readFromDB ( $ret->id );
			$ret = $tmp;
		}
		else {
			$ret = null;
		}

		unset ( $places );
		return $ret;
	}
}

?>
