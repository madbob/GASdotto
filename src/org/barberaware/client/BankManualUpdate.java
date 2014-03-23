/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

package org.barberaware.client;

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankManualUpdate extends DialogBox implements SavingDialog, ObjectWidget {
	/*
		Viene tenuto un indice diverso rispetto a quello integrato in
		BankMovement in quanto qui vengono riportate solo alcune voci,
		e si distinguono direttamente pagamenti in contanti o attraverso
		conto bancario.
		I valori effettivi vengono assegnati popolando il menu "reason",
		alcuni non sempre sono presenti (e.g. quelli per il pagamento
		delle quote di iscrizione)
	*/
	private int				USER_CREDIT_CASH	= 100;
	private int				USER_CREDIT_BANK	= 100;
	private int				GAS_BANK_TO_CASH	= 100;
	private int				GAS_CASH_TO_BANK	= 100;
	private int				GAS_BUY_BY_BANK		= 100;
	private int				GAS_BUY_BY_CASH		= 100;
	private int				GENERIC_PUT_CASH	= 100;
	private int				GENERIC_GET_CASH	= 100;
	private int				USER_ANNUAL_BANK	= 100;
	private int				USER_ANNUAL_CASH	= 100;
	private int				USER_DEPOSIT_PAY	= 100;
	private int				USER_DEPOSIT_RETURN	= 100;

	private CustomFormTable			header;
	private ListBox				reason;
	private FromServerSelector		user;
	private BankMovementForm		info;
	private ArrayList<SavingDialogCallback>	savingCallbacks;

	public BankManualUpdate () {
		int item_index;
		final VerticalPanel pan;
		DialogButtons buttons;

		this.setText ( "Gestione Movimenti" );

		pan = new VerticalPanel ();
		this.setWidget ( pan );

		pan.add ( new HTML ( "Per movimenti come il pagamento di o per un ordine o il pagamento<br />" +
					"della quota di iscrizione da parte di un utente, fare riferimento alle<br />" +
					"apposite caselle nei relativi pannelli.<br />" ) );

		header = new CustomFormTable ();
		pan.add ( header );

		item_index = 0;

		reason = new ListBox ();
		reason.addItem ( "Versamento Credito Utente in Contanti" );
		USER_CREDIT_CASH = item_index++;
		reason.addItem ( "Versamento Credito Utente via Bonifico" );
		USER_CREDIT_BANK = item_index++;
		reason.addItem ( "Trasferimento Conto / Cassa" );
		GAS_BANK_TO_CASH = item_index++;
		reason.addItem ( "Trasferimento Cassa / Conto" );
		GAS_CASH_TO_BANK = item_index++;
		reason.addItem ( "Acquisto del GAS con Bonifico" );
		GAS_BUY_BY_BANK = item_index++;
		reason.addItem ( "Acquisto del GAS in Contanti" );
		GAS_BUY_BY_CASH = item_index++;
		reason.addItem ( "Versamento in Cassa" );
		GENERIC_PUT_CASH = item_index++;
		reason.addItem ( "Prelievo da Cassa" );
		GENERIC_GET_CASH = item_index++;

		if ( Session.getGAS ().getBool ( "payments" ) == true ) {
			reason.addItem ( "Quota Annuale da Conto" );
			USER_ANNUAL_BANK = item_index++;
			reason.addItem ( "Quota Annuale in Contanti" );
			USER_ANNUAL_CASH = item_index++;
		}

		reason.addItem ( "Cauzione Utente" );
		USER_DEPOSIT_PAY = item_index++;
		reason.addItem ( "Restituzione Cauzione" );
		USER_DEPOSIT_RETURN = item_index++;
		header.addPair ( "Tipo", reason );

		user = new FromServerSelector ( "User", true, true, false );
		header.addPair ( "Socio", user );

		reason.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				int selected;

				selected = reason.getSelectedIndex ();

				if ( selected == USER_CREDIT_CASH ||
				     selected == USER_CREDIT_BANK ||
				     selected == USER_ANNUAL_CASH ||
				     selected == USER_ANNUAL_BANK ||
				     selected == USER_DEPOSIT_PAY ||
				     selected == USER_DEPOSIT_RETURN ) {

					header.showByLabel ( "Socio", true );
				}
				else if ( selected == GAS_BANK_TO_CASH ||
				          selected == GAS_BUY_BY_BANK ||
				          selected == GAS_CASH_TO_BANK ||
				          selected == GAS_BUY_BY_CASH ||
				          selected == GENERIC_PUT_CASH ||
				          selected == GENERIC_GET_CASH) {

					header.showByLabel ( "Socio", false );
				}
			}
		}, ChangeEvent.getType () );

		info = new BankMovementForm ();
		pan.add ( info );
		info.setValue ( new BankMovement () );
		info.showMethod ( false );
		/*
			Di default il pagamento e' settato su "Versamento Utente", che si intende
			in contanti, dunque a prescindere lo imposto cosi'
		*/
		info.setDefaultMethod ( BankMovement.BY_CASH );

		info.setDefaultDate ( new Date ( System.currentTimeMillis () ) );

		buttons = new DialogButtons ();

		buttons.addCallback (
			new SavingDialogCallback () {
				public void onSave ( SavingDialog d ) {
					FromServer movement;

					movement = getValue ();
					
					if ( movement.getFloat ( "amount" ) == 0 ) {
						Utils.showNotification ( "Importo a 0 non valido" );
						return;
					}
					
					movement.save ( new ServerResponse () {
						protected void onComplete ( JSONValue response ) {
							executeCallbacks ( 0 );
						}
					} );

					hide ();
				}

				public void onCancel ( SavingDialog d ) {
					executeCallbacks ( 1 );
					hide ();
				}
			}
		);

		pan.add ( buttons );
	}

	private void executeCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		if ( selected == null )
			return;

		reason.setVisible ( false );
		user.setValue ( Utils.getServer ().getObjectFromCache ( "User", selected.getInt ( "payuser" ) ) );
		info.setValue ( selected );
	}

	public FromServer getValue () {
		int selected;
		FromServer movement;

		movement = info.getValue ();
		selected = reason.getSelectedIndex ();

		if ( selected == USER_CREDIT_CASH ) {
			movement.setInt ( "movementtype", BankMovement.USER_CREDIT );
			movement.setInt ( "method", BankMovement.BY_CASH );
			movement.setInt ( "payuser", user.getValue ().getLocalID () );
		}
		else if ( selected == USER_CREDIT_BANK ) {
			movement.setInt ( "movementtype", BankMovement.USER_CREDIT );
			movement.setInt ( "method", BankMovement.BY_BANK );
			movement.setInt ( "payuser", user.getValue ().getLocalID () );
		}
		else if ( selected == GAS_BANK_TO_CASH ) {
			movement.setInt ( "movementtype", BankMovement.INTERNAL_TRANSFER );
			movement.setInt ( "method", BankMovement.BY_BANK );
		}
		else if ( selected == GAS_CASH_TO_BANK ) {
			movement.setInt ( "movementtype", BankMovement.INTERNAL_TRANSFER );
			movement.setInt ( "method", BankMovement.BY_CASH );
		}
		else if ( selected == GAS_BUY_BY_BANK ) {
			movement.setInt ( "movementtype", BankMovement.GAS_BUYING );
			movement.setInt ( "method", BankMovement.BY_BANK );
		}
		else if ( selected == GAS_BUY_BY_CASH ) {
			movement.setInt ( "movementtype", BankMovement.GAS_BUYING );
			movement.setInt ( "method", BankMovement.BY_CASH );
		}
		else if ( selected == GENERIC_PUT_CASH ) {
			movement.setInt ( "movementtype", BankMovement.GENERIC_PUT );
			movement.setInt ( "method", BankMovement.BY_CASH );
		}
		else if ( selected == GENERIC_GET_CASH ) {
			movement.setInt ( "movementtype", BankMovement.GENERIC_GET );
			movement.setInt ( "method", BankMovement.BY_CASH );
		}
		else if ( selected == USER_ANNUAL_BANK ) {
			movement.setInt ( "movementtype", BankMovement.ANNUAL_PAYMENT );
			movement.setInt ( "method", BankMovement.BY_BANK );
			movement.setInt ( "payuser", user.getValue ().getLocalID () );
		}
		else if ( selected == USER_ANNUAL_CASH ) {
			movement.setInt ( "movementtype", BankMovement.ANNUAL_PAYMENT );
			movement.setInt ( "method", BankMovement.BY_CASH );
			movement.setInt ( "payuser", user.getValue ().getLocalID () );
		}
		else if ( selected == USER_DEPOSIT_PAY ) {
			movement.setInt ( "movementtype", BankMovement.DEPOSIT_PAYMENT );
			movement.setInt ( "method", BankMovement.BY_CASH );
			movement.setInt ( "payuser", user.getValue ().getLocalID () );
		}
		else if ( selected == USER_DEPOSIT_RETURN ) {
			movement.setInt ( "movementtype", BankMovement.DEPOSIT_RETURN );
			movement.setInt ( "method", BankMovement.BY_CASH );
			movement.setInt ( "payuser", user.getValue ().getLocalID () );
		}

		if ( movement.getString ( "notes" ) == "" )
			movement.setString ( "notes", reason.getItemText ( reason.getSelectedIndex () ) );

		return movement;
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			savingCallbacks = new ArrayList<SavingDialogCallback> ();
		savingCallbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			return;
		savingCallbacks.remove ( callback );
	}
}

