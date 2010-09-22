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
require_once ( "tcpdf/tcpdf.php" );

class DeliveryReport extends TCPDF {
	public function ColoredTable ( $data ) {
		$this->SetFillColor ( 224, 235, 255 );
		$this->SetTextColor ( 0 );
		$this->SetDrawColor ( 128, 0, 0 );
		$this->SetLineWidth ( 0.3 );
		$this->SetFont ( 'helvetica', '', 7 );

		$this->AddPage ();
		$this->writeHTML ( $data, true, false, false, false, 'C' );
	}
}

$id = $_GET [ 'id' ];
if ( isset ( $id ) == false )
	error_exit ( "Richiesta non specificata" );

$format = $_GET [ 'format' ];
if ( isset ( $format ) == false )
	error_exit ( "Formato non specificato" );

if ( $format == 'csv' ) {
	$block_begin = '';
	$block_end = "\n";
	$row_begin = '';
	$row_end = "\n";
	$head_begin = $row_begin;
	$head_end = $row_end;
	$inrow_separator = ';';
	$string_begin = '"';
	$string_end = '"';
}
else if ( $format == 'pdf' ) {
	$block_begin = '<table cellspacing="0" cellpadding="1" border="1" width="100%">';
	$block_end = '</table><br />';
	$row_begin = '<tr><td width="25%">';
	$row_end = '</td></tr>';
	$head_begin = '<tr><td colspan="4"><b>';
	$head_end = '</b>' . $row_end;
	$inrow_separator = '</td><td width="25%">';
	$string_begin = '';
	$string_end = '';
}
else {
	error_exit ( "Formato non valido" );
}

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$order = new Order ();
$order->readFromDB ( $id );

$supplier = $order->getAttribute ( 'supplier' )->value;
$supplier_name = $supplier->getAttribute ( 'name' )->value;
$shipping_date = $order->getAttribute ( 'shippingdate' )->value;

$products = $order->getAttribute ( "products" )->value;
usort ( $products, "sort_product_by_name" );

/*
	Questo e' per evitare che lo script ricarichi per intero l'ordine di riferimento e tutti
	i prodotti per ogni singolo OrderUser
*/
$request = new stdClass ();
$request->baseorder = $id;
$request->has_Order = array ( $id );
$request->has_Product = array ();
for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$request->has_Product [] = $prod->getAttribute ( "id" )->value;
}

$output = '';

$order_user_proxy = new OrderUser ();
$contents = $order_user_proxy->get ( $request, false );
usort ( $contents, "sort_orders_by_user" );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$user_products = sort_products_on_products ( $products, $user_products );
	$user_total = 0;
	$user_total_ship = 0;

	$output .= $block_begin;
	$output .= $head_begin;
	$output .= sprintf ( "%s%s %s%s", $string_begin, $order_user->baseuser->surname, $order_user->baseuser->firstname, $string_end );
	$output .= $head_end;

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product ) {
			/*
				Se l'ordine risulta gia' prezzato, riporto nel documento le
				quantita' precedentemente assegnate in fase di prezzatura
			*/
			if ( $order_user->status == 3 )
				$prod_user->quantity = $prod_user->delivered;

			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			if ( $unit <= 0.0 )
				$q = $prod_user->quantity;
			else
				$q = ( $prod_user->quantity / $unit );

			$q = comma_format ( $q );

			$quprice = ( $prod_user->quantity * $uprice );
			$user_total += $quprice;
			$quprice = format_price ( round ( $quprice, 2 ), false );

			$qsprice = ( $prod_user->quantity * $sprice );
			$user_total_ship += $qsprice;
			$qsprice = format_price ( round ( $qsprice, 2 ), false );

			$output .= $row_begin;
			$output .= ( sprintf ( "%s%s%s", $string_begin, $prod->getAttribute ( "name" )->value, $string_end ) );
			$output .= $inrow_separator . $q . $inrow_separator . $quprice . $inrow_separator . $qsprice;
			$output .= $row_end;

			$e++;
		}
	}

	$output .= $row_begin . $inrow_separator . $inrow_separator;
	$output .= ( format_price ( round ( $user_total, 2 ), false ) ) . $inrow_separator . ( format_price ( round ( $user_total_ship, 2 ), false ) );
	$output .= $row_end . $block_end;
}

if ( $format == 'csv' ) {
	header ( "Content-Type: plain/text" );
	header ( 'Content-Disposition: inline; filename="' . 'consegne_' . $supplier_name . '_' . $shipping_date . '.csv' . '";' );
	echo $output;
}
else if ( $format == 'pdf' ) {
	$pdf = new DeliveryReport ( 'P', 'mm', 'A4', true, 'UTF-8', false );
	$pdf->SetCreator ( 'TCPDF' );
	$pdf->SetAuthor ( 'GASdotto' );
	$pdf->SetTitle ( 'Consegne ordine a ' . $supplier_name . ' del ' . $shipping_date );
	$pdf->SetSubject ( 'Consegne ordine a ' . $supplier_name . ' del ' . $shipping_date );
	$pdf->SetKeywords ( 'consegne, ordini, GASdotto, GAS, ' . $supplier_name );
	$pdf->SetHeaderData ( '', 0, 'Consegne ordine a ' . $supplier_name . ' del ' . $shipping_date, '' );
	$pdf->setHeaderFont ( Array ( 'helvetica', '', 10 ) );
	$pdf->setFooterFont ( Array ( 'helvetica', '', 8 ) );
	$pdf->SetDefaultMonospacedFont ( 'courier' );
	$pdf->SetMargins ( 15, 27, 25, 15 );
	$pdf->SetHeaderMargin ( 5 );
	$pdf->SetFooterMargin ( 10 );
	$pdf->SetAutoPageBreak ( true, 25 );
	$pdf->setImageScale ( 1 );
	$pdf->ColoredTable ( $output );
	$pdf->Output ( 'consegne_' . $supplier_name . '_' . $shipping_date . '.pdf', 'I' );
}

?>
