<?php

/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

class ProductVariant extends SharableFromServer {
	public function __construct () {
		parent::__construct ( "ProductVariant" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "values", "ARRAY::ProductVariantValue" );

		$this->setSorting ( "name" );
	}

	public function export ( $options ) {
		/*
			TODO
		*/
	}

	public static function import ( &$ref, $contents ) {
		$ret = array ();

		$elements = $contents->xpath ( '//variants/variant' );

		foreach ( $elements as $el ) {
			$var = new ProductVariant ();

			foreach ( $el->attributes () as $name => $value ) {
				if ( $name == 'name' )
					$var->getAttribute ( 'name' )->value = $value;
			}

			$values = array ();
			$vals = $el->xpath ( '//variant/value' );

			foreach ( $vals as $val ) {
				$v = new ProductVariantValue ();
				$v->getAttribute ( 'name' )->value = $val;
				$values [] = $v;
			}

			$var->getAttribute ( 'values' )->value = $vals;
			$ret [] = $var->exportable ();
		}

		return $ret;
	}
}

?>
