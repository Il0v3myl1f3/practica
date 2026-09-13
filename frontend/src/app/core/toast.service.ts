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

  /**
   * `detail` e textul în română pus de EntityErrorException; `message` e doar cheia
   * ("error.chatidexists"), bună de citit în log, nu de arătat omului. De aceea
   * detail are prioritate, iar cheia rămâne ultima plasă de siguranță.
   */
  error(err: unknown): void {
    const body = (err as { error?: { message?: string; detail?: string } })?.error;
    this.show(body?.detail ?? body?.message ?? 'Ceva nu a mers. Încearcă din nou.');
  }
}
