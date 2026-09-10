import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API } from './api.service';

const TOKEN_KEY = 'notificari-mud.token';
const LOGIN_KEY = 'notificari-mud.login';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly login = signal<string>(read(LOGIN_KEY) ?? '');
  readonly token = signal<string>(read(TOKEN_KEY) ?? '');

  isAuthenticated(): boolean {
    return !!this.token();
  }

  initials(): string {
    return (this.login() || 'u').slice(0, 2).toUpperCase();
  }

  signIn(username: string, password: string): Observable<{ id_token: string }> {
    return this.http.post<{ id_token: string }>(`${API}/api/authenticate`, { username, password }).pipe(
      tap(res => {
        this.token.set(res.id_token);
        this.login.set(username);
        write(TOKEN_KEY, res.id_token);
        write(LOGIN_KEY, username);
      }),
    );
  }

  signOut(): void {
    this.token.set('');
    remove(TOKEN_KEY);
    this.router.navigate(['/login']);
  }
}

/** Tokenul merge pe fiecare cerere către API, nu și către alte origini. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  if (!token || !req.url.startsWith(API)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

// localStorage poate arunca (fereastra privată, cookies blocate): sesiunea
// trebuie doar să nu se păstreze, nu să pice aplicația.
function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* sesiune doar în memorie */
  }
}

function remove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    /* nimic de curățat */
  }
}
