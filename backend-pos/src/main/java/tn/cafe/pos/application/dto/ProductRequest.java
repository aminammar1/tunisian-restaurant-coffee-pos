package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String nom,
        @NotNull @DecimalMin("0.0") BigDecimal prix,
        @NotBlank String categorieId,
        String description,
        String imageUrl,
        Boolean disponible) {}
