<?php

/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

/**
	TODO	I vari "explode" utilizzati per dividere i datatype sarebbero da rivedere,
		generano sfilze di warning ogni volta che non si sta usando un tipo composito (ad
		esempio "OBJECT::Tipo")
*/

class FromServerAttribute {
	public $name		= "";
	public $type		= "";
	public $value		= "";

	public function __construct ( $name, $type ) {
		$this->name = $name;
		$this->type = $type;

		if ( strncmp ( $type, "ARRAY", 5 ) == 0 )
			$this->value = array ();
		else
			$this->value = "";
	}

	public function traslate_field ( $parent, $value ) {
		if ( strstr ( $this->type, '::' ) == false )
			$type = $this->type;
		else
			list ( $type, $objtype ) = explode ( '::', $this->type );

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
				if ( $value != null ) {
					$obj = new $objtype;
					$obj->readFromDB ( $value );
					return $obj;
				}
				else {
					return null;
				}

				break;

			case "ARRAY":
				$ref = new $objtype;

				$ret = array ();
				$attr = $parent->getAttribute ( "id" );
				$id = $attr->value;

				$tabname = $parent->tablename . '_' . $this->name;

				$query = sprintf ( "SELECT target FROM %s, %s WHERE %s.parent = %d AND %s.id = %s.target %s ORDER BY %s.%s",
							$tabname,
							$ref->tablename, $tabname, $id, $ref->tablename, $tabname,
							$ref->filter_by_current_gas ( $tabname . '.target' ), $ref->tablename, $ref->sorting );

				$existing = query_and_check ( $query, "Impossibile recuperare array per " . $parent->classname );
				$rows = $existing->fetchAll ( PDO::FETCH_ASSOC );
				unset ( $existing );

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
		if ( $object != null ) {
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
		else {
			return null;
		}
	}

	public function export_field ( $parent, $filter, $compress ) {
		if ( strstr ( $this->type, '::' ) == false )
			$type = $this->type;
		else
			list ( $type, $objtype ) = explode ( '::', $this->type );

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
				if ( $elements_num == 0 )
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
	public		$is_public	= true;

	/*
		Qui, se is_public e' false, viene piazzato un array con le seguenti chiavi:

		mode: plain, desc o asc.
			plain = la classe e' mappata direttamente nella tabella delle ACL. "class" e "attribute" non vengono usati
			desc = dipendenza discendente: l'accesso alla classe dipende dai permessi di accesso dell'oggetto di classe
				"class" che si trova all'attributo "attribute"
			asc = dipendenza ascendente: l'accesso alla classe dipende dai permessi di accesso dell'oggetto di classe
				"class" che lo contiene all'interno dell'array "attribute"

		class: la classe da cui dipende questa classe

		attribute: l'attributo in cui si trova la dipendenza
	*/
	public		$public_mode	= null;

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

	protected function setPublic ( $public, $mode = 'plain', $dependency_class = '', $dependency_attribute = '' ) {
		$this->is_public = $public;
		$this->public_mode = array ( 'mode' => $mode, 'class' => $dependency_class, 'attribute' => $dependency_attribute );
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

		if ( array_key_exists ( $token, $cache ) && isset ( $cache [ $token ] ) ) {
			$ret = $cache [ $token ];
			$this->attributes = $ret->attributes;
			return;
		}

		$query = sprintf ( "SELECT * FROM %s WHERE id = %d", $this->tablename, $id );
		$returned = query_and_check ( $query, "Impossibile recuperare oggetto " . $this->classname );
		$row = $returned->fetchAll ( PDO::FETCH_ASSOC );
		unset ( $returned );

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];

			if ( isset ( $row [ 0 ] [ $attr->name ] ) )
				$val = $attr->traslate_field ( $this, $row [ 0 ] [ $attr->name ] );
			else
				$val = $attr->traslate_field ( $this, null );

			if ( $val !== null )
				$attr->value = $val;
		}

		$cache [ $token ] = $this;
	}

	public function get ( $request, $compress ) {
		global $current_user;

		$ret = array ();

		if ( $request != null && ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query = sprintf ( "SELECT id FROM %s WHERE id NOT IN ( %s )", $this->tablename, $ids );
		}
		else {
			/*
				Per far quagliare la concatenazione di altri frammenti di query
				forzo l'esistenza di uno statement WHERE cui accodare gli altri
				in AND
			*/
			$query = sprintf ( "SELECT id FROM %s WHERE true", $this->tablename );
		}

		if ( isset ( $request ) && isset ( $request->id ) )
			$query .= sprintf ( " AND id = %d ", $request->id );

		if ( $this->user_check != null ) {
			if ( current_permissions () == 0 )
				$query .= sprintf ( " AND %s = %d ", $this->user_check, $current_user );
		}

		$query .= $this->filter_by_current_gas ();

		$query .= sprintf ( " ORDER BY %s", $this->sorting );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
		unset ( $returned );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, $obj->exportable ( $request, $compress ) );
		}

		return $ret;
	}

	public function filter_by_current_gas ( $field_name = 'id' ) {
		if ( $this->is_public == true ) {
			return "";
		}
		else {
			switch ( $this->public_mode [ 'mode' ] ) {
				case 'desc':
					$ret = acl_filter_hierarchy_desc ( $this, $this->public_mode [ 'class' ], $this->public_mode [ 'attribute' ] );
					break;

				case 'asc':
					$ret = acl_filter_hierarchy_asc ( $this, $this->public_mode [ 'class' ], $this->public_mode [ 'attribute' ] );
					break;

				default:
				case 'plain':
					$ret = acl_filter_plain ( $this );
					break;
			}

			return " AND $field_name IN $ret";
		}
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

			else {
				$attr->value = $val . "";
			}
		}
	}

	protected function attr_to_db ( $attr ) {
		if ( strstr ( $attr->type, '::' ) == false )
			$type = $attr->type;
		else
			list ( $type, $objtype ) = explode ( '::', $attr->type );

		switch ( $type ) {
			case "STRING":
			case "ADDRESS":
			case "PERCENTAGE":
				$ret = "'" . ( addslashes ( $attr->value ) ) . "'";
				break;

			case "INTEGER":
			case "FLOAT":
			case "BOOLEAN":
				$ret = $attr->value;
				break;

			case "OBJECT":
				$ret = $attr->value;
				if ( $ret == -1 )
					$ret = 'null';
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

				if ( strstr ( $attr->type, '::' ) == false )
					$type = $attr->type;
				else
					list ( $type, $objtype ) = explode ( '::', $attr->type );

				if ( $type == "ARRAY" ) {
					$name = $attr->name;

					if ( property_exists ( $obj, $name ) == false )
						continue;

					$arr = $obj->$name;

					for ( $a = 0; $a < count ( $arr ); $a++ ) {
						$element = $arr [ $a ];

						if ( check_acl_easy ( $element->type, $element->id, 1 ) == false )
							continue;

						$singleid = $element->id;

						if ( $singleid == -1 ) {
							$tmpobj = new $element->type ();
							$singleid = $tmpobj->save ( $element );
						}

						unset ( $tmpobj );

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

				if ( strstr ( $attr->type, '::' ) == false )
					$type = $attr->type;
				else
					list ( $type, $objtype ) = explode ( '::', $attr->type );

				if ( $type == "ARRAY" ) {
					$name = $attr->name;

					if ( property_exists ( $obj, $name ) == false )
						continue;

					$arr = $obj->$name;
					$tmpobj = new $objtype ();

					$query = sprintf ( "SELECT target FROM %s_%s WHERE parent = %d %s ORDER BY target ASC",
								$this->tablename, $name, $id, $tmpobj->filter_by_current_gas ( 'target' ) );
					$existing = query_and_check ( $query, "Impossibile recuperare lista per sincronizzare oggetto " . $this->classname );
					$rows = $existing->fetchAll ( PDO::FETCH_ASSOC );
					unset ( $existing );
					unset ( $tmpobj );

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
								if ( check_acl_easy ( $element->type, $singleid, 1 ) ) {
									$tmpobj = new $element->type ();
									$tmpobj->save ( $element );
								}

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
							if ( check_acl_easy ( $single_data->type, $single_data->id, 1 ) == false )
								continue;

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
		global $dbdriver;
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

			if ( count ( $names ) == 0 ) {
				/*
					Questo e' per gestire correttamente le classi FromServer che non hanno
					contenuti diretti, ad esempio che contengono solo array di altri oggetti
				*/
				if ( $dbdriver == 'pgsql' ) {
					array_push ( $names, 'id' );
					array_push ( $values, "nextval('" . $this->tablename . "_id_seq'::regclass)" );
				}
				else {
					/*
						Sperimentalmente, MySQL assegna l'ID correttamente autoincrementato
						se si definisce -1 in fase di INSERT
					*/
					array_push ( $names, 'id' );
					array_push ( $values, '-1' );
				}
			}

			$query = sprintf ( "INSERT INTO %s ( %s ) VALUES ( %s )",
						$this->tablename, join ( ", ", $names ), join ( ", ", $values ) );
			query_and_check ( $query, "Impossibile salvare oggetto " . $this->classname );

			$ret = last_id ( $this->tablename );
			$this->save_arrays ( true, $obj, $ret );
			$this->getAttribute ( 'id' )->value = $ret;

			if ( $this->is_public == false )
				save_acl ( $this, 0 );
		}
		else {
			if ( $this->is_public == true || ( $this->is_public == false && check_acl ( $this, 1 ) == true ) ) {
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

				if ( count ( $values ) > 0 ) {
					$query = sprintf ( "UPDATE %s SET %s WHERE id = %d",
								$this->tablename, join ( ", ", $values ), $id );
					query_and_check ( $query, "Impossibile aggiornare oggetto " . $this->classname );
				}
			}

			$ret = $id;
			$this->save_arrays ( false, $obj, $ret );
		}

		return $ret;
	}

	private function destroy_related () {
		/*
			Sicuramente usare "ON DELETE CASCADE" sul database sarebbe piu'
			efficiente, ma rischierebbe di essere meno flessibile nei confronti delle
			query autogenerate in checkdb.php
		*/

		/*
			TODO	Qui ci sarebbe da spianare anche gli oggetti che fanno riferimento a quello che si
				sta distruggendo, ma solo in determinati casi (eliminando un Product si eliminano le
				relative varianti, ma distruggendo un OrderAggregate non si distruggono gli Order)
		*/

		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];

			if ( strstr ( $attr->type, '::' ) == false )
				$type = $attr->type;
			else
				list ( $type, $objtype ) = explode ( '::', $attr->type );

			if ( $type == "ARRAY" ) {
				$query = sprintf ( "DELETE FROM %s_%s WHERE parent = %d",
						$this->tablename, $attr->name, $id );
				query_and_check ( $query, "Impossibile eliminare oggetti correlati a " . $this->classname );
			}
		}

		$classes = get_from_server_classes ();

		foreach ( $classes as $class ) {
			$tmp = new $class;

			for ( $i = 0; $i < count ( $tmp->attributes ); $i++ ) {
				$attr = $tmp->attributes [ $i ];

				if ( strstr ( $attr->type, '::' ) == true ) {
					list ( $type, $objtype ) = explode ( '::', $attr->type );

					if ( $objtype == $this->classname ) {
						if ( $type == "OBJECT" ) {
							$query = sprintf ( "SELECT id FROM %s WHERE %s = %d", $tmp->tablename, $attr->name, $id );
							$existing = query_and_check ( $query, "Impossibile trovare oggetti correlati a " . $this->classname );
							$rows = $existing->fetchAll ( PDO::FETCH_ASSOC );

							foreach ( $rows as $row ) {
								$tmp->readFromDB ( $row [ 'id' ] );
								$tmp->destroy_myself ();
							}
						}
						else if ( $type == "ARRAY" ) {
							$query = sprintf ( "DELETE FROM %s_%s WHERE target = %d", $tmp->tablename, $attr->name, $id );
							query_and_check ( $query, "Impossibile eliminare oggetti correlati a " . $this->classname );
						}
					}
				}
			}

			unset ( $tmp );
		}
	}

	public function destroy_myself () {
		$attr = $this->getAttribute ( "id" );
		$id = $attr->value;

		$this->destroy_related ();

		$query = sprintf ( "DELETE FROM %s WHERE id = %d",
					$this->tablename, $id );
		query_and_check ( $query, "Impossibile eliminare oggetto " . $this->classname );
		destroy_acl ( $this );
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

		if ( $this->is_public == false )
			$obj->sharing_privileges = get_acl ( $this );

		return $obj;
	}
}

?>
