package com.eodigaljido.backend.domain.route;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "route_legs",
    indexes = {
        @Index(name = "idx_route_legs_route_seq", columnList = "route_id, sequence")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RouteLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(nullable = false)
    private int sequence;

    @Column(length = 20)
    private String mode;

    private Integer minutes;

    @Column(name = "transit_type", length = 20)
    private String transitType;

    @Column(name = "directions_summary", columnDefinition = "TEXT")
    private String directionsSummary;

    @Column(name = "directions_detail", columnDefinition = "TEXT")
    private String directionsDetail;

    @Column(name = "distance_meters")
    private Integer distanceMeters;
}
