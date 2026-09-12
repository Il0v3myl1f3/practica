import { Component, ElementRef, HostListener, computed, inject, input, model, signal } from '@angular/core';
import { IconComponent } from './icon.component';
import { ChevronDown } from './icons';

export interface SelectOption {
  id: string;
  title: string;
  count?: number;
}

@Component({
  selector: 'app-select',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div class="wrap">
      <button
        type="button"
        class="trigger"
        [class.hot]="open() || !!value()"
        [attr.aria-label]="label()"
        [attr.aria-expanded]="open()"
        aria-haspopup="listbox"
        (click)="open.set(!open())"
        (keydown)="onTriggerKey($event)"
      >
        <span class="value">{{ valueLabel() }}</span>
        <app-icon class="chev" [class.up]="open()" [icon]="I.ChevronDown" [size]="20" />
      </button>

      @if (open()) {
        <div class="list" role="listbox" [attr.aria-label]="label()">
          @for (o of options(); track o.id; let i = $index) {
            <button
              type="button"
              class="option"
              role="option"
              [class.on]="o.id === value()"
              [class.active]="i === activeIndex()"
              [attr.aria-selected]="o.id === value()"
              (click)="pick(o.id)"
            >
              <span>{{ o.title }}</span>
              @if (o.count !== undefined) {
                <span class="count">{{ o.count }}</span>
              }
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .wrap { position: relative; flex: 0 1 auto; min-width: 0; }
      .trigger {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        height: 40px;
        min-width: 180px;
        padding: 0 12px;
        background: var(--surface);
        border: 1px solid var(--faint);
        border-radius: var(--r-md);
        font-family: inherit;
        font-size: 14px;
        line-height: 20px;
        color: var(--ink);
        text-align: left;
        cursor: pointer;
        transition: border-color 150ms ease-out;
      }
      .trigger.hot { border: 2px solid var(--brand); padding: 0 11px; }
      .value { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .chev { flex: none; color: var(--ink-3); transition: transform 150ms ease-out; }
      .chev.up { transform: rotate(180deg); }
      .list {
        position: absolute;
        z-index: 20;
        top: calc(100% + 4px);
        left: 0;
        min-width: 100%;
        max-height: 320px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 4px;
        padding: 8px;
        background: var(--surface);
        border-radius: var(--r-xl);
        box-shadow: var(--shadow-menu);
      }
      .option {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        min-height: 40px;
        padding: 12px 16px;
        border: none;
        border-radius: var(--r-md);
        background: var(--surface);
        color: var(--ink-2);
        font-family: inherit;
        font-size: 14px;
        line-height: 20px;
        font-weight: 500;
        text-align: left;
        cursor: pointer;
        transition: background 150ms ease-out;
      }
      .option:hover, .option.active { background: #f5f5f5; }
      .option.on { background: #f5f5f5; color: var(--brand); }
      .count { font-weight: 400; color: var(--muted); }
    `,
  ],
})
export class SelectComponent {
  private el = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly label = input.required<string>();
  readonly options = input.required<SelectOption[]>();
  readonly value = model<string>('');

  readonly I = { ChevronDown };
  readonly open = signal(false);
  readonly activeIndex = signal(-1);

  valueLabel = computed(() => {
    const current = this.options().find(o => o.id === this.value()) ?? this.options()[0];
    if (!current) return this.label();
    return current.count === undefined ? current.title : `${current.title} · ${current.count}`;
  });

  pick(id: string): void {
    this.value.set(id);
    this.open.set(false);
    this.activeIndex.set(-1);
  }

  onTriggerKey(e: KeyboardEvent): void {
    if (e.key === 'Escape') {
      this.open.set(false);
      return;
    }
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp' && e.key !== 'Enter') return;

    if (!this.open()) {
      e.preventDefault();
      this.open.set(true);
      this.activeIndex.set(this.options().findIndex(o => o.id === this.value()));
      return;
    }

    e.preventDefault();
    if (e.key === 'Enter') {
      const active = this.options()[this.activeIndex()];
      if (active) this.pick(active.id);
      return;
    }
    const step = e.key === 'ArrowDown' ? 1 : -1;
    const last = this.options().length - 1;
    const next = this.activeIndex() + step;
    this.activeIndex.set(next < 0 ? last : next > last ? 0 : next);
  }

  @HostListener('document:mousedown', ['$event'])
  onDocDown(e: MouseEvent): void {
    if (this.open() && !this.el.nativeElement.contains(e.target as Node)) {
      this.open.set(false);
      this.activeIndex.set(-1);
    }
  }
}
