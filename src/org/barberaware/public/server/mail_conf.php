<?php

/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

function decode_parameter_string ( $parameter, $username, $domain ) {
	if ( $parameter == '%EMAILADDRESS%' )
		return $username . '@' . $domain;
	else if ( $parameter == '%EMAILLOCALPART%' )
		return $username;
	else if ( $parameter == '%EMAILDOMAIN%' )
		return $domain;
	else
		return $parameter;
}

function search_and_assign ( $username, $domain, $ret, $xml, $array, $service ) {
	$nodes = $xml->xpath ( '//emailProvider/' . $array . '[@type=\'' . $service . '\']' );

	if ( $nodes != null ) {
		$reference = $nodes [ 0 ];

		$ret->$service = new stdClass ();
		$ret->$service->server = decode_parameter_string ( ( string ) $reference->hostname, $username, $domain );
		$ret->$service->port = ( string ) $reference->port;
		$ret->$service->username = decode_parameter_string ( ( string ) $reference->username, $username, $domain );

		if ( ( ( string ) $reference->socketType ) == 'SSL' )
			$ret->$service->ssl = "true";
		else
			$ret->$service->ssl = "false";

		unset ( $nodes );
	}
}

function check_config ( $address ) {
	list ( $username, $domain ) = explode ( '@', $address );

	$conf = file_get_contents ( "https://live.mozillamessaging.com/autoconfig/v1.1/" . $domain );
	if ( !$conf )
		return null;

	$xml = simplexml_load_string ( $conf );

	$ret = new stdClass ();

	search_and_assign ( $username, $domain, $ret, $xml, 'outgoingServer', 'smtp' );
	// search_and_assign ( $username, $domain, $ret, $xml, 'incomingServer', 'imap' );
	// search_and_assign ( $username, $domain, $ret, $xml, 'incomingServer', 'pop3' );

	unset ( $xml );
	return $ret;
}

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$perm = current_permissions ();
if ( $perm != 1 && $perm != 2 && $perm != 4 )
	error_exit ( "Permessi non sufficienti" );

$address = require_param ( 'address', "Richiesta non specificata, manca l'indirizzo mail" );

$ret = check_config ( $address );
if ( $ret != null )
	echo json_encode ( $ret ) . "\n";

?>
