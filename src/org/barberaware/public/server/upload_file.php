<?php

/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

$name = $_FILES [ 'uploadedfile' ] [ 'name' ];

if ( !isset ( $name ) || $name == "" ) {
	echo "";
}
else {
	$target_path = "/uploads/";
	$target_path = $target_path . ( unique_filesystem_name ( '../uploads/', basename ( $name ) ) );
	move_uploaded_file ( $_FILES [ 'uploadedfile' ] [ 'tmp_name' ], "../" . $target_path );
	echo $target_path;
}

?>
