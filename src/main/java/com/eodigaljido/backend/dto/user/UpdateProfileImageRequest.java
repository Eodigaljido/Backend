package com.eodigaljido.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileImageRequest(
        @NotBlank(message = "이미지 URL은 필수입니다.")
        @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.")
        @Pattern(
                regexp = "^https?://[\\w\\-]+(\\.[\\w\\-]+)+(:\\d+)?(/[\\w\\-./?%&=+#~:@!,;]*)?$",
                message = "올바른 HTTP/HTTPS URL 형식이어야 합니다."
        )
        String profileImageUrl
) {}
