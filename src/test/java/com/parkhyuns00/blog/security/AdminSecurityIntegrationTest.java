package com.parkhyuns00.blog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.parkhyuns00.blog.config.security.dto.AdminProperties;
import com.parkhyuns00.blog.domain.auth.repository.AdminAuthAttemptRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminSecurityIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-key";
    private static final String OTP_SECRET = "TEST_OTP_SCERET";
    private static final String OTP_CODE = "123123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminAuthAttemptRepository adminAuthAttemptRepository;

    @MockitoBean
    private AdminProperties adminProperties;

    @MockitoBean
    private GoogleAuthenticator googleAuthenticator;

    @BeforeEach
    void setUp() {
        adminAuthAttemptRepository.deleteAll();
        when(adminProperties.accessKeyHash()).thenReturn(passwordEncoder.encode(ADMIN_KEY));
        when(adminProperties.otpSecret()).thenReturn(OTP_SECRET);
    }

    @Test
    @DisplayName("CSRF 토큰 발급 엔드포인트는 인증 없이 접근 가능하고 XSRF-TOKEN 쿠키를 내려준다.")
    void test_csrf_endpoint_issue_csrf_cookie() throws Exception {
        mockMvc.perform(get("/api/csrf"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 관리자 키 로그인을 요청하면 거부된다.")
    void test_admin_key_login_without_csrf_token() throws Exception {
        mockMvc.perform(post("/api/admin/auth/key")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"adminKey":"test-admin-key"}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("올바른 관리자 키로 로그인하면 OTP_REQUIRED 응답과 PRE_ADMIN 세션이 생성된다.")
    void test_admin_key_login_success() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/auth/key")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"adminKey":"test-admin-key"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("OTP_REQUIRED"));
    }

    @Test
    @DisplayName("잘못된 관리자 키로 로그인하면 인증 실패 응답을 반환한다.")
    void test_admin_key_login_failure() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/auth/key")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"adminKey":"asdfasdfasdfasdf"}
                """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("AU01"));
    }

    @Test
    @DisplayName("관리자 키 인증 없이 OTP 인증을 요청하면 거절된다.")
    void test_otp_login_without_pre_admin_session_is_forbidden() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/auth/otp")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType("application/json")
            .content("""
                {"otpCode":"123123"}
                """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PRE_ADMIN 세션에서 올바른 OTP 코드를 입력하면 ADMIN 인증 상태가 된다.")
    void test_otp_login_success_after_admin_key_login() throws Exception {
        MockHttpSession session = adminKeyLogin();
        Cookie csrfCookie = issueCsrfToken();

        when(googleAuthenticator.authorize(OTP_SECRET, Integer.parseInt(OTP_CODE))).thenReturn(true);

        mockMvc.perform(post("/api/admin/auth/otp")
            .session(session)
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"otpCode":"123123"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("AUTHENTICATED"));
    }

    @Test
    @DisplayName("PRE_ADMIN 상태로 관리자 API에 접근하면 거절된다.")
    void test_pre_admin_cannot_access_admin_api() throws Exception {
        MockHttpSession session = adminKeyLogin();

        mockMvc.perform(get("/api/admin/test")
            .session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 상태면 관리자 API 인증을 통과한다")
    void test_admin_can_pass_admin_api_security() throws Exception {
        MockHttpSession session = adminKeyLogin();
        Cookie csrfCookie = issueCsrfToken();

        when(googleAuthenticator.authorize(OTP_SECRET, Integer.parseInt(OTP_CODE))).thenReturn(true);

        mockMvc.perform(post("/api/admin/auth/otp")
            .session(session)
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"otpCode":"123123"}
                """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/test").session(session))
            .andExpect(status().isNotFound());
    }

    private MockHttpSession adminKeyLogin() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/admin/auth/key")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"adminKey":"test-admin-key"}
                """))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);

        assertThat(session).isNotNull();
        return session;
    }

    private Cookie issueCsrfToken() throws Exception {
        return mockMvc.perform(get("/api/csrf")
            .session(new MockHttpSession()))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn()
            .getResponse()
            .getCookie("XSRF-TOKEN");
    }
}
