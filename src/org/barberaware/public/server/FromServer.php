<?php

/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
	public $name			= "";
	public $type			= "";
	public $value			= "";
	public $preserveOnDestroy	= false;

	public function __construct ( $name, $type ) {
		$this->name = $name;
		$this->type = $type;

		if ( strncmp ( $type, "ARRAY", 5 ) == 0 )
			$this->value = array ();
		else
			$this->value = "";
	}

	public function preserve () {
		$this->preserveOnDestroy = true;
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
				return broken_address ( $value );
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

	public static function filter_object ( $object, &$filter, $compress ) {
		if ( $object != null ) {
			if ( is_numeric ( $object ) == true )
				return $object . "";

			if ( $filter === null )
				$filter = new stdClass ();

			$id = $object->getAttribute ( "id" )->value;
			$id_name = 'has_' . $object->classname;

			if ( isset ( $filter->$id_name ) ) {
				if ( search_in_array ( $filter->$id_name, $id ) != -1 )
					return $id . "";
			}
			else {
				$filter->$id_name = array ();
			}

			array_push ( $filter->$id_name, $id );
			return $object->exportable ( $filter, $compress );
		}
		else {
			return null;
		}
	}

	public function export_field ( $parent, &$filter, $compress ) {
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

	public function duplicate () {
		$ret = new FromServerAttribute ( $this->name, $this->type );
		$ret->value = $this->value;
		return $ret;
	}
}

abstract class FromServer {
	public		$classname	= "";
	public		$tablename	= "";
	public		$sorting	= "id DESC";
	public		$user_check	= null;
	public		$attributes	= array ();
	public		$is_public	= true;
	public		$back_destroy	= true;

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

	/*
		Da invocare sugli attributi facenti riferimento ad altri oggetti
		i quali non devono essere distrutti in caso di eliminazione
		dell'elemento per mezzo della funzione destroy()
	*/
	public function preserveAttribute ( $name ) {
		$a = $this->getAttribute ( $name );
		$a->preserve ();
	}

	/*
		Se invocata, un oggetto della classe di riferimento non transita
		per destroy_related() (dunque la catena di eliminazioni non
		risale)
	*/
	public function noBackDestroy () {
		$this->back_destroy = false;
	}

	public function readFromDB ( $id ) {
		global $cache;

		$token = $this->tablename . "::" . $id;

		if ( array_key_exists ( $token, $cache ) && isset ( $cache [ $token ] ) ) {
			$ret = $cache [ $token ];
			$this->attributes = $ret->attributes;
			return;
		}

		$cache [ $token ] = &$this;

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
	}

	public function readFromDBAlt ( $parameters, $create ) {
		$strings = array ();

		foreach ( $parameters as $field => $value ) {
			$attr = $this->getAttribute ( $field )->duplicate ();
			$attr->value = $value;
			$strings [] = "$field = " . $this->attr_to_db ( $attr );
			unset ( $attr );
		}

		$query = "FROM " . $this->tablename . " WHERE " . join ( ' AND ', $strings );

		if ( db_row_count ( $query ) == 0 ) {
			if ( $create == true ) {
				foreach ( $parameters as $field => $value )
					$this->getAttribute ( $field )->value = $value;

				$this->save ( $this->exportable () );
			}
			else {
				return false;
			}
		}
		else {
			$query = "SELECT id " . $query;
			$returned = query_and_check ( $query, "Impossibile recuperare oggetto con parametri aggiuntivi" );
			$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
			$this->readFromDB ( $rows [ 0 ] [ 'id' ] );
		}

		return true;
	}

	/*
		Per selezionare in funzione del valore assunto da un dato attributo dell'oggetto in
		relazione
	*/
	public function arrayRelationCustomQuery ( $type, $column, $op, $value ) {
		$attr = $this->getAttribute ( $type );

		if ( strstr ( $attr->type, '::' ) == false )
			error_exit ( 'Impossibile ricostruire relazione su attributo non di tipo ARRAY' );
		else
			list ( $stype, $objtype ) = explode ( '::', $attr->type );

		$table = $this->tablename;

		$obj = new $objtype ();
		$othertable = $obj->tablename;

		$ret = "id IN (SELECT ${table}_{$type}.parent FROM ${table}_${type}, ${othertable} WHERE ${table}_${type}.target = ${othertable}.id AND ${othertable}.${column} $op $value)";

		unset ( $obj );
		return $ret;
	}

	/*
		Per selezionare in funzione di una specifica relazione
	*/
	public function arrayRelationQuery ( $type, $value ) {
		$table = $this->tablename;
		$ret = "id IN (SELECT ${table}_{$type}.parent FROM ${table}_${type} WHERE ${table}_${type}.target = ${value})";
		return $ret;
	}

	protected function getByQuery ( $request, $compress, $partial_query = null ) {
		global $current_user;

		$ret = array ();

		$query = sprintf ( "SELECT id FROM %s WHERE ", $this->tablename );

		/*
			Se viene richiesto un ID specifico solitamente sto
			ricaricando un ordine utente e cose del genere (oggetto
			con alwaysReload() nel client), di cui gia' ho l'ID
			locale ma che appunto devo ricaricare. Ignoro dunque il
			filtro in 'has'
		*/
		if ( $request != null && property_exists ( $request, 'id' ) ) {
			$query .= sprintf ( "id = %d ", $request->id );
		}
		else {
			if ( $request != null && property_exists ( $request, 'has' ) && ( count ( $request->has ) != 0 ) ) {
				$ids = join ( ',', $request->has );
				$query .= sprintf ( "id NOT IN ( %s )", $ids );
			}
			else {
				/*
					Per far quagliare la concatenazione di altri frammenti di query
					forzo l'esistenza di uno statement WHERE cui accodare gli altri
					in AND
				*/
				$query .= sprintf ( "id > 0" );
			}
		}

		if ( $this->user_check != null ) {
			if ( current_permissions () == 0 )
				$query .= sprintf ( " AND %s = %d ", $this->user_check, $current_user );
		}

		if ( $partial_query != null )
			$query .= ' AND ' . $partial_query . ' ';

		$query .= $this->filter_by_current_gas ();

		$query .= sprintf ( " ORDER BY %s", $this->sorting );

		if ( isset ( $request->query_limit ) )
			$query .= sprintf ( " LIMIT %d", $request->query_limit );

		$returned = query_and_check ( $query, "Impossibile recuperare lista oggetti " . $this->classname );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
		unset ( $returned );

		foreach ( $rows as $row ) {
			$obj = new $this->classname;
			$obj->readFromDB ( $row [ 'id' ] );
			array_push ( $ret, FromServerAttribute::filter_object ( $obj, $request, $compress ) );
			unset ( $obj );
		}

		return $ret;
	}

	public function get ( $request, $compress ) {
		return $this->getByQuery ( $request, $compress, null );
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

			if ( $field_name === -1 )
				return $ret;
			else
				return " AND $field_name IN $ret";
		}
	}

	protected function from_object_to_internal ( $obj ) {
		$fields = get_object_vars ( $obj );
		$keys = array_keys ( $fields );

		for ( $i = 0; $i < count ( $fields ); $i++ ) {
			$attr = $this->getAttribute ( $keys [ $i ] );
			if ( $attr === null )
				continue;

			$val = $fields [ $keys [ $i ] ];

			if ( strncmp ( $attr->type, "OBJECT", strlen ( "OBJECT" ) ) == 0 ) {
				if ( is_object ( $val ) ) {
					if ( $val->id == -1 ) {
						list ( $type, $objtype ) = explode ( '::', $attr->type );
						$attr->value = new $objtype ();
						$attr->value->from_object_to_internal ( $val );
					}
					else {
						$attr->value = $val->id . "";
					}
				}
				else {
					$attr->value = $val . "";
				}
			}

			else if ( strcmp ( $attr->type, "ADDRESS" ) == 0 ) {
				if ( is_string ( $val ) == true ) {
					$final = $val;
				}
				else {
					$final = "";
					$addr_fields = get_object_vars ( $val );
					$addr_keys = array_keys ( $addr_fields );

					for ( $a = 0; $a < count ( $addr_fields ); $a++ )
						$final .= $addr_keys [ $a ] . ":" . $addr_fields [ $addr_keys [ $a ] ] . ";";
				}

				$attr->value = $final;
			}

			else {
				$attr->value = $val;
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

				if ( is_object ( $ret ) ) {
					if ( property_exists ( $ret, 'id' ) )
						$ret = $ret->id;
					else
						$ret = $ret->getAttribute ( 'id' )->value;

					if ( $ret == -1 )
						$ret = null;
				}
				else {
					if ( is_numeric ( $ret ) && $ret == -1 )
						$ret = null;
				}

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

						if ( is_object ( $element ) )
							$singleid = $element->id;
						else
							$singleid = $element;

						if ( check_acl_easy ( $objtype, $singleid, 1 ) == false )
							continue;

						if ( is_object ( $element ) && $singleid == -1 ) {
							$tmpobj = new $element->type ();
							$singleid = $tmpobj->save ( $element );
							unset ( $tmpobj );
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

					$founds = array ();

					for ( $a = 0; $a < count ( $arr ); $a++ ) {
						$element = $arr [ $a ];
						$found = false;

						if ( is_object ( $element ) )
							$singleid = $element->id;
						else
							$singleid = $element;

						if ( is_object ( $element ) ) {
							$tmpobj = new $element->type ();
							$singleid = $tmpobj->save ( $element );
							unset ( $tmpobj );
						}

						foreach ( $rows as $row ) {
							if ( $singleid == $row [ 'target' ] ) {
								$found = true;
								$founds [] = $singleid;
								break;
							}
						}

						if ( $found == false ) {
							$query = sprintf ( "INSERT INTO %s_%s ( parent, target ) VALUES ( %d, %d )",
										$this->tablename, $name, $id, $singleid );
							query_and_check ( $query, "Impossibile aggiungere elemento per sincronizzare oggetto " . $this->classname );
							$founds [] = $singleid;
						}
					}

					if ( count ( $founds ) != 0 ) {
						$query = sprintf ( "DELETE FROM %s_%s WHERE parent = %d AND target NOT IN (%s)",
									$this->tablename, $name, $id, join ( ', ', $founds ) );
					}
					else {
						$query = sprintf ( "DELETE FROM %s_%s WHERE parent = %d", $this->tablename, $name, $id );
					}

					query_and_check ( $query, "Impossibile eliminare oggetto per sincronizzare oggetto " . $this->classname );
				}
			}
		}
	}

	protected function save_objects ( $obj, $id ) {
		$updates = array ();

		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$attr = $this->attributes [ $i ];

			if ( strstr ( $attr->type, '::' ) == false )
				$type = $attr->type;
			else
				list ( $type, $objtype ) = explode ( '::', $attr->type );

			if ( $type == "OBJECT" ) {
				$name = $attr->name;

				if ( property_exists ( $obj, $name ) == false )
					continue;

				$sub = $obj->$name;

				if ( is_object ( $sub ) ) {
					$tmpobj = new $objtype ();
					$sub_id = $tmpobj->save ( $sub );
					$updates [] = "$name = $sub_id";
				}
			}
		}

		if ( count ( $updates ) != 0 ) {
			$query = sprintf ( "UPDATE %s SET %s WHERE id = %d",
						$this->tablename, join (', ', $updates), $id );

			query_and_check ( $query, "Impossibile salvare oggetti" );
		}
	}

	public function save ( $obj ) {
		global $dbdriver;
		global $current_user;

		if ( $obj instanceof FromServer )
			$this->dupBy ( $obj );
		else
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
				if ( $value === null )
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
			$this->save_objects ( $obj, $ret );
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
					if ( $value === null )
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
			$this->save_objects ( $obj, $ret );
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
				relative varianti, ma distruggendo un OrderAggregate non si distruggono gli Order).
				Applicare l'attributo preserveOnDestroy in FromServerAttribute
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

		if ( $this->back_destroy == false )
			return;

		$classes = get_from_server_classes ();

		foreach ( $classes as $class ) {
			$tmp = new ReflectionClass ( $class );
			if ( $tmp->IsInstantiable () == false )
				continue;

			$tmp = new $class;

			for ( $i = 0; $i < count ( $tmp->attributes ); $i++ ) {
				$attr = $tmp->attributes [ $i ];

				if ( strstr ( $attr->type, '::' ) == true ) {
					list ( $type, $objtype ) = explode ( '::', $attr->type );

					if ( $objtype == $this->classname ) {
						if ( $type == "OBJECT" ) {
							/*
								TODO	Questa procedura e' decisamente lenta, in
									quanto carica e distrugge tutti i singoli
									oggetti uno alla volta.
									Ottimizzare togliendo almeno il passaggio da
									readFromDB()
							*/
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

	/*
		Invocata all'atto della creazione della relativa tabella nel
		database. La maggior parte delle classi non la usa
	*/
	public function install () {
		/*
			dummy
		*/
	}

	protected function dupBy ( $obj ) {
		for ( $i = 0; $i < count ( $this->attributes ); $i++ ) {
			$my_attr = $this->attributes [ $i ];
			$his_attr = $obj->getAttribute ( $my_attr->name );
			$my_attr->value = $his_attr->value;
		}
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

		if ( $this->is_public == false ) {
			/*
				get_acl() ritorna un intero, ma qui lo castiamo
				per buona misura. E' stato osservato che in
				alcune condizioni (non chiare) l'oggetto in
				uscita riporta l'attributo "sharing_privileges"
				come stringa, confondendo il parser sul client
			*/
			$obj->sharing_privileges = (int) get_acl ( $this );
		}

		return $obj;
	}

	public static function exportable_array ( $array, $filter = null, $compress = false ) {
		$ret = array ();

		foreach ( $array as $obj )
			$ret [] = $obj->exportable ( $filter, $compress );

		return $ret;
	}
}

?>
