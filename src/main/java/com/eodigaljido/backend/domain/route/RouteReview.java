package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "route_reviews",
    indexes = {
        @Index(name = "idx_route_reviews_route_id", columnList = "route_id"),
        @Index(name = "idx_route_reviews_user_id", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RouteReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String text;
}
