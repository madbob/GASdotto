<?php

/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

require_once ( "extra.php" );
require_once ( "config.php" );

require_once ( "FromServer.php" );
require_once ( "SharableFromServer.php" );
require_once ( "Session.php" );
require_once ( "SystemConf.php" );
require_once ( "ACL.php" );
require_once ( "CustomFile.php" );
require_once ( "GAS.php" );
require_once ( "ShippingPlace.php" );
require_once ( "User.php" );
require_once ( "Notification.php" );
require_once ( "Supplier.php" );
require_once ( "Measure.php" );
require_once ( "Category.php" );
require_once ( "Product.php" );
require_once ( "ProductVariant.php" );
require_once ( "ProductVariantValue.php" );
require_once ( "Order.php" );
require_once ( "OrderAggregate.php" );
require_once ( "ProductUser.php" );
require_once ( "ProductUserVariant.php" );
require_once ( "ProductUserVariantComponent.php" );
require_once ( "OrderUserFriend.php" );
require_once ( "OrderUser.php" );
require_once ( "Probe.php" );

function error_exit ( $string ) {
	$output = json_encode ( "Errore: " . $string );
	print ( $output );
	exit;
}

function strbegins ( $str, $start ) {
	return ( strncmp ( $str, $start, strlen ( $start ) ) == 0 );
}

function class_name ( $name ) {
	$fragments = explode ( ".", $name );
	return $fragments [ count ( $fragments ) - 1 ];
}

function search_in_array ( $array, $val ) {
	for ( $i = 0; $i < count ( $array ); $i++ )
		if ( $array [ $i ] == $val )
			return $i;

	return -1;
}

function require_param ( $name, $msg = 'Richiesta non valida' ) {
	if ( array_key_exists ( $name, $_GET ) == false )
		error_exit ( $msg );
	return $_GET [ $name ];
}

function get_param ( $name, $default ) {
	if ( array_key_exists ( $name, $_GET ) == false )
		return $default;
	else
		return $_GET [ $name ];
}

/****************************************************************** db management */

function query_and_check ( $query, $error ) {
	global $db;

	$ret = $db->query ( $query );
	if ( $ret == false ) {
		require_once ( "checkdb.php" );
		check_db_schema ();

		$ret = $db->query ( $query );
		if ( $ret == false ) {
			$error_code = $db->errorInfo ();
			error_exit ( $error . " executing |" . $query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
		}
	}

	return $ret;
}

function db_row_count ( $query ) {
	global $db;

	$true_query = "SELECT COUNT(*) " . $query;
	$ret = $db->query ( $true_query );

	if ( $ret == false ) {
		$error_code = $db->errorInfo ();
		error_exit ( "Error checking for row count, executing |" . $true_query . "| " . " (" . ( $error_code [ 2 ] ) . ")" );
	}

	return $ret->fetchColumn ();
}

function escape_string ( $string ) {
	/**
		TODO	Implementare decentemente questa funzione
	*/
	return $string;
}

function db_date_plus_one () {
	global $dbdriver;

	if ( $dbdriver == "pgsql" )
		return " + INTERVAL '1 DAY' ";
	else
		return " + INTERVAL 1 DAY ";
}

function last_id ( $class ) {
	global $db;
	global $dbdriver;

	if ( $dbdriver == "pgsql" )
		return $db->lastInsertId ( $class . "_id_seq" );
	else
		return $db->lastInsertId ();
}

function connect_to_the_database () {
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

		$query = sprintf ( "SELECT * FROM GAS" );
		if ( $db->query ( $query ) == false )
			throw new PDOException ();

		return true;
	}
	catch ( PDOException $e ) {
		$output = json_encode ( "no_db" );
		print ( $output );
		exit;
	}
}

function get_from_server_classes () {
	$ret = array ();

	foreach ( get_declared_classes () as $classname )
		if ( in_array ( 'FromServer', class_parents ( $classname ) ) )
			$ret [] = $classname;

	return $ret;
}

/****************************************************************** formatting */

function comma_format ( $a ) {
	/*
		Cast esplicito: e' successo che il cast implicito provocasse problemi
	*/
	$a = ( string ) $a;

	$decimal = strlen ( strstr ( $a, '.' ) );
	if ( $decimal != 0 )
		return number_format ( $a, $decimal - 1, ',', '' );
	else
		return $a;
}

function format_price ( $price, $symbol = true ) {
	if ( $symbol == true )
		return "€ " . number_format ( $price, 2, ',', '' );
	else
		return number_format ( $price, 2, ',', '' );
}

function format_date ( $dbdate, $month_num = false ) {
	if ( $dbdate == null || $dbdate == '' ) {
		return '';
	}
	else {
		list ( $year, $month, $day ) = explode ( '-', $dbdate );

		if ( $month_num == false ) {
			$months = array ( 'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno', 'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre' );

			/*
				La variabile $day viene castata ad int per accertarsi di eliminare l'eventuale
				zero posto dinnanzi ai giorni con una cifra sola
			*/
			return ( ( int ) $day ) . ' ' . ( $months [ $month - 1 ] ) . ' ' . $year;
		}
		else {
			return ( ( int ) $day ) . ' ' . ( ( int ) $month ) . ' ' . $year;
		}
	}
}

function format_address ( $address ) {
	return $address->street . ', ' . $address->city;
}

function broken_address ( $value ) {
	$obj = new stdClass ();
	$tokens = explode ( ";", $value );

	for ( $i = 0; $i < count ( $tokens ); $i++ ) {
		if ( strlen ( $tokens [ $i ] ) != 0 ) {
			list ( $name, $value ) = explode ( ":", $tokens [ $i ] );
			if ( $name != "" )
				$obj->$name = $value;
		}
	}

	return $obj;
}

function broken_to_stamp ( $broken ) {
	return mktime ( $broken [ 'tm_hour' ], $broken [ 'tm_min' ], $broken [ 'tm_sec' ], $broken [ 'tm_mon' ], $broken [ 'tm_mday' ], $broken [ 'tm_year' ] + 1900 );
}

function random_string ( $length ) {
	$characters = '0123456789abcdefghijklmnopqrstuvwxyz';
	$string = '';

	for ( $p = 0; $p < $length; $p++ )
		$string .= $characters [ mt_rand ( 0, strlen ( $characters ) - 1 ) ];

	return $string;
}

function details_about_order ( $id, $is_aggregate ) {
	if ( $is_aggregate == true ) {
		$aggregate = new OrderAggregate ();
		$aggregate->readFromDB ( $id );

		$orders = $aggregate->getAttribute ( 'orders' )->value;
		$max_shipping_date = 0;
		$supplier_ships = 1;

		foreach ( $orders as $order ) {
			$supplier = $order->getAttribute ( 'supplier' )->value;
			$suppliers [] = $supplier->getAttribute ( 'name' )->value;

			if ( $supplier_ships != 0 && $supplier->getAttribute ( 'shipping_manage' )->value == 0 )
				$supplier_ships = 0;

			$shipping_date = $order->getAttribute ( 'shippingdate' )->value;
			if ( $shipping_date == '' )
				$shipping_date = $order->getAttribute ( 'enddate' )->value;

			$sd = broken_to_stamp ( strptime ( $shipping_date, '%Y-%m-%d' ) );
			if ( $max_shipping_date < $sd )
				$max_shipping_date = $sd;
		}

		$supplier_name = join ( ' / ', $suppliers );
		$shipping_date = date ( 'Y-m-d', $max_shipping_date );
	}
	else {
		$order = new Order ();
		$order->readFromDB ( $id );
		$supplier = $order->getAttribute ( 'supplier' )->value;
		$supplier_name = $supplier->getAttribute ( 'name' )->value;
		$supplier_ships = $supplier->getAttribute ( 'shipping_manage' )->value;

		$shipping_date = $order->getAttribute ( 'shippingdate' )->value;
		if ( $shipping_date == '' )
			$shipping_date = $order->getAttribute ( 'enddate' )->value;

		$orders = array ( $order );
	}

	return array ( $orders, $supplier_name, $supplier_ships, $shipping_date );
}

function sort_product_by_name ( $first, $second ) {
	return strcmp ( $first->getAttribute ( "name" )->value, $second->getAttribute ( "name" )->value );
}

function sort_orders_by_user ( $first, $second ) {
	return strcmp ( $first->baseuser->surname, $second->baseuser->surname );
}

function sort_orders_by_user_and_date ( $first, $second ) {
	if ( property_exists ( $first, 'deliverydate' ) && property_exists ( $second, 'deliverydate' ) ) {
		if ( $first->deliverydate == $second->deliverydate )
			return strcmp ( $first->baseuser->surname, $second->baseuser->surname );
		else
			return strcmp ( $first->deliverydate, $second->deliverydate );
	}
	if ( property_exists ( $first, 'deliverydate' ) == false && property_exists ( $second, 'deliverydate' ) == false ) {
		return strcmp ( $first->baseuser->surname, $second->baseuser->surname );
	}
	else if ( property_exists ( $first, 'deliverydate' ) == false ) {
		return 1;
	}
	else if ( property_exists ( $second, 'deliverydate' ) == false ) {
		return -1;
	}
}

function sort_friend_orders_by_user ( $first, $second ) {
	return strcmp ( $first->friendname, $second->friendname );
}

function get_product_name ( $product ) {
	$code = $product->getAttribute ( "code" )->value;
	if ( $code != '' )
		$code = ' - ' . $code;

	return ( $product->getAttribute ( "name" )->value ) . $code;
}

function get_product_measure_symbol ( $product ) {
	$measure = $product->getAttribute ( "measure" )->value;
	if ( $measure != null )
		return $measure->getAttribute ( 'name' )->value;

	return '';
}

function get_product_quantity_stocks ( $product, $quantity ) {
	if ( $quantity > 0 ) {
		$stock = $product->getAttribute ( "stock_size" )->value;

		if ( $stock != 0 )
			return ( comma_format ( round ( $quantity, 2 ), false ) ) . ' (' . ( ceil ( $quantity / $stock ) ) . ' confezioni da ' . $stock . ')';
	}

	return comma_format ( round ( $quantity, 2 ), false );
}

function sum_percentage ( $original, $perc ) {
	if ( $perc == null || $perc == '' || $perc == '0' ) {
		return $original;
	}
	else if ( strstr ( $perc, '%' ) ) {
		list ( $integer ) = explode ( '%', $perc );
		$sum = ( $original * $integer ) / 100;
		return $original + $sum;
	}
	else {
		return $original + $perc;
	}
}

function ellipse_string ( $string, $len ) {
	if ( strlen ( $string ) > $len ) {
		$string = substr ( $string, 0, $len - 2 );
		$string .= '...';
	}

	return $string;
}

function sort_products_on_products ( $products, $user_products ) {
	$proxy = array ();

	for ( $e = 0; $e < count ( $products ); $e++ ) {
		$prod = $products [ $e ];

		for ( $a = 0; $a < count ( $user_products ); $a++ ) {
			$prod_user = $user_products [ $a ];

			/*
				Questo e' perche' possono arrivare prodotti "compressi" (espressi solo per mezzo
				dell'ID) o meno (rappresentati dalla struttura completa)
			*/
			if ( isset ( $prod_user->product->id ) )
				$id = $prod_user->product->id;
			else
				$id = $prod_user->product;

			if ( $prod->getAttribute ( "id" )->value == $id ) {
				$proxy [] = $prod_user;
				break;
			}
		}
	}

	return $proxy;
}

function merge_order_users ( $all_orders, $orders, $also_friends = false ) {
	foreach ( $orders as $order ) {
		$found = false;

		foreach ( $all_orders as $main_order ) {
			if ( $order->baseuser->id == $main_order->baseuser->id ) {
				$main_order->products = array_merge ( $main_order->products, $order->products );

				/*
					Gli ordini per gli amici vengono sempre aggregati in funzione del nome
					dell'amico
				*/
				if ( $also_friends == true && isset ( $order->friends ) ) {
					$friend_found = false;
					$order_friends = $order->friends;

					if ( isset ( $main_order->friends ) == false )
						$main_order->friends = array ();

					$main_friends = $main_order->friends;

					foreach ( $order_friends as $order_friend ) {
						$friend_found = false;

						foreach ( $main_friends as $main_friend ) {
							if ( $order_friend->friendname == $main_friend->friendname ) {
								$main_friend->products = array_merge ( $main_friend->products, $order_friend->products );
								$friend_found = true;
							}
						}

						if ( $friend_found == false )
							array_push ( $main_order->friends, $order_friend );
					}
				}

				$found = true;
			}
		}

		if ( $found == false )
			array_push ( $all_orders, $order );
	}

	return $all_orders;
}

function aggregate_variants ( $variants ) {
	$tmp_variants = array ();
	$ret_quantities = array_fill ( 0, 20, 0 );

	foreach ( $variants as $var ) {
		$exists = false;
		$index = 0;
		$i = 0;

		foreach ( $tmp_variants as $test ) {
			for ( $i = 0; $i < count ( $var->components ); $i++ ) {
				$equals = true;

				/*
					I componenti si assumono gia' ordinati
					per nome
				*/

				$var_comp = $var->components [ $i ];
				$test_comp = $test->components [ $i ];

				if ( $var_comp->value->id != $test_comp->value->id ) {
					$equals = false;
					break;
				}
			}

			if ( $equals == true ) {
				$exists = true;
				break;
			}

			$index++;
		}

		if ( $exists == false ) {
			$tmp_variants [] = $var;
			$ret_quantities [] = 1;
		}
		else {
			$ret_quantities [ $i ] = $ret_quantities [ $i ] + 1;
		}
	}

	$ret_variants = array ();

	foreach ( $tmp_variants as $var ) {
		$desc = array ();

		foreach ( $var->components as $comp )
			$desc [] = $comp->variant->name . ': ' . $comp->value->name;

		$ret_variants [] = join ( '; ', $desc );
		unset ( $desc );
	}

	unset ( $tmp_variants );

	return array ( $ret_variants, $ret_quantities );
}

function formatting_entities ( $format ) {
	global $block_begin;
	global $block_end;
	global $row_begin;
	global $row_end;
	global $title_begin;
	global $title_end;
	global $head_begin;
	global $head_end;
	global $inrow_separator;
	global $string_begin;
	global $string_end;
	global $content_sep;
	global $double_line_sep;
	global $double_line_end;
	global $onelinepadding;
	global $emptycell;

	if ( $format == 'csv' ) {
		$block_begin = '';
		$block_end = "\n";
		$row_begin = '';
		$row_end = "\n";
		$title_begin = '';
		$title_end = '';
		$head_begin = $row_begin;
		$head_end = $row_end;
		$inrow_separator = ';';
		$string_begin = '"';
		$string_end = '"';
		$content_sep = ' - ';
		$double_line_sep = ' ';
		$double_line_end = '';
		$onelinepadding = '';
		$emptycell = '';
	}
	else if ( $format == 'pdf' ) {
		$block_begin = '<table cellspacing="0" cellpadding="1" border="1" width="100%">';
		$block_end = '</table><br /><br /><br />';
		$row_begin = '<tr><td width="25%">';
		$row_end = '</td></tr>';
		$title_begin = '<b>';
		$title_end = '</b>';
		$head_begin = '<tr><td colspan="4">' . $title_begin;
		$head_end = $title_end . $row_end;
		$inrow_separator = '</td><td width="25%">';
		$string_begin = '';
		$string_end = '';
		$content_sep = '<br />';
		$double_line_sep = '<br />';
		$double_line_end = '<br />';
		$onelinepadding = '<br />';
		$emptycell = '<br />';
	}
	else {
		error_exit ( "Formato non valido" );
	}
}

class ExportDocument extends TCPDF {
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
				$r_tot = count ( $row );

				if ( $r_tot < $tot && $r_tot == 1 && $tot != 1 ) {
					$html .= '<td colspan="' . ( $end - $offset ) . '">' . $row [ 0 ] . '</td>';
				}
				else {
					for ( $i = $offset; $i < $end; $i++ ) {
						if ( $i < count ( $row ) )
							$val = $row [ $i ];
						else
							$val = '&nbsp;';

						$html .= '<td>' . $val . '</td>';
					}
				}

				$html .= '</tr>';
			}

			$html .= '</table>';

			$this->writeHTML ( $html, true, false, false, false, 'C' );

			$offset = $end;

		} while ( $end < $tot );
	}
}

function format_csv ( $headers, $data ) {
	$output = join ( ';', $headers ) . "\n";

	foreach ( $data as $row )
		$output .= join ( ';', $row ) . "\n";

	return $output;
}

function output_formatted_document ( $title, $headers, $data, $format ) {
	if ( $format == 'csv' ) {
		header ( "Content-Type: plain/text" );
		header ( 'Content-Disposition: inline; filename="' . $title . '.csv";' );
		$output = format_csv ( $headers, $data );
		echo $output;
	}
	else if ( $format == 'pdf' ) {
		$pdf = new ExportDocument ( 'P', 'mm', 'A4', true, 'UTF-8', false );
		$pdf->SetCreator ( 'TCPDF' );
		$pdf->SetAuthor ( 'GASdotto' );
		$pdf->SetTitle ( $title );
		$pdf->SetSubject ( $title );
		$pdf->SetHeaderData ( '', 0, $title );
		$pdf->setHeaderFont ( Array ( 'helvetica', '', 10 ) );
		$pdf->setFooterFont ( Array ( 'helvetica', '', 8 ) );
		$pdf->SetDefaultMonospacedFont ( 'courier' );
		$pdf->SetMargins ( 15, 27, 25, 15 );
		$pdf->SetHeaderMargin ( 5 );
		$pdf->SetFooterMargin ( 10 );
		$pdf->SetAutoPageBreak ( true, 25 );
		$pdf->setImageScale ( 1 );
		$pdf->ColoredTable ( $headers, $data );
		$pdf->Output ( $title . '.pdf', 'I' );
	}
}

function exportable_date ( $date ) {
	return str_replace ( '-', '', $date );
}

function import_date ( $date ) {
	preg_match ('/([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])/', $date, $matches);
	return $matches [1] . '-' . $matches [2] . '-' . $matches [3];
}

function exportable_products ( $products ) {
	$ret = "\t\t<products>\n";

	foreach ( $products as $product ) {
		$ret .= "\t\t\t<product>\n";

		if ( property_exists ( $product, 'code' ) && $product->code != '' )
			$ret .= "\t\t\t\t<sku>" . $product->code . "</sku>\n";

		$ret .= "\t\t\t\t<name>" . $product->name . "</name>\n";
		$ret .= "\t\t\t\t<category>" . $product->category->name . "</category>\n";
		$ret .= "\t\t\t\t<um>" . $product->measure->name . "</um>\n";
		$ret .= "\t\t\t\t<description>" . $product->description . "</description>\n";

		$ret .= "\t\t\t\t<orderInfo>\n";

		if ( $product->minimum_order > 0 )
			$ret .= "\t\t\t\t\t<minQty>" . $product->minimum_order . "</minQty>\n";
		if ( $product->multiple_order > 0 )
			$ret .= "\t\t\t\t\t<mulQty>" . $product->multiple_order . "</mulQty>\n";
		if ( property_exists ( $product, 'total_max_order' ) && $product->total_max_order > 0 )
			$ret .= "\t\t\t\t\t<maxQty>" . $product->total_max_order . "</maxQty>\n";

		$ret .= "\t\t\t\t\t<umPrice>" . $product->unit_price . "</umPrice>\n";

		if ( $product->shipping_price > 0 )
			$ret .= "\t\t\t\t\t<shippingCost>" . $product->shipping_price . "</shippingCost>\n";

		if ( property_exists ( $product, 'variants' ) && is_array ( $product->variants ) && count ( $product->variants ) != 0 ) {
			$ret .= "\t\t\t\t\t<variants>\n";

			foreach ( $product->variants as $variant ) {
				$ret .= "\t\t\t\t\t\t<variant name=\"" . $variant->name . "\">\n";

				foreach ( $variant->values as $value )
					$ret .= "\t\t\t\t\t\t\t<value>" . $value->name . "</value>\n";

				$ret .= "\t\t\t\t\t\t</variant>\n";
			}

			$ret .= "\t\t\t\t\t</variants>\n";
		}

		$ret .= "\t\t\t\t</orderInfo>\n";
		$ret .= "\t\t\t</product>\n";
	}

	$ret .= "\t\t</products>\n";
	return $ret;
}

/****************************************************************** shortcuts */

function base_orderuser_request ( $order ) {
	$id = $order->getAttribute ( 'id' )->value;

	$request = new stdClass ();
	$request->baseorder = $id;

	/*
		Questo e' per evitare che lo script ricarichi per intero l'ordine di riferimento e tutti
		i prodotti per ogni singolo OrderUser
	*/
	$request->has_Order = array ( $id );

	$request->has_Product = array ();
	$products = $order->getAttribute ( 'products' )->value;

	for ( $i = 0; $i < count ( $products ); $i++ ) {
		$prod = $products [ $i ];
		$request->has_Product [] = $prod->getAttribute ( 'id' )->value;
	}

	return $request;
}

function get_orderuser_by_order ( $order, $user = null ) {
	$request = base_orderuser_request ( $order );

	if ( $user != null )
		$request->baseuser = $user;

	$order_user_proxy = new OrderUser ();
	$ret = $order_user_proxy->get ( $request, false );

	unset ( $request );
	unset ( $order_user_proxy );

	return $ret;
}

/****************************************************************** files management */

function unique_filesystem_name ( $folder, $name ) {
	$i = 0;
	$final_name = $name;

	while ( file_exists ( $folder . '/' . $final_name ) ) {
		$final_name = $i . '_' . $name;
		$i++;
	}

	return $final_name;
}

/****************************************************************** mail */

function my_send_mail ( $recipients, $subject, $public, $body, $html = null ) {
	global $current_user;

	$gas = current_gas ();
	$mailconf = $gas->getAttribute ( 'mail_conf' )->value;
	$name = $gas->getAttribute ( 'name' )->value;
	unset ( $gas );

	list ( $from, $username, $password, $host, $port, $ssl ) = explode ( '::', $mailconf );

	if ( $ssl == 'true' )
		$host = 'ssl://' . $host;

	$mysubject = '[' . $name . '] ' . $subject;
	$headers = array ( 'From' => $from, 'Subject' => $mysubject );

	if ( $current_user != -1 ) {
		$sender = current_user_obj ();
		$headers [ 'Reply-To' ] = $sender->getAttribute ( 'mail' )->value;
		unset ( $sender );
	}

	if ( $html != null ) {
		$message = new Mail_mime ();
		$message->setTXTBody ( $body );
		$message->setHTMLBody ( $html );
		$body = $message->get ( array ( 'html_charset' => 'UTF-8', 'text_charset' => 'UTF-8' ) );
		$headers = $message->headers ( $headers );
	}
	else {
		$headers [ 'Content-Type' ] = "text/plain; charset=\"UTF-8\"";
	}

	$smtp = Mail::factory ( 'smtp', array ( 'host' => $host, 'port' => $port, 'auth' => true, 'username' => $username, 'password' => $password ) );

	$tot = count ( $recipients );

	/*
		Se ci sono piu' di 50 destinatari, mando piu' mail, li divido in porzioni da 50.
		Questo perche' pressoche' tutti i providers di posta impongono un limite sul
		numero di destinatari (oltre il quale la mail viene bloccata), il quale e' assai
		variabile: alcuni lo hanno a 100, altri a 200. Per scaramanzia mi tengo basso nel
		conto
	*/
	if ( $tot > 50 ) {
		for ( $i = 0; $i < $tot; $i += 50 ) {
			$end = $i + 50;
			if ( $end > $tot )
				$end = $tot;

			$recipients_part = array_slice ( $recipients, $i, $end );

			if ( $public == true )
				$headers [ 'To' ] = '<' . ( join ( '>, <', $recipients_part ) ) . '>';

			$ret = $smtp->send ( $recipients_part, $headers, $body );

			if ( PEAR::isError ( $ret ) )
				return $ret->getMessage ();
		}

		return null;
	}
	else {
		if ( $public == true )
			$headers [ 'To' ] = '<' . ( join ( '>, <', $recipients ) ) . '>';

		$ret = $smtp->send ( $recipients, $headers, $body );

		if ( PEAR::isError ( $ret ) )
			return $ret->getMessage ();
		else
			return null;
	}
}

/****************************************************************** multigas */

function current_gas () {
	global $current_gas;

	$ret = new GAS ();
	$ret->readFromDB ( $current_gas );
	return $ret;
}

function acl_filter_plain ( $obj ) {
	global $current_gas;

	return " ( SELECT target_id FROM acl WHERE gas = $current_gas AND target_type = '" . $obj->classname . "' ) ";
}

function acl_filter_hierarchy_asc ( $obj, $parent_class, $attribute ) {
	$p = new $parent_class ();
	$p_filter = $p->filter_by_current_gas ();

	if ( $p_filter == "" ) {
		$ret = "";
	}
	else {
		global $current_gas;
		$ret = sprintf ( " ( SELECT target FROM %s_%s WHERE parent IN %s ) ", $p->tablename, $attribute, $p_filter );
	}

	unset ( $p );
	return $ret;
}

function acl_filter_hierarchy_desc ( $obj, $child_class, $attribute ) {
	$c = new $child_class ();
	$c_filter = $c->filter_by_current_gas ();

	if ( $c_filter == "" ) {
		$ret = "";
	}
	else {
		global $current_gas;
		$ret = sprintf ( " ( SELECT id FROM %s WHERE %s IN %s ) ", $obj->tablename, $attribute, $c_filter );
	}

	unset ( $c );
	return $ret;
}

function get_acl_easy ( $type, $id ) {
	global $current_gas;

	$o = new $type ();

	if ( $o->is_public == true ) {
		return 0;
	}
	else {
		switch ( $o->public_mode [ 'mode' ] ) {
			case 'asc':
				$query = "SELECT privileges FROM acl WHERE gas = $current_gas AND target_type = '" . $o->public_mode [ 'class' ] . "' AND target_id IN ( SELECT " . $o->public_mode [ 'attribute' ] . " FROM " . $o->tablename . " WHERE id = $id )";
				break;

			case 'desc':
				$uclass = $o->public_mode [ 'class' ];
				$u = new $uclass ();
				$query = "SELECT privileges FROM acl WHERE gas = $current_gas AND target_type = '" . $o->public_mode [ 'class' ] . "' AND target_id IN ( SELECT parent FROM " . $u->tablename . "_" . $o->public_mode [ 'attribute' ] . " WHERE target = $id )";
				break;

			case 'plain':
			default:
				$query = "SELECT privileges FROM acl WHERE gas = $current_gas AND target_type = '$type' AND target_id = $id";
				break;
		}

		$result = query_and_check ( $query, "Impossibile recuperare privilegi di accesso al dato" );
		$row = $result->fetchAll ( PDO::FETCH_NUM );

		if ( count ( $row ) > 0 )
			return $row [ 0 ] [ 0 ];
		else
			return 10;
	}
}

function get_acl ( $obj ) {
	$type = $obj->classname;
	$id = $obj->getAttribute ( 'id' )->value;
	return get_acl_easy ( $type, $id );
}

function save_acl ( $obj, $priv ) {
	global $current_gas;

	$type = $obj->classname;
	$id = $obj->getAttribute ( 'id' )->value;

	$query = "FROM acl WHERE gas = $current_gas AND target_type = '$type' AND target_id = $id";
	if ( db_row_count ( $query ) == 0 )
		$query = "INSERT INTO acl ( gas, target_type, target_id, privileges ) VALUES ( $current_gas, '$type', $id, $priv )";
	else
		$query = "UPDATE acl SET privileges = $priv WHERE gas = $current_gas AND target_type = '$type' AND target_id = $id";

	query_and_check ( $query, "Impossibile aggiornare permessi di accesso" );
}

function destroy_acl ( $obj ) {
	if ( $obj->is_public == true ) {
		$type = $obj->classname;
		$id = $obj->getAttribute ( 'id' )->value;

		$query = "DELETE FROM acl WHERE target_type = '$type' AND target_id = $id";
		query_and_check ( $query, "Impossibile aggiornare permessi di accesso" );
	}
}

function check_acl_easy ( $type, $id, $min ) {
	$p = get_acl_easy ( $type, $id );

	if ( $p > $min )
		return false;
	else
		return true;
}

function check_acl ( $obj, $min ) {
	$p = get_acl ( $obj );

	if ( $p > $min )
		return false;
	else
		return true;
}

/****************************************************************** authentication */

function parse_session_data () {
	global $session_key;

	if ( array_key_exists ( 'gasdotto', $_COOKIE ) == false )
		return false;

	$session_data = $_COOKIE [ 'gasdotto' ];

	list ( $session_serial, $hash ) = explode ( '-*-', $session_data );
	$session_serial = base64_decode ( $session_serial );
	$new_hash = md5 ( $session_serial . $session_key );

	if ( $hash != $new_hash ) {
		setcookie ( 'gasdotto', "", 0, '/', '', 0 );
		error_exit ( "Sessione non validata" );
	}

	list ( $session_id, $ip ) = explode ( '-', $session_serial, 4 );

	if (  $ip != $_SERVER [ 'REMOTE_ADDR' ] ) {
		setcookie ( 'gasdotto', "", 0, '/', '', 0 );
		error_exit ( "IP non riconosciuto" );
	}

	/*
		lo User-Agent non e' volutamente controllato
	*/

	return $session_id;
}

function check_session () {
	global $current_user;
	global $current_gas;
	global $db;
	global $cache;

	$current_user = -1;
	$current_gas = -1;
	$cache = array ();

	if ( connect_to_the_database () == false )
		error_exit ( "Impossibile connettersi al database" );

	$current_session_id = parse_session_data ();
	if ( $current_session_id == false )
		return false;

	$query = sprintf ( "FROM current_sessions WHERE session_id = '%s'", $current_session_id );

	if ( db_row_count ( $query ) == 0 ) {
		/*
			Se sono qui e' probabilmente perche' il cookie sulla macchina dell'utente
			non e' coerente con quanto salvato nel DB, dunque cancello suddetto
			cookie ed emetto un errore. Sul client, la classe Session lo intercetta e
			ricarica la pagina rieseguendo tutta la procedura stavolta senza cookie e
			dunque fermandosi alla schermata di login
		*/
		setcookie ( 'gasdotto', "", 0, '/', '', 0 );
		error_exit ( "Impossibile accedere alla sessione" );
	}

	$query = "SELECT username, gas " . $query;
	$result = query_and_check ( $query, "Impossibile identificare sessione aperta" );

	$row = $result->fetchAll ( PDO::FETCH_NUM );
	unset ( $result );

	/*
		Nella tabella "current_sessions" il campo "username" contiene l'ID dell'utente.
		Triste scelta di nome...
	*/
	$current_user = $row [ 0 ] [ 0 ];
	$current_gas = $row [ 0 ] [ 1 ];

	return true;
}

function perform_authentication ( $userid, $permanent ) {
	global $session_key;

	$query = "SELECT gas FROM acl WHERE target_type = 'User' AND target_id = $userid";
	$result = query_and_check ( $query, "Impossibile recuperare GAS di appartenenza del dato" );
	$row = $result->fetchAll ( PDO::FETCH_NUM );
	$gasid = $row [ 0 ] [ 0 ];

	$t = time ();

	/*
		tutte le sessioni piu' vecchie di una settimana sono eliminate
	*/

	$old_now = date ( "Y-m-d", ( $t - ( 60 * 60 * 24 * 7 ) ) );
	$query = sprintf ( "DELETE FROM current_sessions
				WHERE init < '%s' OR
				username = %d",
					$old_now, $userid );
	query_and_check ( $query, "Impossibile sincronizzare sessioni" );

	do {
		$session_id = substr ( md5 ( time () ), 0, 20 );

		$query = sprintf ( "SELECT COUNT(id) FROM current_sessions WHERE session_id = '%s'", $session_id );
		$result = query_and_check ( $query, "Impossibile salvare sessione" );
		$row = $result->fetchAll ( PDO::FETCH_NUM );
		unset ( $result );
	} while ( $row [ 0 ] [ 0 ] != 0 && sleep ( 1 ) == 0 );

	if ( $permanent == false ) {
		$now = date ( "Y-m-d", $t );
		$expiry = 0;
	}
	else {
		$now = date ( "Y-m-d", PHP_INT_MAX );
		$expiry = PHP_INT_MAX;
	}

	$query = sprintf ( "INSERT INTO current_sessions ( session_id, init, username, gas )
				VALUES ( '%s', DATE('%s'), %d, %d )",
					$session_id, $now, $userid, $gasid );
	query_and_check ( $query, "Impossibile salvare sessione" );

	$session_serial = $session_id . '-' . $_SERVER [ 'REMOTE_ADDR' ];
	$session_hash = md5 ( $session_serial . $session_key );
	$session_cookie = base64_encode ( $session_serial ) . '-*-' . $session_hash;

	if ( setcookie ( 'gasdotto', $session_cookie, $expiry, '/', '', 0 ) == false )
		error_exit ( "Impossibile settare il cookie" );
}

function current_permissions () {
	/**
		TODO	Qui si puo' evitare di leggere tutto l'utente ma solo i permessi
			direttamente dal DB
	*/

	$u = current_user_obj ();
	$privileges = $u->getAttribute ( "privileges" );
	return $privileges->value;
}

function current_user_obj () {
	global $current_user;

	$u = new User ();

	if ( $current_user != -1 )
		$u->readFromDB ( $current_user );

	return $u;
}

?>
