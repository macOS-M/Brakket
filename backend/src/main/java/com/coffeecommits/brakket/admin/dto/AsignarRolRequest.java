package com.coffeecommits.brakket.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignarRolRequest {

    @NotNull(message = "Debe indicar el rol a asignar")
    private Long rolId;
}