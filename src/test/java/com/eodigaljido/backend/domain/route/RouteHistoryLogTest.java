package com.eodigaljido.backend.domain.route;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteHistoryLogTest {

    @Test
    void mapsLegacyChatEditLogToChat() {
        RouteHistoryLog log = RouteHistoryLog.builder()
                .type(RouteHistoryLog.Type.EDIT)
                .editAction("CHAT_EDITED")
                .build();

        assertThat(log.getEffectiveType()).isEqualTo(RouteHistoryLog.Type.CHAT);
    }

    @Test
    void mapsLegacyCourseEditLogToCourse() {
        RouteHistoryLog log = RouteHistoryLog.builder()
                .type(RouteHistoryLog.Type.EDIT)
                .editAction("ROUTE_UPDATED")
                .build();

        assertThat(log.getEffectiveType()).isEqualTo(RouteHistoryLog.Type.COURSE);
    }

    @Test
    void keepsCurrentTypesUnchanged() {
        RouteHistoryLog chatLog = RouteHistoryLog.builder()
                .type(RouteHistoryLog.Type.CHAT)
                .build();
        RouteHistoryLog courseLog = RouteHistoryLog.builder()
                .type(RouteHistoryLog.Type.COURSE)
                .build();

        assertThat(chatLog.getEffectiveType()).isEqualTo(RouteHistoryLog.Type.CHAT);
        assertThat(courseLog.getEffectiveType()).isEqualTo(RouteHistoryLog.Type.COURSE);
    }

    @Test
    void suppliesActionsMissingFromLegacyLogs() {
        RouteHistoryLog chatLog = RouteHistoryLog.builder()
                .type(RouteHistoryLog.Type.CHAT)
                .build();
        RouteHistoryLog courseLog = RouteHistoryLog.builder()
                .type(RouteHistoryLog.Type.EDIT)
                .build();

        assertThat(chatLog.getEffectiveEditAction()).isEqualTo("CHAT_SENDED");
        assertThat(courseLog.getEffectiveEditAction()).isEqualTo("ROUTE_UPDATED");
    }
}
