import { Component, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ComposeStore } from '../core/store';
import { ToastService } from '../core/toast.service';
import { dateLabel } from '../core/format';
import { Channel, MessageDetail, channelTitle, statusLabel } from '../core/models';

@Component({
  selector: 'app-sent-detail',
  standalone: true,
  template: `
    @if (detail(); as d) {
      <section class="shell">
        <div class="head">
          <div>
            <h2>{{ d.summary.subject }}</h2>
            <span class="status" [class]="d.summary.status">{{ label(d.summary.status) }}</span>
          </div>
          <div class="actions">
            <button type="button" class="btn btn-ghost sm" (click)="back()">Înapoi la listă</button>
            <button type="button" class="btn btn-primary sm" (click)="resend()">Retrimite mesajul</button>
          </div>
        </div>

        <div class="meta">
          <div><span>Trimis</span><b>{{ date(d.summary.sentAt ?? d.summary.createdAt) }}</b></div>
          <div><span>Canal</span><b>{{ channelsLabel(d.summary.channels) }}</b></div>
          <div><span>Destinatari</span><b>{{ d.summary.recipientCount }}</b></div>
          <div><span>Stare</span><b>{{ label(d.summary.status) }}</b></div>
          <div><span>Imagini</span><b>{{ d.summary.images || 'fără' }}</b></div>
        </div>

        <div class="body" [innerHTML]="html()"></div>

        @if (d.recipientNames.length) {
          <div class="recipients">
            <span>Destinatari</span>
            <p>{{ names(d.recipientNames) }}</p>
          </div>
        }
      </section>
    } @else {
      <div class="empty">Se încarcă…</div>
    }
  `,
  styles: [
    `
      .head {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 16px;
        flex-wrap: wrap;
        padding: 20px 24px;
        border-bottom: 1px solid var(--line);
      }
      .head h2 { margin: 0 0 8px; font-size: 18px; font-weight: 600; }
      .actions { display: flex; gap: 8px; flex-wrap: wrap; }
      .meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 1px; background: var(--line-soft); }
      .meta > div { background: var(--surface); padding: 12px 24px; display: flex; flex-direction: column; gap: 2px; }
      .meta span { font-size: 12px; color: var(--muted); }
      .meta b { font-size: 14px; font-weight: 500; }
      .body { padding: 24px; font-size: 15px; line-height: 24px; border-top: 1px solid var(--line-soft); }
      .recipients { padding: 0 24px 24px; }
      .recipients span { font-size: 12px; color: var(--muted); }
      .recipients p { margin: 4px 0 0; font-size: 14px; color: var(--ink-2); }
    `,
  ],
})
export class SentDetailComponent {
  private api = inject(ApiService);
  private router = inject(Router);
  private compose = inject(ComposeStore);
  private toast = inject(ToastService);
  private sanitizer = inject(DomSanitizer);

  readonly id = input.required<string>();
  readonly detail = signal<MessageDetail | null>(null);
  readonly label = statusLabel;
  readonly date = dateLabel;

  ngOnInit(): void {
    this.api.message(+this.id()).subscribe({
      next: d => this.detail.set(d),
      error: e => {
        this.toast.error(e);
        this.router.navigate(['/trimise']);
      },
    });
  }

  /** Continutul e scris chiar in aplicatie si salvat de backend-ul nostru. */
  html(): SafeHtml {
    const body = this.detail()?.bodyHtml;
    return this.sanitizer.bypassSecurityTrustHtml(body || '<em style="color:#757575">Mesaj fără conținut salvat.</em>');
  }

  channelsLabel(list: Channel[]): string {
    return list.map(channelTitle).join(' + ');
  }

  names(list: string[]): string {
    return list.slice(0, 8).join(', ') + (list.length > 8 ? ` și încă ${list.length - 8}` : '');
  }

  back(): void {
    this.router.navigate(['/trimise']);
  }

  resend(): void {
    const d = this.detail();
    if (!d) return;
    this.compose.reset();
    this.compose.subject.set(d.summary.subject);
    this.compose.bodyHtml.set(d.bodyHtml);
    this.compose.channels.set(d.summary.channels.length ? d.summary.channels : ['EMAIL']);
    this.toast.show('Mesaj încărcat pentru retrimitere.');
    this.router.navigate(['/mesaj/compune']);
  }
}
