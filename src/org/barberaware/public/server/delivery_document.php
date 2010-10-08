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
	$block_end = '</table><br /><br /><br />';
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

$contents = get_orderuser_by_order ( $order );
usort ( $contents, "sort_orders_by_user" );

$output = '';

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$user_products = sort_products_on_products ( $products, $user_products );
	$user_total = 0;

	$output .= $block_begin;
	$output .= $head_begin;
	$output .= sprintf ( "%s%s %s%s", $string_begin, $order_user->baseuser->surname, $order_user->baseuser->firstname, $string_end );
	$output .= $head_end;

	$output .= $row_begin . 'Prodotto' . $inrow_separator . 'Quantità' . $inrow_separator . 'Prezzo Totale' . $inrow_separator . 'Prezzo Trasporto' . $row_end;

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

			if ( $unit <= 0.0 ) {
				$q = $prod_user->quantity;
				$q = comma_format ( $q );
			}
			else {
				/*
					Richiesto in bug #144

					Per i prodotti con pezzatura, se l'ordine non e' ancora stato consegnato (o
					salvato) visualizzo il numero di pezzi, altrimenti la quantita'
					effettivamente consegnata/da consegnare
				*/
				if ( $order_user->status == 0 ) {
					$q = ( $prod_user->quantity / $unit );
					$q = ( comma_format ( $q ) ) . ' pezzi';
				}
				else {
					$q = $prod_user->quantity;
					$measure = $prod->getAttribute ( "measure" )->value;
					$q = ( comma_format ( $q ) ) . ' ' . ( $measure->getAttribute ( "name" )->value );
				}
			}

			$quprice = ( $prod_user->quantity * $uprice );
			$qsprice = ( $prod_user->quantity * $sprice );

			$user_total += sum_percentage ( $quprice, $prod->getAttribute ( "surplus" )->value ) + $qsprice;

			$quprice = format_price ( round ( $quprice, 2 ), false );
			$qsprice = format_price ( round ( $qsprice, 2 ), false );

			$output .= $row_begin;
			$output .= ( sprintf ( "%s%s%s", $string_begin, $prod->getAttribute ( "name" )->value, $string_end ) );
			$output .= $inrow_separator . $q . $inrow_separator . $quprice . $inrow_separator . $qsprice;
			$output .= $row_end;

			$e++;
		}
	}

	$output .= $head_begin . "Totale: " . ( format_price ( round ( $user_total, 2 ) ) ) . $head_end;
	$output .= $block_end;
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
