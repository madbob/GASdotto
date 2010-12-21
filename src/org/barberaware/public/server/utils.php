<?php

/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
require_once ( "Mail.php" );
require_once ( "Mail/mime.php" );

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
require_once ( "ProductVariant.php" );
require_once ( "ProductVariantValue.php" );
require_once ( "Order.php" );
require_once ( "ProductUser.php" );
require_once ( "ProductUserVariant.php" );
require_once ( "ProductUserVariantComponent.php" );
require_once ( "OrderUserFriend.php" );
require_once ( "OrderUser.php" );
require_once ( "Probe.php" );

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

function search_in_array ( $array, $val ) {
	for ( $i = 0; $i < count ( $array ); $i++ )
		if ( $array [ $i ] == $val )
			return $i;

	return -1;
}

/****************************************************************** db management */

function query_and_check ( $query, $error ) {
	global $db;

	$ret = $db->query ( $query );
	if ( $ret == false ) {
		require_once ( "checkdb.php" );
		check_db_schema ();

		$ret = $db->query ( $query );
		if ( $ret == false ) {
			$error_code = $db->errorInfo ();
			error_exit ( $error . " executing |" . $query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
		}
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

function db_date_plus_one () {
	global $dbdriver;

	if ( $dbdriver == "pgsql" )
		return " + INTERVAL '1 DAY' ";
	else
		return " + INTERVAL 1 DAY ";
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
	global $db;

	if ( !isset ( $dbhost ) )
		$dbhost = 'localhost';

	if ( !isset ( $dbport ) ) {
		if ( $dbdriver == 'mysql' )
			$dbport = 3306;
		else if ( $dbdriver == 'pgsql' )
			$dbport = 5432;
	}

	try {
		$db = new PDO ( $dbdriver . ':host=' . $dbhost . ';dbname=' . $dbname . ';port=' . $dbport, $dbuser, $dbpassword );

		$query = sprintf ( "SELECT * FROM GAS" );
		if ( $db->query ( $query ) == false )
			throw new PDOException ();

		return true;
	}
	catch ( PDOException $e ) {
		$json = new Services_JSON ();
		$output = $json->encode ( "no_db" );
		print ( $output );
		exit;
	}
}

/****************************************************************** formatting */

function comma_format ( $a ) {
	/*
		Cast esplicito: e' successo che il cast implicito provocasse problemi
	*/
	$a = ( string ) $a;

	$decimal = strlen ( strstr ( $a, '.' ) );
	if ( $decimal != 0 )
		return number_format ( $a, $decimal - 1, ',', '' );
	else
		return $a;
}

function format_price ( $price, $symbol = true ) {
	if ( $symbol == true )
		return "€ " . number_format ( $price, 2, ',', '' );
	else
		return number_format ( $price, 2, ',', '' );
}

function format_date ( $dbdate ) {
	$months = array ( 'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno', 'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre' );
	list ( $year, $month, $day ) = explode ( '-', $dbdate );
	return $day . ' ' . ( $months [ $month - 1 ] ) . ' ' . $year;
}

function sort_product_by_name ( $first, $second ) {
	return strcmp ( $first->getAttribute ( "name" )->value, $second->getAttribute ( "name" )->value );
}

function sort_orders_by_user ( $first, $second ) {
	return strcmp ( $first->baseuser->surname, $second->baseuser->surname );
}

function get_product_name ( $product ) {
	$code = $product->getAttribute ( "code" )->value;
	if ( $code != '' )
		$code = ' - ' . $code;

	return ( $product->getAttribute ( "name" )->value ) . $code;
}

function get_product_quantity_stocks ( $product, $quantity ) {
	if ( $quantity > 0 ) {
		$stock = $product->getAttribute ( "stock_size" )->value;

		if ( $stock != 0 )
			return ( comma_format ( round ( $quantity, 2 ), false ) ) . ' (' . ( ceil ( $quantity / $stock ) ) . ' confezioni da ' . $stock . ')';
	}

	return comma_format ( round ( $quantity, 2 ), false );
}

function sum_percentage ( $original, $perc ) {
	if ( $perc == null || $perc == '' || $perc == '0' ) {
		return $original;
	}
	else if ( strstr ( $perc, '%' ) ) {
		list ( $integer ) = explode ( '%', $perc );
		$sum = ( $original * $integer ) / 100;
		return $original + $sum;
	}
	else {
		return $original + $perc;
	}
}

function ellipse_string ( $string, $len ) {
	if ( strlen ( $string ) > $len ) {
		$string = substr ( $string, 0, $len - 2 );
		$string .= '...';
	}

	return $string;
}

function sort_products_on_products ( $products, $user_products ) {
	$proxy = array ();

	for ( $e = 0; $e < count ( $products ); $e++ ) {
		$prod = $products [ $e ];

		for ( $a = 0; $a < count ( $user_products ); $a++ ) {
			$prod_user = $user_products [ $a ];

			if ( $prod->getAttribute ( "id" )->value == $prod_user->product ) {
				$proxy [] = $prod_user;
				break;
			}
		}
	}

	return $proxy;
}

/****************************************************************** shortcuts */

function get_orderuser_by_order ( $order ) {
	$id = $order->getAttribute ( 'id' )->value;

	$request = new stdClass ();
	$request->baseorder = $id;

	/*
		Questo e' per evitare che lo script ricarichi per intero l'ordine di riferimento e tutti
		i prodotti per ogni singolo OrderUser
	*/
	$request->has_Order = array ( $id );

	$request->has_Product = array ();
	$products = $order->getAttribute ( 'products' )->value;

	for ( $i = 0; $i < count ( $products ); $i++ ) {
		$prod = $products [ $i ];
		$request->has_Product [] = $prod->getAttribute ( 'id' )->value;
	}

	$order_user_proxy = new OrderUser ();
	$ret = $order_user_proxy->get ( $request, false );

	unset ( $request );
	unset ( $order_user_proxy );

	return $ret;
}

/****************************************************************** files management */

function unique_filesystem_name ( $folder, $name ) {
	$i = 0;
	$final_name = $name;

	while ( file_exists ( $folder . '/' . $final_name ) ) {
		$final_name = $i . '_' . $name;
		$i++;
	}

	return $final_name;
}

/****************************************************************** mail */

function my_send_mail ( $recipients, $subject, $public, $body, $html = null ) {
	global $current_user;

	$gas = new GAS ();
	$gas->readFromDB ( 1 );
	$mailconf = $gas->getAttribute ( 'mail_conf' )->value;
	$name = $gas->getAttribute ( 'name' )->value;
	unset ( $gas );

	list ( $from, $username, $password, $host, $port, $ssl ) = explode ( '::', $mailconf );

	if ( $ssl == 'true' )
		$host = 'ssl://' . $host;

	$mysubject = '[' . $name . '] ' . $subject;
	$headers = array ( 'From' => $from, 'Subject' => $mysubject );

	if ( $current_user != -1 ) {
		$sender = new User ();
		$sender->readFromDB ( $current_user );
		$headers [ 'Reply-To' ] = $sender->getAttribute ( 'mail' )->value;
		unset ( $sender );
	}

	if ( $html != null ) {
		$message = new Mail_mime ();
		$message->setTXTBody ( $body );
		$message->setHTMLBody ( $html );
		$body = $message->get ( array ( 'html_charset' => 'UTF-8', 'text_charset' => 'UTF-8' ) );
		$headers = $message->headers ( $headers );
	}
	else {
		$headers [ 'Content-Type' ] = "text/plain; charset=\"UTF-8\"";
	}

	$smtp = Mail::factory ( 'smtp', array ( 'host' => $host, 'port' => $port, 'auth' => true, 'username' => $username, 'password' => $password ) );

	$tot = count ( $recipients );

	/*
		Se ci sono piu' di 50 destinatari, mando piu' mail, li divido in porzioni da 50.
		Questo perche' pressoche' tutti i providers di posta impongono un limite sul
		numero di destinatari (oltre il quale la mail viene bloccata), il quale e' assai
		variabile: alcuni lo hanno a 100, altri a 200. Per scaramanzia mi tengo basso nel
		conto
	*/
	if ( $tot > 50 ) {
		for ( $i = 0; $i < $tot; $i += 50 ) {
			$end = $i + 50;
			if ( $end > $tot )
				$end = $tot;

			$recipients_part = array_slice ( $recipients, $i, $end );

			if ( $public == true )
				$headers [ 'To' ] = '<' . ( join ( '>, <', $recipients_part ) ) . '>';

			$ret = $smtp->send ( $recipients_part, $headers, $body );

			if ( PEAR::isError ( $ret ) )
				return $ret->getMessage ();
		}

		return null;
	}
	else {
		if ( $public == true )
			$headers [ 'To' ] = '<' . ( join ( '>, <', $recipients ) ) . '>';

		$ret = $smtp->send ( $recipients, $headers, $body );

		if ( PEAR::isError ( $ret ) )
			return $ret->getMessage ();
		else
			return null;
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

	if ( $hash != $new_hash ) {
		setcookie ( 'gasdotto', "", 0, '/', '', 0 );
		error_exit ( "Sessione non validata" );
	}

	list ( $session_id, $ip, $user_agent ) = explode ( '-', $session_serial, 4 );

	if (  $ip != $_SERVER [ 'REMOTE_ADDR' ] ) {
		setcookie ( 'gasdotto', "", 0, '/', '', 0 );
		error_exit ( "IP non riconosciuto" );
	}

	/*
		lo User-Agent non e' volutamente controllato
	*/

	return $session_id;
}

function check_session () {
	global $current_user;
	global $db;
	global $cache;

	$current_user = -1;
	$cache = array ();

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
