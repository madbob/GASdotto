<?

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

$products = $order->getAttribute ( "products" )->value;
usort ( $products, "sort_product_by_name" );

$header = array ( 'Prodotto', 'Quantità', 'Prezzo Totale', 'Prezzo Trasporto' );

$products_sums = array ();
$quantities_sums = array ();
$shipping_sum = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$products_sums [] = 0;
	$quantities_sums [] = 0;
	$shipping_sum [] = 0;
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
$contents = $order_user_proxy->get ( $request, false );
usort ( $contents, "sort_orders_by_user" );

for ( $i = 0; $i < count ( $contents ); $i++ ) {
	$order_user = $contents [ $i ];

	$user_products = $order_user->products;
	if ( is_array ( $user_products ) == false )
		continue;

	$user_total = 0;

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
			$unit = $prod->getAttribute ( "unit_size" )->value;
			$uprice = $prod->getAttribute ( "unit_price" )->value;
			$sprice = $prod->getAttribute ( "shipping_price" )->value;

			if ( $unit <= 0.0 )
				$q = $prod_user->quantity;
			else
				$q = ( $prod_user->quantity / $unit );

			$quantities_sums [ $a ] += $q;

			$sum = $prod_user->quantity * $uprice;
			$products_sums [ $a ] += $sum;

			$sum = $prod_user->quantity * $sprice;
			$shipping_sum [ $a ] += $sum;

			$e++;
		}
	}
}

$data = array ();

for ( $i = 0; $i < count ( $products ); $i++ ) {
	$name = get_product_name ( $products, $i );
	$q = comma_format ( round ( $quantities_sums [ $i ], 2 ), false );
	$p = format_price ( round ( $products_sums [ $i ], 2 ), false );
	$s = format_price ( round ( $shipping_sum [ $i ], 2 ), false );
	$row = array ( $name, $q, $p, $s );
	$data [] = $row;
}

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
$pdf->Output ( 'ordinazioni_' . $supplier_name . '_' . $shipping_date . '.pdf', 'I' );

?>
