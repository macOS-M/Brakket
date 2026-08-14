package com.coffeecommits.brakket.league.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.model.FormatoCompetitivo;
import com.coffeecommits.brakket.game.repository.FormatoCompetitivoRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.dto.ActualizarLigaRequest;
import com.coffeecommits.brakket.league.dto.ActualizarTemporadaRequest;
import com.coffeecommits.brakket.league.dto.CrearLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearTemporadaRequest;
import com.coffeecommits.brakket.league.dto.JuegoOpcionResponse;
import com.coffeecommits.brakket.league.dto.LigaResponse;
import com.coffeecommits.brakket.league.dto.TemporadaResponse;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LigaServiceImplTest {

    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private TemporadaRepository temporadaRepository;
    @Mock
    private JuegoRepository juegoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private FormatoCompetitivoRepository formatoRepository;
    @Mock
    private TorneoRepository torneoRepository;
    @InjectMocks
    private LigaServiceImpl ligaService;

    private static final String CORREO = "ana@brakket.gg";

    /**
     * Fechas relativas y no fijas: una temporada no puede arrancar en el pasado,
     * asi que un LocalDate.of() escrito hoy convierte la prueba en una bomba de
     * tiempo que empieza a fallar sola cuando esa fecha queda atras.
     */
    private static final LocalDate MANANA = LocalDate.now().plusDays(1);
    private static final LocalDate EN_UN_MES = LocalDate.now().plusMonths(1);

    private Usuario comisionado() {
        return Usuario.builder().id(1L).nombre("Ana").correo(CORREO).build();
    }

    private Juego juego() {
        return Juego.builder().id(3L).nombre("Valorant").genero("FPS").activo(true).build();
    }

    private FormatoCompetitivo formato() {
        return FormatoCompetitivo.builder().id(7L).nombre("Eliminacion directa").activo(true).build();
    }

    @Test
    void crearLiga_persiste_y_asigna_comisionado_autenticado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));
        when(ligaRepository.existsByComisionadoIdAndNombreIgnoreCase(1L, "Liga Pro")).thenReturn(false);
        when(ligaRepository.save(any(Liga.class))).thenAnswer(inv -> {
            Liga l = inv.getArgument(0);
            l.setId(50L);
            return l;
        });

        LigaResponse resp = ligaService.crearLiga(CORREO, new CrearLigaRequest("Liga Pro", 3L, null, null, null));

        assertThat(resp.id()).isEqualTo(50L);
        assertThat(resp.nombre()).isEqualTo("Liga Pro");
        assertThat(resp.juegoNombre()).isEqualTo("Valorant");
        assertThat(resp.comisionadoId()).isEqualTo(1L);
    }

    @Test
    void crearLiga_rechaza_nombre_duplicado_del_mismo_comisionado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));
        when(ligaRepository.existsByComisionadoIdAndNombreIgnoreCase(1L, "Liga Pro")).thenReturn(true);

        assertThatThrownBy(() -> ligaService.crearLiga(CORREO, new CrearLigaRequest("Liga Pro", 3L, null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(ligaRepository, never()).save(any(Liga.class));
    }

    @Test
    void crearLiga_lanza_404_si_el_juego_no_existe() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(juegoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ligaService.crearLiga(CORREO, new CrearLigaRequest("Liga X", 99L, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearTemporada_rechaza_fecha_fin_no_posterior_al_inicio() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));

        CrearTemporadaRequest req = new CrearTemporadaRequest(
                "Temporada 1", MANANA, MANANA.minusDays(1));

        assertThatThrownBy(() -> ligaService.crearTemporada(50L, CORREO, req))
                .isInstanceOf(BusinessException.class);
        verify(temporadaRepository, never()).save(any());
    }

    @Test
    void crearTemporada_persiste_cuando_las_fechas_son_coherentes() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(temporadaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(formatoRepository.findById(7L)).thenReturn(Optional.of(formato()));

        CrearTemporadaRequest req = new CrearTemporadaRequest(
                "Temporada 1", MANANA, EN_UN_MES,
                "Reglas", "PLANIFICADA", 8, 7L);

        TemporadaResponse resp = ligaService.crearTemporada(50L, CORREO, req);

        assertThat(resp.nombre()).isEqualTo("Temporada 1");
        assertThat(resp.ligaId()).isEqualTo(50L);
        verify(temporadaRepository).save(any());
    }

    @Test
    void crearTemporada_bloquea_a_quien_no_es_comisionado() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        Usuario otro = Usuario.builder().id(2L).nombre("Beto").correo("beto@brakket.gg").build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo("beto@brakket.gg")).thenReturn(Optional.of(otro));

        CrearTemporadaRequest req = new CrearTemporadaRequest(
                "Temporada 1", MANANA, EN_UN_MES);

        assertThatThrownBy(() -> ligaService.crearTemporada(50L, "beto@brakket.gg", req))
                .isInstanceOf(ForbiddenException.class);
        verify(temporadaRepository, never()).save(any());
    }

    @Test
    void crearLiga_rechaza_juego_inactivo() {
        Juego inactivo = Juego.builder().id(4L).nombre("Juego Viejo").genero("MOBA").activo(false).build();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(juegoRepository.findById(4L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> ligaService.crearLiga(CORREO, new CrearLigaRequest("Liga X", 4L, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(ligaRepository, never()).save(any(Liga.class));
    }

    @Test
    void crearTemporada_rechaza_una_fecha_de_inicio_en_el_pasado() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));

        // Sin este control se podia crear una temporada que arrancaba en 1990.
        CrearTemporadaRequest req = new CrearTemporadaRequest(
                "Temporada vieja", LocalDate.now().minusDays(1), EN_UN_MES);

        assertThatThrownBy(() -> ligaService.crearTemporada(50L, CORREO, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anterior a hoy");
        verify(temporadaRepository, never()).save(any());
    }

    @Test
    void actualizarTemporada_deja_editar_una_que_ya_empezo_si_no_se_mueve_el_inicio() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        LocalDate inicioPasado = LocalDate.now().minusMonths(1);
        Temporada enCurso = Temporada.builder().id(9L).liga(liga).nombre("Apertura")
                .fechaInicio(inicioPasado).fechaFin(EN_UN_MES).reglas("Reglas")
                .estado("ACTIVA").cupoEquipos(8).formato(formato()).version(0L).build();

        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(temporadaRepository.findById(9L)).thenReturn(Optional.of(enCurso));
        when(formatoRepository.findById(7L)).thenReturn(Optional.of(formato()));
        when(temporadaRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        // Se cambian las reglas y se conserva el inicio: una temporada en curso
        // tiene su fecha de arranque en el pasado por definicion, y validarla
        // siempre la volveria intocable apenas pasara su primer dia.
        CrearTemporadaRequest config = new CrearTemporadaRequest(
                "Apertura", inicioPasado, EN_UN_MES, "Reglas nuevas", "ACTIVA", 8, 7L);

        TemporadaResponse resp = ligaService.actualizarTemporada(
                50L, 9L, CORREO, new ActualizarTemporadaRequest(config, 0L));

        assertThat(resp.nombre()).isEqualTo("Apertura");
        verify(temporadaRepository).saveAndFlush(any());
    }

    @Test
    void crearTemporada_rechaza_fechas_solapadas_con_otra_temporada() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(temporadaRepository.existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                50L, EN_UN_MES, MANANA)).thenReturn(true);

        CrearTemporadaRequest req = new CrearTemporadaRequest(
                "Temporada 2", MANANA, EN_UN_MES);

        assertThatThrownBy(() -> ligaService.crearTemporada(50L, CORREO, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solapan");
        verify(temporadaRepository, never()).save(any());
    }

    @Test
    void actualizarLiga_bloquea_a_quien_no_es_comisionado() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        Usuario otro = Usuario.builder().id(2L).nombre("Beto").correo("beto@brakket.gg").build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo("beto@brakket.gg")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> ligaService.actualizarLiga(
                50L, "beto@brakket.gg", new ActualizarLigaRequest("Otro nombre", 3L, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void actualizarLiga_rechaza_nombre_duplicado_al_renombrar() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(ligaRepository.existsByComisionadoIdAndNombreIgnoreCase(1L, "Liga Elite")).thenReturn(true);

        assertThatThrownBy(() -> ligaService.actualizarLiga(
                50L, CORREO, new ActualizarLigaRequest("Liga Elite", 3L, null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizarLiga_permite_renombrar_cambiando_solo_mayusculas() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));

        LigaResponse resp = ligaService.actualizarLiga(
                50L, CORREO, new ActualizarLigaRequest("LIGA PRO", 3L, null, null, null));

        assertThat(resp.nombre()).isEqualTo("LIGA PRO");
        verify(ligaRepository, never()).existsByComisionadoIdAndNombreIgnoreCase(any(), any());
    }

    @Test
    void listarJuegosDisponibles_filtra_juegos_inactivos() {
        Juego inactivo = Juego.builder().id(4L).nombre("Juego Viejo").genero("MOBA").activo(false).build();
        when(juegoRepository.findAll()).thenReturn(List.of(juego(), inactivo));

        List<JuegoOpcionResponse> opciones = ligaService.listarJuegosDisponibles();

        assertThat(opciones).hasSize(1);
        assertThat(opciones.get(0).nombre()).isEqualTo("Valorant");
    }

    @Test
    void eliminarLiga_borra_temporadas_y_liga_si_es_el_comisionado() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(comisionado()));

        ligaService.eliminarLiga(50L, CORREO, false);

        verify(temporadaRepository).deleteByLigaId(50L);
        verify(ligaRepository).delete(liga);
    }

    @Test
    void eliminarLiga_rechaza_a_quien_no_es_comisionado_ni_admin() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        Usuario otro = Usuario.builder().id(2L).nombre("Bruno").correo("bruno@brakket.gg").build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));
        when(usuarioRepository.findByCorreo("bruno@brakket.gg")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> ligaService.eliminarLiga(50L, "bruno@brakket.gg", false))
                .isInstanceOf(ForbiddenException.class);
        verify(ligaRepository, never()).delete(any(Liga.class));
        verify(temporadaRepository, never()).deleteByLigaId(any());
    }

    @Test
    void eliminarLiga_permite_a_un_admin_borrar_liga_ajena() {
        Liga liga = Liga.builder().id(50L).nombre("Liga Pro")
                .juego(juego()).comisionado(comisionado()).build();
        when(ligaRepository.findById(50L)).thenReturn(Optional.of(liga));

        ligaService.eliminarLiga(50L, "admin@brakket.gg", true);

        // Con esAdmin no hace falta resolver el usuario: borra directo.
        verify(temporadaRepository).deleteByLigaId(50L);
        verify(ligaRepository).delete(liga);
    }
}

