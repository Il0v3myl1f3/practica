import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Store } from '../core/store';
import { dateLabel } from '../core/format';
import { channelTitle, statusLabel } from '../core/models';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (o(); as ov) {
      <div class="kpis">
        <div class="kpi wide">
          <div class="k-label">Trimiteri</div>
          <div class="k-value big">{{ ov.totalSent }}</div>
          <div class="k-sub">în total</div>
        </div>
        <div class="kpi">
          <div class="k-label">Destinatari</div>
          <div class="k-value">{{ ov.recipients }}</div>
          <div class="k-sub">{{ ov.groups }} grupuri</div>
        </div>
        <div class="kpi">
          <div class="k-label">Ciorne</div>
          <div class="k-value">{{ ov.drafts }}</div>
          <div class="k-sub" [class.brand]="ov.drafts > 0">
            {{ ov.drafts ? 'așteaptă finalizare' : 'nimic în așteptare' }}
          </div>
        </div>
        <div class="kpi">
          <div class="k-label">Acoperire medie</div>
          <div class="k-value">{{ ov.averageReach }}</div>
          <div class="k-sub">destinatari / trimitere</div>
        </div>
        <div class="kpi">
          <div class="k-label">Livrate integral</div>
          <div class="k-value">{{ ov.fullyDelivered }}</div>
          <div class="k-sub">fără eșecuri parțiale</div>
        </div>
      </div>

      <div class="cols">
        <section class="shell">
          <div class="card-head">
            <h2>Ultimele trimiteri</h2>
            <a routerLink="/trimise">Vezi toate</a>
          </div>
          @for (r of recent(); track r.id) {
            <a class="recent" [routerLink]="['/trimise', r.id]">
              <div>
                <div class="cell-strong">{{ r.subject }}</div>
                <div class="meta">{{ meta(r.sentAt ?? r.createdAt, r.recipientCount, r.channels) }}</div>
              </div>
              <span class="status" [class]="r.status">{{ label(r.status) }}</span>
            </a>
          } @empty {
            <div class="empty">Nicio trimitere încă.</div>
          }
        </section>

        <section class="shell">
          <div class="card-head"><h2>Distribuție pe grupuri</h2></div>
          <div class="dist">
            @for (g of ov.groupDistribution; track g.id) {
              <div class="grp">
                <div class="grp-top"><span>{{ g.name }}</span><span class="muted">{{ g.count }}</span></div>
                <div class="track"><div class="bar" [style.width.%]="pct(g.count, ov.recipients)"></div></div>
              </div>
            } @empty {
              <div class="empty">Niciun grup.</div>
            }
          </div>
        </section>
      </div>
    } @else {
      <div class="empty">Se încarcă…</div>
    }
  `,
  styles: [
    `
      .kpis {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 1px;
        background: var(--line);
        border: 1px solid var(--line);
        border-radius: var(--r-lg);
        overflow: hidden;
      }
      .kpi { padding: 20px 24px; background: var(--surface); }
      .kpi.wide { grid-column: span 2; }
      .k-label { font-size: 13px; color: var(--muted); }
      .k-value { font-size: 28px; line-height: 36px; font-weight: 600; margin: 6px 0 4px; }
      .k-value.big { font-size: 36px; line-height: 44px; }
      .k-sub { font-size: 13px; color: var(--muted); }
      .k-sub.brand { color: var(--brand); }
      .cols { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr); gap: 20px; }
      .card-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px 20px;
        border-bottom: 1px solid var(--line);
      }
      .card-head h2 { margin: 0; font-size: 16px; font-weight: 600; }
      .card-head a { font-size: 13px; color: var(--brand); text-decoration: none; }
      .recent {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        padding: 12px 20px;
        border-bottom: 1px solid var(--line-soft);
        text-decoration: none;
        color: inherit;
      }
      .recent:hover { background: #fafafa; }
      .recent:last-child { border-bottom: none; }
      .meta { font-size: 12px; color: var(--muted); margin-top: 2px; }
      .dist { padding: 16px 20px; display: flex; flex-direction: column; gap: 14px; }
      .grp-top { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; }
      .muted { color: var(--muted); }
      .track { height: 8px; background: var(--line-soft); border-radius: 9999px; overflow: hidden; }
      .bar { height: 100%; background: var(--brand); border-radius: 9999px; }
      @media (max-width: 900px) {
        .cols { grid-template-columns: 1fr; }
        .kpi.wide { grid-column: span 1; }
      }
    `,
  ],
})
export class OverviewComponent {
  private store = inject(Store);
  readonly o = this.store.overview;
  readonly recent = computed(() => this.store.sent().slice(0, 5));
  readonly label = statusLabel;

  constructor() {
    this.store.loadOverview();
    this.store.loadSent();
  }

  pct(count: number, total: number): number {
    return total ? Math.round((count / total) * 100) : 0;
  }

  meta(date: string | null, count: number, channels: string[]): string {
    return `${dateLabel(date)} · ${count} destinatari · ${channels.map(c => channelTitle(c as never)).join(' + ')}`;
  }
}
