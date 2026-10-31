package com.fooddelivery.dto.auth;

import java.time.LocalDateTime;

public record CustomerResponse(Long id, String name, String email, String phone, LocalDateTime createdAt) { }
