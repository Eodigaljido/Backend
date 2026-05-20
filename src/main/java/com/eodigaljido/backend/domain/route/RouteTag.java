package com.eodigaljido.backend.domain.route;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "루트 태그 프리셋")
public enum RouteTag {
    산책, 카페, 맛집, 데이트, 관광, 야경, 쇼핑, 역사, 해변, 가족, 운동, 반려동물
}
