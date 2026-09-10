import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ComposeStore, Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { COL, downloadCsv, findColumn, parseCsv } from '../core/csv';
import { plural } from '../core/format';
import { Template } from '../core/models';
import { templateToHtml } from '../shared/editor.component';
import { IconComponent } from '../shared/icon.component';
import { Download, Plus, Search, Trash2, Upload } from '../shared/icons';

const COLS = 'minmax(200px, 1fr) 160px';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [FormsModule, IconComponent],
  template: `
    <section class="shell">
      <div class="toolbar">
        <label class="search-wrap">
          <app-icon [icon]="I.Search" [size]="16" />
          <input placeholder="Caută șablon…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
        </label>
        <span class="grow"></span>
        <button type="button" class="btn btn-ghost sm" (click)="file.click()"><app-icon [icon]="I.Upload" [size]="15" />Importă CSV</button>
        <button type="button" class="btn btn-ghost sm" (click)="exportCsv()"><app-icon [icon]="I.Download" [size]="15" />Exportă CSV</button>
        <button type="button" class="btn btn-primary sm" (click)="router.navigate(['/sabloane/nou'])">
          <app-icon [icon]="I.Plus" [size]="15" />Șablon nou
        </button>
        <input #file type="file" accept=".csv,text/csv" hidden (change)="onFile($event)" />
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
      .grow { flex: 1 1 auto; }
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
  readonly I = { Search, Trash2, Upload, Download, Plus };
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

  exportCsv(): void {
    const rows = [
      ['nume', 'descriere', 'subiect', 'continut'],
      ...this.store.templates().map(t => [t.name, t.description, t.subject, t.body]),
    ];
    downloadCsv(rows, 'sabloane');
    this.toast.show(`${this.store.templates().length} șabloane exportate în CSV.`);
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
      const iName = findColumn(head, COL.tplName);
      if (iName < 0) {
        return this.toast.show('Lipsește coloana obligatorie: nume.');
      }
      const iDesc = findColumn(head, COL.tplDesc);
      const iSubject = findColumn(head, COL.tplSubject);
      const iBody = findColumn(head, COL.tplBody);

      const payload: Template[] = rows
        .slice(1)
        .map(r => ({
          id: null,
          name: (r[iName] ?? '').trim(),
          description: iDesc >= 0 ? (r[iDesc] ?? '').trim() : '',
          subject: iSubject >= 0 ? (r[iSubject] ?? '').trim() : '',
          // "\n" scris literal in CSV devine rand nou real.
          body: iBody >= 0 ? (r[iBody] ?? '').replace(/\\n/g, '\n') : '',
        }))
        .filter(t => !!t.name);

      if (!payload.length) return this.toast.show('Nicio linie validă de importat.');

      this.api.importTemplates(payload).subscribe({
        next: created => {
          this.store.loadTemplates();
          const skipped = payload.length - created.length;
          this.toast.show(
            `${plural(created.length, 'șablon adăugat', 'șabloane adăugate')}${skipped ? `, ${skipped} sărite (nume existent)` : ''}.`,
          );
        },
        error: err => this.toast.error(err),
      });
    };
    reader.readAsText(f, 'utf-8');
  }
}
