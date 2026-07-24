package com.coffeecommits.brakket.upload;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    /**
     * Guarda una imagen subida y devuelve la URL absoluta para servirla.
     * Valida tipo y tamaño; el nombre en disco es un UUID (el nombre del
     * cliente jamás toca el filesystem).
     */
    String guardarImagen(MultipartFile archivo);
}
