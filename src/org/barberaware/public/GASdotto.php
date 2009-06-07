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

require_once ( "server/utils.php" );

$link = $_GET [ 'internal' ];
if ( isset ( $link ) ) {
	/*
		Il cookie per l'URL di ingresso scade dopo 10 minuti, e comunque viene rimosso
		dal client una volta letto
	*/
	setcookie ( "initial_url", $link, time () + ( 60 * 10 ) );
}

$auth = $_GET [ 'auth' ];
if ( ( isset ( $auth ) ) && ( check_session () == false ) ) {
	$user = retrieve_automatic_session ( $auth );

	if ( $user )
		error_exit ( "Impossibile autenticare la sessione, procedere con il login" );
	else
		perform_authentication ( $user );
}

$host = $_SERVER [ 'HTTP_HOST' ];
$uri = rtrim(dirname ( $_SERVER [ 'PHP_SELF' ] ), '/\\' );
$extra = 'GASdotto.html';
header ( "Location: http://$host$uri/$extra" );

?>
