package com.coffeecommits.brakket.notification.controller;

import com.coffeecommits.brakket.notification.dto.NotificacionResponse;
import com.coffeecommits.brakket.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificacionResponse> listar(Authentication auth) {
        return notificationService.listar(auth.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> contarNoLeidas(Authentication auth) {
        return Map.of("count", notificationService.contarNoLeidas(auth.getName()));
    }

    @PatchMapping("/{id}/read")
    public NotificacionResponse marcarLeida(@PathVariable Long id, Authentication auth) {
        return notificationService.marcarLeida(auth.getName(), id);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> marcarTodasLeidas(Authentication auth) {
        notificationService.marcarTodasLeidas(auth.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        notificationService.eliminarDeBandeja(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
