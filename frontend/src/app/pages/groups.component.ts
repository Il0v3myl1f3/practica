import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { plural } from '../core/format';
import { Group } from '../core/models';
import { IconComponent } from '../shared/icon.component';
import { Pencil, Plus, Search, Trash2 } from '../shared/icons';

const COLS = 'minmax(200px, 1fr) 160px 110px';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [FormsModule, IconComponent],
  template: `
    <section class="shell">
      <div class="toolbar">
        <label class="search-wrap">
          <app-icon [icon]="I.Search" [size]="16" />
          <input placeholder="Caută grup…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
        </label>
        <span class="grow"></span>
        <button type="button" class="btn btn-primary sm" (click)="openNew()">
          <app-icon [icon]="I.Plus" [size]="15" />Grup nou
        </button>
      </div>

      <div class="tbody">
        <div class="row head" [style.gridTemplateColumns]="cols" [style.minWidth]="'470px'">
          <span>Nume</span><span>Destinatari</span><span></span>
        </div>

        @for (g of filtered(); track g.id) {
          <div class="row" [style.gridTemplateColumns]="cols" [style.minWidth]="'470px'">
            <span class="cell-strong">{{ g.name }}</span>
            <span class="cell-muted">{{ plural(g.count, 'destinatar', 'destinatari') }}</span>
            <span class="cell-actions">
              <button type="button" class="icon-btn" title="Editează" (click)="openEdit(g)">
                <app-icon [icon]="I.Pencil" />
              </button>
              <button type="button" class="icon-btn" title="Șterge" (click)="toDelete.set(g)">
                <app-icon [icon]="I.Trash2" />
              </button>
            </span>
          </div>
        }

        @if (!filtered().length) {
          <div class="empty">
            {{ store.groups().length ? 'Niciun grup nu corespunde căutării.' : 'Niciun grup încă.' }}
          </div>
        }
      </div>

      <div class="pager"><span>{{ rangeLabel() }}</span></div>
    </section>

    @if (editing()) {
      <div class="backdrop" (click)="closeEdit($event)">
        <div class="modal" (click)="$event.stopPropagation()">
          <h3>{{ editing()!.id ? 'Editează grupul' : 'Grup nou' }}</h3>
          <div class="form">
            <label class="field"><span>Nume</span><input class="input" [(ngModel)]="name" placeholder="Marketing" /></label>
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

    @if (toDelete(); as d) {
      <div class="backdrop">
        <div class="modal" style="max-width:440px">
          <h3>Ștergi acest grup?</h3>
          <p class="modal-sub">{{ d.name }} — acțiunea nu poate fi anulată.</p>
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="toDelete.set(null)">Anulează</button>
            <button type="button" class="btn btn-danger" (click)="confirmDelete(d)">Șterge grupul</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class GroupsComponent {
  readonly store = inject(Store);
  private api = inject(ApiService);
  private toast = inject(ToastService);

  readonly cols = COLS;
  readonly plural = plural;
  readonly I = { Search, Plus, Pencil, Trash2 };

  readonly q = signal('');
  readonly editing = signal<{ id: number | null } | null>(null);
  readonly editError = signal('');
  name = '';

  readonly toDelete = signal<Group | null>(null);

  constructor() {
    this.store.loadGroups();
  }

  filtered = computed(() => {
    const q = this.q().trim().toLowerCase();
    return this.store.groups().filter(g => !q || g.name.toLowerCase().includes(q));
  });

  rangeLabel = computed(() => {
    const shown = this.filtered().length;
    const total = this.store.groups().length;
    return shown === total ? plural(total, 'grup', 'grupuri') : `${shown} din ${total} grupuri`;
  });

  openNew(): void {
    this.name = '';
    this.editError.set('');
    this.editing.set({ id: null });
  }

  openEdit(g: Group): void {
    this.name = g.name;
    this.editError.set('');
    this.editing.set({ id: g.id });
  }

  closeEdit(e: Event): void {
    if (e.target === e.currentTarget) this.editing.set(null);
  }

  save(): void {
    const name = this.name.trim();
    if (!name) {
      return this.editError.set('Numele grupului este obligatoriu.');
    }
    const target = this.editing();
    if (!target) return;

    const done = () => {
      this.editing.set(null);
      this.store.loadGroups();
      this.toast.show(target.id ? 'Grup actualizat.' : 'Grup adăugat.');
    };
    const fail = (e: unknown) => this.editError.set(errMessage(e));
    if (target.id) {
      this.api.updateGroup(target.id, { name }).subscribe({ next: done, error: fail });
    } else {
      this.api.createGroup({ name }).subscribe({ next: done, error: fail });
    }
  }

  confirmDelete(g: Group): void {
    this.api.deleteGroup(g.id).subscribe({
      next: () => {
        this.toDelete.set(null);
        this.store.loadGroups();
        this.toast.show('Grup șters.');
      },
      error: e => {
        this.toDelete.set(null);
        this.toast.error(e);
      },
    });
  }
}

/** `detail` e textul în română; `message` e doar cheia erorii (vezi ToastService). */
function errMessage(e: unknown, fallback = 'Salvarea nu a reușit.'): string {
  const body = (e as { error?: { message?: string; detail?: string } })?.error;
  return body?.detail ?? body?.message ?? fallback;
}
