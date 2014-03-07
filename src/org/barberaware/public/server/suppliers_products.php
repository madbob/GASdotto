<?php

/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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

$supp_id = require_param ( 'supplier' );
$format = require_param ( 'format' );
formatting_entities ( $format );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$r = new stdClass ();
$r->supplier = $supp_id;
$tmp_prod = new Product ();
$products = $tmp_prod->get ( $r, false );

$headers = array ( 'Prodotto', 'Unità Misura', 'Prezzo Unitario', 'Prezzo Trasporto' );
$data = array ();

foreach ( $products as $p ) {
	if ( is_object ( $p->measure ) )
		$m = $p->measure->name;
	else
		$m = '';

	$u = format_price ( round ( $p->unit_price, 2 ), false );
	$s = format_price ( round ( $p->shipping_price, 2 ), false );
	$data [] = array ( $p->name, $m, $u, $s );
}

$supplier = new Supplier ();
$supplier->readFromDB ( $supp_id );
$supplier_name = $supplier->getAttribute ( 'name' )->value;

output_formatted_document ( 'Listino ' . $supplier_name, $headers, $data, $format );

