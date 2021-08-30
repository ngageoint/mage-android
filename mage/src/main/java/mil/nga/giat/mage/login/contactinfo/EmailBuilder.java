package mil.nga.giat.mage.login.contactinfo;

public class EmailBuilder {

    private final String myStatusMessage;
    private final String myIdentifier;
    private final String myStrategy;
    private final StringBuilder mySubject = new StringBuilder();
    private final StringBuilder myBody = new StringBuilder();

    public EmailBuilder(String statusMessage, String identifier, String strategy) {
        myStatusMessage = statusMessage;
        myIdentifier = identifier;
        myStrategy = strategy;
    }

    public String getSubject() {
        return mySubject.toString();
    }

    public String getBody() {
        return myBody.toString();
    }

    public void build() {

        final String upperStatusMsg = this.myStatusMessage.toUpperCase();
    }
}
