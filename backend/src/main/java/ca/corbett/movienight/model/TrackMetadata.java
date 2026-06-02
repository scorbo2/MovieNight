package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents simple metadata for an audio or a subtitle track within a video file.
 * This is used for videos that have multiple audio tracks (for example, multiple languages),
 * so that the UI can display the available tracks and allow the user to select which one they
 * want to use when watching the video. Note that this feature only works with the "Watch in VLC"
 * option. The inline HTML 5 video player does not support multiple tracks.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TrackMetadata {

    /**
     * A unique, 0-based numeric index for this track.
     * This is used as an identifier when generating VLC playlists.
     */
    private int index;

    /**
     * Either a three-letter ISO 639-2 language code (example: "eng" for English, "spa" for Spanish),
     * or a two-letter ISO 639-1 language code (example: "en" for English, "es" for Spanish).
     * (Some video files use ISO 639-2 codes, while others use ISO 639-1 codes, so we need to support both.)
     */
    private String language;

    /**
     * The user-friendly name of the track's language, derived from the "language" field.
     * This value is not localized.
     */
    @JsonProperty(value = "language_name", access = JsonProperty.Access.READ_ONLY)
    private String languageName;

    /**
     * We ignore this currently. It may be used in future versions.
     */
    private String codec;

    /**
     * An optional user-friendly title for the track, if it exists.
     * This is just whatever metadata was in the video file - we don't set this.
     */
    private String title;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
