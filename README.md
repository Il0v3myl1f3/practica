# Practică — Aplicație de trimitere mesaje

Aplicație internă pentru trimiterea de anunțuri către angajați pe **email, Telegram
și WhatsApp**, dintr-un singur loc: șabloane reutilizabile, selecție manuală a
destinatarilor și istoric complet al trimiterilor.

Monorepo cu două proiecte independente:

| Folder | Ce e | Stare |
|---|---|---|
| [`backend/`](backend) | API REST — Spring Boot 4 + JHipster 9, PostgreSQL, JWT | funcțional |
| [`frontend/`](frontend) | Client Angular 22 (SCSS, fără SSR) | funcțional |
| [`frontend/design/`](frontend/design) | Prototipul de UI și token-urile de design MUD | referință |

## Pornire rapidă

```bash
cd backend
docker compose -f src/main/docker/postgresql.yml up -d   # baza de date
./mvnw                                                   # API pe :8080, admin/admin

cd ../frontend && npm start                              # UI pe :4200
```

Baza de date este **PostgreSQL 18.4 în Docker**, atât în dezvoltare cât și în
producție. Datele stau în volumul `notificari-mud-db` și supraviețuiesc opririi
sau ștergerii containerului:

```bash
docker compose -f src/main/docker/postgresql.yml down      # datele rămân
docker compose -f src/main/docker/postgresql.yml down -v   # șterge și datele
```

La prima pornire pe o bază goală, `DevDataSeeder` încarcă setul din prototip:
34 destinatari, 4 grupuri, 4 șabloane și istoricul de 9 trimiteri.

## Modelul de date

Totul pleacă din [`backend/app.jdl`](backend/app.jdl) — 10 entități, plus, la
finalul fișierului, regulile de business care nu se pot exprima în JDL și se
implementează manual în service layer.

```
Organization ─┬─ Membership ── User
              ├─ RecipientGroup ── Recipient ── RecipientChannel
              ├─ MessageTemplate
              └─ Message ─┬─ MessageChannel
                          ├─ MessageRecipient ── Recipient
                          └─ MessageAttachment
```

Regenerare după modificarea modelului:

```bash
cd backend && npx jhipster jdl app.jdl
```

## Prototipul

`frontend/design/Trimitere Mesaje.dc.html` este prototipul complet, funcțional în
browser. El este sursa de adevăr pentru fluxurile aplicației: cei 3 pași de
compunere, rezolvarea conflictelor de canal, importul CSV cu validare și ecran de
verificare, variabilele de personalizare `{{nume}}`, `{{prenume}}`, `{{grup}}`,
`{{email}}`.

## Ce funcționează

Toate ecranele din prototip sunt implementate și legate de backend: login pe JWT,
panou cu indicatori, istoricul trimiterilor cu filtre și sortare, ciorne,
destinatari cu import/export CSV, șabloane, și fluxul de trimitere în trei pași
cu editor bogat, variabile de personalizare și rezolvarea conflictelor de canal.

Trimiterea creează livrări reale, una per destinatar și canal, din care se
calculează starea mesajului. **Emailul** pleacă prin SMTP (Gmail) de îndată ce
pui credențialele; până atunci se comportă ca mock, ca istoricul să nu fie tot
roșu. **Telegram** și **WhatsApp** sunt mock-uri, izolate în spatele interfeței
`ChannelSender` — se înlocuiesc fără să atingi restul.

```bash
# ca emailurile să plece cu adevărat (App password din contul Google, nu parola)
setx MAIL_USERNAME "adresa@gmail.com"
setx MAIL_PASSWORD "parola-de-aplicatie"
```

## De făcut

- [ ] Index unic în baza de date pentru email/grup/șablon per organizație
      (verificarea există deja în service layer)
- [ ] Clienți reali pentru Telegram și WhatsApp (înlocuiesc `MockChannelSender`)
