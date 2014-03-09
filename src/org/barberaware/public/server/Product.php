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

class Product extends SharableFromServer {
	public function __construct () {
		parent::__construct ( "Product" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "code", "STRING" );
		$this->addAttribute ( "category", "OBJECT::Category" );
		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "photo", "STRING" );
		$this->addAttribute ( "shipping_price", "FLOAT" );
		$this->addAttribute ( "unit_price", "FLOAT" );
		$this->addAttribute ( "surplus", "PERCENTAGE" );
		$this->addAttribute ( "measure", "OBJECT::Measure" );
		$this->addAttribute ( "minimum_order", "FLOAT" );
		$this->addAttribute ( "multiple_order", "FLOAT" );
		$this->addAttribute ( "total_max_order", "FLOAT" );
		$this->addAttribute ( "stock_size", "FLOAT" );
		$this->addAttribute ( "unit_size", "FLOAT" );
		$this->addAttribute ( "mutable_price", "BOOLEAN" );
		$this->addAttribute ( "available", "BOOLEAN" );
		$this->addAttribute ( "variants", "ARRAY::ProductVariant" );
		$this->addAttribute ( "archived", "BOOLEAN" );
		$this->addAttribute ( "previous_description", "INTEGER" );

		$this->setPublic ( false, 'desc', 'Supplier', 'supplier' );
	}

	public function get ( $request, $compress ) {
		$query = 'archived = false';

		if ( $request != null && property_exists ( $request, 'supplier' ) )
			$query .= sprintf ( " AND supplier = %d ", $request->supplier );

		return parent::getByQuery ( $request, $compress, $query );
	}

	public function save ( $obj ) {
		if ( $obj->id != -1 ) {
			if ( $obj->available == "false" ) {
				$query = sprintf ( "DELETE FROM Orders_products WHERE target = %d", $obj->id );
				query_and_check ( $query, "Impossibile eliminare prodotto non piu' ordinabile" );

				$query = sprintf ( "DELETE FROM ProductUser WHERE product = %d", $obj->id );
				query_and_check ( $query, "Impossibile eliminare prodotto non piu' ordinabile" );

				/*
					Da orderuser_products viene eliminato in cascata con la rimozione da
					productuser
				*/
			}
			else {
				/*
					Questo e' per fornire un'ultima speranza di salvezza qualora venga avanzata
					una richiesta di salvataggio per un prodotto che per qualche motivo e'
					contemplato in un ordine ma non e' ancora stato archiviato (e/o il cui
					aggiornamento sullo stato di archiviazione non sia giunto al client, che
					dunque vuole salvarlo con uno stato sbagliato).
					Da notare che comunque viene fatto pure un check sul fatto che esista davvero
					un prodotto successivo a quello or ora modificato, onde evitare di archiviare
					un prodotto che non deve essere archiviato. Se si avvera, la situazione e'
					quantomeno imbarazzante in quanto non dovrebbe mai verificarsi, ma cerchiamo
					di salvare il salvabile
				*/
				if ( $obj->archived == "false" ) {
					$query = sprintf ( "FROM Orders_products WHERE target = %d", $obj->id );
					if ( db_row_count ( $query ) != 0 ) {
						$query = sprintf ( "FROM %s WHERE previous_description = %d", $this->tablename, $obj->id );
						if ( db_row_count ( $query ) != 0 )
							$obj->archived = "true";
					}
				}
			}
		}

		return parent::save ( $obj );
	}

	public function destroy ( $obj ) {
		$query = sprintf ( "UPDATE Product SET previous_description = %d WHERE previous_description = %d", $obj->previous_description, $obj->id );
		query_and_check ( $query, "Impossibile allineare riferimenti storici del prodotto" );

		parent::destroy ( $obj );
		return $obj->id;
	}

	public function export ( $options ) {
		/*
			TODO
		*/
	}

	public static function import ( &$ref, $contents ) {
		$ret = array ();

		$elements = $contents->xpath ( '//products/product' );

		foreach ( $elements as $el ) {
			$final = new Product ();

			foreach ( $el->children () as $child ) {
				$name = $child->getName ();
				$value = $child;

				switch ( $name ) {
					case 'sku':
						$final->getAttribute ( 'code' )->value = $value;
						break;

					case 'name':
						$final->getAttribute ( 'name' )->value = $value;
						break;

					case 'category':
						$cat = new Category ();
						$cat->readFromDBAlt ( array ( 'name' => $value ), true );
						$final->getAttribute ( 'category' )->value = $cat;
						break;

					case 'um':
						$um = new Measure ();
						$um->readFromDBAlt ( array ( 'name' => $value ), true );
						$final->getAttribute ( 'measure' )->value = $um;
						break;

					case 'description':
						$final->getAttribute ( 'description' )->value = $value;
						break;

					case 'orderInfo':
						foreach ( $child->children () as $subchild ) {
							$info_name = $subchild->getName ();
							$info_value = $subchild;

							switch ( $info_name ) {
								case 'packageQty':
									$final->getAttribute ( 'stock_size' )->value = $info_value;
									break;

								case 'minQty':
									$final->getAttribute ( 'minimum_order' )->value = $info_value;
									break;

								case 'mulQty':
									$final->getAttribute ( 'multiple_order' )->value = $info_value;
									break;

								case 'maxQty':
									$final->getAttribute ( 'total_max_order' )->value = $info_value;
									break;

								case 'umPrice':
									$final->getAttribute ( 'unit_price' )->value = $info_value;
									break;

								case 'shippingCost':
									$final->getAttribute ( 'shipping_price' )->value = $info_value;
									break;
							}
						}

						break;

					case 'variants':
						$v = SharableFromServer::mapTag ( $name );
						$final->getAttribute ( 'variants' )->value = $v->import ( null, $contents->$name );
						break;
				}
			}

			$ret [] = $final->exportable ();
		}

		$ref->products = $ret;
	}
}

?>
