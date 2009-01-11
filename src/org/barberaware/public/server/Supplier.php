<?

/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		$this->addAttribute ( "address", "ADDRESS" );
		$this->addAttribute ( "order_mode", "STRING" );
		$this->addAttribute ( "paying_mode", "STRING" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "references", "ARRAY::User" );
	}

	public function readFromDB ( $id ) {
		parent::readFromDB ( $id );

		$references = $this->getAttribute ( "references" );

		$query = sprintf ( "SELECT * FROM supplier_references WHERE parent = %d", $id );
		$returned = query_and_check ( $query, "Impossibile recuperare lista referenti per fornitore " . $id );

		while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
			$obj = new User ();
			$obj->readFromDB ( $row [ "target" ] );
			array_push ( $references->value, $obj );
		}
	}

	public function save ( $obj ) {
		$id = parent::save ( $obj );

		$query = sprintf ( "SELECT target FROM supplier_references
					WHERE parent = %d",
						$id );
		$existing = query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );

		while ( $row = $existing->fetch ( PDO::FETCH_ASSOC ) ) {
			$found = false;

			for ( $i = 0; $i < count ( $obj->references ); $i++ ) {
				$ref = $obj->references [ $i ];

				if ( $ref->id == $row [ 'target' ] ) {
					$ref->id = -1;
					$found = true;
					break;
				}
			}

			if ( $found == false ) {
				$query = sprintf ( "DELETE FROM supplier_references
							WHERE parent = %d
							AND target = %d",
								$id, $row [ 'target' ] );
				query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );
			}
		}

		for ( $i = 0; $i < count ( $obj->references ); $i++ ) {
			$ref = $obj->references [ $i ];

			if ( $ref->id != -1 ) {
				$query = sprintf ( "INSERT INTO supplier_references ( parent, target )
							VALUES ( %d, %d )",
								$id, $ref->id );
				query_and_check ( $query, "Impossibile sincronizzare oggetto " . $this->classname );
			}
		}

		return $id;
	}
}

?>
