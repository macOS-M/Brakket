package com.coffeecommits.brakket.auth.dto;

/** JWT emitido tras registro o login local; el frontend lo persiste igual que el de Google. */
public record TokenResponse(String token) {
}
