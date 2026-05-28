package ca.corbett.movienight.api.util;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * This utility class manages thumbnail storage and retrieval for MediaItems and MediaGroups.
 * This code handles setting the "hasThumbnail" property on the model objects as needed.
 * <p>
 * Thumbnail images for media items are stored as sidecar files alongside the media file itself.
 * For example:
 * </p>
 * <pre>
 * Bladerunner.mkv
 * Bladerunner.jpg
 * StarTrek2.mkv
 * StarTrek2.jpg
 * </pre>
 * <p>
 * Thumbnail images for media groups are stored in the configured thumbnail directory with a
 * filename pattern of "MediaGroup_{id}.jpg". This is because media groups are abstract containers that
 * may not map to a specific directory on disk.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ThumbnailUtil {

    private static final Logger log = Logger.getLogger(ThumbnailUtil.class.getName());

    private ThumbnailUtil() {
    }

    /**
     * Returns true if the given MediaItem has a sidecar thumbnail image stored alongside the media file.
     */
    public static boolean hasThumbnail(MediaItem mediaItem, AppConfig appConfig) {
        if (mediaItem == null || appConfig == null) {
            return false;
        }

        return thumbnailExists(mediaItem, mediaItem.getId(), appConfig);
    }

    /**
     * Deletes any sidecar thumbnail image associated with the given MediaItem from the media directory.
     */
    public static void removeThumbnail(MediaItem mediaItem, AppConfig appConfig) {
        if (mediaItem == null || appConfig == null) {
            return;
        }

        removeThumbnail(mediaItem, mediaItem.getId(), appConfig);
    }

    /**
     * Handy convenience method for thumbnail deletion when only a media file path is known.
     */
    public static void removeMediaItemThumbnail(String mediaFilePath, AppConfig appConfig) {
        if (mediaFilePath == null || mediaFilePath.isBlank() || appConfig == null) {
            return;
        }

        MediaItem mediaItem = new MediaItem();
        mediaItem.setMediaFilePath(mediaFilePath);
        removeThumbnail(mediaItem, 0L, appConfig);
    }

    /**
     * Loads and returns the sidecar thumbnail image for the given MediaItem,
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
     * Stores the given thumbnail image for the given MediaItem as a sidecar file in the media directory.
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
        // Media item thumbnails are stored as sidecar files in the mediaDir:
        if (something instanceof MediaItem mediaItem) {
            Path mediaDir = appConfig.getMediaDir();
            String basename = getFilenameWithoutExtension(mediaItem.getMediaFilePath());
            if (basename == null || basename.isBlank()) {
                return false;
            }
            return mediaDir.resolve(basename + ".jpg").normalize().toFile().exists()
                    || mediaDir.resolve(basename + ".jpeg").normalize().toFile().exists()
                    || mediaDir.resolve(basename + ".png").normalize().toFile().exists();
        }

        // Otherwise, we'll assume it's a media group, and look in the configured thumbnail directory:
        Path thumbDir = appConfig.getThumbnailDir();
        String className = something.getClass().getSimpleName();
        return thumbDir.resolve(className + "_" + id + ".jpg").toFile().exists()
                || thumbDir.resolve(className + "_" + id + ".jpeg").toFile().exists()
                || thumbDir.resolve(className + "_" + id + ".png").toFile().exists();
    }

    private static void removeThumbnail(Object something, long id, AppConfig appConfig) {
        // Media item thumbnails are stored as sidecar files in the mediaDir:
        if (something instanceof MediaItem mediaItem) {
            Path mediaDir = appConfig.getMediaDir();
            String basename = getFilenameWithoutExtension(mediaItem.getMediaFilePath());
            if (basename == null || basename.isBlank()) {
                return;
            }
            boolean failed = false;
            failed |= !deleteIfExists(mediaDir.resolve(basename + ".jpg").normalize().toFile());
            failed |= !deleteIfExists(mediaDir.resolve(basename + ".jpeg").normalize().toFile());
            failed |= !deleteIfExists(mediaDir.resolve(basename + ".png").normalize().toFile());
            if (failed) {
                log.warning("Failed to delete one or more thumbnail files for media item id " + id);
            }
            return;
        }

        // Otherwise, we'll assume it's a media group, and look in the configured thumbnail directory:
        Path thumbDir = appConfig.getThumbnailDir();
        String className = something.getClass().getSimpleName();
        boolean failed = false;
        failed |= !deleteIfExists(thumbDir.resolve(className + "_" + id + ".jpg").toFile());
        failed |= !deleteIfExists(thumbDir.resolve(className + "_" + id + ".jpeg").toFile());
        failed |= !deleteIfExists(thumbDir.resolve(className + "_" + id + ".png").toFile());
        if (failed) {
            log.warning("Failed to delete one or more thumbnail files for media group id " + id);
        }
    }

    private static BufferedImage getThumbnail(Object something, long id, AppConfig appConfig) throws IOException {
        Path[] paths;

        // Media item thumbnails are stored as sidecar files in the mediaDir:
        if (something instanceof MediaItem mediaItem) {
            Path mediaDir = appConfig.getMediaDir();
            String basename = getFilenameWithoutExtension(mediaItem.getMediaFilePath());
            if (basename == null || basename.isBlank()) {
                return null;
            }
            paths = new Path[]{
                    mediaDir.resolve(basename + ".jpg").normalize(),
                    mediaDir.resolve(basename + ".jpeg").normalize(),
                    mediaDir.resolve(basename + ".png").normalize()
            };
        }

        // Otherwise, we'll assume it's a media group, and look in the configured thumbnail directory:
        else {
            Path thumbDir = appConfig.getThumbnailDir();
            String className = something.getClass().getSimpleName();
            paths = new Path[]{
                    thumbDir.resolve(className + "_" + id + ".jpg"),
                    thumbDir.resolve(className + "_" + id + ".jpeg"),
                    thumbDir.resolve(className + "_" + id + ".png")
            };
        }

        // Just return the first one that exists, if any:
        for (Path path : paths) {
            if (path.toFile().exists()) {
                return ImageIO.read(path.toFile());
            }
        }

        // It's not an error if we found nothing, it just means there's no thumbnail image for this model object.
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
     * @param appConfig Contains the location of our data directory (media sidecars for items,
     *                  thumbnail dir for groups).
     * @throws IOException If the write fails for any reason.
     */
    private static void storeThumbnail(Object something, long id, BufferedImage image, AppConfig appConfig)
            throws IOException {
        if (image == null) {
            throw new IOException("Cannot store null image as thumbnail");
        }
        File[] existingFiles;
        Path outputPath;
        String formatName = image.getColorModel().hasAlpha() ? "png" : "jpg";

        // Media item thumbnails are stored as sidecar files in the mediaDir:
        if (something instanceof MediaItem mediaItem) {
            Path mediaDir = appConfig.getMediaDir();
            String basename = getFilenameWithoutExtension(mediaItem.getMediaFilePath());
            if (basename == null || basename.isBlank()) {
                throw new IOException("Cannot store media item thumbnail without a valid media file path");
            }
            existingFiles = new File[]{
                    mediaDir.resolve(basename + ".jpg").normalize().toFile(),
                    mediaDir.resolve(basename + ".jpeg").normalize().toFile(),
                    mediaDir.resolve(basename + ".png").normalize().toFile()
            };
            outputPath = mediaDir.resolve(basename + "." + formatName).normalize();
        }

        // Otherwise, we'll assume it's a media group, and look in the configured thumbnail directory:
        else {
            Path thumbDir = appConfig.getThumbnailDir();
            String className = something.getClass().getSimpleName();
            existingFiles = new File[]{
                    thumbDir.resolve(className + "_" + id + ".jpg").toFile(),
                    thumbDir.resolve(className + "_" + id + ".jpeg").toFile(),
                    thumbDir.resolve(className + "_" + id + ".png").toFile()
            };
            outputPath = thumbDir.resolve(className + "_" + id + "." + formatName);
        }

        // The thumbnail write is not guaranteed to succeed!
        // But, we will nonetheless delete any existing thumbnails before proceeding.
        // If our write succeeds, there will be exactly one thumbnail for this model object.
        // If our write fails, we have "un-set" the thumbnail for that model object.
        // Perhaps not ideal, but simple and deterministic.
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
        if (!ImageIO.write(image, formatName, outputPath.toFile())) {
            throw new IOException("Failed to write thumbnail image as " + formatName);
        }
    }

    /**
     * Strips the last extension from the given file path and returns the result as a string.
     * If the file path has no "." in its name, you get the file path as-is.
     * If the file path has multiple dots, only the last one is stripped.
     */
    private static String getFilenameWithoutExtension(String mediaFilePath) {
        if (mediaFilePath == null || mediaFilePath.isBlank()) {
            return null;
        }
        int dotIndex = mediaFilePath.lastIndexOf('.');
        if (dotIndex > 0) {
            return mediaFilePath.substring(0, dotIndex);
        }
        return mediaFilePath;
    }

    private static boolean deleteIfExists(File file) {
        return !file.exists() || file.delete();
    }
}
