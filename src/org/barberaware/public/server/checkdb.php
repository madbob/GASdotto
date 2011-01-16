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
	global $dbdriver;

	if ( $dbdriver == 'pgsql' )
		$query = sprintf ( 'ALTER TABLE %s ALTER COLUMN %s TYPE %s', $table, $column, $type );
	else
		$query = sprintf ( 'ALTER TABLE %s CHANGE %s %s %s', $table, $column, $column, $type );

	local_query_and_check ( $query, "Impossibile modificare tipo della colonna" );
}

function map_type ( $type, $objtype, $default ) {
	switch ( $type ) {
		case "STRING":
		case "PERCENTAGE":
		case "ADDRESS":
			$ret = 'varchar (500)';
			break;

		case "INTEGER":
			$ret = 'int';
			if ( $default == true )
				$ret .= ' default 0';
			break;

		case "OBJECT":
			$tmp = new $objtype;
			$ret = 'int references ' . $tmp->tablename . ' (id)';
			break;

		case "FLOAT":
			$ret = 'float';
			if ( $default == true )
				$ret .= ' default 0';
			break;

		case "DATE":
			$ret = 'date';
			break;

		case "BOOLEAN":
			$ret = 'boolean';
			break;

		case "ARRAY":
		default:
			$ret = null;
			break;
	}

	return $ret;
}

function check_type ( $correct_type, $t ) {
	global $dbdriver;

	$change = null;

	if ( strstr ( $correct_type, '::' ) == false )
		$type = $correct_type;
	else
		list ( $type, $objtype ) = explode ( "::", $correct_type );

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

		/*
			Gli "ARRAY" sono gestiti su diverse tabelle, dunque non appaiono come
			colonne nella tabella principale e non sono contemplate in questo
			controllo.
			Verranno poi verificate fuori da questo ciclo, nel blocco dedicato ai
			nuovi attributi
		*/

		default:
			break;
	}

	return $change;
}

function create_table_class ( $obj ) {
	$columns = array ();
	$extras = array ();

	$query = sprintf ( 'CREATE TABLE %s ( id serial primary key, ', $obj->tablename );

	foreach ( $obj->attributes as $attr ) {
		if ( $attr->name == 'id' )
			continue;

		if ( strstr ( $attr->type, '::' ) == false )
			$type = $attr->type;
		else
			list ( $type, $objtype ) = explode ( "::", $attr->type );

		if ( $type == 'ARRAY' ) {
			$tmp = new $objtype;
			$extras [] = linking_table_query ( $obj, $attr->name, $objtype );
		}
		else {
			$columns [] = $attr->name . ' ' . map_type ( $type, $objtype, true );
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
		$found_attrs = array ();

		for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
			$meta = $ret->getColumnMeta ( $i );
			if ( $meta [ 'name' ] == 'id' )
				continue;

			$found = false;
			$change = null;
			$t = $meta [ 'native_type' ];

			foreach ( $obj->attributes as $attr ) {
				if ( $meta [ 'name' ] == $attr->name ) {
					$found = true;
					$found_attrs [] = $attr->name;
					$change = check_type ( $attr->type, $t );
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

		foreach ( $obj->attributes as $attr ) {
			if ( $attr->name == 'id' )
				continue;

			$ok = false;

			foreach ( $found_attrs as $f )
				if ( $f == $attr->name ) {
					$ok = true;
					break;
				}

			if ( $ok == false ) {
				if ( strstr ( $attr->type, '::' ) == false )
					$type = $attr->type;
				else
					list ( $type, $objtype ) = explode ( '::', $attr->type );

				if ( $type == 'ARRAY' ) {
					$query = sprintf ( 'SELECT * FROM %s_%s', $obj->tablename, $attr->name );
					$subret = $db->query ( $query );

					if ( $subret == false ) {
						$query = linking_table_query ( $obj, $attr->name, $objtype );
						local_query_and_check ( $query, "Impossibile creare tabella di collegamento" );
					}
				}
				else {
					$type = map_type ( $type, $objtype, false );
					$query = sprintf ( 'ALTER TABLE %s ADD COLUMN %s %s', $obj->tablename, $attr->name, $type );
					local_query_and_check ( $query, "Impossibile aggiungere colonna" );
				}
			}
		}
	}
}

function check_manual_columns ( $tablename, $columns, $ret ) {
	$found_attrs = array ();

	for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
		$meta = $ret->getColumnMeta ( $i );
		$name = $meta [ 'name' ];
		$t = $meta [ 'native_type' ];

		if ( $name == 'id' )
			continue;

		$found = false;
		$change = null;

		for ( $a = 0; $a < count ( $columns ); $a = $a + 2 ) {
			if ( $name == $columns [ $a ] ) {
				$found = true;
				$found_attrs [] = $columns [ $a ];
				$change = check_type ( $columns [ $a + 1 ], $t );
				break;
			}
		}

		if ( $found == false ) {
			$query = sprintf ( 'ALTER TABLE %s DROP COLUMN %s', $tablename, $name );
			local_query_and_check ( $query, "Impossibile eliminare colonna non piu' usata" );
		}
		if ( $change != null ) {
			update_column ( $tablename, $name, $change );
		}
	}

	for ( $i = 0; $i < count ( $columns ); $i = $i + 2 ) {
		if ( $columns [ $i ] == 'id' )
			continue;

		$ok = false;

		foreach ( $found_attrs as $f )
			if ( $f == $columns [ $i ] ) {
				$ok = true;
				break;
			}

		if ( $ok == false ) {
			$type = $columns [ $i + 1 ];

			if ( strstr ( $type, '::' ) == false )
				$type = $type;
			else
				list ( $type, $objtype ) = explode ( '::', $type );

			$type = map_type ( $type, $objtype, false );
			$query = sprintf ( 'ALTER TABLE %s ADD COLUMN %s %s', $tablename, $columns [ $i ], $type );
			local_query_and_check ( $query, "Impossibile aggiungere colonna" );
		}
	}
}

function test_static_tables () {
	global $db;
	global $dbdriver;

	$query = 'SELECT * FROM accounts';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$query = 'CREATE TABLE accounts ( username int references Users ( id ), password varchar ( 100 ) default \'\', reset varchar ( 100 ) default \'\' )';
		local_query_and_check ( $query, "Impossibile creare tabella accounts" );
	}
	else {
		$columns = array ( 'username', 'OBJECT::User', 'password', 'STRING', 'reset', 'STRING' );
		check_manual_columns ( 'accounts', $columns, $ret );
	}

	$query = 'SELECT * FROM current_sessions';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$query = 'CREATE TABLE current_sessions ( id serial primary key, session_id varchar ( 100 ), init date,
							username int references Users ( id ) on delete cascade )';
		local_query_and_check ( $query, "Impossibile creare tabella sessioni" );
	}
	else {
		$columns = array ( 'id', 'INTEGER', 'session_id', 'STRING', 'init', 'DATE', 'username', 'OBJECT::User' );
		check_manual_columns ( 'current_sessions', $columns, $ret );
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
	test_class ( OrderUserFriend );
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
