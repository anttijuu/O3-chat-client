# Using the ChatClient JUnit tests

The necessary components needed to run JUnit tests are already included in the `pom.xml` of the project.

If your VS Code does not have the necessary Extensions to test Java apps, see this VS Code help page:

[https://code.visualstudio.com/docs/java/java-testing](https://code.visualstudio.com/docs/java/java-testing)

For an overview of the testing using VS Code, see [this video](https://youtu.be/ZO2aJSiDRSw) (Finnish only).

## Where are the tests?

You can find the test source code files in `src/test` folder in your project.

VS Code has a Test view you can select from the left side. The button reminds of a lab liquid bottle:

![VS Code Test View](vs-code-test-view.png)

There you can see the tests. If not, use the Refresh button to refresh the view.

## How to configure the tests?

Tests are designed to work with different versions of your server. When you finish exercise 2, for example,
you can test it by changing the server version to `2` in both of the tests.

Also you need to provide the tests the same client side certificate file that you must give to the actual
ChatClient app. See instructions in [README.md](README.md) on how to do that.

So before you run the tests, change these test configurations from both of the test Java files:

```Java
    // TODO: Change these for your setup!
    // Also retrieve the server client side certificate and save it to a file
    // as instructed in the Preparing the client section (and video), and
    // Change the path of the client side certificate!
    private int serverVersion = 2;              // The exercise number you test
    private String existingUser = "antti";      // Must be a registered user in your server already
    private String existingPassword = "juu";    // Must be a valid password for the above user
    private String clientSideCertificate = "/Users/anttijuustila/workspace/O3/O3-chat-client/localhost.cer";
```

Where

* `serverVersion` is the number of the exercise you are testing. If you have just finished exercise 2, this should be 2. When 
you have finished exercise 3, change this number to 3 in *both* of the test Java files.
* `existingUser`and `existingPassword` must be a user that is already registered in your server. So use either curl or ChatClient to register a 
user in your server, and then use this username/password here.
* the `clientSideCertificate` must be set as mentioned above, and this path must be a full path to that certificate file.

## How to run the tests?

Before using the tests, you obviously need to run the server.  After launching it, you can then execute the tests.

Make sure you configured the tests (above). Then you can run them. From command line, `mvn package` builds the client
*and* executes the tests too.

In Visual Studio Code, select the Test view, then select the Run Tests button (triangle pointing right):

![Running tests](vs-code-run-tests.png) 

It executes all of the tests. If you want to run just one of the tests, hover your mouse over the test and
select the Run test (triangle pointing right).

When the tests pass, the test symbol is green "OK". When it is red, the test fails. You should then analyse the situation and
think why the test failed and what you should change in your server to fix the situation.

**Note** That if the server is already implementing things from Exercise 3, for example, but the `serverVersion` in the tests is
2, for example, then the tests may fail. So you must configure the tests correctly, telling version of the server you are testing.

## Questions?

Discuss at the course Slack workspace and show up in the exercise sessions.

## Contact info

* Antti Juustila
* INTERACT Research Unit, University of Oulu, Finland

