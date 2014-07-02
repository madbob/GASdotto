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

		Sarebbe comunque opportuno identificare una struttura dati piu'
		sana per tener conto del rapporto tra gli indici sequenziali e
		la loro rappresentazione tipo + metodo
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
	private int				ROUNDING_DISCOUNT	= 100;

	private CustomFormTable			header;
	private ListBox				reason;
	private int				originalReason;
	private FromServerSelector		user;
	private FromServerSelector		supplier;
	private BankMovementForm		info;
	private ArrayList<SavingDialogCallback>	savingCallbacks;

	public BankManualUpdate () {
		int item_index;
		final VerticalPanel pan;
		DialogButtons buttons;

		originalReason = -1;
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
		reason.addItem ( "Arrotondamento/Sconto Fornitore" );
		ROUNDING_DISCOUNT = item_index++;

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

		supplier = new FromServerSelector ( "Supplier", true, true, false );
		header.addPair ( "Fornitore", supplier );
		header.showByLabel ( "Fornitore", false );

		reason.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				int selected;

				selected = reason.getSelectedIndex ();
				testSelectableHeaders ( reasonToType ( selected ) );
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

	private int reasonToType ( int selected ) {
		if ( selected == USER_CREDIT_CASH )
			return BankMovement.USER_CREDIT;
		else if ( selected == USER_CREDIT_BANK )
			return BankMovement.USER_CREDIT;
		else if ( selected == GAS_BANK_TO_CASH )
			return BankMovement.INTERNAL_TRANSFER;
		else if ( selected == GAS_CASH_TO_BANK )
			return BankMovement.INTERNAL_TRANSFER;
		else if ( selected == GAS_BUY_BY_BANK )
			return BankMovement.GAS_BUYING;
		else if ( selected == GAS_BUY_BY_CASH )
			return BankMovement.GAS_BUYING;
		else if ( selected == GENERIC_PUT_CASH )
			return BankMovement.GENERIC_PUT;
		else if ( selected == GENERIC_GET_CASH )
			return BankMovement.GENERIC_GET;
		else if ( selected == USER_ANNUAL_BANK )
			return BankMovement.ANNUAL_PAYMENT;
		else if ( selected == USER_ANNUAL_CASH )
			return BankMovement.ANNUAL_PAYMENT;
		else if ( selected == USER_DEPOSIT_PAY )
			return BankMovement.DEPOSIT_PAYMENT;
		else if ( selected == USER_DEPOSIT_RETURN )
			return BankMovement.DEPOSIT_RETURN;
		else if ( selected == ROUNDING_DISCOUNT )
			return BankMovement.ROUND_SUPPLIER;
		else
			return -1;
	}

	private void testSelectableHeaders ( int selected ) {
		if ( selected == BankMovement.USER_CREDIT ||
		     selected == BankMovement.ANNUAL_PAYMENT ||
		     selected == BankMovement.DEPOSIT_PAYMENT ) {

			header.showByLabel ( "Socio", true );
		}
		else {
			header.showByLabel ( "Socio", false );
		}

		if ( selected == BankMovement.ROUND_SUPPLIER )
			header.showByLabel ( "Fornitore", true );
		else
			header.showByLabel ( "Fornitore", false );
	}

	private void executeCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		int type;
		int method;

		if ( selected == null ) {
			reason.setVisible ( true );
			reason.setEnabled ( true );
			originalReason = -1;
			return;
		}

		type = selected.getInt ( "movementtype" );
		method = selected.getInt ( "method" );

		/*
			Quando si visualizza un movimento esistente, esso puo'
			essere di un tipo diverso rispetto a quelli previsti in
			modalita' di creazione. Pertanto le informazioni
			accessorie vengono salvate a parte - per evitare che
			siano sovrascritte nei vari passaggi in
			BankMovementForm - e riassegnate in fase di ripescaggio
		*/
		originalReason = type;
		header.showByLabel ( "Tipo", true );
		info.showMethod ( true );

		if ( method == BankMovement.BY_CASH ) {
			if ( type == BankMovement.USER_CREDIT )
				reason.setSelectedIndex ( USER_CREDIT_CASH );
			else if ( type == BankMovement.INTERNAL_TRANSFER )
				reason.setSelectedIndex ( GAS_BANK_TO_CASH );
			else if ( type == BankMovement.GAS_BUYING )
				reason.setSelectedIndex ( GAS_BUY_BY_CASH );
			else if ( type == BankMovement.GENERIC_PUT )
				reason.setSelectedIndex ( GENERIC_PUT_CASH );
			else if ( type == BankMovement.GENERIC_GET )
				reason.setSelectedIndex ( GENERIC_GET_CASH );
			else if ( type == BankMovement.ANNUAL_PAYMENT )
				reason.setSelectedIndex ( USER_ANNUAL_CASH );
			else if ( type == BankMovement.DEPOSIT_PAYMENT )
				reason.setSelectedIndex ( USER_DEPOSIT_PAY );
			else if ( type == BankMovement.DEPOSIT_RETURN )
				reason.setSelectedIndex ( USER_DEPOSIT_RETURN );
			else
				header.showByLabel ( "Tipo", false );
		}
		else if ( method == BankMovement.BY_BANK ) {
			if ( type == BankMovement.USER_CREDIT )
				reason.setSelectedIndex ( USER_CREDIT_BANK );
			else if ( type == BankMovement.INTERNAL_TRANSFER )
				reason.setSelectedIndex ( GAS_CASH_TO_BANK );
			else if ( type == BankMovement.GAS_BUYING )
				reason.setSelectedIndex ( GAS_BUY_BY_BANK );
			else if ( type == BankMovement.ANNUAL_PAYMENT )
				reason.setSelectedIndex ( USER_ANNUAL_BANK );
			else if ( type == BankMovement.ROUND_SUPPLIER )
				reason.setSelectedIndex ( ROUNDING_DISCOUNT );
			else
				header.showByLabel ( "Tipo", false );
		}

		info.setDefaultMethod ( method );
		reason.setEnabled ( false );
		testSelectableHeaders ( type );

		user.setValue ( Utils.getServer ().getObjectFromCache ( "User", selected.getInt ( "payuser" ) ) );
		info.setValue ( selected );
	}

	public FromServer getValue () {
		int selected;
		int method;
		FromServer movement;

		movement = info.getValue ();
		method = movement.getInt ( "method" );

		if ( originalReason == -1 ) {
			selected = reason.getSelectedIndex ();
			movement.setInt ( "movementtype", reasonToType ( selected ) );

			if ( selected == USER_CREDIT_CASH )
				movement.setInt ( "payuser", user.getValue ().getLocalID () );
			else if ( selected == USER_CREDIT_BANK )
				movement.setInt ( "payuser", user.getValue ().getLocalID () );
			else if ( selected == USER_ANNUAL_BANK )
				movement.setInt ( "payuser", user.getValue ().getLocalID () );
			else if ( selected == USER_ANNUAL_CASH )
				movement.setInt ( "payuser", user.getValue ().getLocalID () );
			else if ( selected == USER_DEPOSIT_PAY )
				movement.setInt ( "payuser", user.getValue ().getLocalID () );
			else if ( selected == USER_DEPOSIT_RETURN )
				movement.setInt ( "payuser", user.getValue ().getLocalID () );
			else if ( selected == ROUNDING_DISCOUNT )
				movement.setInt ( "paysupplier", supplier.getValue ().getLocalID () );
		}
		else {
			movement.setInt ( "movementtype", originalReason );
		}

		movement.setInt ( "method", method );

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

