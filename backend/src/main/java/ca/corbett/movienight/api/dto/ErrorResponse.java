package ca.corbett.movienight.api.dto;

/**
 * Standard error response shape for the API.
 * <p>
 * All error responses follow this envelope:
 * <pre>
 * {
 *   "error": "Bad Request",
 *   "message": "group.title cannot be blank",
 *   "status": 400
 * }
 * </pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class ErrorResponse {

    private final String error;
    private final String message;
    private final int status;

    public ErrorResponse(String error, String message, int status) {
        this.error = error;
        this.message = message;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
