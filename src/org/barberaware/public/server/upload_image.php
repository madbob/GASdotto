<?

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

require_once ( "utils.php" );

$name = $_FILES [ 'uploadedfile' ] [ 'name' ];

if ( !isset ( $name ) || $name == "" ) {
	echo "";
}
else {
	$target_path = "uploads/" . ( unique_filesystem_name ( '../uploads/', basename ( $name ) ) );
	$filesystem_path = '../' . $target_path;
	move_uploaded_file ( $_FILES [ 'uploadedfile' ] [ 'tmp_name' ], $filesystem_path );

	list ( $width, $height, $type ) = getimagesize ( $filesystem_path );
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
		exit ( 0 );

	imagecopyresampled ( $imageResized, $imageTmp, 0, 0, 0, 0, $new_width, $new_height, $width, $height );

	if ( imagejpeg ( $imageResized, $filesystem_path ) == true )
		echo $target_path;
}

?>
