package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.ClaveSerie;
import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.twitch.model.MetricaAudiencia;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.OrigenMetrica;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricasTransmisionServiceTest {

    @Mock TransmisionTwitchRepository transmisionRepository;
    @Mock MetricaAudienciaRepository audienciaRepository;
    @Mock MetricaChatRepository chatRepository;
    @Mock AnalisisSentimientoRepository sentimientoRepository;
    @Mock UsuarioRepository usuarioRepository;

    MetricasTransmisionService service;

    private static final LocalDateTime T18 = LocalDateTime.of(2026, 7, 24, 18, 0);
    private static final long COMISIONADO_ID = 42L;

    @BeforeEach
    void setup() {
        service = new MetricasTransmisionService(transmisionRepository, audienciaRepository,
                chatRepository, sentimientoRepository, usuarioRepository, 60_000L);
        when(transmisionRepository.findById(7L)).thenReturn(Optional.of(transmision()));
        when(audienciaRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of());
        when(chatRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of());
        when(sentimientoRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of());
    }

    // --- fixtures ---

    private Usuario usuario(long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    /** Transmisión colgada de un torneo cuya liga tiene comisionado. */
    private TransmisionTwitch transmision() {
        Liga liga = Liga.builder().id(1L).nombre("Liga Cenfotec")
                .comisionado(usuario(COMISIONADO_ID)).build();
        Temporada temporada = Temporada.builder().id(1L).nombre("2026").liga(liga).build();
        Torneo torneo = Torneo.builder().id(3L).nombre("Copa Brakket").temporada(temporada).build();
        return TransmisionTwitch.builder().id(7L).estado("EN_VIVO").torneo(torneo)
                .iniciadaEn(T18).creadaEn(T18).build();
    }

    private MetricaAudiencia audiencia(LocalDateTime instante, int espectadores) {
        return MetricaAudiencia.builder().fechaHora(instante).espectadores(espectadores)
                .origen(OrigenMetrica.REAL).build();
    }

    private MetricaChat chat(LocalDateTime instante, int mensajes, int usuarios) {
        return MetricaChat.builder().fechaHora(instante).mensajesPorMinuto(mensajes)
                .usuariosActivos(usuarios).build();
    }

    private AnalisisSentimiento sentimiento(LocalDateTime instante, String clasificacion, String puntaje) {
        return AnalisisSentimiento.builder().fechaHora(instante).clasificacion(clasificacion)
                .puntaje(new BigDecimal(puntaje)).build();
    }

    private SeriesTransmisionResponse.Serie serie(SeriesTransmisionResponse respuesta, ClaveSerie clave) {
        return respuesta.series().stream().filter(s -> s.clave() == clave).findFirst().orElseThrow();
    }

    private SeriesTransmisionResponse comoAdmin(String agrupacion) {
        return service.series(7L, null, null, agrupacion, "admin@brakket.cr", true);
    }

    // --- contrato de series ---

    @Test
    void devuelveLasCuatroSeriesAunSinMuestras() {
        SeriesTransmisionResponse respuesta = comoAdmin("CRUDA");

        assertThat(respuesta.series()).extracting(SeriesTransmisionResponse.Serie::clave)
                .containsExactlyInAnyOrder(ClaveSerie.values());
        assertThat(respuesta.series()).allSatisfy(s -> {
            assertThat(s.puntos()).isEmpty();
            assertThat(s.muestras()).isZero();
            assertThat(s.promedio()).isNull();
        });
        assertThat(respuesta.resumen().muestrasAudiencia()).isZero();
        assertThat(respuesta.resumen().picoEspectadores()).isNull();
    }

    @Test
    void agrupacionCrudaDevuelveUnPuntoPorMuestra() {
        when(audienciaRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of(
                audiencia(T18, 100), audiencia(T18.plusMinutes(1), 200), audiencia(T18.plusMinutes(2), 300)));

        SeriesTransmisionResponse.Serie espectadores = serie(comoAdmin("CRUDA"), ClaveSerie.ESPECTADORES);

        assertThat(espectadores.puntos()).hasSize(3);
        assertThat(espectadores.puntos()).extracting(SeriesTransmisionResponse.Punto::valor)
                .containsExactly(100.0, 200.0, 300.0);
        assertThat(espectadores.muestras()).isEqualTo(3);
    }

    @Test
    void agrupacionPorHoraPromediaCadaBucket() {
        when(audienciaRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of(
                audiencia(T18.plusMinutes(10), 100), audiencia(T18.plusMinutes(20), 300),
                audiencia(T18.plusHours(1), 800), audiencia(T18.plusHours(1).plusMinutes(30), 1000)));

        SeriesTransmisionResponse.Serie espectadores = serie(comoAdmin("HORA"), ClaveSerie.ESPECTADORES);

        assertThat(espectadores.puntos()).hasSize(2);
        assertThat(espectadores.puntos()).extracting(SeriesTransmisionResponse.Punto::instante)
                .containsExactly(T18, T18.plusHours(1));
        assertThat(espectadores.puntos()).extracting(SeriesTransmisionResponse.Punto::valor)
                .containsExactly(200.0, 900.0);
    }

    @Test
    void agrupacionPorHoraAlineaLasSeriesEnLosMismosBuckets() {
        when(audienciaRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of(
                audiencia(T18, 100), audiencia(T18.plusHours(1), 200)));
        // El chat solo tiene muestras en la segunda hora.
        when(chatRepository.buscarPorTransmisionYRango(any(), any(), any()))
                .thenReturn(List.of(chat(T18.plusHours(1), 50, 20)));

        SeriesTransmisionResponse respuesta = comoAdmin("HORA");

        assertThat(respuesta.series()).allSatisfy(s -> assertThat(s.puntos()).hasSize(2));
        SeriesTransmisionResponse.Serie mensajes = serie(respuesta, ClaveSerie.MENSAJES_POR_MINUTO);
        assertThat(mensajes.puntos().get(0).instante()).isEqualTo(T18);
        assertThat(mensajes.puntos().get(0).valor()).isNull();
        assertThat(mensajes.puntos().get(1).valor()).isEqualTo(50.0);
    }

    @Test
    void elPicoNoDependeDeLaAgrupacion() {
        List<MetricaAudiencia> muestras = List.of(
                audiencia(T18.plusMinutes(10), 100), audiencia(T18.plusMinutes(20), 900));
        when(audienciaRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(muestras);

        SeriesTransmisionResponse cruda = comoAdmin("CRUDA");
        SeriesTransmisionResponse porHora = comoAdmin("HORA");

        assertThat(serie(cruda, ClaveSerie.ESPECTADORES).pico()).isEqualTo(900.0);
        assertThat(serie(porHora, ClaveSerie.ESPECTADORES).pico()).isEqualTo(900.0);
        assertThat(porHora.resumen()).isEqualTo(cruda.resumen());
    }

    @Test
    void elSentimientoPredominanteEsLaModaDeLasClasificaciones() {
        when(sentimientoRepository.buscarPorTransmisionYRango(any(), any(), any())).thenReturn(List.of(
                sentimiento(T18, "POSITIVO", "0.80"),
                sentimiento(T18.plusMinutes(1), "NEGATIVO", "-0.40"),
                sentimiento(T18.plusMinutes(2), "POSITIVO", "0.60")));

        SeriesTransmisionResponse respuesta = comoAdmin("CRUDA");

        assertThat(respuesta.resumen().clasificacionPredominante()).isEqualTo("POSITIVO");
        assertThat(respuesta.resumen().muestrasSentimiento()).isEqualTo(3);
    }

    // --- rango y parámetros ---

    @Test
    void propagaElRangoAlRepositorio() {
        LocalDateTime hasta = T18.plusHours(2);
        service.series(7L, T18, hasta, "CRUDA", "admin@brakket.cr", true);

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(audienciaRepository).buscarPorTransmisionYRango(eq(7L), desdeCaptor.capture(), hastaCaptor.capture());
        assertThat(desdeCaptor.getValue()).isEqualTo(T18);
        assertThat(hastaCaptor.getValue()).isEqualTo(hasta);
        verify(chatRepository).buscarPorTransmisionYRango(eq(7L), eq(T18), eq(hasta));
        verify(sentimientoRepository).buscarPorTransmisionYRango(eq(7L), eq(T18), eq(hasta));
    }

    /**
     * Sin rango se consultan límites abiertos, nunca null: un parámetro suelto
     * comparado con "is null" no tiene tipo inferible en Postgres y revienta.
     */
    @Test
    void sinRangoConsultaConLimitesAbiertosYNoConNull() {
        SeriesTransmisionResponse respuesta = comoAdmin("CRUDA");

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(audienciaRepository).buscarPorTransmisionYRango(eq(7L), desde.capture(), hasta.capture());

        assertThat(desde.getValue()).isNotNull().isBefore(T18);
        assertThat(hasta.getValue()).isNotNull().isAfter(T18);
        // La respuesta sigue informando que no hubo rango pedido.
        assertThat(respuesta.desde()).isNull();
        assertThat(respuesta.hasta()).isNull();
    }

    @Test
    void rechazaRangoInvertido() {
        assertThatThrownBy(() -> service.series(7L, T18.plusHours(3), T18, "CRUDA", "admin@brakket.cr", true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El rango de fechas no es valido");

        verifyNoInteractions(audienciaRepository, chatRepository, sentimientoRepository);
    }

    @Test
    void rechazaAgrupacionDesconocida() {
        assertThatThrownBy(() -> comoAdmin("SEMANA")).isInstanceOf(BusinessException.class);
    }

    @Test
    void aceptaAgrupacionSinImportarMayusculas() {
        assertThat(comoAdmin("hora").agrupacion()).isEqualTo("HORA");
    }

    @Test
    void sinAgrupacionUsaCruda() {
        assertThat(comoAdmin(null).agrupacion()).isEqualTo("CRUDA");
    }

    @Test
    void lanzaNotFoundSiLaTransmisionNoExiste() {
        when(transmisionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.series(99L, null, null, "CRUDA", "admin@brakket.cr", true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculaDuracionHastaAhoraCuandoLaTransmisionSigueAbierta() {
        assertThat(comoAdmin("CRUDA").duracionMinutos()).isPositive();
    }

    // --- acceso ---

    @Test
    void adminAccedeSinConsultarElUsuario() {
        comoAdmin("CRUDA");

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void comisionadoDeLaLigaAccede() {
        when(usuarioRepository.findByCorreo("comi@brakket.cr")).thenReturn(Optional.of(usuario(COMISIONADO_ID)));

        SeriesTransmisionResponse respuesta =
                service.series(7L, null, null, "CRUDA", "comi@brakket.cr", false);

        assertThat(respuesta.transmisionId()).isEqualTo(7L);
    }

    @Test
    void comisionadoDeOtraLigaRecibeForbidden() {
        when(usuarioRepository.findByCorreo("otro@brakket.cr")).thenReturn(Optional.of(usuario(99L)));

        assertThatThrownBy(() -> service.series(7L, null, null, "CRUDA", "otro@brakket.cr", false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void unaTransmisionSinTorneoNoEsVisibleParaElComisionado() {
        when(transmisionRepository.findById(7L)).thenReturn(Optional.of(
                TransmisionTwitch.builder().id(7L).estado("EN_VIVO").creadaEn(T18).build()));
        when(usuarioRepository.findByCorreo("comi@brakket.cr")).thenReturn(Optional.of(usuario(COMISIONADO_ID)));

        assertThatThrownBy(() -> service.series(7L, null, null, "CRUDA", "comi@brakket.cr", false))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- catálogo ---

    @Test
    void elCatalogoCuentaLasMuestrasDeCadaTransmision() {
        when(transmisionRepository.findParaAnalitica()).thenReturn(List.of(transmision()));
        when(audienciaRepository.contarMuestrasPorTransmision(List.of(7L)))
                .thenReturn(List.<Object[]>of(new Object[]{7L, 268L}));

        var catalogo = service.catalogo("admin@brakket.cr", true);

        assertThat(catalogo).hasSize(1);
        assertThat(catalogo.get(0).muestras()).isEqualTo(268L);
        assertThat(catalogo.get(0).etiqueta()).isEqualTo("Copa Brakket — 24/07 18:00");
        assertThat(catalogo.get(0).nombreTorneo()).isEqualTo("Copa Brakket");
    }

    @Test
    void elCatalogoSoloTraeLasTransmisionesDelComisionado() {
        Torneo ajeno = Torneo.builder().id(9L).nombre("Copa ajena").build();
        TransmisionTwitch deOtro = TransmisionTwitch.builder().id(8L).estado("EN_VIVO")
                .torneo(ajeno).creadaEn(T18).build();
        when(transmisionRepository.findParaAnalitica()).thenReturn(List.of(transmision(), deOtro));
        when(usuarioRepository.findByCorreo("comi@brakket.cr")).thenReturn(Optional.of(usuario(COMISIONADO_ID)));
        when(audienciaRepository.contarMuestrasPorTransmision(List.of(7L))).thenReturn(List.of());

        var catalogo = service.catalogo("comi@brakket.cr", false);

        assertThat(catalogo).extracting(r -> r.id()).containsExactly(7L);
        assertThat(catalogo.get(0).muestras()).isZero();
    }
}
