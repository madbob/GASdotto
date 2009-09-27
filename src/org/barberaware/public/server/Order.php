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

class Order extends FromServer {
	public function __construct () {
		parent::__construct ( "Order", "orders" );

		$this->addAttribute ( "supplier", "OBJECT::Supplier" );
		$this->addAttribute ( "products", "ARRAY::Product" );
		$this->addAttribute ( "startdate", "DATE" );
		$this->addAttribute ( "enddate", "DATE" );
		$this->addAttribute ( "status", "INTEGER" );
		$this->addAttribute ( "shippingdate", "DATE" );
		$this->addAttribute ( "nextdate", "STRING" );
		$this->addAttribute ( "anticipated", "STRING" );
	}

	public function get ( $request ) {
		/*
			Non e' particolarmente efficiente fare il check sullo stato degli ordini
			ad ogni interrogazione, ma l'alternativa sarebbe piazzare uno script in
			cron rendendo piu' problematico il deploy dell'applicazione
		*/
		$query = sprintf ( "UPDATE %s SET status = 1 WHERE enddate < NOW()", $this->tablename );
		query_and_check ( $query, "Impossibile chiudere vecchi ordini" );

		/*
			Questo e' per aprire gli ordini che sono stati creati con data di inizio
			nel futuro
		*/
		$query = sprintf ( "UPDATE %s SET status = 0 WHERE status = 3 AND startdate < NOW() AND enddate > NOW()", $this->tablename );
		query_and_check ( $query, "Impossibile aprire ordini sospesi" );

		/**
			TODO	Settare status ordini con ciclicita'
		*/

		if ( isset ( $request->status ) ) {
			$ret = array ();

			$query = sprintf ( "SELECT id FROM %s WHERE status = %d ", $this->tablename, $request->status );

			if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
				$ids = join ( ',', $request->has );
				$query .= sprintf ( "AND id NOT IN ( %s ) ", $ids );
			}

			$query .= "ORDER BY id";

			$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );

			while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
				$obj = new $this->classname;
				$obj->readFromDB ( $row [ 'id' ] );
				array_push ( $ret, $obj->exportable () );
			}
		}
		else
			$ret = parent::get ( $request );

		return $ret;
	}

	public function save ( $obj ) {
		$obj->products = array ();
		$prod = new Product ();

		/*
			Quando salvo un nuovo ordine prendo per buoni i prodotti correntemente
			ordinabili presso il fornitore di riferimento, e li scrivo nell'apposita
			tabella
		*/
		if ( $obj->id == -1 )
			$query = sprintf ( "SELECT id FROM %s
						WHERE supplier = %d
						AND available = true
						AND archived = false
						ORDER BY id",
							$prod->tablename, $obj->supplier->id );
		else
			$query = sprintf ( "SELECT target FROM %s_%s WHERE parent = %d ORDER BY id",
						$this->tablename, "products", $obj->id );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $prod->classname );

		while ( $row = $returned->fetch ( PDO::FETCH_NUM ) ) {
			$product = new $prod->classname;
			$product->readFromDB ( $row [ 0 ] );
			array_push ( $obj->products, $product->exportable () );
		}

		/*
			Gli ordini con data di apertura nel futuro vengono marcati come "sospesi"
		*/
		$startdate = $obj->startdate . " 23:59:59";
		if ( strtotime ( $startdate ) > time () )
			$obj->status = 3;

		return parent::save ( $obj );
	}

	/**
		TODO	Re-implementare destroy(), da applicare solo sugli Orders che non sono
			mai stati applicati e distruggendo i Products rimasti archiviati apposta
			per esso
	*/
}

?>
