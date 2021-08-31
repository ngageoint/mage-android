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
        if (upperStatusMsg.contains("DEVICE")) {
            if (upperStatusMsg.contains("REGISTER")) {
                this.mySubject.append("Please approve my device");
            } else {
                this.mySubject.append("Device ID issue");
            }
        } else {
            if (upperStatusMsg.contains("APPROVED")) {
                this.mySubject.append("Please activate my account");
            } else if (upperStatusMsg.contains("DISABLED")) {
                this.mySubject.append("Please enable my account");
            } else if (upperStatusMsg.contains("LOCKED")) {
                this.mySubject.append("Please unlock my account");
            } else {
                this.mySubject.append("User login issue");
            }
        }

        if (this.myIdentifier != null) {
            this.mySubject.append(" - " + this.myIdentifier);
            this.myBody.append("Identifier (username or device id): " + this.myIdentifier + '\n');
        }
        if (this.myStrategy != null) {
            this.myBody.append("Authentication Method: ");
            //TODO get check how title is accessed
            //if (this.myStrategy.title != null) {
           //     this.myBody.append(this.myStrategy.title);
            //} else {
                this.myBody.append(this.myStrategy);
            //}
            this.myBody.append('\n');
        }

        this.myBody.append("Error Message Received: " + this.myStatusMessage);
    }
}
