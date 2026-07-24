package com.coffeecommits.brakket.upload;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Subida directa de imágenes (logos, banners, portadas). Cualquier usuario
 * autenticado puede subir; la imagen resultante se sirve pública en
 * {@code /api/uploads/**} igual que cualquier URL externa que se pegue.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> subir(@RequestParam("archivo") MultipartFile archivo) {
        return Map.of("url", uploadService.guardarImagen(archivo));
    }
}
