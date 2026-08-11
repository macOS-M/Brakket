import { Component, DestroyRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, filter, fromEvent, merge, switchMap, timer } from 'rxjs';

import { TransmisionesService } from '../../services/transmisiones.service';
import {
  TarjetaTransmision,
  Transmision,
  TransmisionesRespuesta
} from '../../../../models/transmision.model';
import { DestacadoHeroComponent } from '../../components/destacado-hero/destacado-hero.component';
import { StreamCardComponent } from '../../components/stream-card/stream-card.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { AdSlotComponent } from '../../../../shared/components/ad-slot/ad-slot.component';

/** Cada cuánto se refresca el estado en vivo. Con el caché backend de 25s el
 *  peor caso de desfase queda en ~55s, dentro del minuto de RNF-02. */
const INTERVALO_REFRESCO_MS = 30_000;

/** Huecos mínimos de la grilla principal; se rellenan con "próximamente". */
const TARJETAS_MINIMAS = 4;

interface SeccionGrid {
  titulo: string;
  tarjetas: TarjetaTransmision[];
}

/**
 * Página /transmisiones (RF-35): carrusel destacado arriba y grillas por
 * sección debajo, inspirado en la home de Twitch. Estados: cargando
 * (skeletons), con datos, vacío, error y degradado (Twitch sin responder).
 */
@Component({
  selector: 'app-transmisiones-page',
  standalone: true,
  imports: [DatePipe, DestacadoHeroComponent, StreamCardComponent, PageHeaderComponent, EmptyStateComponent, AdSlotComponent],
  templateUrl: './transmisiones-page.component.html',
  styleUrl: './transmisiones-page.component.scss'
})
export class TransmisionesPageComponent implements OnInit {
  private readonly servicio = inject(TransmisionesService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild(DestacadoHeroComponent) private hero?: DestacadoHeroComponent;

  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly respuesta = signal<TransmisionesRespuesta | null>(null);

  readonly transmisiones = computed(() => this.respuesta()?.transmisiones ?? []);
  readonly degradado = computed(() => this.respuesta()?.degradado ?? false);
  readonly actualizadoEn = computed(() => this.respuesta()?.actualizadoEn ?? null);

  /** El hero muestra las destacadas; si ninguna lo es, todas las reales. */
  readonly destacadas = computed<Transmision[]>(() => {
    const marcadas = this.transmisiones().filter((t) => t.destacada);
    return marcadas.length > 0 ? marcadas : this.transmisiones();
  });

  /** Grilla principal: canales reales (en vivo primero) + relleno. */
  readonly seccionCanales = computed<SeccionGrid>(() => {
    const reales: TarjetaTransmision[] = [...this.transmisiones()]
      .sort((a, b) => Number(b.estado === 'EN_VIVO') - Number(a.estado === 'EN_VIVO'))
      .map((t) => ({ tipo: 'real', transmision: t }));
    const relleno = Math.max(0, TARJETAS_MINIMAS - reales.length);
    return {
      titulo: 'Canales en vivo',
      tarjetas: [...reales, ...Array.from({ length: relleno }, () => ({ tipo: 'proximamente' as const }))]
    };
  });

  /** Secciones agrupadas desde la misma estructura de datos. */
  readonly seccionesPorJuego = computed<SeccionGrid[]>(() =>
    this.agrupar((t) => t.categoria)
  );
  readonly seccionesPorTorneo = computed<SeccionGrid[]>(() =>
    this.agrupar((t) => (t.nombreTorneo ? `Torneo: ${t.nombreTorneo}` : null))
  );

  ngOnInit(): void {
    // La primera carga corre SIEMPRE (aunque la pestaña esté de fondo: si no,
    // el usuario vuelve y encuentra skeletons); el polling posterior se pausa
    // con la pestaña oculta para no gastar la cuota de Helix, y al volver a
    // la pestaña se refresca de inmediato en vez de esperar al próximo tick.
    merge(
      timer(0, INTERVALO_REFRESCO_MS).pipe(filter((tick) => tick === 0 || !document.hidden)),
      fromEvent(document, 'visibilitychange').pipe(filter(() => !document.hidden))
    )
      .pipe(
        switchMap(() =>
          this.servicio.listar().pipe(
            catchError(() => {
              // Sin datos previos es un error de página; con datos, se
              // conserva lo último y el próximo tick reintenta.
              if (!this.respuesta()) {
                this.error.set(true);
                this.cargando.set(false);
              }
              return EMPTY;
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((respuesta) => this.aplicar(respuesta));
  }

  reintentar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.servicio.listar().subscribe({
      next: (respuesta) => this.aplicar(respuesta),
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      }
    });
  }

  seleccionarEnHero(transmision: Transmision): void {
    this.hero?.seleccionar(transmision);
    document.querySelector('app-destacado-hero')?.scrollIntoView({
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
      block: 'start'
    });
  }

  private aplicar(respuesta: TransmisionesRespuesta): void {
    this.respuesta.set(respuesta);
    this.cargando.set(false);
    this.error.set(false);
  }

  private agrupar(clave: (t: Transmision) => string | null): SeccionGrid[] {
    const grupos = new Map<string, TarjetaTransmision[]>();
    for (const t of this.transmisiones()) {
      const titulo = clave(t);
      if (!titulo) {
        continue;
      }
      const tarjetas = grupos.get(titulo) ?? [];
      tarjetas.push({ tipo: 'real', transmision: t });
      grupos.set(titulo, tarjetas);
    }
    return Array.from(grupos, ([titulo, tarjetas]) => ({ titulo, tarjetas }));
  }
}
