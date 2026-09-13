import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ComposeStore, Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { saveBlob, stamp } from '../core/csv';
import { plural } from '../core/format';
import { Template } from '../core/models';
import { templateToHtml } from '../shared/editor.component';
import { IconComponent } from '../shared/icon.component';
import { Download, Pencil, Plus, Search, Trash2, Upload } from '../shared/icons';

const COLS = 'minmax(200px, 1fr) 200px';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [FormsModule, IconComponent],
  template: `
    <section class="shell">
      <div class="toolbar split">
        <div class="toolbar-main">
          <label class="search-wrap">
            <app-icon [icon]="I.Search" [size]="16" />
            <input placeholder="Caută șablon…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
          </label>
          <input #file type="file" accept=".zip,application/zip" hidden (change)="onFile($event)" />
        </div>

        <div class="toolbar-actions">
          <button type="button" class="btn btn-primary sm" (click)="router.navigate(['/sabloane/nou'])">
            <app-icon [icon]="I.Plus" [size]="15" />Șablon nou
          </button>
        </div>

        <div class="seg" role="group" aria-label="Import și export">
          <button type="button" class="seg-btn" title="Importă ZIP" aria-label="Importă ZIP" (click)="file.click()">
            <app-icon [icon]="I.Upload" />
          </button>
          <button type="button" class="seg-btn" title="Exportă ZIP" aria-label="Exportă ZIP" (click)="exportZip()">
            <app-icon [icon]="I.Download" />
          </button>
        </div>
      </div>

      <div class="tbody">
        <div class="row head" [style.gridTemplateColumns]="cols" [style.minWidth]="'480px'">
          <span>Șablon</span><span></span>
        </div>

        @for (t of filtered(); track t.id) {
          <div class="row" [style.gridTemplateColumns]="cols" [style.minWidth]="'480px'">
            <div>
              <div class="cell-strong">{{ t.name }}</div>
              <div class="desc">{{ t.description }}</div>
            </div>
            <span class="cell-actions">
              <button type="button" class="btn btn-ghost xs" (click)="use(t)">Folosește</button>
              <button type="button" class="icon-btn" title="Editează" (click)="edit(t)"><app-icon [icon]="I.Pencil" /></button>
              <button type="button" class="icon-btn" title="Șterge" (click)="remove(t)"><app-icon [icon]="I.Trash2" /></button>
            </span>
          </div>
        }

        @if (!filtered().length) {
          <div class="empty">
            {{ store.templates().length ? 'Niciun șablon nu corespunde căutării.' : 'Niciun șablon încă.' }}
          </div>
        }
      </div>

      <div class="pager"><span>{{ rangeLabel() }}</span></div>
    </section>
  `,
  styles: [
    `
      .desc { font-size: 12px; color: var(--muted); margin-top: 2px; }
    `,
  ],
})
export class TemplatesComponent {
  readonly store = inject(Store);
  readonly router = inject(Router);
  private api = inject(ApiService);
  private compose = inject(ComposeStore);
  private toast = inject(ToastService);

  readonly cols = COLS;
  readonly I = { Search, Trash2, Upload, Download, Plus, Pencil };
  readonly q = signal('');

  constructor() {
    this.store.loadTemplates();
  }

  filtered = computed(() => {
    const q = this.q().trim().toLowerCase();
    return this.store.templates().filter(t => !q || `${t.name} ${t.description}`.toLowerCase().includes(q));
  });

  rangeLabel = computed(() => {
    const shown = this.filtered().length;
    const total = this.store.templates().length;
    return shown === total ? plural(total, 'șablon', 'șabloane') : `${shown} din ${total} șabloane`;
  });

  use(t: Template): void {
    this.compose.reset();
    this.compose.subject.set(t.subject ?? '');
    this.compose.bodyHtml.set(templateToHtml(t.body ?? ''));
    this.compose.templateId.set(t.id);
    this.compose.templateName.set(t.name);
    this.router.navigate(['/mesaj/compune']);
  }

  edit(t: Template): void {
    this.router.navigate(['/sabloane', t.id, 'editeaza']);
  }

  remove(t: Template): void {
    if (t.id == null) return;
    this.api.deleteTemplate(t.id).subscribe({
      next: () => {
        this.store.loadTemplates();
        this.toast.show('Șablon șters.');
      },
      error: e => this.toast.error(e),
    });
  }

  exportZip(): void {
    this.api.exportTemplatesZip().subscribe({
      next: blob => {
        saveBlob(blob, `sabloane-${stamp()}.zip`);
        this.toast.show(`${plural(this.store.templates().length, 'șablon exportat', 'șabloane exportate')} în arhivă.`);
      },
      error: e => this.toast.error(e),
    });
  }

  onFile(e: Event): void {
    const input = e.target as HTMLInputElement;
    const f = input.files?.[0];
    input.value = '';
    if (!f) return;

    this.api.importTemplatesZip(f).subscribe({
      next: created => {
        this.store.loadTemplates();
        this.toast.show(
          created.length
            ? `${plural(created.length, 'șablon adăugat', 'șabloane adăugate')} din arhivă.`
            : 'Niciun șablon nou — toate numele există deja.',
        );
      },
      error: err => this.toast.error(err),
    });
  }
}
