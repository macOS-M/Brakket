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

  readonly usuario = this.authService.usuario;
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
          exact: false,
          proximamente: true
        },
        { ruta: '/games', etiqueta: 'Juegos', icono: 'gamepad', exact: false }
      ]
    },
    {
      titulo: 'Mi equipo',
      enlaces: [
        { ruta: '/teams', etiqueta: 'Equipos', icono: 'users', exact: false },
        { ruta: '/transfers', etiqueta: 'Transferencias', icono: 'exchange', exact: false },
        {
          ruta: '/disputes',
          etiqueta: 'Disputas',
          icono: 'shield',
          exact: false,
          proximamente: true
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
          proximamente: true
        },
        {
          ruta: '/progression',
          etiqueta: 'Progresión',
          icono: 'star',
          exact: false,
          proximamente: true
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
      titulo: 'Gestión',
      enlaces: [
        {
          ruta: '/sponsorships',
          etiqueta: 'Patrocinios',
          icono: 'briefcase',
          exact: false,
          proximamente: true
        },
        {
          ruta: '/analytics',
          etiqueta: 'Analítica',
          icono: 'pulse',
          exact: false,
          proximamente: true
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

  readonly iniciales = computed(() => {
    const nombre = this.usuario()?.nombre?.trim();
    if (!nombre) {
      return '?';
    }
    return nombre
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte.charAt(0).toUpperCase())
      .join('');
  });

  alternarMenu(): void {
    this.abiertoEnMovil.update((abierto) => !abierto);
  }

  cerrarMenu(): void {
    this.abiertoEnMovil.set(false);
  }

  alternarTema(): void {
    this.themeService.alternar();
  }

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }

  private puedeVer(enlace: EnlaceNav): boolean {
    if (!enlace.roles) {
      return true;
    }
    const roles = this.authService.roles();
    return enlace.roles.some((rol) => roles.includes(rol));
  }
}
