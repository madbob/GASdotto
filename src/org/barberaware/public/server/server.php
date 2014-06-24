<?php

/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

$action = require_param ( 'action' );
$ret = -1;
$input = file_get_contents ( 'php://input', 1000000 );
$obj = json_decode ( $input );
$type = escape_string ( $obj->type );

if ( $type == "Probe" ) {
	$ret = new Probe ();

	switch ( $action ) {
		case "get":
			$ret = $ret->get ( $obj, false );
			break;

		case "save":
			$ret = $ret->save ( $obj );
			break;

		default:
			error_exit ( "Richiesta non identificata" );
			break;
	}
}

else if ( $type == "Reset" ) {
	global $current_gas;

	$mail = $obj->mail;
	if ( !isset ( $mail ) )
		exit ( 0 );

	connect_to_the_database ();

	$query = sprintf ( "FROM Users WHERE mail = '%s' OR mail2 = '%s'", $mail, $mail );
	if ( db_row_count ( $query ) == 1 ) {
		$password = ( string ) mt_rand ( 1000000, 9999999 );

		$query = sprintf ( "UPDATE accounts SET reset = '%s'
					WHERE username = ( SELECT id FROM Users WHERE mail = '%s' OR mail2 = '%s' )",
						md5 ( $password ), $mail, $mail );
		query_and_check ( $query, "Impossibile salvare nuova password" );

		/*
			Qui cerco il GAS di riferimento per l'utente con
			il dato indirizzo mail, e lo metto nella
			variabile globale $current_gas in modo che sia
			accessibile anche altrove (nello specifico, in
			my_send_mail() )
		*/

		$query = sprintf ( "SELECT gas FROM ACL
					WHERE target_type = 'User' AND
						target_id = ( SELECT id FROM Users WHERE mail = '%s' OR mail2 = '%s' )",
					$mail, $mail );
		$returned = query_and_check ( $query, "Impossibile recuperare GAS di riferimento" );
		$row = $returned->fetchAll ( PDO::FETCH_ASSOC );
		$current_gas = $row [ 0 ] [ 'gas' ];

		$gas = current_gas ();

		$notice = sprintf ( "E' stata ricevuta una richiesta per resettare la tua password sull'istanza GASdotto di %s.\n", $gas->getAttribute ( 'name' )->value );
		$notice .= sprintf ( "Se non hai espressamente richiesto una nuova password, ignora assolutamente questa mail.\n" );
		$notice .= sprintf ( "\n" );
		$notice .= sprintf ( "La tua nuova password è '%s' (da scrivere senza le virgolette!).\n", $password );
		$notice .= sprintf ( "Con essa potrai accedere nuovamente alle informazioni del tuo GAS. Ti suggeriamo di cambiarla al più presto dal pannello 'Profilo Utente'.\n" );
		$notice .= sprintf ( "\n" );

		my_send_mail ( array ( $mail ), "Password resettata", true, $notice );
	}
}

else {
	if ( check_session () == false ) {
		switch ( $action ) {
			case "get":
				switch ( $type ) {
					case "Login":
						$ret = new User ();

						$name = escape_string ( $obj->username );
						$pwd = escape_string ( $obj->password );

						if ( property_exists ( $obj, 'permanent' ) && $obj->permanent == 'true' )
							$permanent = true;
						else
							$permanent = false;

						$query = sprintf ( "FROM Users WHERE login = '%s'", $name );
						if ( db_row_count ( $query ) == 1 ) {
							$query = "SELECT id, privileges " . $query;
							$returned = query_and_check ( $query, "Impossibile recuperare utente" );
							$row = $returned->fetchAll ( PDO::FETCH_ASSOC );
							unset ( $returned );
							$privileges = $row [ 0 ] [ 'privileges' ];

							/*
								Gli utenti con account marcato come "cessato" non
								possono accedere
							*/
							if ( $privileges != 3 ) {
								$query = sprintf ( "FROM accounts WHERE username = %d", $row [ 0 ] [ 'id' ] );
								if ( db_row_count ( $query ) == 1 ) {
									$login_ok = false;

									$query = "SELECT * " . $query;
									$returned = query_and_check ( $query, "Impossibile validare utente" );
									$row = $returned->fetchAll ( PDO::FETCH_ASSOC );
									unset ( $returned );

									$id = $row [ 0 ] [ 'username' ];

									if ( md5 ( $pwd ) == $row [ 0 ] [ 'password' ] ) {
										$login_ok = true;
									}
									else if ( $row [ 0 ] [ 'reset' ] != '' && md5 ( $pwd ) == $row [ 0 ] [ 'reset' ] ) {
										$login_ok = true;

										$query = sprintf ( "UPDATE accounts SET password = reset WHERE username = %d", $id );
										query_and_check ( $query, "Impossibile settare nuova password" );
									}

									if ( $login_ok == true ) {
										/*
											Se era stato chiesto un reset della password,
											rimetto le cose a posto
										*/
										if ( $row [ 0 ] [ 'reset' ] != '' ) {
											$query = sprintf ( "UPDATE accounts SET reset = '' WHERE username = %d", $id );
											query_and_check ( $query, "Impossibile resettare password temporanea" );
										}

										/*
											Controllo se l'istanza è in modalità di
											accesso limitato
										*/
										$query = "SELECT emergency_access FROM GAS WHERE id =
												(SELECT gas FROM ACL
													WHERE target_type = 'User' AND target_id = $id)";
										$result = query_and_check ( $query, "Impossibile recuperare GAS" );
										$row = $result->fetchAll ( PDO::FETCH_NUM );
										if ( $row [ 0 ] [ 0 ] == true && $privileges != 2 )
											error_exit ( "Modalità ad accesso limitato" );

										$userid = $id;
										perform_authentication ( $userid, $permanent );
										$ret->readFromDB ( $userid );
										$ret->registerLogin ();
									}
								}
							}
						}

						$ret = $ret->exportable ();
						break;

					case "Session":
						$ret = new Session ();
						$ret->get ( $obj, false );
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
				switch ( $type ) {
					case "Logout":
						setcookie ( 'gasdotto', "", 0, '/', '', 0 );
						$ret = 1;
						break;

					default:
						$type = escape_string ( $obj->type );
						$class = class_name ( $type );
						$ret = new $class;
						$ret = $ret->get ( $obj, true );
						break;
				}

				break;

			case "save":
				$class = class_name ( $type );
				$ret = new $class;
				$ret = $ret->save ( $obj );
				break;

			case "destroy":
				/**
					TODO	Eseguire controllo permessi su singole classi che possono essere
						eliminate dai diversi tipi di utente
				*/

				$class = class_name ( $type );
				$ret = new $class;
				$ret = $ret->destroy ( $obj );
				break;

			default:
				error_exit ( "Richiesta non identificata" );
				break;
		}
	}
}

header ( 'Cache-Control: no-cache, must-revalidate' );
header ( 'Expires: Mon, 26 Jul 1997 05:00:00 GMT' );
header ( 'Content-type: application/json' );
echo json_encode ( $ret ) . "\n";

?>
