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

class Product extends FromServer {
	public function __construct () {
		parent::__construct ( "Product" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "code", "STRING" );
		$this->addAttribute ( "category", "OBJECT::Category" );
		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "shipping_price", "FLOAT" );
		$this->addAttribute ( "unit_price", "FLOAT" );
		$this->addAttribute ( "surplus", "PERCENTAGE" );
		$this->addAttribute ( "measure", "OBJECT::Measure" );
		$this->addAttribute ( "minimum_order", "FLOAT" );
		$this->addAttribute ( "multiple_order", "FLOAT" );
		$this->addAttribute ( "stock_size", "FLOAT" );
		$this->addAttribute ( "unit_size", "FLOAT" );
		$this->addAttribute ( "mutable_price", "BOOLEAN" );
		$this->addAttribute ( "available", "BOOLEAN" );
		$this->addAttribute ( "archived", "BOOLEAN" );
		$this->addAttribute ( "previous_description", "INTEGER" );
	}

	public function get ( $request, $compress ) {
		$ret = array ();

		if ( isset ( $request->supplier ) )
			$tuning = sprintf ( " AND supplier = %d ", $request->supplier );
		else
			$tuning = "";

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query = sprintf ( "SELECT id FROM %s WHERE id NOT IN ( %s ) AND archived = false %s ORDER BY id",
						$this->tablename, $ids, $tuning );
		}
		else {
			$query = sprintf ( "SELECT id FROM %s WHERE archived = false %s ORDER BY id",
						$this->tablename, $tuning );
		}

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
		}

		return $ret;
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
}

?>
