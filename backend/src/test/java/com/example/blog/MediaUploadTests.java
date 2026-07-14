package com.example.blog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MediaUploadTests {

    /** 1x1 PNG */
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
            0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
            0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
            0x00, 0x03, 0x00, 0x01, 0x00, 0x05, (byte) 0xFE, 0x02, (byte) 0xFE,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void registerUploadDir(DynamicPropertyRegistry registry) {
        registry.add("blog.upload.dir", () -> tempUploadDir.toAbsolutePath().toString());
    }

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAndGetToken("admin", "admin123");
    }

    @Test
    void uploadPngThenPublicGet() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", PNG_BYTES);

        MvcResult upload = mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.originalFilename").value("cover.png"))
                .andReturn();

        String url = JsonPath.read(upload.getResponse().getContentAsString(), "$.data.url");
        assertThat(url).startsWith("/uploads/");
        assertThat(url).doesNotContain("..");
        assertThat(url).endsWith(".png");

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("image")));
    }

    @Test
    void uploadRequiresAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", PNG_BYTES);
        mockMvc.perform(multipart("/api/admin/media").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void authorCanUpload() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "author_media_" + suffix;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass12345"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));
        String authorToken = loginAndGetToken(username, "pass12345");

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", PNG_BYTES);
        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").isNotEmpty());
    }

    @Test
    void rejectsIllegalType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.exe", "application/octet-stream", new byte[]{0x4D, 0x5A});
        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsFakeExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", new byte[]{0x00, 0x01, 0x02, 0x03});
        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsOversizedFile() throws Exception {
        byte[] huge = new byte[8 * 1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, huge, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", huge);
        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void maliciousFilenameDoesNotTraverse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../evil.png", "image/png", PNG_BYTES);
        MvcResult upload = mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String url = JsonPath.read(upload.getResponse().getContentAsString(), "$.data.url");
        assertThat(url).doesNotContain("..");
        assertThat(url).startsWith("/uploads/");
        String originalName = JsonPath.read(
                upload.getResponse().getContentAsString(), "$.data.originalFilename");
        assertThat(originalName).isEqualTo("evil.png");

        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }
}
