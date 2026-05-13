package com.eodigaljido.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GroupRouteRequest(
        @NotBlank String destination,
        @Size(min = 2) List<GroupMember> members,
        String sessionId
) {}
