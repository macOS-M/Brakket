package com.coffeecommits.brakket.config;

import com.coffeecommits.brakket.auth.service.AutoridadesService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que, en cada request, lee el header {@code Authorization: Bearer <jwt>},
 * valida el token y coloca la autenticación en el contexto de seguridad,
 * con los roles y permisos reales del usuario cargados desde la BD (RF-19).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AutoridadesService autoridadesService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AutoridadesService autoridadesService) {
        this.jwtService = jwtService;
        this.autoridadesService = autoridadesService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                String correo = jwtService.extractSubject(token);
                // Roles y permisos reales desde la BD (RF-19). Si el Optional
                // viene vacío (cuenta bloqueada o inexistente), NO se autentica
                // y la request terminará en 401.
                autoridadesService.cargarAutoridades(correo).ifPresent(autoridades -> {
                    var auth = new UsernamePasswordAuthenticationToken(correo, null, autoridades);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}