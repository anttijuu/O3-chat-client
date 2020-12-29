package oy.tol.chatserver;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {

	private Map<String,String> users = null;
	
	ChatAuthenticator() {
		super("chat");
		users = new Hashtable<String,String>();
		users.put("dummy", "passwd");
	}
	
	public boolean addUser(String username, String password) {
		if (!users.containsKey(username)) {
			users.put(username, password);
			return true;
		}
		return false;
	}

	@Override
	public boolean checkCredentials(String username, String password) {
		if (users.containsKey(username)) {
			if (users.get(username).equals(password)) {
				return true;
			}
		}
		return false;
	}
}
