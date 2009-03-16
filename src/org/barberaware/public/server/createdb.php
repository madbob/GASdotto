<?

/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

function install_main_db () {
	global $instance_identifier;

	$dbname = 'gasdotto_' . $instance_identifier;

	/*
		Si assume che il database sia gia' stato creato e sia vuoto
	*/

	if ( connect_to_the_database ( $dbname ) == false )
		error_exit ( "Impossibile selezionare database primario" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE gas (
					id serial,
					name varchar ( 100 ) default '',
					mail varchar ( 100 ) default '',
					image varchar ( 100 ) default '',
					payments boolean default false,
					description varchar ( 500 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella gas" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE users (
					id serial,
					login varchar ( 100 ) default '',
					firstname varchar ( 100 ) default '',
					surname varchar ( 100 ) default '',
					join_date date,
					card_number varchar ( 100 ) default '',
					phone varchar ( 100 ) default '',
					mobile varchar ( 100 ) default '',
					mail varchar ( 100 ) default '',
					mail2 varchar ( 100 ) default '',
					address varchar ( 100 ) default '',
					paying boolean default false,
					privileges int default 1,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella users" );

	$query = sprintf ( "INSERT INTO users ( login, firstname, paying, privileges ) VALUES ( 'root', 'Root', true, 2 )" );
	query_and_check ( $query, "Impossibile inizializzare tabella users" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE accounts (
					username int references users ( id ),
					password varchar ( 100 ) default ''
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella accounts" );

	$query = sprintf ( "INSERT INTO accounts ( username, password ) VALUES ( 1, '27b4b5b01b0d1fcab2046369720ff75e' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella accounts" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE current_sessions (
					id serial,
					session_id varchar ( 100 ),
					init date,
					username int references users ( id ) on delete cascade,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella current_sessions" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE notification (
					id serial,
					type int default 0,
					description varchar ( 500 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella notification" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE supplier (
					id serial,
					name varchar ( 100 ) default '',
					contact varchar ( 100 ) default '',
					phone varchar ( 100 ) default '',
					fax varchar ( 100 ) default '',
					mail varchar ( 100 ) default '',
					address varchar ( 100 ) default '',
					order_mode varchar ( 500 ) default '',
					paying_mode varchar ( 500 ) default '',
					description varchar ( 500 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella supplier" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE supplier_references (
					id serial,
					parent int references supplier ( id ) on delete cascade,
					target int references users ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella supplier_references" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE category (
					id serial,
					name varchar ( 100 ) default '',
					description varchar ( 500 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella category" );

	$query = sprintf ( "INSERT INTO category ( name ) VALUES ( 'Non Specificato' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	$query = sprintf ( "INSERT INTO category ( name ) VALUES ( 'Frutta e Verdura' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	$query = sprintf ( "INSERT INTO category ( name ) VALUES ( 'Cosmesi' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	$query = sprintf ( "INSERT INTO category ( name ) VALUES ( 'Bevande' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella category" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE measure (
					id serial,
					name varchar ( 100 ) default '',
					symbol varchar ( 100 ) default '',
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella measure" );

	$query = sprintf ( "INSERT INTO measure ( name, symbol ) VALUES ( 'Non Specificato', '?' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	$query = sprintf ( "INSERT INTO measure ( name, symbol ) VALUES ( 'Chili', 'kg' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	$query = sprintf ( "INSERT INTO measure ( name, symbol ) VALUES ( 'Litri', 'l' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	$query = sprintf ( "INSERT INTO measure ( name, symbol ) VALUES ( 'Pezzi', 'pezzi' )" );
	query_and_check ( $query, "Impossibile inizializzare tabella measure" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE product (
					id serial,
					name varchar ( 100 ) default '',
					category int references category ( id ),
					supplier int references supplier ( id ) on delete cascade,
					description varchar ( 500 ) default '',
					shipping_price float default 0,
					unit_price float default 0,
					surplus float default 0,
					measure int references measure ( id ),
					minimum_order int default 0,
					multiple_order int default 0,
					stock_size int default 0,
					mutable_price boolean default false,
					available boolean default true,
					archived boolean default false,
					primary key ( id )
				)"
	);
	query_and_check ( $query, "Impossibile creare tabella product" );

	/*
		=======================================================================================
	*/

	$query = sprintf ( "CREATE TABLE orders (
					id serial,
					supplier int references supplier ( id ) on delete cascade,
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

        $query = sprintf ( "CREATE TABLE orders_products (
					id serial,
					parent int references orders ( id ) on delete cascade,
					target int references product ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella orders_products" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE orderuser (
					id serial,
					baseorder int references orders ( id ) on delete cascade,
					baseuser int references users ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella orderuser" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE productuser (
					id serial,
					product int references product ( id ) on delete cascade,
					quantity float,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella productuser" );

	/*
		=======================================================================================
	*/

        $query = sprintf ( "CREATE TABLE orderuser_products (
					id serial,
					parent int references orderuser ( id ) on delete cascade,
					target int references productuser ( id ) on delete cascade,
					primary key ( id )
				)"
	);

	query_and_check ( $query, "Impossibile creare tabella orderuser_products" );
}

?>
