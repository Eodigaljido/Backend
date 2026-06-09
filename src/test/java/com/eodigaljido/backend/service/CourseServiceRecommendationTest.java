package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;
import com.eodigaljido.backend.domain.route.Route;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseServiceRecommendationTest {

    @Test
    void matchesCompletedUserByRegionOrActivityWithoutPartialMatches() {
        Route route = Route.builder()
                .uuid("route-uuid")
                .title("추천 코스")
                .region("서울")
                .activityType("산책")
                .build();

        OnboardingAnswer sameRegion = OnboardingAnswer.builder()
                .region("서울")
                .activity(List.of("카페"))
                .build();
        OnboardingAnswer sameActivity = OnboardingAnswer.builder()
                .region("부산")
                .activity(List.of("산책"))
                .build();
        OnboardingAnswer partialActivity = OnboardingAnswer.builder()
                .region("부산")
                .activity(List.of("산책로 탐방"))
                .build();

        assertThat(CourseService.matchesRecommendedCourse(sameRegion, route)).isTrue();
        assertThat(CourseService.matchesRecommendedCourse(sameActivity, route)).isTrue();
        assertThat(CourseService.matchesRecommendedCourse(partialActivity, route)).isFalse();
    }
}
