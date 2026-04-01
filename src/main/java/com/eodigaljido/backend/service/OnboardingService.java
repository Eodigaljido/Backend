package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.onboarding.*;
import com.eodigaljido.backend.exception.OnboardingException;
import com.eodigaljido.backend.exception.UserException;
import com.eodigaljido.backend.repository.OnboardingAnswerRepository;
import com.eodigaljido.backend.repository.RouteRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final OnboardingAnswerRepository onboardingAnswerRepository;
    private final RouteRepository routeRepository;

    // 온보딩 상태 조회
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        User user = findActiveUser(userId);
        return onboardingAnswerRepository.findByUser(user)
                .map(OnboardingStatusResponse::of)
                .orElse(OnboardingStatusResponse.notStarted());
    }

    // 온보딩 시작
    @Transactional
    public void start(Long userId) {
        User user = findActiveUser(userId);
        if (onboardingAnswerRepository.existsByUser(user)) {
            throw new OnboardingException("이미 온보딩을 시작한 사용자입니다.", HttpStatus.CONFLICT);
        }
        onboardingAnswerRepository.save(OnboardingAnswer.builder().user(user).build());
    }

    // 저장된 답변 조회
    @Transactional(readOnly = true)
    public OnboardingAnswersResponse getAnswers(Long userId) {
        User user = findActiveUser(userId);
        OnboardingAnswer answer = onboardingAnswerRepository.findByUser(user)
                .orElseThrow(() -> new OnboardingException("온보딩을 시작하지 않은 사용자입니다.", HttpStatus.NOT_FOUND));
        return OnboardingAnswersResponse.of(answer);
    }

    // 온보딩 건너뛰기
    @Transactional
    public void skip(Long userId) {
        User user = findActiveUser(userId);
        OnboardingAnswer answer = onboardingAnswerRepository.findByUser(user)
                .orElseGet(() -> onboardingAnswerRepository.save(
                        OnboardingAnswer.builder().user(user).build()
                ));

        if (answer.getStatus() == OnboardingAnswer.OnboardingStatus.COMPLETED) {
            throw new OnboardingException("이미 온보딩을 완료한 사용자입니다.", HttpStatus.CONFLICT);
        }

        answer.skip();
    }

    // 질문 목록 조회
    public OnboardingQuestionsResponse getQuestions() {
        return OnboardingQuestionsResponse.defaultQuestions();
    }

    // 단계별 임시 저장
    @Transactional
    public void saveStep(Long userId, OnboardingStepAnswerRequest request) {
        User user = findActiveUser(userId);
        OnboardingAnswer answer = onboardingAnswerRepository.findByUser(user)
                .orElseGet(() -> onboardingAnswerRepository.save(
                        OnboardingAnswer.builder().user(user).build()
                ));

        if (answer.getStatus() == OnboardingAnswer.OnboardingStatus.COMPLETED) {
            throw new OnboardingException("이미 온보딩을 완료한 사용자입니다.", HttpStatus.CONFLICT);
        }
        if (answer.getStatus() == OnboardingAnswer.OnboardingStatus.SKIPPED) {
            answer.resetAnswers();
        }

        answer.updateStep(request.step(), request.answer());
    }

    // 최종 제출
    @Transactional
    public OnboardingSubmitResponse submit(Long userId, OnboardingSubmitRequest request) {
        User user = findActiveUser(userId);
        OnboardingAnswer answer = onboardingAnswerRepository.findByUser(user)
                .orElseGet(() -> onboardingAnswerRepository.save(
                        OnboardingAnswer.builder().user(user).build()
                ));

        if (answer.getStatus() == OnboardingAnswer.OnboardingStatus.COMPLETED) {
            throw new OnboardingException("이미 온보딩을 완료한 사용자입니다.", HttpStatus.CONFLICT);
        }

        answer.updateStep(1, request.region());
        answer.updateStep(2, request.age());
        answer.updateStep(3, request.activity());
        answer.updateStep(4, request.gender());
        answer.complete();

        List<OnboardingSubmitResponse.RecommendedRouteDto> routes = getRecommendedRoutes();

        return new OnboardingSubmitResponse("온보딩이 완료되었습니다.", routes);
    }

    // 답변 초기화 (재설문)
    @Transactional
    public void resetAnswers(Long userId) {
        User user = findActiveUser(userId);
        OnboardingAnswer answer = onboardingAnswerRepository.findByUser(user)
                .orElseThrow(() -> new OnboardingException("온보딩 기록이 없습니다.", HttpStatus.NOT_FOUND));
        answer.resetAnswers();
    }

    // 특정 항목만 수정
    @Transactional
    public void patchAnswers(Long userId, OnboardingPatchRequest request) {
        User user = findActiveUser(userId);
        OnboardingAnswer answer = onboardingAnswerRepository.findByUser(user)
                .orElseThrow(() -> new OnboardingException("온보딩 기록이 없습니다. 먼저 설문을 시작해주세요.", HttpStatus.NOT_FOUND));

        if (answer.getStatus() != OnboardingAnswer.OnboardingStatus.COMPLETED) {
            throw new OnboardingException("완료된 온보딩 답변만 수정할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        if (request.region() != null) answer.updateStep(1, request.region());
        if (request.age() != null) answer.updateStep(2, request.age());
        if (request.activity() != null) answer.updateStep(3, request.activity());
        if (request.gender() != null) answer.updateStep(4, request.gender());
    }

    private List<OnboardingSubmitResponse.RecommendedRouteDto> getRecommendedRoutes() {
        List<Route> routes = routeRepository.findByStatusAndIsSharedAndDeletedAtIsNull(
                Route.RouteStatus.PUBLISHED, true, PageRequest.of(0, 3)
        );
        return routes.stream()
                .map(OnboardingSubmitResponse.RecommendedRouteDto::of)
                .toList();
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UserException("이미 탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        return user;
    }
}
