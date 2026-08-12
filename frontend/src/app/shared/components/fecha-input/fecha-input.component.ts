import {
  Component,
  ElementRef,
  HostListener,
  computed,
  forwardRef,
  inject,
  input,
  signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Una celda de la cuadrícula del mes. */
interface Celda {
  /** Valor ISO local (YYYY-MM-DD) que representa la celda. */
  iso: string;
  dia: number;
  /** Relleno del mes anterior o siguiente: se pinta apagado. */
  otroMes: boolean;
  esHoy: boolean;
  deshabilitada: boolean;
}

const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
];

const MESES_CORTOS = [
  'ene', 'feb', 'mar', 'abr', 'may', 'jun',
  'jul', 'ago', 'sep', 'oct', 'nov', 'dic'
];

/** La semana arranca en lunes, como se lee un calendario en español. */
const DIAS_SEMANA = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'];

/** Hora por defecto al elegir día en un campo con hora, si no había valor. */
const HORA_POR_DEFECTO = '12:00';

/**
 * Campo de fecha con calendario propio, en lugar del selector nativo del
 * navegador (que ignora la paleta de la app y cambia de aspecto según el
 * sistema operativo).
 *
 * Se usa con formControlName o ngModel, igual que un input nativo, y el valor
 * que entrega mantiene el mismo formato que estos: `YYYY-MM-DD`, o
 * `YYYY-MM-DDTHH:mm` cuando `conHora` está activo. Así ningún servicio ni el
 * backend tuvieron que cambiar al reemplazar los inputs.
 *
 * Las fechas se parsean y formatean a mano, nunca con `new Date('2026-08-11')`:
 * ese constructor interpreta la cadena como UTC y en Costa Rica (-06:00)
 * devuelve el día anterior.
 */
@Component({
  selector: 'app-fecha-input',
  standalone: true,
  templateUrl: './fecha-input.component.html',
  styleUrl: './fecha-input.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FechaInputComponent),
      multi: true
    }
  ]
})
export class FechaInputComponent implements ControlValueAccessor {
  private readonly elemento = inject(ElementRef<HTMLElement>);

  /** Añade selección de hora y cambia el formato del valor a YYYY-MM-DDTHH:mm. */
  readonly conHora = input(false);
  /** Límites opcionales, en el mismo formato ISO local del valor. */
  readonly min = input<string | null>(null);
  readonly max = input<string | null>(null);
  /** Texto del campo cuando está vacío. */
  readonly marcador = input('Seleccionar fecha');
  /** Nombra el campo para lectores de pantalla. */
  readonly etiqueta = input('Fecha');
  /** id del disparador, para poder enlazarlo desde un <label for>. */
  readonly idCampo = input<string | null>(null);
  /**
   * Muestra el calendario fijo en la página, sin campo que lo despliegue.
   *
   * Es para contenedores que recortan: el modal del wizard de torneos tiene
   * `overflow-y: auto` y `transform`, y ese `transform` lo convierte en el
   * bloque contenedor de sus descendientes, así que ni `position: fixed`
   * escapa de su recorte. Un panel flotante ahí sale cortado siempre.
   */
  readonly incrustado = input(false);

  readonly valor = signal<string>('');
  readonly abierto = signal(false);
  readonly deshabilitado = signal(false);

  /** Mes que muestra la cuadrícula: {anio, mes} con mes 0-11. */
  private readonly vista = signal(this.mesDeHoy());

  readonly diasSemana = DIAS_SEMANA;
  readonly horas = Array.from({ length: 24 }, (_, i) => this.dosDigitos(i));
  readonly minutos = Array.from({ length: 12 }, (_, i) => this.dosDigitos(i * 5));

  private alCambiar: (valor: string) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  /** Incrustado el calendario está siempre a la vista. */
  readonly panelVisible = computed(() => this.incrustado() || this.abierto());

  /** Parte de fecha del valor (YYYY-MM-DD), o '' si no hay valor. */
  readonly fechaSeleccionada = computed(() => this.valor().slice(0, 10));

  /** Parte de hora del valor (HH:mm); '' si el campo no lleva hora. */
  readonly horaSeleccionada = computed(() => {
    const v = this.valor();
    return this.conHora() && v.length >= 16 ? v.slice(11, 16) : '';
  });

  // Sin valor aún, los selectores muestran la hora que se aplicará al elegir
  // día. Si mostraran 00:00 y al hacer clic saltara a las 12:00, el cambio
  // parecería un error.
  readonly horaActual = computed(() => (this.horaSeleccionada() || HORA_POR_DEFECTO).slice(0, 2));
  readonly minutoActual = computed(() => (this.horaSeleccionada() || HORA_POR_DEFECTO).slice(3, 5));

  /** Encabezado del calendario: "agosto 2026". */
  readonly tituloVista = computed(() => {
    const { anio, mes } = this.vista();
    return `${MESES[mes]} ${anio}`;
  });

  /** Texto del campo cerrado: "11 ago 2026" o "11 ago 2026, 14:45". */
  readonly textoValor = computed(() => {
    const fecha = this.fechaSeleccionada();
    if (!fecha) {
      return '';
    }
    const [anio, mes, dia] = fecha.split('-').map(Number);
    const base = `${dia} ${MESES_CORTOS[mes - 1]} ${anio}`;
    const hora = this.horaSeleccionada();
    return hora ? `${base}, ${hora}` : base;
  });

  /**
   * Cuadrícula de 6 semanas. Siempre 42 celdas para que el alto del panel no
   * salte al cambiar de mes.
   */
  readonly celdas = computed<Celda[]>(() => {
    const { anio, mes } = this.vista();
    const hoy = this.isoDe(new Date());
    const primero = new Date(anio, mes, 1);
    // getDay() devuelve 0 para domingo; con la semana en lunes, domingo es 6.
    const desplazamiento = (primero.getDay() + 6) % 7;

    return Array.from({ length: 42 }, (_, i) => {
      const fecha = new Date(anio, mes, 1 - desplazamiento + i);
      const iso = this.isoDe(fecha);
      return {
        iso,
        dia: fecha.getDate(),
        otroMes: fecha.getMonth() !== mes,
        esHoy: iso === hoy,
        deshabilitada: this.fueraDeRango(iso)
      };
    });
  });

  // ---------- ControlValueAccessor ----------

  writeValue(valor: string | null): void {
    this.valor.set(valor ?? '');
    this.situarVista();
  }

  registerOnChange(fn: (valor: string) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
    if (deshabilitado) {
      this.abierto.set(false);
    }
  }

  // ---------- interacción ----------

  alternar(): void {
    if (this.deshabilitado()) {
      return;
    }
    const abriendo = !this.abierto();
    if (abriendo) {
      this.situarVista();
    }
    this.abierto.set(abriendo);
    if (!abriendo) {
      this.alTocar();
    }
  }

  cerrar(): void {
    if (this.abierto()) {
      this.abierto.set(false);
      this.alTocar();
    }
  }

  mesAnterior(): void {
    const { anio, mes } = this.vista();
    this.vista.set(mes === 0 ? { anio: anio - 1, mes: 11 } : { anio, mes: mes - 1 });
  }

  mesSiguiente(): void {
    const { anio, mes } = this.vista();
    this.vista.set(mes === 11 ? { anio: anio + 1, mes: 0 } : { anio, mes: mes + 1 });
  }

  elegir(celda: Celda): void {
    if (celda.deshabilitada) {
      return;
    }
    if (this.conHora()) {
      // Conserva la hora ya elegida; si no había, una neutral del mediodía.
      const hora = this.horaSeleccionada() || HORA_POR_DEFECTO;
      this.emitir(`${celda.iso}T${hora}`);
      // Con hora el panel sigue abierto: falta elegirla.
      return;
    }
    this.emitir(celda.iso);
    if (!this.incrustado()) {
      this.abierto.set(false);
    }
    this.alTocar();
  }

  cambiarHora(hora: string): void {
    const fecha = this.fechaSeleccionada() || this.isoDe(new Date());
    const minuto = this.minutoActual() || '00';
    this.emitir(`${fecha}T${hora}:${minuto}`);
  }

  cambiarMinuto(minuto: string): void {
    const fecha = this.fechaSeleccionada() || this.isoDe(new Date());
    const hora = this.horaActual() || '12';
    this.emitir(`${fecha}T${hora}:${minuto}`);
  }

  hoy(): void {
    const iso = this.isoDe(new Date());
    if (this.fueraDeRango(iso)) {
      return;
    }
    this.elegir({ iso, dia: 0, otroMes: false, esHoy: true, deshabilitada: false });
  }

  limpiar(): void {
    this.emitir('');
    this.abierto.set(false);
    this.alTocar();
  }

  /** Cierra al hacer clic fuera; sin esto el panel queda colgado en pantalla. */
  @HostListener('document:pointerdown', ['$event'])
  alClicFuera(evento: PointerEvent): void {
    if (this.abierto() && !this.elemento.nativeElement.contains(evento.target as Node)) {
      this.cerrar();
    }
  }

  @HostListener('keydown.escape')
  alEscape(): void {
    this.cerrar();
  }

  // ---------- utilidades ----------

  private emitir(valor: string): void {
    this.valor.set(valor);
    this.alCambiar(valor);
  }

  /** Lleva la cuadrícula al mes del valor actual, o al de hoy si está vacío. */
  private situarVista(): void {
    const fecha = this.fechaSeleccionada();
    if (!fecha) {
      this.vista.set(this.mesDeHoy());
      return;
    }
    const [anio, mes] = fecha.split('-').map(Number);
    this.vista.set({ anio, mes: mes - 1 });
  }

  private mesDeHoy(): { anio: number; mes: number } {
    const hoy = new Date();
    return { anio: hoy.getFullYear(), mes: hoy.getMonth() };
  }

  /** Fecha local a YYYY-MM-DD, sin pasar por UTC. */
  private isoDe(fecha: Date): string {
    return [
      fecha.getFullYear(),
      this.dosDigitos(fecha.getMonth() + 1),
      this.dosDigitos(fecha.getDate())
    ].join('-');
  }

  private fueraDeRango(iso: string): boolean {
    const min = this.min()?.slice(0, 10);
    const max = this.max()?.slice(0, 10);
    // Comparación de cadenas: en formato ISO el orden lexicográfico coincide
    // con el cronológico.
    return (!!min && iso < min) || (!!max && iso > max);
  }

  private dosDigitos(n: number): string {
    return String(n).padStart(2, '0');
  }
}
