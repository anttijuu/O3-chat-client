package oy.tol.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class HttpHeaderTests implements ChatClientDataProvider {
    // TODO: add here only tests that can be done with the final version
    // - test that when you post one chat, you get one back.
    // - And when you post two, you get two back.
    // Tests assume no one else is using the server a the same time so we can do that.

    private static ChatHttpClient httpClient = null;
    private String username = null;
    private String password = null;
    HttpHeaderTests() {
        httpClient = new ChatHttpClient(this, ChatUnitTestSettings.clientSideCertificate);
    }

    @Order(8)
    @Test
    @DisplayName("Testing message counts sent and received to/from server.")
    void testModifiedSinceHeaders() {
        if (ChatUnitTestSettings.serverVersion < 5) {
            return;
        }
        
        try {
            // Must be an existing user in the database.
            username = ChatUnitTestSettings.existingUser;
            password = ChatUnitTestSettings.existingPassword;
            final int MSGS_TO_ADD = 10;
            final int LOOPS_TO_RUN = 10;
            int loop = LOOPS_TO_RUN;
            int result = httpClient.getChatMessages();
            assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
            List<ChatMessage> messages = httpClient.getNewMessages();
            if (null != messages) {
                messages.clear();
            }
            while (loop >= 0) {
                for (int looper = 0; looper < MSGS_TO_ADD; looper++) {
                    String message = randomString(120);
                    result = httpClient.postChatMessage(message);
                    assertTrue(result == 200, () -> "Must get 200 from server");
                }
                // Wait after posting a bit and then get new messages.
                Thread.sleep(1000);
                result = httpClient.getChatMessages();
                assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
                messages = httpClient.getNewMessages();
                assertNotNull(messages, () -> "Should get new messages");
                assertEquals(MSGS_TO_ADD, messages.size(), () -> "Must get the same number of messages than were sent.");
                messages.clear();
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
		return "";
	}

	@Override
	public int getServerVersion() {
		return ChatUnitTestSettings.serverVersion;
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
