package ca.corbett.movienight;

import ca.corbett.movienight.service.RuntimeConfigService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:runtime-config-tests?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "movienight.data-dir=",
        "movienight.admin.username=admin",
        "movienight.admin.password=secret"
})
class RuntimeConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RuntimeConfigService runtimeConfigService;

    @BeforeEach
    void setUp() {
        runtimeConfigService.setFullyLocal(false);
    }

    @Test
    void getFullyLocal_returnsCurrentState() throws Exception {
        mockMvc.perform(get("/api/runtime-config/fully-local")
                                .with(remoteAddr("127.0.0.1"))
                                .with(httpBasic("admin", "secret")))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fullyLocal").value(false));
    }

    @Test
    void putFullyLocal_updatesCurrentState() throws Exception {
        mockMvc.perform(put("/api/runtime-config/fully-local")
                                .with(remoteAddr("127.0.0.1"))
                                .with(httpBasic("admin", "secret"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "fullyLocal": true
                                                 }
                                                 """))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fullyLocal").value(true));

        mockMvc.perform(get("/api/runtime-config/fully-local")
                                .with(remoteAddr("127.0.0.1"))
                                .with(httpBasic("admin", "secret")))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fullyLocal").value(true));
    }

    @Test
    void putFullyLocal_requiresField() throws Exception {
        mockMvc.perform(put("/api/runtime-config/fully-local")
                                .with(remoteAddr("127.0.0.1"))
                                .with(httpBasic("admin", "secret"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("fullyLocal is required"));
    }

    private static RequestPostProcessor remoteAddr(String remoteAddress) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
    }
}

