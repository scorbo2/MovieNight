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
    private File dataDir;
    private File thumbnailDir;

    @BeforeEach
    public void setup() {
        appConfig = AppConfig.withDataDir(dataDir.toPath());
        thumbnailDir = appConfig.getThumbnailDir().toFile();
    }

    @Test
    public void hasThumbnail_withNullAppConfig_returnsFalse() throws IOException {
        // GIVEN model objects that have valid thumbnails:
        MediaItem testItem = new MediaItem();
        testItem.setId(22L);
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
    public void removeThumbnail_withThumbnail_removesThumbnail() throws IOException {
        // GIVEN model objects that have a valid thumbnail:
        MediaItem testItem = new MediaItem();
        testItem.setId(111L);
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