package com.parkhyuns00.blog.domain.visitor.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.parkhyuns00.blog.domain.visitor.exception.VisitorException;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorExceptionCode;
import com.parkhyuns00.blog.domain.visitor.service.VisitorService;
import com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto;
import com.parkhyuns00.blog.global.web.cookie.VisitorCookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@WebMvcTest(VisitorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisitorService visitorService;

    @MockitoBean
    private VisitorCookieManager visitorCookieManager;

    @Test
    @DisplayName("방문자 쿠키가 있으면 해당 UUID로 방문을 기록한다.")
    void test_record_visit_success_when_cookie_exists() throws Exception {
        UUID visitorId = UUID.randomUUID();
        Cookie cookie = new Cookie("VISITOR_ID", visitorId.toString());

        when(visitorCookieManager.resolve(
            eq("VISITOR_ID"),
            eq(visitorId.toString()),
            any(HttpServletResponse.class)
        )).thenReturn(visitorId);

        mockMvc.perform(post("/api/visitors").cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        verify(visitorCookieManager).resolve(
            eq("VISITOR_ID"),
            eq(visitorId.toString()),
            any(HttpServletResponse.class)
        );
        verify(visitorService).recordVisit(visitorId);
    }

    @Test
    @DisplayName("방문자 쿠키가 없으면 발급된 UUID로 방문을 기록한다.")
    void test_record_visit_success_when_cookie_missing() throws Exception {
        UUID visitorId = UUID.randomUUID();

        when(visitorCookieManager.resolve(
            eq("VISITOR_ID"),
            isNull(),
            any(HttpServletResponse.class)
        )).thenReturn(visitorId);

        mockMvc.perform(post("/api/visitors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        verify(visitorCookieManager).resolve(
            eq("VISITOR_ID"),
            isNull(),
            any(HttpServletResponse.class)
        );
        verify(visitorService).recordVisit(visitorId);
    }

    @Test
    @DisplayName("게시글 조회 쿠키만 있으면 방문자 쿠키가 없는 것으로 처리한다.")
    void test_record_visit_success_when_only_post_viewer_cookie_exists()
        throws Exception {
        UUID postViewerId = UUID.randomUUID();
        UUID visitorId = UUID.randomUUID();
        Cookie cookie = new Cookie("POST_VIEWER_ID", postViewerId.toString());

        when(visitorCookieManager.resolve(
            eq("VISITOR_ID"),
            isNull(),
            any(HttpServletResponse.class)
        )).thenReturn(visitorId);

        mockMvc.perform(post("/api/visitors").cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        verify(visitorCookieManager).resolve(
            eq("VISITOR_ID"),
            isNull(),
            any(HttpServletResponse.class)
        );
        verify(visitorService).recordVisit(visitorId);
    }

    @Test
    @DisplayName("방문 기록 저장에 실패하면 비즈니스 오류 응답을 반환한다.")
    void test_record_visit_fail_when_record_failed() throws Exception {
        UUID visitorId = UUID.randomUUID();

        when(visitorCookieManager.resolve(
            eq("VISITOR_ID"),
            isNull(),
            any(HttpServletResponse.class)
        )).thenReturn(visitorId);

        doThrow(new VisitorException(VisitorExceptionCode.VISITOR_RECORD_FAILED))
            .when(visitorService).recordVisit(visitorId);

        mockMvc.perform(post("/api/visitors"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error.code").value("V_002"))
            .andExpect(jsonPath("$.error.message").value("방문 기록 저장에 실패했습니다."));

        verify(visitorService).recordVisit(visitorId);
    }

    @Test
    @DisplayName("방문자 통계 조회가 성공하면 전체와 오늘 및 어제 방문자 수를 반환한다.")
    void test_get_visitor_stats_success() throws Exception {
        when(visitorService.getVisitorStats()).thenReturn(new VisitorStatsDto(150L, 12L, 18L));

        mockMvc.perform(get("/api/visitors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.total").value(150))
            .andExpect(jsonPath("$.data.today").value(12))
            .andExpect(jsonPath("$.data.yesterday").value(18));

        verify(visitorService).getVisitorStats();
        verify(visitorService, never()).recordVisit(any(UUID.class));
        verifyNoInteractions(visitorCookieManager);
    }

    @Test
    @DisplayName("방문자 통계가 없으면 모든 방문자 수를 0으로 반환한다.")
    void test_get_visitor_stats_success_when_stats_empty() throws Exception {
        when(visitorService.getVisitorStats()).thenReturn(new VisitorStatsDto(0L, 0L, 0L));

        mockMvc.perform(get("/api/visitors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.today").value(0))
            .andExpect(jsonPath("$.data.yesterday").value(0));

        verify(visitorService).getVisitorStats();
        verifyNoInteractions(visitorCookieManager);
    }

    @Test
    @DisplayName("방문자는 쿠키와 CSRF 토큰 없이 방문자 통계를 조회할 수 있다.")
    void test_unauthenticated_user_can_get_visitor_stats() throws Exception {
        when(visitorService.getVisitorStats()).thenReturn(new VisitorStatsDto(150L, 12L, 18L));

        mockMvc.perform(get("/api/visitors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.total").value(150))
            .andExpect(jsonPath("$.data.today").value(12))
            .andExpect(jsonPath("$.data.yesterday").value(18))
            .andExpect(cookie().doesNotExist("VISITOR_ID"));

        verify(visitorService).getVisitorStats();
        verify(visitorService, never()).recordVisit(any(UUID.class));
    }
}
