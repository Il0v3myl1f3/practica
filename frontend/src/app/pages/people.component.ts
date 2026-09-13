import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { COL, EMAIL_RE, downloadCsv, findColumn, parseCsv } from '../core/csv';
import { pagerItems, plural } from '../core/format';
import { Recipient, RecipientUpsert, TelegramContact } from '../core/models';
import { IconComponent } from '../shared/icon.component';
import { SelectComponent, SelectOption } from '../shared/select.component';
import { Check, ChevronLeft, ChevronRight, Download, Pencil, Plus, RotateCcw, Search, Send, Trash2, Upload } from '../shared/icons';

const PAGE = 10;
const COLS = 'minmax(160px, 2fr) minmax(0, 1.4fr) 150px 110px 88px';

interface Staged extends RecipientUpsert {
  key: number;
}

/** Modalul de editare serveste si lista reala, si randurile citite din fisier. */
type EditTarget = { scope: 'people'; id: number | null } | { scope: 'staged'; key: number };

@Component({
  selector: 'app-people',
  standalone: true,
  imports: [FormsModule, IconComponent, SelectComponent],
  template: `
    @if (staged() !== null) {
      <!-- Verificarea importului: randurile citite din fisier, editabile inainte de a fi scrise. -->
      <section class="shell">
        <div class="toolbar">
          <div class="grow">
            <b>{{ plural(staged()!.length, 'destinatar nou', 'destinatari noi') }}</b>
            <div class="notes">
              @for (n of stagedNotes(); track n) {
                <div class="note">{{ n }}</div>
              }
            </div>
          </div>
          <button type="button" class="btn btn-ghost sm" (click)="cancelImport()">Renunță</button>
          <button type="button" class="btn btn-primary sm" [disabled]="!staged()!.length" (click)="finishImport()">
            Finalizează importul
          </button>
        </div>

        <div class="tbody">
          <div class="row head" [style.gridTemplateColumns]="stagedCols" [style.minWidth]="'640px'">
            <span>Nume</span><span>Email</span><span>Grup</span><span></span>
          </div>
          @for (s of staged(); track s.key) {
            <div class="row" [style.gridTemplateColumns]="stagedCols" [style.minWidth]="'640px'">
              <span class="cell-strong">{{ s.firstName }} {{ s.lastName }}</span>
              <span class="cell-muted">{{ s.email }}</span>
              <span>{{ s.group }}</span>
              <span class="cell-actions">
                <button type="button" class="icon-btn" title="Corectează linia" (click)="openEditStaged(s)">
                  <app-icon [icon]="I.Pencil" />
                </button>
                <button type="button" class="icon-btn" title="Scoate din import" (click)="dropStaged(s)">
                  <app-icon [icon]="I.Trash2" />
                </button>
              </span>
            </div>
          } @empty {
            <div class="empty">Nu a mai rămas nicio linie de importat.</div>
          }
        </div>
      </section>
    } @else {
      <section class="shell">
        <div class="toolbar">
          <label class="search-wrap">
            <app-icon [icon]="I.Search" [size]="16" />
            <input placeholder="Caută nume, email sau grup…" [ngModel]="q()" (ngModelChange)="q.set($event); page.set(0)" />
          </label>

          <app-select
            label="Grup"
            [options]="groupOptions()"
            [value]="groupFilter()"
            (valueChange)="groupFilter.set($event); page.set(0)"
          />

          @if (filtersActive()) {
            <button type="button" class="btn btn-ghost sm" (click)="resetFilters()">Curăță filtrele</button>
          }

          <span class="grow"></span>
          <button type="button" class="btn btn-ghost sm" (click)="file.click()">
            <app-icon [icon]="I.Upload" [size]="15" />Importă CSV
          </button>
          <button type="button" class="btn btn-ghost sm" (click)="exportCsv()">
            <app-icon [icon]="I.Download" [size]="15" />Exportă CSV
          </button>
          <button type="button" class="btn btn-ghost sm" (click)="openTelegram()">
            <app-icon [icon]="I.Send" [size]="15" />Conectează Telegram
          </button>
          <button type="button" class="btn btn-primary sm" (click)="openNew()">
            <app-icon [icon]="I.Plus" [size]="15" />Adaugă
          </button>
          <input #file type="file" accept=".csv,text/csv" hidden (change)="onFile($event)" />
        </div>

        <div class="tbody">
          <div class="row head" [style.gridTemplateColumns]="cols" [style.minWidth]="'740px'">
            <span>Nume</span><span>Email</span><span>Grup</span><span>Canale</span><span></span>
          </div>

          @for (p of slice(); track p.id) {
            <div class="row" [style.gridTemplateColumns]="cols" [style.minWidth]="'740px'">
              <span class="cell-strong">{{ p.name }}</span>
              <span class="cell-muted">{{ p.email }}</span>
              <span>{{ p.group }}</span>
              <span class="chans">
                @for (c of p.channels; track c) {
                  <span class="chan" [title]="c">{{ short(c) }}</span>
                }
              </span>
              <span class="cell-actions">
                <button type="button" class="icon-btn" title="Editează" (click)="openEdit(p)">
                  <app-icon [icon]="I.Pencil" />
                </button>
                <button type="button" class="icon-btn" title="Șterge" (click)="toDelete.set(p)">
                  <app-icon [icon]="I.Trash2" />
                </button>
              </span>
            </div>
          }

          @if (!filtered().length) {
            <div class="empty">Nicio persoană nu corespunde căutării.</div>
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
    }

    @if (editing()) {
      <div class="backdrop" (click)="closeEdit($event)">
        <div class="modal" (click)="$event.stopPropagation()">
          <h3>{{ editTitle() }}</h3>
          <div class="form">
            <div class="two">
              <label class="field"><span>Prenume</span><input class="input" [(ngModel)]="form.firstName" /></label>
              <label class="field"><span>Nume</span><input class="input" [(ngModel)]="form.lastName" /></label>
            </div>
            <label class="field"><span>Email</span><input class="input" type="email" [(ngModel)]="form.email" /></label>
            <label class="field">
              <span>Grup</span>
              <input class="input" list="grp-list" [(ngModel)]="form.group" placeholder="Marketing" />
              <datalist id="grp-list">
                @for (g of store.groups(); track g.id) {
                  <option [value]="g.name"></option>
                }
              </datalist>
            </label>
            <div class="two">
              <label class="field">
                <span>Telefon WhatsApp</span>
                <input class="input" [(ngModel)]="form.phoneNumber" placeholder="+373…" />
              </label>
              <label class="field">
                <span>Telegram chat ID</span>
                <input class="input" [(ngModel)]="form.telegramChatId" placeholder="opțional" />
              </label>
            </div>
            <p class="hint">Canalele fără adresă completată nu apar la selecția destinatarilor.</p>
            @if (editError()) {
              <div class="alert">{{ editError() }}</div>
            }
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="editing.set(null)">Anulează</button>
            <button type="button" class="btn btn-primary" (click)="save()">Salvează</button>
          </div>
        </div>
      </div>
    }

    @if (telegramOpen()) {
      <!-- Conectarea chat-urilor: un bot nu poate scrie primul, deci chat ID-ul vine
           din mesajul trimis de om botului. -->
      <div class="backdrop" (click)="closeTelegram($event)">
        <div class="modal tg" (click)="$event.stopPropagation()">
          <h3>Conectează Telegram</h3>
          <p class="modal-sub">
            Un bot nu poate scrie primul. Fiecare persoană deschide botul, apasă <b>Start</b>, și abia
            apoi apare aici — cu chat ID-ul ei, pe care îl legi la destinatarul potrivit.
          </p>

          @if (botLink()) {
            <p class="tg-link">
              Linkul de trimis oamenilor:
              <a [href]="botLink()" target="_blank" rel="noopener">{{ botLink() }}</a>
            </p>
          }

          @if (tgError()) {
            <div class="alert">{{ tgError() }}</div>
          }

          @if (tgLoading()) {
            <div class="empty">Se citesc contactele botului…</div>
          } @else if (!tgError()) {
            <div class="tg-list">
              @for (c of contacts(); track c.chatId) {
                <div class="tg-row">
                  <span class="tg-who">
                    <span class="cell-strong">{{ c.name }}</span>
                    <span class="cell-muted">{{ c.username ? '@' + c.username + ' · ' : '' }}{{ c.chatId }}</span>
                  </span>
                  @if (c.recipientId) {
                    <span class="tg-done">
                      <app-icon [icon]="I.Check" [size]="14" />{{ c.recipientName }}
                    </span>
                  } @else {
                    <select
                      class="input"
                      [ngModel]="pick()[c.chatId] ?? ''"
                      (ngModelChange)="choose(c.chatId, $event)"
                    >
                      <option value="">Alege destinatarul…</option>
                      @for (p of store.recipients(); track p.id) {
                        <option [value]="p.id">{{ p.name }} — {{ p.email }}</option>
                      }
                    </select>
                    <button
                      type="button"
                      class="btn btn-primary sm"
                      [disabled]="!pick()[c.chatId] || linking() === c.chatId"
                      (click)="linkContact(c)"
                    >
                      Leagă
                    </button>
                  }
                </div>
              } @empty {
                <div class="empty">Nimeni nu i-a scris botului încă.</div>
              }
            </div>
          }

          <p class="hint">
            Telegram ține mesajele neconfirmate circa 24 de ore. Dacă o persoană nu mai apare în listă,
            roagă-o să scrie din nou botului.
          </p>

          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="loadTelegram()">
              <app-icon [icon]="I.RotateCcw" [size]="15" />Reîncarcă
            </button>
            <span class="grow"></span>
            <button type="button" class="btn btn-ghost" (click)="telegramOpen.set(false)">Închide</button>
          </div>
        </div>
      </div>
    }

    @if (toDelete(); as d) {
      <div class="backdrop">
        <div class="modal" style="max-width:440px">
          <h3>Ștergi acest destinatar?</h3>
          <p style="margin:0 0 4px">{{ d.name }}</p>
          <p class="modal-sub">{{ d.email }} — acțiunea nu poate fi anulată.</p>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="toDelete.set(null)">Anulează</button>
            <button type="button" class="btn btn-danger" (click)="confirmDelete(d)">Șterge destinatarul</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .grow { flex: 1 1 auto; min-width: 0; }
      .chans { display: flex; gap: 4px; }
      .chan {
        width: 22px;
        height: 22px;
        border-radius: 9999px;
        background: var(--brand-soft);
        color: var(--brand-ink);
        font-size: 11px;
        font-weight: 700;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
      .form { display: flex; flex-direction: column; gap: 16px; }
      .two { display: flex; gap: 12px; flex-wrap: wrap; }
      .two > .field { flex: 1 1 160px; }
      .hint { margin: -6px 0 0; font-size: 12px; color: var(--muted); }
      .notes { margin-top: 4px; }
      .note { font-size: 13px; color: var(--warn-ink); }

      .modal.tg { max-width: 620px; display: flex; flex-direction: column; gap: 12px; }
      .tg-link { margin: 0; font-size: 13px; }
      .tg-link a { color: var(--brand-ink); }
      .tg-list { max-height: 46vh; overflow-y: auto; display: flex; flex-direction: column; }
      .tg-row {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 2px;
        border-bottom: 1px solid var(--line);
      }
      .tg-row:last-child { border-bottom: 0; }
      .tg-who { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
      .tg-who > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .tg-row select { flex: 0 1 230px; min-width: 0; }
      .tg-done {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 600;
        color: var(--ok-ink);
      }
    `,
  ],
})
export class PeopleComponent {
  readonly store = inject(Store);
  private api = inject(ApiService);
  private toast = inject(ToastService);

  readonly cols = COLS;
  readonly stagedCols = 'minmax(160px, 2fr) minmax(0, 1.4fr) 150px 88px';
  readonly plural = plural;
  readonly I = { Search, Trash2, Upload, Download, Plus, Pencil, ChevronLeft, ChevronRight, Send, Check, RotateCcw };

  readonly q = signal('');
  readonly groupFilter = signal('');
  readonly page = signal(0);

  readonly editing = signal<EditTarget | null>(null);
  readonly editError = signal('');
  form: RecipientUpsert = blank();

  readonly toDelete = signal<Recipient | null>(null);

  readonly staged = signal<Staged[] | null>(null);
  readonly stagedNotes = signal<string[]>([]);

  // --- conectarea chat-urilor de Telegram
  readonly telegramOpen = signal(false);
  readonly tgLoading = signal(false);
  readonly tgError = signal('');
  readonly contacts = signal<TelegramContact[]>([]);
  readonly botUsername = signal<string | null>(null);
  /** chatId -> id-ul destinatarului ales în select, cât timp nu s-a apăsat Leagă. */
  readonly pick = signal<Record<string, string>>({});
  readonly linking = signal<string | null>(null);

  constructor() {
    this.store.loadRecipients();
    this.store.loadGroups();
  }

  filtered = computed(() => {
    const q = this.q().trim().toLowerCase();
    const g = this.groupFilter();
    return this.store
      .recipients()
      .filter(p => (!g || p.group === g) && (!q || `${p.name} ${p.email} ${p.group}`.toLowerCase().includes(q)));
  });

  pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / PAGE)));
  slice = computed(() => {
    const p = Math.min(this.page(), this.pageCount() - 1);
    return this.filtered().slice(p * PAGE, p * PAGE + PAGE);
  });
  pages = computed(() => pagerItems(Math.min(this.page(), this.pageCount() - 1), this.pageCount()));

  rangeLabel = computed(() => {
    const total = this.filtered().length;
    if (!total) return 'Nicio persoană';
    const p = Math.min(this.page(), this.pageCount() - 1);
    return `${p * PAGE + 1}–${Math.min(total, p * PAGE + PAGE)} din ${total} persoane`;
  });

  filtersActive = computed(() => !!this.groupFilter() || !!this.q().trim());

  groupOptions = computed<SelectOption[]>(() => [
    { id: '', title: 'Toate grupurile', count: this.store.recipients().length },
    ...this.store.groups().map(g => ({ id: g.name, title: g.name, count: g.count })),
  ]);

  short(c: string): string {
    return { EMAIL: '@', TELEGRAM: 'TG', WHATSAPP: 'WA' }[c] ?? c[0];
  }

  resetFilters(): void {
    this.q.set('');
    this.groupFilter.set('');
    this.page.set(0);
  }

  // ------------------------------------------------------------ editare

  editTitle = computed(() => {
    const e = this.editing();
    if (!e) return '';
    if (e.scope === 'staged') return 'Corectează linia din import';
    return e.id ? 'Editează destinatarul' : 'Destinatar nou';
  });

  openNew(): void {
    this.form = blank();
    this.form.group = this.store.groupNames()[0] ?? '';
    this.editError.set('');
    this.editing.set({ scope: 'people', id: null });
  }

  openEditStaged(s: Staged): void {
    this.form = {
      firstName: s.firstName,
      lastName: s.lastName,
      email: s.email,
      group: s.group,
      phoneNumber: s.phoneNumber ?? '',
      telegramChatId: s.telegramChatId ?? '',
    };
    this.editError.set('');
    this.editing.set({ scope: 'staged', key: s.key });
  }

  openEdit(p: Recipient): void {
    this.form = {
      firstName: p.firstName,
      lastName: p.lastName,
      email: p.email,
      group: p.group,
      phoneNumber: p.phoneNumber ?? '',
      telegramChatId: p.telegramChatId ?? '',
    };
    this.editError.set('');
    this.editing.set({ scope: 'people', id: p.id });
  }

  closeEdit(e: Event): void {
    if (e.target === e.currentTarget) this.editing.set(null);
  }

  save(): void {
    const f = this.form;
    if (!f.firstName.trim() || !f.lastName.trim()) {
      return this.editError.set('Numele și prenumele sunt obligatorii.');
    }
    if (!EMAIL_RE.test(f.email.trim())) {
      return this.editError.set('Adresa de email nu este validă.');
    }
    if (!f.group.trim()) {
      return this.editError.set('Alege un grup.');
    }
    const target = this.editing();
    if (!target) return;

    if (target.scope === 'staged') {
      // Randul din import nu a fost inca scris in baza: se corecteaza local.
      // Emailul trebuie sa ramana unic si fata de lista reala, si fata de
      // celelalte randuri din acelasi fisier.
      const email = f.email.trim().toLowerCase();
      const clash =
        this.store.recipients().some(p => p.email.toLowerCase() === email) ||
        (this.staged() ?? []).some(s => s.key !== target.key && s.email.toLowerCase() === email);
      if (clash) {
        return this.editError.set('Un alt destinatar are deja acest email.');
      }
      this.staged.set((this.staged() ?? []).map(s => (s.key === target.key ? { ...s, ...f, key: s.key } : s)));
      this.editing.set(null);
      this.toast.show('Linie corectată.');
      return;
    }

    const done = () => {
      this.editing.set(null);
      this.store.loadRecipients();
      this.store.loadGroups();
      this.toast.show(target.id ? 'Destinatar actualizat.' : 'Destinatar adăugat.');
    };
    const fail = (e: unknown) => this.editError.set(errMessage(e));
    if (target.id) {
      this.api.updateRecipient(target.id, f).subscribe({ next: done, error: fail });
    } else {
      this.api.createRecipient(f).subscribe({ next: done, error: fail });
    }
  }

  confirmDelete(p: Recipient): void {
    this.api.deleteRecipient(p.id).subscribe({
      next: () => {
        this.toDelete.set(null);
        this.store.loadRecipients();
        this.store.loadGroups();
        this.toast.show('Destinatar șters.');
      },
      error: e => {
        this.toDelete.set(null);
        this.toast.error(e);
      },
    });
  }

  // ------------------------------------------------------------ Telegram

  botLink = computed(() => (this.botUsername() ? `https://t.me/${this.botUsername()}` : ''));

  openTelegram(): void {
    this.telegramOpen.set(true);
    this.pick.set({});
    this.loadTelegram();
  }

  closeTelegram(e: Event): void {
    if (e.target === e.currentTarget) this.telegramOpen.set(false);
  }

  loadTelegram(): void {
    this.tgLoading.set(true);
    this.tgError.set('');
    this.api.telegramContacts().subscribe({
      next: d => {
        this.botUsername.set(d.botUsername);
        this.contacts.set(d.contacts ?? []);
        this.tgLoading.set(false);
      },
      error: e => {
        this.contacts.set([]);
        this.tgError.set(errMessage(e, 'Contactele botului nu au putut fi citite.'));
        this.tgLoading.set(false);
      },
    });
  }

  choose(chatId: string, recipientId: string): void {
    this.pick.set({ ...this.pick(), [chatId]: recipientId });
  }

  linkContact(c: TelegramContact): void {
    const recipientId = Number(this.pick()[c.chatId]);
    if (!recipientId) return;
    this.linking.set(c.chatId);
    this.api.linkTelegram(c.chatId, recipientId).subscribe({
      next: () => {
        this.linking.set(null);
        this.store.loadRecipients();
        // Reîncărcăm lista, ca rândul să apară imediat ca legat.
        this.loadTelegram();
        this.toast.show('Chat Telegram legat.');
      },
      error: e => {
        this.linking.set(null);
        this.tgError.set(errMessage(e, 'Legarea nu a reușit.'));
      },
    });
  }

  // ---------------------------------------------------------------- CSV

  exportCsv(): void {
    const rows = [['nume', 'prenume', 'email', 'grup'], ...this.store.recipients().map(p => [p.lastName, p.firstName, p.email, p.group])];
    downloadCsv(rows, 'destinatari');
    this.toast.show(`${this.store.recipients().length} destinatari exportați în CSV.`);
  }

  onFile(e: Event): void {
    const input = e.target as HTMLInputElement;
    const f = input.files?.[0];
    input.value = '';
    if (!f) return;

    const reader = new FileReader();
    reader.onerror = () => this.toast.show('Fișierul nu a putut fi citit.');
    reader.onload = () => {
      const rows = parseCsv(String(reader.result ?? ''));
      if (rows.length < 2) return this.toast.show('Fișierul este gol sau are doar antetul.');

      const head = rows[0];
      const idx = {
        last: findColumn(head, COL.last),
        first: findColumn(head, COL.first),
        email: findColumn(head, COL.email),
        group: findColumn(head, COL.group),
      };
      const missing = (['last', 'first', 'email', 'group'] as const)
        .filter(k => idx[k] < 0)
        .map(k => ({ last: 'nume', first: 'prenume', email: 'email', group: 'grup' })[k]);
      if (missing.length) {
        return this.toast.show(`Lipsesc coloanele obligatorii: ${missing.join(', ')}.`);
      }

      const existing = new Set(this.store.recipients().map(p => p.email.toLowerCase()));
      const seen = new Set<string>();
      const added: Staged[] = [];
      const invalid: string[] = [];
      let duplicates = 0;

      rows.slice(1).forEach((r, i) => {
        const get = (k: keyof typeof idx) => (r[idx[k]] ?? '').trim();
        const last = get('last');
        const first = get('first');
        const email = get('email');
        const group = get('group');
        const line = i + 2;
        if (!last || !first || !email || !group) {
          invalid.push(`Linia ${line}: câmpuri lipsă`);
          return;
        }
        if (!EMAIL_RE.test(email)) {
          invalid.push(`Linia ${line}: email invalid (${email})`);
          return;
        }
        const key = email.toLowerCase();
        if (existing.has(key) || seen.has(key)) {
          duplicates++;
          return;
        }
        seen.add(key);
        added.push({ key: line, firstName: first, lastName: last, email, group });
      });

      if (!added.length) {
        return this.toast.show(
          `Nicio linie validă. ${duplicates ? duplicates + ' duplicate. ' : ''}${invalid.length ? invalid.length + ' respinse.' : ''}`,
        );
      }

      const notes: string[] = [];
      if (duplicates) notes.push(`${duplicates} linii sărite — emailul există deja în listă.`);
      if (invalid.length) {
        notes.push(`${invalid.length} linii respinse: ${invalid.slice(0, 3).join('; ')}${invalid.length > 3 ? ' …' : ''}`);
      }
      this.stagedNotes.set(notes);
      this.staged.set(added);
    };
    reader.readAsText(f, 'utf-8');
  }

  dropStaged(s: Staged): void {
    this.staged.set((this.staged() ?? []).filter(x => x.key !== s.key));
  }

  cancelImport(): void {
    this.staged.set(null);
    this.stagedNotes.set([]);
  }

  finishImport(): void {
    const rows = this.staged() ?? [];
    this.api.importRecipients(rows.map(({ key, ...rest }) => rest)).subscribe({
      next: created => {
        this.cancelImport();
        this.store.loadRecipients();
        this.store.loadGroups();
        this.page.set(0);
        this.toast.show(`${plural(created.length, 'destinatar importat', 'destinatari importați')}.`);
      },
      error: e => this.toast.error(e),
    });
  }
}

function blank(): RecipientUpsert {
  return { firstName: '', lastName: '', email: '', group: '', phoneNumber: '', telegramChatId: '' };
}

/** `detail` e textul în română; `message` e doar cheia erorii (vezi ToastService). */
function errMessage(e: unknown, fallback = 'Salvarea nu a reușit.'): string {
  const body = (e as { error?: { message?: string; detail?: string } })?.error;
  return body?.detail ?? body?.message ?? fallback;
}
