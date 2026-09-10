import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ComposeStore, Store } from '../core/store';
import { Template } from '../core/models';
import { templateToHtml } from '../shared/editor.component';

@Component({
  selector: 'app-compose-pick',
  standalone: true,
  template: `
    <div class="grid">
      <button type="button" class="card blank" (click)="blank()">
        <div class="mark">＋</div>
        <div class="body">
          <div class="name">Mesaj gol</div>
          <div class="desc">Pornește de la zero, fără text predefinit.</div>
        </div>
      </button>

      @for (t of store.templates(); track t.id) {
        <button type="button" class="card" (click)="pick(t)">
          <div class="mark">{{ t.name.charAt(0).toUpperCase() }}</div>
          <div class="body">
            <div class="name">{{ t.name }}</div>
            <div class="desc">{{ t.description }}</div>
          </div>
        </button>
      }
    </div>
  `,
  styles: [
    `
      .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
      .card {
        display: flex;
        gap: 14px;
        align-items: flex-start;
        text-align: left;
        padding: 20px;
        background: var(--surface);
        border: 1px solid var(--line);
        border-radius: var(--r-lg);
        cursor: pointer;
        transition: border-color 150ms ease-out, box-shadow 150ms ease-out;
      }
      .card:hover { border-color: var(--brand); box-shadow: 0 0 0 4px var(--brand-ring); }
      .mark {
        flex: none;
        width: 40px;
        height: 40px;
        border-radius: var(--r-md);
        background: var(--brand-soft);
        color: var(--brand);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        font-weight: 700;
      }
      .body { min-width: 0; }
      .name { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
      .desc { font-size: 13px; color: var(--muted); line-height: 19px; }
      .blank .mark { background: var(--line-soft); color: var(--ink-2); }
    `,
  ],
})
export class ComposePickComponent {
  readonly store = inject(Store);
  private compose = inject(ComposeStore);
  private router = inject(Router);

  constructor() {
    this.store.loadTemplates();
  }

  blank(): void {
    this.compose.subject.set('');
    this.compose.bodyHtml.set('');
    this.compose.templateId.set(null);
    this.compose.templateName.set('');
    this.router.navigate(['/mesaj/compune']);
  }

  pick(t: Template): void {
    this.compose.subject.set(t.subject ?? '');
    this.compose.bodyHtml.set(templateToHtml(t.body ?? ''));
    this.compose.templateId.set(t.id);
    this.compose.templateName.set(t.name);
    this.router.navigate(['/mesaj/compune']);
  }
}
