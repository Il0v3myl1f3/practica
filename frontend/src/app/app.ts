import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastService } from './core/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <router-outlet />
    @if (toast.message()) {
      <div class="toast">{{ toast.message() }}</div>
    }
  `,
})
export class App {
  readonly toast = inject(ToastService);
}
