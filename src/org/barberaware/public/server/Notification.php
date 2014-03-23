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

class Notification extends FromServer {
	public function __construct () {
		parent::__construct ( "Notification" );

		$this->addAttribute ( "name", "STRING" );
		$this->addAttribute ( "alert_type", "INTEGER" );
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "startdate", "DATE" );
		$this->addAttribute ( "enddate", "DATE" );
		$this->addAttribute ( "sender", "OBJECT::User" );
		$this->addAttribute ( "recipent", "ARRAY::User" );
		$this->addAttribute ( "send_mail", "BOOLEAN" );
		$this->addAttribute ( "send_mailinglist", "BOOLEAN" );

		$this->preserveAttribute ( "sender" );
		$this->preserveAttribute ( "recipent" );

		$this->setPublic ( false, 'desc', 'User', 'sender' );
	}

	public function get ( $request, $compress ) {
		global $current_user;

		/*
			Elimino le notifiche oramai scadute
		*/
		$query = sprintf ( "SELECT id FROM %s WHERE enddate < DATE('%s')", $this->tablename, date ( "Y-m-d" ) );
		$returned = query_and_check ( $query, "Impossibile identificare vecchie notifiche" );
		$rows = $returned->fetchAll ( PDO::FETCH_ASSOC );
		unset ( $returned );

		if ( count ( $rows ) != 0 ) {
			$tmp = new Notification ();

			foreach ( $rows as $row ) {
				$tmp->readFromDB ( $row [ 'id' ] );
				$tmp->destroy_myself ();
			}

			unset ( $tmp );
		}

		$ret = array ();

		$u = new User ();
		$references = $this->tablename . "_recipent";
		$query = sprintf ( "SELECT %s.id FROM %s, %s WHERE ", $this->tablename, $this->tablename, $references );

		if ( ( isset ( $request->has ) ) && ( count ( $request->has ) != 0 ) ) {
			$ids = join ( ',', $request->has );
			$query .= sprintf ( " %s.id NOT IN ( %s ) AND ", $this->tablename, $ids );
		}

		if ( !isset ( $request->all ) ) {
			if ( !isset ( $request->mine ) )
				$query .= sprintf ( "%s.target = %d AND ", $references, $current_user );
			else
				$query .= sprintf ( "%s.sender = %d AND ", $this->tablename, $current_user );
		}

		$query .= sprintf ( "%s.id = %s.parent ORDER BY startdate DESC", $this->tablename, $references );
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

	private function send_as_mail ( $obj ) {
		$dests = array ();

		foreach ( $obj->recipent as $destination ) {
			$destination_user = new User ();
			$destination_user->readFromDB ( $destination );

			$mail = $destination_user->getAttribute ( "mail" )->value;
			$mail2 = $destination_user->getAttribute ( "mail2" )->value;

			if ( isset ( $mail ) && $mail != '' )
				$dests [] = $mail;
			if ( isset ( $mail2 ) && $mail2 != '' )
				$dests [] = $mail2;
		}

		if ( $obj->send_mailinglist == true ) {
			$gas = current_gas ();
			$dests [] = $gas->getAttribute ( 'mailinglist' )->value;
			unset ( $gas );
		}

		my_send_mail ( $dests, $obj->name, false, $obj->description );
	}

	public function save ( $obj ) {
		global $current_user;

		if ( $obj->sender != $current_user )
			error_exit ( "Richiesta di salvataggio notifica non autorizzata" );

		$sendmail = false;
		if ( $obj->id == -1 && $obj->send_mail == true )
			$sendmail = true;

		$id = parent::save ( $obj );

		if ( $id > 0 && $sendmail == true ) {
			$m = self::send_as_mail ( $obj );
			if ( $m != null )
				error_exit ( "Notifica salvata, ma problema in invio mail: " . $m );
		}

		return $id;
	}
}

?>
