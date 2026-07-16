package com.example.blog;

import com.example.blog.config.CorsConfig;
import com.example.blog.config.CorsProperties;
import com.example.blog.config.ProdSecurityValidator;
import com.example.blog.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProdHardeningTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void publicSiteExposesRegistrationFlag() throws Exception {
        mockMvc.perform(get("/api/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publicRegistrationEnabled").isBoolean());
    }

    @Test
    void corsAllowsLocalhostPatternsByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/site");
        CorsConfiguration resolved = corsConfigurationSource.getCorsConfiguration(request);
        assertThat(resolved).isNotNull();
        assertThat(resolved.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(resolved.checkOrigin("https://evil.example.com")).isNull();
    }

    @Test
    void corsEnvCsvOverridesPatterns() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOriginPatterns(List.of("http://localhost:*"));
        MockEnvironment env = new MockEnvironment();
        env.setProperty(
                "BLOG_CORS_ALLOWED_ORIGIN_PATTERNS",
                "https://blog.example.com,https://www.blog.example.com"
        );
        CorsConfig config = new CorsConfig(props, env);
        assertThat(config.resolvePatterns())
                .containsExactly("https://blog.example.com", "https://www.blog.example.com");
    }

    @Test
    void prodValidatorRejectsDefaultJwtAndWeakSecrets() {
        assertThatThrownBy(() -> ProdSecurityValidator.validateJwtSecret(
                ProdSecurityValidator.INSECURE_DEFAULT_JWT_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development default");

        assertThatThrownBy(() -> ProdSecurityValidator.validateJwtSecret("short"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> ProdSecurityValidator.validateAdminPassword("admin123"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> ProdSecurityValidator.validateDbPassword("root"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> ProdSecurityValidator.validateSiteBaseUrl("http://localhost:5173"))
                .isInstanceOf(IllegalStateException.class);

        ProdSecurityValidator.validateJwtSecret("a-strong-production-jwt-secret-key-32b");
        ProdSecurityValidator.validateAdminPassword("strong-admin-pass");
        ProdSecurityValidator.validateDbPassword("strong-db-pass");
        ProdSecurityValidator.validateSiteBaseUrl("https://blog.example.com");
    }

    @Test
    void testProfileExposesRegistrationEnabledAndProdYamlPresent() {
        assertThat(environment.getProperty("blog.auth.public-registration-enabled")).isEqualTo("true");
        assertThat(getClass().getClassLoader().getResource("application-prod.yml")).isNotNull();
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "blog.auth.public-registration-enabled=false")
class ProdHardeningRegistrationClosedTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerRejectedWhenPublicRegistrationDisabled() throws Exception {
        String username = "closed_reg_" + System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("公开注册已关闭"));

        assertThat(userRepository.existsByUsername(username)).isFalse();

        mockMvc.perform(get("/api/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicRegistrationEnabled").value(false));
    }
}
