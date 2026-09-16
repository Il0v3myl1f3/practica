import { Component, ElementRef, HostListener, model, signal, viewChild } from '@angular/core';
import { VARS } from '../core/models';
import { IconComponent } from './icon.component';
import { Baseline, Bold, Braces, ChevronDown, Eraser, Italic, List, ListOrdered, Underline } from './icons';

export function varChip(key: string): string {
  return (
    `<span data-var="${key}" contenteditable="false">` + `{{${key}}}` + `</span>`
  );
}

/** Sablon vechi cu [nume] -> chip, plus newline -> <br>. */
export function templateToHtml(body: string): string {
  return body
    .split('\n')
    .join('<br>')
    .replace(/\[(nume|prenume|grup|email)\]/g, (_, k) => varChip(k))
    .replace(/\{\{\s*(nume|prenume|grup|email)\s*\}\}/g, (_, k) => varChip(k));
}

/**
 * Parseaza HTML-ul editorului intr-un div, cu un spatiu adaugat la fiecare
 * <br> si la inceputul/finalul fiecarui <div>/<p>/<li> - altfel .textContent
 * lipeste doua paragrafe alaturate fara nimic intre ele (ex. "timp.Dacă").
 * Prima linie nu e invelita in niciun tag (doar cele adaugate dupa un Enter),
 * deci separatorul trebuie sa vina si la deschidere, nu doar la inchidere.
 */
function parseForText(html: string): HTMLDivElement {
  const spaced = (html ?? '')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<(div|p|li)(\s[^>]*)?>/gi, ' <$1$2>')
    .replace(/<\/(div|p|li)>/gi, ' </$1>');
  const d = document.createElement('div');
  d.innerHTML = spaced;
  return d;
}

const SIZES = [
  { title: 'Text mic', size: '14px' },
  { title: 'Text normal', size: '16px' },
  { title: 'Text mare', size: '20px' },
  { title: 'Titlu', size: '24px' },
];

/** Comenzile care comuta o stare, nu seteaza o valoare. */
const TOGGLES = ['bold', 'italic', 'underline'];

/** Comenzile a caror stare curenta se aprinde in bara. */
const STATEFUL = ['bold', 'italic', 'underline', 'insertUnorderedList', 'insertOrderedList'];

/** Proprietatile pe care editorul le scrie inline pe chip-uri, in ordinea CSS. */
const MANAGED = ['font-size', 'color', 'font-weight', 'font-style', 'text-decoration-line'];

const COLORS = [
  { title: 'Negru', hex: '#121212' },
  { title: 'Albastru brand', hex: '#0058d2' },
  { title: 'Roșu', hex: '#d92d20' },
  { title: 'Verde', hex: '#039855' },
  { title: 'Gri', hex: '#757575' },
];

@Component({
  selector: 'app-editor',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div class="editor">
      <div class="bar">
        <div class="menu-wrap" data-menu>
          <button type="button" class="trigger" [class.on]="menu() === 'size'" (mousedown)="toggle($event, 'size')">
            Mărime text<app-icon [icon]="I.ChevronDown" [size]="13" />
          </button>
          @if (menu() === 'size') {
            <div class="menu">
              @for (z of sizes; track z.size) {
                <button type="button" class="menu-item" [style.fontSize]="z.size" (mousedown)="applySize($event, z.size)">
                  <span>{{ z.title }}</span><span class="menu-hint">{{ z.size }}</span>
                </button>
              }
            </div>
          }
        </div>

        <span class="sep"></span>

        <button
          type="button"
          class="icon-btn"
          title="Îngroșat (Ctrl+B)"
          [class.active]="activeCmds().includes('bold')"
          (mousedown)="exec($event, 'bold')"
        >
          <app-icon [icon]="I.Bold" [size]="16" />
        </button>
        <button
          type="button"
          class="icon-btn"
          title="Cursiv (Ctrl+I)"
          [class.active]="activeCmds().includes('italic')"
          (mousedown)="exec($event, 'italic')"
        >
          <app-icon [icon]="I.Italic" [size]="16" />
        </button>
        <button
          type="button"
          class="icon-btn"
          title="Subliniat (Ctrl+U)"
          [class.active]="activeCmds().includes('underline')"
          (mousedown)="exec($event, 'underline')"
        >
          <app-icon [icon]="I.Underline" [size]="16" />
        </button>

        <div class="menu-wrap" data-menu>
          <button type="button" class="trigger" [class.on]="menu() === 'color'" title="Culoare text" (mousedown)="toggle($event, 'color')">
            <app-icon [icon]="I.Baseline" [size]="16" /><app-icon [icon]="I.ChevronDown" [size]="13" />
          </button>
          @if (menu() === 'color') {
            <div class="menu">
              @for (c of colors; track c.hex) {
                <button type="button" class="menu-item" (mousedown)="exec($event, 'foreColor', c.hex)">
                  <span class="dot" [style.background]="c.hex"></span><span>{{ c.title }}</span>
                </button>
              }
            </div>
          }
        </div>

        <span class="sep"></span>

        <button
          type="button"
          class="icon-btn"
          title="Listă cu buline"
          [class.active]="activeCmds().includes('insertUnorderedList')"
          (mousedown)="exec($event, 'insertUnorderedList')"
        >
          <app-icon [icon]="I.List" [size]="16" />
        </button>
        <button
          type="button"
          class="icon-btn"
          title="Listă numerotată"
          [class.active]="activeCmds().includes('insertOrderedList')"
          (mousedown)="exec($event, 'insertOrderedList')"
        >
          <app-icon [icon]="I.ListOrdered" [size]="16" />
        </button>
        <button type="button" class="icon-btn" title="Curăță formatarea" (mousedown)="exec($event, 'removeFormat')">
          <app-icon [icon]="I.Eraser" [size]="16" />
        </button>

        <span class="sep"></span>

        <div class="menu-wrap" data-menu>
          <button type="button" class="trigger" [class.on]="menu() === 'vars'" (mousedown)="toggle($event, 'vars')">
            <app-icon [icon]="I.Braces" [size]="15" />Inserează variabilă<app-icon [icon]="I.ChevronDown" [size]="13" />
          </button>
          @if (menu() === 'vars') {
            <div class="menu wide">
              @for (v of vars; track v.key) {
                <button type="button" class="menu-item" (mousedown)="insertVar($event, v.key)">
                  <span class="tag">{{ '{{' + v.key + '}}' }}</span>
                  <span class="menu-hint">{{ v.label }} · ex. {{ v.sample }}</span>
                </button>
              }
            </div>
          }
        </div>
      </div>

      <div
        #host
        class="canvas"
        contenteditable="true"
        (input)="onInput()"
        (keyup)="onSelect()"
        (mouseup)="onSelect()"
      ></div>

      <div class="stats">
        <span>{{ stats() }}</span>
        <span>Variabilele se completează per destinatar</span>
      </div>
    </div>
  `,
  styles: [
    `
      .editor { border: 1px solid var(--line); border-radius: var(--r-md); background: var(--surface); }
      .bar {
        display: flex;
        align-items: center;
        gap: 4px;
        flex-wrap: wrap;
        padding: 6px 8px;
        border-bottom: 1px solid var(--line-soft);
      }
      .sep { width: 1px; height: 20px; background: var(--line); margin: 0 4px; }
      .trigger {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 32px;
        padding: 0 10px;
        border: none;
        border-radius: var(--r-sm);
        background: transparent;
        color: var(--ink-2);
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
      }
      .trigger:hover, .trigger.on { background: var(--line-soft); }
      .menu-wrap { position: relative; }
      .menu {
        position: absolute;
        z-index: 30;
        top: calc(100% + 4px);
        left: 0;
        min-width: 190px;
        display: flex;
        flex-direction: column;
        gap: 2px;
        padding: 8px;
        background: var(--surface);
        border-radius: var(--r-lg);
        box-shadow: var(--shadow-menu);
      }
      .menu.wide { min-width: 260px; }
      .menu-item {
        display: flex;
        align-items: center;
        gap: 10px;
        justify-content: space-between;
        min-height: 36px;
        padding: 6px 10px;
        border: none;
        border-radius: var(--r-md);
        background: var(--surface);
        color: var(--ink-2);
        cursor: pointer;
        text-align: left;
        line-height: 1.3;
      }
      .menu-item:hover { background: var(--line-soft); }
      .menu-hint { font-size: 12px; color: var(--muted); }
      .dot { width: 16px; height: 16px; border-radius: 9999px; border: 1px solid var(--line); flex: none; }
      .canvas {
        min-height: 260px;
        padding: 16px;
        font-size: 15px;
        line-height: 24px;
        color: var(--ink);
        outline: none;
        overflow-y: auto;
        max-height: 46vh;
        /* Fara asta spatiul tastat langa un chip sau la final e colapsat, deci invizibil. */
        white-space: pre-wrap;
      }
      .stats {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        flex-wrap: wrap;
        padding: 8px 16px;
        border-top: 1px solid var(--line-soft);
        font-size: 12px;
        color: var(--muted);
      }
    `,
  ],
})
export class EditorComponent {
  readonly value = model<string>('');
  private host = viewChild.required<ElementRef<HTMLDivElement>>('host');

  readonly menu = model<'size' | 'color' | 'vars' | null>(null);
  /** Comenzile active pe selectia curenta, ca sa se aprinda butoanele din bara. */
  readonly activeCmds = signal('');
  readonly I = { Bold, Italic, Underline, List, ListOrdered, Eraser, Baseline, Braces, ChevronDown };
  readonly sizes = SIZES;
  readonly colors = COLORS;
  readonly vars = VARS;

  /** Selectia ca offset-uri de caracter in editor, nu ca noduri: execCommand rescrie
   *  nodurile din selectie, iar un Range salvat ar ramane agatat de cele vechi. */
  private sel: [number, number] | null = null;
  /** Range-ul viu, sursa principala: stie si pozitiile pe care offset-urile de caracter
   *  nu le pot distinge (un rand nou gol are acelasi offset ca finalul celui precedent). */
  private liveRange: Range | null = null;
  private lastPushed = ' ';

  constructor() {
    // Impinge HTML in editor doar cand se schimba din afara (sablon ales, ciorna
    // reluata). Pe fiecare tasta ar muta cursorul la final.
    queueMicrotask(() => this.syncIn());
  }

  ngAfterViewChecked(): void {
    this.syncIn();
  }

  private syncIn(): void {
    const el = this.host()?.nativeElement;
    if (!el) return;
    const incoming = this.value() ?? '';
    if (incoming !== this.lastPushed && incoming !== el.innerHTML) {
      el.innerHTML = incoming;
      this.lastPushed = incoming;
    }
  }

  onInput(): void {
    this.autoChip();
    this.cleanupZwsp();
    this.push();
  }

  /**
   * Un chip sters cu Backspace/Delete lasa cele doua ZWSP puse la inserare (unul
   * de fiecare parte) lipite unul de altul, fara niciun chip intre ele - nu mai
   * au niciun rol. normalize() intai, ca sa vedem si perechile impartite intre
   * doua noduri text vecine intr-un singur nod continuu.
   */
  private cleanupZwsp(): void {
    const el = this.host().nativeElement;
    el.normalize();
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    let node: Node | null;
    while ((node = walker.nextNode())) {
      const text = (node as Text).nodeValue ?? '';
      if (/​{2,}/.test(text)) (node as Text).nodeValue = text.replace(/​{2,}/g, '​');
    }
  }

  private push(): void {
    const html = this.host().nativeElement.innerHTML;
    this.lastPushed = html;
    this.value.set(html);
  }

  /** Cursorul/selectia s-a mutat: retinem pozitia si aprindem butoanele potrivite. */
  onSelect(): void {
    this.saveRange();
    this.readState();
  }

  saveRange(): void {
    const el = this.host().nativeElement;
    const sel = window.getSelection();
    if (!sel || !sel.rangeCount) return;
    if (!el.contains(sel.anchorNode)) return;
    const r = sel.getRangeAt(0);
    const before = document.createRange();
    before.selectNodeContents(el);
    before.setEnd(r.startContainer, r.startOffset);
    const start = before.toString().length;
    this.sel = [start, start + r.toString().length];
    this.liveRange = r.cloneRange();
  }

  private readState(): void {
    const el = this.host().nativeElement;
    const sel = window.getSelection();
    if (!sel || !sel.rangeCount || !el.contains(sel.anchorNode)) return;
    const next = STATEFUL.filter(cmd => document.queryCommandState(cmd)).join(' ');
    if (next !== this.activeCmds()) this.activeCmds.set(next);
  }

  toggle(e: Event, which: 'size' | 'color' | 'vars'): void {
    e.preventDefault();
    this.saveRange();
    this.menu.set(this.menu() === which ? null : which);
  }

  @HostListener('document:mousedown', ['$event'])
  onDocDown(e: MouseEvent): void {
    const t = e.target as HTMLElement;
    if (this.menu() && !t.closest?.('[data-menu]')) {
      this.menu.set(null);
    }
  }

  exec(e: Event, cmd: string, val?: string): void {
    e.preventDefault();
    this.restore();
    // Chip-urile se strang inainte: execCommand rescrie nodurile din selectie.
    const chips = this.chipsInSelection();
    // Starea dorita se citeste inainte de comanda, ca sa urmam aceeasi logica de
    // comutare ca browserul. Cand selectia n-are niciun caracter editabil - e in
    // intregime in interiorul unui chip, posibil dupa un dublu-click pe text -
    // queryCommandState nu are ce sa inspecteze si raporteaza mereu false: bold-ul
    // s-ar aplica mereu, fara sa se mai poata scoate. In cazul asta citim starea
    // direct din chip-uri, nu din browser.
    const sel = window.getSelection();
    const range = sel && sel.rangeCount ? sel.getRangeAt(0) : null;
    const chipOnly = !!range && !range.collapsed && chips.length > 0 && !this.hasEditableText(range);
    const on = TOGGLES.includes(cmd)
      ? chipOnly
        ? !chips.every(chip => this.chipHasFormat(chip, cmd))
        : !document.queryCommandState(cmd)
      : false;
    try {
      document.execCommand('styleWithCSS', false, 'true');
    } catch {
      /* browserul nu suporta, formatarea merge oricum */
    }
    document.execCommand(cmd, false, val);
    chips.forEach(chip => this.styleChip(chip, cmd, on, val));
    // execCommand poate desparte exact la marginea selectiei (splitText): nodul vechi
    // ramane valid dar scurtat, deci liveRange ajunge sa indice o granita gresita fara
    // sa para "corupt" (nu devine collapsed). Fortam reconstructia din offsete.
    this.liveRange = null;
    this.menu.set(null);
    this.reselect();
    this.readState();
    this.push();
  }

  /**
   * execCommand('fontSize') accepta doar 1-7, deci oricum i-am rescrie rezultatul - iar
   * pe drum normalizeaza spatiile si sterge nodul dintre doua chip-uri, lipindu-le.
   * Impachetam selectia noi insine: Range API atinge exclusiv nodurile selectate.
   */
  applySize(e: Event, px: string): void {
    e.preventDefault();
    this.restore();
    const sel = window.getSelection();
    const range = sel && sel.rangeCount ? sel.getRangeAt(0) : null;
    if (range && !range.collapsed) {
      this.snapOut(range);
      const span = document.createElement('span');
      span.style.fontSize = px;
      span.append(range.extractContents());
      // Marimile dinauntru ar bate-o pe cea noua (castiga elementul cel mai apropiat):
      // le stergem, iar chip-urile si-o iau inapoi mai jos.
      span.querySelectorAll<HTMLElement>('[style*="font-size"]').forEach(n => n.style.removeProperty('font-size'));
      range.insertNode(span);
      // Chip-urile sunt contenteditable=false, deci nu mostenesc marimea: le-o punem
      // inline. Le cautam din DOM dupa inserare, deci referintele sunt sigur vii.
      span.querySelectorAll<HTMLElement>('[data-var]').forEach(chip => (chip.style.fontSize = px));
      // Acelasi motiv ca in exec(): range-ul a rescris nodurile, liveRange nu mai e de incredere.
      this.liveRange = null;
    }
    this.menu.set(null);
    this.reselect();
    this.push();
  }

  /** Chip-urile sunt atomice: capetele range-ului ies in afara lor, ca extractContents
   *  sa nu taie o pastila in doua. */
  private snapOut(r: Range): void {
    const chipOf = (n: Node): HTMLElement | null =>
      (n.nodeType === Node.ELEMENT_NODE ? (n as Element) : n.parentElement)?.closest<HTMLElement>('[data-var]') ?? null;
    const start = chipOf(r.startContainer);
    if (start) r.setStartBefore(start);
    const end = chipOf(r.endContainer);
    if (end) r.setEndAfter(end);
  }

  /**
   * Chip-urile prinse in selectie. Sunt contenteditable=false, deci formatarea
   * nu li se aplica singura - le stilam noi, inline, ca sa bata regula din
   * styles.scss care le fixeaza marimea, culoarea si grosimea.
   */
  private chipsInSelection(): HTMLElement[] {
    const sel = window.getSelection();
    if (!sel || !sel.rangeCount) return [];
    const range = sel.getRangeAt(0);
    if (range.collapsed) return [];
    const chips = this.host().nativeElement.querySelectorAll<HTMLElement>('[data-var]');
    return Array.from(chips).filter(chip => range.intersectsNode(chip));
  }

  /**
   * Selectia n-are niciun caracter din afara chip-urilor. Umblam prin nodurile de
   * text VII ale editorului, nu printr-un clon - range.cloneContents() pe un chip
   * contenteditable=false partial selectat (ex. dublu-click pe "nume" din
   * "{{nume}}") scoate textul fara sa mai pastreze si span-ul data-var din jur,
   * deci parentElement ar iesi null si am citi gresit "e editabil".
   */
  private hasEditableText(range: Range): boolean {
    const walker = document.createTreeWalker(this.host().nativeElement, NodeFilter.SHOW_TEXT);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      if (range.intersectsNode(n) && !(n.parentElement)?.closest('[data-var]')) return true;
    }
    return false;
  }

  /** Starea curenta a unui chip pentru comenzile de comutare - oglinda lui styleChip. */
  private chipHasFormat(chip: HTMLElement, cmd: string): boolean {
    switch (cmd) {
      case 'bold':
        return chip.style.fontWeight === 'bold' || Number(chip.style.fontWeight) >= 700;
      case 'italic':
        return chip.style.fontStyle === 'italic';
      case 'underline':
        return chip.style.textDecorationLine === 'underline';
      default:
        return false;
    }
  }

  private styleChip(chip: HTMLElement, cmd: string, on: boolean, val?: string): void {
    switch (cmd) {
      case 'bold':
        // Cuvant-cheie, nu numar: unele randatoare de email (Outlook/Word) ignora
        // font-weight numeric. Pe "off" stergem proprietatea in loc s-o fortam la o
        // valoare fixa, ca sa revina la cele 600 implicite din [data-var], nu mai subtire.
        if (on) chip.style.fontWeight = 'bold';
        else chip.style.removeProperty('font-weight');
        break;
      case 'italic':
        chip.style.fontStyle = on ? 'italic' : 'normal';
        break;
      case 'underline':
        chip.style.textDecorationLine = on ? 'underline' : 'none';
        break;
      case 'foreColor':
        chip.style.color = val ?? '';
        break;
      case 'removeFormat':
        MANAGED.forEach(prop => chip.style.removeProperty(prop));
        break;
    }
  }

  /**
   * Variabilele se insereaza ca "chip" contenteditable=false: se sterg dintr-o
   * singura apasare de Backspace si nu pot fi editate partial din greseala.
   */
  insertVar(e: Event, key: string): void {
    e.preventDefault();
    this.restore();
    const before = new Set(this.host().nativeElement.querySelectorAll('[data-var]'));
    // ZWSP de-o parte si de alta: intre doua chip-uri lipite trebuie sa existe o
    // pozitie de cursor, altfel nu se poate tasta nimic acolo.
    document.execCommand('insertHTML', false, '&#8203;' + varChip(key) + '&#8203;');
    const added = Array.from(this.host().nativeElement.querySelectorAll<HTMLElement>('[data-var]')).find(c => !before.has(c));
    if (added) this.adoptContext(added);
    this.saveRange();
    this.menu.set(null);
    this.push();
  }

  /**
   * Chip nou intr-un text deja formatat: preia formatarea din jur. Copiem doar
   * ce difera de stilul de baza al canvasului, altfel fiecare chip ar primi
   * inline valorile implicite si si-ar pierde aspectul propriu.
   */
  private adoptContext(chip: HTMLElement): void {
    const parent = chip.parentElement;
    if (!parent) return;
    const base = getComputedStyle(this.host().nativeElement);
    const around = getComputedStyle(parent);
    MANAGED.forEach(prop => {
      const value = around.getPropertyValue(prop);
      if (value && value !== base.getPropertyValue(prop)) {
        chip.style.setProperty(prop, value);
      }
    });
  }

  /**
   * Reasaza selectia dupa o comanda care a rescris nodurile, ca urmatoarea apasare
   * sa lucreze pe acelasi text. La cursor colapsat n-o atingem: a rescrie selectia
   * ar sterge stilul pe care execCommand tocmai l-a pregatit pentru ce se tasteaza.
   */
  private reselect(): void {
    if (this.sel && this.sel[0] !== this.sel[1]) this.restore();
  }

  private restore(): void {
    const el = this.host().nativeElement;
    el.focus();
    const sel = window.getSelection();
    if (!sel) return;
    // Offset-urile intra in joc doar cand nodul range-ului viu a fost scos din document
    // de o rescriere - altfel range-ul viu e mai precis, si se actualizeaza singur.
    const live = this.liveRange;
    // execCommand poate rescrie exact nodurile pe care le tine range-ul viu: acesta
    // ramane tehnic "in el" (el.contains trece), dar capetele i se prabusesc intr-un
    // punct fara sens. O selectie care nu era goala nu poate deveni goala doar pentru
    // ca s-a schimbat formatarea - cand se intampla asta, offset-urile de caracter
    // raman singura sursa de adevar.
    const corrupted = !!live && !!this.sel && this.sel[0] !== this.sel[1] && live.collapsed;
    const r = live && !corrupted && el.contains(live.commonAncestorContainer) ? live.cloneRange() : this.rangeAt(this.sel);
    sel.removeAllRanges();
    sel.addRange(r);
  }

  /**
   * Range-ul care acopera intervalul de caractere [start, end] din editor. Cauta
   * capetele mergand prin nodurile de text, deci nu-l deranjeaza ca execCommand a
   * inlocuit intre timp elementele din jur.
   */
  private rangeAt(offsets: [number, number] | null): Range {
    const el = this.host().nativeElement;
    const r = document.createRange();
    r.selectNodeContents(el);
    if (!offsets) {
      r.collapse(false);
      return r;
    }
    const [start, end] = offsets;
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    let seen = 0;
    let node: Node | null;
    let open = false;
    while ((node = walker.nextNode())) {
      const len = (node.nodeValue ?? '').length;
      if (!open && seen + len >= start) {
        r.setStart(node, start - seen);
        open = true;
      }
      if (open && seen + len >= end) {
        r.setEnd(node, end - seen);
        return r;
      }
      seen += len;
    }
    // Textul s-a scurtat sub offset-urile salvate: cadem pe finalul continutului.
    if (!open) r.collapse(false);
    return r;
  }

  /** Text tastat manual {{nume}} devine automat chip, ca la inserarea din bara. */
  private autoChip(): void {
    const el = this.host().nativeElement;
    const rx = /\{\{\s*(nume|prenume|grup|email)\s*\}\}/;
    for (let guard = 0; guard < 20; guard++) {
      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
      let node: Node | null;
      let hit: { node: Text; m: RegExpExecArray } | null = null;
      while ((node = walker.nextNode())) {
        const parent = (node as Text).parentElement;
        if (parent?.closest('[data-var]')) continue;
        const m = rx.exec(node.nodeValue ?? '');
        if (m) {
          hit = { node: node as Text, m };
          break;
        }
      }
      if (!hit) return;
      const tail = hit.node.splitText(hit.m.index);
      tail.nodeValue = (tail.nodeValue ?? '').slice(hit.m[0].length);
      const holder = document.createElement('span');
      holder.innerHTML = varChip(hit.m[1]);
      const chip = holder.firstChild as HTMLElement;
      const zw = document.createTextNode('​');
      tail.parentNode!.insertBefore(chip, tail);
      tail.parentNode!.insertBefore(zw, tail);
      this.adoptContext(chip);
      const sel = window.getSelection();
      const r = document.createRange();
      r.setStart(zw, 1);
      r.collapse(true);
      sel?.removeAllRanges();
      sel?.addRange(r);
      this.saveRange();
    }
  }

  stats(): string {
    const d = parseForText(this.value() ?? '');
    const vars = d.querySelectorAll('[data-var]').length;
    d.querySelectorAll('[data-var]').forEach(x => x.remove());
    const txt = (d.textContent ?? '').replace(/​/g, '').trim();
    const words = txt ? txt.split(/\s+/).length : 0;
    return `${words} ${words === 1 ? 'cuvânt' : 'cuvinte'} · ${txt.length} caractere · ${vars} ${vars === 1 ? 'variabilă' : 'variabile'}`;
  }
}

/** Textul curat, fara chip-uri si fara HTML — pentru validarea "e gol?". */
export function plainText(html: string): string {
  const d = parseForText(html);
  return (d.textContent ?? '').replace(/​/g, '').trim();
}
