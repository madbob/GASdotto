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

require_once ( "utils.php" );

/*
	Non usare la funzione query_and_check() in questo file, essa richiama check_db_schema()
	se la query fallisce dunque facilmente finisce in un loop infinito
*/
function local_query_and_check ( $query, $error ) {
	global $db;

	$ret = $db->query ( $query );
	if ( $ret == false ) {
		$error_code = $db->errorInfo ();
		error_exit ( $error . " executing |" . $query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
	}

	return $ret;
}

function linking_table_query ( $obj, $name, $objtype ) {
	$tmp = new $objtype;
	return sprintf ( 'CREATE TABLE %s_%s ( parent int references %s (id), target int references %s (id) )',
				$obj->tablename, $name, $obj->tablename, $tmp->tablename );
}

function update_column ( $table, $column, $type ) {
	global $db;
	global $dbdriver;

	if ( $dbdriver == 'pgsql' )
		$query = sprintf ( 'ALTER TABLE %s ALTER COLUMN %s TYPE %s', $obj->tablename, $attr->name, $change );
	else
		$query = sprintf ( 'ALTER TABLE %s CHANGE %s %s %s', $obj->tablename, $attr->name, $attr->name, $change );

	local_query_and_check ( $query, "Impossibile modificare tipo della colonna" );
}

function create_table_class ( $obj ) {
	global $db;
	global $dbdriver;

	$columns = array ();
	$extras = array ();

	$query = sprintf ( 'CREATE TABLE %s ( id serial primary key, ', $obj->tablename );

	for ( $a = 0; $a < count ( $obj->attributes ); $a++ ) {
		$attr = $obj->attributes [ $a ];
		if ( $attr->name == 'id' )
			continue;

		list ( $type, $objtype ) = explode ( "::", $attr->type );

		switch ( $type ) {
			case "STRING":
			case "PERCENTAGE":
			case "ADDRESS":
				$columns [] =  $attr->name . ' varchar (500)';
				break;

			case "INTEGER":
				$columns [] =  $attr->name . ' int default 0';
				break;

			case "OBJECT":
				$tmp = new $objtype;
				$columns [] =  $attr->name . ' int references ' . $tmp->tablename . ' (id)';
				break;

			case "FLOAT":
				$columns [] =  $attr->name . ' float default 0';
				break;

			case "DATE":
				$columns [] =  $attr->name . ' date';
				break;

			case "BOOLEAN":
				$columns [] =  $attr->name . ' boolean';
				break;

			case "ARRAY":
				$tmp = new $objtype;
				$extras [] = linking_table_query ( $obj, $attr->name, $objtype );
				break;

			default:
				break;
		}
	}

	$query .= ( join ( ', ', $columns ) ) . ' )';
	local_query_and_check ( $query, "Impossibile creare nuova tabella" );

	foreach ( $extras as $extra )
		local_query_and_check ( $extra, "Impossibile eseguire query di supporto a creazione nuova tabella" );
}

function test_class ( $class ) {
	global $db;
	global $dbdriver;

	$obj = new $class;

	$query = 'SELECT * FROM ' . $obj->tablename;
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		create_table_class ( $obj );
	}
	else {
		for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
			$meta = $ret->getColumnMeta ( $i );
			if ( $meta [ 'name' ] == 'id' )
				continue;

			$found = false;
			$change = null;
			$t = $meta [ 'native_type' ];

			for ( $a = 0; $a < count ( $obj->attributes ); $a++ ) {
				$attr = $obj->attributes [ $a ];

				if ( $meta [ 'name' ] == $attr->name ) {
					$found = true;

					if ( strstr ( $attr->type, '::' ) == false )
						$type = $attr->type;
					else
						list ( $type, $objtype ) = explode ( "::", $attr->type );

					switch ( $type ) {
						case "STRING":
						case "PERCENTAGE":
						case "ADDRESS":
							if ( ( $dbdriver == 'pgsql' && $t != 'varchar' ) || ( $dbdriver == 'mysql' && $t != 'VAR_STRING' ) )
								$change = 'varchar (500)';
							break;

						case "INTEGER":
							if ( ( $dbdriver == 'pgsql' && $t != 'int4' ) || ( $dbdriver == 'mysql' && $t != 'LONG' ) )
								$change = 'int';
							break;

						case "OBJECT":
							if ( ( $dbdriver == 'pgsql' && $t != 'int4' ) || ( $dbdriver == 'mysql' && $t != 'LONG' ) ) {
								$tmp = new $objtype;
								$change = sprintf ( 'int references %s (id)', $tmp->tablename );
							}

							break;

						case "FLOAT":
							if ( ( $dbdriver == 'pgsql' && $t != 'float8' ) || ( $dbdriver == 'mysql' && $t != 'FLOAT' ) )
								$change = 'float';
							break;

						case "DATE":
							if ( ( $dbdriver == 'pgsql' && $t != 'date' ) || ( $dbdriver == 'mysql' && $t != 'DATE' ) )
								$change = 'date';
							break;

						case "BOOLEAN":
							/*
								Ci deve essere una anomalia nel driver PDO di MySQL:
								se ho una colonna rappresentante un booleano, tale
								informazione non viene riportata tra le informazioni
							*/
							if ( ( $dbdriver == 'pgsql' && $t != 'bool' ) || ( $dbdriver == 'mysql' && isset ( $t ) ) )
								$change = 'boolean';
							break;

						case "ARRAY":
							$query = sprintf ( 'SELECT * FROM %s_%s' . $obj->tablename, $attr->name );
							$subret = $db->query ( $query );

							if ( $subret == false ) {
								$query = linking_table_query ( $obj, $attr->name, $objtype );
								local_query_and_check ( $query, "Impossibile creare tabella di collegamento" );
							}

							break;

						default:
							break;
					}

					break;
				}
			}

			if ( $found == false ) {
				$query = sprintf ( 'ALTER TABLE %s DROP COLUMN %s', $obj->tablename, $attr->name );
				local_query_and_check ( $query, "Impossibile eliminare colonna non piu' usata" );
			}
			if ( $change != null ) {
				update_column ( $obj->tablename, $attr->name, $change );
			}
		}
	}
}

function test_static_tables () {
	global $db;
	global $dbdriver;

	$query = 'SELECT * FROM accounts';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$query = 'CREATE TABLE accounts ( username int references Users ( id ), password varchar ( 100 ) default \'\' )';
		local_query_and_check ( $query, "Impossibile creare tabella accounts" );
	}
	else {
		for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
			$meta = $ret->getColumnMeta ( $i );
			$name = $meta [ 'name' ];

			if ( $name == 'username' ) {
				if ( ( $dbdriver == 'pgsql' && $t != 'int4' ) || ( $dbdriver == 'mysql' && $t != 'LONG' ) )
					update_column ( 'accounts', 'username', 'int' );
			}
			else if ( $name == 'password' ) {
				if ( ( $dbdriver == 'pgsql' && $t != 'varchar' ) || ( $dbdriver == 'mysql' && $t != 'VAR_STRING' ) )
					update_column ( 'accounts', 'password', 'varchar' );
			}
			else {
				$query = 'ALTER TABLE accounts DROP COLUMN ' . $name;
				local_query_and_check ( $query, "Impossibile eliminare colonna non piu' usata" );
			}
		}
	}

	$query = 'SELECT * FROM current_sessions';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$query = 'CREATE TABLE current_sessions ( id serial primary key, session_id varchar ( 100 ), init date,
							username int references Users ( id ) on delete cascade )';
		local_query_and_check ( $query, "Impossibile creare tabella sessioni" );
	}
	else {
		for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
			$meta = $ret->getColumnMeta ( $i );
			$name = $meta [ 'name' ];

			if ( $name == 'id' ) {
				/*
					Il controllo sull'id lo salto bellamente...
				*/
			}
			else if ( $name == 'session_id' ) {
				if ( ( $dbdriver == 'pgsql' && $t != 'varchar' ) || ( $dbdriver == 'mysql' && $t != 'VAR_STRING' ) )
					update_column ( 'current_sessions', 'session_id', 'varchar' );
			}
			else if ( $name == 'init' ) {
				if ( ( $dbdriver == 'pgsql' && $t != 'date' ) || ( $dbdriver == 'mysql' && $t != 'DATE' ) )
					update_column ( 'current_sessions', 'init', 'date' );
			}
			else if ( $name == 'username' ) {
				if ( ( $dbdriver == 'pgsql' && $t != 'int4' ) || ( $dbdriver == 'mysql' && $t != 'LONG' ) )
					update_column ( 'current_sessions', 'username', 'int references Users (id)' );
			}
			else {
				$query = 'ALTER TABLE accounts DROP COLUMN ' . $name;
				local_query_and_check ( $query, "Impossibile eliminare colonna non piu' usata" );
			}
		}
	}
}

function check_db_schema () {
	test_class ( GAS );
	test_class ( User );
	test_class ( CustomFile );
	test_class ( Notification );
	test_class ( Supplier );
	test_class ( Measure );
	test_class ( Category );
	test_class ( ProductVariantValue );
	test_class ( ProductVariant );
	test_class ( Product );
	test_class ( Order );
	test_class ( ProductUserVariantComponent );
	test_class ( ProductUserVariant );
	test_class ( ProductUser );
	test_class ( OrderUser );

	test_static_tables ();
}

/*
	Questa funzione e' un duplicato di connect_to_the_database(), tranne per il fatto che non
	esegue un controllo sull'esistenza delle tabelle. Poiche' questo script e' dedicato a
	crearle, e' giusto che non ci siano!
*/
function target_connect_to_the_database () {
	global $dbdriver;
	global $dbhost;
	global $dbport;
	global $dbname;
	global $dbuser;
	global $dbpassword;
	global $db;

	if ( !isset ( $dbhost ) )
		$dbhost = 'localhost';

	if ( !isset ( $dbport ) ) {
		if ( $dbdriver == 'mysql' )
			$dbport = 3306;
		else if ( $dbdriver == 'pgsql' )
			$dbport = 5432;
	}

	try {
		$db = new PDO ( $dbdriver . ':host=' . $dbhost . ';dbname=' . $dbname . ';port=' . $dbport, $dbuser, $dbpassword );
		return true;
	}
	catch ( PDOException $e ) {
		return false;
	}
}

function install_main_db () {
	/*
		Si assume che il database sia gia' stato creato e sia vuoto
	*/

	if ( target_connect_to_the_database () == false )
		error_exit ( "Impossibile selezionare database primario" );

	check_db_schema ();
}

?>
