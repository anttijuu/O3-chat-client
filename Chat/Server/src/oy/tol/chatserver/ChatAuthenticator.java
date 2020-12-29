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
	
	public void addUser(String userName, String password) {
		// TODO implement this.
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
