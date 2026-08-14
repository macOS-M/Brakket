import { Component } from '@angular/core';

import { ahoraCostaRica } from '../../shared/utils/hora-costa-rica';

/**
 * Pie de pagina global.
 */
@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  readonly year = ahoraCostaRica().getFullYear();
}
