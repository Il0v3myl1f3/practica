import { Component, ElementRef, effect, inject, input } from '@angular/core';
import type { IconNode } from 'lucide';

export type { IconNode };

const SVG_NS = 'http://www.w3.org/2000/svg';

/**
 * Randeaza un icon Lucide.
 *
 * Wrapper-ul oficial lucide-angular accepta doar Angular 13-21, asa ca luam
 * pachetul de baza (fara nicio dependinta de framework) si construim SVG-ul
 * direct. Construirea imperativa acopera orice forma de icon fara sa enumeram
 * fiecare tag si atribut in template.
 *
 * Grosimea implicita 1.5 e cea din prototip; Lucide vine cu 2.
 */
@Component({
  selector: 'app-icon',
  standalone: true,
  template: '',
  styles: [':host { display: inline-flex; align-items: center; justify-content: center; flex: none; }'],
})
export class IconComponent {
  readonly icon = input.required<IconNode>();
  readonly size = input(18);
  readonly stroke = input(1.5);

  private host = inject(ElementRef<HTMLElement>);

  constructor() {
    effect(() => {
      const svg = document.createElementNS(SVG_NS, 'svg');
      svg.setAttribute('width', String(this.size()));
      svg.setAttribute('height', String(this.size()));
      svg.setAttribute('viewBox', '0 0 24 24');
      svg.setAttribute('fill', 'none');
      svg.setAttribute('stroke', 'currentColor');
      svg.setAttribute('stroke-width', String(this.stroke()));
      svg.setAttribute('stroke-linecap', 'round');
      svg.setAttribute('stroke-linejoin', 'round');
      svg.setAttribute('aria-hidden', 'true');

      for (const [tag, attrs] of this.icon()) {
        const node = document.createElementNS(SVG_NS, tag);
        for (const [k, v] of Object.entries(attrs)) {
          if (v != null) {
            node.setAttribute(k, String(v));
          }
        }
        svg.appendChild(node);
      }

      const el = this.host.nativeElement as HTMLElement;
      el.replaceChildren(svg);
    });
  }
}
