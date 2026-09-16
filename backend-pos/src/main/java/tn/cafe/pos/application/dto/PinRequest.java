package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PinRequest(@NotBlank String username, @NotBlank String pin) {}
