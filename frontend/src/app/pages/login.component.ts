import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="split">
      <aside>
        <div class="brand">
          <div class="mark">M</div>
          <div class="name">Notificări MUD</div>
        </div>
        <div>
          <h1>Trimite anunțuri interne pe email, Telegram și Discord, dintr-un singur loc.</h1>
          <p>Șabloane reutilizabile, selecție manuală a destinatarilor și istoric complet al trimiterilor.</p>
        </div>
      </aside>

      <main>
        <form class="card" (ngSubmit)="submit()">
          <h2>Intră în cont</h2>
          <p class="sub">Destinatarii, șabloanele și istoricul sunt legate de contul tău.</p>

          <label class="field">
            <span>Utilizator</span>
            <input class="input" name="username" [(ngModel)]="username" autocomplete="username" placeholder="admin" />
          </label>

          <label class="field">
            <span>Parolă</span>
            <input
              class="input"
              type="password"
              name="password"
              [(ngModel)]="password"
              autocomplete="current-password"
              placeholder="••••••••"
            />
          </label>

          @if (error()) {
            <div class="alert">{{ error() }}</div>
          }

          <button class="btn btn-primary" type="submit" [disabled]="busy()">
            {{ busy() ? 'Se verifică…' : 'Intră în cont' }}
          </button>

          <p class="hint">Cont de test: <b>admin</b> / <b>admin</b></p>
        </form>
      </main>
    </div>
  `,
  styles: [
    `
      .split { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
      aside {
        background: var(--ink);
        color: #fff;
        padding: 48px 56px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        min-width: 0;
      }
      .brand { display: flex; align-items: center; gap: 12px; }
      .mark {
        width: 32px;
        height: 32px;
        border-radius: var(--r-md);
        background: var(--brand);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
      }
      .name { font-size: 16px; font-weight: 600; }
      aside h1 { font-size: 32px; line-height: 40px; font-weight: 600; max-width: 460px; margin: 0; }
      aside p { font-size: 16px; line-height: 24px; color: #b2b2b2; margin: 16px 0 0; max-width: 420px; }
      main { display: flex; align-items: center; justify-content: center; padding: 48px 32px; min-width: 0; }
      .card {
        width: 100%;
        max-width: 400px;
        background: var(--surface);
        border: 1px solid var(--line);
        border-radius: var(--r-lg);
        padding: 32px;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }
      .card h2 { margin: 0; font-size: 24px; line-height: 32px; font-weight: 600; }
      .sub { margin: -10px 0 8px; font-size: 14px; color: var(--muted); }
      .hint { margin: 0; font-size: 13px; color: var(--muted); text-align: center; }
      @media (max-width: 860px) {
        .split { grid-template-columns: 1fr; }
        aside { display: none; }
      }
    `,
  ],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  username = 'admin';
  password = 'admin';
  readonly error = signal('');
  readonly busy = signal(false);

  submit(): void {
    if (!this.username.trim() || !this.password) {
      this.error.set('Completează utilizatorul și parola.');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.auth.signIn(this.username.trim(), this.password).subscribe({
      next: () => this.router.navigate(['/panou']),
      error: () => {
        this.busy.set(false);
        this.error.set('Utilizator sau parolă greșită.');
      },
    });
  }
}
