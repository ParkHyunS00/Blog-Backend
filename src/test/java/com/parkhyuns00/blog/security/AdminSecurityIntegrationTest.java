package com.parkhyuns00.blog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.parkhyuns00.blog.config.security.dto.AdminProperties;
import com.parkhyuns00.blog.domain.auth.repository.AdminAuthAttemptRepository;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.service.PostImageService;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.PostCreateDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostDraftDetailDto;
import com.parkhyuns00.blog.util.GarageUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostImageService postImageService;

    @MockitoBean
    private GarageUtil garageUtil;

    @BeforeEach
    void setUp() {
        adminAuthAttemptRepository.deleteAll();
        when(adminProperties.accessKeyHash()).thenReturn(passwordEncoder.encode(ADMIN_KEY));
        when(adminProperties.otpSecret()).thenReturn(OTP_SECRET);
    }

    @Test
    @DisplayName("CSRF 토큰 발급 엔드포인트는 인증 없이 접근 가능하고 XSRF-TOKEN 쿠키를 내려준다.")
    void test_csrf_endpoint_issue_csrf_cookie() throws Exception {
        mockMvc.perform(get("/api/admin/csrf"))
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
    @DisplayName("ADMIN 세션에서 로그아웃하면 관리자 인증 상태가 해제된다.")
    void test_admin_logout_success() throws Exception {
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

        Cookie logoutCsrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/auth/logout")
                .session(session)
                .cookie(logoutCsrfCookie)
                .header("X-XSRF-TOKEN", logoutCsrfCookie.getValue()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/test")
                .session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PRE_ADMIN 세션에서는 로그아웃 요청이 거절된다.")
    void test_pre_admin_logout_is_forbidden() throws Exception {
        MockHttpSession session = adminKeyLogin();
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/auth/logout")
                .session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 관리자 인증 상태는 ADMIN_KEY_REQUIRED이다.")
    void test_admin_auth_status_without_session() throws Exception {
        mockMvc.perform(get("/api/admin/auth/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.authenticated").value(false))
            .andExpect(jsonPath("$.data.step").value("ADMIN_KEY_REQUIRED"));
    }

    @Test
    @DisplayName("PRE_ADMIN 세션의 관리자 인증 상태는 OTP_REQUIRED이다.")
    void test_admin_auth_status_with_pre_admin_session() throws Exception {
        MockHttpSession session = adminKeyLogin();

        mockMvc.perform(get("/api/admin/auth/status")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.authenticated").value(false))
            .andExpect(jsonPath("$.data.step").value("OTP_REQUIRED"));
    }

    @Test
    @DisplayName("ADMIN 세션의 관리자 인증 상태는 AUTHENTICATED이다.")
    void test_admin_auth_status_with_admin_session() throws Exception {
        MockHttpSession session = adminLogin();

        mockMvc.perform(get("/api/admin/auth/status")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.authenticated").value(true))
            .andExpect(jsonPath("$.data.step").value("AUTHENTICATED"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 생성 API에 접근할 수 없다.")
    void test_unauthenticated_user_cannot_create_post() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/posts")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPostRequestBody()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PRE_ADMIN 사용자는 게시글 생성 API에 접근할 수 없다.")
    void test_pre_admin_cannot_create_post() throws Exception {
        MockHttpSession session = adminKeyLogin();
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/posts")
                .session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPostRequestBody()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 사용자는 게시글 생성 API에 접근할 수 있다.")
    void test_admin_can_create_post() throws Exception {
        MockHttpSession session = adminLogin();
        Cookie csrfCookie = issueCsrfToken();

        when(postService.create(any(PostCreateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.PUBLISHED));

        mockMvc.perform(post("/api/admin/posts")
                .session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPostRequestBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("모든 사용자는 공개 게시글 목록을 조회할 수 있다.")
    void test_unauthenticated_user_can_get_published_posts() throws Exception {
        when(postService.getPublishedPosts(any(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("모든 사용자는 공개 게시글 상세 정보를 조회할 수 있다.")
    void test_unauthenticated_user_can_get_published_post() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(postService.getPublishedPost(1L))
            .thenReturn(new PostDetailDto(
                1L,
                "title",
                "summary",
                "content",
                10L,
                "Backend",
                "backend",
                List.of(),
                List.of(),
                now,
                now
            ));

        mockMvc.perform(get("/api/posts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.postId").value(1));
    }

    @Test
    @DisplayName("모든 사용자는 태그 목록을 조회할 수 있다.")
    void test_unauthenticated_user_can_get_tags() throws Exception {
        mockMvc.perform(get("/api/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 임시저장 API에 접근할 수 없다.")
    void test_unauthenticated_user_cannot_create_draft() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(post("/api/admin/posts/draft")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
        )
        .andExpect(status().isForbidden());

        verify(postService, never()).createDraft(any(PostDraftCreateRequest.class));
    }

    @Test
    @DisplayName("ADMIN 사용자는 게시글을 임시저장할 수 있다.")
    void test_admin_can_create_draft() throws Exception {
        MockHttpSession session = adminLogin();
        Cookie csrfCookie = issueCsrfToken();

        when(postService.createDraft(any(PostDraftCreateRequest.class))
        ).thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(post("/api/admin/posts/draft")
            .session(session)
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.postId").value(1))
        .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(postService).createDraft(any(PostDraftCreateRequest.class));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 임시저장 수정 API에 접근할 수 없다.")
    void test_unauthenticated_user_cannot_update_draft() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(
            put("/api/admin/posts/draft/{postId}", 1L)
                .cookie(csrfCookie)
                .header(
                    "X-XSRF-TOKEN",
                    csrfCookie.getValue()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());

        verify(postService, never()).updateDraft(anyLong(), any(PostDraftUpdateRequest.class));
    }

    @Test
    @DisplayName("ADMIN 사용자는 게시글 임시저장을 수정할 수 있다.")
    void test_admin_can_update_draft() throws Exception {
        MockHttpSession session = adminLogin();
        Cookie csrfCookie = issueCsrfToken();

        when(postService.updateDraft(eq(1L), any(PostDraftUpdateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(
            put("/api/admin/posts/draft/{postId}", 1L)
                .session(session)
                .cookie(csrfCookie)
                .header(
                    "X-XSRF-TOKEN",
                    csrfCookie.getValue()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {
                      "title": "updated title",
                      "summary": "updated summary",
                      "content": "<p>updated content</p>",
                      "categoryName": null,
                      "tagNames": [],
                      "thumbnailImageId": null,
                      "contentImageIds": []
                  }
                  """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(postService).updateDraft(eq(1L), any(PostDraftUpdateRequest.class));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 임시저장 목록을 조회할 수 없다.")
    void test_unauthenticated_user_cannot_get_draft_posts() throws Exception {
        mockMvc.perform(get("/api/admin/posts/draft")).andExpect(status().isForbidden());

        verify(postService, never()).getDraftPosts(anyInt());
    }

    @Test
    @DisplayName("ADMIN 사용자는 게시글 임시저장 목록을 조회할 수 있다.")
    void test_admin_can_get_draft_posts() throws Exception {
        MockHttpSession session = adminLogin();

        when(postService.getDraftPosts(0))
            .thenReturn(Page.empty(PageRequest.of(0, 10)));

        mockMvc.perform(
            get("/api/admin/posts/draft")
                .session(session)
                .param("page", "0")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.content").isEmpty())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.totalPages").value(0));

        verify(postService).getDraftPosts(0);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 임시저장 상세 정보를 조회할 수 없다.")
    void test_unauthenticated_user_cannot_get_draft_post() throws Exception {
        mockMvc.perform(
            get(
                "/api/admin/posts/draft/{postId}",
                1L
            ))
            .andExpect(status().isForbidden());

        verify(postService, never()).getDraftPost(anyLong());
    }

    @Test
    @DisplayName("ADMIN 사용자는 게시글 임시저장 상세 정보를 조회할 수 있다.")
    void test_admin_can_get_draft_post() throws Exception {
        MockHttpSession session = adminLogin();
        LocalDateTime now = LocalDateTime.now();

        PostDraftDetailDto detail = new PostDraftDetailDto(
            1L,
            "draft title",
            "draft summary",
            "<p>draft content</p>",
            null,
            List.of(),
            null,
            List.of(),
            now,
            now
        );

        when(postService.getDraftPost(1L)).thenReturn(detail);

        mockMvc.perform(
            get(
                "/api/admin/posts/draft/{postId}",
                1L
            ).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.title").value("draft title"))
            .andExpect(jsonPath("$.data.content").value("<p>draft content</p>"));

        verify(postService).getDraftPost(1L);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 임시저장을 삭제할 수 없다.")
    void test_unauthenticated_user_cannot_delete_draft() throws Exception {
        Cookie csrfCookie = issueCsrfToken();

        mockMvc.perform(
            delete(
                "/api/admin/posts/draft/{postId}",
                1L)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
            )
            .andExpect(status().isForbidden());

        verify(postService, never()).deleteDraft(anyLong());
    }

    @Test
    @DisplayName("ADMIN 사용자도 CSRF 토큰 없이는 게시글 임시저장을 삭제할 수 없다.")
    void test_admin_cannot_delete_draft_without_csrf() throws Exception {
        MockHttpSession session = adminLogin();

        mockMvc.perform(
            delete(
                "/api/admin/posts/draft/{postId}",
                1L)
                .session(session)
            )
            .andExpect(status().isForbidden());

        verify(postService, never()).deleteDraft(anyLong());
    }

    @Test
    @DisplayName("ADMIN 사용자는 게시글 임시저장을 삭제할 수 있다.")
    void test_admin_can_delete_draft() throws Exception {
        MockHttpSession session = adminLogin();
        Cookie csrfCookie = issueCsrfToken();

        doNothing().when(postService).deleteDraft(1L);

        mockMvc.perform(
            delete(
                "/api/admin/posts/draft/{postId}",
                1L)
                .session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        verify(postService).deleteDraft(1L);
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

    private MockHttpSession adminLogin() throws Exception {
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

        return session;
    }

    private Cookie issueCsrfToken() throws Exception {
        return mockMvc.perform(get("/api/admin/csrf")
            .session(new MockHttpSession()))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn()
            .getResponse()
            .getCookie("XSRF-TOKEN");
    }

    private String createPostRequestBody() {
        return """
          {
            "title": "title",
            "summary": "summary",
            "content": "content",
            "status": "PUBLISHED",
            "categoryName": "Spring",
            "tagNames": ["Java"],
            "thumbnailImageId": 1,
            "contentImageIds": [2]
          }
          """;
    }
}
