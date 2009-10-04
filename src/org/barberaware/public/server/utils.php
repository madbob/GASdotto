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

require_once ( "config.php" );
require_once ( "JSON.php" );

require_once ( "FromServer.php" );
require_once ( "Session.php" );
require_once ( "SystemConf.php" );
require_once ( "CustomFile.php" );
require_once ( "GAS.php" );
require_once ( "User.php" );
require_once ( "Notification.php" );
require_once ( "Supplier.php" );
require_once ( "Measure.php" );
require_once ( "Category.php" );
require_once ( "Product.php" );
require_once ( "Order.php" );
require_once ( "ProductUser.php" );
require_once ( "OrderUser.php" );

function error_exit ( $string ) {
	$json = new Services_JSON ();
	$output = $json->encode ( "Errore: " . $string );
	print ( $output );
	exit;
}

function strbegins ( $str, $start ) {
	return ( strncmp ( $str, $start, strlen ( $start ) ) == 0 );
}

function class_name ( $name ) {
	$fragments = explode ( ".", $name );
	return $fragments [ count ( $fragments ) - 1 ];
}

/****************************************************************** db management */

function query_and_check ( $query, $error ) {
	global $db;

	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$error_code = $db->errorInfo ();
		error_exit ( $error . " executing |" . $query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
	}

	return $ret;
}

function db_row_count ( $query ) {
	global $db;

	$true_query = "SELECT COUNT(*) " . $query;
	$ret = $db->query ( $true_query );

	if ( $ret == false ) {
		$error_code = $db->errorInfo ();
		error_exit ( "Error checking for row count, executing |" . $true_query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
	}

	return $ret->fetchColumn ();
}

function escape_string ( $string ) {
	/**
		TODO	Implementare decentemente questa funzione
	*/
	return $string;
}

function last_id ( $class ) {
	global $db;
	global $dbdriver;

	if ( $dbdriver == "pgsql" )
		return $db->lastInsertId ( $class . "_id_seq" );
	else
		return $db->lastInsertId ();
}

function connect_to_the_database () {
	global $dbdriver;
	global $dbhost;
	global $dbport;
	global $dbname;
	global $dbuser;
	global $dbpassword;
	global $instance_identifier;
	global $db;

	if ( !isset ( $dbhost ) )
		$dbhost = 'localhost';

	if ( !isset ( $dbport ) ) {
		if ( $dbdriver == 'mysql' )
			$dbport = 3306;
		else if ( $dbdriver == 'pgsql' )
			$dbport = 5432;
	}

	if ( !isset ( $instance_identifier ) )
		$instance_identifier = 1;

	if ( !isset ( $dbname ) )
		$dbname = 'gasdotto_' . $instance_identifier;

	try {
		$db = new PDO ( $dbdriver . ':host=' . $dbhost . ';dbname=' . $dbname . ';port=' . $dbport, $dbuser, $dbpassword );
		return true;
	}
	catch ( PDOException $e ) {
		$json = new Services_JSON ();
		$output = $json->encode ( "no_db" );
		print ( $output );
		exit;
	}
}

/****************************************************************** authentication */

function parse_session_data () {
	global $session_key;

	$session_data = $_COOKIE [ 'gasdotto' ];
	if ( !isset ( $session_data ) )
		return false;

	list ( $session_serial, $hash ) = explode ( '-*-', $session_data );
	$session_serial = base64_decode ( $session_serial );
	$new_hash = md5 ( $session_serial . $session_key );

	if ( $hash != $new_hash )
		error_exit ( "Sessione non validata" );

	list ( $session_id, $ip, $user_agent ) = explode ( '-', $session_serial, 4 );

	if (  $ip != $_SERVER [ 'REMOTE_ADDR' ] )
		error_exit ( "IP non riconosciuto" );

	/*
		lo User-Agent non e' volutamente controllato
	*/

	return $session_id;
}

function check_session () {
	global $current_user;
	global $db;

	$current_user = -1;

	if ( connect_to_the_database () == false )
		error_exit ( "Impossibile connettersi al database" );

	$current_session_id = parse_session_data ();
	if ( $current_session_id == false )
		return false;

	$query = sprintf ( "FROM current_sessions WHERE session_id = '%s'", $current_session_id );

	if ( db_row_count ( $query ) == 0 ) {
		/*
			Se sono qui e' probabilmente perche' il cookie sulla macchina dell'utente
			non e' coerente con quanto salvato nel DB, dunque cancello suddetto
			cookie ed emetto un errore. Sul client, la classe Session lo intercetta e
			ricarica la pagina rieseguendo tutta la procedura stavolta senza cookie e
			dunque fermandosi alla schermata di login
		*/
		setcookie ( 'gasdotto', "", 0, '/', '', 0 );
		error_exit ( "Impossibile accedere alla sessione" );
	}

	$query = "SELECT username " . $query;
	$result = query_and_check ( $query, "Impossibile identificare sessione aperta" );

	$row = $result->fetchAll ( PDO::FETCH_NUM );
	/*
		Nella tabella "current_sessions" il campo "username" contiene l'ID dell'utente.
		Triste scelta di nome...
	*/
	$current_user = $row [ 0 ] [ 0 ];
	return true;
}

function perform_authentication ( $userid ) {
	global $session_key;

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
}

/*
	Le "sessioni automatiche" permettono sostanzialmente di accedere all'applicazione senza
	un login esplicito, ma per mezzo di un hash. L'utilizzo primario di questo strumento e'
	nelle notifiche via mail: per ogni utente viene generato un diverso hash, che viene
	impresso nell'URL riportato nella mail ad esso destinata, ed accedendo a tale URL
	l'autenticazione si svolge implicitamente
*/

function create_automatic_session ( $userid ) {
	/*
		tutte le sessioni piu' vecchie di una settimana sono eliminate
	*/

	$old_now = date ( "Y-m-d", ( time () - ( 60 * 60 * 24 * 7 ) ) );
	$query = sprintf ( "DELETE FROM automatic_sessions
				WHERE init < '%s'",
					$old_now );
	query_and_check ( $query, "Impossibile sincronizzare sessioni" );

	$session_id = substr ( md5 ( rand () ), 0, 10 );
	$now = date ( "Y-m-d", time () );

	$query = sprintf ( "INSERT INTO current_sessions ( session_id, init, username )
				VALUES ( '%s', DATE('%s'), %d )",
					$session_id, $now, $userid );
	query_and_check ( $query, "Impossibile salvare sessione" );

	return $session_id;
}

function retrieve_automatic_session ( $hash ) {
	$query = sprintf ( "FROM automatic_sessions WHERE session_id = '%s'", $hash );

	if ( db_row_count ( $query ) == 0 ) {
		return -1;
	}
	else {
		$query = "SELECT username " . $query;
		$result = query_and_check ( $query, "Impossibile recuperare sessione automatica" );

		$row = $result->fetchAll ( PDO::FETCH_NUM );
		return $row [ 0 ] [ 0 ];
	}
}

function current_permissions () {
	global $current_user;

	/**
		TODO	Qui si puo' evitare di leggere tutto l'utente ma solo i permessi
			direttamente dal DB
	*/

	$u = new User ();
	$u->readFromDB ( $current_user );
	$privileges = $u->getAttribute ( "privileges" );
	return $privileges->value;
}

?>
