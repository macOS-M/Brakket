import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { MyInvitationsComponent } from './my-invitations.component';
import { TeamsService } from '../../services/teams.service';

describe('MyInvitationsComponent', () => {
  let component: MyInvitationsComponent;
  let fixture: ComponentFixture<MyInvitationsComponent>;

  beforeEach(async () => {
    const teamsServiceMock = {
      misInvitacionesPendientes: () => of([]),
      responderInvitacion: () => of({})
    };

    await TestBed.configureTestingModule({
      imports: [MyInvitationsComponent],
      providers: [{ provide: TeamsService, useValue: teamsServiceMock }]
    }).compileComponents();

    fixture = TestBed.createComponent(MyInvitationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load an empty list without error', () => {
    expect(component.invitaciones().length).toBe(0);
    expect(component.error()).toBeNull();
  });
});
