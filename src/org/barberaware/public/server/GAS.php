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

class GAS extends FromServer {
	public function __construct () {
		parent::__construct ( "GAS" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "is_master", "BOOLEAN" );
		$this->addAttribute ( "mail", "STRING" );
		$this->addAttribute ( "image", "STRING" );
		$this->addAttribute ( "payments", "BOOLEAN" );
		$this->addAttribute ( "payment_date", "DATE" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "use_mail", "BOOLEAN" );
		$this->addAttribute ( "mail_conf", "STRING" );
		$this->addAttribute ( "mailinglist", "STRING" );
		$this->addAttribute ( "use_rid", "BOOLEAN" );
		$this->addAttribute ( "rid_conf", "STRING" );
		$this->addAttribute ( "use_shipping", "BOOLEAN" );
	}

	public function get ( $request, $compress ) {
		$ret = parent::get ( $request, $compress );

		foreach ( $ret as $gas ) {
			if ( property_exists ( $gas, 'mail_conf' ) )
				$gas->mail_conf = self::hidePassword ( $gas->mail_conf );
		}

		return $ret;
	}

	public function save ( $obj ) {
		if ( $obj->id != -1 && property_exists ( $obj, 'mail_conf' ) && $obj->mail_conf != '' ) {
			list ( $from, $username, $password, $host, $port, $ssl ) = explode ( '::', $obj->mail_conf );

			if ( $password == '###' ) {
				$g = new GAS ();
				$g->readFromDB ( $obj->id );
				list ( $useless, $useless, $password, $useless, $useless, $useless ) = explode ( '::', $g->getAttribute ( 'mail_conf' )->value );
				$obj->mail_conf = "$from::$username::$password::$host::$port::$ssl";
			}
		}

		return parent::save ( $obj );
	}

	public static function getMasterGAS () {
		$query = "SELECT id FROM GAS WHERE is_master = true";
		$returned = query_and_check ( $query, "Impossibile recuperare GAS master" );

		$row = $returned->fetch ( PDO::FETCH_ASSOC );
		if ( $row == FALSE )
			return null;

		$ret = new GAS ();
		$ret->readFromDB ( $row [ 'id' ] );

		$attr = $ret->getAttribute ( 'mail_conf' );
		$attr->value = self::hidePassword ( $attr->value );

		return $ret;
	}

	private static function hidePassword ( $mailconf ) {
		if ( $mailconf != '' ) {
			list ( $from, $username, $password, $host, $port, $ssl ) = explode ( '::', $mailconf );
			return "$from::$username::###::$host::$port::$ssl";
		}
		else {
			return '';
		}
	}
}
