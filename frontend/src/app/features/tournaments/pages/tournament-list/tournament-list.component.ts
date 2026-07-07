import { Component } from '@angular/core';

import { BracketViewComponent } from '../../components/bracket-view/bracket-view.component';

/**
 * Torneos. Placeholder de la feature "tournaments".
 * Pendiente EPIC-07.
 */
@Component({
  selector: 'app-tournament-list',
  standalone: true,
  imports: [BracketViewComponent],
  templateUrl: './tournament-list.component.html',
  styleUrl: './tournament-list.component.scss'
})
export class TournamentListComponent {}
