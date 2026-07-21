import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { FooterComponent } from './footer/footer.component';
import { SidebarComponent } from './sidebar/sidebar.component';

/**
 * Shell principal de la aplicacion: navegacion lateral, area de contenido
 * y pie de pagina.
 *
 * No hay cabecera superior: la marca y las acciones de sesion viven en el
 * sidebar (segun el diseno), y cada pagina aporta su propio titulo.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, FooterComponent],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent {}
