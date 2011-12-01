<?php

/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

$type = require_param ( 'type' );
$id = require_param ( 'id' );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$obj = new $type ();
$obj->readFromDB ( $id );
list ( $name, $outputs ) = $obj->export ( array () );

$path = str_replace ( array ( '&', "\r\n", "\n", '+', ',', '/' ), '-', strtolower ( trim ( $name ) ) );
$path = sys_get_temp_dir () . '/' . $path;
$archive = new Archive_Tar ( $path, 'gz' );

$contents = array ();

foreach ( $outputs as $out ) {
	$infile = md5 ( $out );
	$child = sys_get_temp_dir () . '/' . $infile;
	$contents [] = $infile;
	file_put_contents ( $child, $out );
}

chdir ( sys_get_temp_dir () );
$archive->create ( $contents );

header ( "Content-Type: application/octet" );
header ( "Content-Disposition: disposition-type=attachment; filename=\"" . $name . ".gdxp\"" );

echo file_get_contents ( $path );

foreach ( $contents as $child )
	unlink ( $child );

unlink ( $path );

?>

