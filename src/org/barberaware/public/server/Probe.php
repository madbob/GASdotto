<?php

/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
require_once ( "checkdb.php" );

class Probe extends FromServer {
	public function __construct () {
		parent::__construct ( "Probe" );

		$this->addAttribute ( "servername", "STRING" );
		$this->addAttribute ( "writable", "BOOLEAN" );
		$this->addAttribute ( "dbdrivers", "STRING" );
		$this->addAttribute ( "dbdriver", "STRING" );
		$this->addAttribute ( "dbuser", "STRING" );
		$this->addAttribute ( "dbpassword", "STRING" );
		$this->addAttribute ( "dbname", "STRING" );
		$this->addAttribute ( "dbhost", "STRING" );
		$this->addAttribute ( "gasname", "STRING" );
		$this->addAttribute ( "gasmail", "STRING" );
		$this->addAttribute ( "rootpassword", "STRING" );
	}

	public function get ( $request, $compress ) {
		$this->getAttribute ( "servername" )->value = $_SERVER [ 'SERVER_NAME' ];
		$this->getAttribute ( "newdb" )->value = true;
		$this->getAttribute ( "writable" )->value = is_writable ( "./config.php" );

		$drivers = PDO::getAvailableDrivers ();
		$valid_drivers = array ();

		foreach ( $drivers as $driver ) {
			if ( $driver == 'pgsql' )
				$valid_drivers [] = 'PostgreSQL/pgsql';
			else if ( $driver == 'mysql' )
				$valid_drivers [] = 'MySQL/mysql';
		}

		$this->getAttribute ( "dbdrivers" )->value = join (';', $valid_drivers);
		$this->getAttribute ( "dbdriver" )->value = $valid_drivers [ 0 ];

		return $this->exportable ( $request, $compress );
	}

	function random_string () {
		$length = 20;
		$characters = '0123456789abcdefghijklmnopqrstuvwxyz';
		$string = '';

		for ( $p = 0; $p < $length; $p++ )
			$string .= $characters [ mt_rand ( 0, strlen ( $characters ) ) ];

		return $string;
	}

	/*
		Funzione copiata da
		http://www.php.happycodings.com/File_Manipulation/code48.html
	*/
	function filefind ( $basedirectory, $needle ) {
		$handle = opendir ( $basedirectory );

		while ( $file = readdir ( $handle ) ) {
			if ( ( $file == "." ) || ( $file == ".." ) )
				continue;

			if ( is_dir ( $basedirectory . '/' . $file ) ) {
				$subDirResult = self::filefind ( $basedirectory . '/' . $file, $needle );

				if ( $subDirResult != "" ) {
					closedir ( $handle );
					return $subDirResult;
				}
			}

			if ( strcmp ( $file, $needle ) == 0 ) {
				closedir ( $handle );
				return $basedirectory . '/' . $needle;
			}
		}

		closedir ( $handle );
		return "";
	}

	private function write_config ( $obj ) {
		$f = fopen ( "./config.php", "w" );

		fwrite ( $f, "<?php\n" );
		fwrite ( $f, sprintf ( "\$dbdriver = \"%s\";\n", $obj->dbdriver ) );
		fwrite ( $f, sprintf ( "\$dbuser = \"%s\";\n", $obj->dbuser ) );
		fwrite ( $f, sprintf ( "\$dbpassword = \"%s\";\n", $obj->dbpassword ) );
		fwrite ( $f, sprintf ( "\$dbname = \"%s\";\n", $obj->dbname ) );
		fwrite ( $f, sprintf ( "\$dbhost = \"%s\";\n", $obj->dbhost ) );
		fwrite ( $f, sprintf ( "\$session_key = \"%s\";\n", self::random_string () ) );
		fwrite ( $f, "?>" );

		fclose ( $f );
	}

	public function save ( $obj ) {
		if ( is_writable ( "./config.php" ) == false )
			return "0";

		self::write_config ( $obj );
		install_main_db ();

		$query = sprintf ( "INSERT INTO GAS ( name, mail ) VALUES ( '%s', '%s' )", $obj->gasname, $obj->gasmail );
		query_and_check ( $query, "Impossibile salvare dati del GAS" );

		/*
			Inizializzo utente root
		*/

		$query = sprintf ( "INSERT INTO Users ( login, firstname, paying, privileges ) VALUES ( 'root', 'Root', now(), 2 )" );
		query_and_check ( $query, "Impossibile inizializzare tabella utenti" );

		$query = sprintf ( "INSERT INTO accounts ( password, username ) VALUES ( '%s', ( SELECT id FROM Users WHERE login = 'root' ) )", md5 ( $obj->rootpassword ) );
		query_and_check ( $query, "Impossibile salvare password per utente root" );

		/*
			Inizializzo categorie
		*/

		$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Non Specificato' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella category" );

		$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Frutta e Verdura' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella category" );

		$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Cosmesi' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella category" );

		$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Bevande' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella category" );

		/*
			Inizializzo unita' di misura
		*/

		$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Non Specificato', '?' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella measure" );

		$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Chili', 'kg' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella measure" );

		$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Litri', 'l' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella measure" );

		$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Pezzi', 'pezzi' )" );
		query_and_check ( $query, "Impossibile inizializzare tabella measure" );

		return "1";
	}
}

?>
