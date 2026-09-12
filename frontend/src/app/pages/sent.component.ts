import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '../core/store';
import { dateLabel, pagerItems } from '../core/format';
import { CHANNELS, Channel, MessageStatus, channelTitle, statusLabel } from '../core/models';
import { IconComponent } from '../shared/icon.component';
import { SelectComponent, SelectOption } from '../shared/select.component';
import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, Search } from '../shared/icons';

const PAGE = 6;
const COLS = 'minmax(180px, 2.2fr) 130px 100px 140px 100px';

@Component({
  selector: 'app-sent',
  standalone: true,
  imports: [FormsModule, IconComponent, SelectComponent],
  template: `
    <section class="shell">
      <div class="toolbar">
        <label class="search-wrap">
          <app-icon [icon]="I.Search" [size]="16" />
          <input placeholder="Caută după subiect…" [ngModel]="q()" (ngModelChange)="q.set($event); page.set(0)" />
        </label>

        <app-select
          label="Canal"
          [options]="channelOptions()"
          [value]="chFilter()"
          (valueChange)="chFilter.set($event); page.set(0)"
        />

        <app-select
          label="Stare"
          [options]="statusOptions()"
          [value]="statusFilter()"
          (valueChange)="statusFilter.set($event); page.set(0)"
        />

        @if (filtersActive()) {
          <button type="button" class="btn btn-ghost sm" (click)="resetFilters()">Curăță filtrele</button>
        }
      </div>

      <div class="tbody">
        <div class="row head" [style.gridTemplateColumns]="cols" [style.minWidth]="'720px'">
          <button type="button" class="sort" (click)="sortBy('subject')">
            Subiect @if (arrow('subject'); as a) {<app-icon [icon]="a" [size]="13" />}
          </button>
          <span>Canal</span>
          <button type="button" class="sort" (click)="sortBy('count')">
            Destinatari @if (arrow('count'); as a) {<app-icon [icon]="a" [size]="13" />}
          </button>
          <button type="button" class="sort" (click)="sortBy('date')">
            Dată @if (arrow('date'); as a) {<app-icon [icon]="a" [size]="13" />}
          </button>
          <span>Stare</span>
        </div>

        @for (r of slice(); track r.id) {
          <div class="row clickable" [style.gridTemplateColumns]="cols" [style.minWidth]="'720px'" (click)="open(r.id)">
            <span class="cell-strong">{{ r.subject }}</span>
            <span class="tags">
              @for (c of r.channels; track c) {
                <span class="tag">{{ title(c) }}</span>
              }
            </span>
            <span>{{ r.recipientCount }}</span>
            <span class="cell-muted">{{ date(r.sentAt ?? r.createdAt) }}</span>
            <span><span class="status" [class]="r.status">{{ label(r.status) }}</span></span>
          </div>
        }

        @if (!filtered().length) {
          <div class="empty">Nicio trimitere nu corespunde filtrelor.</div>
        }
      </div>

      <div class="pager">
        <span>{{ rangeLabel() }}</span>
        <div class="pager-items">
          <button type="button" class="pager-item" [disabled]="page() === 0" (click)="page.set(page() - 1)"><app-icon [icon]="I.ChevronLeft" [size]="16" /></button>
          @for (p of pages(); track $index) {
            @if (p === '…') {
              <span class="pager-item" style="cursor:default">…</span>
            } @else {
              <button type="button" class="pager-item" [class.on]="p === page()" (click)="page.set(+p)">{{ +p + 1 }}</button>
            }
          }
          <button type="button" class="pager-item" [disabled]="page() >= pageCount() - 1" (click)="page.set(page() + 1)"><app-icon [icon]="I.ChevronRight" [size]="16" /></button>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .tags { display: flex; gap: 4px; flex-wrap: wrap; }
      .sort {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        border: none;
        background: none;
        padding: 0;
        font: inherit;
        font-weight: 500;
        color: var(--ink);
        cursor: pointer;
        text-align: left;
      }
    `,
  ],
})
export class SentComponent {
  readonly store = inject(Store);
  private router = inject(Router);

  readonly channels = CHANNELS;
  // QUEUED e filtrabil de când trimiterea e asincronă: un mesaj chiar zăbovește acolo.
  readonly statuses: MessageStatus[] = ['QUEUED', 'SENT', 'PARTIAL', 'FAILED'];
  readonly cols = COLS;
  readonly label = statusLabel;
  readonly title = channelTitle;
  readonly date = dateLabel;

  readonly I = { Search, ChevronLeft, ChevronRight };

  readonly q = signal('');
  readonly chFilter = signal<string>('');
  readonly statusFilter = signal<string>('');
  readonly page = signal(0);
  readonly sortKey = signal<'subject' | 'count' | 'date'>('date');
  readonly sortDir = signal<'asc' | 'desc'>('desc');

  constructor() {
    this.store.loadSent();
  }

  filtered = computed(() => {
    const q = this.q().trim().toLowerCase();
    const ch = this.chFilter();
    const st = this.statusFilter();
    const rows = this.store
      .sent()
      .filter(r => (!ch || r.channels.includes(ch as Channel)) && (!st || r.status === st) && (!q || r.subject.toLowerCase().includes(q)));
    const dir = this.sortDir() === 'asc' ? 1 : -1;
    const key = this.sortKey();
    return [...rows].sort((a, b) => {
      if (key === 'count') return (a.recipientCount - b.recipientCount) * dir;
      if (key === 'subject') return a.subject.localeCompare(b.subject, 'ro') * dir;
      const at = new Date(a.sentAt ?? a.createdAt).getTime();
      const bt = new Date(b.sentAt ?? b.createdAt).getTime();
      return (at - bt) * dir;
    });
  });

  pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / PAGE)));
  slice = computed(() => {
    const p = Math.min(this.page(), this.pageCount() - 1);
    return this.filtered().slice(p * PAGE, p * PAGE + PAGE);
  });
  pages = computed(() => pagerItems(Math.min(this.page(), this.pageCount() - 1), this.pageCount()));

  rangeLabel = computed(() => {
    const total = this.filtered().length;
    if (!total) return 'Nicio trimitere';
    const p = Math.min(this.page(), this.pageCount() - 1);
    return `${p * PAGE + 1}–${Math.min(total, p * PAGE + PAGE)} din ${total} trimiteri`;
  });

  filtersActive = computed(() => !!this.chFilter() || !!this.statusFilter() || !!this.q().trim());

  channelOptions = computed<SelectOption[]>(() => [
    { id: '', title: 'Toate canalele', count: this.store.sent().length },
    ...this.channels.map(c => ({ id: c.id, title: c.title, count: this.countByChannel(c.id) })),
  ]);

  statusOptions = computed<SelectOption[]>(() => [
    { id: '', title: 'Orice stare', count: this.store.sent().length },
    ...this.statuses.map(s => ({ id: s, title: statusLabel(s), count: this.countByStatus(s) })),
  ]);

  countByChannel(id: Channel): number {
    return this.store.sent().filter(r => r.channels.includes(id)).length;
  }

  countByStatus(s: MessageStatus): number {
    return this.store.sent().filter(r => r.status === s).length;
  }

  resetFilters(): void {
    this.q.set('');
    this.chFilter.set('');
    this.statusFilter.set('');
    this.page.set(0);
  }

  sortBy(key: 'subject' | 'count' | 'date'): void {
    if (this.sortKey() === key) {
      this.sortDir.set(this.sortDir() === 'desc' ? 'asc' : 'desc');
    } else {
      this.sortKey.set(key);
      this.sortDir.set('desc');
    }
    this.page.set(0);
  }

  arrow(key: 'subject' | 'count' | 'date') {
    if (this.sortKey() !== key) return null;
    return this.sortDir() === 'asc' ? ArrowUp : ArrowDown;
  }

  open(id: number): void {
    this.router.navigate(['/trimise', id]);
  }
}
