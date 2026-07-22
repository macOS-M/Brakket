import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-tournament-list',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './tournament-list.component.html',
  styleUrl: './tournament-list.component.scss'
})
export class TournamentListComponent {}
