import { Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Torneo } from '../../../../models/tournament.model';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

/**
 * Tarjeta de torneo con la anatomía de la referencia Challenger Mode:
 * arte, fecha, nombre, organizador, formato, tamaño, slots y estado.
 */
@Component({
  selector: 'app-torneo-card',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './torneo-card.component.html',
  styleUrl: './torneo-card.component.scss'
})
export class TorneoCardComponent {
  readonly torneo = input.required<Torneo>();

  readonly arte = computed(() => {
    const t = this.torneo();
    return t.juegoImagenUrl || portadaFoto(t.juegoNombre);
  });

  readonly gradiente = computed(() => portadaGradiente(this.torneo().juegoNombre));

  readonly cupoLleno = computed(() => this.torneo().inscritos >= this.torneo().maxEquipos);

  readonly comenzo = computed(() => new Date(this.torneo().fechaInicio) <= new Date());

  readonly badge = computed(() => {
    const t = this.torneo();
    switch (t.estado) {
      case 'EN_CURSO':
        return { texto: '● En curso', tono: 'verde' };
      case 'FINALIZADO':
        return { texto: 'Finalizado', tono: 'neutro' };
      case 'CANCELADO':
        return { texto: 'Cancelado', tono: 'neutro' };
      case 'INSCRIPCION_ABIERTA':
        if (this.comenzo()) {
          return { texto: 'Comenzó', tono: 'neutro' };
        }
        if (this.cupoLleno()) {
          return { texto: 'Cupo lleno', tono: 'ambar' };
        }
        return { texto: 'Abierta', tono: 'verde' };
      default:
        return { texto: t.estado.toLowerCase(), tono: 'neutro' };
    }
  });
}
