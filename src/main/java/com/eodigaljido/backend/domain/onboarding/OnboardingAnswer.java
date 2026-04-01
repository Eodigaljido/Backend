package com.eodigaljido.backend.domain.onboarding;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OnboardingAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "age", length = 20)
    private String age;

    @Column(name = "activity", length = 50)
    private String activity;

    @Column(name = "gender", length = 20)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum OnboardingStatus {
        IN_PROGRESS, COMPLETED, SKIPPED
    }

    public void skip() {
        this.status = OnboardingStatus.SKIPPED;
    }

    public void complete() {
        this.status = OnboardingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void updateStep(int step, String answer) {
        switch (step) {
            case 1 -> this.region = answer;
            case 2 -> this.age = answer;
            case 3 -> this.activity = answer;
            case 4 -> this.gender = answer;
            default -> throw new IllegalArgumentException("잘못된 스텝 번호입니다: " + step);
        }
    }

    public void resetAnswers() {
        this.region = null;
        this.age = null;
        this.activity = null;
        this.gender = null;
        this.status = OnboardingStatus.IN_PROGRESS;
        this.completedAt = null;
    }

    public int getCurrentStep() {
        if (region == null) return 0;
        if (age == null) return 1;
        if (activity == null) return 2;
        if (gender == null) return 3;
        return 4;
    }
}
