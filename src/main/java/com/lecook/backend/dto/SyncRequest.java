package com.lecook.backend.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record SyncRequest(@NotNull @Size(max = 10000) List<@NotNull UUID> knownIds) {}
