package oy.tol.chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ChatHandler implements HttpHandler {

	private ArrayList messages;
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		
		if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
			InputStream in = exchange.getRequestBody();
			Headers headers = exchange.getRequestHeaders();
			// Check the content-length from header
			// allocate room for content bytes
			// Create a string from bytes
			// Add to messages.
		} else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
			// Create a string from all messages (or latest 100)
			// Calculate length from the string
			// write to os			
		}
		// TODO: Handle errors in post/get, and if request is not supported.
		// TODO: Write simple client which can get and post.
		String response = "Hello Chat World!";
		exchange.sendResponseHeaders(200, response.length());
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

}
