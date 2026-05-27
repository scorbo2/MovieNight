package ca.corbett.movienight.api.util;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
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
     * The save format will be JPEG for any image that has no alpha channel (regardless of input format).
     * For images with alpha channel, we will save as PNG instead to preserve the transparency.
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
     * The save format will be JPEG for any image that has no alpha channel (regardless of input format).
     * For images with alpha channel, we will save as PNG instead to preserve the transparency.
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

    /**
     * Thumbnails are generally stored as jpeg images regardless of the uploaded format.
     * The exception is if we detect an alpha channel, we will attempt to save as PNG instead.
     * If ImageIO fails to write the image in the expected format, an IOException will be thrown.
     *
     * @param something A model object whose class will be used as a basis for the thumbnail filename.
     * @param id        The numeric id of the model object in question, also used in the thumbnail filename.
     * @param image     Any non-null BufferedImage. If it's transparent, we write PNG, else we write JPEG.
     * @param appConfig Contains the location of our data directory (more specifically, our thumbnail dir).
     * @throws IOException If the write fails for any reason.
     */
    private static void storeThumbnail(Object something, long id, BufferedImage image, AppConfig appConfig)
            throws IOException {
        if (image == null) {
            throw new IOException("Cannot store null image as thumbnail");
        }
        Path thumbDir = appConfig.getThumbnailDir();
        String className = something.getClass().getSimpleName();
        String formatName = image.getColorModel().hasAlpha() ? "png" : "jpg";

        // The thumbnail write is not guaranteed to succeed!
        // But, we will nonetheless delete any existing thumbnails before proceeding.
        // If our write succeeds, there will be exactly one thumbnail for this model object.
        // If our write fails, we have "un-set" the thumbnail for that model object.
        // Perhaps not ideal, but simple and deterministic.
        File[] existingFiles = {
                thumbDir.resolve(className + "_" + id + ".jpg").toFile(),
                thumbDir.resolve(className + "_" + id + ".jpeg").toFile(),
                thumbDir.resolve(className + "_" + id + ".png").toFile()
        };
        for (File file : existingFiles) {
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IOException("Failed to delete existing thumbnail file: " + file.getAbsolutePath());
                }
            }
        }

        // Now we try to create the new thumbnail file.
        // We know the input image is valid because we've already parsed it.
        // At this point, it can only fail if there's no writer for JPG or PNG, which is unlikely.
        Path outputPath = thumbDir.resolve(className + "_" + id + "." + formatName);
        if (!ImageIO.write(image, formatName, outputPath.toFile())) {
            throw new IOException("Failed to write thumbnail image as " + formatName);
        }
    }
}
