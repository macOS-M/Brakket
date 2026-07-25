import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './notification-list.component.html',
  styleUrl: './notification-list.component.scss'
})
export class NotificationListComponent {}
