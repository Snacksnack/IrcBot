package org.snack.irc.handler.message;

import org.pircbotx.hooks.events.MessageEvent;
import org.snack.irc.main.Monitor;
import org.snack.irc.main.TriggerHandler;
import org.snack.irc.model.Chan;
import org.snack.irc.settings.Config;

public class Whisper extends TriggerHandler {

	private MessageEvent<?> event;

	public Whisper() {};

	public Whisper(MessageEvent<?> event) {
		this.event = event;
	}

	@Override
	public void run() {
		whisper();
	}

	private void whisper() {
		String msg = event.getMessage();
		String sub = msg.substring(msg.indexOf(" ") + 1);
		Monitor.print("~INFO Response: whispered.");
		event.getBot().sendNotice(msg.split(" ")[1], sub.substring(sub.indexOf(" ") + 1));
	}

	@Override
	public boolean trigger(MessageEvent<?> event) {
		return (Config.sett_str.get("IDENTIFIERS").contains(event.getMessage().substring(0, 1)) && event.getMessage().length() > 10 && event.getMessage().substring(1, 9).equals("whisper ") && event.getMessage().split(" ").length > 3);
	}

	@Override
	public boolean permission(Chan chan) {
		if (chan.functions.containsKey("whisper")) {
			return chan.functions.get("whisper");
		} else {
			return true;
		}
	}

	@Override
	public void attachEvent(MessageEvent<?> event) {
		this.event = event;
	}
}