import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly message = signal('');
  private timer: ReturnType<typeof setTimeout> | null = null;

  show(text: string): void {
    this.message.set(text);
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => this.message.set(''), 2600);
  }

  /** Erorile de la API vin ca {message}; altfel arătăm ceva inteligibil. */
  error(err: unknown): void {
    const body = (err as { error?: { message?: string; detail?: string } })?.error;
    this.show(body?.message ?? body?.detail ?? 'Ceva nu a mers. Încearcă din nou.');
  }
}
