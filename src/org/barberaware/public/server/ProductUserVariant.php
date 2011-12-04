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

class ProductUserVariant extends FromServer {
	public function __construct () {
		parent::__construct ( "ProductUserVariant" );

		$this->addAttribute ( "delivered", "BOOLEAN" );
		$this->addAttribute ( "components", "ARRAY::ProductUserVariantComponent" );

		$this->setPublic ( false, 'asc', 'ProductUser', 'variants' );
	}

	private function sort_components ( $first, $second ) {
		return strcmp ( $first->variant->name, $second->variant->name );
	}

	public function get ( $request, $compress ) {
		$ret = parent::get ( $request, $compress );

		foreach ( $ret as $i )
			usort ( $i->components, "sort_components" );

		return $ret;
	}
}

?>
