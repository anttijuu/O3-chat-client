package oy.tol.chatclient;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ChatUnitTestSettings {

    // NOTE ALERT!! Do NOT change these settings by hand anymore.
    // Tests are configured from the XML test config files. See TESTS-README.md for
    // instructions!
    public static String clientSideCertificate = null;
    public static int serverVersion = 3;
    // NOTE: Test users are read from the test config XML files.
    public static String existingUser = null;
    public static String existingPassword = null;
    public static String existingUser2 = null;
    public static String existingPassword2 = null;

    private static ChatHttpClient httpClient = null;
    public static TestDataProvider dataProvider = new TestDataProvider();

    // TODO: Keep the server URL only here in one place
    // TODO: if no cert/-http config, remove s from https
    public static boolean readSettingsXML() {
        try {
            String fileName = System.getProperty("testsettings");
            if (fileName == null) {
                fileName = "test-config-1.xml";
            }
            System.out.println("Test setting file: " + fileName);
            File fXmlFile = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            if (!doc.getDocumentElement().getNodeName().equals("testconfig")) {
                System.out.println("*** ERROR: test user file invalid");
                System.exit(-1);
            }

            NodeList node = doc.getElementsByTagName("servercertificate");
            if (node != null && node.getLength() > 0) {
                clientSideCertificate = doc.getElementsByTagName("servercertificate").item(0).getTextContent();
                if (null != clientSideCertificate && clientSideCertificate.trim().length() == 0) {
                    clientSideCertificate = null;
                }
            }
            String tmpNum = doc.getElementsByTagName("serverversion").item(0).getTextContent();
            serverVersion = Integer.parseInt(tmpNum);

            httpClient = new ChatHttpClient(dataProvider, clientSideCertificate);

            NodeList usersList = doc.getElementsByTagName("user");
            for (int temp = 0; temp < usersList.getLength(); temp++) {
                Node user = usersList.item(temp);
                if (user.getNodeType() == Node.ELEMENT_NODE) {
                    Element userElement = (Element) user;
                    if (temp == 0) {
                        existingUser = userElement.getElementsByTagName("username").item(0).getTextContent();
                        existingPassword = userElement.getElementsByTagName("password").item(0).getTextContent();
                        try {
                            httpClient.registerUser();
                        } catch (IOException e) {
                            // Here we CAN ignore the IOException error(s) since failing to register
                            // usually means user has already been registered, and that is OK.
                            // If the problem is something larger (like server not running),
                            // then those errors will be surely seen in the actual tests later.
                        }
                        existingUser2 = existingUser;
                        existingPassword2 = existingPassword;
                    } else {
                        existingUser = userElement.getElementsByTagName("username").item(0).getTextContent();
                        existingPassword = userElement.getElementsByTagName("password").item(0).getTextContent();
                        try {
                            httpClient.registerUser();
                        } catch (IOException e) {
                            // Here we CAN ignore the IOException error(s) since failing to register
                            // usually means user has already been registered, and that is OK.
                            // If the problem is something larger (like server not running),
                            // then those errors will be surely seen in the actual tests later.
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("*** ERROR: test user XML file does not exist or is invalid: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("*** ERROR: test user XML file does not exist or is invalid: " + e.getMessage());
            return false;
        }
        return (existingUser != null && 
                existingPassword != null && 
                existingUser2 != null && 
                existingPassword2 != null); 
    }

    public static class TestDataProvider implements ChatClientDataProvider {
        @Override
        public String getServer() {
            if (clientSideCertificate != null) {
                return "https://localhost:10001/";
            } else {
                return "http://localhost:10001/";
            }
        }

        @Override
        public String getUsername() {
            return ChatUnitTestSettings.existingUser;
        }

        @Override
        public String getPassword() {
            return ChatUnitTestSettings.existingPassword;
        }

        @Override
        public String getNick() {
            return ChatUnitTestSettings.existingUser;
        }

        @Override
        public String getEmail() {
            return "no email sorry";
        }

        public int getServerVersion() {
            return ChatUnitTestSettings.serverVersion;
        }

        @Override
        public String getContentTypeUsed() {
            if (getServerVersion() < 3) {
                return "text/plain";
            }
            return "application/json";
        }

        @Override
        public boolean useModifiedSinceHeaders() {
            if (getServerVersion() >= 5) {
                return true;
            }
            return false;
        }
    }

}
