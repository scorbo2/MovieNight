package ca.corbett.movienight.api.util;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaItem;
import ca.corbett.movienight.model.TrackMetadata;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private TrackMetadataUtil() {
    }

    /**
     * Makes a best-effort attempt to load track metadata from a sidecar JSON file and
     * populate the given MediaItem's audioTracks and subtitleTracks lists.
     * Exceptions are logged and swallowed - the MediaItem will simply have
     * empty track lists in this case.
     */
    public static void populateTrackMetadata(MediaItem item, AppConfig appConfig) {
        Path sidecar = appConfig.getMediaDir().resolve(item.getMediaFilePath()).resolveSibling(item.getMediaFilePath() + ".tracks.json");

        // If it doesn't exist, we're done here:
        if (!sidecar.toFile().exists()) {
            return;
        }

        // Otherwise, try to parse it using our expected wrapper format:
        try {
            MetadataWrapper wrapper = objectMapper.readValue(sidecar.toFile(), MetadataWrapper.class);
            item.setAudioTracks(wrapper.audioTracks);
            item.setSubtitleTracks(wrapper.subtitleTracks);
        } catch (Exception e) {
            // If parsing fails for any reason, just log it and move on - we don't want to break
            // the whole API just because of a bad sidecar file.
            log.warning("Failed to parse track metadata sidecar file: " + sidecar + " - " + e.getMessage());
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
