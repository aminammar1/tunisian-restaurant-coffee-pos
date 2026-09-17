package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.NotNull;

/** Explicit PATCH body: omitting availability must be rejected, never treated as false. */
public record AvailabilityRequest(@NotNull Boolean disponible) {}
