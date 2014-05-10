<?php

/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$attributes = require_param ( 'attributes' );
$filters = require_param ( 'filter' );

global $block_begin;
global $block_end;
global $row_begin;
global $row_end;
global $head_begin;
global $head_end;
global $inrow_separator;
global $string_begin;
global $string_end;
global $content_sep;
formatting_entities ( 'csv' );

$attributes = explode ( ',', $attributes );
$filters = explode ( ',', $filters );

$user_request = null;

foreach ( $filters as $filter ) {
	if ( strncmp ( $filter, 'leaved', 6 ) == 0 ) {
		$user_request = new stdClass ();
		$user_request->privileges = -1;
	}
}

/*
	Qui prendo tutti gli utenti disponibili, e sottraggo via via gli
	elementi in funzione dei filtri
*/

$u = new User ();
$users = $u->get ( $user_request, false );

foreach ( $filters as $filter ) {
	if ( strcmp ( $filter, 'l' ) == 0 ) {
		continue;
	}
	else if ( strncmp ( $filter, 'order', 5 ) == 0 ) {
		list ( $useless, $orderid ) = explode ( ':', $filter );

		$o = new Order ();
		$o->readFromDB ( $orderid );
		$orders = get_orderuser_by_order ( $o );

		for ( $i = 0; $i < count ( $users ); $i++ ) {
			if ( $users [ $i ] == null )
				continue;

			$keep = false;
			$u = $users [ $i ];

			foreach ( $orders as $order ) {
				$a = get_actual_id ( $order->baseuser );
				if ( $a == $u->id ) {
					$keep = true;
					break;
				}
			}

			if ( $keep == false )
				$users [ $i ] = null;
		}

		unset ( $orders );
	}
	else if ( strncmp ( $filter, 'credit', 6 ) == 0 ) {
		list ( $useless, $type ) = explode ( ':', $filter );

		for ( $i = 0; $i < count ( $users ); $i++ ) {
			if ( $users [ $i ] == null )
				continue;

			$keep = true;
			$u = get_actual_user ( $users [ $i ] );
			$credit = floatval ( $u->current_balance );

			switch ( $type ) {
				case 'zero':
					$keep = ( $credit == 0 );
					break;
				case 'pluszero':
					$keep = ( $credit > 0 );
					break;
				case 'minuszero':
					$keep = ( $credit < 0 );
					break;
			}

			if ( $keep == false ) {
				$users [ $i ] = null;
			}
		}
	}
}

$output = "";

foreach ( $users as $u ) {
	if ( $u == null )
		continue;

	$data = array ();

	foreach ( $attributes as $attr ) {
		if ( property_exists ( $u, $attr ) == false ) {
			$a = '';
		}
		else {
			$a = $u->$attr;

			if ( $attr == 'deposit' || $attr == 'paying' ) {
				if ( property_exists ( $a, 'date' ) == true )
					$a = format_date ( $a->date );
				else
					$a = '';
			}
			else if ( $attr == 'birthday' || $attr == 'leaving_date' || $attr == 'lastlogin' || $attr == 'sepa_subscribe' ) {
				$a = format_date ( $a );
			}
			else if ( $attr == 'address' ) {
				$a = format_address ( $a );
			}
			else if ( $attr == 'current_balance' ) {
				$a = format_price ( $a );
			}
		}

		$data [] = $a;
	}

	$output .= $row_begin . join ( $inrow_separator, $data ) . $row_end;
	unset ( $data );
}

header ( "Content-Type: plain/text" );
header ( 'Content-Disposition: inline; filename="Utenti.csv' . '";' );
echo $output;

?>
