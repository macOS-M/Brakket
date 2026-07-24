import { Pipe, PipeTransform } from '@angular/core';

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

/**
 * Fecha con urgencia, como la referencia Challenger Mode: "En 16 minutos",
 * "Hoy, 19:00", "Mañana, 00:45", "viernes, 13:00", "24/8, 16:00". Para
 * fechas pasadas: "Comenzó hace 2 h". Hace que el panel se sienta vivo.
 *
 * <p>Impuro a propósito: el resultado depende del reloj, no solo del input.
 * Un pipe puro cachearía "En 16 minutos" para siempre en una pantalla que
 * queda abierta (demo 24/7). El cálculo es aritmética barata por CD.</p>
 */
@Pipe({ name: 'fechaRelativa', standalone: true, pure: false })
export class FechaRelativaPipe implements PipeTransform {
  transform(valor: string | Date | null | undefined): string {
    if (!valor) {
      return '';
    }
    const fecha = new Date(valor);
    const ahora = new Date();
    const diffMs = fecha.getTime() - ahora.getTime();
    const diffMin = Math.round(diffMs / 60000);
    const hora = `${fecha.getHours().toString().padStart(2, '0')}:${fecha.getMinutes().toString().padStart(2, '0')}`;

    if (diffMin < 0) {
      const min = -diffMin;
      if (min < 60) {
        return `Comenzó hace ${min} min`;
      }
      if (min < 60 * 24) {
        return `Comenzó hace ${Math.round(min / 60)} h`;
      }
      return `Comenzó hace ${Math.round(min / (60 * 24))} día(s)`;
    }
    if (diffMin < 1) {
      return 'Ahora';
    }
    if (diffMin < 60) {
      return `En ${diffMin} minutos`;
    }

    const esMismoDia = fecha.toDateString() === ahora.toDateString();
    if (esMismoDia) {
      return `Hoy, ${hora}`;
    }
    const manana = new Date(ahora);
    manana.setDate(ahora.getDate() + 1);
    if (fecha.toDateString() === manana.toDateString()) {
      return `Mañana, ${hora}`;
    }
    // Hasta 6 días: con 7 el mismo día de semana reaparece y "martes, 19:00"
    // se leería como hoy.
    if (diffMin < 60 * 24 * 6) {
      return `${DIAS[fecha.getDay()]}, ${hora}`;
    }
    return `${fecha.getDate()}/${fecha.getMonth() + 1}, ${hora}`;
  }
}
