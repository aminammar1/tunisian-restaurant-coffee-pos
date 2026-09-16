package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ProximityRequest(@NotBlank String commandeId, Boolean signalDetecte) {}
