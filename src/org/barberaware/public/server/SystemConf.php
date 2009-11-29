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

		$GASDOTTO_VERSION = "0";
		$GASDOTTO_COMMIT = "0";
		$GASDOTTO_BUILT = "1970-01-01";

		$this->addAttribute ( "gasdotto_main_version", "STRING" );
		$this->addAttribute ( "gasdotto_commit_version", "STRING" );
		$this->addAttribute ( "gasdotto_build_date", "DATE" );
		$this->addAttribute ( "has_file", "BOOLEAN" );
		$this->addAttribute ( "has_mail", "BOOLEAN" );

		$this->getAttribute ( "id" )->value = "1";
		$this->getAttribute ( "gasdotto_main_version" )->value = $GASDOTTO_VERSION;
		$this->getAttribute ( "gasdotto_commit_version" )->value = $GASDOTTO_COMMIT;
		$this->getAttribute ( "gasdotto_build_date" )->value = $GASDOTTO_BUILT;
		$this->getAttribute ( "has_mail" )->value = $this->probe_mail ();
		$this->getAttribute ( "has_file" )->value = $this->probe_file_write ();
	}

	private function probe_mail () {
		return false;
	}

	private function probe_file_write () {
		return posix_access ( "../uploads/", POSIX_W_OK );
	}

	public function get ( $request ) {
		$arr = array ();
		$obj = new SystemConf ();
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
