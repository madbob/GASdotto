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

class User extends FromServer {
	public function __construct () {
		parent::__construct ( "User", "users" );

		$this->addAttribute ( "login", "STRING" );
		$this->addAttribute ( "firstname", "STRING" );
		$this->addAttribute ( "surname", "STRING" );
		$this->addAttribute ( "join_date", "DATE" );
		$this->addAttribute ( "phone", "STRING" );
		$this->addAttribute ( "mobile", "STRING" );
		$this->addAttribute ( "mail", "STRING" );
		$this->addAttribute ( "address", "ADDRESS" );
		$this->addAttribute ( "paying", "BOOLEAN", "false" );
		$this->addAttribute ( "privileges", "INTEGER", "1" );
	}
}

?>
