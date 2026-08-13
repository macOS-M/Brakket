package com.coffeecommits.brakket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.coffeecommits.brakket.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rechaza cuerpos de request desmesurados antes de que Spring los lea a
 * memoria. El límite de {@code spring.servlet.multipart} solo cubre subidas
 * de archivos (multipart); un POST con un JSON gigante lo esquivaba y se
 * bufferizaba completo — vector para tumbar la app por memoria.
 *
 * <p>El tope (8MB) queda por encima del límite de subida de imágenes (6MB),
 * así que no toca el tráfico legítimo: ninguna API de JSON manda más que unos
 * pocos KB. Se corre primero (orden alto y negativo) para cortar antes de
 * gastar nada. Basado en Content-Length; una request sin ese header (chunked)
 * queda acotada por el read-timeout del servidor.</p>
 */
@Component
@Order(-100)
public class LimiteTamanoRequestFilter extends OncePerRequestFilter {

    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long largo = request.getContentLengthLong();
        if (largo > MAX_BYTES) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("La solicitud es demasiado grande."));
            return;
        }
        chain.doFilter(request, response);
    }
}
