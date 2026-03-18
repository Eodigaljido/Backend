# DB 설계 — 어디갈지도 (Eodigaljido)

---

## 목차

1. [users — 유저 기본 정보](#1-users--유저-기본-정보)
2. [profiles — 유저 프로필](#2-profiles--유저-프로필)
3. [refresh_tokens — 토큰 관리](#3-refresh_tokens--토큰-관리)
4. [phone_verifications — SMS 인증](#4-phone_verifications--sms-인증)
5. [routes — 루트(경로) 정보](#5-routes--루트경로-정보)
6. [route_waypoints — 루트 경유지](#6-route_waypoints--루트-경유지)
7. [saved_routes — 즐겨찾기 루트](#7-saved_routes--즐겨찾기-루트)
8. [friends — 친구 관계](#8-friends--친구-관계)
9. [chat_rooms — 채팅방](#9-chat_rooms--채팅방)
10. [chat_room_members — 채팅방 참여자](#10-chat_room_members--채팅방-참여자)
11. [chat_messages — 채팅 메시지](#11-chat_messages--채팅-메시지)

---

## 1. `users` — 유저 기본 정보

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK (내부용) |
| uuid | CHAR(36) UNIQUE NOT NULL | 외부 노출용 식별자 |
| email | VARCHAR(255) UNIQUE | 이메일 (소셜은 null 가능) |
| password_hash | VARCHAR(255) NULL | bcrypt 해시 (소셜은 null) |
| phone | VARCHAR(20) UNIQUE NULL | 전화번호 |
| phone_verified_at | TIMESTAMP NULL | 인증 완료 시각 |
| provider | ENUM('local','google','kakao') | 로그인 방식 |
| provider_id | VARCHAR(255) NULL | OAuth 유저 ID |
| role | ENUM('user','admin') DEFAULT 'user' | 권한 |
| status | ENUM('active','suspended','deleted') DEFAULT 'active' | 계정 상태 |
| last_login_at | TIMESTAMP NULL | 마지막 로그인 시각 |
| deleted_at | TIMESTAMP NULL | soft delete |
| created_at | TIMESTAMP DEFAULT now() | |
| updated_at | TIMESTAMP DEFAULT now() | |

> **Index:** `uuid`, `email`, `phone`, `provider + provider_id` (복합)

---

## 2. `profiles` — 유저 프로필

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| user_id | BIGINT UNSIGNED UNIQUE NOT NULL | FK → users.id |
| nickname | VARCHAR(50) UNIQUE NOT NULL | 닉네임 |
| profile_image_url | VARCHAR(512) NULL | 프로필 이미지 URL |
| is_default_image | TINYINT(1) DEFAULT 1 | 기본 이미지 여부 |
| bio | VARCHAR(255) NULL | 자기소개 |
| created_at | TIMESTAMP DEFAULT now() | |
| updated_at | TIMESTAMP DEFAULT now() | |

> **Index:** `user_id`, `nickname`

---

## 3. `refresh_tokens` — 토큰 관리

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| user_id | BIGINT UNSIGNED NOT NULL | FK → users.id |
| token_hash | VARCHAR(255) NOT NULL | refresh token 해시값 저장 |
| device_info | VARCHAR(255) NULL | 디바이스 정보 (앱/OS 등) |
| ip_address | VARCHAR(45) NULL | 접속 IP (IPv6 대응) |
| expires_at | TIMESTAMP NOT NULL | 만료 시각 |
| revoked_at | TIMESTAMP NULL | 폐기 시각 (로그아웃 시) |
| created_at | TIMESTAMP DEFAULT now() | |

> **Index:** `user_id`, `token_hash`
>
> **모든 디바이스 로그아웃** 시 `user_id` 기준으로 전체 `revoked_at` 업데이트

---

## 4. `phone_verifications` — SMS 인증

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| phone | VARCHAR(20) NOT NULL | 인증 대상 전화번호 |
| code | CHAR(6) NOT NULL | 6자리 인증번호 |
| purpose | ENUM('register','change_phone') | 인증 목적 |
| verified | TINYINT(1) DEFAULT 0 | 인증 완료 여부 |
| expires_at | TIMESTAMP NOT NULL | 만료 시각 (보통 5분) |
| attempts | TINYINT DEFAULT 0 | 시도 횟수 (브루트포스 방지) |
| created_at | TIMESTAMP DEFAULT now() | |

> **Index:** `phone`, `expires_at`
>
> **attempts가 5회 초과 시** 해당 phone 잠금 처리 권장

---

## 5. `routes` — 루트(경로) 정보

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| uuid | CHAR(36) UNIQUE NOT NULL | 외부 노출용 식별자 |
| user_id | BIGINT UNSIGNED NOT NULL | FK → users.id (제작자) |
| title | VARCHAR(100) NOT NULL | 루트 이름 |
| description | TEXT NULL | 루트 설명 |
| status | ENUM('draft','published','deleted') DEFAULT 'draft' | 상태 |
| is_shared | TINYINT(1) DEFAULT 0 | 공유 여부 |
| share_token | VARCHAR(100) UNIQUE NULL | 공유 링크용 토큰 |
| total_distance | DECIMAL(10,2) NULL | 총 거리 (km) |
| estimated_time | INT NULL | 예상 소요시간 (분) |
| thumbnail_url | VARCHAR(512) NULL | 대표 이미지 |
| deleted_at | TIMESTAMP NULL | soft delete |
| created_at | TIMESTAMP DEFAULT now() | |
| updated_at | TIMESTAMP DEFAULT now() | |

> **Index:** `user_id`, `status`, `is_shared`, `share_token`

---

## 6. `route_waypoints` — 루트 경유지

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| route_id | BIGINT UNSIGNED NOT NULL | FK → routes.id |
| sequence | SMALLINT UNSIGNED NOT NULL | 순서 |
| name | VARCHAR(100) NULL | 지점 이름 |
| latitude | DECIMAL(10,7) NOT NULL | 위도 |
| longitude | DECIMAL(10,7) NOT NULL | 경도 |
| address | VARCHAR(255) NULL | 주소 |
| memo | VARCHAR(255) NULL | 메모 |
| created_at | TIMESTAMP DEFAULT now() | |

> **Index:** `route_id`, `sequence`
>
> 루트 수정 시 waypoints 전체 delete 후 재삽입 방식이 일반적

---

## 7. `saved_routes` — 즐겨찾기 루트

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| user_id | BIGINT UNSIGNED NOT NULL | FK → users.id |
| route_id | BIGINT UNSIGNED NOT NULL | FK → routes.id |
| created_at | TIMESTAMP DEFAULT now() | |

> **UNIQUE:** `(user_id, route_id)` — 중복 즐겨찾기 방지
>
> **Index:** `user_id`, `route_id`

---

## 8. `friends` — 친구 관계

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| requester_id | BIGINT UNSIGNED NOT NULL | FK → users.id (요청자) |
| receiver_id | BIGINT UNSIGNED NOT NULL | FK → users.id (수신자) |
| status | ENUM('pending','accepted','rejected','blocked') DEFAULT 'pending' | 관계 상태 |
| responded_at | TIMESTAMP NULL | 수락/거절 시각 |
| created_at | TIMESTAMP DEFAULT now() | |
| updated_at | TIMESTAMP DEFAULT now() | |

> **UNIQUE:** `(requester_id, receiver_id)` — 중복 요청 방지
>
> **Index:** `requester_id`, `receiver_id`, `status`
>
> 친구 목록 조회 시 `status = 'accepted'`이고 `requester_id OR receiver_id = 본인` 조건으로 양방향 조회

---

## 9. `chat_rooms` — 채팅방

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| uuid | CHAR(36) UNIQUE NOT NULL | 외부 노출용 식별자 |
| name | VARCHAR(100) NULL | 방 이름 (그룹채팅용) |
| type | ENUM('direct','group') DEFAULT 'direct' | 채팅 타입 |
| created_by | BIGINT UNSIGNED NOT NULL | FK → users.id |
| deleted_at | TIMESTAMP NULL | soft delete |
| created_at | TIMESTAMP DEFAULT now() | |
| updated_at | TIMESTAMP DEFAULT now() | |

> **Index:** `created_by`, `type`

---

## 10. `chat_room_members` — 채팅방 참여자

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| room_id | BIGINT UNSIGNED NOT NULL | FK → chat_rooms.id |
| user_id | BIGINT UNSIGNED NOT NULL | FK → users.id |
| role | ENUM('admin','member') DEFAULT 'member' | 방 내 역할 |
| last_read_at | TIMESTAMP NULL | 마지막 읽은 시각 (안읽음 수 계산용) |
| left_at | TIMESTAMP NULL | 탈퇴 시각 |
| created_at | TIMESTAMP DEFAULT now() | |

> **UNIQUE:** `(room_id, user_id)`
>
> **Index:** `room_id`, `user_id`

---

## 11. `chat_messages` — 채팅 메시지

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| id | BIGINT UNSIGNED AUTO_INCREMENT | PK |
| uuid | CHAR(36) UNIQUE NOT NULL | 외부 노출용 식별자 |
| room_id | BIGINT UNSIGNED NOT NULL | FK → chat_rooms.id |
| sender_id | BIGINT UNSIGNED NOT NULL | FK → users.id |
| type | ENUM('text','image','route','system') DEFAULT 'text' | 메시지 타입 |
| content | TEXT NULL | 텍스트 내용 |
| attachment_url | VARCHAR(512) NULL | 이미지 등 첨부파일 |
| ref_route_id | BIGINT UNSIGNED NULL | FK → routes.id (루트 공유 메시지) |
| is_deleted | TINYINT(1) DEFAULT 0 | 삭제 여부 |
| created_at | TIMESTAMP DEFAULT now() | |

> **Index:** `(room_id, created_at)` (복합) — 페이지네이션 성능 핵심
>
> `type = 'route'`일 때 `ref_route_id` 활용해 루트 카드 렌더링 가능
