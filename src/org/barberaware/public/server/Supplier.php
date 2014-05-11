<?php

/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class Supplier extends SharableFromServer {
	public function __construct () {
		parent::__construct ( "Supplier" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "tax_code", "STRING" );
		$this->addAttribute ( "vat_number", "STRING" );
		$this->addAttribute ( "contact", "STRING" );
		$this->addAttribute ( "phone", "STRING" );
		$this->addAttribute ( "fax", "STRING" );
		$this->addAttribute ( "mail", "STRING" );
		$this->addAttribute ( "website", "STRING" );
		$this->addAttribute ( "address", "ADDRESS" );
		$this->addAttribute ( "order_mode", "STRING" );
		$this->addAttribute ( "paying_mode", "STRING" );
		$this->addAttribute ( "paying_by_bank", "BOOLEAN" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "references", "ARRAY::User" );
		$this->addAttribute ( "carriers", "ARRAY::User" );
		$this->addAttribute ( "files", "ARRAY::CustomFile" );
		$this->addAttribute ( "orders_months", "STRING" );
		$this->addAttribute ( "shipping_manage", "INTEGER" );
		$this->addAttribute ( "hidden", "BOOLEAN" );
		$this->addAttribute ( "current_balance", "FLOAT" );

		$this->preserveAttribute ( "references" );
		$this->preserveAttribute ( "carriers" );

		$this->setSorting ( "name" );
		$this->setPublic ( false );
	}

	public function get ( $request, $compress ) {
		$query = 'id > 0';

		if ( $request != null && property_exists ( $request, 'hidden') ) {
			if ( $request->hidden != -1 )
				$query = sprintf ( 'hidden = %s', $request->hidden );
		}
		else {
			/*
				Il controllo su "is null" e' per gestire i fornitori gia' esistenti nel DB in caso di
				aggiornamento alla versione 3.0. Effettivamente in checkdb.php manca la definizioni
				di un trigger per settare i valori di default (in questo caso, "false")
			*/
			$query = 'hidden = false OR hidden IS NULL';
		}

		return parent::getByQuery ( $request, $compress, $query );
	}

	public function export ( $options ) {
		$ret = SharableFromServer::header ();

		$ret .= "\t<supplier>\n";
		$ret .= "\t\t<taxCode>" . self::getAttribute ( 'tax_code' )->value . "</taxCode>\n";
		$ret .= "\t\t<vatNumber>" . self::getAttribute ( 'vat_number' )->value . "</vatNumber>\n";
		$ret .= "\t\t<name>" . self::getAttribute ( 'name' )->value . "</name>\n";

		$address = self::getAttribute ( 'address' )->value;
		$ret .= "\t\t<address>\n";
		$ret .= "\t\t\t<street>" . $address->street . "</street>\n";
		$ret .= "\t\t\t<locality>" . $address->city . "</locality>\n";
		$ret .= "\t\t\t<zipCode>" . $address->cap . "</zipCode>\n";
		$ret .= "\t\t\t<country>IT</country>\n";
		$ret .= "\t\t</address>\n";

		$ret .= "\t\t<contacts>\n";
		$ret .= "\t\t\t<contact>\n";
		$ret .= "\t\t\t\t<primary>\n";
		$ret .= "\t\t\t\t\t<phoneNumber>" . self::getAttribute ( 'phone' )->value . "</phoneNumber>\n";
		$ret .= "\t\t\t\t\t<faxNumber>" . self::getAttribute ( 'fax' )->value . "</faxNumber>\n";
		$ret .= "\t\t\t\t\t<emailAddress>" . self::getAttribute ( 'mail' )->value . "</emailAddress>\n";
		$ret .= "\t\t\t\t\t<webSite>" . self::getAttribute ( 'website' )->value . "</webSite>\n";
		$ret .= "\t\t\t\t</primary>\n";
		$ret .= "\t\t\t</contact>\n";
		$ret .= "\t\t</contacts>\n";

		$ret .= "\t\t<note>" . join ( ' / ', array ( self::getAttribute ( 'order_mode' )->value, self::getAttribute ( 'paying_mode' )->value ) ) . "</note>\n";

		if ( array_key_exists ( 'products', $options ) == FALSE || $options [ 'products' ] === TRUE ) {
			$r = new stdClass ();
			$r->supplier = self::getAttribute ( 'id' )->value;
			$tmp_prod = new Product ();
			$products = $tmp_prod->get ( $r, false );
			unset ( $tmp_prod );

			$ret .= exportable_products ( $products );
		}
		else if ( array_key_exists ( 'products', $options ) == TRUE || $options [ 'products' ] !== FALSE ) {
			$ret .= $options [ 'products' ];
		}

		if ( array_key_exists ( 'child', $options ) == TRUE )
			$ret .= $options [ 'child' ];

		$ret .= "\t</supplier>\n";
		$ret .= SharableFromServer::footer ();

		return array ( self::getAttribute ( 'name' )->value, array ( $ret ) );
	}

	public static function import ( &$ref, $contents ) {
		$final = new Supplier ();

		foreach ( $contents->children () as $child ) {
			$name = $child->getName ();
			$value = $child;

			switch ( $name ) {
				case 'taxCode':
					$final->getAttribute ( 'tax_code' )->value = $value;
					break;

				case 'vatNumber':
					$final->getAttribute ( 'vat_number' )->value = $value;
					break;

				case 'name':
					$final->getAttribute ( 'name' )->value = $value;
					break;

				case 'address':
					$tmp = 'street:' . $contents->address->street . ';cap:' . $contents->address->zipCode . ';city:' . $contents->address->locality . ';';
					$final->getAttribute ( 'address' )->value = broken_address ( $tmp );
					break;

				case 'contacts':
					$tmp = $contents->xpath ( '//contacts/contact' );
					foreach ( $tmp as $t ) {
						if ( property_exists ( $t, 'primary' ) ) {
							$primary = $t->primary;

							foreach ( $primary as $contact_name => $contact_value ) {
								switch ( $contact_name ) {
									case 'phoneNumber':
										$final->getAttribute ( 'phone' )->value = $contact_value;
										break;

									case 'faxNumber':
										$final->getAttribute ( 'fax' )->value = $contact_value;
										break;

									case 'emailAddress':
										$final->getAttribute ( 'mail' )->value = $contact_value;
										break;

									case 'webSite':
										$final->getAttribute ( 'website' )->value = $contact_value;
										break;
								}
							}
						}
					}

				case 'note':
					$final->getAttribute ( 'description' )->value = $value;
					break;

				default:
					$proxy = SharableFromServer::mapTag ( $name );
					if ( $proxy != null )
						$proxy->import ( $ref, $contents->name );
			}
		}

		$ref->supplier = $final->exportable ();
	}
}

?>
