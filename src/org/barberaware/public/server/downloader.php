<?php

/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

$path = $_GET [ 'path' ];
if ( isset ( $path ) == false )
	error_exit ( "Richiesta non specificata" );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

/*
	Solo i contenuti della cartella "uploads" possono essere scaricati con questo metodo
*/
$tokens = explode ( '/', $path );
if ( $tokens [ 1 ] != 'uploads' )
	error_exit ( "Path non valido" );

$filesystem = '../' . $path;
$filename = $tokens [ 2 ];

header ( "Content-Type: application/octet-stream" );
header ( "Content-Disposition: disposition-type=attachment; filename=\"$filename\"" );

readfile ( $filesystem );
