package oy.tol.chatclient;

public interface ChatClientDataProvider {
	String getServer();
	String getUsername();
	String getPassword();
	String getNick();
	String getEmail();
	String getContentTypeUsed();
	boolean useModifiedSinceHeaders();
}
