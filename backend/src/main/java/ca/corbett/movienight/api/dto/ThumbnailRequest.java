package ca.corbett.movienight.api.dto;

/**
 * Request DTO for thumbnail creation/replacement via JSON (base64).
 * <p>
 * Used when the Content-Type is {@code application/json}. For file uploads,
 * use {@code multipart/form-data} instead.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class ThumbnailRequest {

    private String thumbnailBase64;

    public ThumbnailRequest() {
    }

    /**
     * Base64-encoded image data.
     */
    public String getThumbnailBase64() {
        return thumbnailBase64;
    }

    public void setThumbnailBase64(String thumbnailBase64) {
        this.thumbnailBase64 = thumbnailBase64;
    }
}
