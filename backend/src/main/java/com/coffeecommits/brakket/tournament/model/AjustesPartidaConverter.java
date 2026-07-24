package com.coffeecommits.brakket.tournament.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** Serializa los ajustes de partida como JSON en una columna de texto. */
@Converter
public class AjustesPartidaConverter implements AttributeConverter<List<AjustePartida>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AjustePartida> ajustes) {
        if (ajustes == null || ajustes.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(ajustes);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron serializar los ajustes de partida", e);
        }
    }

    @Override
    public List<AjustePartida> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<AjustePartida>>() { });
        } catch (Exception e) {
            // Dato corrupto en BD: mejor un torneo sin ajustes que un 500.
            return List.of();
        }
    }
}
