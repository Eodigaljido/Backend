package com.eodigaljido.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigTest {

    @Test
    void publicCourseDetailMatcherAllowsOnlyUuidCourseIds() {
        assertThat(SecurityConfig.PUBLIC_COURSE_DETAIL_MATCHER.matches(request("GET",
                "/api/courses/550e8400-e29b-41d4-a716-446655440000"))).isTrue();

        assertThat(SecurityConfig.PUBLIC_COURSE_DETAIL_MATCHER.matches(request("GET",
                "/api/courses/my"))).isFalse();
        assertThat(SecurityConfig.PUBLIC_COURSE_DETAIL_MATCHER.matches(request("GET",
                "/api/courses/saved"))).isFalse();
    }

    @Test
    void publicCourseReviewMatcherAllowsOnlyUuidCourseIds() {
        assertThat(SecurityConfig.PUBLIC_COURSE_REVIEW_MATCHER.matches(request("POST",
                "/api/courses/550e8400-e29b-41d4-a716-446655440000/reviews"))).isTrue();

        assertThat(SecurityConfig.PUBLIC_COURSE_REVIEW_MATCHER.matches(request("POST",
                "/api/courses/my/reviews"))).isFalse();
    }

    private HttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
