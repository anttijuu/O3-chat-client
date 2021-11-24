package oy.tol.chatclient;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

/**
 * ChatClient is the console based UI for the ChatServer. It profides the
 * necessary functionality for chatting. The actual comms with the ChatServer
 * happens in the ChatHttpClient class.
 */
public class ChatClient implements ChatClientDataProvider {

	private static final String SERVER = "https://localhost:8001/";
	private static final String CMD_SERVER = "/server";
	private static final String CMD_REGISTER = "/register";
	private static final String CMD_LOGIN = "/login";
	private static final String CMD_NICK = "/nick";
	private static final String CMD_AUTO = "/auto";
	private static final String CMD_COLOR = "/color";
	private static final String CMD_GET = "/get";
	private static final String CMD_HELP = "/help";
	private static final String CMD_INFO = "/info";
	private static final String CMD_EXIT = "/exit";

	private static final int AUTO_FETCH_INTERVAL = 1000; // ms

	private String currentServer = SERVER; // URL of the server without paths.
	private String clientCertificateFile = null; // The client cert for the server.
	private String payloadFormat = null;
	private boolean useModifiedHeaders = false;
	private String username = null; // Registered & logged user.
	private String password = null; // The password in clear text.
	private String email = null; // Email address of user, needed for registering.
	private String nick = null; // Nickname, user can change the name visible in chats.

	private ChatHttpClient httpClient = null; // Client handling the requests & responses.

	private boolean autoFetch = false;
	private Timer autoFetchTimer = null;
	private boolean useColorOutput = false;

	static final Attribute colorDate = Attribute.GREEN_TEXT();
	static final Attribute colorNick = Attribute.BRIGHT_BLUE_TEXT();
	static final Attribute colorMsg = Attribute.CYAN_TEXT();
	static final Attribute colorError = Attribute.BRIGHT_RED_TEXT();
	static final Attribute colorInfo = Attribute.YELLOW_TEXT();

	/**
	 * 2: Exercise 2 testing 3: Exercise 3 testing 4: Exercise 4 - only internal
	 * server, no API changes, so not needed. 5: HTTP If-Modified-Since and
	 * Modified-After support in client and server
	 */
	public static int serverVersion = 3;

	public static void main(String[] args) {
		if (args.length == 1) {								
			try {
				System.out.println("Launching ChatClient with config file " + args[0]);
				ChatClient client = new ChatClient();
				client.run(args[0]);
			} catch (Exception e) {
				System.out.println("Failed to run the ChatClient");
				System.out.println("Reason: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println("Usage: java -jar chat-client-jar-file chatclient.properties");
			System.out.println("Where chatclient.properties is the client configuration file");
			return;
		}
	}

	/**
	 * Runs the show: - Creates the http client - displays the menu - handles
	 * commands until user enters command /exit.
	 */
	public void run(String configFile) throws IOException {
		println("Reading configuration...", colorInfo);
		readConfiguration(configFile);

		boolean useHttps = false;
		if (currentServer.contains("https")) {
			useHttps = true;
		}
		httpClient = new ChatHttpClient(this, clientCertificateFile, useHttps);
		printCommands();
		printInfo();
		Console console = System.console();
		if (null == username) {
			println("!! Register or login to server first.", colorInfo);
		}
		boolean running = true;
		while (running) {
			try {
				print("O3-chat > ", colorInfo);
				String command = console.readLine().trim();
				switch (command) {
					case CMD_SERVER:
						changeServer(console);
						break;
					case CMD_REGISTER:
						registerUser(console);
						break;
					case CMD_LOGIN:
						getUserCredentials(console, false);
						break;
					case CMD_NICK:
						getNick(console);
						break;
					case CMD_GET:
						if (!autoFetch) {
							if (getNewMessages() == 0) {
								println("No new messages from server.", colorInfo);
							}
						}
						break;
					case CMD_AUTO:
						toggleAutoFetch();
						break;
					case CMD_COLOR:
						useColorOutput = !useColorOutput;
						println("Using color in output: " + (useColorOutput ? "yes" : "no"), colorInfo);
						break;
					case CMD_HELP:
						printCommands();
						break;
					case CMD_INFO:
						printInfo();
						break;
					case CMD_EXIT:
						cancelAutoFetch();
						running = false;
						break;
					default:
						if (command.length() > 0 && !command.startsWith("/")) {
							postMessage(command);
						}
						break;
				}
			} catch (Exception e) {
				println(" *** ERROR : " + e.getMessage(),colorError);
				e.printStackTrace();
			}
		}
		println("Bye!", colorInfo);
	}

	/**
	 * Toggles autofetch on and off. When autofetch is on, client fetches new chat
	 * messages peridically. Requires that user has logged in. In case of errors,
	 * autofetch may be switched off (see calls to cancelAutoFetch).
	 */
	private void toggleAutoFetch() {
		if (null == username) {
			println("Login first to fetch messages", colorInfo);
			return;
		}
		autoFetch = !autoFetch;
		if (autoFetch) {
			autoFetch();
		} else {
			cancelAutoFetch();
		}
	}

	/**
	 * Cancels the autofetch.
	 */
	private void cancelAutoFetch() {
		if (null != autoFetchTimer) {
			autoFetchTimer.cancel();
			autoFetchTimer = null;
		}
		autoFetch = false;
	}

	/**
	 * Creates and launches the autofetch timer task.
	 */
	private void autoFetch() {
		if (autoFetch) {
			if (null == autoFetchTimer) {
				autoFetchTimer = new Timer();
			}
			try {
				autoFetchTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						// Check if autofetch was switched off.
						if (!autoFetch) {
							cancel();
						} else if (getNewMessages() > 0) {
							// Neet to print the prompt after printing messages.
							print("O3-chat > ", colorInfo);
						}
					}
				}, AUTO_FETCH_INTERVAL, AUTO_FETCH_INTERVAL);
			} catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
				println(" **** Faulty timer usage: " + e.getLocalizedMessage(), colorError);
				autoFetch = false;
			}
		}
	}

	/**
	 * Handles the server address change command. When server address is changed,
	 * username and password must be given again (register and/or login).
	 */
	private void changeServer(Console console) {
		print("Enter server address > ", colorInfo);
		String newServer = console.readLine().trim();
		if (newServer.length() > 0) {
			print("Change server from " + currentServer + " to " + newServer + "Y/n? > ", colorInfo);
			String confirmation = console.readLine().trim();
			if (confirmation.length() == 0 || confirmation.equalsIgnoreCase("Y")) {
				// Need to cancel autofetch since must register/login first.
				cancelAutoFetch();
				currentServer = newServer;
				username = null;
				nick = null;
				password = null;
				println("Remember to register and/or login to the new server!", colorInfo);
			}
		}
		println("Server in use is " + currentServer, colorInfo);
	}

	/**
	 * Get user credentials from console (i.e. login or register). Registering a new
	 * user actually communicates with the server. When logging in, user enters the
	 * credentials (username & password), but no comms with server happens until
	 * user actually either retrieves new chat messages from the server or posts a
	 * new chat message.
	 * 
	 * @param console        The console for the UI
	 * @param forRegistering If true, asks all registration data, otherwise just
	 *                       login data.
	 */
	private void getUserCredentials(Console console, boolean forRegistering) {
		print("Enter username > ", colorInfo);
		String newUsername = console.readLine().trim();
		if (newUsername.length() > 0) {
			// Need to cancel autofetch since username/pw not usable anymore
			// until login has been fully done (including password).
			cancelAutoFetch();
			username = newUsername;
			nick = username;
			email = null;
			password = null;
		} else {
			print("Continuing with existing credentials", colorInfo);
			printInfo();
			return;
		}
		print("Enter password > ", colorInfo);
		char[] newPassword = console.readPassword();
		if (null != newPassword && newPassword.length > 0) {
			password = new String(newPassword);
		} else {
			print("Canceled, /register or /login!", colorError);
			username = null;
			password = null;
			email = null;
			nick = null;
			return;
		}
		httpClient.resetLatestDataTimestamp();
		if (forRegistering) {
			print("Enter email > ", colorInfo);
			String newEmail = console.readLine().trim();
			if (null != newEmail && newEmail.length() > 0) {
				email = newEmail;
			} else {
				print("Canceled, /register or /login!", colorError);
				username = null;
				password = null;
				email = null;
				nick = null;
			}
		} else {
			if (null != username && null != password) {
				getNewMessages();
			}
		}
	}

	/**
	 * User wants to change the nick, so ask it.
	 * 
	 * @param console
	 */
	private void getNick(Console console) {
		print("Enter nick > ", colorInfo);
		String newNick = console.readLine().trim();
		if (newNick.length() > 0) {
			nick = newNick;
		}
	}

	/**
	 * Handles the registration of the user with the server. All credentials
	 * (username, email and password) must be given. User is then registered with
	 * the server.
	 * 
	 * @param console
	 */
	private void registerUser(Console console) {
		println("Give user credentials for new user for server " + currentServer, colorInfo);
		getUserCredentials(console, true);
		try {
			if (username == null || password == null || email == null) {
				println("Must specify all user information for registration!", colorError);
				return;
			}
			// Execute the HTTPS request to the server.
			int response = httpClient.registerUser();
			if (response >= 200 || response < 300) {
				println("Registered successfully, you may start chatting!", colorInfo);
			} else {
				println("Failed to register!", colorError);
				println("Error from server: " + response + " " + httpClient.getServerNotification(), colorError);
			}
		} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException
				| FileNotFoundException e) {
			println(" **** ERROR in server certificate", colorError);
			println(e.getLocalizedMessage(),colorError);
		} catch (Exception e) {
			println(" **** ERROR in user registration with server " + currentServer, colorError);
			println(e.getLocalizedMessage(),colorError);
		}
	}

	/**
	 * Fetches new chat messages from the server. User must be logged in.
	 * 
	 * @return The count of new messages from server.
	 */
	private int getNewMessages() {
		int count = 0;
		try {
			if (null != username && null != password) {
				int response = httpClient.getChatMessages();
				if (response >= 200 || response < 300) {
					if (serverVersion >= 3) {
						List<ChatMessage> messages = httpClient.getNewMessages();
						if (null != messages) {
							count = messages.size();
							for (ChatMessage message : messages) {
								print(message.sentAsString(), colorDate);
								System.out.print(" ");
								print(message.nick, colorNick);
								System.out.print(" ");
								println(message.message, colorMsg);
							}
						}
					} else {
						List<String> messages = httpClient.getPlainStringMessages();
						if (null != messages) {
							count = messages.size();
							for (String message : messages) {
								println(message, colorMsg);
							}
						}
					}
				} else {
					println(" **** Error from server: " + response + " " + httpClient.getServerNotification(), colorError);
				}
			} else {
				println("Not yet registered or logged in!", colorError);
			}
		} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException
				| FileNotFoundException e) {
			println(" **** ERROR in server certificate",colorError);
			println(e.getLocalizedMessage(), colorError);
		} catch (IOException e) {
			println(" **** ERROR in getting messages from server " + currentServer,colorError);
			println(e.getLocalizedMessage(), colorError);
		}
		return count;
	}

	/**
	 * Sends a new chat message to the server. User must be logged in to the server.
	 * 
	 * @param message The chat message to send.
	 */
	private void postMessage(String message) {
		if (null != username) {
			try {
				int response = httpClient.postChatMessage(message);
				if (response < 200 || response >= 300) {
					println("Error from server: " + response + " " + httpClient.getServerNotification(), colorError);
				}
			} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException
					| FileNotFoundException e) {
				println(" **** ERROR in server certificate",colorError);
				println(e.getLocalizedMessage(), colorError);
			} catch (IOException e) {
				println(" **** ERROR in posting message to server " + currentServer, colorError);
				println(e.getLocalizedMessage(), colorError);
			}
		} else {
			println("Must register/login to server before posting messages!", colorInfo);
		}
	}

	/**
	 * Print out the available commands.
	 */
	private void printCommands() {
		println("--- O3 Chat Client Commands ---", colorInfo);
		println("/server    -- Change the server", colorInfo);
		println("/register  -- Register as a new user in server", colorInfo);
		println("/login     -- Login using already registered credentials", colorInfo);
		println("/nick      -- Specify a nickname to use in chat server", colorInfo);
		println("/get       -- Get new messages from server", colorInfo);
		println("/auto      -- Toggles automatic /get in " + AUTO_FETCH_INTERVAL / 1000.0 + " sec intervals", colorInfo);
		println("/color     -- Toggles color output on/off", colorInfo);
		println("/help      -- Prints out this information", colorInfo);
		println("/info      -- Prints out settings and user information", colorInfo);
		println("/exit      -- Exit the client app", colorInfo);
		println(" > To chat, write a message and press enter to send a message.", colorInfo);
	}

	/**
	 * Prints out the configuration of the client.
	 */
	private void printInfo() {
		println("Server: " + currentServer, colorInfo);
		println("Content type used: " + payloadFormat, colorInfo);
		println("User: " + username, colorInfo);
		println("Nick: " + nick, colorInfo);
		println("Autofetch is " + (autoFetch ? "on" : "off"), colorInfo);
		println("Using color in output: " + (useColorOutput ? "yes" : "no"), colorInfo);
	}

	private void print(String item, Attribute withAttribute) {
		if (useColorOutput) {
			System.out.print(Ansi.colorize(item, withAttribute));
		} else {
			System.out.print(item);
		}
	}

	private void println(String item, Attribute withAttribute) {
		if (useColorOutput) {
			System.out.println(Ansi.colorize(item, withAttribute));
		} else {
			System.out.println(item);
		}
	}

	private void readConfiguration(String configFileName) throws FileNotFoundException, IOException {
		System.out.println("Using configuration: " + configFileName);
		File configFile = new File(configFileName);
		Properties config = new Properties();
		FileInputStream istream;
		istream = new FileInputStream(configFile);
		config.load(istream);
		String protocol = config.getProperty("protocol", "http");
		String server = config.getProperty("server", "localhost:10000");
		currentServer = protocol + "://" + server;
		clientCertificateFile = config.getProperty("certfile", "");
		payloadFormat = config.getProperty("format", "text/plain");
		if (config.getProperty("modified-headers", "false").equalsIgnoreCase("true")) {
			useModifiedHeaders = true;
		}
		if (config.getProperty("usecolor", "false").equalsIgnoreCase("true")) {
			useColorOutput = true;
		}
		istream.close();
	}
	
	/*
	 * Implementation of the ChatClientDataProvider interface. The ChatHttpClient
	 * calls these methods to get configuration info needed in communication with
	 * the server.
	 */

	@Override
	public String getServer() {
		return currentServer;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getNick() {
		return nick;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public String getContentTypeUsed() {
		return payloadFormat;
	}

	@Override
	public boolean useModifiedSinceHeaders() {
		return useModifiedHeaders;
	}


}
