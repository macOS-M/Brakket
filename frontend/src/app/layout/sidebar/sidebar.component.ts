import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

export interface EnlaceNav {
  ruta: string;
  etiqueta: string;
  icono: string;
  exact: boolean;
  /** La pantalla sigue siendo un placeholder; se marca para no prometer de más. */
  proximamente?: boolean;
  /** Solo visible para quien tenga alguno de estos roles. */
  roles?: string[];
  /**
   * Oculto para quien tenga alguno de estos roles. Se usa para limpiar el
   * portal del patrocinador (ACT-06), que no participa de la competencia.
   * Nota: es una denylist simple; una cuenta que combine PATROCINADOR con
   * otro rol pierde el enlace igual. Para la demo alcanza.
   */
  ocultarPara?: string[];
}

export interface GrupoNav {
  titulo: string | null;
  enlaces: EnlaceNav[];
}

/**
 * Navegacion lateral. Agrupa las secciones por area y marca las que
 * todavia no tienen backend, para que la demo no parezca rota.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);
  private readonly themeService = inject(ThemeService);

  readonly abiertoEnMovil = signal(false);
  readonly tema = this.themeService.tema;

  /** Etiqueta accesible del control de tema, según el estado actual. */
  readonly etiquetaTema = computed(() => {
    switch (this.tema()) {
      case 'claro':
        return 'Tema claro. Cambiar a oscuro';
      case 'oscuro':
        return 'Tema oscuro. Cambiar a automático';
      default:
        return 'Tema automático. Cambiar a claro';
    }
  });

  private readonly grupos: GrupoNav[] = [
    {
      titulo: null,
      // El dashboard vive en /inicio: la raíz es el landing institucional.
      enlaces: [{ ruta: '/inicio', etiqueta: 'Inicio', icono: 'home', exact: true }]
    },
    {
      titulo: 'Competencia',
      enlaces: [
        { ruta: '/leagues', etiqueta: 'Ligas', icono: 'trophy', exact: false },
        {
          ruta: '/tournaments',
          etiqueta: 'Torneos',
          icono: 'swords',
          exact: false
        },
        { ruta: '/games', etiqueta: 'Juegos', icono: 'gamepad', exact: false },
        { ruta: '/calendar', etiqueta: 'Calendario', icono: 'calendar', exact: false }
      ]
    },
    {
      titulo: 'Mi equipo',
      enlaces: [
        {
          ruta: '/teams',
          etiqueta: 'Equipos',
          icono: 'users',
          exact: false,
          ocultarPara: ['PATROCINADOR']
        },
        {
          ruta: '/transfers',
          etiqueta: 'Transferencias',
          icono: 'exchange',
          exact: false,
          ocultarPara: ['PATROCINADOR']
        },
        {
          ruta: '/disputes',
          etiqueta: 'Disputas',
          icono: 'shield',
          exact: false,
          proximamente: false,
          ocultarPara: ['PATROCINADOR']
        }
      ]
    },
    {
      titulo: 'Seguimiento',
      enlaces: [
        {
          ruta: '/statistics',
          etiqueta: 'Estadísticas',
          icono: 'chart',
          exact: false,
          ocultarPara: ['PATROCINADOR']
        },
        {
          ruta: '/progression',
          etiqueta: 'Progresión',
          icono: 'star',
          exact: false,
          proximamente: true,
          ocultarPara: ['PATROCINADOR']
        },
        {
          ruta: '/notifications',
          etiqueta: 'Notificaciones',
          icono: 'bell',
          exact: false
        },
        {
          ruta: '/transmisiones',
          etiqueta: 'Transmisiones',
          icono: 'video',
          exact: false
        }
      ]
    },
    {
      // Portal del patrocinador (ACT-06): el panel comercial de RF-44 y el
      // termómetro de sentimiento de RF-40, que son las dos vistas que puede
      // consultar. El panel vive en la raíz y no bajo /sponsorships para no
      // compartir prefijo con "Gestión > Patrocinios", que marcaría los dos
      // enlaces como activos a la vez.
      titulo: 'Mi marca',
      enlaces: [
        {
          ruta: '/panel-comercial',
          etiqueta: 'Panel comercial',
          icono: 'briefcase',
          exact: false,
          roles: ['PATROCINADOR']
        },
        {
          ruta: '/analytics/termometro',
          etiqueta: 'Termómetro del chat',
          icono: 'pulse',
          exact: false,
          proximamente: false,
          roles: ['PATROCINADOR']
        }
      ]
    },
    {
      // Gestión de la competencia y de la plataforma. Los patrocinios los
      // gestionan administrador y comisionado (RF-42/RF-43 de la ERS).
      titulo: 'Gestión',
      enlaces: [
        {
          ruta: '/sponsorships',
          etiqueta: 'Patrocinios',
          icono: 'briefcase',
          exact: false,
          proximamente: false,
          roles: ['ADMIN', 'COMISIONADO']
        },

        {
          // RF-37: consulta de métricas de transmisión por período. Ruta propia
          // porque /analytics quedó para el sentimiento y el termómetro.
          ruta: '/metricas',
          etiqueta: 'Analítica',
          icono: 'chart',
          exact: false,
          roles: ['ADMIN', 'COMISIONADO']
        },
        {
          // RF-40. El panel de análisis manual de RF-39 vive en /analytics y es
          // solo de administración; el termómetro lo consultan tambien los
          // comisionados, asi que es este el que va al menu.
          ruta: '/analytics/termometro',
          etiqueta: 'Termómetro del chat',
          icono: 'pulse',
          exact: false,
          proximamente: false,
          roles: ['ADMIN', 'COMISIONADO']
        },
        {
          // Configuración del canal oficial (RF-34); la vitrina pública
          // de directos vive en /transmisiones.
          ruta: '/twitch',
          etiqueta: 'Canal de Twitch',
          icono: 'video',
          exact: false,
          roles: ['ADMIN']
        },
        {
          ruta: '/admin',
          etiqueta: 'Administración',
          icono: 'settings',
          exact: false,
          roles: ['ADMIN']
        }
      ]
    }
  ];

  /** Oculta los enlaces cuyo rol el usuario no tiene. */
  readonly gruposVisibles = computed<GrupoNav[]>(() =>
    this.grupos
      .map((grupo) => ({
        ...grupo,
        enlaces: grupo.enlaces.filter((enlace) => this.puedeVer(enlace))
      }))
      .filter((grupo) => grupo.enlaces.length > 0)
  );

  alternarMenu(): void {
    this.abiertoEnMovil.update((abierto) => !abierto);
  }

  cerrarMenu(): void {
    this.abiertoEnMovil.set(false);
  }

  alternarTema(): void {
    this.themeService.alternar();
  }

  private puedeVer(enlace: EnlaceNav): boolean {
    const roles = this.authService.roles();
    if (enlace.roles && !enlace.roles.some((rol) => roles.includes(rol))) {
      return false;
    }
    if (enlace.ocultarPara && enlace.ocultarPara.some((rol) => roles.includes(rol))) {
      return false;
    }
    return true;
  }
}
