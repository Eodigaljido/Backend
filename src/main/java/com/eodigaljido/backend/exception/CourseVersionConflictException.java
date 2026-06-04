package com.eodigaljido.backend.exception;

import com.eodigaljido.backend.dto.course.MyCourseDetailResponse;

public class CourseVersionConflictException extends RuntimeException {

    private final long currentVersion;
    private final MyCourseDetailResponse course;

    public CourseVersionConflictException(long currentVersion, MyCourseDetailResponse course) {
        super("COURSE_VERSION_CONFLICT");
        this.currentVersion = currentVersion;
        this.course = course;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }

    public MyCourseDetailResponse getCourse() {
        return course;
    }
}
