package oy.tol.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.BeforeAll;

@TestMethodOrder(OrderAnnotation.class)
public class ChatHttpServerTests implements ChatClientDataProvider {

    private static ChatHttpClient httpClient = null;
    private String username = null;
    private String password = null;
    private String email = null;

    ChatHttpServerTests() {
        httpClient = new ChatHttpClient(this, "/Users/anttijuustila/workspace/O3/O3-chat-client/localhost.cer");
    }

    @Test 
    @Order(1)
    @DisplayName("Testing HTTP GET /chat without valid user credentials")
    void getWithoutCredentials() {
        assertThrows(Exception.class, () -> httpClient.getChatMessages());
    }

    @Test 
    @Order(2)
    @DisplayName("Testing HTTP GET /chat with invalid user credentials")
    void getWithInvalidCredentials() {
        username = "randomnonexistentusernamehere";
        password = "invalidpasswordtoo";
        assertThrows(Exception.class, () -> httpClient.getChatMessages());
    }

    @Test 
    @Order(3)
    @DisplayName("Testing user registration")
    void testUserRegistration() {
        username = randomString(15);
        password = randomString(15);
        email = randomString(30);
        try {
            assertEquals(200, httpClient.registerUser());
		} catch (Exception e) {
			fail("Exception in registering a user");
		}
    }

    @Order(4)
    @RepeatedTest(10)
    @DisplayName("Testing getting messages from server")
    void testGetMessages() {
        try {
            // Must be an existing user in the database.
            username = "antti";
            password = "juu";
            int result = httpClient.getChatMessages();
            assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
		} catch (Exception e) {
			fail("Exception in getting chat messages from server: " + e.getMessage());
		}
    }

    @Order(5)
    @RepeatedTest(10)
    @DisplayName("Testing posting messages to server")
    void testPostMessages() {
        try {
            // Must be an existing user in the database.
            username = "antti";
            password = "juu";
            String message = randomString(120);
            int result = httpClient.postChatMessage(message);
            assertTrue(result == 200, () -> "Must get 200 from server");
		} catch (Exception e) {
			fail("Exception in getting chat messages from server: " + e.getMessage());
		}
    }

    @Order(6)
    @Test
    @DisplayName("Testing posting and getting messages to and from server")
    void testHeavyGetPostMessages() {
        try {
            // Must be an existing user in the database.
            username = "antti";
            password = "juu";
            final int MSGS_TO_ADD = 10;
            final int LOOPS_TO_RUN = 10;
            int loop = LOOPS_TO_RUN;
            int result = httpClient.getChatMessages();
            assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
            List<ChatMessage> messages = httpClient.getNewMessages();
            while (loop >= 0) {
                for (int looper = 0; looper < MSGS_TO_ADD; looper++) {
                    String message = randomString(120);
                    result = httpClient.postChatMessage(message);
                    assertTrue(result == 200, () -> "Must get 200 from server");
                }
                result = httpClient.getChatMessages();
                assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
                messages = httpClient.getNewMessages();
                loop--;
            }
		} catch (Exception e) {
			fail("Exception in getting chat messages from server: " + e.getMessage());
		}
    }

	@Override
	public String getServer() {
		return "https://localhost:8001/";
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
		return username;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public int getServerVersion() {
		return 5;
	}

    private String randomString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
    
        String generatedString = random.ints(leftLimit, rightLimit + 1)
          .limit(length)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
    
        return generatedString;
    }

}
