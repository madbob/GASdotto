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

class FromServerAttribute {
	public $name		= "";
	public $type		= "";
	public $value		= "";

	public function __construct ( $name, $type ) {
		$this->name = $name;
		$this->type = $type;

		list ( $type, $objtype ) = explode ( "::", $type );
		if ( $type == "ARRAY" )
			$this->value = array ();
		else
			$this->value = "";
	}

	public function traslate_field ( $value ) {
		list ( $type, $objtype ) = explode ( "::", $this->type );

		switch ( $type ) {
			case "STRING":
			case "INTEGER":
			case "FLOAT":
			case "DATE":
			case "BOOLEAN":
			case "PERCENTAGE":
				return $value;
				break;

			case "ADDRESS":
				$obj = new stdClass ();
				$tokens = explode ( ";", $value );

				for ( $i = 0; $i < count ( $tokens ); $i++ ) {
					list ( $name, $value ) = explode ( ":", $tokens [ $i ] );
					$obj->$name = $value;
				}

				return $obj;
				break;

			case "OBJECT":
				$obj = new $objtype;
				$obj->readFromDB ( $value );
				return $obj;
				break;

			case "ARRAY":
				break;

			default:
				return null;
		}
	}

	public function export_field () {
		list ( $type, $objtype ) = explode ( "::", $this->type );

		switch ( $type ) {
			case "STRING":
			case "INTEGER":
			case "FLOAT":
			case "DATE":
			case "PERCENTAGE":
				return $this->value . "";
				break;

			case "BOOLEAN":
				return $this->value == 1 ? "true" : "false";
				break;

			case "ADDRESS":
				return $this->value;
				break;

			case "OBJECT":
				return $this->value->exportable ();
				break;

			case "ARRAY":
				$ret = array ();
				for ( $i = 0; $i < count ( $this->value ); $i++ ) {
					$obj = $this->value [ $i ];
					array_push ( $ret, $obj->exportable () );
				}
				return $ret;
				break;

			default:
				return null;
		}
	}
}

abstract class FromServer {
	protected	$classname	= "";
	protected	$tablename	= "";
	protected	$attributes	= array ();

	protected function __construct ( $name, $tablename = '' ) {
		$this->classname = $name;

		if ( $tablename == '' )
			$this->tablename = $this->classname;
		else
			$this->tablename = $tablename;

		$attr = new FromServerAttribute ( 'id', "STRING" );
		$attr->value = "-1";
		array_push ( $this->attributes, $attr );
	}

	protected function addAttribute ( $name, $type, $default = "" ) {
		$attr = new FromServerAttribute ( $name, $type );

		if ( $default != "" )
			$attr->value = $default;

		array_push ( $this->attributes, $attr );
	}

	public function getAttribute ( $name ) {
		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];
			if ( $attr->name == $name )
				return $attr;
		}

		return null;
	}

	public function readFromDB ( $id ) {
		$query = sprintf ( "SELECT * FROM %s WHERE id = %d", $this->tablename, $id );
		$returned = query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );
		$row = $returned->fetch ( PDO::FETCH_ASSOC );

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];

			if ( isset ( $row [ $attr->name ] ) )
				$attr->value = $attr->traslate_field ( $row [ $attr->name ] );
		}
	}

	public function get ( $request ) {
		$ret = array ();

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query = sprintf ( "SELECT id FROM %s
						WHERE id NOT IN ( %s )
							ORDER BY id",
								$this->tablename, $ids );
		}
		else
			$query = sprintf ( "SELECT id FROM %s
						ORDER BY id",
							$this->tablename );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );

		while ( $row = $returned->fetch ( PDO::FETCH_ASSOC ) ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable () );
		}

		return $ret;
	}

	protected function from_object_to_internal ( $obj ) {
		$fields = get_object_vars ( $obj );
		$keys = array_keys ( $fields );

		for ( $i = 0; $i < count ( $fields ); $i++ ) {
			$attr = $this->getAttribute ( $keys [ $i ] );
			if ( $attr == null )
				continue;

			$val = $fields [ $keys [ $i ] ];

			if ( strncmp ( $attr->type, "OBJECT", strlen ( "OBJECT" ) ) == 0 )
				$attr->value = $val->id . "";
			else
				$attr->value = $val . "";
		}
	}

	protected function attr_to_db ( $attr ) {
		list ( $type, $objtype ) = explode ( "::", $attr->type );

		switch ( $type ) {
			case "STRING":
				$ret = "'" . ( addslashes ( $attr->value ) ) . "'";
				break;

			case "INTEGER":
			case "FLOAT":
			case "BOOLEAN":
			case "PERCENTAGE":
			case "OBJECT":
				$ret = $attr->value;
				break;

			case "DATE":
				$ret = "DATE('" . $attr->value . "')";
				break;

			/**
				TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO

				Aggiungere trattamento indirizzi

				TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
			*/
			case "ADDRESS":
				break;

			/**
				@todo	Attualmente gli array non sono gestiti in modo automatico, in
					quanto si presuppone che i dati all'interno di un array siano da
					trattare in modo particolare (magari in una tabella diversa da
					quella principale per il tipo. Sarebbe opportuno inventarsi un
					modo per trattare in modo trasparente anche tali dati, magari
					definendo in fase di specifica del campo quale tabella deve
					contenere suddetti elementi ed appoggiandosi su un formalismo
					unico con cui maneggiarli sempre allo stesso modo
			*/
			case "ARRAY":
				$ret = null;
				break;
		}

		return $ret;
	}

	public function save ( $obj ) {
		$this->from_object_to_internal ( $obj );

		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		if ( $id == -1 ) {
			$names = array ();
			$values = array ();

			for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
				$attr = $this->attributes [ $i ];

				if ( $attr->name == "id" )
					continue;

				$value = $this->attr_to_db ( $attr );
				if ( $value == null )
					continue;

				array_push ( $names, $attr->name );
				array_push ( $values, $value );
			}

			$query = sprintf ( "INSERT INTO %s ( %s )
						VALUES ( %s )",
							$this->tablename, join ( ", ", $names ), join ( ", ", $values ) );
			$ret = last_id ( $this->tablename );
		}
		else {
			$values = array ();

			for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
				$attr = $this->attributes [ $i ];

				if ( $attr->name == "id" )
					continue;

				$value = $this->attr_to_db ( $attr );
				if ( $value == null )
					continue;

				array_push ( $values, ( $attr->name . " = " . $value ) );
			}

			$query = sprintf ( "UPDATE %s SET %s
						WHERE id = %d",
							$this->tablename, join ( ", ", $values ), $id );
			$ret = $id;
		}

		query_and_check ( $query, "Impossibile salvare oggetto " . $this->classname );
		return $ret;
	}

	public function exportable () {
		$obj = new stdClass ();

		$obj->type = $this->classname;

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];
			$name = $attr->name;
			$obj->$name = $attr->export_field ();
		}

		return $obj;
	}
}

?>
