package oy.tol.chatclient;

import java.io.Console;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ChatClient is the console based UI for the ChatServer.
 * It profides the necessary functionality for chatting.
 * The actual comms with the ChatServer happens in the 
 * ChatHttpClient class.
 */
public class ChatClient implements ChatClientDataProvider {

	private static final String SERVER = "https://localhost:8001/";
	private static final String CMD_SERVER	 = "/server";
	private static final String CMD_REGISTER = "/register";
	private static final String CMD_LOGIN = "/login";
	private static final String CMD_NICK = "/nick";
	private static final String CMD_AUTO = "/auto";
	private static final String CMD_GET = "/get";
	private static final String CMD_HELP = "/help";
	private static final String CMD_INFO = "/info";
	private static final String CMD_TEST = "/test";
	private static final String CMD_EXIT = "/exit";

	private static final int AUTO_FETCH_INTERVAL = 1000; // ms
	
	private String currentServer = SERVER; 	// URL of the server without paths.
	private String username = null;			// Registered & logged user.
	private String password = null;			// The password in clear text.
	private String email = null;			// Email address of user, needed for registering.
	private String nick = null;				// Nickname, user can change the name visible in chats.
	
	private ChatHttpClient httpClient = null;	// Client handling the requests & responses.
	
	private boolean autoFetch = false;
	private Timer autoFetchTimer = null;
	
	/**
	 * 2: Exercise 2 testing
	 * 3: Exercise 3 testing
	 * 4: Exercise 4 - only internal server, no API changes, so not needed.
	 * 5: HTTP If-Modified-Since and Modified-After support in client and server
	 */
	private static int serverVersion = 2;

	public static void main(String[] args) {
		// Run the client.
		if (args.length == 2) {
			System.out.println("Launching ChatClient with args " + args[0] + " " + args[1]);
			serverVersion = Integer.parseInt(args[0]);
			if (serverVersion < 2) {
				serverVersion = 2;
			} else if (serverVersion > 5) {
				serverVersion = 5;
			}
		} else {
			System.out.println("Usage: java -jar chat-client-jar-file 2 ../localhost.cer");
			System.out.println("Where first parameter is the server version number (exercise number),");
			System.out.println("and the 2nd parameter is the server's client certificate file with path.");
			return;
		}
		ChatClient client = new ChatClient();
		client.run(args[1]);
	}

	/**
	 * Runs the show: - Creates the http client - displays the menu - handles
	 * commands until user enters command /exit.
	 */
	public void run(String certificateFileWithPath) {
		httpClient = new ChatHttpClient(this, certificateFileWithPath);
		printCommands();
		printInfo();
		Console console = System.console();
		if (null == username) {
			System.out.println("!! Register or login to server first.");
		}
		boolean running = true;
		while (running) {
			System.out.print("O3-chat > ");
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
						System.out.println("No new messages from server.");
					}
				}
				break;
			case CMD_AUTO:
				toggleAutoFetch();
				break;
			case CMD_HELP:
				printCommands();
				break;
			case CMD_INFO:
				printInfo();
				break;
			case CMD_TEST:
				doTests();
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
		}
		System.out.println("Bye!");
	}
	
	/**
	 * Does various tests to the server:
	 * - registering
	 * - logging in without registering
	 * - getting chats (without if-modified-since, with it, logged in and not)
	 * - posting chats (logged in and not)
	 * - doing POST with empty or invalid JSON
	 * - doing these in various speeds
	 */
	private void doTests() {
		System.out.println("Not yet implemented! ");
	}

	/**
	 * Toggles autofetch on and off.
	 * When autofetch is on, client fetches new chat messages peridically.
	 * Requires that user has logged in. In case of errors, autofetch
	 * may be switched off (see calls to cancelAutoFetch).
	 */
	private void toggleAutoFetch() {
		if (null == username) {
			System.out.println("Login first to fetch messages");
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
							System.out.print("O3-chat > ");
						}
					}
				}, AUTO_FETCH_INTERVAL, AUTO_FETCH_INTERVAL);
			} catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
				System.out.println(" **** Faulty timer usage: " + e.getLocalizedMessage());
				autoFetch = false;
			}
		}
	}

	/**
	 * Handles the server address change command.
	 * When server address is changed, username and password
	 * must be given again (register and/or login).
	 */
	private void changeServer(Console console) {
		System.out.print("Enter server address > ");
		String newServer = console.readLine().trim();
		if (newServer.length() > 0) {
			System.out.print("Change server from " + currentServer + " to " + newServer + "Y/n? > ");
			String confirmation = console.readLine().trim();
			if (confirmation.length() == 0 || confirmation.equalsIgnoreCase("Y")) {
				// Need to cancel autofetch since must register/login first.
				cancelAutoFetch();
				currentServer = newServer;
				username = null;
				nick = null;
				password = null;
				System.out.println("Remember to register and/or login to the new server!");
			}
		}
		System.out.println("Server in use is " + currentServer);
	}
	
	/**
	 * Get user credentials from console (i.e. login or register).
	 * Registering a new user actually communicates with the server.
	 * When logging in, user enters the credentials (username & password),
	 * but no comms with server happens until user actually either retrieves
	 * new chat messages from the server or posts a new chat message.
	 * @param console The console for the UI
	 * @param forRegistering If true, asks all registration data, otherwise just login data.
	 */
	private void getUserCredentials(Console console, boolean forRegistering) {
		System.out.print("Enter username > ");
		String newUsername = console.readLine().trim();
		if (newUsername.length() > 0) {
			// Need to cancel autofetch since username/pw not usable anymore
			// until login has been fully done (including password).
			cancelAutoFetch();
			username = newUsername;
			nick = username;
		}
		System.out.print("Enter password > ");
		char [] newPassword = console.readPassword();
		if (newPassword.length > 0) {
			password = new String(newPassword);
		}
		if (forRegistering) {
			System.out.print("Enter email > ");
			String newEmail = console.readLine().trim();
			if (newEmail.length() > 0) {
				email = newEmail;
			}
		} else {
			getNewMessages();
		}
	}
	
	/**
	 * User wants to change the nick, so ask it.
	 * @param console
	 */
	private void getNick(Console console) {
		System.out.print("Enter nick > ");
		String newNick = console.readLine().trim();
		if (newNick.length() > 0) {
			nick = newNick;
		}
	}
	
	/**
	 * Handles the registration of the user with the server.
	 * All credentials (username, email and password) must be given.
	 * User is then registered with the server.
	 * @param console
	 */
	private void registerUser(Console console) {
		System.out.println("Give user credentials for new user for server " + currentServer);
		getUserCredentials(console, true);
		try {
			if (username == null || password == null || email == null) {
				System.out.println("Must specify all user information for registration!");
				return;
			}
			// Execute the HTTPS request to the server.
			int response = httpClient.registerUser();
			if (response >= 200 || response < 300) {
				System.out.println("Registered successfully, you may start chatting!");
			} else {
				System.out.println("Failed to register!");
				System.out.println("Error from server: " + response + " " + httpClient.getServerNotification());
			}
		} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException | FileNotFoundException e) {
			System.out.println(" **** ERROR in server certificate");
			System.out.println(e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println(" **** ERROR in user registration with server " + currentServer);
			System.out.println(e.getLocalizedMessage());
		}
	}

	/**
	 * Fetches new chat messages from the server.
	 * User must be logged in.
	 * @return The count of new messages from server.
	 */
	private int getNewMessages() {
		int count = 0;
		try {
			if (null != username) {
				int response = httpClient.getChatMessages();		
				if (response >= 200 || response < 300) {
					if (serverVersion >= 3) {
						List<ChatMessage> messages = httpClient.getNewMessages();
						if (null != messages) {
							count = messages.size();
							for (ChatMessage message : messages) {
								System.out.println(message);
							}
						}
					} else {
						List<String> messages = httpClient.getPlainStringMessages();
						if (null != messages) {
							count = messages.size();
							for (String message: messages) {
								System.out.println(message);
							}
						}
					}
				} else {
					System.out.println("Error from server: " + response + " " + httpClient.getServerNotification());
				}
			} else {
				System.out.println("Not yet registered or logged in!");
			}
		} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException | FileNotFoundException e) {
			System.out.println(" **** ERROR in server certificate");
			System.out.println(e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println(" **** ERROR in getting messages from server " + currentServer);
			System.out.println(e.getLocalizedMessage());
		}
		return count;
	}
	
	/**
	 * Sends a new chat message to the server.
	 * User must be logged in to the server.
	 * @param message The chat message to send.
	 */
	private void postMessage(String message) {
		if (null != username) {
			try {
				int response = httpClient.postChatMessage(message);
				if (response < 200 || response >= 300) {
					System.out.println("Error from server: " + response + " " + httpClient.getServerNotification());
				}
			} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException | FileNotFoundException e) {
				System.out.println(" **** ERROR in server certificate");
				System.out.println(e.getLocalizedMessage());
			} catch (IOException e) {
				System.out.println(" **** ERROR in posting message to server " + currentServer);
				System.out.println(e.getLocalizedMessage());
			}
		} else {
			System.out.println("Must register/login to server before posting messages!");
		}
	}
	
	/**
	 * Print out the available commands.
	 */
	private void printCommands() {
		System.out.println("--- O3 Chat Client Commands ---");
		System.out.println("/server -- Change the server");
		System.out.println("/register  -- Register as a new user in server");
		System.out.println("/login -- Login using already registered credentials");
		System.out.println("/nick -- Specify a nickname to use in chat server");
		System.out.println("/get -- Get new messages from server");
		System.out.println("/auto -- Toggles automatic /get in " + AUTO_FETCH_INTERVAL / 1000.0 + " sec intervals");
		System.out.println("/help -- Prints out this information");
		System.out.println("/info -- Prints out settings and user information");
		System.out.println("/test -- Tests the server by automatically using the API");
		System.out.println("/exit -- Exit the client app");
		System.out.println(" > To chat, write a message and press enter to send a message.");
	}

	/**
	 * Prints out the configuration of the client.
	 */
	private void printInfo() {
		System.out.println("Server: " + currentServer);
		System.out.println("Server version assumed: " + serverVersion);
		System.out.println("User: " + username);
		System.out.println("Nick: " + nick);
		System.out.println("Autofetch is " + (autoFetch ? "on" : "off"));
	}
	
	/* 
	 Implementation of the ChatClientDataProvider interface.
	 The ChatHttpClient calls these methods to get configuration info
	 needed in communication with the server.
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
	public int getServerVersion() {
		return serverVersion;
	}
	
}