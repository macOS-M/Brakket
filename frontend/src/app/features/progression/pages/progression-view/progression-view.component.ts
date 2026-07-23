import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-progression-view',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './progression-view.component.html',
  styleUrl: './progression-view.component.scss'
})
export class ProgressionViewComponent {}
