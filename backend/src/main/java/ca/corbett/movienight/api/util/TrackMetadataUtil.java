package ca.corbett.movienight.api.util;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaItem;
import ca.corbett.movienight.model.TrackMetadata;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Loads and parses our track metadata sidecar file if present.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TrackMetadataUtil {

    private static final Logger log = Logger.getLogger(TrackMetadataUtil.class.getName());

    /**
     * ObjectMapper is thread-safe and reusable, so we can use a single static instance for the whole class.
     */
    private static final ObjectMapper objectMapper = JsonSupport.getObjectMapper();
    private TrackMetadataUtil() {
    }

    /**
     * Makes a best-effort attempt to load track metadata from a sidecar JSON file and
     * populate the given MediaItem's audioTracks and subtitleTracks lists.
     * Exceptions are logged and swallowed - the MediaItem will simply have
     * empty track lists in this case.
     */
    public static void populateTrackMetadata(MediaItem item, AppConfig appConfig) {
        // We have to resolve the media file within our configured media dir first:
        Path mediaDir = appConfig.getMediaDir().toAbsolutePath().normalize();
        Path mediaPath = mediaDir.resolve(item.getMediaFilePath()).normalize();
        if (!mediaPath.startsWith(mediaDir)) {
            log.warning("TrackMetadataUtil: mediaFilePath resolved outside mediaDir; skipping: " + item.getMediaFilePath());
            return;
        }
        File mediaFile = mediaPath.toFile();

        // Then we have to compute the expected sidecar file:
        File parentDir = mediaFile.getParentFile();
        if (parentDir == null) {
            return;
        }
        File sidecarFile = new File(parentDir, mediaFile.getName() + ".tracks.json");

        // If it doesn't exist, we're done here:
        if (!sidecarFile.exists()) {
            return;
        }

        // Otherwise, try to parse it using our expected wrapper format:
        try {
            MetadataWrapper wrapper = objectMapper.readValue(sidecarFile, MetadataWrapper.class);
            item.setAudioTracks(wrapper.audioTracks);
            item.setSubtitleTracks(wrapper.subtitleTracks);
        } catch (Exception e) {
            // If parsing fails for any reason, just log it and move on - we don't want to break
            // the whole API just because of a bad sidecar file.
            log.warning("Failed to parse track metadata sidecar file: "
                                + sidecarFile.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    /**
     * The expected format of our track metadata sidecar JSON file.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class MetadataWrapper {
        @JsonProperty("file")
        String filename;

        @JsonProperty("audio")
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        List<TrackMetadata> audioTracks;

        @JsonProperty("subtitle")
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        List<TrackMetadata> subtitleTracks;

        public String getFilename() {
            return filename;
        }

        public List<TrackMetadata> getAudioTracks() {
            return audioTracks;
        }

        public List<TrackMetadata> getSubtitleTracks() {
            return subtitleTracks;
        }
    }
}
