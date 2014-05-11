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

$request = null;

foreach ( $filters as $filter ) {
	if ( strncmp ( $filter, 'leaved', 6 ) == 0 ) {
		$request = new stdClass ();
		$request->hidden = -1;
	}
}

/*
	Qui prendo tutti gli utenti disponibili, e sottraggo via via gli
	elementi in funzione dei filtri
*/

$u = new Supplier ();
$suppliers = $u->get ( $request, false );

foreach ( $filters as $filter ) {
	if ( strcmp ( $filter, 'l' ) == 0 ) {
		continue;
	}
	else if ( strncmp ( $filter, 'credit', 6 ) == 0 ) {
		list ( $useless, $type ) = explode ( ':', $filter );

		for ( $i = 0; $i < count ( $suppliers ); $i++ ) {
			if ( $suppliers [ $i ] == null )
				continue;

			$keep = true;
			$u = get_actual_object ( $suppliers [ $i ], Supplier );
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

			if ( $keep == false )
				$suppliers [ $i ] = null;
		}
	}
}

$output = "";

foreach ( $suppliers as $u ) {
	if ( $u == null )
		continue;

	$data = array ();

	foreach ( $attributes as $attr ) {
		if ( property_exists ( $u, $attr ) == false ) {
			$a = '';
		}
		else {
			$a = $u->$attr;

			if ( $attr == 'address' ) {
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
header ( 'Content-Disposition: inline; filename="Fornitori.csv' . '";' );
echo $output;

?>
