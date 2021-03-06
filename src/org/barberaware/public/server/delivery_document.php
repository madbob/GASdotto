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
require_once ( "tcpdf/tcpdf.php" );

global $block_begin;
global $block_end;
global $row_begin;
global $row_end;
global $head_begin;
global $head_end;
global $inrow_separator;
global $string_begin;
global $string_end;
global $content_sep;

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

$id = require_param ( 'id' );
$format = require_param ( 'format' );
$is_aggregate = get_param ( 'aggregate', false );
$location = get_param ( 'location', -1 );

formatting_entities ( $format );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

list ( $orders, $supplier_name, $supplier_ships, $shipping_date ) = details_about_order ( $id, $is_aggregate );

$all_products = array ();
$all_contents = array ();

foreach ( $orders as $order ) {
	$products = $order->getAttribute ( "products" )->value;
	usort ( $products, "sort_product_by_name" );

	$contents = get_orderuser_by_order ( $order );

	$all_products = array_merge ( $all_products, $products );
	$all_contents = merge_order_users ( $all_contents, $contents );
}

usort ( $all_contents, "sort_orders_by_user_and_date" );

$output = '';

$gas = current_gas ();
$use_bank = $gas->getAttribute ( 'use_bank' )->value;

for ( $i = 0; $i < count ( $all_contents ); $i++ ) {
	$order_user = $all_contents [ $i ];
	$user = $order_user->baseuser;

	/*
		Salto gli ordini gia' consegnati
	*/
	if ( $order_user->status == 2 )
		continue;

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	if ( $location != -1 ) {
		$u_location = $user->shipping->id;
		if ( $u_location != $location )
			continue;
	}

	$user_products = sort_products_on_products ( $all_products, $user_products );
	$user_total = 0;

	$output .= $block_begin;
	$output .= $head_begin;
	$output .= sprintf ( "%s%s %s%s", $string_begin, $user->surname, $user->firstname, $string_end );
	$output .= $head_end;

	$output .= $row_begin . 'Prodotto' . $inrow_separator . 'Quantità' . $inrow_separator . 'Prezzo Totale' . $inrow_separator . 'Prezzo Trasporto' . $row_end;

	/*
		Se l'ordine risulta gia' prezzato, riporto nel documento le
		quantita' precedentemente assegnate in fase di prezzatura
	*/
	if ( $order_user->status == 3 )
		$param = 'delivered';
	else
		$param = 'quantity';

	for ( $a = 0, $e = 0; $a < count ( $all_products ); $a++ ) {
		$prod = $all_products [ $a ];
		$prodid = $prod->getAttribute ( 'id' )->value;
		$quantity = 0;

		/*
			Comunque lo inizializzo, nel caso in cui non ci fosse
			nessun prodotto dell'utente ma ce ne fossero per gli
			amici (e dunque devo invocare array_merge())
		*/
		$variants = array ();

		if ( $e < count ( $user_products ) ) {
			$prod_user = $user_products [ $e ];

			if ( $prodid == $prod_user->product ) {
				$quantity = $prod_user->$param;

				if ( property_exists ( $prod_user, 'variants' ) && is_array ( $prod_user->variants ) )
					$variants = $prod_user->variants;

				$e++;
			}
		}

		if ( property_exists ( $order_user, 'friends' ) && count ( $order_user->friends ) != 0 ) {
			foreach ( $order_user->friends as $friend ) {
				foreach ( $friend->products as $fprod ) {
					if ( $fprod->product == $prodid ) {
						$quantity += $fprod->$param;

						if ( property_exists ( $fprod, 'variants' ) && is_array ( $fprod->variants ) )
							$variants = array_merge ( $variants, $fprod->variants );

						break;
					}
				}
			}
		}

		if ( $quantity != 0 ) {
			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			if ( $unit <= 0.0 ) {
				$q = $quantity;
				$measure = $prod->getAttribute ( "measure" )->value;
				$q = ( comma_format ( $q ) ) . ' ' . ( $measure->getAttribute ( "name" )->value );

				if ( count ( $variants ) != 0 ) {
					list ( $variants, $quantities ) = aggregate_variants ( $variants );

					for ( $j = 0; $j < count ( $variants ); $j++ )
						$q .= $content_sep . ( $quantities [ $j ] ) . ' ' . ( $variants [ $j ] );
				}
			}
			else {
				/*
					Richiesto in bug #144

					Per i prodotti con pezzatura, se l'ordine non e' ancora stato consegnato (o
					salvato) visualizzo il numero di pezzi, altrimenti la quantita'
					effettivamente consegnata/da consegnare
				*/
				if ( $order_user->status == 0 ) {
					$q = ( $quantity / $unit );
					$q = ( comma_format ( $q ) ) . ' pezzi';
				}
				else {
					$q = $quantity;
					$measure = $prod->getAttribute ( "measure" )->value;
					$q = ( comma_format ( $q ) ) . ' ' . ( $measure->getAttribute ( "name" )->value );
				}

				if ( count ( $variants ) != 0 ) {
					list ( $variants, $quantities ) = aggregate_variants ( $variants );

					for ( $j = 0; $j < count ( $variants ); $j++ )
						$q .= $content_sep . ( $quantities [ $j ] ) . ' ' . ( $variants [ $j ] );
				}
			}

			$quprice = ( $quantity * $uprice );
			$qsprice = ( $quantity * $sprice );

			$user_total += sum_percentage ( $quprice, $prod->getAttribute ( "surplus" )->value ) + $qsprice;

			$quprice = format_price ( round ( $quprice, 2 ), false );
			$qsprice = format_price ( round ( $qsprice, 2 ), false );

			$output .= $row_begin;

			$output .= $string_begin;
			$output .= $prod->getAttribute ( "name" )->value;
			if ( $is_aggregate )
				$output .= sprintf ( "%s(%s)", $double_line_sep, $prod->getAttribute ( "supplier" )->value->getAttribute ( "name" )->value );
			$output .= $string_end;

			$output .= $inrow_separator . $q . $inrow_separator . $quprice . $inrow_separator . $qsprice;
			$output .= $row_end;
		}
	}

	$credit_test = '';
	if ( $use_bank == 1 ) {
		if ( $user_total > $user->current_balance )
			$credit_test = ' (credito non sufficiente)';
	}

	$output .= $head_begin . "Totale: " . ( format_price ( round ( $user_total, 2 ) ) ) . $credit_test . $head_end;
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
