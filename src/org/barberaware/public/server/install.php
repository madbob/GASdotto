<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<title>Installazione GASdotto</title>
	</head>

	<body>
	</body>

<?

if ( isset ( $_GET [ 'step' ] ) )
	$action = $_GET [ 'step' ];
else
	$action = "first";

?>

<h2>GASdotto: Installer</h2>

<?php

switch ( $action ) {
	case "first":
		?>

		<div style="width: 60%; margin: auto;">
			<?php

			if ( posix_access ( "./config.php", POSIX_W_OK ) == FALSE && @chmod ( "./config.php", 0770 ) == FALSE ) {
				?>

				<div style="width: 100%; border: 5px solid #FF0000; text-align: center;">ATTENZIONE!</div>

				<p>
				Il file <?php echo ( dirname ( __FILE__ ) . '/config.php' ); ?> non è scrivibile, dunque non è possibile salvare le impostazioni!
				</p>

				<p>
				Purtroppo non è possibile modificare automoticamente questa condizione, devi manualmente settare tale file in modo che sia scrivibile da questa applicazione.
				</p>

				<?

			}
			else {

				?>

				<p>
					E così, vuoi installare <a href="http://gasdotto.barberaware.org">GASdotto</a> sul tuo server? Basta poco!
				</p>

				<form method="GET" action="<?php $_SERVER [ 'PHP_SELF' ] . "?step=install" ?>">
				<table>
					<?php

					$drivers = PDO::getAvailableDrivers ();

					if ( count ( $drivers ) > 1 ) {
						$dbs = array ();

						foreach ( $drivers as $d ) {
							if ( $id == "mysql" )
								$dbs [] = array ( $id, "MySQL" );
							else if ( $id == "pgsql" )
								$dbs [] = array ( $id, "PostGreSQL" );
						}

						if ( count ( $dbs ) > 1 ) {
							?>

							<tr><td>Database da utilizzare:</td><td><select name="dbdriver">

							<?

							foreach ( $dbs as $d )
								echo "<option value=\"" . ( $d [ 0 ] ) . "\">" . ( $d [ 1 ] ) . "</option>";

							?>

							</select></td></tr>

							<?php
						}
					}
					else if ( count ( $drivers ) == 1 ) {
						echo "<hidden name=\"dbdriver\" value=\"" . ( $drivers [ 0 ] ) . "\">";
					}

					/**
						TODO	Che fare se non si trova alcun driver PDO???
					*/

					?>
					<tr>
						<td>Nome utente database:</td><td><input type="text" name="dbuser" /></td>
					</tr>
					<tr>
						<td>Password per il database:</td><td><input type="password" name="dbpassword" /></td>
					</tr>
					<tr style="height: 30px;"><td>&nbsp;</td></tr>
					<tr>
						<td>Password dell'amministratore di GASdotto:</td><td><input type="password" name="userpassword" /></td>
					</tr>
					<tr>
						<td>Ripeti password dell'amministratore:</td><td><input type="password" name="checkuserpassword" /></td>
					</tr>
				</table>
				</form>
			<?php
			}
			?>
		</div>

		<?
		break;

	case "install":
		require_once ( "createdb.php" );
		install_main_db ();
		break;
}

?>

</html>
