import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-statistics-view',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './statistics-view.component.html',
  styleUrl: './statistics-view.component.scss'
})
export class StatisticsViewComponent {}
