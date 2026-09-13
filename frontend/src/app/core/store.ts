import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AttachmentPayload, Channel, Group, MessageView, Overview, Recipient, Template } from './models';
import { ToastService } from './toast.service';

/**
 * Cache-ul comun al ecranelor. Datele sunt mici (zeci de randuri), asa ca le
 * tinem intregi in memorie si filtram/paginam client-side, exact ca prototipul.
 */
@Injectable({ providedIn: 'root' })
export class Store {
  private api = inject(ApiService);
  private toast = inject(ToastService);

  readonly recipients = signal<Recipient[]>([]);
  readonly groups = signal<Group[]>([]);
  readonly templates = signal<Template[]>([]);
  readonly sent = signal<MessageView[]>([]);
  readonly drafts = signal<MessageView[]>([]);
  readonly overview = signal<Overview | null>(null);
  readonly ready = signal(false);

  loadAll(): void {
    this.loadRecipients();
    this.loadGroups();
    this.loadTemplates();
    this.loadSent();
    this.loadDrafts();
    this.ready.set(true);
  }

  loadRecipients(): void {
    this.api.recipients().subscribe({ next: r => this.recipients.set(r), error: e => this.toast.error(e) });
  }

  loadGroups(): void {
    this.api.groups().subscribe({ next: g => this.groups.set(g), error: e => this.toast.error(e) });
  }

  loadTemplates(): void {
    this.api.templates().subscribe({ next: t => this.templates.set(t), error: e => this.toast.error(e) });
  }

  loadSent(): void {
    this.api.sent().subscribe({ next: m => this.sent.set(m), error: e => this.toast.error(e) });
  }

  loadDrafts(): void {
    this.api.drafts().subscribe({ next: m => this.drafts.set(m), error: e => this.toast.error(e) });
  }

  loadOverview(): void {
    this.api.overview().subscribe({ next: o => this.overview.set(o), error: e => this.toast.error(e) });
  }

  groupNames(): string[] {
    return this.groups().map(g => g.name);
  }
}

/**
 * Mesajul in lucru, impartit intre cei 3 pasi ai fluxului (sablon, compunere,
 * destinatari). Traieste in memorie: parasirea fluxului inseamna renuntare.
 */
@Injectable({ providedIn: 'root' })
export class ComposeStore {
  readonly subject = signal('');
  readonly bodyHtml = signal('');
  readonly channels = signal<Channel[]>(['EMAIL']);
  readonly templateId = signal<number | null>(null);
  readonly templateName = signal('');
  readonly attachments = signal<(AttachmentPayload & { url: string })[]>([]);
  readonly selected = signal<number[]>([]);
  readonly overrides = signal<Record<number, Channel[]>>({});
  /** 'block' = trimiterea e oprita pana la rezolvare; 'fallback' = doar canalele disponibile. */
  readonly conflictMode = signal<'block' | 'fallback'>('block');
  readonly draftId = signal<number | null>(null);

  reset(): void {
    this.subject.set('');
    this.bodyHtml.set('');
    this.channels.set(['EMAIL']);
    this.templateId.set(null);
    this.templateName.set('');
    this.attachments.set([]);
    this.selected.set([]);
    this.overrides.set({});
    this.conflictMode.set('block');
    this.draftId.set(null);
  }

  clearResolutions(): void {
    this.overrides.set({});
    this.conflictMode.set('block');
  }

  /** Fiecare clic comuta canalul; ultimul canal ramas nu poate fi scos. */
  toggleChannel(id: Channel): void {
    const cur = this.channels();
    const next = cur.includes(id) ? cur.filter(c => c !== id) : [...cur, id];
    this.channels.set(next.length ? next : cur);
    this.clearResolutions();
  }

  toggleRecipient(id: number): void {
    const cur = this.selected();
    this.selected.set(cur.includes(id) ? cur.filter(x => x !== id) : [...cur, id]);
  }

  setMany(ids: number[], on: boolean): void {
    const set = new Set(this.selected());
    ids.forEach(id => (on ? set.add(id) : set.delete(id)));
    this.selected.set([...set]);
  }
}
