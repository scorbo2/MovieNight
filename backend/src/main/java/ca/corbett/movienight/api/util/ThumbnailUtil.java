package ca.corbett.movienight.api.util;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This utility class manages thumbnail storage and retrieval for MediaItems and MediaGroups.
 * Thumbnails are not stored in the SQLite database file, but rather stored externally in a
 * configured directory on the filesystem. If an image file with the expected name (including
 * the numeric id of the item/group) exists, then "hasThumbnail" will be true.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ThumbnailUtil {

    private ThumbnailUtil() {
    }

    /**
     * Returns true if the given MediaItem has a thumbnail image stored in the configured thumbnail directory.
     */
    public static boolean hasThumbnail(MediaItem mediaItem, AppConfig appConfig) {
        if (mediaItem == null || appConfig == null) {
            return false;
        }

        return thumbnailExists(mediaItem, mediaItem.getId(), appConfig);
    }

    /**
     * Deletes any thumbnail image associated with the given MediaItem from the configured thumbnail directory.
     */
    public static void removeThumbnail(MediaItem mediaItem, AppConfig appConfig) {
        if (mediaItem == null || appConfig == null) {
            return;
        }

        removeThumbnail(mediaItem, mediaItem.getId(), appConfig);
    }

    /**
     * Handy convenience method for thumbnail deletion when the id is known but there is no model object instance.
     */
    public static void removeMediaItemThumbnail(long id, AppConfig appConfig) {
        removeThumbnail(MediaItem.class.getSimpleName(), id, appConfig);
    }

    /**
     * Loads and returns the thumbnail image for the given MediaItem from the configured thumbnail directory,
     * or null if no thumbnail exists. No caching is enabled here! A new image will be loaded and returned
     * each time this method is invoked.
     */
    public static BufferedImage getThumbnail(MediaItem mediaItem, AppConfig appConfig) throws IOException {
        if (mediaItem == null || appConfig == null) {
            return null;
        }

        return getThumbnail(mediaItem, mediaItem.getId(), appConfig);
    }

    /**
     * Stores the given thumbnail image for the given MediaItem in the configured thumbnail directory.
     * Note that the save format will always be JPEG, regardless of the original image format.
     * Any previously stored thumbnail for this MediaItem will be overwritten.
     */
    public static void storeThumbnail(MediaItem mediaItem, BufferedImage image, AppConfig appConfig)
            throws IOException {
        if (mediaItem == null || image == null || appConfig == null) {
            return;
        }

        storeThumbnail(mediaItem, mediaItem.getId(), image, appConfig);
    }

    /**
     * Returns true if the given MediaGroup has a thumbnail image stored in the configured thumbnail directory.
     */
    public static boolean hasThumbnail(MediaGroup mediaGroup, AppConfig appConfig) {
        if (mediaGroup == null || appConfig == null) {
            return false;
        }

        return thumbnailExists(mediaGroup, mediaGroup.getId(), appConfig);
    }

    /**
     * Deletes any thumbnail image associated with the given MediaGroup from the configured thumbnail directory.
     */
    public static void removeThumbnail(MediaGroup mediaGroup, AppConfig appConfig) {
        if (mediaGroup == null || appConfig == null) {
            return;
        }

        removeThumbnail(mediaGroup, mediaGroup.getId(), appConfig);
    }

    /**
     * Handy convenience method for thumbnail deletion when the id is known but there is no model object instance.
     */
    public static void removeMediaGroupThumbnail(long id, AppConfig appConfig) {
        removeThumbnail(MediaGroup.class.getSimpleName(), id, appConfig);
    }

    /**
     * Stores the given thumbnail image for the given MediaGroup in the configured thumbnail directory.
     * Note that the save format will always be JPEG, regardless of the original image format.
     * Any previously stored thumbnail for this MediaGroup will be overwritten.
     */
    public static void storeThumbnail(MediaGroup mediaGroup, BufferedImage image, AppConfig appConfig)
            throws IOException {
        if (mediaGroup == null || image == null || appConfig == null) {
            return;
        }

        storeThumbnail(mediaGroup, mediaGroup.getId(), image, appConfig);
    }

    /**
     * Loads and returns the thumbnail image for the given MediaGroup from the configured thumbnail directory,
     * or null if no thumbnail exists. No caching is enabled here! A new image will
     * be loaded and returned each time this method is invoked.
     */
    public static BufferedImage getThumbnail(MediaGroup mediaGroup, AppConfig appConfig) throws IOException {
        if (mediaGroup == null || appConfig == null) {
            return null;
        }

        return getThumbnail(mediaGroup, mediaGroup.getId(), appConfig);
    }

    private static boolean thumbnailExists(Object something, long id, AppConfig appConfig) {
        Path thumbDir = appConfig.getThumbnailDir();
        String className = something.getClass().getSimpleName();
        return thumbDir.resolve(className + "_" + id + ".jpg").toFile().exists()
                || thumbDir.resolve(className + "_" + id + ".jpeg").toFile().exists()
                || thumbDir.resolve(className + "_" + id + ".png").toFile().exists();
    }

    private static void removeThumbnail(Object something, long id, AppConfig appConfig) {
        removeThumbnail(something.getClass().getSimpleName(), id, appConfig);
    }

    private static void removeThumbnail(String className, long id, AppConfig appConfig) {
        Path thumbDir = appConfig.getThumbnailDir();
        thumbDir.resolve(className + "_" + id + ".jpg").toFile().delete();
        thumbDir.resolve(className + "_" + id + ".jpeg").toFile().delete();
        thumbDir.resolve(className + "_" + id + ".png").toFile().delete();
    }

    private static BufferedImage getThumbnail(Object something, long id, AppConfig appConfig) throws IOException {
        Path thumbDir = appConfig.getThumbnailDir();
        String className = something.getClass().getSimpleName();
        Path[] paths = {
                thumbDir.resolve(className + "_" + id + ".jpg"),
                thumbDir.resolve(className + "_" + id + ".jpeg"),
                thumbDir.resolve(className + "_" + id + ".png")
        };

        // Just return the first one that exists, if any:
        for (Path path : paths) {
            if (path.toFile().exists()) {
                return ImageIO.read(path.toFile());
            }
        }

        return null;
    }

    private static void storeThumbnail(Object something, long id, BufferedImage image, AppConfig appConfig)
            throws IOException {
        Path thumbDir = appConfig.getThumbnailDir();
        String className = something.getClass().getSimpleName();
        Path outputPath = thumbDir.resolve(className + "_" + id + ".jpg");
        ImageIO.write(image, "jpg", outputPath.toFile());
    }
}
