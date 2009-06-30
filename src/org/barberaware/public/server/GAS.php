<?

/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		$this->addAttribute ( "description", "STRING" );
		$this->addAttribute ( "use_mail", "BOOLEAN" );
	}
}
