import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-dispute-list',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './dispute-list.component.html',
  styleUrl: './dispute-list.component.scss'
})
export class DisputeListComponent {}
