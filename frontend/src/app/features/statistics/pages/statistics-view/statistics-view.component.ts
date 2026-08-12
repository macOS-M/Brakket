import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, catchError, debounceTime, distinctUntilChanged, finalize, of, switchMap } from 'rxjs';
import { GamesService } from '../../../games/services/games.service';
import { Juego } from '../../../../models/juego.model';
import { CatalogoEstadisticas, EstadisticasHistoricas, OpcionEstadistica, StatisticsService } from '../../services/statistics.service';
import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';

@Component({ selector: 'app-statistics-view', standalone: true, imports: [CommonModule, ReactiveFormsModule, FechaInputComponent], templateUrl: './statistics-view.component.html', styleUrl: './statistics-view.component.scss' })
export class StatisticsViewComponent implements OnInit {
  private readonly fb = inject(FormBuilder); private readonly statistics = inject(StatisticsService); private readonly games = inject(GamesService);
  readonly catalogo = signal<CatalogoEstadisticas>({ ligas: [], temporadas: [] }); readonly juegos = signal<Juego[]>([]);
  readonly resultado = signal<EstadisticasHistoricas|null>(null); readonly cargando = signal(false); readonly error = signal('');
  readonly sugerencias = signal<OpcionEstadistica[]>([]); readonly buscandoSujetos = signal(false);
  readonly dropdownAbierto = signal(false); readonly busquedaRealizada = signal(false);
  readonly sujetoSeleccionado = signal<OpcionEstadistica|null>(null);
  private readonly busqueda$ = new Subject<string>();
  readonly form = this.fb.group({ tipo: this.fb.nonNullable.control<'JUGADOR'|'EQUIPO'>('JUGADOR'), busqueda: this.fb.nonNullable.control(''), sujetoId: this.fb.control<number|null>(null, Validators.required), juegoId: this.fb.control<number|null>(null), ligaId: this.fb.control<number|null>(null), temporadaId: this.fb.control<number|null>(null), desde: this.fb.nonNullable.control(''), hasta: this.fb.nonNullable.control('') });
  ngOnInit(): void {
    this.statistics.catalogo().subscribe({ next: c => this.catalogo.set(c), error: () => this.error.set('No se pudo cargar el catálogo de participantes.') });
    this.games.listActivos().subscribe({ next: j => this.juegos.set(j) });
    this.form.controls.tipo.valueChanges.subscribe(() => this.reiniciarSujeto());
    this.form.controls.juegoId.valueChanges.subscribe(() => { this.reiniciarSujeto(); });
    this.busqueda$.pipe(debounceTime(300), distinctUntilChanged(), switchMap(texto => {
      this.buscandoSujetos.set(true);
      return this.statistics.buscarSujetos(this.form.controls.tipo.value!, texto,
        this.form.controls.juegoId.value ?? undefined).pipe(
          catchError(() => of({ items: [], pagina: 0, tamano: 5, total: 0 })),
          finalize(() => this.buscandoSujetos.set(false)));
    })).subscribe(pagina => {
      this.sugerencias.set(pagina.items.slice(0, 5)); this.busquedaRealizada.set(true);
      this.dropdownAbierto.set(true);
    });
  }
  buscarSujeto(event: Event): void {
    const texto = (event.target as HTMLInputElement).value;
    this.form.controls.sujetoId.setValue(null); this.sujetoSeleccionado.set(null);
    this.busquedaRealizada.set(false); this.dropdownAbierto.set(texto.trim().length >= 2);
    if (texto.trim().length < 2) { this.sugerencias.set([]); return; }
    this.busqueda$.next(texto.trim());
  }
  seleccionarSujeto(opcion: OpcionEstadistica, input: HTMLInputElement): void {
    this.form.controls.sujetoId.setValue(opcion.id); this.sujetoSeleccionado.set(opcion);
    this.form.controls.busqueda.setValue(opcion.nombre); input.value = opcion.nombre;
    this.sugerencias.set([]); this.dropdownAbierto.set(false);
  }
  abrirDropdown(): void { if (this.form.controls.busqueda.value.trim().length >= 2) this.dropdownAbierto.set(true); }
  cerrarDropdown(): void { setTimeout(() => this.dropdownAbierto.set(false), 150); }
  private reiniciarSujeto(): void { this.form.controls.busqueda.setValue(''); this.form.controls.sujetoId.setValue(null); this.sujetoSeleccionado.set(null); this.sugerencias.set([]); this.dropdownAbierto.set(false); this.resultado.set(null); }
  consultar(): void {
    if (this.form.invalid || this.form.controls.sujetoId.value === null) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    if (raw.desde && raw.hasta && raw.desde > raw.hasta) { this.error.set('La fecha inicial no puede ser posterior a la fecha final.'); return; }
    this.cargando.set(true); this.error.set(''); this.resultado.set(null);
    this.statistics.consultar({ ...(raw.tipo === 'JUGADOR' ? { jugadorId: raw.sujetoId! } : { equipoId: raw.sujetoId! }), juegoId: raw.juegoId ?? undefined, ligaId: raw.ligaId ?? undefined, temporadaId: raw.temporadaId ?? undefined, desde: raw.desde || undefined, hasta: raw.hasta || undefined })
      .pipe(finalize(() => this.cargando.set(false))).subscribe({ next: r => this.resultado.set(r), error: e => this.error.set(e?.error?.message ?? 'No fue posible calcular las estadísticas.') });
  }
  limpiar(): void { const tipo = this.form.controls.tipo.value; this.form.reset({ tipo, busqueda: '', sujetoId: null, juegoId: null, ligaId: null, temporadaId: null, desde: '', hasta: '' }); this.sujetoSeleccionado.set(null); this.sugerencias.set([]); this.resultado.set(null); this.error.set(''); }
}
