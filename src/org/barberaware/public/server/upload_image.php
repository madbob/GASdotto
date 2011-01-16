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

$name = $_FILES [ 'uploadedfile' ] [ 'name' ];
if ( !isset ( $name ) || $name == '' ) {
	echo '';
	exit ( 0 );
}

$tmp_name = $_FILES [ 'uploadedfile' ] [ 'tmp_name' ];
if ( !isset ( $tmp_name ) || $tmp_name == '' )
	error_exit ( 'caricamento del file non riuscito. Bada che la dimensione massima concessa è di ' . ( ini_get ( 'upload_max_filesize' ) ) );

$target_path = "uploads/" . ( unique_filesystem_name ( '../uploads/', basename ( $name ) ) );
$filesystem_path = '../' . $target_path;

if ( move_uploaded_file ( $tmp_name, $filesystem_path ) == false )
	error_exit ( 'caricamento del file non riuscito' );

list ( $width, $height, $type ) = getimagesize ( $filesystem_path );
if ( $width == 0 || $height == 0 )
	error_exit ( 'il file caricato sembra non essere una foto' );

$xscale = $width / 250;
$yscale = $height / 250;

if ( $yscale > $xscale ){
	$new_width = round ( $width * ( 1 / $yscale ) );
	$new_height = round ( $height * ( 1 / $yscale ) );
}
else {
	$new_width = round ( $width * ( 1 / $xscale ) );
	$new_height = round ( $height * ( 1 / $xscale ) );
}

$imageResized = imagecreatetruecolor ( $new_width, $new_height );

switch ( $type ) {
	case IMAGETYPE_GIF:
		$imageTmp = imagecreatefromgif ( $filesystem_path );
		break;
	case IMAGETYPE_JPEG:
		$imageTmp = imagecreatefromjpeg ( $filesystem_path );
		break;
	case IMAGETYPE_PNG:
		$imageTmp = imagecreatefrompng ( $filesystem_path );
		break;
	default:
		$imageTmp = null;
		break;
}

if ( $imageTmp == null )
	error_exit ( 'l\'immagine è in un formato non supportato. Sono ammessi solo GIF, JPEG e PNG' );

imagecopyresampled ( $imageResized, $imageTmp, 0, 0, 0, 0, $new_width, $new_height, $width, $height );

if ( imagejpeg ( $imageResized, $filesystem_path ) == true )
	echo $target_path;
else
	error_exit ( 'salvataggio della foto fallito' );

?>
