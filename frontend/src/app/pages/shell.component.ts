import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { ComposeStore, Store } from '../core/store';
import { IconComponent } from '../shared/icon.component';
import {
  Check,
  FileText,
  LayoutGrid,
  LayoutTemplate,
  LogOut,
  PanelLeft,
  Plus,
  Send,
  Users,
} from '../shared/icons';

const FLOW = ['/mesaj/sablon', '/mesaj/compune', '/mesaj/destinatari'];

const TITLES: Record<string, [string, string]> = {
  '/panou': ['Panou', 'Starea trimiterilor din contul tău, pe scurt.'],
  '/trimise': ['Mesaje trimise', 'Istoricul complet al trimiterilor, cu canal, destinatari și stare.'],
  '/ciorne': ['Ciorne', 'Mesaje salvate pentru mai târziu — text, imagini și canal, exact ca la salvare.'],
  '/destinatari': ['Destinatari', 'Toate persoanele din contul tău, cu grupul din care fac parte.'],
  '/sabloane': ['Șabloane', 'Adaugă, șterge sau folosește direct un șablon ca punct de plecare.'],
  '/sabloane/nou': ['Șablon nou', 'Definește un punct de plecare reutilizabil pentru mesajele viitoare.'],
  '/mesaj/sablon': ['Mesaj nou', 'Alege un șablon ca punct de plecare — tot textul rămâne editabil.'],
  '/mesaj/compune': ['Compune mesajul', 'Editează liber textul, adaugă imagini și alege canalul de livrare.'],
  '/mesaj/destinatari': ['Selectează destinatarii', 'Selecția este manuală și reversibilă până la apăsarea butonului de trimitere.'],
};

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, IconComponent],
  template: `
    <div class="layout">
      <nav class="rail" [class.closed]="!railOpen()">
        <div class="rail-brand">
          <div class="mark">M</div>
          @if (railOpen()) {
            <div>
              <div class="rail-name">Notificări MUD</div>
              <div class="rail-sub">Spațiu de lucru</div>
            </div>
          }
        </div>

        <div class="rail-body">
          <div class="rail-heading">Comunicare</div>
          <a class="nav" routerLink="/panou" [class.on]="is('/panou')" title="Panou">
            <app-icon class="ico" [icon]="I.LayoutGrid" />@if (railOpen()) {<span class="lbl">Panou</span>}
          </a>
          <a class="nav" (click)="newMessage()" [class.on]="inFlow()" title="Mesaj nou">
            <app-icon class="ico" [icon]="I.Plus" />@if (railOpen()) {<span class="lbl">Mesaj nou</span>}
          </a>
          <a class="nav" routerLink="/trimise" [class.on]="is('/trimise')" title="Mesaje trimise">
            <app-icon class="ico" [icon]="I.Send" />
            @if (railOpen()) {
              <span class="lbl">Mesaje trimise</span><span class="badge">{{ store.sent().length }}</span>
            }
          </a>
          <a class="nav" routerLink="/ciorne" [class.on]="is('/ciorne')" title="Ciorne">
            <app-icon class="ico" [icon]="I.FileText" />
            @if (railOpen()) {
              <span class="lbl">Ciorne</span><span class="badge">{{ store.drafts().length }}</span>
            }
          </a>

          <div class="rail-heading">Administrare</div>
          <a class="nav" routerLink="/destinatari" [class.on]="is('/destinatari')" title="Destinatari">
            <app-icon class="ico" [icon]="I.Users" />
            @if (railOpen()) {
              <span class="lbl">Destinatari</span><span class="badge">{{ store.recipients().length }}</span>
            }
          </a>
          <a class="nav" routerLink="/sabloane" [class.on]="is('/sabloane')" title="Șabloane">
            <app-icon class="ico" [icon]="I.LayoutTemplate" />
            @if (railOpen()) {
              <span class="lbl">Șabloane</span><span class="badge">{{ store.templates().length }}</span>
            }
          </a>
        </div>

        <div class="rail-foot">
          <div class="avatar">{{ auth.initials() }}</div>
          @if (railOpen()) {
            <div class="who">
              <div class="login">{{ auth.login() }}</div>
              <button type="button" class="link" (click)="auth.signOut()">
                <app-icon [icon]="I.LogOut" [size]="12" />Ieși din cont
              </button>
            </div>
          }
        </div>
      </nav>

      <div class="main">
        <header class="top">
          <button type="button" class="icon-btn" (click)="railOpen.set(!railOpen())" title="Restrânge meniul"><app-icon [icon]="I.PanelLeft" /></button>
          <div class="crumbs">
            @for (c of crumbs(); track c.label; let last = $last) {
              @if (!$first) {<span class="sep">/</span>}
              @if (last) {
                <span class="current">{{ c.label }}</span>
              } @else {
                <a [routerLink]="c.link">{{ c.label }}</a>
              }
            }
          </div>
        </header>

        <div class="scroll">
          <div class="page">
            <div class="page-head">
              <h1 class="page-title">{{ head()[0] }}</h1>
              <p class="page-sub">{{ head()[1] }}</p>
            </div>

            @if (inFlow()) {
              <div class="steps">
                @for (s of steps; track s.path; let i = $index) {
                  <div class="step-wrap">
                    <button type="button" class="step" [class.active]="i === stepIndex()" [disabled]="i > stepIndex()" (click)="goStep(i)">
                      <span class="dot" [class.done]="i < stepIndex()" [class.active]="i === stepIndex()">
                        @if (i < stepIndex()) {
                          <app-icon [icon]="I.Check" [size]="13" [stroke]="2.5" />
                        } @else {
                          {{ i + 1 }}
                        }
                      </span>
                      <span class="step-label">{{ s.label }}</span>
                    </button>
                    @if (i < 2) {
                      <span class="line" [class.done]="i < stepIndex()"></span>
                    }
                  </div>
                }
              </div>
            }

            <router-outlet />
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .layout { height: 100vh; display: flex; overflow: hidden; }
      .rail {
        flex: none;
        width: 264px;
        background: var(--surface);
        border-right: 1px solid var(--line);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        transition: width 150ms ease-out;
      }
      .rail.closed { width: 68px; }
      .rail-brand {
        height: 56px;
        flex: none;
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 0 20px;
        border-bottom: 1px solid var(--line-soft);
      }
      .rail.closed .rail-brand { padding: 0; justify-content: center; }
      .mark {
        width: 28px;
        height: 28px;
        border-radius: var(--r-md);
        background: var(--brand);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        flex: none;
      }
      .rail-name { font-size: 14px; font-weight: 600; white-space: nowrap; }
      .rail-sub { font-size: 12px; color: var(--muted); white-space: nowrap; }
      .rail-body { flex: 1; overflow-y: auto; padding: 16px 0; }
      .rail-heading {
        padding: 12px 20px 6px;
        font-size: 12px;
        font-weight: 500;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--muted);
        white-space: nowrap;
      }
      .rail.closed .rail-heading { visibility: hidden; }
      .nav {
        display: flex;
        align-items: center;
        gap: 12px;
        min-height: 48px;
        padding: 12px 16px 12px 17px;
        cursor: pointer;
        text-decoration: none;
        color: var(--ink-2);
        border-left: 3px solid transparent;
        transition: background 150ms ease-out;
      }
      .rail.closed .nav { padding: 12px 0; justify-content: center; }
      .nav:hover { background: var(--brand-soft); }
      .nav.on { border-left-color: var(--brand); background: var(--brand-soft); color: var(--brand); font-weight: 500; }
      .ico { flex: none; width: 18px; text-align: center; font-size: 15px; }
      .lbl { flex: 1; min-width: 0; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .badge {
        flex: none;
        min-width: 24px;
        padding: 0 8px;
        border-radius: 9999px;
        font-size: 12px;
        line-height: 20px;
        font-weight: 500;
        text-align: center;
        background: var(--line-soft);
        color: var(--ink-2);
      }
      .nav.on .badge { background: #fff; }
      .rail-foot {
        flex: none;
        border-top: 1px solid var(--line-soft);
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 20px;
      }
      .rail.closed .rail-foot { padding: 12px 0; justify-content: center; }
      .avatar {
        width: 32px;
        height: 32px;
        border-radius: 9999px;
        background: var(--brand-soft);
        color: var(--brand);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 13px;
        font-weight: 600;
        flex: none;
      }
      .who { min-width: 0; }
      .login { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 130px; }
      .link {
        display: flex;
        align-items: center;
        gap: 4px;
        border: none;
        background: none;
        padding: 0;
        font-size: 12px;
        color: var(--brand);
        cursor: pointer;
      }
      .main { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
      .top {
        height: 56px;
        flex: none;
        background: var(--surface);
        border-bottom: 1px solid var(--line);
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 0 24px;
      }
      .crumbs { display: flex; align-items: center; gap: 8px; font-size: 13px; min-width: 0; overflow: hidden; }
      .crumbs a { color: var(--muted); text-decoration: none; white-space: nowrap; }
      .crumbs a:hover { color: var(--brand); text-decoration: underline; }
      .sep { color: var(--faint); }
      .current { color: var(--ink); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .scroll { flex: 1; overflow-y: auto; }
      .page { padding: 24px; max-width: 1180px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
      .page-head { display: flex; flex-direction: column; }
      .steps { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
      .step-wrap { display: flex; align-items: center; gap: 12px; flex: 1 1 0; min-width: 0; }
      .step-wrap:last-child { flex: none; }
      .step {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 10px;
        border: none;
        border-radius: 9999px;
        background: transparent;
        cursor: pointer;
        flex: none;
      }
      .step:disabled { cursor: default; }
      .step.active { background: var(--brand-soft); }
      .dot {
        width: 24px;
        height: 24px;
        border-radius: 9999px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: 500;
        border: 1.5px solid var(--faint);
        color: var(--faint);
        background: var(--surface);
        flex: none;
      }
      .dot.active { border-color: var(--brand); color: var(--brand); }
      .dot.done { border-color: var(--brand); background: var(--brand); color: #fff; }
      .step-label { font-size: 13px; white-space: nowrap; color: var(--muted); }
      .step.active .step-label { color: var(--ink); font-weight: 500; }
      .line { flex: 1 1 auto; min-width: 24px; height: 2px; border-radius: 2px; background: var(--line); }
      .line.done { background: var(--brand); }
      @media (max-width: 720px) {
        .rail { position: absolute; z-index: 40; height: 100%; }
      }
    `,
  ],
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly store = inject(Store);
  private compose = inject(ComposeStore);
  private router = inject(Router);

  readonly I = { LayoutGrid, Plus, Send, FileText, Users, LayoutTemplate, PanelLeft, LogOut, Check };

  readonly railOpen = signal(true);
  readonly steps = [
    { label: 'Șablon', path: '/mesaj/sablon' },
    { label: 'Mesaj', path: '/mesaj/compune' },
    { label: 'Destinatari', path: '/mesaj/destinatari' },
  ];

  private url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(e => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  constructor() {
    this.store.loadAll();
  }

  is(path: string): boolean {
    return this.url() === path || this.url().startsWith(path + '/');
  }

  inFlow = computed(() => FLOW.includes(this.url()));
  stepIndex = computed(() => FLOW.indexOf(this.url()));

  head = computed<[string, string]>(() => {
    const u = this.url();
    if (u.startsWith('/trimise/')) {
      return ['Mesaj trimis', 'Conținutul livrat, canalele folosite și destinatarii trimiterii.'];
    }
    return TITLES[u] ?? ['Panou', ''];
  });

  crumbs = computed(() => {
    const u = this.url();
    const trail: { label: string; link: string }[] = [{ label: 'Panou', link: '/panou' }];
    if (u === '/panou') return trail;
    if (u.startsWith('/sabloane')) {
      trail.push({ label: 'Șabloane', link: '/sabloane' });
      if (u !== '/sabloane') trail.push({ label: 'Șablon nou', link: u });
    } else if (u.startsWith('/trimise')) {
      trail.push({ label: 'Mesaje trimise', link: '/trimise' });
      if (u !== '/trimise') trail.push({ label: this.head()[0], link: u });
    } else if (this.inFlow()) {
      trail.push({ label: 'Mesaj nou', link: '/mesaj/sablon' });
      if (u !== '/mesaj/sablon') trail.push({ label: this.head()[0], link: u });
    } else {
      trail.push({ label: this.head()[0], link: u });
    }
    return trail;
  });

  newMessage(): void {
    this.compose.reset();
    this.router.navigate(['/mesaj/sablon']);
  }

  goStep(i: number): void {
    if (i <= this.stepIndex()) {
      this.router.navigateByUrl(FLOW[i]);
    }
  }
}
