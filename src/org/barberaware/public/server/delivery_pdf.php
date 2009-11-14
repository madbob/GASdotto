<?

/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	public function ColoredTable ( $header, $data ) {
		$this->SetFillColor ( 224, 235, 255 );
		$this->SetTextColor ( 0 );
		$this->SetDrawColor ( 128, 0, 0 );
		$this->SetLineWidth ( 0.3 );
		$this->SetFont ( 'helvetica', '', 7 );

		$html = '<table cellspacing="0" cellpadding="1" border="1"><tr>';
		for ( $i = 0; $i < count ( $header ); $i++ )
			$html .= '<td>' . ( $header [ $i ] ) . '</td>';
		$html .= '</tr>';

		foreach ( $data as $row ) {
			$html .= '<tr>';

			foreach ( $row as $val )
				$html .= '<td>' . $val . '</td>';

			$html .= '</tr>';
		}

		$html .= '</table>';
		$this->writeHTML ( $html, true, false, false, false, 'C' );
	}
}

/*
	First checks
*/

$id = $_GET [ 'id' ];
if ( isset ( $id ) == false )
	error_exit ( "Richiesta non specificata" );

if ( check_session () == false )
	error_exit ( "Sessione non autenticata" );

$order = new Order ();
$order->readFromDB ( $id );
$supplier = $order->getAttribute ( 'supplier' )->value;
$supplier_name = $supplier->getAttribute ( 'name' )->value;
$shipping_date = $order->getAttribute ( 'shippingdate' )->value;

/*
	Init headers
*/

$products = $order->getAttribute ( "products" )->value;
usort ( $products, "sort_product_by_name" );

$header = array ( 'Utenti' );

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$name = $prod->getAttribute ( "name" )->value;
	$price = format_price ( $prod->getTotalPrice () );

	$measure = $prod->getAttribute ( "measure" )->value;
	if ( $measure != null )
		$symbol = " / " . $measure->getAttribute ( "symbol" )->value;
	else
		$symbol = "";

	array_push ( $header, $name . "<br />(" . $price . $symbol . ")" );
}

array_push ( $header, 'Totale per Utente' );

/*
	Init PDF
*/

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
$pdf->setLanguageArray ( $l );
$pdf->AddPage ();

/*
	Format data
*/

$data = array ();

$products_sums = array ();
for ( $i = 0; $i < count ( $products ); $i++ )
    $products_sums [] = 0;

$request = new stdClass ();
$request->order = $id;
$order_user_proxy = new OrderUser ();
$contents = $order_user_proxy->get ( $request );
usort ( $contents, "sort_orders_by_user" );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$row = array ();
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$row [] = sprintf ( "%s", $order_user->baseuser->surname );

	$user_total = 0;
	usort ( $user_products, "sort_product_user_by_name" );

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product->id ) {
			$decimal = strlen ( strstr ( $prod_user->quantity, '.' ) );
			if ( $decimal != 0 )
				$row [] = number_format ( $prod_user->quantity, $decimal - 1, ',', '' );
			else
				$row [] = sprintf ( "%d", $prod_user->quantity );

			$sum = $prod_user->quantity * $prod->getTotalPrice ();
			$products_sums [ $a ] += $sum;
			$user_total += $sum;
			$e++;
		}
		else
			$row [] = "";
	}

	$row [] = format_price ( $user_total );

	$data [] = $row;
}

$gran_total = 0;
$row = array ();
$row [] = "Totale per Prodotto";

for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	$p = $products_sums [ $i ];
	$row [] = format_price ( $p );
	$gran_total += $p;
}

$row [] = format_price ( $gran_total );

$data [] = $row;

/*
	Output
*/

$pdf->ColoredTable ( $header, $data );
$pdf->Output ( 'consegne_' . $supplier_name . '_' . $shipping_date . '.pdf', 'I' );

/*
	Support callbacks
*/

function sort_product_by_name ( $first, $second ) {
	return strcmp ( $first->getAttribute ( "name" )->value, $second->getAttribute ( "name" )->value );
}

function sort_product_user_by_name ( $first, $second ) {
	return strcmp ( $first->product->name, $second->product->name );
}

function sort_orders_by_user ( $first, $second ) {
	return strcmp ( $first->baseuser->surname, $second->baseuser->surname );
}

?>
