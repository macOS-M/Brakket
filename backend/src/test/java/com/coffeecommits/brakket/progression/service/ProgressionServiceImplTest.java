package com.coffeecommits.brakket.progression.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.progression.model.*;
import com.coffeecommits.brakket.progression.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressionServiceImplTest {
    private static final String CORREO="ana@brakket.gg";
    @Mock UsuarioRepository usuarios;
    @Mock LogroRepository logros;
    @Mock LogroJugadorRepository logrosJugador;
    @Mock ElementoPersonalizacionRepository elementos;
    @Mock CanjePersonalizacionRepository canjes;
    @Mock PersonalizacionPerfilRepository perfiles;
    @Mock AchievementSynchronizer synchronizer;
    ProgressionServiceImpl service;
    Usuario ana;

    @BeforeEach void setUp() {
        service=new ProgressionServiceImpl(usuarios,logros,logrosJugador,elementos,canjes,perfiles,synchronizer);
        ana=Usuario.builder().id(7L).correo(CORREO).nombre("Ana").build();
    }

    @Test void consultar_sincroniza_y_muestra_desbloqueados_antes_que_pendientes() {
        Logro obtenido=logro(1L,"Primera victoria",100);
        Logro pendiente=logro(2L,"Campeón",500);
        when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(ana));
        when(logrosJugador.findByUsuarioIdAndActivoTrue(7L)).thenReturn(List.of(registro(obtenido)));
        when(logros.findAll()).thenReturn(List.of(pendiente,obtenido));
        respuestasVacias();

        var respuesta=service.consultar(CORREO);

        verify(synchronizer).sincronizar(ana);
        assertThat(respuesta.logros()).extracting(l -> l.nombre()).containsExactly("Primera victoria","Campeón");
        assertThat(respuesta.logros().getFirst().desbloqueado()).isTrue();
        assertThat(respuesta.logros().getLast().desbloqueado()).isFalse();
        assertThat(respuesta.puntos()).isEqualTo(100);
    }

    @Test void canjear_bloquea_saldo_insuficiente_sin_guardar() {
        ElementoPersonalizacion marco=elemento(20L,"Bronce",TipoPersonalizacion.MARCO,200);
        when(usuarios.findLockedByCorreo(CORREO)).thenReturn(Optional.of(ana));
        when(elementos.findById(20L)).thenReturn(Optional.of(marco));
        when(canjes.existsByUsuarioIdAndElementoId(7L,20L)).thenReturn(false);
        when(logrosJugador.findByUsuarioIdAndActivoTrue(7L)).thenReturn(List.of(registro(logro(1L,"En equipo",50))));
        when(canjes.findByUsuarioId(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.canjear(CORREO,20L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("puntos suficientes");
        verify(canjes,never()).save(any());
    }

    @Test void canjear_guarda_el_costo_y_la_propiedad() {
        ElementoPersonalizacion titulo=elemento(10L,"Estratega",TipoPersonalizacion.TITULO,100);
        when(usuarios.findLockedByCorreo(CORREO)).thenReturn(Optional.of(ana));
        when(elementos.findById(10L)).thenReturn(Optional.of(titulo));
        when(canjes.existsByUsuarioIdAndElementoId(7L,10L)).thenReturn(false);
        when(logrosJugador.findByUsuarioIdAndActivoTrue(7L)).thenReturn(List.of(registro(logro(1L,"Debut",150))));
        when(canjes.findByUsuarioId(7L)).thenReturn(List.of());
        when(logros.findAll()).thenReturn(List.of()); when(elementos.findAllByOrderByCostoPuntosAsc()).thenReturn(List.of());
        when(perfiles.findByUsuarioId(7L)).thenReturn(List.of());

        service.canjear(CORREO,10L);

        ArgumentCaptor<CanjePersonalizacion> captor=ArgumentCaptor.forClass(CanjePersonalizacion.class);
        verify(canjes).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(ana);
        assertThat(captor.getValue().getElemento()).isEqualTo(titulo);
        assertThat(captor.getValue().getCostoPuntos()).isEqualTo(100);
    }

    @Test void aplicar_reemplaza_el_cosmetico_del_mismo_tipo() {
        ElementoPersonalizacion anterior=elemento(10L,"Estratega",TipoPersonalizacion.TITULO,100);
        ElementoPersonalizacion nuevo=elemento(11L,"Rival",TipoPersonalizacion.TITULO,250);
        PersonalizacionPerfil seleccion=PersonalizacionPerfil.builder().usuario(ana).tipo(TipoPersonalizacion.TITULO).elemento(anterior).build();
        when(usuarios.findLockedByCorreo(CORREO)).thenReturn(Optional.of(ana));
        when(elementos.findById(11L)).thenReturn(Optional.of(nuevo));
        when(canjes.existsByUsuarioIdAndElementoId(7L,11L)).thenReturn(true);
        when(perfiles.findById(new PersonalizacionPerfilId(7L,TipoPersonalizacion.TITULO))).thenReturn(Optional.of(seleccion));
        respuestasVacias();

        service.aplicar(CORREO,11L);

        assertThat(seleccion.getElemento()).isEqualTo(nuevo);
        verify(perfiles).save(seleccion);
    }

    @Test void quitar_elimina_solo_la_seleccion_y_no_el_canje() {
        ElementoPersonalizacion insignia=elemento(30L,"Fénix",TipoPersonalizacion.INSIGNIA,150);
        PersonalizacionPerfil seleccion=PersonalizacionPerfil.builder().usuario(ana).tipo(TipoPersonalizacion.INSIGNIA).elemento(insignia).build();
        when(usuarios.findLockedByCorreo(CORREO)).thenReturn(Optional.of(ana));
        when(elementos.findById(30L)).thenReturn(Optional.of(insignia));
        when(perfiles.findById(new PersonalizacionPerfilId(7L,TipoPersonalizacion.INSIGNIA))).thenReturn(Optional.of(seleccion));
        respuestasVacias();

        service.quitar(CORREO,30L);

        verify(perfiles).delete(seleccion);
        verify(canjes,never()).delete(any());
    }

    private void respuestasVacias() {
        lenient().when(canjes.findByUsuarioId(7L)).thenReturn(List.of());
        lenient().when(perfiles.findByUsuarioId(7L)).thenReturn(List.of());
        lenient().when(elementos.findAllByOrderByCostoPuntosAsc()).thenReturn(List.of());
    }
    private Logro logro(Long id,String nombre,int puntos) { return Logro.builder().id(id).nombre(nombre).descripcion(nombre).puntosValor(puntos).activo(true).origen("Sistema").build(); }
    private LogroJugador registro(Logro logro) { return LogroJugador.builder().id(logro.getId()).usuario(ana).logro(logro).fechaDesbloqueo(LocalDate.now()).activo(true).referenciaSistema("AUTO").build(); }
    private ElementoPersonalizacion elemento(Long id,String nombre,TipoPersonalizacion tipo,int costo) { return ElementoPersonalizacion.builder().id(id).nombre(nombre).descripcion(nombre).tipo(tipo).costoPuntos(costo).activo(true).build(); }
}
