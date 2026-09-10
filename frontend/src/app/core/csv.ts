/**
 * Parser si serializator CSV, portate din prototip.
 *
 * Acceptam ce produc Excel-ul si Google Sheets in Romania: separator virgula,
 * punct-si-virgula sau tab, ghilimele escapate prin dublare, BOM la inceput.
 */

export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let quoted = false;
  const src = text.replace(/^﻿/, '').replace(/\r\n?/g, '\n');

  for (let i = 0; i < src.length; i++) {
    const c = src[i];
    if (quoted) {
      if (c === '"' && src[i + 1] === '"') {
        cell += '"';
        i++;
      } else if (c === '"') {
        quoted = false;
      } else {
        cell += c;
      }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ',' || c === ';' || c === '\t') {
      row.push(cell);
      cell = '';
    } else if (c === '\n') {
      row.push(cell);
      rows.push(row);
      row = [];
      cell = '';
    } else {
      cell += c;
    }
  }
  row.push(cell);
  rows.push(row);
  return rows.filter(r => r.some(x => x.trim() !== ''));
}

function csvCell(v: unknown): string {
  const s = String(v ?? '');
  return /[",;\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
}

/** BOM-ul e obligatoriu: fara el Excel strica diacriticele. */
export function downloadCsv(rows: unknown[][], basename: string): void {
  const csv = rows.map(r => r.map(csvCell).join(',')).join('\r\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${basename}-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function findColumn(header: string[], pattern: RegExp): number {
  return header.findIndex(h => pattern.test(h.trim().toLowerCase().replace(/^"|"$/g, '')));
}

export const COL = {
  last: /^(nume|nume de familie|last ?name|surname)$/,
  first: /^(prenume|first ?name|given ?name)$/,
  email: /^(e-?mail|adresa e-?mail)$/,
  group: /^(grup|grupa|group|departament|department)$/,
  tplName: /^(nume|nume ?sablon|nume ?șablon|name|title|titlu)$/,
  tplDesc: /^(descriere|scop|description)$/,
  tplSubject: /^(subiect|subject)$/,
  tplBody: /^(conținut|continut|content|body|mesaj|text)$/,
};

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
