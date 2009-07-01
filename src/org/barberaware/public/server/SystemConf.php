<?

/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class SystemConf extends FromServer {
	public function __construct () {
		parent::__construct ( "SystemConf" );

		$this->addAttribute ( "has_file", "BOOLEAN" );
		$this->addAttribute ( "has_mail", "BOOLEAN" );
	}

	public function probe_mail () {
		return false;
	}

	public function probe_file_write () {
		return posix_access ( "../uploads/", POSIX_W_OK );
	}

	public function get ( $request ) {
		$arr = array ();

		$obj = new SystemConf ();
		$obj->getAttribute ( "has_mail" )->value = probe_mail ();
		$obj->getAttribute ( "has_file" )->value = probe_file_write ();

		array_push ( $arr, $obj );
		return $arr;
	}

	public function save ( $obj ) {
		/*
			Ovviamente, le proprieta' del sistema non possono essere modificate
		*/
		return -1;
	}
}