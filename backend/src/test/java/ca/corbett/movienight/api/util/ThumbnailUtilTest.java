package ca.corbett.movienight.api.util;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThumbnailUtilTest {

    private AppConfig appConfig;

    @TempDir
    private File mediaDir;
    private File thumbnailDir;

    @BeforeEach
    public void setup() throws IOException {
        thumbnailDir = new File(mediaDir, "thumbnails");
        if (!thumbnailDir.mkdirs()) {
            throw new IOException("Failed to create thumbnail directory for tests");
        }
        File dbFile = new File(mediaDir, "test.db");
        appConfig = AppConfig.withCustomPaths(mediaDir.toPath(), thumbnailDir.toPath(), dbFile.toPath());
    }

    @Test
    public void hasThumbnail_withNullAppConfig_returnsFalse() throws IOException {
        // GIVEN model objects that have valid thumbnails:
        MediaItem testItem = new MediaItem();
        testItem.setId(22L);
        testItem.setMediaFilePath("test-item-22.mkv");
        ThumbnailUtil.storeThumbnail(testItem, createTestImage(), appConfig);
        MediaGroup testGroup = new MediaGroup();
        testGroup.setId(55L);
        ThumbnailUtil.storeThumbnail(testGroup, createTestImage(), appConfig);

        // WHEN we query for the thumbnail with a null AppConfig:
        // THEN we should get false, since the AppConfig is required to determine the thumbnail path:
        assertFalse(ThumbnailUtil.hasThumbnail(testItem, null));
        assertFalse(ThumbnailUtil.hasThumbnail(testGroup, null));
    }

    @Test
    public void hasThumbnail_withNullModelObject_returnsFalse() throws IOException {
        // WHEN we query for the thumbnail with a null model object:
        // THEN we should get false, since the model object is required to determine the thumbnail path:
        assertFalse(ThumbnailUtil.hasThumbnail((MediaItem)null, appConfig));
        assertFalse(ThumbnailUtil.hasThumbnail((MediaGroup)null, appConfig));
    }

    @Test
    public void hasThumbnail_withNoThumbnail_returnsFalse() {
        // GIVEN model objects that have no thumbnails:
        MediaItem testItem = new MediaItem();
        testItem.setId(33L);
        testItem.setMediaFilePath("test-item-33.mkv");
        MediaGroup testGroup = new MediaGroup();
        testGroup.setId(66L);

        // WHEN we query for the thumbnail:
        // THEN we should get false, since no thumbnail file exists for this item:
        assertFalse(ThumbnailUtil.hasThumbnail(testItem, appConfig));
        assertFalse(ThumbnailUtil.hasThumbnail(testGroup, appConfig));
    }

    @Test
    public void hasThumbnail_withThumbnail_returnsTrue() throws IOException {
        // GIVEN model objects that have valid thumbnails:
        MediaItem testItem = new MediaItem();
        testItem.setId(44L);
        testItem.setMediaFilePath("test-item-44.mkv");
        ThumbnailUtil.storeThumbnail(testItem, createTestImage(), appConfig);
        MediaGroup testGroup = new MediaGroup();
        testGroup.setId(77L);
        ThumbnailUtil.storeThumbnail(testGroup, createTestImage(), appConfig);

        // WHEN we query for the thumbnail:
        // THEN we should get true, since the thumbnail file exists for this item:
        assertTrue(ThumbnailUtil.hasThumbnail(testItem, appConfig));
        assertTrue(ThumbnailUtil.hasThumbnail(testGroup, appConfig));
    }

    @Test
    public void getThumbnail_withThumbnail_returnsImage() throws IOException {
        // GIVEN model objects that have a valid thumbnail:
        MediaItem testItem = new MediaItem();
        testItem.setId(88L);
        testItem.setMediaFilePath("test-item-88.mkv");
        ThumbnailUtil.storeThumbnail(testItem, createTestImage(), appConfig);
        MediaGroup testGroup = new MediaGroup();
        testGroup.setId(99L);
        ThumbnailUtil.storeThumbnail(testGroup, createTestImage(), appConfig);

        // WHEN we retrieve the thumbnail:
        // THEN we should get back a non-null image:
        assertNotNull(ThumbnailUtil.getThumbnail(testItem, appConfig));
        assertNotNull(ThumbnailUtil.getThumbnail(testGroup, appConfig));
    }

    @Test
    public void storeThumbnail_forMediaItem_shouldStoreAsSidecar() throws IOException {
        // GIVEN a MediaItem and a thumbnail image:
        MediaItem testItem = new MediaItem();
        testItem.setId(101L);
        testItem.setMediaFilePath("MyAmazingMediaFile.mkv");
        BufferedImage thumb = createTestImage();

        // WHEN we store this thumbnail:
        ThumbnailUtil.storeThumbnail(testItem, thumb, appConfig);

        // THEN the thumbnail image should have been stored as a sidecar file
        // alongside the media file, and NOT in the thumbnailDir:
        File expectedThumbnailFile = new File(mediaDir, "MyAmazingMediaFile.jpg");
        assertTrue(expectedThumbnailFile.exists(),
                   "Thumbnail file should have been stored as sidecar: " + expectedThumbnailFile.getAbsolutePath());
        assertFalse(new File(thumbnailDir, "MediaItem_101.jpg").exists(),
                    "Thumbnail should not be stored in thumbnailDir for MediaItem");
    }

    @Test
    public void storeThumbnail_forMediaGroup_shouldStoreInThumbnailDir() throws IOException {
        // GIVEN a MediaGroup and a thumbnail image:
        MediaGroup testGroup = new MediaGroup();
        testGroup.setId(202L);
        BufferedImage thumb = createTestImage();

        // WHEN we store this thumbnail:
        ThumbnailUtil.storeThumbnail(testGroup, thumb, appConfig);

        // THEN the thumbnail image should have been stored in the thumbnailDir using the group ID as the filename:
        File expectedThumbnailFile = new File(thumbnailDir, "MediaGroup_202.jpg");
        assertTrue(expectedThumbnailFile.exists(),
                   "Expected thumbnail file does not exist: " + expectedThumbnailFile.getAbsolutePath());
    }

    @Test
    public void removeThumbnail_withThumbnail_removesThumbnail() throws IOException {
        // GIVEN model objects that have a valid thumbnail:
        MediaItem testItem = new MediaItem();
        testItem.setId(111L);
        testItem.setMediaFilePath("test-item-111.mkv");
        ThumbnailUtil.storeThumbnail(testItem, createTestImage(), appConfig);
        MediaGroup testGroup = new MediaGroup();
        testGroup.setId(222L);
        ThumbnailUtil.storeThumbnail(testGroup, createTestImage(), appConfig);
        assertTrue(ThumbnailUtil.hasThumbnail(testItem, appConfig));
        assertTrue(ThumbnailUtil.hasThumbnail(testGroup, appConfig));
        assertNotNull(ThumbnailUtil.getThumbnail(testItem, appConfig));
        assertNotNull(ThumbnailUtil.getThumbnail(testGroup, appConfig));

        // WHEN we remove the thumbnail:
        ThumbnailUtil.removeThumbnail(testItem, appConfig);
        ThumbnailUtil.removeThumbnail(testGroup, appConfig);

        // THEN the thumbnail should no longer exist:
        assertFalse(ThumbnailUtil.hasThumbnail(testItem, appConfig));
        assertFalse(ThumbnailUtil.hasThumbnail(testGroup, appConfig));

        // AND getThumbnail should return null:
        assertNull(ThumbnailUtil.getThumbnail(testItem, appConfig));
        assertNull(ThumbnailUtil.getThumbnail(testGroup, appConfig));
    }

    private BufferedImage createTestImage() {
        // Create a simple 1x1 pixel image for testing
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFFFFFF); // white pixel
        return img;
    }
}