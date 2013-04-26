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

abstract class SharableFromServer extends FromServer {
	abstract public function export ( $options );
	abstract public static function import ( &$ref, $contents );

	public static function header () {
		$date = date ('d/m/Y');

		return <<<HEAD
<?xml version="1.0" encoding="UTF-8"?>
<gdxp protocolVersion="1.0" creationDate="$date" applicationSignature="GASdotto">

HEAD;
	}

	public static function footer () {
		return "</gdxp>";
	}

	public static function mapTag ( $tag ) {
		switch ( $tag ) {
			case 'supplier':
				return new Supplier ();
				break;

			case 'orders':
				return new Order ();
				break;

			case 'products':
				return new Product ();
				break;

			case 'variants':
				return new ProductVariant ();
				break;

			default:
				return null;
		}
	}
}

