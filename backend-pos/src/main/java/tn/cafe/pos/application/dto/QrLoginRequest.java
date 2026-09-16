package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record QrLoginRequest(@NotBlank String qrKey) {}
