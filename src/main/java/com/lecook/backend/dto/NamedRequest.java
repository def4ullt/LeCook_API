package com.lecook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NamedRequest(@NotBlank @Size(max = 60) String name) {}
