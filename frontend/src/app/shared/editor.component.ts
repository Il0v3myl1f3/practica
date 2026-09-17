import { Component, ElementRef, HostListener, model, signal, viewChild } from '@angular/core';
import { VARS } from '../core/models';
import { IconComponent } from './icon.component';
import { Baseline, Bold, Braces, ChevronDown, Eraser, Italic, Link, List, ListOrdered, Underline } from './icons';

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

/** Comenzile aplicate manual prin Range (toggleFormat), nu prin execCommand - vezi exec(). */
const MANUAL = ['bold', 'italic', 'underline', 'foreColor'];

/** Proprietatea CSS pe care o scrie fiecare comanda din MANUAL. */
const PROP: Record<string, string> = {
  bold: 'font-weight',
  italic: 'font-style',
  underline: 'text-decoration-line',
  foreColor: 'color',
};

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

        <div class="menu-wrap" data-menu>
          <button
            type="button"
            class="icon-btn"
            title="Inserează link"
            [class.active]="menu() === 'link'"
            (mousedown)="openLink($event)"
          >
            <app-icon [icon]="I.Link" [size]="16" />
          </button>
          @if (menu() === 'link') {
            <div class="menu link-menu">
              <input
                type="url"
                class="link-input"
                placeholder="https://exemplu.ro"
                [value]="linkUrl()"
                (input)="linkUrl.set($any($event.target).value)"
                (keydown.enter)="applyLink($event)"
              />
              <div class="link-actions">
                <button type="button" class="menu-item" [disabled]="!linkUrl().trim()" (mousedown)="applyLink($event)">Salvează</button>
                @if (editingLink()) {
                  <button type="button" class="menu-item" (mousedown)="removeLink($event)">Elimină link</button>
                }
              </div>
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
        (keydown)="onKeyDown($event)"
        (keyup)="onSelect()"
        (mouseup)="onSelect($event)"
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
      .link-menu { min-width: 240px; gap: 8px; }
      .link-input {
        height: 32px;
        padding: 0 10px;
        border: 1px solid var(--line);
        border-radius: var(--r-sm);
        font-size: 13px;
        color: var(--ink);
      }
      .link-actions { display: flex; gap: 4px; }
      .link-actions .menu-item { flex: 1; justify-content: center; }
      .link-actions .menu-item:disabled { color: var(--muted); cursor: not-allowed; }
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

  readonly menu = model<'size' | 'color' | 'vars' | 'link' | null>(null);
  /** Comenzile active pe selectia curenta, ca sa se aprinda butoanele din bara. */
  readonly activeCmds = signal('');
  /** URL-ul din campul meniului de link, si daca selectia curenta e deja un link existent. */
  readonly linkUrl = signal('');
  readonly editingLink = signal(false);
  readonly I = { Bold, Italic, Underline, List, ListOrdered, Eraser, Baseline, Braces, ChevronDown, Link };
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

  /**
   * Cursorul/selectia s-a mutat: retinem pozitia si aprindem butoanele potrivite.
   * Daca a ajuns in interiorul unui chip (Firefox permite uneori un click direct
   * inauntru, spre deosebire de Chrome), il scoatem imediat pe partea cea mai
   * apropiata de unde s-a dat clic.
   */
  onSelect(e?: MouseEvent): void {
    const chip = this.chipAtCaret();
    if (chip) {
      const rect = chip.getBoundingClientRect();
      this.exitChip(chip, e && e.clientX < rect.left + rect.width / 2 ? -1 : 1);
    }
    this.saveRange();
    this.readState();
  }

  /**
   * Firefox nu trateaza mereu contenteditable=false ca un bloc atomic la
   * navigarea cu sageata: cursorul poate ajunge in interiorul span-ului
   * chip-ului si ramane blocat acolo, fara sa mai iasa cu nicio apasare (nu se
   * reproduce in Chromium). Doua plase de siguranta: daca cursorul e deja in
   * interiorul unui chip (indiferent cum a ajuns acolo - click, stare veche),
   * il scoatem direct; altfel, daca urmeaza sa intre intr-un chip lipit de
   * cursor, sarim noi peste el inainte sa apuce browserul sa incerce. Fara asta,
   * chiar si acolo unde chip-ul e tratat atomic (Chrome), fiecare ZWSP din jurul
   * lui costa o apasare separata care nu misca nimic vizual.
   */
  onKeyDown(e: KeyboardEvent): void {
    if (e.shiftKey || e.altKey || e.ctrlKey || e.metaKey) return;
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
    const sel = window.getSelection();
    if (!sel || !sel.rangeCount || !sel.isCollapsed) return;
    const el = this.host().nativeElement;
    if (!el.contains(sel.anchorNode)) return;
    const dir: 1 | -1 = e.key === 'ArrowRight' ? 1 : -1;
    const chip = this.chipAtCaret() ?? this.adjacentChip(sel.getRangeAt(0), dir);
    if (!chip) return;
    e.preventDefault();
    this.exitChip(chip, dir);
    this.saveRange();
  }

  /** Chip-ul care contine cursorul, daca acesta a ajuns in interiorul lui. */
  private chipAtCaret(): HTMLElement | null {
    const sel = window.getSelection();
    if (!sel || !sel.rangeCount || !sel.isCollapsed) return null;
    const el = this.host().nativeElement;
    const node = sel.anchorNode;
    if (!node || !el.contains(node)) return null;
    const from = node.nodeType === Node.ELEMENT_NODE ? (node as Element) : node.parentElement;
    return from?.closest<HTMLElement>('[data-var]') ?? null;
  }

  /**
   * Chip-ul spre care s-ar indrepta cursorul, sarind peste ZWSP-urile de umplutura din
   * cale (invizibile, nu conteaza pentru navigare - doua chip-uri inserate unul dupa
   * altul lasa cate un ZWSP separat de fiecare parte, nu unul singur). Fara saltul
   * asta, fiecare ZWSP dintre chip-uri alaturate ar costa o apasare separata care nu
   * misca nimic vizual, chiar daca fiecare chip in sine e deja atomic pentru cursor.
   */
  private adjacentChip(range: Range, dir: 1 | -1): HTMLElement | null {
    let container: Node = range.startContainer;
    let idx = range.startOffset;
    for (let guard = 0; guard < 6; guard++) {
      let sib: ChildNode | null;
      if (container.nodeType === Node.TEXT_NODE) {
        const value = container.nodeValue ?? '';
        // Ce mai ramane de parcurs in acest nod, in directia de mers, pana la capat -
        // fie tot nodul (doi chip-uri alaturate, separate doar de ZWSP), fie coada lui
        // lipita de text real ("Salut " + ZWSP, un singur nod). Daca partea ramasa e
        // numai ZWSP, n-are "mijloc" vizual - o tratam ca fiind deja la marginea
        // dinspre chip, indiferent de cate ZWSP mai sunt de "trecut" pana acolo, ca sa
        // nu coste o apasare separata doar pentru ele inainte de saltul peste chip.
        const rest = dir === -1 ? value.slice(0, idx) : value.slice(idx);
        const atEdge = /^​*$/.test(rest);
        if (!atEdge) return null;
        sib = dir === -1 ? container.previousSibling : container.nextSibling;
      } else {
        sib = container.childNodes[dir === -1 ? idx - 1 : idx] ?? null;
      }
      if (!sib) return null;
      if (sib instanceof HTMLElement && sib.hasAttribute('data-var')) return sib;
      if (sib.nodeType === Node.TEXT_NODE && /^​*$/.test(sib.nodeValue ?? '')) {
        const parent = sib.parentNode as Node;
        idx = Array.prototype.indexOf.call(parent.childNodes, sib) + (dir === -1 ? 0 : 1);
        container = parent;
        continue;
      }
      return null;
    }
    return null;
  }

  /**
   * Muta cursorul chiar inainte sau chiar dupa chip, in afara lui. Preferam sa
   * aterizam in ZWSP-ul vecin (nod text) in loc de direct la marginea
   * elementului: Firefox deseneaza un chenar de "selectie" in jurul unui
   * contenteditable=false cand Range-ul se opreste chiar la marginea lui, in
   * loc de un caret normal - un nod text vecin evita complet acel desen.
   */
  private exitChip(chip: HTMLElement, dir: 1 | -1): void {
    const sel = window.getSelection();
    if (!sel) return;
    const r = document.createRange();
    const sib = dir === 1 ? chip.nextSibling : chip.previousSibling;
    if (sib && sib.nodeType === Node.TEXT_NODE) {
      r.setStart(sib, dir === 1 ? 0 : (sib as Text).length);
    } else if (dir === 1) {
      r.setStartAfter(chip);
    } else {
      r.setStartBefore(chip);
    }
    r.collapse(true);
    sel.removeAllRanges();
    sel.addRange(r);
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
    // Chip-urile se strang inainte: si execCommand, si toggleFormat rescriu nodurile din selectie.
    const chips = this.chipsInSelection();
    const sel = window.getSelection();
    const range = sel && sel.rangeCount ? sel.getRangeAt(0) : null;
    // Starea dorita se citeste inainte de comanda, ca sa urmam aceeasi logica de comutare
    // ca un toggle normal: daca tot ce-i selectat e deja formatat, se scoate; altfel se pune.
    const on = TOGGLES.includes(cmd) ? !this.allFormatted(range, chips, cmd) : false;
    if (range && !range.collapsed && MANUAL.includes(cmd)) {
      // Firefox (Gecko): execCommand('bold'/...) pe o selectie care contine un chip
      // contenteditable=false NU formateaza doar in jurul lui - aduna tot textul editabil
      // din selectie intr-un singur span nou si muta chip-urile dupa el, stricand ordinea
      // (confirmat cu innerHTML inainte/dupa, direct din Firefox). Range API (extractContents/
      // insertNode), folosit deja de applySize(), nu are conceptul asta - muta exact nodurile
      // din Range, in ordinea lor originala, indiferent de contenteditable.
      // foreColor nu e toggle (TOGGLES nu-l contine, deci `on` de mai sus e mereu false) -
      // pentru el toggleFormat trebuie mereu sa impacheteze/aplice culoarea, niciodata sa o scoata.
      this.toggleFormat(range, cmd, TOGGLES.includes(cmd) ? on : true, val);
    } else if (range) {
      // Selectie goala (doar cursor, ex. "apasa Bold, apoi scrie") sau comenzi ramase pe
      // execCommand (liste, removeFormat) - stilul de scriere "in asteptare" la cursor e nativ
      // browserului, nu are sens sa-l reimplementam.
      try {
        document.execCommand('styleWithCSS', false, 'true');
      } catch {
        /* browserul nu suporta, formatarea merge oricum */
      }
      document.execCommand(cmd, false, val);
    }
    chips.forEach(chip => this.styleChip(chip, cmd, on, val));
    // Range-ul a rescris nodurile (split/extract/insert): nodul vechi al liveRange-ului poate fi
    // scurtat sau scos din document, fara sa para "corupt". Fortam reconstructia din offsete.
    this.liveRange = null;
    this.menu.set(null);
    this.reselect();
    this.readState();
    this.push();
  }

  /**
   * Inlocuieste execCommand(cmd, ...) pentru Bold/Italic/Underline/Culoare pe o selectie
   * nevida: acelasi tipar ca applySize() - extractContents()/insertNode() muta exact
   * nodurile din Range, pastrandu-le ordinea, fara "curatarea" facuta de execCommand in
   * Firefox care rearanja chip-urile.
   */
  private toggleFormat(range: Range, cmd: string, on: boolean, val?: string): void {
    this.snapOut(range);
    const frag = range.extractContents();
    if (on) {
      const span = document.createElement('span');
      this.styleChip(span, cmd, true, val);
      // Un descendent cu aceeasi proprietate setata explicit (ex. font-style:normal ramas
      // dintr-un toggle anterior) ar bate valoarea noua a wrapper-ului, oricat de "afara"
      // ar fi acesta - o proprietate setata direct pe element bate mereu una mostenita.
      frag.querySelectorAll<HTMLElement>('*').forEach(el => el.style.removeProperty(PROP[cmd]));
      span.append(frag);
      range.insertNode(span);
    } else {
      frag.querySelectorAll<HTMLElement>('*').forEach(el => {
        if (this.hasFormat(el, cmd)) this.styleChip(el, cmd, false);
      });
      range.insertNode(frag);
    }
    // Cand range-ul incepe/se termina exact la granita unui nod (nu in interiorul lui),
    // extractContents() poate lasa artefacte la acea granita: noduri text goale ("") -
    // normalize() le sterge si lipeste perechile de text vecine - sau, mai rar, un <span>
    // clon complet gol. Niciunul din ele nu are vreun rol in acest editor.
    const el = this.host().nativeElement;
    el.querySelectorAll<HTMLElement>('span').forEach(span => {
      if (!span.textContent && !span.querySelector('[data-var]')) {
        span.remove();
      } else if (span.getAttribute('style') === '' && span.attributes.length === 1) {
        // Un <span style=""> fara alt atribut nu (mai) formateaza nimic - doar un invelis
        // ramas dupa ce styleChip() i-a golit ultima proprietate. Toggle-uri repetate ar
        // acumula tot mai multe din astea, imbricate; il scoatem, pastrandu-i continutul.
        span.replaceWith(...Array.from(span.childNodes));
      }
    });
    // Cand range-ul incepe/se termina exact la granita unui nod (nu in interiorul lui),
    // extractContents() poate lasa si noduri text goale ("") la acea granita - normalize()
    // le sterge si lipeste perechile de text vecine (inclusiv cele create de unwrap-ul de mai sus).
    el.normalize();
  }

  /**
   * Selectia curenta are deja formatarea `cmd` peste tot - pe chip-uri si pe textul editabil
   * deopotriva. Inlocuieste document.queryCommandState(cmd): calculam noi starea, in loc sa
   * ne bazam pe browser, ca sa nu depindem de execCommand nici macar pentru citit starea.
   */
  private allFormatted(range: Range | null, chips: HTMLElement[], cmd: string): boolean {
    if (!range || range.collapsed) return false;
    if (!chips.every(chip => this.hasFormat(chip, cmd))) return false;
    const el = this.host().nativeElement;
    // Range.intersectsNode()/compareBoundaryPoints() sunt "generoase": un nod care doar
    // atinge granita range-ului (ex. un text gol lasat de extractContents la granita, sau
    // textul de dinaintea inceputului selectiei) poate iesi "inclus" din cauza modului cum
    // selectNode() compara pozitii relativ la parinte, nu la caractere. Recalculam offsetele
    // de caracter ale range-ului, la fel ca saveRange()/rangeAt(), si verificam suprapunere
    // aritmetica - acelasi sistem de coordonate, deja dovedit corect in tot restul fisierului.
    const before = document.createRange();
    before.selectNodeContents(el);
    before.setEnd(range.startContainer, range.startOffset);
    const start = before.toString().length;
    const end = start + range.toString().length;
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    let seen = 0;
    let any = false;
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      const len = (n.nodeValue ?? '').length;
      const overlaps = seen < end && seen + len > start;
      seen += len;
      if (!overlaps || (n.parentElement)?.closest('[data-var]')) continue;
      any = true;
      if (!this.isFormatted(n.parentElement, cmd)) return false;
    }
    return any || chips.length > 0;
  }

  /** Urca prin parinti pana la canvas, cautand un ancestor cu formatarea `cmd` pe el. */
  private isFormatted(el: Element | null, cmd: string): boolean {
    const host = this.host().nativeElement;
    for (let node = el; node && node !== host; node = node.parentElement) {
      if (node instanceof HTMLElement && this.hasFormat(node, cmd)) return true;
    }
    return false;
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

  /** Linkul (daca exista) care contine nodul unde e cursorul/inceputul selectiei. */
  private linkAtSelection(): HTMLAnchorElement | null {
    const sel = window.getSelection();
    if (!sel || !sel.rangeCount) return null;
    const el = this.host().nativeElement;
    const node = sel.anchorNode;
    if (!node || !el.contains(node)) return null;
    const from = node.nodeType === Node.ELEMENT_NODE ? (node as Element) : node.parentElement;
    return from?.closest<HTMLAnchorElement>('a[href]') ?? null;
  }

  /** Deschide meniul de link, precompletat cu URL-ul existent daca selectia e deja un link. */
  openLink(e: Event): void {
    e.preventDefault();
    this.saveRange();
    if (this.menu() === 'link') {
      this.menu.set(null);
      return;
    }
    const a = this.linkAtSelection();
    this.linkUrl.set(a?.getAttribute('href') ?? '');
    this.editingLink.set(!!a);
    this.menu.set('link');
  }

  /**
   * Impacheteaza selectia intr-un <a>, la fel ca applySize() - Range API, nu execCommand,
   * ca sa nu rearanjeze chip-urile prinse in selectie (acelasi motiv ca in exec()/toggleFormat()).
   * Fara text selectat, doar actualizam href-ul unui link existent la cursor.
   */
  applyLink(e: Event): void {
    e.preventDefault();
    const url = this.linkUrl().trim();
    if (!url) return;
    const href = /^[a-z][a-z0-9+.-]*:/i.test(url) ? url : `https://${url}`;
    this.restore();
    const sel = window.getSelection();
    const range = sel && sel.rangeCount ? sel.getRangeAt(0) : null;
    if (!range) return;
    if (range.collapsed) {
      const a = this.linkAtSelection();
      if (a) a.href = href;
    } else {
      this.snapOut(range);
      const frag = range.extractContents();
      // Un link nu poate contine alt link - daca selectia se suprapune partial cu unul
      // existent, scoatem invelisul vechi, pastrandu-i continutul.
      frag.querySelectorAll('a').forEach(old => old.replaceWith(...Array.from(old.childNodes)));
      const a = document.createElement('a');
      a.href = href;
      a.target = '_blank';
      a.rel = 'noopener noreferrer';
      a.append(frag);
      range.insertNode(a);
      this.host().nativeElement.normalize();
    }
    this.liveRange = null;
    this.menu.set(null);
    this.linkUrl.set('');
    this.reselect();
    this.readState();
    this.push();
  }

  /** Scoate link-ul de la cursor/selectie, pastrandu-i continutul. */
  removeLink(e: Event): void {
    e.preventDefault();
    this.restore();
    const a = this.linkAtSelection();
    if (a) {
      a.replaceWith(...Array.from(a.childNodes));
      this.host().nativeElement.normalize();
    }
    this.liveRange = null;
    this.menu.set(null);
    this.linkUrl.set('');
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

  /** Starea curenta a unui element (chip sau span de formatare) - oglinda lui styleChip. */
  private hasFormat(el: HTMLElement, cmd: string): boolean {
    switch (cmd) {
      case 'bold':
        return el.style.fontWeight === 'bold' || Number(el.style.fontWeight) >= 700;
      case 'italic':
        return el.style.fontStyle === 'italic';
      case 'underline':
        return el.style.textDecorationLine === 'underline';
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
