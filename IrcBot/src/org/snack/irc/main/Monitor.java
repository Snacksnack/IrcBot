package org.snack.irc.main;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.snack.irc.enums.PromptType;
import org.snack.irc.settings.Config;

public class Monitor {
	private static boolean showInterface;

	private static JFrame monitorFrame;
	private static JPanel monitorPanel;
	private static JScrollPane monitorScrollPane;
	private static JTextArea monitorTextArea;
	private static JMenuBar menuBar;
	private static JMenu mainMenu;
	private static JMenuItem menuSend, menuBroadcast, menuClear, menuQuit;

	private static Monitor m;
	private static Logger logger;

	public static Monitor getInstance() {
		if (m == null) {
			m = new Monitor(Config.sett_bool.get("INTERFACE"));
		}
		return m;
	}

	private Monitor(boolean showInterface) {
		Monitor.showInterface = showInterface;
		if (showInterface) {
			redirectSystemStreams();
			createAndShowGUI();
		}
		try {
			logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
			logger.setLevel(Level.INFO);
			FileHandler handler = new FileHandler(Config.sett_str.get("LOG_LOC"));
			handler.setFormatter(new SimpleFormatter());
			logger.addHandler(handler);
		} catch (Exception e) {
			Monitor.print("~ERROR Couldn't create log file");
		}
		Monitor.print("~INFO Initialized interface");
	}

	/**
	 * A frame that shows all messages the server returns
	 */
	public static void createAndShowGUI() {
		// make the interface parts
		monitorFrame = new JFrame();
		monitorPanel = new JPanel();
		monitorScrollPane = new JScrollPane();
		monitorTextArea = new JTextArea();
		// set up the menu
		menuBar = new JMenuBar();

		// the main menu
		mainMenu = new JMenu("Main");
		mainMenu.setMnemonic(KeyEvent.VK_M);
		mainMenu.getAccessibleContext().setAccessibleDescription("Main menu.");
		menuBar.add(mainMenu);

		// load menu item
		menuSend = new JMenuItem("Send message", KeyEvent.VK_S);
		menuSend.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		menuSend.getAccessibleContext().setAccessibleDescription("Send a message");
		menuSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Prompt(getFrame(), PromptType.MESSAGE);
			}
		});
		menuClear = new JMenuItem("Clear monitor", KeyEvent.VK_C);
		menuClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menuClear.getAccessibleContext().setAccessibleDescription("Clear the monitor");
		menuClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				monitorTextArea.removeAll();
			}
		});
		menuBroadcast = new JMenuItem("Send broadcast", KeyEvent.VK_B);
		menuBroadcast.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
		menuBroadcast.getAccessibleContext().setAccessibleDescription("Send a broadcast");
		menuBroadcast.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Prompt(getFrame(), PromptType.BROADCAST);
			}
		});
		menuQuit = new JMenuItem("Shutdown", KeyEvent.VK_Q);
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		menuQuit.getAccessibleContext().setAccessibleDescription("Shut the bot down");
		menuQuit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		mainMenu.add(menuSend);
		mainMenu.add(menuBroadcast);
		mainMenu.add(menuClear);
		mainMenu.add(menuQuit);
		monitorFrame.setJMenuBar(menuBar);
		// set up the frame
		monitorFrame.setTitle("Bot Monitor");
		monitorFrame.setSize(700, 800);
		monitorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		monitorFrame.setResizable(true);
		monitorFrame.setLocationRelativeTo(null);
		monitorFrame.getContentPane().setLayout(null);
		// set up the text area
		monitorTextArea.setToolTipText("");
		monitorTextArea.setEditable(false);
		monitorTextArea.getDocument().addDocumentListener(new MonitorLimiter(50));
		// set up the frame to hold the text area
		monitorScrollPane.getViewport().add(monitorTextArea);
		monitorScrollPane.setPreferredSize(new Dimension(monitorFrame.getWidth() - 25, monitorFrame.getHeight() - 60));
		monitorPanel.add(monitorScrollPane);
		monitorPanel.setBounds(-5, 0, monitorFrame.getWidth(), monitorFrame.getHeight());
		monitorFrame.getContentPane().add(monitorPanel);
		// show frame
		monitorFrame.setVisible(true);
	}

	/**
	 * Returns the frame
	 * 
	 * @return The frame
	 */
	private static JFrame getFrame() {
		return monitorFrame;
	}

	/**
	 * Prints a message to the monitor
	 * 
	 * @param message
	 *            message to print
	 */
	public static void print(String message) {
		if (showInterface) {
			if (monitorTextArea.getLineCount() > Config.sett_int.get("SCROLLBACK")) {
				monitorTextArea.remove(0);
			}
			monitorTextArea.append(message + "\n");
			// scroll down
			JScrollBar sb = monitorScrollPane.getVerticalScrollBar();
			while (!(sb.getMaximum() == (sb.getValue() + sb.getVisibleAmount()))) {
				sb.setValue(sb.getMaximum());
			}
		}
		Monitor.logger.info(message);
	}

	/**
	 * Redirects system.out/err to the monitor
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				print(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				print(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}

}
