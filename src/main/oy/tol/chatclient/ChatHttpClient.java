package oy.tol.chatclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatHttpClient {

	// Different paths (contexts) the server supports and this client implements.
	private static final String CHAT = "chat";
	private static final String REGISTRATION = "registration";

	// When using JSON (excercise 3), List<ChatMessage> is used,
	// and earlier, use List<String>.
	private List<ChatMessage> newMessages = null;
	private List<String> plainStringMessages = null;

	private String serverNotification = "";

	private ChatClientDataProvider dataProvider = null;

	private static final int CONNECT_TIMEOUT = 10 * 1000;
	private static final int REQUEST_TIMEOUT = 30 * 1000;

	private static final DateTimeFormatter jsonDateFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	private String latestDataFromServerIsFrom = null;

	private String certificateFile;

	ChatHttpClient(ChatClientDataProvider provider, String certificateFileWithPath) {
		dataProvider = provider;
		certificateFile = certificateFileWithPath;
	}

	public String getServerNotification() {
		return serverNotification;
	}

	public List<ChatMessage> getNewMessages() {
		return newMessages;
	}

	public List<String> getPlainStringMessages() {
		return plainStringMessages;
	}

	public int getChatMessages() throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += CHAT;
		URL url = new URL(addr);

		HttpsURLConnection connection = createTrustingConnectionDebug(url);

		connection.setRequestMethod("GET");
		if (dataProvider.getServerVersion() >= 3) {
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			connection.setRequestProperty("Content-Type", "text/plain");
		}
		if (dataProvider.getServerVersion() >= 5 && null != latestDataFromServerIsFrom) {
			connection.setRequestProperty("If-Modified-Since", latestDataFromServerIsFrom);
		}

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		int responseCode = connection.getResponseCode();
		if (responseCode == 204) {
			newMessages = null;
		} else if (responseCode >= 200 && responseCode < 300) {
			if (dataProvider.getServerVersion() >= 5) {
				latestDataFromServerIsFrom = connection.getHeaderField("Last-Modified");
			}
			String input;
			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			if (dataProvider.getServerVersion() >= 3) {
				String totalInput = "";
				while ((input = in.readLine()) != null) {
					totalInput += input;
				}
				JSONArray jsonArray = new JSONArray(totalInput);
				if (jsonArray.length() > 0) {
					newMessages = new ArrayList<ChatMessage>();
					for (int index = 0; index < jsonArray.length(); index++) {
						JSONObject object = jsonArray.getJSONObject(index);
						ChatMessage msg = ChatMessage.from(object);
						newMessages.add(msg);
					}
					Collections.sort(newMessages, new Comparator<ChatMessage>() {
						@Override
						public int compare(ChatMessage lhs, ChatMessage rhs) {
							return lhs.sent.compareTo(rhs.sent);
						}
					});
				}
			} else { // Server not yet supports JSON.
				plainStringMessages = new ArrayList<String>();
				while ((input = in.readLine()) != null) {
					plainStringMessages.add(input);
				}
			}
			in.close();
			serverNotification = "";
		} else {
			newMessages = null;
			plainStringMessages = null;
			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				serverNotification += " " + inputLine;
			}
			in.close();
		}
		return responseCode;
	}

	public int postChatMessage(String message) throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += CHAT;
		URL url = new URL(addr);

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();

		HttpsURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes;
		if (dataProvider.getServerVersion() >= 3) {
			JSONObject msg = new JSONObject();
			msg.put("user", dataProvider.getNick());
			msg.put("message", message);
			ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
			String dateText = now.format(jsonDateFormatter);
			msg.put("sent", dateText);
			msgBytes = msg.toString().getBytes(StandardCharsets.UTF_8);
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			msgBytes = message.getBytes("UTF-8");
			connection.setRequestProperty("Content-Type", "text/plain");
		}
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode < 300) {
			// Successfully posted.
			serverNotification = "";
		} else {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				serverNotification += " " + inputLine;
			}
			in.close();
		}
		return responseCode;
	}

	public int registerUser() throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += REGISTRATION;
		URL url = new URL(addr);

		HttpsURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes;
		if (dataProvider.getServerVersion() >= 3) {
			JSONObject registrationMsg = new JSONObject();
			registrationMsg.put("username", dataProvider.getUsername());
			registrationMsg.put("password", dataProvider.getPassword());
			registrationMsg.put("email", dataProvider.getEmail());
			msgBytes = registrationMsg.toString().getBytes(StandardCharsets.UTF_8);
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			String registrationMsg = dataProvider.getUsername() + ":" + dataProvider.getPassword();
			msgBytes = registrationMsg.getBytes("UTF-8");
			connection.setRequestProperty("Content-Type", "text/plain");
		}

		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode < 300) {
			// Successfully registered.
			serverNotification = "";
		} else {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				serverNotification += " " + inputLine;
			}
			in.close();
		}
		return responseCode;
	}

	// For accepting self signed certificates. Not to be used in production
	// software!

	private HttpsURLConnection createTrustingConnectionDebug(URL url) throws KeyStoreException, CertificateException,
			NoSuchAlgorithmException, FileNotFoundException, KeyManagementException, IOException {
		Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(certificateFile));

		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(null, null);
		keyStore.setCertificateEntry("localhost", certificate);

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(keyStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setSSLSocketFactory(sslContext.getSocketFactory());
		// All requests use these common timeouts.
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.setReadTimeout(REQUEST_TIMEOUT);
		return connection;
	}
}
