package com.coffeecommits.brakket.upload;

import com.coffeecommits.brakket.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceImplTest {

    @TempDir
    Path tempDir;

    private UploadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UploadServiceImpl(tempDir.toString());
        // La URL absoluta se construye desde la request actual.
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void guardaUnPngYDevuelveUrlBajoApiUploads() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", new byte[] {1, 2, 3});

        String url = service.guardarImagen(archivo);

        assertThat(url).contains("/api/uploads/");
        assertThat(url).endsWith(".png");
        String nombre = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(nombre))).isTrue();
    }

    @Test
    void elNombreEnDiscoEsUuidNoElDelCliente() {
        var archivo = new MockMultipartFile(
                "archivo", "../../../evil.png", "image/png", new byte[] {1});

        String url = service.guardarImagen(archivo);

        String nombre = url.substring(url.lastIndexOf('/') + 1);
        assertThat(nombre).doesNotContain("evil");
        assertThat(nombre).matches("[0-9a-f-]{36}\\.png");
    }

    @Test
    void rechazaTiposQueNoSonImagen() {
        var archivo = new MockMultipartFile(
                "archivo", "malware.exe", "application/octet-stream", new byte[] {1});

        assertThatThrownBy(() -> service.guardarImagen(archivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formato no soportado");
    }

    @Test
    void rechazaArchivosVacios() {
        var archivo = new MockMultipartFile("archivo", "vacio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.guardarImagen(archivo))
                .isInstanceOf(BusinessException.class);
    }
}
