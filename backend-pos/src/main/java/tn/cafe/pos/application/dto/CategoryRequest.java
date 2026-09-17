package tn.cafe.pos.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String nom, String description, @Min(0) int ordre, String imageUrl) {
	public CategoryRequest(String nom, String description, int ordre) {
		this(nom, description, ordre, null);
	}
}
