import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CanalTwitch, MetricasTransmision, TransmisionTwitch } from '../../../../models/twitch.model';
import { TwitchService } from '../../services/twitch.service';

@Component({
  selector: 'app-twitch-panel',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe],
  templateUrl: './twitch-panel.component.html',
  styleUrl: './twitch-panel.component.scss'
})
export class TwitchPanelComponent implements OnInit {
  private readonly twitch = inject(TwitchService);
  canal: CanalTwitch | null = null;
  // Sin valor hardcodeado: se precarga con lo que responda el backend (que a
  // su vez cae a la env var TWITCH_CHANNEL si no hay canal en BD).
  canalEntrada = '';
  torneoId: number | null = null;
  partidaId: number | null = null;
  transmision: TransmisionTwitch | null = null;
  metricas: MetricasTransmision | null = null;
  cargando = false;
  confirmandoFinalizar = false;
  mensaje = '';
  error = '';

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.twitch.obtener().subscribe({
      next: data => {
        this.canal = data;
        if (!this.canalEntrada) {
          this.canalEntrada = data.urlCanal ?? '';
        }
      },
      error: err => this.error = this.mensajeError(err)
    });
    this.cargarTransmisionAbierta();
  }

  /**
   * Recupera la transmisión abierta al entrar a la pantalla. Sin esto el panel
   * solo conocía la que acababa de asociar, y al refrescar el navegador perdía
   * el estado: la única acción posible era volver a asociar, que choca contra
   * el índice único del stream de Twitch.
   */
  cargarTransmisionAbierta(): void {
    this.twitch.abiertas().subscribe({
      next: lista => {
        if (lista.length > 0) {
          this.transmision = lista[0];
          this.cargarMetricas();
        }
      },
      error: err => this.error = this.mensajeError(err)
    });
  }

  guardar(): void {
    this.ejecutar(this.twitch.configurar(this.canalEntrada), 'Canal guardado.');
  }

  validar(): void {
    this.ejecutar(this.twitch.validar(), 'Conexión validada correctamente.');
  }

  asociar(): void {
    this.limpiar();
    if (!this.torneoId && !this.partidaId) {
      this.error = 'Indique el ID de un torneo o una partida.';
      return;
    }
    this.cargando = true;
    this.twitch.asociar(this.torneoId, this.partidaId).subscribe({
      next: data => {
        this.transmision = data;
        this.mensaje = data.estado === 'EN_VIVO'
          ? 'Transmisión en vivo asociada.'
          : 'Asociación guardada; el canal no está transmitiendo en vivo.';
        this.cargando = false;
        this.cargarMetricas();
      },
      error: err => { this.error = this.mensajeError(err); this.cargando = false; }
    });
  }

  /**
   * RF-34: cierra el período de captura. Se pide confirmación en dos pasos
   * porque no hay vuelta atrás: una vez cerrada, el muestreo deja de escribir
   * y para volver a capturar hay que asociar una transmisión nueva.
   */
  pedirConfirmacion(): void {
    this.limpiar();
    this.confirmandoFinalizar = true;
  }

  cancelarFinalizar(): void {
    this.confirmandoFinalizar = false;
  }

  finalizar(): void {
    if (!this.transmision) {
      return;
    }
    this.limpiar();
    this.cargando = true;
    this.twitch.finalizar(this.transmision.id).subscribe({
      next: data => {
        this.transmision = data;
        this.confirmandoFinalizar = false;
        this.mensaje = 'Transmisión finalizada. Las métricas capturadas se conservan.';
        this.cargando = false;
        this.cargarMetricas();
      },
      error: err => {
        this.error = this.mensajeError(err);
        this.confirmandoFinalizar = false;
        this.cargando = false;
      }
    });
  }

  /** RF-36: indicadores capturados por el muestreo para la transmisión asociada. */
  cargarMetricas(): void {
    if (!this.transmision) {
      return;
    }
    this.twitch.metricas(this.transmision.id).subscribe({
      next: data => this.metricas = data,
      error: err => this.error = this.mensajeError(err)
    });
  }

  private ejecutar(request: ReturnType<TwitchService['validar']>, exito: string): void {
    this.limpiar();
    this.cargando = true;
    request.subscribe({
      next: data => { this.canal = data; this.mensaje = exito; this.cargando = false; },
      error: err => { this.error = this.mensajeError(err); this.cargando = false; }
    });
  }
  private limpiar(): void { this.mensaje = ''; this.error = ''; }
  private mensajeError(err: HttpErrorResponse): string {
    return err.error?.message ?? 'No fue posible completar la operación.';
  }
}
