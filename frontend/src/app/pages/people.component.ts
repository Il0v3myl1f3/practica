import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '../core/api.service';
import { Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { COL, EMAIL_RE, downloadCsv, findColumn, parseCsv } from '../core/csv';
import { pagerItems, plural } from '../core/format';
import { DiscordDirectory, DiscordMember, Recipient, RecipientUpsert, TelegramContact } from '../core/models';
import { IconComponent } from '../shared/icon.component';
import { SelectComponent, SelectOption } from '../shared/select.component';
import {
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Download,
  Ellipsis,
  Pencil,
  Plug,
  Plus,
  RotateCcw,
  Search,
  Send,
  Trash2,
  Upload,
  X,
} from '../shared/icons';

const PAGE = 10;
const COLS = 'minmax(160px, 2fr) minmax(0, 1.4fr) 150px 110px 88px';

interface Staged extends RecipientUpsert {
  key: number;
}

/** Linie citita din fisier, cat timp celulele Telegram si Discord sunt inca text brut. */
interface Draft extends Staged {
  rawTg: string;
  rawDiscord: string;
}

const CHAT_ID_RE = /^-?\d+$/;

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
          <div class="row head" [style.gridTemplateColumns]="cols" [style.minWidth]="'740px'">
            <span>Nume</span><span>Email</span><span>Grup</span><span>Canale</span><span></span>
          </div>
          @for (s of staged(); track s.key) {
            <div class="row" [style.gridTemplateColumns]="cols" [style.minWidth]="'740px'">
              <span class="cell-strong">{{ s.firstName }} {{ s.lastName }}</span>
              <span class="cell-muted">{{ s.email }}</span>
              <span>{{ s.group }}</span>
              <span class="chans">
                @for (c of stagedChannels(s); track c) {
                  <span class="chan" [title]="c">{{ short(c) }}</span>
                }
              </span>
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
        <div class="toolbar people-toolbar">
          <div class="tb-row">
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
          </div>

          <div class="tb-row">
            <div class="menu-wrap" data-menu>
              <button type="button" class="btn btn-ghost sm" (click)="toggleMenu('channel')">
                <app-icon [icon]="I.Plug" [size]="15" />Conectează canal<app-icon [icon]="I.ChevronDown" [size]="13" />
              </button>
              @if (openMenu() === 'channel') {
                <div class="menu">
                  <button type="button" class="menu-item" (click)="openMenu.set(null); openTelegram()">
                    <app-icon [icon]="I.Send" [size]="15" />Conectează Telegram
                  </button>
                  <button type="button" class="menu-item" (click)="openMenu.set(null); openDiscord()">
                    <app-icon [icon]="I.Send" [size]="15" />Conectează Discord
                  </button>
                </div>
              }
            </div>

            <div class="menu-wrap" data-menu>
              <button type="button" class="icon-btn" title="Mai multe" (click)="toggleMenu('more')">
                <app-icon [icon]="I.Ellipsis" [size]="16" />
              </button>
              @if (openMenu() === 'more') {
                <div class="menu">
                  <button type="button" class="menu-item" (click)="openMenu.set(null); file.click()">
                    <app-icon [icon]="I.Upload" [size]="15" />Importă CSV
                  </button>
                  <button type="button" class="menu-item" (click)="openMenu.set(null); exportCsv()">
                    <app-icon [icon]="I.Download" [size]="15" />Exportă CSV
                  </button>
                </div>
              }
            </div>

            @if (filtersActive()) {
              <button type="button" class="btn btn-ghost sm" (click)="resetFilters()">Curăță filtrele</button>
            }

            <span class="grow"></span>

            <button type="button" class="btn btn-primary sm" (click)="openNew()">
              <app-icon [icon]="I.Plus" [size]="15" />Adaugă contact
            </button>
          </div>

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
              <button type="button" class="input group-trigger" (click)="openGroupPicker()">
                <span class="group-trigger-value">{{ form.group || 'Alege un grup' }}</span>
                <app-icon [icon]="I.ChevronDown" [size]="15" />
              </button>
            </label>
            <div class="two">
              <label class="field">
                <span>Telegram chat ID</span>
                <input class="input" [(ngModel)]="form.telegramChatId" placeholder="opțional" />
              </label>
              <label class="field">
                <span>Discord user ID</span>
                <input class="input" [(ngModel)]="form.discordUserId" placeholder="opțional" />
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

    @if (groupPickerOpen()) {
      <div class="backdrop" (click)="closeGroupPicker($event)">
        <div class="modal" style="max-width:420px" (click)="$event.stopPropagation()">
          <h3>Alege un grup</h3>
          <div class="form">
            <label class="field">
              <span>Caută sau scrie un grup nou</span>
              <input
                class="input"
                [ngModel]="groupSearch()"
                (ngModelChange)="groupSearch.set($event)"
                placeholder="Marketing"
                autocomplete="off"
              />
            </label>
            <div class="group-list">
              @for (g of groupPickerOptions(); track g) {
                <button type="button" class="menu-item" [class.on]="g === form.group" (click)="pickGroup(g)">{{ g }}</button>
              }
              @if (groupSearchIsNew()) {
                <button type="button" class="menu-item" (click)="pickGroup(groupSearch().trim())">
                  <app-icon [icon]="I.Plus" [size]="14" />Creează „{{ groupSearch().trim() }}”
                </button>
              }
              @if (!groupPickerOptions().length && !groupSearchIsNew()) {
                <div class="empty">Niciun grup încă.</div>
              }
            </div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="groupPickerOpen.set(false)">Închide</button>
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
                  @if (c.recipientId && editingChat() !== c.chatId) {
                    <span class="tg-done">
                      <app-icon [icon]="I.Check" [size]="14" />{{ c.recipientName }}
                    </span>
                    <button type="button" class="icon-btn" title="Schimbă destinatarul" (click)="startRelinkTelegram(c)">
                      <app-icon [icon]="I.Pencil" [size]="14" />
                    </button>
                    <button type="button" class="icon-btn" title="Dezleagă" (click)="unlinkTelegram(c)">
                      <app-icon [icon]="I.X" [size]="14" />
                    </button>
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
                      (click)="c.recipientId ? relinkTelegram(c) : linkContact(c)"
                    >
                      Leagă
                    </button>
                    @if (c.recipientId) {
                      <button type="button" class="btn btn-ghost sm" (click)="editingChat.set(null)">Anulează</button>
                    }
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

    @if (discordOpen()) {
      <!-- Structura si clasele CSS (tg-*) sunt cele de la modalul Telegram de mai sus. -->
      <div class="backdrop" (click)="closeDiscord($event)">
        <div class="modal tg" (click)="$event.stopPropagation()">
          <h3>Conectează Discord</h3>
          <p class="modal-sub">
            Botul listează membrii serverului. Alege destinatarul pentru fiecare membru și apasă
            <b>Leagă</b>.
          </p>

          @if (guildName()) {
            <p class="tg-link">Server: <b>{{ guildName() }}</b></p>
          }
          @if (inviteUrl()) {
            <p class="tg-link">
              Linkul de invitare pe server:
              <a [href]="inviteUrl()" target="_blank" rel="noopener">{{ inviteUrl() }}</a>
            </p>
          }

          @if (dcError()) {
            <div class="alert">{{ dcError() }}</div>
          }

          @if (dcLoading()) {
            <div class="empty">Se citesc membrii serverului…</div>
          } @else if (!dcError()) {
            <div class="tg-list">
              @for (m of members(); track m.userId) {
                <div class="tg-row">
                  <span class="tg-who">
                    <span class="cell-strong">{{ m.name }}</span>
                    <span class="cell-muted">{{ m.username ? '@' + m.username + ' · ' : '' }}{{ m.userId }}</span>
                  </span>
                  @if (m.recipientId && editingMember() !== m.userId) {
                    <span class="tg-done">
                      <app-icon [icon]="I.Check" [size]="14" />{{ m.recipientName }}
                    </span>
                    <button type="button" class="icon-btn" title="Schimbă destinatarul" (click)="startRelinkDiscord(m)">
                      <app-icon [icon]="I.Pencil" [size]="14" />
                    </button>
                    <button type="button" class="icon-btn" title="Dezleagă" (click)="unlinkDiscord(m)">
                      <app-icon [icon]="I.X" [size]="14" />
                    </button>
                  } @else {
                    <select
                      class="input"
                      [ngModel]="dcPick()[m.userId] ?? ''"
                      (ngModelChange)="chooseDiscord(m.userId, $event)"
                    >
                      <option value="">Alege destinatarul…</option>
                      @for (p of store.recipients(); track p.id) {
                        <option [value]="p.id">{{ p.name }} — {{ p.email }}</option>
                      }
                    </select>
                    <button
                      type="button"
                      class="btn btn-primary sm"
                      [disabled]="!dcPick()[m.userId] || dcLinking() === m.userId"
                      (click)="m.recipientId ? relinkDiscord(m) : linkDiscordMember(m)"
                    >
                      Leagă
                    </button>
                    @if (m.recipientId) {
                      <button type="button" class="btn btn-ghost sm" (click)="editingMember.set(null)">Anulează</button>
                    }
                  }
                </div>
              } @empty {
                <div class="empty">Niciun membru pe server încă.</div>
              }
            </div>
          }

          <p class="hint">
            Botul poate trimite mesaje private doar cuiva care e pe același server cu el și nu a
            dezactivat DM-urile de la membrii serverului.
          </p>

          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="loadDiscord()">
              <app-icon [icon]="I.RotateCcw" [size]="15" />Reîncarcă
            </button>
            <span class="grow"></span>
            <button type="button" class="btn btn-ghost" (click)="discordOpen.set(false)">Închide</button>
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
      .people-toolbar { flex-direction: column; align-items: stretch; gap: 10px; }
      /* flex-wrap: wrap aici producea un artefact real in Chromium: cand search-wrap
         (flex-grow:1) umple exact spatiul ramas, fara slack, motorul de layout rupe
         randul pe 2 linii "invizibile" (aceeasi pozitie Y, dar inaltime dubla), lasand
         un gol sub el - reprodus si eliminat confirmat cu flex-wrap: nowrap. Randurile
         astea au mereu exact 2 elemente, nu au nevoie sa se rupa pe linii. */
      .tb-row { display: flex; align-items: center; gap: 8px 12px; flex-wrap: nowrap; }
      .menu-wrap { position: relative; }
      .menu {
        position: absolute;
        z-index: 30;
        top: calc(100% + 4px);
        left: 0;
        min-width: 210px;
        max-height: 240px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 2px;
        padding: 8px;
        background: var(--surface);
        border-radius: var(--r-lg);
        box-shadow: var(--shadow-menu);
      }
      .menu-item {
        display: flex;
        align-items: center;
        gap: 8px;
        min-height: 36px;
        padding: 6px 10px;
        border: none;
        border-radius: var(--r-md);
        background: var(--surface);
        color: var(--ink-2);
        font-size: 13px;
        text-align: left;
        cursor: pointer;
      }
      .menu-item:hover { background: var(--line-soft); }
      .menu-item.on { background: var(--brand-soft); color: var(--brand-ink); }
      .group-trigger {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        cursor: pointer;
        text-align: left;
        color: var(--ink);
      }
      .group-trigger-value { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .group-list {
        max-height: 260px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 2px;
        border: 1px solid var(--line);
        border-radius: var(--r-md);
        padding: 6px;
      }
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
  readonly plural = plural;
  readonly I = {
    Search,
    Trash2,
    Upload,
    Download,
    Plus,
    Pencil,
    ChevronLeft,
    ChevronRight,
    ChevronDown,
    Send,
    Check,
    RotateCcw,
    Plug,
    Ellipsis,
    X,
  };

  readonly q = signal('');
  readonly groupFilter = signal('');
  readonly page = signal(0);
  /** Meniul deschis din bara: legarea canalelor sau import/export. */
  readonly openMenu = signal<'channel' | 'more' | null>(null);

  readonly editing = signal<EditTarget | null>(null);
  readonly editError = signal('');
  form: RecipientUpsert = blank();
  /** Modalul de alegere a grupului (nu datalist nativ - nestilizabil cross-browser). */
  readonly groupPickerOpen = signal(false);
  readonly groupSearch = signal('');

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
  /** chatId-ul aflat in modul "schimbă destinatarul" - readus la select in loc de bifa verde. */
  readonly editingChat = signal<string | null>(null);

  // --- conectarea membrilor de Discord
  readonly discordOpen = signal(false);
  readonly dcLoading = signal(false);
  readonly dcError = signal('');
  readonly members = signal<DiscordMember[]>([]);
  readonly guildName = signal<string | null>(null);
  readonly inviteUrl = signal<string | null>(null);
  /** userId -> id-ul destinatarului ales în select, cât timp nu s-a apăsat Leagă. */
  readonly dcPick = signal<Record<string, string>>({});
  readonly dcLinking = signal<string | null>(null);
  /** userId-ul aflat in modul "schimbă destinatarul" - readus la select in loc de bifa verde. */
  readonly editingMember = signal<string | null>(null);

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
    return { EMAIL: '@', TELEGRAM: 'TG', DISCORD: 'DC' }[c] ?? c[0];
  }

  resetFilters(): void {
    this.q.set('');
    this.groupFilter.set('');
    this.page.set(0);
  }

  toggleMenu(which: 'channel' | 'more'): void {
    this.openMenu.set(this.openMenu() === which ? null : which);
  }

  @HostListener('document:mousedown', ['$event'])
  onDocDown(e: MouseEvent): void {
    const t = e.target as HTMLElement;
    if (this.openMenu() && !t.closest?.('[data-menu]')) {
      this.openMenu.set(null);
    }
  }

  /** Grupurile care se potrivesc cu ce s-a scris in cautare; toate daca e goala. */
  groupPickerOptions = computed(() => {
    const q = this.groupSearch().trim().toLowerCase();
    const names = this.store.groupNames();
    return q ? names.filter(n => n.toLowerCase().includes(q)) : names;
  });

  /** Textul din cautare nu se potriveste cu niciun grup existent - se poate crea unul nou cu el. */
  groupSearchIsNew = computed(() => {
    const q = this.groupSearch().trim();
    return !!q && !this.store.groupNames().some(n => n.toLowerCase() === q.toLowerCase());
  });

  openGroupPicker(): void {
    this.groupSearch.set(this.form.group ?? '');
    this.groupPickerOpen.set(true);
  }

  closeGroupPicker(e: Event): void {
    if (e.target === e.currentTarget) this.groupPickerOpen.set(false);
  }

  pickGroup(name: string): void {
    if (!name) return;
    this.form.group = name;
    this.groupPickerOpen.set(false);
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
    this.groupPickerOpen.set(false);
    this.editing.set({ scope: 'people', id: null });
  }

  openEditStaged(s: Staged): void {
    this.form = {
      firstName: s.firstName,
      lastName: s.lastName,
      email: s.email,
      group: s.group,
      telegramChatId: s.telegramChatId ?? '',
      discordUserId: s.discordUserId ?? '',
    };
    this.editError.set('');
    this.groupPickerOpen.set(false);
    this.editing.set({ scope: 'staged', key: s.key });
  }

  openEdit(p: Recipient): void {
    this.form = {
      firstName: p.firstName,
      lastName: p.lastName,
      email: p.email,
      group: p.group,
      telegramChatId: p.telegramChatId ?? '',
      discordUserId: p.discordUserId ?? '',
    };
    this.editError.set('');
    this.groupPickerOpen.set(false);
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
      // Acelasi chat Telegram la doi oameni ar duce mesajul de doua ori la unul.
      const chatId = f.telegramChatId?.trim() ?? '';
      if (chatId) {
        const taken = this.store.recipients().find(p => p.telegramChatId?.trim() === chatId);
        const takenInFile = (this.staged() ?? []).find(s => s.key !== target.key && s.telegramChatId?.trim() === chatId);
        const owner = taken?.name ?? (takenInFile ? `${takenInFile.firstName} ${takenInFile.lastName}`.trim() : '');
        if (owner) {
          return this.editError.set(`Chat ID-ul ${chatId} e deja legat la ${owner}.`);
        }
      }
      // Acelasi userId Discord la doi oameni ar duce mesajul de doua ori la unul.
      const discordId = f.discordUserId?.trim() ?? '';
      if (discordId) {
        const taken = this.store.recipients().find(p => p.discordUserId?.trim() === discordId);
        const takenInFile = (this.staged() ?? []).find(s => s.key !== target.key && s.discordUserId?.trim() === discordId);
        const owner = taken?.name ?? (takenInFile ? `${takenInFile.firstName} ${takenInFile.lastName}`.trim() : '');
        if (owner) {
          return this.editError.set(`Discord user ID-ul ${discordId} e deja legat la ${owner}.`);
        }
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

  /** Reia un chat deja legat, cu selectul readus la vedere in locul bifei verzi. */
  startRelinkTelegram(c: TelegramContact): void {
    this.pick.set({ ...this.pick(), [c.chatId]: c.recipientId ? String(c.recipientId) : '' });
    this.editingChat.set(c.chatId);
  }

  /**
   * Chat-ul e deja legat la alt destinatar (cel curent) - link() refuza un chat
   * deja legat, asa ca scoatem intai vechea legatura, apoi legam noul destinatar.
   */
  relinkTelegram(c: TelegramContact): void {
    const newId = Number(this.pick()[c.chatId]);
    if (!newId) return;
    this.linking.set(c.chatId);
    const finish = () => {
      this.api.linkTelegram(c.chatId, newId).subscribe({
        next: () => {
          this.linking.set(null);
          this.editingChat.set(null);
          this.store.loadRecipients();
          this.loadTelegram();
          this.toast.show('Chat Telegram legat.');
        },
        error: e => {
          this.linking.set(null);
          this.tgError.set(errMessage(e, 'Legarea nu a reușit.'));
        },
      });
    };
    if (c.recipientId && c.recipientId !== newId) {
      const unlink$ = this.unlinkChannel(c.recipientId, 'telegramChatId');
      if (unlink$) {
        unlink$.subscribe({ next: finish, error: finish });
        return;
      }
    }
    finish();
  }

  /** Scoate legatura Telegram a destinatarului curent, ca chat-ul sa ramana liber. */
  unlinkTelegram(c: TelegramContact): void {
    if (!c.recipientId) return;
    const unlink$ = this.unlinkChannel(c.recipientId, 'telegramChatId');
    if (!unlink$) return;
    this.linking.set(c.chatId);
    unlink$.subscribe({
      next: () => {
        this.linking.set(null);
        this.editingChat.set(null);
        this.store.loadRecipients();
        this.loadTelegram();
        this.toast.show('Legătura Telegram a fost scoasă.');
      },
      error: e => {
        this.linking.set(null);
        this.tgError.set(errMessage(e, 'Dezlegarea nu a reușit.'));
      },
    });
  }

  // ------------------------------------------------------------- Discord

  openDiscord(): void {
    this.discordOpen.set(true);
    this.dcPick.set({});
    this.loadDiscord();
  }

  closeDiscord(e: Event): void {
    if (e.target === e.currentTarget) this.discordOpen.set(false);
  }

  loadDiscord(): void {
    this.dcLoading.set(true);
    this.dcError.set('');
    this.api.discordMembers().subscribe({
      next: (d: DiscordDirectory) => {
        this.guildName.set(d.guildName);
        this.inviteUrl.set(d.inviteUrl);
        this.members.set(d.members ?? []);
        this.dcLoading.set(false);
      },
      error: e => {
        this.members.set([]);
        this.dcError.set(errMessage(e, 'Membrii serverului nu au putut fi citiți.'));
        this.dcLoading.set(false);
      },
    });
  }

  chooseDiscord(userId: string, recipientId: string): void {
    this.dcPick.set({ ...this.dcPick(), [userId]: recipientId });
  }

  linkDiscordMember(m: DiscordMember): void {
    const recipientId = Number(this.dcPick()[m.userId]);
    if (!recipientId) return;
    this.dcLinking.set(m.userId);
    this.api.linkDiscord(m.userId, recipientId).subscribe({
      next: () => {
        this.dcLinking.set(null);
        this.store.loadRecipients();
        // Reîncărcăm lista, ca rândul să apară imediat ca legat.
        this.loadDiscord();
        this.toast.show('Cont Discord legat.');
      },
      error: e => {
        this.dcLinking.set(null);
        this.dcError.set(errMessage(e, 'Legarea nu a reușit.'));
      },
    });
  }

  /** Reia un cont deja legat, cu selectul readus la vedere in locul bifei verzi. */
  startRelinkDiscord(m: DiscordMember): void {
    this.dcPick.set({ ...this.dcPick(), [m.userId]: m.recipientId ? String(m.recipientId) : '' });
    this.editingMember.set(m.userId);
  }

  /** Acelasi motiv ca relinkTelegram: scoatem vechea legatura inainte de a lega alt destinatar. */
  relinkDiscord(m: DiscordMember): void {
    const newId = Number(this.dcPick()[m.userId]);
    if (!newId) return;
    this.dcLinking.set(m.userId);
    const finish = () => {
      this.api.linkDiscord(m.userId, newId).subscribe({
        next: () => {
          this.dcLinking.set(null);
          this.editingMember.set(null);
          this.store.loadRecipients();
          this.loadDiscord();
          this.toast.show('Cont Discord legat.');
        },
        error: e => {
          this.dcLinking.set(null);
          this.dcError.set(errMessage(e, 'Legarea nu a reușit.'));
        },
      });
    };
    if (m.recipientId && m.recipientId !== newId) {
      const unlink$ = this.unlinkChannel(m.recipientId, 'discordUserId');
      if (unlink$) {
        unlink$.subscribe({ next: finish, error: finish });
        return;
      }
    }
    finish();
  }

  /** Scoate legatura Discord a destinatarului curent, ca contul sa ramana liber. */
  unlinkDiscord(m: DiscordMember): void {
    if (!m.recipientId) return;
    const unlink$ = this.unlinkChannel(m.recipientId, 'discordUserId');
    if (!unlink$) return;
    this.dcLinking.set(m.userId);
    unlink$.subscribe({
      next: () => {
        this.dcLinking.set(null);
        this.editingMember.set(null);
        this.store.loadRecipients();
        this.loadDiscord();
        this.toast.show('Legătura Discord a fost scoasă.');
      },
      error: e => {
        this.dcLinking.set(null);
        this.dcError.set(errMessage(e, 'Dezlegarea nu a reușit.'));
      },
    });
  }

  /**
   * Goleste un singur canal al unui destinatar existent, ca adresa lui sa poata
   * fi reasignata altcuiva - updateRecipient rescrie toate canalele din payload,
   * asa ca pornim de la destinatarul curent si golim doar campul cerut.
   */
  private unlinkChannel(recipientId: number, field: 'telegramChatId' | 'discordUserId'): Observable<Recipient> | null {
    const r = this.store.recipients().find(p => p.id === recipientId);
    if (!r) return null;
    return this.api.updateRecipient(recipientId, {
      firstName: r.firstName,
      lastName: r.lastName,
      email: r.email,
      group: r.group,
      telegramChatId: field === 'telegramChatId' ? '' : (r.telegramChatId ?? ''),
      discordUserId: field === 'discordUserId' ? '' : (r.discordUserId ?? ''),
    });
  }

  // ---------------------------------------------------------------- CSV

  /**
   * `telegram` si `telefon` sunt adresele reale ale canalelor: fara ele, un
   * export urmat de un import ar pierde tot ce nu e email. `canale` e doar
   * pentru citit in Excel — la import se ignora, fiindca se deduce din adrese.
   */
  exportCsv(): void {
    const rows = [
      ['nume', 'prenume', 'email', 'grup', 'telegram', 'discord', 'canale'],
      ...this.store
        .recipients()
        .map(p => [
          p.lastName,
          p.firstName,
          p.email,
          p.group,
          p.telegramChatId ?? '',
          p.discordUserId ?? '',
          p.channels.join(';'),
        ]),
    ];
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
    reader.onload = () => this.readRows(String(reader.result ?? ''));
    reader.readAsText(f, 'utf-8');
  }

  /** Canalele pe care le va primi o linie din import, deduse din adresele ei. */
  stagedChannels(s: Staged): string[] {
    const out = ['EMAIL'];
    if (s.telegramChatId?.trim()) out.push('TELEGRAM');
    if (s.discordUserId?.trim()) out.push('DISCORD');
    return out;
  }

  /** Pasul 1: citirea fisierului. Celula Telegram ramane bruta, se rezolva mai jos. */
  private readRows(text: string): void {
    const rows = parseCsv(text);
    if (rows.length < 2) return this.toast.show('Fișierul este gol sau are doar antetul.');

    const head = rows[0];
    const idx = {
      last: findColumn(head, COL.last),
      first: findColumn(head, COL.first),
      email: findColumn(head, COL.email),
      group: findColumn(head, COL.group),
      tg: findColumn(head, COL.tg),
      discord: findColumn(head, COL.discord),
    };
    const missing = (['last', 'first', 'email', 'group'] as const)
      .filter(k => idx[k] < 0)
      .map(k => ({ last: 'nume', first: 'prenume', email: 'email', group: 'grup' })[k]);
    if (missing.length) {
      return this.toast.show(`Lipsesc coloanele obligatorii: ${missing.join(', ')}.`);
    }

    const existing = new Set(this.store.recipients().map(p => p.email.toLowerCase()));
    const seen = new Set<string>();
    const added: Draft[] = [];
    const invalid: string[] = [];
    let duplicates = 0;

    rows.slice(1).forEach((r, i) => {
      // Coloanele de canale sunt optionale: lipsa lor inseamna celula goala.
      const get = (k: keyof typeof idx) => (idx[k] < 0 ? '' : (r[idx[k]] ?? '').trim());
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
      added.push({
        key: line,
        firstName: first,
        lastName: last,
        email,
        group,
        telegramChatId: '',
        rawTg: get('tg'),
        discordUserId: '',
        rawDiscord: get('discord'),
      });
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
    this.resolveTelegram(added, notes);
  }

  /**
   * Pasul 2: celulele Telegram devin chat ID-uri. Bot API-ul nu accepta
   * `@username`, deci un username trebuie cautat in contactele botului — de
   * aici apelul HTTP. Chat ID-urile numerice nu au nevoie de el, asa ca un
   * export reimportat merge si cu botul oprit.
   */
  private resolveTelegram(drafts: Draft[], notes: string[]): void {
    const pending = drafts.filter(d => d.rawTg && !CHAT_ID_RE.test(d.rawTg));
    if (!pending.length) {
      return this.resolveDiscord(drafts, notes, []);
    }
    this.api.telegramContacts().subscribe({
      next: d => this.resolveDiscord(drafts, notes, d.contacts ?? []),
      error: () => {
        notes.push(`Telegram nu e disponibil — ${plural(pending.length, 'linie importată', 'linii importate')} fără chat.`);
        // Golim celulele nerezolvabile, ca sa nu mai fie raportate inca o data.
        pending.forEach(d => (d.rawTg = ''));
        this.resolveDiscord(drafts, notes, []);
      },
    });
  }

  /** Pasul 3: la fel ca resolveTelegram, dar cu membrii serverului Discord. */
  private resolveDiscord(drafts: Draft[], notes: string[], contacts: TelegramContact[]): void {
    const pending = drafts.filter(d => d.rawDiscord && !CHAT_ID_RE.test(d.rawDiscord));
    if (!pending.length) {
      return this.stageDrafts(drafts, notes, contacts, []);
    }
    this.api.discordMembers().subscribe({
      next: d => this.stageDrafts(drafts, notes, contacts, d.members ?? []),
      error: () => {
        notes.push(`Discord nu e disponibil — ${plural(pending.length, 'linie importată', 'linii importate')} fără Discord.`);
        pending.forEach(d => (d.rawDiscord = ''));
        this.stageDrafts(drafts, notes, contacts, []);
      },
    });
  }

  /**
   * Pasul 4: fiecare chat ID / userId ajunge la un singur destinatar. Doi
   * oameni pe acelasi chat/cont ar insemna acelasi mesaj de doua ori, deci al
   * doilea intra fara acel canal — persoana nu se pierde pentru o coloana.
   */
  private stageDrafts(drafts: Draft[], notes: string[], contacts: TelegramContact[], members: DiscordMember[]): void {
    const taken = new Map<string, string>();
    for (const p of this.store.recipients()) {
      if (p.telegramChatId?.trim()) taken.set(p.telegramChatId.trim(), p.name);
    }
    const takenDiscord = new Map<string, string>();
    for (const p of this.store.recipients()) {
      if (p.discordUserId?.trim()) takenDiscord.set(p.discordUserId.trim(), p.name);
    }

    const dropped: string[] = [];
    const staged = drafts.map(({ rawTg, rawDiscord, ...row }) => {
      const who = `${row.firstName} ${row.lastName}`.trim();

      let telegramChatId = '';
      const chatId = resolveChatId(rawTg, contacts);
      if (rawTg && !chatId) {
        dropped.push(`Linia ${row.key}: „${rawTg}" nu se regăsește în contactele botului`);
      } else if (chatId && taken.has(chatId)) {
        dropped.push(`Linia ${row.key}: chat ID-ul ${chatId} e deja legat la ${taken.get(chatId)} — ${who} se importă fără Telegram`);
      } else if (chatId) {
        taken.set(chatId, who);
        telegramChatId = chatId;
      }

      let discordUserId = '';
      const discordId = resolveDiscordId(rawDiscord, members);
      if (rawDiscord && !discordId) {
        dropped.push(`Linia ${row.key}: „${rawDiscord}" nu se regăsește în membrii serverului`);
      } else if (discordId && takenDiscord.has(discordId)) {
        dropped.push(
          `Linia ${row.key}: contul Discord ${discordId} e deja legat la ${takenDiscord.get(discordId)} — ${who} se importă fără Discord`,
        );
      } else if (discordId) {
        takenDiscord.set(discordId, who);
        discordUserId = discordId;
      }

      return { ...row, telegramChatId, discordUserId };
    });

    if (dropped.length) {
      notes.push(`${plural(dropped.length, 'linie', 'linii')} fără un canal: ${dropped.slice(0, 3).join('; ')}${dropped.length > 3 ? ' …' : ''}`);
    }
    this.stagedNotes.set(notes);
    this.staged.set(staged);
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
  return { firstName: '', lastName: '', email: '', group: '', telegramChatId: '', discordUserId: '' };
}

/**
 * Chat ID-ul numeric se ia ca atare; un `@username` sau un nume se caută în
 * contactele botului. Un nume care apare de două ori acolo e ambiguu, deci se
 * refuză — mai bine o linie fără Telegram decât mesajul la altcineva.
 */
function resolveChatId(raw: string, contacts: TelegramContact[]): string {
  const value = raw.trim();
  if (!value) return '';
  if (CHAT_ID_RE.test(value)) return value;
  const needle = value.replace(/^@/, '').toLowerCase();
  const byUsername = contacts.filter(c => c.username?.toLowerCase() === needle);
  const matches = byUsername.length ? byUsername : contacts.filter(c => c.name.trim().toLowerCase() === needle);
  return matches.length === 1 ? matches[0].chatId : '';
}

/** Aceeasi logica ca resolveChatId, dar pe lista membrilor serverului Discord. */
function resolveDiscordId(raw: string, members: DiscordMember[]): string {
  const value = raw.trim();
  if (!value) return '';
  if (CHAT_ID_RE.test(value)) return value;
  const needle = value.replace(/^@/, '').toLowerCase();
  const byUsername = members.filter(m => m.username?.toLowerCase() === needle);
  const matches = byUsername.length ? byUsername : members.filter(m => m.name.trim().toLowerCase() === needle);
  return matches.length === 1 ? matches[0].userId : '';
}

/** `detail` e textul în română; `message` e doar cheia erorii (vezi ToastService). */
function errMessage(e: unknown, fallback = 'Salvarea nu a reușit.'): string {
  const body = (e as { error?: { message?: string; detail?: string } })?.error;
  return body?.detail ?? body?.message ?? fallback;
}
