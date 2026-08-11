package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TeamSearchServiceImpl implements TeamSearchService {

    /** Largo máximo del texto de búsqueda: igual al del nombre de equipo. */
    static final int LARGO_MAXIMO_TEXTO = 120;
    static final int TAMANO_MAXIMO_PAGINA = 50;
    private static final Set<String> ESTADOS_VALIDOS = Set.of("ACTIVO", "DISUELTO");

    private final EquipoRepository equipoRepository;
    private final JuegoRepository juegoRepository;

    public TeamSearchServiceImpl(EquipoRepository equipoRepository,
                                 JuegoRepository juegoRepository) {
        this.equipoRepository = equipoRepository;
        this.juegoRepository = juegoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EquipoBusquedaResponse> buscar(String texto,
                                                       Long juegoId,
                                                       String disciplina,
                                                       String estado,
                                                       int page,
                                                       int size) {
        String textoNormalizado = normalizarTexto(texto);
        String estadoNormalizado = normalizarEstado(estado);
        String disciplinaNormalizada = normalizarDisciplina(disciplina);
        validarJuego(juegoId);

        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo");
        }
        if (size < 1 || size > TAMANO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException(
                    "El tamaño de página debe estar entre 1 y %d".formatted(TAMANO_MAXIMO_PAGINA));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nombre"));
        Specification<Equipo> filtro = construirFiltro(
                textoNormalizado, juegoId, disciplinaNormalizada, estadoNormalizado);
        Page<Equipo> resultado = equipoRepository.findAll(filtro, pageable);
        return PageResponse.from(resultado, EquipoBusquedaResponse::fromEntity);
    }

    /** Arma la consulta solo con los filtros presentes. */
    private Specification<Equipo> construirFiltro(String texto,
                                                  Long juegoId,
                                                  String disciplina,
                                                  String estado) {
        List<Specification<Equipo>> condiciones = new ArrayList<>();

        // El DTO siempre proyecta datos del juego: se trae con fetch en la
        // misma query (evita un select extra por cada equipo de la página).
        // La query de conteo no admite fetch, por eso se excluye.
        condiciones.add((root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("juego", JoinType.LEFT);
            }
            return cb.conjunction();
        });

        if (texto != null) {
            String patron = "%" + escaparComodinesLike(texto) + "%";
            condiciones.add((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombre")), patron, '\\'));
        }
        if (juegoId != null) {
            condiciones.add((root, query, cb) -> {
                if (query != null) query.distinct(true);
                return cb.equal(root.join("juegos", JoinType.LEFT).get("id"), juegoId);
            });
        }
        if (disciplina != null) {
            condiciones.add((root, query, cb) -> {
                if (query != null) query.distinct(true);
                return cb.equal(cb.lower(root.join("juegos", JoinType.LEFT).get("genero")), disciplina);
            });
        }
        if (estado != null) {
            condiciones.add((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        return Specification.allOf(condiciones);
    }

    /**
     * Escapa los comodines de LIKE para que se busquen literales: sin esto,
     * "_" matchea cualquier carácter y "%" cualquier secuencia.
     */
    private static String escaparComodinesLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > LARGO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(
                    "El texto de búsqueda supera el largo máximo de %d caracteres"
                            .formatted(LARGO_MAXIMO_TEXTO));
        }
        return limpio.toLowerCase();
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "El estado '%s' no es válido; use ACTIVO o DISUELTO".formatted(estado));
        }
        return normalizado;
    }

    private String normalizarDisciplina(String disciplina) {
        if (disciplina == null || disciplina.isBlank()) {
            return null;
        }
        String limpia = disciplina.trim();
        if (!juegoRepository.existsByGeneroIgnoreCase(limpia)) {
            throw new IllegalArgumentException(
                    "La disciplina '%s' no existe en el catálogo de juegos".formatted(disciplina));
        }
        return limpia.toLowerCase();
    }

    private void validarJuego(Long juegoId) {
        if (juegoId != null && !juegoRepository.existsById(juegoId)) {
            throw new IllegalArgumentException(
                    "El juego con id %d no existe en el catálogo".formatted(juegoId));
        }
    }
}
