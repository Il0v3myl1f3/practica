import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ComposePayload,
  Group,
  MessageDetail,
  MessageView,
  Overview,
  Recipient,
  RecipientUpsert,
  SendPayload,
  SendResult,
  TelegramDirectory,
  Template,
} from './models';

export const API = 'http://localhost:8080';

/** Un apel per lucru de care are nevoie un ecran. Totul e deja scopat pe organizație. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  overview(): Observable<Overview> {
    return this.http.get<Overview>(`${API}/api/app/overview`);
  }

  recipients(): Observable<Recipient[]> {
    return this.http.get<Recipient[]>(`${API}/api/app/recipients`);
  }

  createRecipient(body: RecipientUpsert): Observable<Recipient> {
    return this.http.post<Recipient>(`${API}/api/app/recipients`, body);
  }

  updateRecipient(id: number, body: RecipientUpsert): Observable<Recipient> {
    return this.http.put<Recipient>(`${API}/api/app/recipients/${id}`, body);
  }

  deleteRecipient(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/api/app/recipients/${id}`);
  }

  importRecipients(rows: RecipientUpsert[]): Observable<Recipient[]> {
    return this.http.post<Recipient[]>(`${API}/api/app/recipients/bulk`, rows);
  }

  groups(): Observable<Group[]> {
    return this.http.get<Group[]>(`${API}/api/app/groups`);
  }

  /** Contactele botului de Telegram. 409 dacă Telegramul real nu e pornit. */
  telegramContacts(): Observable<TelegramDirectory> {
    return this.http.get<TelegramDirectory>(`${API}/api/app/telegram/contacts`);
  }

  linkTelegram(chatId: string, recipientId: number): Observable<void> {
    return this.http.post<void>(`${API}/api/app/telegram/link`, { chatId, recipientId });
  }

  templates(): Observable<Template[]> {
    return this.http.get<Template[]>(`${API}/api/app/templates`);
  }

  createTemplate(body: Template): Observable<Template> {
    return this.http.post<Template>(`${API}/api/app/templates`, body);
  }

  updateTemplate(id: number, body: Template): Observable<Template> {
    return this.http.put<Template>(`${API}/api/app/templates/${id}`, body);
  }

  /** Toate sabloanele, ca arhiva ZIP cu cate un HTML fiecare. */
  exportTemplatesZip(): Observable<Blob> {
    return this.http.get(`${API}/api/app/templates/export`, { responseType: 'blob' });
  }

  importTemplatesZip(file: File): Observable<Template[]> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Template[]>(`${API}/api/app/templates/import`, form);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/api/app/templates/${id}`);
  }

  sent(): Observable<MessageView[]> {
    return this.http.get<MessageView[]>(`${API}/api/app/messages`);
  }

  message(id: number): Observable<MessageDetail> {
    return this.http.get<MessageDetail>(`${API}/api/app/messages/${id}`);
  }

  send(body: SendPayload): Observable<SendResult> {
    return this.http.post<SendResult>(`${API}/api/app/messages/send`, body);
  }

  /** Starea unei trimiteri puse la coadă. Suficient de ieftin pentru poll la câteva secunde. */
  sendStatus(id: number): Observable<SendResult> {
    return this.http.get<SendResult>(`${API}/api/app/messages/${id}/status`);
  }

  drafts(): Observable<MessageView[]> {
    return this.http.get<MessageView[]>(`${API}/api/app/drafts`);
  }

  saveDraft(body: ComposePayload): Observable<MessageView> {
    return this.http.post<MessageView>(`${API}/api/app/drafts`, body);
  }

  deleteDraft(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/api/app/drafts/${id}`);
  }
}
