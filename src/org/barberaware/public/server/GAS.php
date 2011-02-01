<?php

/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

class GAS extends FromServer {
	public function __construct () {
		parent::__construct ( "GAS" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "mail", "STRING" );
		$this->addAttribute ( "image", "STRING" );
		$this->addAttribute ( "payments", "BOOLEAN" );
		$this->addAttribute ( "payment_date", "DATE" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "use_mail", "BOOLEAN" );
		$this->addAttribute ( "mail_conf", "STRING" );
		$this->addAttribute ( "mailinglist", "STRING" );
	}

	public static function get_extra_options () {
		return array (
			'show_all_users' => 'true',
			'admin_power' => 'false'
		);
	}

	public function get ( $request, $compress ) {
		$ret = parent::get ( $request, $compress );

		foreach ( $ret as $gas ) {
			$options = @file ( 'extra_options_' . $gas->id, FILE_SKIP_EMPTY_LINES );

			if ( $options == false ) {
				$options = self::get_extra_options ();

				foreach ( $options as $name => $value )
					$gas->$name = $value;
			}
			else {
				foreach ( $options as $opt ) {
					list ( $name, $value ) = explode ( '=', $opt );
					$name = trim ( $name );
					$value = trim ( $value );
					$gas->$name = $value;
				}
			}
		}

		return $ret;
	}

	public function save ( $obj ) {
		$options = self::get_extra_options ();
		$options = array_keys ( $options );
		$fields = get_object_vars ( $obj );
		$rows = array ();

		foreach ( $fields as $name => $value ) {
			if ( in_array ( $name, $options ) == true ) {
				$rows [] = $name . ' = ' . $value;
				unset ( $obj->$name );
			}
		}

		@file_put_contents ( 'extra_options_' . $obj->id, join ( "\n", $rows ) );
		return parent::save ( $obj );
	}
}
