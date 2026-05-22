package com.eodigaljido.backend.dto.friend;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 초대 링크 preview 응답 (비로그인 허용)")
public record FriendPreviewResponse(
        @Schema(description = "친구 코드", example = "ESSP3P")
        String friendCode,

        @Schema(description = "초대자 닉네임")
        String nickname,

        @Schema(description = "초대자 프로필 이미지 URL (null 가능)")
        String profileImageUrl
) {
    public static FriendPreviewResponse of(User user, Profile profile) {
        String nickname = profile != null ? profile.getNickname() : "어디갈지도 사용자";
        String imageUrl = profile != null ? profile.getProfileImageUrl() : null;
        return new FriendPreviewResponse(user.getFriendCode(), nickname, imageUrl);
    }
}
