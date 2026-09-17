import { Routes } from '@angular/router';
import { authGuard } from './core/auth.service';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell.component').then(m => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'panou' },
      { path: 'panou', loadComponent: () => import('./pages/overview.component').then(m => m.OverviewComponent) },
      { path: 'trimise', loadComponent: () => import('./pages/sent.component').then(m => m.SentComponent) },
      { path: 'trimise/:id', loadComponent: () => import('./pages/sent-detail.component').then(m => m.SentDetailComponent) },
      { path: 'ciorne', loadComponent: () => import('./pages/drafts.component').then(m => m.DraftsComponent) },
      { path: 'destinatari', loadComponent: () => import('./pages/people.component').then(m => m.PeopleComponent) },
      { path: 'grupuri', loadComponent: () => import('./pages/groups.component').then(m => m.GroupsComponent) },
      { path: 'sabloane', loadComponent: () => import('./pages/templates.component').then(m => m.TemplatesComponent) },
      { path: 'sabloane/nou', loadComponent: () => import('./pages/template-new.component').then(m => m.TemplateNewComponent) },
      {
        path: 'sabloane/:id/editeaza',
        loadComponent: () => import('./pages/template-new.component').then(m => m.TemplateNewComponent),
      },
      { path: 'mesaj/sablon', loadComponent: () => import('./pages/compose-pick.component').then(m => m.ComposePickComponent) },
      { path: 'mesaj/compune', loadComponent: () => import('./pages/compose.component').then(m => m.ComposeComponent) },
      { path: 'mesaj/destinatari', loadComponent: () => import('./pages/compose-recipients.component').then(m => m.ComposeRecipientsComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];
