import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/**
 * Navegacion lateral con acceso a las distintas features del producto.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  readonly enlaces = [
    { ruta: '/inicio', etiqueta: 'Inicio', exact: true },
    { ruta: '/teams', etiqueta: 'Equipos', exact: false },
    { ruta: '/transfers', etiqueta: 'Transferencias', exact: false },
    { ruta: '/games', etiqueta: 'Juegos', exact: false },
    { ruta: '/leagues', etiqueta: 'Ligas', exact: false },
    { ruta: '/tournaments', etiqueta: 'Torneos', exact: false },
    { ruta: '/disputes', etiqueta: 'Disputas', exact: false },
    { ruta: '/twitch', etiqueta: 'Twitch', exact: false },
    { ruta: '/analytics', etiqueta: 'Analitica', exact: false },
    { ruta: '/sponsorships', etiqueta: 'Patrocinios', exact: false },
    { ruta: '/notifications', etiqueta: 'Notificaciones', exact: false },
    { ruta: '/statistics', etiqueta: 'Estadisticas', exact: false },
    { ruta: '/progression', etiqueta: 'Progresion', exact: false },
    { ruta: '/admin', etiqueta: 'Admin', exact: false }
  ];
}
