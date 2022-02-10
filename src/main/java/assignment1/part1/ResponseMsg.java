package assignment1.part1;

/**
 * Class represents a response message.
 */
public class ResponseMsg {
    protected String message;

    public ResponseMsg(String message) {
        this.message = message;
    }

    public ResponseMsg() {
    }

    /**
     * Getters and Setters
     * @return
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ResponseMsg{" +
                "message='" + message + '\'' +
                '}';
    }
}
