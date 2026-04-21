package ca.corbett.movienight;

import ca.corbett.movienight.service.ThumbnailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

class ThumbnailServiceTest {

    @TempDir
    Path tempDir;

    private ThumbnailService thumbnailService;

    @BeforeEach
    void setUp() throws Exception {
        thumbnailService = new ThumbnailService(tempDir.toString());
        thumbnailService.init();
    }

    private MockMultipartFile validJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return new MockMultipartFile("file", "test.jpg", "image/jpeg", baos.toByteArray());
    }

    private MockMultipartFile validPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return new MockMultipartFile("file", "test.png", "image/png", baos.toByteArray());
    }

    @Test
    void isEnabledAfterInit() {
        assertThat(thumbnailService.isEnabled()).isTrue();
    }

    @Test
    void subdirectoriesCreatedOnInit() {
        assertThat(Files.exists(tempDir.resolve("movies"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("episodes"))).isTrue();
    }

    @Test
    void disabledWhenDataDirBlank() throws Exception {
        ThumbnailService disabled = new ThumbnailService("");
        disabled.init();
        assertThat(disabled.isEnabled()).isFalse();
    }

    @Test
    void disabledWhenDataDirDoesNotExist() throws Exception {
        ThumbnailService disabled = new ThumbnailService("/nonexistent/path/that/does/not/exist");
        disabled.init();
        assertThat(disabled.isEnabled()).isFalse();
    }

    @Test
    void saveAndRetrieveJpegThumbnail() throws Exception {
        thumbnailService.saveThumbnail(validJpeg(100, 100), "movies", 1L);
        assertThat(thumbnailService.hasThumbnail("movies", 1L)).isTrue();
        assertThat(thumbnailService.getThumbnailPath("movies", 1L)).isNotNull();
    }

    @Test
    void saveAndRetrievePngThumbnail() throws Exception {
        thumbnailService.saveThumbnail(validPng(100, 100), "episodes", 5L);
        assertThat(thumbnailService.hasThumbnail("episodes", 5L)).isTrue();
        Path path = thumbnailService.getThumbnailPath("episodes", 5L);
        assertThat(path).isNotNull();
        assertThat(path.getFileName().toString()).endsWith(".png");
    }

    @Test
    void deleteThumbnailRemovesFile() throws Exception {
        thumbnailService.saveThumbnail(validJpeg(100, 100), "movies", 2L);
        assertThat(thumbnailService.hasThumbnail("movies", 2L)).isTrue();
        thumbnailService.deleteThumbnail("movies", 2L);
        assertThat(thumbnailService.hasThumbnail("movies", 2L)).isFalse();
    }

    @Test
    void deleteThumbnailIsNoopWhenNoneExists() {
        // Should not throw
        thumbnailService.deleteThumbnail("movies", 999L);
        assertThat(thumbnailService.hasThumbnail("movies", 999L)).isFalse();
    }

    @Test
    void saveThumbnailReplacesExisting() throws Exception {
        thumbnailService.saveThumbnail(validJpeg(100, 100), "movies", 3L);
        thumbnailService.saveThumbnail(validPng(200, 200), "movies", 3L);
        // Only one thumbnail should exist after replacement
        int count = 0;
        for (String ext : new String[]{"jpg", "jpeg", "png"}) {
            if (Files.exists(tempDir.resolve("movies").resolve("3." + ext))) count++;
        }
        assertThat(count).isEqualTo(1);
        assertThat(thumbnailService.hasThumbnail("movies", 3L)).isTrue();
    }

    @Test
    void rejectsInvalidImageData() {
        MockMultipartFile garbage = new MockMultipartFile("file", "bad.jpg", "image/jpeg",
                "this is not an image".getBytes());
        assertThatThrownBy(() -> thumbnailService.saveThumbnail(garbage, "movies", 4L))
                .hasMessageContaining("Invalid image");
    }

    @Test
    void rejectsTooLargeImage() throws Exception {
        MockMultipartFile large = validJpeg(2001, 2001);
        assertThatThrownBy(() -> thumbnailService.saveThumbnail(large, "movies", 5L))
                .hasMessageContaining("too large");
    }

    @Test
    void rejectsTooSmallImage() throws Exception {
        MockMultipartFile small = validJpeg(25, 25);
        assertThatThrownBy(() -> thumbnailService.saveThumbnail(small, "movies", 6L))
                .hasMessageContaining("too small");
    }

    @Test
    void returnsNullWhenNoThumbnail() {
        assertThat(thumbnailService.getThumbnailPath("movies", 999L)).isNull();
        assertThat(thumbnailService.hasThumbnail("movies", 999L)).isFalse();
    }

    @Test
    void disabledServiceRejectsUpload() throws Exception {
        ThumbnailService disabled = new ThumbnailService("");
        disabled.init();
        assertThatThrownBy(() -> disabled.saveThumbnail(validJpeg(100, 100), "movies", 1L))
                .hasMessageContaining("not available");
    }

    @Test
    void disabledServiceAlwaysReturnsFalseForHasThumbnail() throws Exception {
        ThumbnailService disabled = new ThumbnailService("");
        disabled.init();
        assertThat(disabled.hasThumbnail("movies", 1L)).isFalse();
    }
}
