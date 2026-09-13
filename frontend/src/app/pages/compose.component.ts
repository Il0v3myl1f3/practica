import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ComposeStore, Store } from '../core/store';
import { ToastService } from '../core/toast.service';
import { CHANNELS, channelTitle } from '../core/models';
import { EditorComponent, plainText } from '../shared/editor.component';
import { IconComponent } from '../shared/icon.component';
import { Check, ImagePlus, X } from '../shared/icons';

@Component({
  selector: 'app-compose',
  standalone: true,
  imports: [FormsModule, EditorComponent, IconComponent],
  template: `
    <div class="cols">
      <section class="shell pad">
        <label class="field">
          <span>Subiect</span>
          <input class="input" [ngModel]="c.subject()" (ngModelChange)="c.subject.set($event)" placeholder="Despre ce e mesajul" />
        </label>

        <div class="field">
          <span>Conținut</span>
          <app-editor [value]="c.bodyHtml()" (valueChange)="c.bodyHtml.set($event)" />
          <p class="hint">
            Variabilele inserate se înlocuiesc cu datele fiecărui destinatar la trimitere. Formatarea se aplică la
            livrarea prin email; pe Telegram și WhatsApp textul pleacă simplu.
          </p>
        </div>

        <div class="field">
          <span>Imagini</span>
          <div class="images">
            @for (img of c.attachments(); track img.url) {
              <div class="thumb">
                <img [src]="img.url" [alt]="img.fileName" />
                <button type="button" class="x" (click)="removeImage(img.url)" title="Șterge">
                  <app-icon [icon]="I.X" [size]="13" [stroke]="2.5" />
                </button>
              </div>
            }
            <button type="button" class="add" (click)="file.click()"><app-icon [icon]="I.ImagePlus" [size]="22" /></button>
            <input #file type="file" accept="image/*" multiple hidden (change)="onFiles($event)" />
          </div>
          <p class="hint">{{ imagesHint() }}</p>
        </div>
      </section>

      <aside class="side">
        <section class="shell pad">
          <div class="side-title">Canal de livrare</div>
          <div class="channels" role="group" aria-label="Canal de livrare">
            @for (ch of channels; track ch.id) {
              <button
                type="button"
                class="channel"
                [class.on]="c.channels().includes(ch.id)"
                [attr.aria-pressed]="c.channels().includes(ch.id)"
                (click)="c.toggleChannel(ch.id)"
              >
                <span>{{ ch.title }}</span>
                @if (c.channels().includes(ch.id)) {
                  <app-icon class="mark" [icon]="I.Check" [size]="15" [stroke]="2.5" />
                }
              </button>
            }
          </div>
          <p class="hint">{{ channelHint() }}</p>
        </section>

        <section class="shell pad">
          <div class="side-title">Rezumat</div>
          <div class="sum"><span>Șablon</span><b>{{ c.templateName() || 'Mesaj gol' }}</b></div>
          <div class="sum"><span>Canal</span><b>{{ channelsLabel() }}</b></div>
          <div class="sum"><span>Imagini</span><b>{{ c.attachments().length || 'fără' }}</b></div>
          <div class="sum"><span>Destinatari</span><b>{{ selectedLabel() }}</b></div>
        </section>

        <div class="actions">
          <button type="button" class="btn btn-primary" (click)="next()">Continuă la destinatari</button>
          <button type="button" class="btn btn-ghost" (click)="saveDraft()">Salvează ciornă</button>
        </div>
      </aside>
    </div>
  `,
  styles: [
    `
      .cols { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 20px; align-items: start; }
      .pad { padding: 20px; display: flex; flex-direction: column; gap: 16px; }
      .side { display: flex; flex-direction: column; gap: 16px; }
      .side-title { font-size: 15px; font-weight: 600; }
      .channels { display: flex; flex-direction: column; gap: 8px; }
      .channel {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        height: 40px;
        padding: 0 12px;
        border: 1px solid var(--line);
        border-radius: var(--r-sm);
        background: var(--surface);
        color: var(--ink-2);
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        text-align: left;
        transition: background 150ms ease-out, border-color 150ms ease-out;
      }
      .channel:hover:not(.on) { background: #f5f5f5; }
      .channel:focus-visible { outline: 2px solid var(--brand); outline-offset: 2px; }
      .channel.on { border-color: var(--brand); background: var(--brand-soft); color: var(--brand-ink); }
      .mark { font-size: 12px; font-weight: 700; color: var(--brand); }
      .hint { margin: 0; font-size: 12px; line-height: 18px; color: var(--muted); }
      .sum { display: flex; justify-content: space-between; gap: 12px; font-size: 13px; }
      .sum span { color: var(--muted); }
      .actions { display: flex; flex-direction: column; gap: 8px; }
      .images { display: flex; gap: 10px; flex-wrap: wrap; }
      .thumb { position: relative; width: 84px; height: 84px; border-radius: var(--r-md); overflow: hidden; border: 1px solid var(--line); }
      .thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
      .x {
        position: absolute;
        top: 4px;
        right: 4px;
        width: 20px;
        height: 20px;
        border: none;
        border-radius: 9999px;
        background: #121212cc;
        color: #fff;
        cursor: pointer;
        line-height: 1;
      }
      .add {
        width: 84px;
        height: 84px;
        border: 1px dashed var(--line);
        border-radius: var(--r-md);
        background: var(--surface);
        color: var(--muted);
        font-size: 20px;
        cursor: pointer;
      }
      .add:hover { border-color: var(--brand); color: var(--brand); }
      @media (max-width: 900px) {
        .cols { grid-template-columns: 1fr; }
      }
    `,
  ],
})
export class ComposeComponent {
  readonly c = inject(ComposeStore);
  private store = inject(Store);
  private api = inject(ApiService);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly channels = CHANNELS;
  readonly I = { Check, X, ImagePlus };

  channelsLabel = computed(() => this.c.channels().map(channelTitle).join(' + '));

  selectedLabel = computed(() => `${this.c.selected().length} din ${this.store.recipients().length}`);

  channelHint = computed(() => {
    const list = this.c.channels();
    if (list.length > 1) {
      const names = list.map(channelTitle);
      const phrase = names.slice(0, -1).join(', ') + ' și ' + names[names.length - 1];
      return `Mesajul va fi livrat prin ${phrase}, în paralel. Dă clic pe un canal ca să-l scoți din listă.`;
    }
    const hint = CHANNELS.find(c => c.id === list[0])?.hint ?? '';
    return `${hint} Dă clic pe alt canal ca să livrezi și acolo.`;
  });

  imagesHint = computed(() => {
    const n = this.c.attachments().length;
    if (!n) return 'Nicio imagine adăugată. Se atașează la email; pe WhatsApp pleacă separat.';
    return `${n} ${n === 1 ? 'imagine adăugată' : 'imagini adăugate'}. Apasă × pe o miniatură pentru a o șterge.`;
  });

  onFiles(e: Event): void {
    const input = e.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    files.forEach(f => {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = String(reader.result ?? '');
        this.c.attachments.set([
          ...this.c.attachments(),
          { fileName: f.name, contentType: f.type || 'application/octet-stream', dataBase64: dataUrl, url: dataUrl },
        ]);
      };
      reader.readAsDataURL(f);
    });
  }

  removeImage(url: string): void {
    this.c.attachments.set(this.c.attachments().filter(a => a.url !== url));
  }

  private payload() {
    return {
      subject: this.c.subject(),
      bodyHtml: this.c.bodyHtml(),
      channels: this.c.channels(),
      templateId: this.c.templateId(),
      attachments: this.c.attachments().map(({ url, ...rest }) => rest),
    };
  }

  saveDraft(): void {
    this.api.saveDraft(this.payload()).subscribe({
      next: () => {
        this.store.loadDrafts();
        this.toast.show(`Ciornă salvată: text, ${this.c.attachments().length} imagini, canal ${this.channelsLabel()}.`);
      },
      error: e => this.toast.error(e),
    });
  }

  next(): void {
    if (!this.c.subject().trim() && !plainText(this.c.bodyHtml())) {
      this.toast.show('Adaugă un subiect sau conținut înainte de a continua.');
      return;
    }
    this.router.navigate(['/mesaj/destinatari']);
  }
}
