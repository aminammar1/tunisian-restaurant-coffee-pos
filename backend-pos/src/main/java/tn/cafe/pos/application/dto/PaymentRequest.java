package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.cafe.pos.domain.model.PaymentType;

public record PaymentRequest(@NotBlank String commandeId, @NotNull PaymentType type) {}
