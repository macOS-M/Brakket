import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-sponsorship-list',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './sponsorship-list.component.html',
  styleUrl: './sponsorship-list.component.scss'
})
export class SponsorshipListComponent {}
