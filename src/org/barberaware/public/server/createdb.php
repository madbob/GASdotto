<?php

/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

function new_tables () {
	$query = sprintf ( "CREATE TABLE Supplier_carriers (
					id serial,
					parent int references Supplier ( id ) on delete cascade,
					target int references Users ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella supplier_carriers" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE ProductVariant (
					id serial,
					name varchar ( 100 ) default '',
					primary key ( id )
				)" );

	query_and_check ( $query, "Impossibile creare tabella productvariant" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE Product_variants (
					id serial,
					parent int references Product ( id ) on delete cascade,
					target int references ProductVariant ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella product_variants" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE ProductVariantValue (
					id serial,
					name varchar ( 100 ) default '',
					primary key ( id )
				)" );

	query_and_check ( $query, "Impossibile creare tabella productvariantvalue" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE ProductVariant_values (
					id serial,
					parent int references ProductVariant ( id ) on delete cascade,
					target int references ProductVariantValue ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productvariant_values" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE ProductUserVariant (
					id serial,
					delivered boolean default false,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productuservariant" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE ProductUserVariantComponent (
					id serial,
					variant int references ProductVariant ( id ) on delete cascade,
					value int references ProductVariantValue ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productuservariantcomponent" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE ProductUserVariant_components (
					id serial,
					parent int references ProductuserVariant ( id ) on delete cascade,
					target int references ProductuserVariantComponent ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productuservariant_components" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE ProductUser_variants (
					id serial,
					parent int references ProductUser ( id ) on delete cascade,
					target int references ProductuserVariant ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productuser_variants" );
}

function install_main_db () {
	/*
		Si assume che il database sia gia' stato creato e sia vuoto
	*/

	if ( target_connect_to_the_database () == false )
		error_exit ( "Impossibile selezionare database primario" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE GAS (
					id serial,
					name varchar ( 100 ) default '',
					mail varchar ( 100 ) default '',
					image varchar ( 100 ) default '',
					payments boolean default false,
					description varchar ( 500 ) default '',
					use_mail boolean default false,
					mail_conf varchar ( 500 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella gas" );

	$query = sprintf ( "INSERT INTO GAS ( name ) VALUES ( 'Il Mio GAS' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella GAS" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Users (
					id serial,
					login varchar ( 100 ) default '',
					firstname varchar ( 100 ) default '',
					surname varchar ( 100 ) default '',
					birthday date,
					join_date date,
					card_number varchar ( 100 ) default '',
					phone varchar ( 100 ) default '',
					mobile varchar ( 100 ) default '',
					mail varchar ( 100 ) default '',
					mail2 varchar ( 100 ) default '',
					address varchar ( 100 ) default '',
					paying date,
					privileges int default 1,
					family int default 1,
					photo varchar ( 100 ) default '',
					lastlogin date,
					leaving_date date,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella users" );

	$query = sprintf ( "INSERT INTO Users ( login, firstname, paying, privileges ) VALUES ( 'root', 'Root', now(), 2 )" );
	query_and_check ( $query, "Impossibile inizializzare tabella users" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE accounts (
					username int references Users ( id ),
					password varchar ( 100 ) default ''
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella accounts" );

	/* Si, la password di default e' molto stupida :-P */
	$query = sprintf ( "INSERT INTO accounts ( username, password ) VALUES ( 1, '" . md5 ( "ciccio" ) . "' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella accounts" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE current_sessions (
					id serial,
					session_id varchar ( 100 ),
					init date,
					username int references Users ( id ) on delete cascade,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella current_sessions" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE automatic_sessions (
					id serial,
					session_id varchar ( 100 ),
					init date,
					username int references Users ( id ) on delete cascade,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella automatic_sessions" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Notification (
					id serial,
					alert_type int default 0,
					description varchar ( 500 ) default '',
					startdate date,
					enddate date,
					send_mail boolean default false,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella notification" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Notification_recipent (
					id serial,
					parent int references Notification ( id ) on delete cascade,
					target int references Users ( id ) on delete cascade,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella notification_recipent" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE CustomFile (
					id serial,
					name varchar ( 100 ) default '',
					server_path varchar ( 100 ) default '',
					by_user int references Users ( id ) on delete cascade,
					upload_date date,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella notification" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Supplier (
					id serial,
					name varchar ( 100 ) default '',
					contact varchar ( 100 ) default '',
					phone varchar ( 100 ) default '',
					fax varchar ( 100 ) default '',
					mail varchar ( 100 ) default '',
					website varchar ( 100 ) default '',
					address varchar ( 100 ) default '',
					order_mode varchar ( 500 ) default '',
					paying_mode varchar ( 500 ) default '',
					description varchar ( 500 ) default '',
					orders_months varchar ( 20 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella supplier" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Supplier_references (
					id serial,
					parent int references Supplier ( id ) on delete cascade,
					target int references Users ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella supplier_references" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Supplier_files (
					id serial,
					parent int references Supplier ( id ) on delete cascade,
					target int references Customfile ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella supplier_files" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Category (
					id serial,
					name varchar ( 100 ) default '',
					description varchar ( 500 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella category" );

	$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Non Specificato' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Frutta e Verdura' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Cosmesi' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	$query = sprintf ( "INSERT INTO Category ( name ) VALUES ( 'Bevande' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Measure (
					id serial,
					name varchar ( 100 ) default '',
					symbol varchar ( 100 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella measure" );

	$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Non Specificato', '?' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Chili', 'kg' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Litri', 'l' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	$query = sprintf ( "INSERT INTO Measure ( name, symbol ) VALUES ( 'Pezzi', 'pezzi' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Product (
					id serial,
					name varchar ( 100 ) default '',
					code varchar ( 100 ) default '',
					category int references Category ( id ),
					supplier int references Supplier ( id ) on delete cascade,
					description varchar ( 500 ) default '',
					shipping_price float default 0,
					unit_price float default 0,
					surplus varchar ( 100 ) default '0',
					measure int references Measure ( id ),
					minimum_order int default 0,
					multiple_order int default 0,
					stock_size float default 0,
					unit_size float default 0,
					mutable_price boolean default false,
					available boolean default true,
					archived boolean default false,
					previous_description int default -1,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella product" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE Orders (
					id serial,
					supplier int references Supplier ( id ) on delete cascade,
					startdate date,
					enddate date,
					status int default 0,
					shippingdate date,
					nextdate varchar ( 100 ) default '0',
					anticipated varchar ( 100 ),
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella order" );

        /*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE Orders_products (
					id serial,
					parent int references Orders ( id ) on delete cascade,
					target int references Product ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella orders_products" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE OrderUser (
					id serial,
					baseorder int references Orders ( id ) on delete cascade,
					baseuser int references Users ( id ) on delete cascade,
					status int default 0,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella orderuser" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE ProductUser (
					id serial,
					product int references Product ( id ) on delete cascade,
					quantity float,
					delivered float,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productuser" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE OrderUser_products (
					id serial,
					parent int references OrderUser ( id ) on delete cascade,
					target int references ProductUser ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella orderuser_products" );

	/*
		=======================================================================================
	*/

	new_tables ();
}

function upgrade_main_db () {
	if ( target_connect_to_the_database () == false )
		error_exit ( "Impossibile selezionare database primario" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "ALTER TABLE GAS ADD COLUMN mail_conf varchar ( 500 )" );
	query_and_check ( $query, "Impossibile aggiornare tabella gas" );
	$query = sprintf ( "ALTER TABLE GAS ALTER COLUMN mail_conf SET DEFAULT ''" );
	query_and_check ( $query, "Impossibile aggiornare tabella gas" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "ALTER TABLE Users ADD COLUMN family int" );
	query_and_check ( $query, "Impossibile aggiornare tabella users" );
	$query = sprintf ( "ALTER TABLE Users ALTER COLUMN family SET DEFAULT 1" );
	query_and_check ( $query, "Impossibile aggiornare tabella users" );

	$query = sprintf ( "ALTER TABLE Users ADD COLUMN photo varchar ( 100 )" );
	query_and_check ( $query, "Impossibile aggiornare tabella users" );
	$query = sprintf ( "ALTER TABLE Users ALTER COLUMN photo SET DEFAULT ''" );
	query_and_check ( $query, "Impossibile aggiornare tabella users" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "ALTER TABLE Notification ADD COLUMN send_mail boolean" );
	query_and_check ( $query, "Impossibile aggiornare tabella notification" );
	$query = sprintf ( "ALTER TABLE Notification ALTER COLUMN send_mail SET DEFAULT false" );
	query_and_check ( $query, "Impossibile aggiornare tabella notification" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "ALTER TABLE Supplier ADD COLUMN  orders_months varchar ( 20 )" );
	query_and_check ( $query, "Impossibile aggiornare tabella supplier" );
	$query = sprintf ( "ALTER TABLE Supplier ALTER COLUMN orders_months SET DEFAULT ''" );
	query_and_check ( $query, "Impossibile aggiornare tabella supplier" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "ALTER TABLE Product ADD COLUMN  code varchar ( 100 )" );
	query_and_check ( $query, "Impossibile aggiornare tabella product" );
	$query = sprintf ( "ALTER TABLE Product ALTER COLUMN code SET DEFAULT ''" );
	query_and_check ( $query, "Impossibile aggiornare tabella product" );

	/*
		=======================================================================================
	*/

	new_tables ();
}

?>
