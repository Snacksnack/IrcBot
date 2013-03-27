package org.snack.irc.handler;

import org.pircbotx.hooks.events.MessageEvent;
import org.snack.irc.database.DatabaseManager;
import org.snack.irc.main.Monitor;
import org.snack.irc.model.LastfmUser;
import org.snack.irc.settings.Config;
import org.snack.irc.worker.LastfmAPI;

public class LastfmHandler implements Runnable {

	private final MessageEvent<?> event;

	public LastfmHandler(MessageEvent<?> event) {
		this.event = event;
	}

	@Override
	public void run() {
		getLastfm();
	}

	/**
	 * Parses the event for the username, makes LastfmAPI return their now
	 * playing/ last played and stores the username.
	 * 
	 * @param event
	 */
	private void getLastfm() {
		DatabaseManager db = DatabaseManager.getInstance();
		LastfmUser user = db.getLastfmUser(event.getUser().getNick());
		String username = (event.getMessage().length() == 3) ? user.getUsername() : event.getMessage().split("np ")[1];
		username = (username.equals("")) ? event.getUser().getNick() : username;

		String response;
		String data[] = LastfmAPI.getSong(username);
		if (data == null) {
			response = Config.speech.get("LA_ERR");
		} else {
			if (data[0].equals("true")) {
				response = Config.speech.get("LA_SUC_NP").replace("<username>", username).replace("<song>", data[2]).replace("<artist>", data[1]).replace("<album>", data[3]);
			} else {
				response = Config.speech.get("LA_SUC_LP").replace("<username>", username).replace("<song>", data[2]).replace("<artist>", data[1]).replace("<album>", data[3]);
			}
		}
		Monitor.print("~RESPONSE  " + response);
		event.getBot().sendMessage(event.getChannel(), response);

		if (!username.equals("")) {
			if (user.getName().equals("")) {
				Monitor.print("~RESPONSE  Put lastfmuser: " + event.getUser().getNick() + " " + username);
				db.putLastfmUser(new LastfmUser(event.getUser().getNick(), username));
			} else if (!user.getUsername().equalsIgnoreCase(username)) {
				Monitor.print("~RESPONSE  Updated lastfmuser: " + user.getName() + " " + username);
				db.updateLastfmUser(new LastfmUser(user.getName(), username));
			}
		}
	}
}
