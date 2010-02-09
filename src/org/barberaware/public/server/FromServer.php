<?

/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

	public function traslate_field ( $parent, $value ) {
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
					if ( strlen ( $tokens [ $i ] ) != 0 ) {
						list ( $name, $value ) = explode ( ":", $tokens [ $i ] );
						if ( $name != "" )
							$obj->$name = $value;
					}
				}

				return $obj;
				break;

			case "OBJECT":
				$obj = new $objtype;
				$obj->readFromDB ( $value );
				return $obj;
				break;

			case "ARRAY":
				$ret = array ();
				$attr = $parent->getAttribute ( "id" );
				$id = $attr->value;

				$query = sprintf ( "SELECT target FROM %s_%s WHERE parent = %d",
							$parent->tablename, $this->name, $id );

				$existing = query_and_check ( $query, "Impossibile recuperare array per " . $parent->classname );
				$rows = $existing->fetchAll ( PDO::FETCH_ASSOC );

				foreach ( $rows as $row ) {
					$subobj = new $objtype;
					$subobj->readFromDB ( $row [ 'target' ] );
					array_push ( $ret, $subobj );
				}

				return $ret;
				break;

			default:
				return null;
		}

		return null;
	}

	private static function filter_object ( $object, $filter, $compress ) {
		if ( $filter != null ) {
			$id_name = 'has_' . $object->classname;

			if ( isset ( $filter->$id_name ) ) {
				$id = $object->getAttribute ( "id" )->value;

				if ( search_in_array ( $filter->$id_name, $id ) != -1 )
					return $id . "";
			}
		}

		return $object->exportable ( $filter, $compress );
	}

	public function export_field ( $parent, $filter, $compress ) {
		list ( $type, $objtype ) = explode ( "::", $this->type );

		switch ( $type ) {
			case "STRING":
				if ( $compress == true && $this->value == "" )
					return null;
				else
					return $this->value . "";
				break;

			case "INTEGER":
				if ( $compress == true && $this->value == "0" )
					return null;
				else
					return $this->value . "";
				break;

			case "FLOAT":
				if ( $compress == true && $this->value == "0" )
					return null;
				else
					return $this->value . "";
				break;

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
				return self::filter_object ( $this->value, $filter, $compress );
				break;

			case "ARRAY":
				$elements_num = count ( $this->value );
				if ( $avoid_defaults == true && $elements_num == 0 )
					return null;

				$ret = array ();

				for ( $i = 0; $i < $elements_num; $i++ ) {
					$obj = $this->value [ $i ];
					array_push ( $ret, self::filter_object ( $obj, $filter, $compress ) );
				}

				return $ret;
				break;

			default:
				return null;
		}

		return null;
	}
}

abstract class FromServer {
	public		$classname	= "";
	public		$tablename	= "";
	public		$sorting	= "id";
	public		$user_check	= null;
	public		$attributes	= array ();

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

	protected function setSorting ( $sorter ) {
		/*
			Hack!!! Attenzione!!!
			Sul lato client quando arriva un array di elementi (solitamenti allo
			startup) vengono immessi nei pannelli in ordine inverso (viene riempita
			sempre la posizione 0, quella piu' in alto, a alla fine l'ultimo risulta
			essere il primo). Per bilanciare tale problema e fare in modo che alla
			fine siano visualizzati nell'ordine atteso glieli si fornisce ordinati al
			contrario.
			Questo si potrebbe risolvere mettendo via via gli elementi nei pannelli
			al fondo anziche' in cima, ma sarebbe un po' piu' lento; valutare un
			compromesso operativo
		*/
		$this->sorting = $sorter . " DESC";
	}

	protected function enforceUserCheck ( $field_to_check ) {
		$this->user_check = $field_to_check;
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
		global $cache;

		$token = $this->tablename . "::" . $id;

		$ret = $cache [ $token ];
		if ( isset ( $ret ) ) {
			$this->attributes = $ret->attributes;
			return;
		}

		$query = sprintf ( "SELECT * FROM %s WHERE id = %d", $this->tablename, $id );
		$returned = query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );
		$row = $returned->fetchAll ( PDO::FETCH_ASSOC );

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];

			if ( isset ( $row [ 0 ] [ $attr->name ] ) )
				$attr->value = $attr->traslate_field ( $this, $row [ 0 ] [ $attr->name ] );
			else
				$attr->value = $attr->traslate_field ( $this, null );
		}

		$cache [ $token ] = $this;
	}

	public function get ( $request, $compress ) {
		global $current_user;

		$ret = array ();

		if ( isset ( $request ) && ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query = sprintf ( "SELECT id FROM %s WHERE id NOT IN ( %s )", $this->tablename, $ids );
		}
		else {
			/*
				Per far quagliare la concatenazione di altri frammenti di query
				forzo l'esistenza di uno statement WHERE cui accodare gli altri
				in AND
			*/
			$query = sprintf ( "SELECT id FROM %s WHERE true", $this->tablename, $check_query );
		}

		if ( isset ( $request ) && isset ( $request->id ) )
			$query .= sprintf ( " AND id = %d ", $request->id );

		if ( $this->user_check != null ) {
			if ( current_permissions () == 0 )
				$query .= sprintf ( " AND %s = %d ", $this->user_check, $current_user );
		}

		$query .= sprintf ( " ORDER BY %s", $this->sorting );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
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

			if ( strncmp ( $attr->type, "OBJECT", strlen ( "OBJECT" ) ) == 0 ) {
				if ( is_object ( $val ) )
					$attr->value = $val->id . "";
				else
					$attr->value = $val . "";
			}

			else if ( strcmp ( $attr->type, "ADDRESS" ) == 0 ) {
				$final = "";
				$addr_fields = get_object_vars ( $val );
				$addr_keys = array_keys ( $addr_fields );

				for ( $a = 0; $a < count ( $addr_fields ); $a++ )
					$final .= $addr_keys [ $a ] . ":" . $addr_fields [ $addr_keys [ $a ] ] . ";";

				$attr->value = $final;
			}

			else
				$attr->value = $val . "";
		}
	}

	protected function attr_to_db ( $attr ) {
		list ( $type, $objtype ) = explode ( "::", $attr->type );

		switch ( $type ) {
			case "STRING":
			case "ADDRESS":
			case "PERCENTAGE":
				$ret = "'" . ( addslashes ( $attr->value ) ) . "'";
				break;

			case "INTEGER":
			case "FLOAT":
			case "BOOLEAN":
			case "OBJECT":
				$ret = $attr->value;
				break;

			case "DATE":
				if ( $attr->value == "" )
					$ret = null;
				else
					$ret = "DATE('" . $attr->value . "')";
				break;

			case "ARRAY":
				/*
					Gli array non vengono trattati in questa funzione, si
					occupa poi save_arrays() di salvare su DB le raccolte di
					oggetti
				*/
				$ret = null;
				break;
		}

		return $ret;
	}

	protected function save_arrays ( $fresh, $obj, $id ) {
		if ( $fresh == true ) {
			for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
				$attr = $this->attributes [ $i ];
				list ( $type, $objtype ) = explode ( "::", $attr->type );

				if ( $type == "ARRAY" ) {
					$name = $attr->name;
					$arr = $obj->$name;

					for ( $a = 0; $a < count ( $arr ); $a++ ) {
						$element = $arr [ $a ];
						$singleid = $element->id;

						if ( $singleid == -1 ) {
							$tmpobj = new $element->type ();
							$singleid = $tmpobj->save ( $element );
						}

						$query = sprintf ( "INSERT INTO %s_%s ( parent, target ) VALUES ( %d, %d )",
									$this->tablename, $name, $id, $singleid );

						query_and_check ( $query, "Impossibile salvare array" );
					}
				}
			}
		}
		else {
			for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
				$attr = $this->attributes [ $i ];
				list ( $type, $objtype ) = explode ( "::", $attr->type );

				if ( $type == "ARRAY" ) {
					$name = $attr->name;
					$arr = $obj->$name;

					$query = sprintf ( "SELECT target FROM %s_%s WHERE parent = %d",
								$this->tablename, $name, $id );
					$existing = query_and_check ( $query, "Impossibile recuperare lista per sincronizzare oggetto " . $this->classname );
					$rows = $existing->fetchAll ( PDO::FETCH_ASSOC );

					/*
						Procedimento:
						- raccolgo tutti gli elementi gia' salvati nel DB
						- li confronto con quelli nell'array in ingresso
							- se sono nell'array e anche nel DB, gli assegno -100
							- se sono nel DB ma non nell'array, li elimino
						- ripasso l'array
							- se hanno ID = -1 li salvo nel DB come nuovi oggetti, ed assegno loro il nuovo ID
							- se hanno ID = -100 li salto (perche' ci sono gia', l'ho controllato prima)
							- se ID != -100, salvo
					*/

					foreach ( $rows as $row ) {
						$found = false;

						for ( $a = 0; $a < count ( $arr ); $a++ ) {
							$element = $arr [ $a ];
							$singleid = $element->id;

							if ( $singleid == $row [ 'target' ] ) {
								$tmpobj = new $element->type ();
								$tmpobj->save ( $element );

								$element->id = -100;
								$found = true;
								break;
							}
						}

						if ( $found == false ) {
							$query = sprintf ( "DELETE FROM %s_%s WHERE parent = %d AND target = %d",
										$this->tablename, $name, $id, $row [ 'target' ] );
							query_and_check ( $query, "Impossibile eliminare oggetto per sincronizzare oggetto " . $this->classname );
						}
					}

					for ( $a = 0; $a < count ( $arr ); $a++ ) {
						$single_data = $arr [ $a ];

						if ( $single_data->id != -100 ) {
							if ( $single_data->id == -1 ) {
								$tmpobj = new $single_data->type ();
								$single_data->id = $tmpobj->save ( $single_data );
							}

							$query = sprintf ( "INSERT INTO %s_%s ( parent, target ) VALUES ( %d, %d )",
										$this->tablename, $name, $id, $single_data->id );
							query_and_check ( $query, "Impossibile aggiungere elemento per sincronizzare oggetto " . $this->classname );
						}
					}
				}
			}
		}
	}

	public function save ( $obj ) {
		global $current_user;

		$this->from_object_to_internal ( $obj );

		$check_query = "";
		if ( $this->user_check != null ) {
			if ( current_permissions () == 0 ) {
				$verify = $this->getAttribute ( $this->user_check );
				if ( $verify->value != $current_user )
					error_exit ( "Invalid user" );
			}
		}

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

			$query = sprintf ( "INSERT INTO %s ( %s ) VALUES ( %s )",
						$this->tablename, join ( ", ", $names ), join ( ", ", $values ) );
			query_and_check ( $query, "Impossibile salvare oggetto " . $this->classname );

			$ret = last_id ( $this->tablename );
			$this->save_arrays ( true, $obj, $ret );
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

			$query = sprintf ( "UPDATE %s SET %s WHERE id = %d",
						$this->tablename, join ( ", ", $values ), $id );
			query_and_check ( $query, "Impossibile aggiornare oggetto " . $this->classname );

			$ret = $id;
			$this->save_arrays ( false, $obj, $ret );
		}

		return $ret;
	}

	private function destroy_related () {
		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];
			list ( $type, $objtype ) = explode ( "::", $attr->type );

			if ( $type == "ARRAY" ) {
				$query = sprintf ( "DELETE FROM %s_%s WHERE parent = %d",
						$this->tablename, $attr->name, $id );
				query_and_check ( $query, "Impossibile eliminare oggetti correlati a " . $this->classname );
			}
		}
	}

	private function destroy_myself () {
		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		$this->destroy_related ();

		$query = sprintf ( "DELETE FROM %s WHERE id = %d",
					$this->tablename, $id );
		query_and_check ( $query, "Impossibile eliminare oggetto " . $this->classname );
	}

	public function destroy ( $obj ) {
		$this->from_object_to_internal ( $obj );

		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		if ( $id != -1 )
			$this->destroy_myself ();

		return $id;
	}

	public function exportable ( $filter = null, $compress = false ) {
		$obj = new stdClass ();

		$obj->type = $this->classname;

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];

			$name = $attr->name;
			$value = $attr->export_field ( $this, $filter, $compress );

			if ( $value != null )
				$obj->$name = $value;
		}

		return $obj;
	}
}

?>
