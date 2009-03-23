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

function query_and_check ( $query, $error ) {
	global $db;

	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$error_code = $db->errorInfo ();
		error_exit ( $error . " executing |" . $query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
	}

	return $ret;
}

function escape_string ( $string ) {
	/**
		TODO	Implementare decentemente questa funzione
	*/
	return $string;
}

function strbegins ( $str, $start ) {
	return ( strncmp ( $str, $start, strlen ( $start ) ) == 0 );
}

function last_id ( $class ) {
	global $db;
	global $dbdriver;

	if ( $dbdriver == "pgsql" )
		return $db->lastInsertId ( $class . "_id_seq" );
	else
		return $db->lastInsertId ();
}

function class_name ( $name ) {
	$fragments = explode ( ".", $name );
	return $fragments [ count ( $fragments ) - 1 ];
}

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

function connect_to_the_database () {
	global $dbdriver;
	global $dbhost;
	global $dbuser;
	global $dbpassword;
	global $instance_identifier;
	global $db;

	try {
		$dbname = 'gasdotto_' . $instance_identifier;
		$db = new PDO ( $dbdriver . ':host=' . $dbhost . ';dbname=' . $dbname, $dbuser, $dbpassword );
		return true;
	}
	catch ( PDOException $e ) {
		echo $e->getMessage ();
		return false;
	}
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

	$query = sprintf ( "SELECT username FROM current_sessions WHERE session_id = '%s'", $current_session_id );
	$result = query_and_check ( $query, "Impossibile identificare sessione aperta" );
	if ( $result->rowCount () == 0 )
		error_exit ( "Impossibile accedere alla sessione" );

	$row = $result->fetch ( PDO::FETCH_NUM );
	/*
		Nella tabella "current_sessions" il campo "username" contiene l'ID dell'utente.
		Triste scelta di nome...
	*/
	$current_user = $row [ 0 ];
	return true;
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
