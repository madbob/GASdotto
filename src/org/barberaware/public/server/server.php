<?

/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

$action = $_GET [ 'action' ];

if ( isset ( $action ) == false )
	error_exit ( "Richiesta non specificata" );

$ret = -1;
$input = file_get_contents ( 'php://input', 1000000 );
$json = new Services_JSON ();
$obj = $json->decode ( $input );

if ( check_session () == false ) {
	switch ( $action ) {
		case "get":
			$type = escape_string ( $obj->type );

			switch ( $type ) {
				case "Login":
					$ret = new User ();

					$name = escape_string ( $obj->username );
					$pwd = escape_string ( $obj->password );
					$query = sprintf ( "SELECT * FROM accounts WHERE username = ( SELECT id FROM Users WHERE login = '%s' )", $name );
					$returned = query_and_check ( $query, "Impossibile validare utente" );

					if ( $returned->rowCount () == 1 ) {
						$row = $returned->fetch ( PDO::FETCH_ASSOC );

						if ( md5 ( $pwd ) == $row [ 'password' ] ) {
							$userid = $row [ 'username' ];

							/*
								tutte le sessioni piu' vecchie di una settimana sono eliminate
							*/

							$old_now = date ( "Y-m-d", ( time () - ( 60 * 60 * 24 * 7 ) ) );
							$query = sprintf ( "DELETE FROM current_sessions
										WHERE init < '%s' OR
										username = %d",
											$old_now, $userid );
							query_and_check ( $query, "Impossibile sincronizzare sessioni" );

							$session_id = substr ( md5 ( time () ), 0, 20 );
							$now = date ( "Y-m-d", time () );

							$query = sprintf ( "INSERT INTO current_sessions ( session_id, init, username )
										VALUES ( '%s', DATE('%s'), %d )",
											$session_id, $now, $userid );
							query_and_check ( $query, "Impossibile salvare sessione" );

							$session_serial = $session_id . '-' . $_SERVER [ 'REMOTE_ADDR' ];
							$session_hash = md5 ( $session_serial . $session_key );
							$session_cookie = base64_encode ( $session_serial ) . '-*-' . $session_hash;

							if ( setcookie ( 'gasdotto', $session_cookie, 0, '/', '', 0 ) == false )
								error_exit ( "Impossibile settare il cookie" );

							$ret->readFromDB ( $userid );
						}
					}

					$ret = $ret->exportable ();
					break;

				case "Session":
					$ret = new Session ();
					$ret->get ( $obj );
					break;

				default:
					error_exit ( "Accesso non autorizzato" );
					break;
			}

			break;
	}
}
else {
	switch ( $action ) {
		case "get":
			$type = escape_string ( $obj->type );

			switch ( $type ) {
				case "Logout":
					setcookie ( 'gasdotto', "", 0, '/', '', 0 );
					$ret = 1;
					break;

				default:
					$type = escape_string ( $obj->type );
					$class = class_name ( $type );
					$ret = new $class;
					$ret = $ret->get ( $obj );
					break;
			}

			break;

		case "save":
			$type = escape_string ( $obj->type );
			$class = class_name ( $type );
			$ret = new $class;
			$ret = $ret->save ( $obj );
			break;

		case "destroy":
			/**
				TODO	Eseguire controllo permessi su singole classi che possono essere
					eliminate dai diversi tipi di utente
			*/

			$type = escape_string ( $obj->type );
			$class = class_name ( $type );
			$ret = new $class;
			$ret = $ret->destroy ( $obj );
			break;

		default:
			error_exit ( "Richiesta non identificata" );
			break;
	}
}

echo $json->encode ( $ret );

?>
