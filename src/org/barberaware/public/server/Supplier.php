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

require_once ( "utils.php" );

class Supplier extends FromServer {
	public function __construct () {
		parent::__construct ( "Supplier" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "contact", "STRING" );
		$this->addAttribute ( "phone", "STRING" );
		$this->addAttribute ( "fax", "STRING" );
		$this->addAttribute ( "mail", "STRING" );
		$this->addAttribute ( "website", "STRING" );
		$this->addAttribute ( "address", "ADDRESS" );
		$this->addAttribute ( "order_mode", "STRING" );
		$this->addAttribute ( "paying_mode", "STRING" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "references", "ARRAY::User" );
		$this->addAttribute ( "carriers", "ARRAY::User" );
		$this->addAttribute ( "files", "ARRAY::CustomFile" );
		$this->addAttribute ( "orders_months", "STRING" );
		$this->addAttribute ( "shipping_manage", "INTEGER" );

		$this->setSorting ( "name" );
		$this->setPublic ( false );
	}
}

?>
