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
require_once ( "createdb.php" );

class Probe extends FromServer {
	public function __construct () {
		parent::__construct ( "Probe" );

		$this->addAttribute ( "writable", "BOOLEAN" );
		$this->addAttribute ( "dbdrivers", "STRING" );
		$this->addAttribute ( "dbdriver", "STRING" );
		$this->addAttribute ( "dbuser", "STRING" );
		$this->addAttribute ( "dbpassword", "STRING" );
		$this->addAttribute ( "dbname", "STRING" );
		$this->addAttribute ( "rootpassword", "STRING" );
	}

	public function get ( $request ) {
		$this->getAttribute ( "writable" )->value = is_writable ( "./config.php" );

		$drivers = PDO::getAvailableDrivers ();
		$this->getAttribute ( "dbdrivers" )->value = join ( ";", $drivers );
		$this->getAttribute ( "dbdriver" )->value = $drivers [ 0 ];

		return $this->exportable ( $request );
	}

	public function save ( $obj ) {
		if ( is_writable ( "./config.php" ) == false )
			return 0;

		$f = fopen ( "./config.php", "w" );

		fwrite ( $f, "<?\n" );
		fwrite ( $f, sprintf ( "\$dbdriver = \"%s\";\n", $obj->dbdriver ) );
		fwrite ( $f, sprintf ( "\$dbuser = \"%s\";\n", $obj->dbuser ) );
		fwrite ( $f, sprintf ( "\$dbpassword = \"%s\";\n", $obj->dbpassword ) );
		fwrite ( $f, sprintf ( "\$dbname = \"%s\";\n", $obj->dbname ) );
		fwrite ( $f, "?>\n" );

		fclose ( $f );

		install_main_db ();

		$query = sprintf ( "UPDATE accounts SET password = '%s' WHERE username = ( SELECT id FROM Users WHERE login = 'root' )",
					md5 ( $obj->rootpassword ) );
		query_and_check ( $query, "Impossibile salvare password per utente root" );

		$query = sprintf ( "UPDATE Gas SET name = '%s', mail = '%s'", $obj->gasname, $obj->gasmail );
		query_and_check ( $query, "Impossibile salvare dati del GAS" );

		return "1";
	}
}

?>
