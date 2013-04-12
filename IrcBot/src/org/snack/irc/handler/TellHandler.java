package org.snack.irc.handler;

import java.util.ArrayList;

import org.pircbotx.User;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.snack.irc.database.DatabaseManager;
import org.snack.irc.enums.TellType;
import org.snack.irc.main.Monitor;
import org.snack.irc.model.Tell;
import org.snack.irc.settings.Config;

public class TellHandler implements Runnable {

	private final MessageEvent<?> mEvent;
	private final JoinEvent<?> jEvent;
	private final NickChangeEvent<?> nEvent;
	private final TellType tellType;

	public TellHandler(MessageEvent<?> mEvent, JoinEvent<?> jEvent, NickChangeEvent<?> nEvent, TellType tellType) {
		this.mEvent = mEvent;
		this.jEvent = jEvent;
		this.nEvent = nEvent;
		this.tellType = tellType;
	}

	@Override
	public void run() {
		if (tellType == TellType.ADD) {
			add();
		} else {
			if (jEvent == null) {
				if (nEvent == null) {
					tell(null, null, mEvent);
				} else {
					tell(null, nEvent, null);
				}
			} else {
				tell(jEvent, null, null);
			}
		}
	}

	/**
	 * Adds a new tell to the database
	 */
	private void add() {
		DatabaseManager db = DatabaseManager.getInstance();
		String nick = mEvent.getMessage().split(" ")[1];

		boolean online = false;

		for (User u : mEvent.getChannel().getUsers()) {
			if (u.getNick().equalsIgnoreCase(nick) && (System.currentTimeMillis() - db.getLastMsg(nick).getTime()) < (10 * 60 * 1000)) {
				online = true;
			}
		}

		if (!online) {
			Tell tell = new Tell(mEvent.getUser().getNick(), nick, mEvent.getMessage().split("tell " + nick)[1]);
			db.putTell(tell);
			Monitor.print("~RESPONSE  Added tell: " + tell.getSender() + " > " + tell.getName() + " : " + tell.getMessage());
			mEvent.getBot().sendMessage(mEvent.getChannel(), Config.speech.get("TE_ADD").replace("<name>", nick));
		} else {
			Monitor.print("~RESPONSE  Error adding tell.");
			mEvent.getBot().sendMessage(mEvent.getChannel(), Config.speech.get("TE_ERR").replace("<name>", nick));
		}
	}

	/**
	 * Tells all the tells it has to tell.
	 */
	private void tell(JoinEvent<?> j, NickChangeEvent<?> n, MessageEvent<?> m) {
		DatabaseManager db = DatabaseManager.getInstance();

		if (j == null) {
			if (n == null) {
				ArrayList<Tell> tells = db.getTells(mEvent.getUser().getNick());
				for (Tell tell : tells) {
					if (tell.getName().equalsIgnoreCase(nEvent.getUser().getNick())) {
						String response = Config.speech.get("TE_TEL").replace("<sender>", tell.getSender()).replace("<message>", tell.getMessage());
						Monitor.print("~RESPONSE  Told: " + response);
						mEvent.getBot().sendNotice(mEvent.getUser(), response);
						db.removeTell(tell);
					}
				}
			} else {
				ArrayList<Tell> tells = db.getTells(nEvent.getUser().getNick());
				for (Tell tell : tells) {
					if (tell.getName().equalsIgnoreCase(nEvent.getUser().getNick())) {
						String response = Config.speech.get("TE_TEL").replace("<sender>", tell.getSender()).replace("<message>", tell.getMessage());
						Monitor.print("~RESPONSE  Told: " + response);
						nEvent.getBot().sendNotice(nEvent.getUser(), response);
						db.removeTell(tell);
					}
				}
			}
		} else {
			ArrayList<Tell> tells = db.getTells(jEvent.getUser().getNick());
			for (Tell tell : tells) {
				if (tell.getName().equalsIgnoreCase(jEvent.getUser().getNick())) {
					String response = Config.speech.get("TE_TEL").replace("<sender>", tell.getSender()).replace("<message>", tell.getMessage());
					Monitor.print("~RESPONSE  Told: " + response);
					jEvent.getBot().sendNotice(jEvent.getUser(), response);
					db.removeTell(tell);
				}
			}
		}
	}
}
