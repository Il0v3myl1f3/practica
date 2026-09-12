import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { EditorComponent } from '../shared/editor.component';

@Component({
  selector: 'app-template-new',
  standalone: true,
  imports: [FormsModule, EditorComponent],
  template: `
    <section class="shell pad">
      <div class="two">
        <label class="field"><span>Nume șablon</span><input class="input" [(ngModel)]="name" placeholder="Felicitare" /></label>
        <label class="field">
          <span>Descriere</span>
          <input class="input" [(ngModel)]="description" placeholder="Când se folosește acest șablon" />
        </label>
      </div>

      <label class="field">
        <span>Subiect</span>
        <input class="input" [(ngModel)]="subject" placeholder="Dacă îl lași gol, devine numele șablonului" />
      </label>

      <div class="field">
        <span>Conținut</span>
        <app-editor [(value)]="body" />
      </div>

      <div class="actions">
        <button type="button" class="btn btn-ghost" (click)="router.navigate(['/sabloane'])">Anulează</button>
        <button type="button" class="btn btn-primary" (click)="save()">
          {{ editing() ? 'Salvează modificările' : 'Salvează șablonul' }}
        </button>
      </div>
    </section>
  `,
  styles: [
    `
      .pad { padding: 24px; display: flex; flex-direction: column; gap: 20px; }
      .two { display: flex; gap: 16px; flex-wrap: wrap; }
      .two > .field { flex: 1 1 240px; }
      .actions { display: flex; gap: 12px; justify-content: flex-end; flex-wrap: wrap; }
    `,
  ],
})
export class TemplateNewComponent {
  readonly router = inject(Router);
  private api = inject(ApiService);
  private store = inject(Store);
  private toast = inject(ToastService);

  /** Legat din ruta /sabloane/:id/editeaza; absent la creare. */
  readonly id = input<string>();

  name = '';
  description = '';
  subject = '';
  readonly body = signal('');

  readonly editing = computed(() => !!this.id());
  private seeded = false;

  constructor() {
    // Sabloanele sunt incarcate de shell; completam formularul cand ajung.
    effect(() => {
      const id = Number(this.id());
      if (this.seeded || !id) return;
      const t = this.store.templates().find(x => x.id === id);
      if (!t) return;
      this.name = t.name;
      this.description = t.description;
      this.subject = t.subject;
      this.body.set(t.body);
      this.seeded = true;
    });
  }

  save(): void {
    if (!this.name.trim()) {
      this.toast.show('Dă un nume șablonului.');
      return;
    }
    const id = Number(this.id());
    const body = {
      id: id || null,
      name: this.name.trim(),
      description: this.description.trim(),
      subject: this.subject.trim(),
      body: this.body(),
    };
    const request = id ? this.api.updateTemplate(id, body) : this.api.createTemplate(body);
    request.subscribe({
      next: () => {
        this.store.loadTemplates();
        this.toast.show(id ? 'Șablon actualizat.' : 'Șablon adăugat.');
        this.router.navigate(['/sabloane']);
      },
      error: e => this.toast.error(e),
    });
  }
}
