import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ComposeStore, Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { dateLabel, plural } from '../core/format';
import { Channel, MessageView, channelTitle } from '../core/models';

const COLS = 'minmax(160px, 2fr) 130px 140px';

@Component({
  selector: 'app-drafts',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="shell">
      <div class="toolbar">
        <label class="search-wrap">
          <span>⌕</span>
          <input placeholder="Caută după subiect…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
        </label>
        <button type="button" class="btn btn-primary sm" (click)="newMessage()">Mesaj nou</button>
      </div>

      <div class="tbody">
        <div class="row head" [style.gridTemplateColumns]="cols" [style.minWidth]="'560px'">
          <span>Subiect</span><span>Canal</span><span></span>
        </div>

        @for (d of filtered(); track d.id) {
          <div class="row" [style.gridTemplateColumns]="cols" [style.minWidth]="'560px'">
            <div>
              <div class="cell-strong">{{ d.subject }}</div>
              <div class="meta">{{ meta(d) }}</div>
            </div>
            <span class="cell-muted">{{ channelsLabel(d.channels) }}</span>
            <span class="cell-actions">
              <button type="button" class="btn btn-ghost xs" (click)="resume(d)">Continuă</button>
              <button type="button" class="icon-btn" title="Șterge" (click)="remove(d)">🗑</button>
            </span>
          </div>
        }

        @if (!filtered().length) {
          <div class="empty">
            {{ store.drafts().length ? 'Nicio ciornă nu corespunde căutării.' : 'Nicio ciornă salvată. Apasă „Salvează ciornă” în pasul de compunere.' }}
          </div>
        }
      </div>

      <div class="pager"><span>{{ count() }}</span></div>
    </section>
  `,
  styles: [`.meta { font-size: 12px; color: var(--muted); margin-top: 2px; }`],
})
export class DraftsComponent {
  readonly store = inject(Store);
  private api = inject(ApiService);
  private compose = inject(ComposeStore);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly cols = COLS;
  readonly q = signal('');

  constructor() {
    this.store.loadDrafts();
  }

  filtered = computed(() => {
    const q = this.q().trim().toLowerCase();
    return this.store.drafts().filter(d => !q || d.subject.toLowerCase().includes(q));
  });

  count = computed(() => (this.filtered().length ? plural(this.filtered().length, 'ciornă', 'ciorne') : 'Nicio ciornă'));

  meta(d: MessageView): string {
    return `${dateLabel(d.createdAt)} · ${d.images ? plural(d.images, 'imagine', 'imagini') : 'fără imagini'}`;
  }

  channelsLabel(list: Channel[]): string {
    return list.map(channelTitle).join(' + ');
  }

  newMessage(): void {
    this.compose.reset();
    this.router.navigate(['/mesaj/sablon']);
  }

  resume(d: MessageView): void {
    // Lista de ciorne nu aduce si continutul; il luam din detaliu inainte de a intra in editor.
    this.api.message(d.id).subscribe({
      next: full => {
        this.compose.reset();
        this.compose.subject.set(d.subject);
        this.compose.bodyHtml.set(full.bodyHtml);
        this.compose.channels.set(d.channels.length ? d.channels : ['EMAIL']);
        this.compose.draftId.set(d.id);
        this.router.navigate(['/mesaj/compune']);
      },
      error: e => this.toast.error(e),
    });
  }

  remove(d: MessageView): void {
    this.api.deleteDraft(d.id).subscribe({
      next: () => {
        this.store.loadDrafts();
        this.toast.show('Ciornă ștearsă.');
      },
      error: e => this.toast.error(e),
    });
  }
}
