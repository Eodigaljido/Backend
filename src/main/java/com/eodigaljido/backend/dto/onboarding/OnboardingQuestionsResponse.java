package com.eodigaljido.backend.dto.onboarding;

import java.util.List;

public record OnboardingQuestionsResponse(
        List<QuestionDto> questions
) {
    public record QuestionDto(
            int step,
            String key,
            String title,
            String subtitle,
            List<String> options
    ) {}

    public static OnboardingQuestionsResponse defaultQuestions() {
        return new OnboardingQuestionsResponse(List.of(
                new QuestionDto(
                        1,
                        "region",
                        "거주 지역이\n어디인가요?",
                        "거주 지역에 따라서 추천하는 장소가 달라져요!",
                        List.of("서울/경기", "충청", "전라", "경상", "강원", "제주")
                ),
                new QuestionDto(
                        2,
                        "age",
                        "나이가\n어떻게 되시나요?",
                        "나이에 따라서 추천하는 장소가 달라져요!",
                        List.of("10대", "20대", "30대", "40대 이상")
                ),
                new QuestionDto(
                        3,
                        "activity",
                        "좋아하는 활동이\n무엇인가요?",
                        "취미 활동에 따라서 추천하는 장소가 달라져요!",
                        List.of("운동/건강", "예술/문화", "음악/공연", "여행/레저")
                ),
                new QuestionDto(
                        4,
                        "gender",
                        "성별이\n무엇인가요?",
                        "성별에 따라 가장 있기있는 장소를 알려드릴게요!",
                        List.of("남성", "여성", "기타")
                )
        ));
    }
}
