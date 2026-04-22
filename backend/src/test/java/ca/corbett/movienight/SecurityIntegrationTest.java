package ca.corbett.movienight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:security-tests?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "movienight.data-dir=",
        "movienight.admin.username=admin",
        "movienight.admin.password=secret"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicReadEndpointsRemainOpen() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRouteRequiresBasicAuthFromLocalhost() throws Exception {
        mockMvc.perform(get("/admin").with(remoteAddr("127.0.0.1")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin")
                        .with(remoteAddr("127.0.0.1"))
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpointsRequireLocalhostAndAuth() throws Exception {
        String movieJson = """
                {
                  "title": "Secured Movie",
                  "year": 2024,
                  "genre": "Drama",
                  "description": "Created through the secured admin API.",
                  "watched": false,
                  "tags": ["security"],
                  "videoFilePath": "/movies/secured_movie.mkv"
                }
                """;

        mockMvc.perform(post("/api/movies")
                        .with(remoteAddr("127.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieJson))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/movies")
                        .with(remoteAddr("192.168.1.50"))
                        .with(httpBasic("admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/movies")
                        .with(remoteAddr("127.0.0.1"))
                        .with(httpBasic("admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieJson))
                .andExpect(status().isCreated());
    }

    private static RequestPostProcessor remoteAddr(String remoteAddress) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
    }
}
