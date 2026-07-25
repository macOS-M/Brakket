import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { FooterComponent } from './footer/footer.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { TopbarComponent } from './topbar/topbar.component';

/**
 * Shell principal de la aplicacion: navegacion lateral, barra superior
 * (buscador global y sesion), area de contenido y pie de pagina.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, FooterComponent],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent {}
