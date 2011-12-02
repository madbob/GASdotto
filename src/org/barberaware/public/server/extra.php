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

global $extraroot;
$extraroot = "https://github.com/madbob/GASdotto/tree/master/extra";

/*
	Preso da:
	http://www.infosys.tuwien.ac.at/staff/treiber/blog/2011/02/15/php-allow_url_fopen-workaround/
*/
function file_get_contents_remote ( $url ) {
	if ( !ini_get ( "allow_url_fopen" ) ) {
		$f = curl_init ( $url );
		curl_setopt ( $f, CURLOPT_RETURNTRANSFER, true );
		$c = curl_exec ( $f );
		curl_close ( $f );
		return $c;
	}

	if ( !function_exists ( "file_get_contents" ) ) {
		function file_get_contents ( $path ) {
			$c = "";
			$f = fopen ( $path, "r" );

			while ( !feof ( $f ) )
				$c .= fread ( $f, 1024 );

			fclose ( $f );
			return $c;
		}
	}

	return file_get_contents ( $url );
}

function check_package ( $file, $url ) {
	global $extraroot;

	$paths = explode ( ':', ini_get ( 'include_path' ) );

	foreach ( $paths as $path ) {
		if ( file_exists ( "$path/$file" ) == true ) {
			require_once ( "$path/$file" );
			return;
		}
	}

	if ( file_exists ( 'extra' )  == false )
		if ( mkdir ( 'extra' ) == false ) {
			$output = json_encode ( "Errore: impossibile creare cartella per pacchetti opzionali" );
			print ( $output );
			exit;
		}

	$contents = file_get_contents_remote ( $url );
	$contents = explode ( "\n", $contents );

	foreach ( $contents as $content ) {
		if ( $content == '' )
			continue;

		$c = file_get_contents_remote ( "$extraroot/$content" );

		$folder = dirname ( $url );
		if ( $folder != '.' )
			mkdir ( "extra/$folder", 0777, true );

		file_put_contents ( 'extra/' . $content, $c );
	}

	require_once ( "extra/$file" );
}

if ( file_exists ( 'extra' ) )
	ini_set ( 'include_path', ini_get ( 'include_path' ) . ':' . getcwd () . '/extra' );

check_package ( 'Mail.php', "$extraroot/Mail.txt" );
check_package ( 'Mail/mime.php', "$extraroot/Mail.txt" );
check_package ( 'tcpdf/tcpdf.php', "$extraroot/tcpdf.txt" );
check_package ( 'Archive/Tar.php', "$extraroot/Tar.txt" );

?>

