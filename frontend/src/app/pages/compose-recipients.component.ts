import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ComposeStore, Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { Channel, Recipient, SendResult, channelTitle } from '../core/models';
import { IconComponent } from '../shared/icon.component';
import { Check, ChevronDown, ChevronRight, Minus, Search } from '../shared/icons';

const COLS = 'minmax(140px, 2fr) minmax(0, 1.4fr) 190px';

@Component({
  selector: 'app-compose-recipients',
  standalone: true,
  imports: [FormsModule, IconComponent],
  template: `
    <section class="shell">
      <div class="toolbar">
        <label class="search-wrap">
          <app-icon [icon]="I.Search" [size]="16" />
          <input placeholder="Caută nume, email sau grup…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
        </label>
        <select class="select filter" [ngModel]="groupFilter()" (ngModelChange)="groupFilter.set($event)">
          <option value="">Toate grupurile</option>
          @for (g of store.groups(); track g.id) {
            <option [value]="g.name">{{ g.name }} · {{ g.count }}</option>
          }
        </select>
        <span class="grow"></span>
        <button type="button" class="btn btn-ghost sm" (click)="selectAll(true)">Bifează tot</button>
        <button type="button" class="btn btn-ghost sm" (click)="selectAll(false)">Debifează tot</button>
      </div>

      @if (conflictOpen()) {
        <div class="notice warn">
          <div>
            <b>{{ conflictTitle() }}</b>
            <div>{{ conflictDetail() }}</div>
          </div>
          <div class="notice-actions">
            <button type="button" class="btn btn-ghost sm" (click)="excludeConflicts()">Exclude-i</button>
            <button type="button" class="btn btn-primary sm" (click)="useFallback()">Trimite pe canalele disponibile</button>
          </div>
        </div>
      }

      @if (conflictFixed()) {
        <div class="notice ok">
          <div>{{ fallbackLabel() }}</div>
          <button type="button" class="btn btn-ghost sm" (click)="c.conflictMode.set('block')">Anulează</button>
        </div>
      }

      <div class="tbody">
        @for (g of groups(); track g.name) {
          <div class="group-head" (click)="toggleOpen(g.name)">
            <span class="checkbox" [class.on]="g.allOn" [class.partial]="g.someOn" (click)="toggleGroup($event, g)">
              @if (g.allOn) {
                <app-icon [icon]="I.Check" [size]="14" [stroke]="2.5" />
              } @else if (g.someOn) {
                <app-icon [icon]="I.Minus" [size]="14" [stroke]="2.5" />
              }
            </span>
            <b>{{ g.name }}</b>
            <span class="count">{{ g.selectedCount }}/{{ g.people.length }}</span>
            <span class="grow"></span>
            <app-icon class="chev" [icon]="isOpen(g.name) ? I.ChevronDown : I.ChevronRight" [size]="16" />
          </div>

          @if (isOpen(g.name)) {
            @for (p of g.people; track p.id) {
              <div
                class="row clickable"
                [class.selected]="isSelected(p.id)"
                [style.gridTemplateColumns]="cols"
                [style.minWidth]="'620px'"
                (click)="c.toggleRecipient(p.id)"
              >
                <span class="name">
                  <span class="checkbox sm" [class.on]="isSelected(p.id)">
                    @if (isSelected(p.id)) {
                      <app-icon [icon]="I.Check" [size]="13" [stroke]="2.5" />
                    }
                  </span>
                  <span class="cell-strong">{{ p.name }}</span>
                </span>
                <span class="cell-muted">{{ p.email }}</span>
                <span class="marks">
                  @if (gapOf(p); as gap) {
                    <button type="button" class="pill gap" [class.hard]="gap.hard" (click)="openPerson($event, p)">
                      fără {{ gap.label }} · rezolvă
                    </button>
                  }
                  @if (fixOf(p); as fix) {
                    <button type="button" class="pill fix" (click)="openPerson($event, p)">doar {{ fix }}</button>
                  }
                </span>
              </div>
            }
          }
        } @empty {
          <div class="empty">Nicio persoană nu corespunde căutării.</div>
        }
      </div>

      <div class="footer">
        <span>{{ counter() }}</span>
        <div class="grow"></div>
        <button type="button" class="btn btn-ghost sm" (click)="router.navigate(['/mesaj/compune'])">Înapoi</button>
        <button type="button" class="btn btn-primary sm" [disabled]="!canSend()" (click)="askSend()">
          {{ selectedCount() ? 'Trimite la ' + selectedCount() : 'Selectează destinatari' }}
        </button>
      </div>
    </section>

    @if (person(); as p) {
      <div class="backdrop">
        <div class="modal" style="max-width:440px">
          <h3>{{ p.name }}</h3>
          <p class="modal-sub">{{ p.email }}</p>
          <p style="margin:0 0 10px;font-size:13px">Alege canalele pe care pleacă mesajul către acest destinatar:</p>
          <div class="pick-list">
            @for (ch of c.channels(); track ch) {
              <button
                type="button"
                class="pick"
                [class.on]="pick().includes(ch)"
                [disabled]="!p.channels.includes(ch)"
                (click)="togglePick(ch)"
              >
                <span>
                  <b>{{ title(ch) }}</b>
                  <small>
                    {{ p.channels.includes(ch) ? (pick().includes(ch) ? 'Se trimite' : 'Nu se trimite') : 'Destinatarul nu are acest canal configurat' }}
                  </small>
                </span>
                <span class="checkbox sm" [class.on]="pick().includes(ch)">
                  @if (pick().includes(ch)) {
                    <app-icon [icon]="I.Check" [size]="13" [stroke]="2.5" />
                  }
                </span>
              </button>
            }
          </div>
          @if (!pick().length) {
            <p class="warn-text">Niciun canal selectat — salvarea nu e posibilă, poți exclude destinatarul.</p>
          }
          <div class="modal-actions" style="justify-content:space-between">
            <button type="button" class="btn btn-danger-ghost" (click)="excludePerson(p)">Exclude destinatarul</button>
            <span style="display:flex;gap:8px">
              <button type="button" class="btn btn-ghost" (click)="person.set(null)">Anulează</button>
              <button type="button" class="btn btn-primary" [disabled]="!pick().length" (click)="savePerson(p)">Salvează</button>
            </span>
          </div>
        </div>
      </div>
    }

    @if (confirming()) {
      <div class="backdrop">
        <div class="modal" style="max-width:440px">
          <h3>Confirmi trimiterea?</h3>
          <p class="modal-sub">{{ confirmText() }}</p>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="confirming.set(false)">Anulează</button>
            <button type="button" class="btn btn-primary" [disabled]="sending()" (click)="doSend()">
              {{ sending() ? 'Se trimite…' : 'Trimite la ' + selectedCount() }}
            </button>
          </div>
        </div>
      </div>
    }

    @if (result(); as r) {
      <div class="backdrop">
        <div class="modal">
          <div class="ok-mark"><app-icon [icon]="I.Check" [size]="22" [stroke]="2.5" /></div>
          <h3>Mesaj trimis</h3>
          <p class="modal-sub">
            {{ r.recipientCount }} {{ r.recipientCount === 1 ? 'destinatar a primit mesajul' : 'destinatari au primit mesajul' }}
            · {{ r.delivered }} livrări reușite@if (r.failed) {, {{ r.failed }} eșuate}.
          </p>
          <div class="recap">
            <div><span>Subiect</span><b>{{ c.subject() || 'Fără subiect' }}</b></div>
            <div><span>Canal</span><b>{{ channelsLabel() }}</b></div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="again()">Mesaj nou</button>
            <button type="button" class="btn btn-primary" (click)="goSent()">Vezi mesajele trimise</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .filter { flex: 0 1 auto; width: auto; min-width: 190px; height: 40px; font-size: 14px; }
      .grow { flex: 1 1 auto; }
      .notice { margin: 0 16px 12px; }
      .notice-actions { display: flex; gap: 8px; flex-wrap: wrap; }
      .group-head {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px clamp(16px, 3.75cqi, 24px);
        background: #fafafa;
        border-bottom: 1px solid var(--line);
        cursor: pointer;
        font-size: 14px;
      }
      .count { font-size: 12px; color: var(--muted); }
      .chev { color: var(--ink-4); }
      .name { display: flex; align-items: center; gap: 10px; min-width: 0; }
      .marks { display: flex; gap: 6px; justify-content: flex-end; }
      .pill {
        font-size: 12px;
        line-height: 16px;
        font-weight: 500;
        padding: 3px 8px;
        border: none;
        border-radius: 9999px;
        cursor: pointer;
        white-space: nowrap;
        font-family: inherit;
      }
      .pill.gap { background: #fdf3e2; color: #8a5a08; }
      .pill.gap.hard { background: var(--err-bg); color: var(--err-ink); }
      .pill.fix { background: var(--ok-bg); color: var(--ok-ink-2); }
      .footer {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        padding: 12px clamp(16px, 3.75cqi, 24px);
        border-top: 1px solid var(--line);
        font-size: 13px;
        color: var(--muted);
      }
      .pick-list { display: flex; flex-direction: column; gap: 8px; }
      .pick {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        width: 100%;
        min-height: 52px;
        padding: 8px 14px;
        text-align: left;
        border: 1px solid var(--line);
        border-radius: var(--r-md);
        background: var(--surface);
        cursor: pointer;
        font-family: inherit;
      }
      .pick.on { border-color: var(--brand); background: var(--brand-soft); }
      .pick:disabled { background: var(--canvas); color: var(--faint); cursor: not-allowed; }
      .pick span { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
      .pick b { font-size: 14px; font-weight: 500; }
      .pick small { font-size: 12px; color: var(--muted); }
      .warn-text { margin: 10px 0 0; font-size: 13px; color: var(--err-ink); }
      .ok-mark {
        width: 44px;
        height: 44px;
        border-radius: 9999px;
        background: var(--ok-bg);
        color: var(--ok-ink);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        font-weight: 700;
        margin-bottom: 16px;
      }
      .recap { border: 1px solid var(--line-soft); border-radius: var(--r-md); overflow: hidden; }
      .recap > div {
        display: grid;
        grid-template-columns: 110px minmax(0, 1fr);
        gap: 16px;
        padding: 10px 16px;
        font-size: 13px;
        border-bottom: 1px solid var(--line-soft);
      }
      .recap > div:last-child { border-bottom: none; }
      .recap span { color: var(--muted); }
    `,
  ],
})
export class ComposeRecipientsComponent {
  readonly c = inject(ComposeStore);
  readonly store = inject(Store);
  readonly router = inject(Router);
  private api = inject(ApiService);
  private toast = inject(ToastService);

  readonly cols = COLS;
  readonly title = channelTitle;
  readonly I = { Search, Check, Minus, ChevronDown, ChevronRight };

  readonly q = signal('');
  readonly groupFilter = signal('');
  readonly collapsed = signal<string[]>([]);
  readonly person = signal<Recipient | null>(null);
  readonly pick = signal<Channel[]>([]);
  readonly confirming = signal(false);
  readonly sending = signal(false);
  readonly result = signal<SendResult | null>(null);

  constructor() {
    this.store.loadRecipients();
    this.store.loadGroups();
  }

  // -------------------------------------------------------------- selectie

  visible = computed(() => {
    const q = this.q().trim().toLowerCase();
    const g = this.groupFilter();
    return this.store
      .recipients()
      .filter(p => (!g || p.group === g) && (!q || `${p.name} ${p.email} ${p.group}`.toLowerCase().includes(q)));
  });

  groups = computed(() => {
    const sel = new Set(this.c.selected());
    const names = [...new Set(this.visible().map(p => p.group))].sort((a, b) => a.localeCompare(b, 'ro'));
    return names.map(name => {
      const people = this.visible().filter(p => p.group === name);
      const selectedCount = people.filter(p => sel.has(p.id)).length;
      return {
        name,
        people,
        selectedCount,
        allOn: people.length > 0 && selectedCount === people.length,
        someOn: selectedCount > 0 && selectedCount < people.length,
      };
    });
  });

  selectedCount = computed(() => this.c.selected().length);
  counter = computed(() => `${this.selectedCount()} din ${this.store.recipients().length} persoane selectate`);

  isSelected(id: number): boolean {
    return this.c.selected().includes(id);
  }

  isOpen(name: string): boolean {
    return !this.collapsed().includes(name);
  }

  toggleOpen(name: string): void {
    const cur = this.collapsed();
    this.collapsed.set(cur.includes(name) ? cur.filter(x => x !== name) : [...cur, name]);
  }

  toggleGroup(e: Event, g: { people: Recipient[]; allOn: boolean }): void {
    e.stopPropagation();
    this.c.setMany(g.people.map(p => p.id), !g.allOn);
  }

  selectAll(on: boolean): void {
    this.c.setMany(this.visible().map(p => p.id), on);
  }

  // -------------------------------------------------- conflictul de canal

  private missingFor(p: Recipient): Channel[] {
    return this.c.channels().filter(ch => !p.channels.includes(ch));
  }

  private selectedPeople = computed(() => {
    const sel = new Set(this.c.selected());
    return this.store.recipients().filter(p => sel.has(p.id));
  });

  private conflictPeople = computed(() => {
    const ov = this.c.overrides();
    return this.selectedPeople().filter(p => this.missingFor(p).length > 0 && !ov[p.id]);
  });

  private unreachable = computed(() => this.conflictPeople().filter(p => this.missingFor(p).length === this.c.channels().length));

  conflictOpen = computed(() => this.conflictPeople().length > 0 && this.c.conflictMode() === 'block');
  conflictFixed = computed(() => this.conflictPeople().length > 0 && this.c.conflictMode() === 'fallback');

  conflictTitle = computed(() => {
    const n = this.conflictPeople().length;
    return n === 1
      ? '1 destinatar selectat nu are toate canalele alese'
      : `${n} destinatari selectați nu au toate canalele alese`;
  });

  conflictDetail = computed(() => {
    const parts = this.c
      .channels()
      .map(ch => ({ ch, count: this.conflictPeople().filter(p => !p.channels.includes(ch)).length }))
      .filter(x => x.count > 0)
      .map(x => `${x.count} fără ${channelTitle(x.ch)}`);
    const un = this.unreachable().length;
    return parts.join(' · ') + (un ? ` · ${un} nu pot fi contactați pe niciun canal ales` : '');
  });

  fallbackLabel = computed(() => {
    const un = this.unreachable().length;
    return (
      'Mesajul pleacă doar pe canalele disponibile pentru fiecare destinatar' +
      (un ? ` (${un} fără niciun canal au fost excluși)` : '') + '.'
    );
  });

  gapOf(p: Recipient): { label: string; hard: boolean } | null {
    if (!this.isSelected(p.id) || this.c.overrides()[p.id]) return null;
    const missing = this.missingFor(p);
    if (!missing.length) return null;
    return { label: missing.map(channelTitle).join(' și '), hard: missing.length === this.c.channels().length };
  }

  fixOf(p: Recipient): string | null {
    const ov = this.c.overrides()[p.id];
    return this.isSelected(p.id) && ov ? ov.map(channelTitle).join(' + ') : null;
  }

  excludeConflicts(): void {
    const ids = this.conflictPeople().map(p => p.id);
    this.c.setMany(ids, false);
    this.c.conflictMode.set('block');
    this.toast.show(`${ids.length} destinatari excluși.`);
  }

  useFallback(): void {
    const un = this.unreachable().map(p => p.id);
    if (un.length) this.c.setMany(un, false);
    this.c.conflictMode.set('fallback');
    this.toast.show('Se trimite pe canalele disponibile.');
  }

  openPerson(e: Event, p: Recipient): void {
    e.stopPropagation();
    const ov = this.c.overrides()[p.id];
    this.pick.set(ov ? [...ov] : this.c.channels().filter(ch => p.channels.includes(ch)));
    this.person.set(p);
  }

  togglePick(ch: Channel): void {
    const cur = this.pick();
    this.pick.set(cur.includes(ch) ? cur.filter(x => x !== ch) : [...cur, ch]);
  }

  savePerson(p: Recipient): void {
    this.c.overrides.set({ ...this.c.overrides(), [p.id]: [...this.pick()] });
    this.person.set(null);
    this.toast.show('Canale setate pentru acest destinatar.');
  }

  excludePerson(p: Recipient): void {
    const next = { ...this.c.overrides() };
    delete next[p.id];
    this.c.overrides.set(next);
    this.c.setMany([p.id], false);
    this.person.set(null);
    this.toast.show('Destinatar exclus.');
  }

  // ------------------------------------------------------------ trimitere

  canSend = computed(() => this.selectedCount() > 0 && !this.conflictOpen());

  channelsLabel = computed(() => this.c.channels().map(channelTitle).join(' + '));

  confirmText = computed(() => {
    const names = this.c.channels().map(channelTitle);
    const phrase = names.length > 1 ? names.slice(0, -1).join(', ') + ' și ' + names[names.length - 1] : names[0];
    const n = this.selectedCount();
    return `Mesajul „${this.c.subject().trim() || 'fără subiect'}” va fi trimis prin ${phrase} către ${n} ${n === 1 ? 'destinatar' : 'destinatari'}. Acțiunea nu poate fi anulată.`;
  });

  askSend(): void {
    if (!this.selectedCount()) return this.toast.show('Bifează cel puțin o persoană.');
    if (this.conflictOpen()) return this.toast.show('Rezolvă mai întâi conflictele de canal.');
    this.confirming.set(true);
  }

  doSend(): void {
    this.sending.set(true);
    this.api
      .send({
        message: {
          subject: this.c.subject(),
          bodyHtml: this.c.bodyHtml(),
          channels: this.c.channels(),
          templateId: this.c.templateId(),
          attachments: this.c.attachments().map(({ url, ...rest }) => rest),
        },
        recipientIds: this.c.selected(),
        channelOverrides: this.c.overrides(),
      })
      .subscribe({
        next: r => {
          this.sending.set(false);
          this.confirming.set(false);
          this.result.set(r);
          this.store.loadSent();
          this.store.loadOverview();
          const draft = this.c.draftId();
          if (draft) {
            // Ciorna si-a atins scopul: mesajul a plecat, nu mai are ce cauta in lista.
            this.api.deleteDraft(draft).subscribe({ next: () => this.store.loadDrafts(), error: () => undefined });
          }
        },
        error: e => {
          this.sending.set(false);
          this.confirming.set(false);
          this.toast.error(e);
        },
      });
  }

  again(): void {
    this.result.set(null);
    this.c.reset();
    this.router.navigate(['/mesaj/sablon']);
  }

  goSent(): void {
    this.result.set(null);
    this.c.reset();
    this.router.navigate(['/trimise']);
  }
}
