<?php

/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

function remove_temp_file () {
	$input = file_get_contents ( 'php://input', 1000000 );
	$obj = json_decode ( $input );
	unlink ( $obj->info->filename );
}

function decode_file ( $path ) {
	$ret = new stdClass ();
	$archive = new Archive_Tar ( $path, 'gz' );
	$files = $archive->listContent ();

	foreach ( $files as $f ) {
		$c = $archive->extractInString ( $f [ 'filename' ] );
		$xml = simplexml_load_string ( $c );

		if ( $xml == FALSE ) {
			/**
				TODO	Qui un giorno ci saranno da gestire files allegati all'archivio
			*/
			error_exit ( 'Formato file non riconosciuto' );
		}
		else {
			$roots = $xml->xpath ( '//gdxp' );

			if ( $roots == FALSE ) {
				print_r ( $xml );
				error_exit ( 'File XML non valido' );
			}
			else {
				$root = $roots [ 0 ];
				$valid = false;

				foreach ( $root->attributes () as $name => $value ) {
					if ( $name == 'protocolVersion' ) {
						if ( $value == '1.0' ) {
							$valid = true;
							break;
						}
					}
				}

				if ( $valid == false ) {
					error_exit ( 'Contenuto file non valido' );
				}
				else {
					foreach ( $root as $name => $value ) {
						$tmp = SharableFromServer::mapTag ( $name );
						if ( $tmp == null )
							error_exit ( 'Contenuto file non valido' );
						else
							$tmp->import ( &$ret, $root->$name );
					}
				}
			}
		}
	}

	return $ret;
}

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$action = get_param ( 'action', 'read' );

if ( $action == 'read' ) {
	$name = $_FILES [ 'uploadedfile' ] [ 'name' ];

	if ( !isset ( $name ) || $name == "" ) {
		error_exit ( 'File non correttamente caricato' );
	}
	else {
		$path = sys_get_temp_dir () . '/' . $name;
		move_uploaded_file ( $_FILES [ 'uploadedfile' ] [ 'tmp_name' ], $path );
		$ret = decode_file ( $path );

		$info = new stdClass ();
		$info->filename = $path;
		$ret->info = $info;

		echo json_encode ( $ret ) . "\n";
	}
}
else if ( $action == 'write' ) {
	$input = file_get_contents ( 'php://input', 1000000 );
	$obj = json_decode ( $input );

	$ret = decode_file ( $obj->info->filename );

	$sup = new Supplier ();
	$u = current_user_obj ();
	$obj->supplier->references = array ( $u->exportable () );
	$id = $sup->save ( $obj->supplier );

	$products = array ();
	$products_ids = array ();

	foreach ( $obj->products as $product ) {
		$product->available = 'true';
		$product->archived = 'false';
		$product->supplier = $id;

		$prod = new Product ();

		if ( $prod->readFromDBAlt ( array ( 'supplier' => $id, 'name' => $product->name, 'archived' => 'false' ), false ) == true ) {
			$product->previous_description = $prod->getAttribute ( 'id' )->value;

			$query = sprintf ( "UPDATE %s SET archived = true WHERE id = %s", $prod->tablename, $product->previous_description );
			query_and_check ( $query, "Impossibile aggiornare prodotto" );
		}

		$pid = $prod->save ( $product );
		$products_ids [] = $pid;

		if ( property_exists ( $obj, 'orders' ) ) {
			$p = new Product ();
			$p->readFromDB ( $pid );
			$products [] = $p->exportable ();
			unset ( $p );
		}

		unset ( $prod );
	}

	$prod = new Product ();
	$query = sprintf ( "UPDATE %s SET archived = true
				WHERE supplier = %s AND archived = false AND id NOT IN ( %s )",
				$prod->tablename, $id, join ( ', ', $products_ids ) );
	query_and_check ( $query, "Impossibile allineare tabella prodotti" );

	if ( property_exists ( $obj, 'orders' ) ) {
		$ord = new Order ();

		foreach ( $obj->orders as $order ) {
			$order->supplier = $id;
			$order->products = $products;
			$ord->save ( $order );
		}
	}

	remove_temp_file ();
	echo json_encode ( "ok" );
}
else if ( $action == 'cancel' ) {
	remove_temp_file ();
}

?>

