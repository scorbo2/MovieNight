package ca.corbett.movienight;

import ca.corbett.movienight.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:stream-tests?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "movienight.data-dir=",
        "movienight.admin.username=admin",
        "movienight.admin.password=secret"
})
class StreamControllerTest {

    // 26 bytes of known content — one per letter — so ranges are easy to reason about.
    private static final byte[] VIDEO_CONTENT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @TempDir
    Path tempDir;

    private Path videoFile;

    @BeforeEach
    void setUp() throws Exception {
        videoFile = tempDir.resolve("test.mp4");
        Files.write(videoFile, VIDEO_CONTENT);
        when(mediaService.findById("M1")).thenReturn(videoFile.toString());
    }

    @Test
    void fullFileRequest_returns200() throws Exception {
        mockMvc.perform(get("/api/stream/M1"))
                .andExpect(status().isOk());
    }

    @Test
    void fullFileRequest_hasAcceptRangesHeader() throws Exception {
        mockMvc.perform(get("/api/stream/M1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"));
    }

    @Test
    void fullFileRequest_hasVideoMp4ContentType() throws Exception {
        mockMvc.perform(get("/api/stream/M1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("video/mp4"));
    }

    @Test
    void fullFileRequest_returnsEntireFileBody() throws Exception {
        mockMvc.perform(get("/api/stream/M1"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VIDEO_CONTENT));
    }

    @Test
    void rangeRequest_returns206() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent());
    }

    @Test
    void rangeRequest_hasAcceptRangesHeader() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Accept-Ranges", "bytes"));
    }

    @Test
    void rangeRequest_hasContentRangeHeader() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range",
                        containsString("bytes 0-9/" + VIDEO_CONTENT.length)));
    }

    @Test
    void rangeRequest_returnsCorrectBytes() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes("ABCDEFGHIJ".getBytes()));
    }

    @Test
    void rangeRequest_midFile_returnsCorrectBytes() throws Exception {
        // bytes=10-14 → "KLMNO"
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=10-14"))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes("KLMNO".getBytes()));
    }

    @Test
    void missingVideoFile_returns404() throws Exception {
        Path nonexistent = tempDir.resolve("nonexistent.mp4");
        when(mediaService.findById("M2")).thenReturn(nonexistent.toString());

        mockMvc.perform(get("/api/stream/M2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidMediaId_returns400() throws Exception {
        when(mediaService.findById("bad"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media id: bad"));

        mockMvc.perform(get("/api/stream/bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void entityNotFound_returns404() throws Exception {
        when(mediaService.findById("M999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with id: 999"));

        mockMvc.perform(get("/api/stream/M999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rangeRequest_openEnded_returnsBytesToEndOfFile() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=10-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range",
                        containsString("bytes 10-25/" + VIDEO_CONTENT.length)))
                .andExpect(content().bytes("KLMNOPQRSTUVWXYZ".getBytes()));
    }

    @Test
    void rangeRequest_suffix_returnsLastBytes() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=-5"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range",
                        containsString("bytes 21-25/" + VIDEO_CONTENT.length)))
                .andExpect(content().bytes("VWXYZ".getBytes()));
    }

    @Test
    void rangeRequest_outOfBounds_returns416() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=999-1000"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string("Content-Range", "bytes */" + VIDEO_CONTENT.length));
    }

    @Test
    void rangeRequest_hasExpectedContentLength() throws Exception {
        mockMvc.perform(get("/api/stream/M1")
                        .header("Range", "bytes=10-14"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Length", "5"));
    }
}
