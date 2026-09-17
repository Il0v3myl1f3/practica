export type Channel = 'EMAIL' | 'TELEGRAM' | 'DISCORD';
export type MessageStatus = 'DRAFT' | 'QUEUED' | 'SENT' | 'PARTIAL' | 'FAILED';

export interface Recipient {
  id: number;
  firstName: string;
  lastName: string;
  name: string;
  email: string;
  group: string;
  telegramChatId: string | null;
  discordUserId: string | null;
  channels: Channel[];
}

export interface RecipientUpsert {
  firstName: string;
  lastName: string;
  email: string;
  group: string;
  telegramChatId?: string | null;
  discordUserId?: string | null;
}

export interface Group {
  id: number;
  name: string;
  count: number;
}

export interface GroupUpsert {
  name: string;
}

/**
 * Cineva care a scris botului de Telegram. Un bot nu poate scrie primul, deci
 * asta e singura sursă de chat ID-uri. `recipientId` e setat când chat ID-ul e
 * deja legat la un destinatar.
 */
export interface TelegramContact {
  chatId: string;
  name: string;
  username: string | null;
  lastMessage: string | null;
  lastAt: string | null;
  recipientId: number | null;
  recipientName: string | null;
}

export interface TelegramDirectory {
  botUsername: string | null;
  contacts: TelegramContact[];
}

/**
 * Un membru al serverului Discord. `recipientId` e setat cand userId-ul e
 * deja legat la un destinatar.
 */
export interface DiscordMember {
  userId: string;
  name: string;
  username: string | null;
  recipientId: number | null;
  recipientName: string | null;
}

export interface DiscordDirectory {
  guildName: string | null;
  inviteUrl: string | null;
  members: DiscordMember[];
}

export interface Template {
  id: number | null;
  name: string;
  description: string;
  subject: string;
  body: string;
}

export interface MessageView {
  id: number;
  subject: string;
  channels: Channel[];
  recipientCount: number;
  createdAt: string;
  sentAt: string | null;
  status: MessageStatus;
  images: number;
}

export interface MessageDetail {
  summary: MessageView;
  bodyHtml: string;
  recipientNames: string[];
}

export interface Overview {
  totalSent: number;
  recipients: number;
  groups: number;
  drafts: number;
  averageReach: number;
  fullyDelivered: number;
  groupDistribution: Group[];
}

export interface AttachmentPayload {
  fileName: string;
  contentType: string;
  dataBase64: string;
}

export interface ComposePayload {
  subject: string;
  bodyHtml: string;
  channels: Channel[];
  templateId?: number | null;
  attachments: AttachmentPayload[];
}

export interface SendPayload {
  message: ComposePayload;
  recipientIds: number[];
  channelOverrides: Record<number, Channel[]>;
}

/**
 * Cât timp `status` e QUEUED, `queued` e numărul de livrări încă în lucru, iar
 * `delivered` / `failed` cresc pe măsură ce coada se scurge. Ecranul de
 * compunere reinterogează până când statusul nu mai e QUEUED.
 */
export interface SendResult {
  messageId: number;
  status: MessageStatus;
  queued: number;
  delivered: number;
  failed: number;
  skipped: number;
  recipientCount: number;
}

export const CHANNELS: { id: Channel; title: string; hint: string }[] = [
  { id: 'EMAIL', title: 'Email', hint: 'Mesajul va fi livrat ca email, cu subiect și imagini atașate.' },
  { id: 'TELEGRAM', title: 'Telegram', hint: 'Mesajul va fi livrat ca mesaj Telegram simplu; subiectul devine prima linie.' },
  {
    id: 'DISCORD',
    title: 'Discord',
    hint: 'Mesaj privat de la bot; subiectul devine prima linie, atașamentele pleacă separat.',
  },
];

export const VARS = [
  { key: 'nume', label: 'Nume de familie', sample: 'Popescu' },
  { key: 'prenume', label: 'Prenume', sample: 'Ana' },
  { key: 'grup', label: 'Grup', sample: 'Marketing' },
  { key: 'email', label: 'Adresă email', sample: 'ana@exemplu.md' },
];

export function channelTitle(id: Channel): string {
  return CHANNELS.find(c => c.id === id)?.title ?? id;
}

export function statusLabel(s: MessageStatus): string {
  return { DRAFT: 'Ciornă', QUEUED: 'În curs', SENT: 'Livrat', PARTIAL: 'Parțial', FAILED: 'Eșuat' }[s] ?? s;
}
