import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TwitchService } from '../../services/twitch.service';
import { CanalTwitch, TransmisionTwitch } from '../../../../models/twitch.model';

@Component({
  selector: 'app-twitch-panel',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './twitch-panel.component.html',
  styleUrl: './twitch-panel.component.scss'
})
export class TwitchPanelComponent implements OnInit {
  private readonly twitch = inject(TwitchService);
  canal: CanalTwitch | null = null;
  canalEntrada = 'https://www.twitch.tv/brakketcenfotec';
  torneoId: number | null = null;
  partidaId: number | null = null;
  transmision: TransmisionTwitch | null = null;
  cargando = false;
  mensaje = '';
  error = '';

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.twitch.obtener().subscribe({
      next: data => this.canal = data,
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
      },
      error: err => { this.error = this.mensajeError(err); this.cargando = false; }
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
