<?php

/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

$path = $_GET [ 'path' ];

/*
	Solo i contenuti della cartella "uploads" possono essere scaricati con questo metodo
*/
$tokens = explode ( '/', $path );
if ( $tokens [ 0 ] != 'uploads' )
	exit ( 0 );

$filesystem = '../' . $path;
$filename = $tokens [ 1 ];

header ( "Content-Type: application/octet-stream" );
header ( "Content-Disposition: disposition-type=attachment; filename=\"$filename\"" );

readfile ( $filesystem );
