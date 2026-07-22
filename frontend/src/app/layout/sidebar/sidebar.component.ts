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
      enlaces: [{ ruta: '/', etiqueta: 'Inicio', icono: 'home', exact: true }]
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
        { ruta: '/games', etiqueta: 'Juegos', icono: 'gamepad', exact: false }
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
          proximamente: true,
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
          proximamente: true,
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
          exact: false,
          proximamente: true
        },
        {
          ruta: '/twitch',
          etiqueta: 'Transmisiones',
          icono: 'video',
          exact: false,
          proximamente: true
        }
      ]
    },
    {
      // Portal del patrocinador (ACT-06): su panel comercial de métricas
      // de audiencia y sentimiento (RF-44). Llega con EPIC-11 (sprint 6).
      titulo: 'Mi marca',
      enlaces: [
        {
          ruta: '/analytics',
          etiqueta: 'Panel comercial',
          icono: 'briefcase',
          exact: false,
          proximamente: true,
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
          proximamente: true,
          roles: ['ADMIN', 'COMISIONADO']
        },
        {
          ruta: '/analytics',
          etiqueta: 'Analítica',
          icono: 'pulse',
          exact: false,
          proximamente: true,
          roles: ['ADMIN', 'COMISIONADO']
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
