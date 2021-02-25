package oy.tol.chatclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Tests using registration and chatting with HttpsUrlConnection")
class RawHttpsTests {
    // Different paths (contexts) the server supports and this client implements.
    private static final String REGISTRATION = "registration";
    private static final String CHAT = "chat";
    private static final int CONNECT_TIMEOUT = 10 * 1000;
    private static final int REQUEST_TIMEOUT = 30 * 1000;

    @Test
    @BeforeAll
    @DisplayName("Setting up the test environment")
    static void initialize() {
        assertTrue(ChatUnitTestSettings.readSettingsXML(), () -> "Could not initialize the tests. Check your test setting XML file");
        System.out.println("Initializing RawHttpsTests");
    }

    @Test
    @AfterAll
    public static void teardown() {
        System.out.println("Finished RawHttpsTests.");
    }
    
    @Test
    @DisplayName("Testing registration with empty strings")
    void testEmptyRegistrationStrings() {
        try {
            System.out.println("Testing registration with empty strings");
            int status = registerUser("", "", "");
            assertTrue(status >= 400, () -> "Server should return error 4xx.");
        } catch (KeyManagementException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (KeyStoreException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (CertificateException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (NoSuchAlgorithmException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (IOException e) {
            fail("IOException because " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Testing registration with whitespaces in registration strings")
    void testWhitespaceRegistrationStrings() {
        try {
            System.out.println("Testing registration with whitespaces in registration strings");
            int status = registerUser("  ", "  ", "  ");
            assertTrue(status >= 400, () -> "Server should return error 4xx.");
        } catch (KeyManagementException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (KeyStoreException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (CertificateException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (NoSuchAlgorithmException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (IOException e) {
            fail("IOException because " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Testing registration with invalid registration strings")
    void testInvalidRegistrationStrings() {
        try {
            System.out.println("Testing registration with invalid registration strings");
            String invalid = "";
            int status = registerWithInvalidContent(invalid);
            assertTrue(status >= 400, () -> "Server should return error 4xx.");
            if (ChatUnitTestSettings.serverVersion >= 3) {
                invalid =  "{ \"diiipa\" : \"daapa\" }";
                status = registerWithInvalidContent(invalid);
                assertTrue(status >= 400, () -> "Server should return error 4xx.");
                invalid =  "{ \"diiipa : \"daapa\" }";
                status = registerWithInvalidContent(invalid);
                assertTrue(status >= 400, () -> "Server should return error 4xx.");
                }
            invalid =  "siskonmakkarakeitto";
            status = registerWithInvalidContent(invalid);
            assertTrue(status >= 400, () -> "Server should return error 4xx.");
        } catch (KeyManagementException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (KeyStoreException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (CertificateException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (NoSuchAlgorithmException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (IOException e) {
            fail("IOException because " + e.getMessage());
        }
    }

    @Test 
    @DisplayName("Send invalid chat JSON")
    void testInvalidJSONChatMessages() {
        if (ChatUnitTestSettings.serverVersion < 3) {
            return;
        }
        System.out.println("Send invalid chat JSON");
        try {
            String invalid = "";
            int status = postInvalidChatJSONMessage(invalid, "application/json");
            assertTrue(status >= 400, () -> "Server should return error 4xx with empty JSON.");
            invalid =  "{ \"diiipa\" : \"daapa\" }";
            status = postInvalidChatJSONMessage(invalid, "application/json");
            assertTrue(status >= 400, () -> "Server should return error 4xx with valid JSON not having required elements.");
            invalid =  "{ \"diiipa : \"daapa\" }";
            status = postInvalidChatJSONMessage(invalid, "application/json");
            assertTrue(status >= 400, () -> "Server should return error 4xx with invalid JSON.");
            invalid =  "siskonmakkarakeitto";
            status = postInvalidChatJSONMessage(invalid, "application/json");
            assertTrue(status >= 400, () -> "Server should return error 4xx with string with no valid JSON.");
        } catch (KeyManagementException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (KeyStoreException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (CertificateException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (NoSuchAlgorithmException e) {
            fail("Test environment fault; check server's client side certicate is available.");
        } catch (IOException e) {
            fail("IOException because " + e.getMessage());
        }
    }

    // Supportive methods used in tests above.

    private int postInvalidChatJSONMessage(String invalidJSON, String contentType)  throws KeyManagementException,
    KeyStoreException, CertificateException, NoSuchAlgorithmException, FileNotFoundException, IOException {
        String addr = ChatUnitTestSettings.dataProvider.getServer();
        ;
		addr += CHAT;
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes = invalidJSON.getBytes(StandardCharsets.UTF_8);

        connection.setRequestProperty("Content-Type", contentType);

		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));
        String auth = ChatUnitTestSettings.existingUser + ":" + ChatUnitTestSettings.existingPassword;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                }
                in.close();    
            } catch (Exception e) {
                // We do not care if reading response body fails, just about the responseCode.
            }
		}
        return responseCode;
    }

    private int registerWithInvalidContent(String invalidRegistrationString) throws KeyManagementException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, FileNotFoundException, IOException {
		String addr = ChatUnitTestSettings.dataProvider.getServer();
		addr += REGISTRATION;
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes  = invalidRegistrationString.getBytes(StandardCharsets.UTF_8);
		if (ChatUnitTestSettings.serverVersion >= 3) {
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
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
		if (responseCode >= 400) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                }
                in.close();    
            } catch (Exception e) {
                // We do not care if reading response body fails, just about the responseCode.
            }
		}
		return responseCode;

    }

    private int registerUser(String withUsername, String withPassword, String withEmail) throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = ChatUnitTestSettings.dataProvider.getServer();
		addr += REGISTRATION;
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes;
		if (ChatUnitTestSettings.serverVersion >= 3) {
			JSONObject registrationMsg = new JSONObject();
			registrationMsg.put("username", withUsername);
			registrationMsg.put("password", withPassword);
			registrationMsg.put("email", withEmail);
			msgBytes = registrationMsg.toString().getBytes(StandardCharsets.UTF_8);
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			String registrationMsg = withUsername + ":" + withPassword;
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
		if (responseCode >= 400) {
            try {
                String result = "";
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result += " " + inputLine;
                }
                in.close();    
            } catch (Exception e) {
                // We do not care if reading response body fails, just about the responseCode.
            }
		}
		return responseCode;
	}

    private HttpURLConnection createTrustingConnectionDebug(URL url) throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, FileNotFoundException, KeyManagementException, IOException {
        if (null != ChatUnitTestSettings.clientSideCertificate) {
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(ChatUnitTestSettings.clientSideCertificate));
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
        } else {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            return connection;
        }
    }

}
