import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-twitch-panel',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './twitch-panel.component.html',
  styleUrl: './twitch-panel.component.scss'
})
export class TwitchPanelComponent {}
