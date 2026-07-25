import { Component } from '@angular/core';

import { ComingSoonComponent } from '../../../../shared/components/coming-soon/coming-soon.component';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [ComingSoonComponent],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss'
})
export class AnalyticsDashboardComponent {}
