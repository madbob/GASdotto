<?php

/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
	global $db;

	$mix_table = sprintf ( "%s_%s", $obj->tablename, $name );

	$query = 'SELECT * FROM ' . $mix_table . ' ORDER BY id LIMIT 1';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$tmp = new $objtype;
		return sprintf ( 'CREATE TABLE %s ( parent int references %s (id), target int references %s (id) )',
					$mix_table, $obj->tablename, $tmp->tablename );
	}
	else {
		return null;
	}
}

function update_column ( $table, $column, $type ) {
	global $dbdriver;

	if ( $dbdriver == 'pgsql' ) {
		if ( strpos ( $type, 'references' ) !== false ) {
			$query = sprintf ( 'ALTER TABLE %s ALTER COLUMN %s TYPE int USING 0', $table, $column );
			local_query_and_check ( $query, "Impossibile modificare tipo della colonna" );

			$type = substr ( $type, 4 );
			$query = sprintf ( 'ALTER TABLE %s ADD CONSTRAINT %s_%s_fkey FOREIGN KEY (%s) %s', $table, $table, $column, $column, $type );
		}
		else {
			$query = sprintf ( 'ALTER TABLE %s ALTER COLUMN %s TYPE %s', $table, $column, $type );
		}
	}
	else {
		/*
			TODO	Gestire correttamente il cambiamento in foreign key per MySQL
		*/
		$query = sprintf ( 'ALTER TABLE %s CHANGE %s %s %s', $table, $column, $column, $type );
	}

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
			unset ( $tmp );
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
			if ( $default == true )
				$ret .= ' default false';
			break;

		case "ARRAY":
		default:
			$ret = null;
			break;
	}

	return $ret;
}

function check_type ( $correct_type, $t ) {
	if ( $t == -1 )
		return null;

	global $dbdriver;

	$change = null;
	list ( $type, $objtype ) = unpack_type ( $correct_type );

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

function unpack_type ( $datatype ) {
	if ( strstr ( $datatype, '::' ) == false )
		return [ $datatype, null ];
	else
		return explode ( "::", $datatype );
}

function create_table_class ( $obj ) {
	$columns = array ();
	$extras = array ();

	$query = sprintf ( 'CREATE TABLE %s ( ', $obj->tablename );
	$columns [] = "id serial primary key";

	foreach ( $obj->attributes as $attr ) {
		if ( $attr->name == 'id' )
			continue;

		list ( $type, $objtype ) = unpack_type ( $attr->type );

		/*
			Vengono deliberatamente saltate le relazioni con altri tipi.
			Cfr. check_db_schema() per maggiori dettagli
		*/

		if ( $type != 'ARRAY' && $type != 'OBJECT' )
			$columns [] = $attr->name . ' ' . map_type ( $type, $objtype, true );
	}

	$query .= ( join ( ', ', $columns ) ) . ' )';
	local_query_and_check ( $query, "Impossibile creare nuova tabella" );

	$obj->install ();
}

function test_class ( $class ) {
	global $db;
	global $dbdriver;

	$obj = new $class;

	$query = 'SELECT * FROM ' . $obj->tablename . ' ORDER BY id LIMIT 1';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		create_table_class ( $obj );
		$ret = false;
	}
	else {
		$found_attrs = array ();

		for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
			$meta = $ret->getColumnMeta ( $i );
			if ( $meta [ 'name' ] == 'id' )
				continue;

			$found = false;
			$change = null;

			if ( array_key_exists ( 'native_type', $meta ) )
				$t = $meta [ 'native_type' ];
			else
				$t = -1;

			foreach ( $obj->attributes as $attr ) {
				if ( $meta [ 'name' ] == $attr->name ) {
					$found = true;
					$found_attrs [] = $attr->name;
					$change = check_type ( $attr->type, $t );
					break;
				}
			}

			if ( $found == false ) {
				$query = sprintf ( 'ALTER TABLE %s DROP COLUMN %s', $obj->tablename, $meta [ 'name' ] );
				local_query_and_check ( $query, "Impossibile eliminare colonna non piu' usata" );
			}
			if ( $change != null )
				update_column ( $obj->tablename, $attr->name, $change );
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
				list ( $type, $objtype ) = unpack_type ( $attr->type );

				if ( $type == 'ARRAY' ) {
					$query = sprintf ( 'SELECT * FROM %s_%s ORDER BY parent LIMIT 1', $obj->tablename, $attr->name );
					$subret = $db->query ( $query );

					if ( $subret == false ) {
						$query = linking_table_query ( $obj, $attr->name, $objtype );
						if ( $query != null )
							local_query_and_check ( $query, "Impossibile creare tabella di collegamento" );
					}
				}
				else {
					$type = map_type ( $type, $objtype, true );
					$query = sprintf ( 'ALTER TABLE %s ADD COLUMN %s %s', $obj->tablename, $attr->name, $type );
					local_query_and_check ( $query, "Impossibile aggiungere colonna" );
				}
			}
		}

		$ret = true;
	}

	unset ( $obj );
	return $ret;
}

function migrate_table ( $class ) {
	global $db;
	global $dbdriver;

	$obj = new $class;

	$query = 'SELECT * FROM ' . $obj->tablename . ' ORDER BY id LIMIT 1';
	$ret = $db->query ( $query );

	if ( $ret == false )
		return;

	switch ( $class ) {
		case 'User':
			for ( $i = 0; $i < $ret->columnCount (); $i++ ) {
				$meta = $ret->getColumnMeta ( $i );

				/*
					Per migrare la quota annuale da semplice data a BankMovement
				*/
				if ( $meta [ 'name' ] == 'paying' ) {
					$t = $meta [ 'native_type' ];
					$change = check_type ( 'OBJECT::BankMovement', $t );

					if ( $change != null ) {
						$map = array ();
						$tmp = new BankMovement ();

						$query = 'SELECT * FROM ' . $obj->tablename;
						$ret = $db->query ( $query );
						$rows = $ret->fetchAll ( PDO::FETCH_ASSOC );

						foreach ( $rows as $row ) {
							$userid = $row [ 'id' ];

							/*
								Usare la classica funzione FromServer::save() qui
								comporta problemi, dunque la query viene forgiata
								manualmente
							*/

							$query = "INSERT INTO " . $tmp->tablename . " (movementtype, method, amount, payuser, paysupplier, date)
									VALUES (2, 1, 0, $userid, -1, '" . $row [ 'paying' ] . "')";
							$db->query ( $query );
							$id = last_id ( 'BankMovement' );

							$map [ $userid ] = $id;
						}

						/*
							Qui non faccio un semplice update_column() in quanto il DB
							tenterebbe di convertire le date in riferimenti per le
							foreign key, ovviamente non trovandole e riportando un errore
						*/

						$query = sprintf ( 'ALTER TABLE %s DROP COLUMN %s', $obj->tablename, $meta [ 'name' ] );
						local_query_and_check ( $query, "Impossibile eliminare colonna non piu' usata" );

						$query = sprintf ( 'ALTER TABLE %s ADD COLUMN %s %s', $obj->tablename, $meta [ 'name' ], $change );
						local_query_and_check ( $query, "Impossibile aggiungere colonna" );

						foreach ( $map as $user => $movement ) {
							$query = sprintf ( "UPDATE %s SET paying = %d WHERE id = %d", $obj->tablename, $movement, $user );
							local_query_and_check ( $query, "Impossibile aggiornare colonna quote utenti" );
						}
					}
				}
			}

			break;
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
		if ( $change != null )
			update_column ( $tablename, $name, $change );
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
			list ( $type, $objtype ) = unpack_type ( $type );
			$type = map_type ( $type, $objtype, true );
			$query = sprintf ( 'ALTER TABLE %s ADD COLUMN %s %s', $tablename, $columns [ $i ], $type );
			local_query_and_check ( $query, "Impossibile aggiungere colonna" );
		}
	}
}

function align_acl ( $type, $table = null ) {
	global $db;

	if ( $table == null )
		$table = $type;

	$query = "SELECT id FROM $table";
	$result = $db->query ( $query );
	$rows = $result->fetchAll ( PDO::FETCH_NUM );

	foreach ( $rows as $row ) {
		$id = $row [ 0 ];
		$query = "INSERT INTO ACL (gas, target_type, target_id, privileges) VALUES (1, '$type', $id, 0)";
		local_query_and_check ( $query, "Impossibile aggiornare permessi" );
	}

	$query = "UPDATE current_sessions SET gas = 1";
	local_query_and_check ( $query, "Impossibile aggiornare sessioni" );

	unset ( $rows );
}

function test_static_tables () {
	global $db;
	global $dbdriver;

	$query = 'SELECT * FROM accounts ORDER BY username LIMIT 1';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$query = "CREATE TABLE accounts (
				username int references Users ( id ),
				password varchar ( 100 ) default '',
				reset varchar ( 100 ) default '' )";
		local_query_and_check ( $query, "Impossibile creare tabella accounts" );
	}
	else {
		$columns = array ( 'username', 'OBJECT::User', 'password', 'STRING', 'reset', 'STRING' );
		check_manual_columns ( 'accounts', $columns, $ret );
	}

	$query = 'SELECT * FROM current_sessions ORDER BY id LIMIT 1';
	$ret = $db->query ( $query );

	if ( $ret == false ) {
		$query = 'CREATE TABLE current_sessions (
				id serial primary key,
				session_id varchar ( 100 ),
				init date,
				username int references Users ( id ) on delete cascade,
				gas int references GAS ( id ) on delete cascade )';
		local_query_and_check ( $query, "Impossibile creare tabella sessioni" );
	}
	else {
		$columns = array ( 'id', 'INTEGER', 'session_id', 'STRING', 'init', 'DATE', 'username', 'OBJECT::User', 'gas', 'OBJECT::GAS' );
		check_manual_columns ( 'current_sessions', $columns, $ret );
	}
}

function check_db_schema () {
	$classes = [
		'GAS',
		'ShippingPlace',
		'BankMovementType',
		'BankMovement',
		'User',
		'CustomFile',
		'Notification',
		'Link',
		'Supplier',
		'Measure',
		'Category',
		'ProductVariantValue',
		'ProductVariant',
		'Product',
		'Order',
		'OrderAggregate',
		'ProductUserVariantComponent',
		'ProductUserVariant',
		'ProductUser',
		'OrderUserFriend',
		'OrderUser'
	];

	$redo = [];

	/*
		Se la tabella che rappresenta il tipo di dato non esiste,
		test_class() invoca create_table_class(), la quale
		deliberatamente non crea attributi e tabelle per le relazioni.
		In tali casi, test_class() viene nuovamente chiamata nel ciclo
		qui sotto sulle stesse tabelle, a questo punto aggiungendo tali
		relazioni.
		Questo è per far si di non trovarsi in cicli ricorsivi di
		dipendenze (e.g. Users che dipende da BankMovement che dipende
		da Users): la prima volta che installo GASdotto creo prima tutte
		le tabelle senza relazioni, dopodiché quando le tabelle ci sono
		tutte le aggiusto
	*/

	foreach ( $classes as $c )
		if ( test_class ( $c ) == false )
			$redo [] = $c;

	foreach ( $redo as $c )
		test_class ( $c );

	foreach ( $classes as $c )
		migrate_table ( $c );

	test_static_tables ();

	if ( test_class ( 'ACL' ) == false ) {
		test_class ( 'ACL' );

		align_acl ( 'Supplier' );
		align_acl ( 'User', 'Users' );
		align_acl ( 'Order', 'Orders' );
		align_acl ( 'OrderAggregate' );
	}
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
