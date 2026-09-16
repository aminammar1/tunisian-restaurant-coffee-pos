package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(@NotBlank String produitId, @Min(1) int quantite) {}
