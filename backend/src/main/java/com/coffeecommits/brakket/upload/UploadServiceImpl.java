package com.coffeecommits.brakket.upload;

import com.coffeecommits.brakket.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Almacenamiento local de imágenes subidas (logos, banners, portadas).
 * Los archivos viven en {@code brakket.uploads.dir} (en Docker, un volumen)
 * y se sirven como recurso estático bajo {@code /api/uploads/**}.
 */
@Service
public class UploadServiceImpl implements UploadService {

    /** Tipos permitidos y la extensión con la que se guardan en disco. */
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path directorio;

    public UploadServiceImpl(@Value("${brakket.uploads.dir:uploads}") String dir) {
        this.directorio = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public String guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("No se recibió ningún archivo.");
        }
        String extension = TIPOS_PERMITIDOS.get(archivo.getContentType());
        if (extension == null) {
            throw new BusinessException(
                    "Formato no soportado: subí una imagen PNG, JPG, WebP o GIF.");
        }

        String nombre = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombre).normalize();
            // Cinturón extra: el nombre es un UUID nuestro, pero el destino
            // jamás puede salirse del directorio de uploads.
            if (!destino.startsWith(directorio)) {
                throw new BusinessException("Nombre de archivo inválido.");
            }
            try (var contenido = archivo.getInputStream()) {
                Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar la imagen; intentá de nuevo.");
        }

        // URL absoluta: pasa las validaciones @URL de los DTOs y sigue al host
        // del backend cuando la app se despliegue fuera de localhost.
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/uploads/")
                .path(nombre)
                .toUriString();
    }
}
