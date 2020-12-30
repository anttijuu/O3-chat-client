package oy.tol.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ChatHandler implements HttpHandler {

	private ArrayList<String> messages = new ArrayList<String>();
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		int code = 200;
		String messageBody = "";
		
		if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
			Headers headers = exchange.getRequestHeaders();
			int contentLength = 0;
			String contentType = "";
			if (headers.containsKey("Content-Length")) {
				contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
			}
			if (headers.containsKey("Content-Type")) {
				contentType = headers.get("Content-Type").get(0);
			}
			if (contentType.equalsIgnoreCase("text/plain")) {
				InputStream stream = exchange.getRequestBody();
				String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
					        .lines()
					        .collect(Collectors.joining("\n"));
				stream.close();
				if (text.length() > 0) {
					messages.add(text);
					exchange.sendResponseHeaders(code, -1);
				} else {
					code = 400;
					messageBody = "No content in request";
				}
			} else {
				code = 411;
				messageBody = "Content-Type must be text/plain.";
			}
		} else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
			for (String message : messages) {
				messageBody += message + "\n";
			}
			byte [] bytes = messageBody.getBytes("UTF-8");
			exchange.sendResponseHeaders(code, bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
		} else {
			code = 400;
			messageBody = "Not supported.";
		}
		if (code < 200 || code > 299) {
			byte [] bytes = messageBody.getBytes("UTF-8");
			exchange.sendResponseHeaders(code, bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
		}
	}
}
