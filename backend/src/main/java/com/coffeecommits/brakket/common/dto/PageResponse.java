package com.coffeecommits.brakket.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados para listados paginados de la API.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <E, T> PageResponse<T> from(Page<E> pagina, Function<E, T> mapper) {
        return new PageResponse<>(
                pagina.getContent().stream().map(mapper).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages()
        );
    }
}
