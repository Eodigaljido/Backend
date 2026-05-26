package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.user.*;
import com.eodigaljido.backend.exception.UserException;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RouteRepository;
import com.eodigaljido.backend.repository.SavedRouteRepository;
import com.eodigaljido.backend.repository.UserOAuthProviderRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserOAuthProviderRepository oAuthProviderRepository;
    private final RouteRepository routeRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final FileStorageService fileStorageService;
    private final FriendCodeService friendCodeService;

    // 내 프로필 전체 조회
    @Transactional
    public MyProfileResponse getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        friendCodeService.assignIfMissing(user);
        Profile profile = profileRepository.findByUser(user).orElse(null);

        long sharedCourseCount = routeRepository.countByUserIdAndIsSharedTrueAndStatusNot(userId, Route.RouteStatus.DELETED);
        BigDecimal averageRating = routeRepository.findAverageRatingOfSharedRoutesByUserId(userId, Route.RouteStatus.DELETED);
        long savedCourseCount = savedRouteRepository.countByUserId(userId);

        return MyProfileResponse.of(user, profile, oAuthProviderRepository.findAllByUser(user),
                sharedCourseCount, averageRating, savedCourseCount);
    }

    // 다른 유저 프로필 조회
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .orElseThrow(() -> new UserException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
        Profile profile = profileRepository.findByUser(user).orElse(null);

        long sharedCourseCount = routeRepository.countByUserIdAndIsSharedTrueAndStatusNot(user.getId(), Route.RouteStatus.DELETED);
        BigDecimal averageRating = routeRepository.findAverageRatingOfSharedRoutesByUserId(user.getId(), Route.RouteStatus.DELETED);
        long savedCourseCount = savedRouteRepository.countByUserId(user.getId());

        return UserProfileResponse.of(user, profile, sharedCourseCount, averageRating, savedCourseCount);
    }

    // 닉네임/바이오 수정
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UserException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (request.nickname() != null && !request.nickname().equals(profile.getNickname())) {
            if (profileRepository.existsByNicknameAndUserNot(request.nickname(), user)) {
                throw new UserException("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);
            }
            profile.updateNickname(request.nickname());
        }
        if (request.bio() != null) {
            profile.updateBio(request.bio());
        }
    }

    // 회원 탈퇴 (soft delete)
    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUser(userId);
        user.markDeleted();
    }

    // 프로필 이미지 변경
    @Transactional
    public MyProfileResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = findActiveUser(userId);
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UserException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        String oldUrl = profile.getProfileImageUrl();
        String newUrl;
        try {
            newUrl = fileStorageService.store(image, "profiles", user.getUuid());
        } catch (IllegalStateException e) {
            throw new UserException("파일 저장소 설정이 누락되었습니다. " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (IllegalArgumentException e) {
            throw new UserException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            throw new UserException("이미지 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        profile.updateProfileImage(newUrl);
        fileStorageService.delete(oldUrl);

        long sharedCourseCount = routeRepository.countByUserIdAndIsSharedTrueAndStatusNot(userId, Route.RouteStatus.DELETED);
        BigDecimal averageRating = routeRepository.findAverageRatingOfSharedRoutesByUserId(userId, Route.RouteStatus.DELETED);
        long savedCourseCount = savedRouteRepository.countByUserId(userId);

        return MyProfileResponse.of(user, profile, oAuthProviderRepository.findAllByUser(user),
                sharedCourseCount, averageRating, savedCourseCount);
    }

    // 프로필 이미지 삭제 (기본 이미지로 변경)
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = findActiveUser(userId);
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UserException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        String oldUrl = profile.getProfileImageUrl();
        profile.resetToDefaultImage();
        fileStorageService.delete(oldUrl);
    }

    // 유저 검색 (닉네임)
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        if (keyword.length() > 50) {
            throw new UserException("검색 키워드는 50자를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        return profileRepository.findByNicknameContainingIgnoreCase(keyword).stream()
                .filter(p -> p.getUser().getStatus() == User.UserStatus.ACTIVE)
                .map(p -> UserSearchResponse.of(p.getUser(), p))
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
