<?

/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

		$offset = 0;
		$end = 0;
		$tot = count ( $header );

		do {
			$this->AddPage ();

			if ( $end + 10 > $tot )
				$end = $tot;
			else
				$end = $end + 10;

			if ( $tot <= 10 )
				$width = 100;
			else
				$width = 10 * ( $end - $offset );

			$html = '<table cellspacing="0" cellpadding="1" border="1" width="' . $width . '%"><tr>';
			for ( $i = $offset; $i < $end; $i++ )
				$html .= '<td>' . ( $header [ $i ] ) . '</td>';
			$html .= '</tr>';

			foreach ( $data as $row ) {
				$html .= '<tr>';

				for ( $i = $offset; $i < $end; $i++ ) {
					$val = $row [ $i ];
					$html .= '<td>' . $val . '</td>';
				}

				$html .= '</tr>';
			}

			$html .= '</table>';

			$this->writeHTML ( $html, true, false, false, false, 'C' );

			$offset = $end;

		} while ( $end < $tot );
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
	$price = format_price ( $prod->getAttribute ( "unit_price" )->value );

	$measure = $prod->getAttribute ( "measure" )->value;
	if ( $measure != null )
		$symbol = " / " . $measure->getAttribute ( "symbol" )->value;
	else
		$symbol = "";

	array_push ( $header, $name . "<br />(" . $price . $symbol . ")" );
}

array_push ( $header, 'Totale Prezzo Prodotti' );
array_push ( $header, 'Totale Prezzo Trasporto' );
array_push ( $header, 'Totale' );
array_push ( $header, 'Pagato' );
array_push ( $header, 'Stato Consegna' );

/*
	Format data
*/

$data = array ();

$products_sums = array ();
$quantities_sums = array ();
$delivery_sums = array ();
$shipped_sums = array ();
$shipping_price = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$products_sums [] = 0;
	$quantities_sums [] = 0;
	$delivery_sums [] = 0;
	$shipped_sums [] = 0;
	$shipping_price [] = 0;
}

$request = new stdClass ();
$request->baseorder = $id;

/*
	Questo e' per evitare che lo script ricarichi per intero l'ordine di riferimento e tutti
	i prodotti per ogni singolo OrderUser
*/
$request->has_Order = array ( $id );
$request->has_Product = array ();
for ( $i = 0; $i < count ( $products ); $i++ ) {
	$prod = $products [ $i ];
	$request->has_Product [] = $prod->getAttribute ( "id" )->value;
}

$order_user_proxy = new OrderUser ();
$contents = $order_user_proxy->get ( $request, true );
usort ( $contents, "sort_orders_by_user" );
unset ( $request );
unset ( $order_user_proxy );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$row = array ();
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$row [] = sprintf ( "%s", $order_user->baseuser->surname );

	$user_total = 0;
	$user_total_ship = 0;
	$shipped_total = 0;

	/*
		Devo ordinare l'array di ProductUser per nome del prodotto, in modo da essere
		allineato all'array di Products, ma i ProductUser non contengono il nome. Percui
		mi tocca ordinarlo prendendo come traccia l'array di Products, e ricostruirlo da
		un'altra parte
	*/

	$proxy = array ();

	for ( $e = 0; $e < count ( $products ); $e++ ) {
		$prod = $products [ $e ];

		for ( $a = 0; $a < count ( $user_products ); $a++ ) {
			$prod_user = $user_products [ $a ];

			if ( $prod->getAttribute ( "id" )->value == $prod_user->product ) {
				$proxy [] = $prod_user;
				break;
			}
		}
	}

	unset ( $user_products );
	$user_products = $proxy;

	for ( $a = 0, $e = 0; $a < count ( $products ); $a++ ) {
		$prod = $products [ $a ];
		$prod_user = $user_products [ $e ];

		if ( $prod->getAttribute ( "id" )->value == $prod_user->product ) {
			/*
				Se l'ordine e' stato solo salvato e non consegnato per davvero (status == 3) ignoro
				le quantita' marcate come consegnate. Semplicemente azzero qui la variabile relativa
				onde evitare di piazzara if() qua e la'
			*/
			if ( $order_user->status == 3 )
				$prod_user->delivered = 0;

			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			if ( $unit <= 0.0 )
				$q = $prod_user->quantity;
			else
				$q = ( $prod_user->quantity / $unit );

			$q = comma_format ( $q );

			if ( $prod_user->delivered != 0 ) {
				$d = comma_format ( $prod_user->delivered );
				$q .= ' ( ' . $d . ' )';
			}

			$row [] = $q;

			/*
				Per i prodotti con pezzatura, il prezzo di trasporto viene
				calcolato in funzione dei numeri di pezzi
			*/
			if ( $unit <= 0 )
				$sum = ( $prod_user->quantity * $sprice );
			else
				$sum = ( ( $prod_user->quantity / $unit ) * $sprice );
			$shipping_price [ $a ] += $sum;
			$user_total_ship += $sum;

			$sum = $prod_user->quantity * $uprice;
			$products_sums [ $a ] += $sum;
			$user_total += $sum;

			$sum = ( $prod_user->delivered * $uprice ) + ( $prod_user->delivered * $sprice );
			$shipped_sums [ $a ] += $sum;
			$shipped_total += $sum;

			$quantities_sums [ $a ] += $prod_user->quantity;
			$delivery_sums [ $a ] += $prod_user->delivered;

			$e++;
		}
		else
			$row [] = "";

		unset ( $prod_user );
	}

	$row [] = format_price ( round ( $user_total, 2 ), false );
	$row [] = format_price ( round ( $user_total_ship, 2 ), false );
	$row [] = format_price ( round ( $user_total + $user_total_ship, 2 ), false );
	$row [] = format_price ( round ( $shipped_total, 2 ), false );

	if ( $order_user->status == 1 )
		$row [] = 'Parzialmente Consegnato';
	else if ( $order_user->status == 2 )
		$row [] = 'Consegnato';
	else
		$row [] = "";

	$data [] = $row;

	unset ( $user_products );
	unset ( $order_user );
}

unset ( $contents );
unset ( $products );

$row = array ();
$row [] = "Quantita' Totali";
for ( $i = 0; $i < count ( $quantities_sums ); $i++ ) {
	$qs = $quantities_sums [ $i ];
	$q = comma_format ( $qs );

	$ds = $delivery_sums [ $i ];
	if ( $ds != 0 ) {
		$d = comma_format ( $ds );
		$q .= ' ( ' . $d . ' )';
	}

	$row [] = $q;
}
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = "Totale Prezzo Prodotti";
foreach ( $products_sums as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p;
	$gran_total += $r;
}
$row [] = format_price ( round ( $gran_total, 2 ), false );
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = "Totale Prezzo Trasporto";
foreach ( $shipping_price as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p;
	$gran_total += $r;
}
$row [] = '';
$row [] = format_price ( round ( $gran_total, 2 ), false );
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = "Totale";
for ( $i = 0; $i < count ( $products_sums ); $i++ ) {
	$ps = $products_sums [ $i ] + $shipping_price [ $i ];
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p;
	$gran_total += $r;
}
$row [] = '';
$row [] = '';
$row [] = format_price ( round ( $gran_total, 2 ), false );
$data [] = $row;

$gran_total = 0;
$row = array ();
$row [] = "Totale Pagato";
foreach ( $shipped_sums as $ps ) {
	$r = round ( $ps, 2 );
	$p = format_price ( $r, false );
	$row [] = $p;
	$gran_total += $r;
}
$row [] = '';
$row [] = '';
$row [] = '';
$row [] = format_price ( round ( $gran_total, 2 ), false ) . "\n";
$data [] = $row;

/*
	Output
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
$pdf->ColoredTable ( $header, $data );
$pdf->Output ( 'consegne_' . $supplier_name . '_' . $shipping_date . '.pdf', 'I' );

?>
