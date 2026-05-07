package com.eodigaljido.backend.domain.following;

public enum FollowingNewsActionType {
    COURSE_PUBLISHED("새 코스를 공개했어요"),
    COURSE_COMPLETED("코스를 완주했어요"),
    COURSE_LIKED("코스를 좋아했어요"),
    COURSE_SAVED("코스를 저장했어요");

    private final String actionText;

    FollowingNewsActionType(String actionText) {
        this.actionText = actionText;
    }

    public String getActionText() {
        return actionText;
    }
}
