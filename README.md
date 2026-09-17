# Practică — Aplicație de trimitere mesaje

Aplicație internă pentru trimiterea de anunțuri către angajați pe **email, Telegram
și Discord**, dintr-un singur loc: șabloane reutilizabile, selecție manuală
a destinatarilor și istoric complet al trimiterilor.

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

La prima pornire pe o bază goală, `DevDataSeeder` creează organizația și cele 4
șabloane. **Destinatarii și grupurile nu se populează** — listele pornesc
goale, ca să adaugi manual sau să imporți din CSV exact pe cine vrei să testezi,
fără să trimiți din greșeală către adrese inventate.

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
destinatari cu import/export CSV (cu ecran de verificare unde poți corecta
liniile citite din fișier) și cu conectarea chat-urilor de Telegram și a
membrilor de Discord, șabloane cu import/export ZIP, și fluxul de trimitere în
trei pași cu editor bogat, variabile de personalizare și rezolvarea conflictelor
de canal.

Trimiterea creează livrări reale, una per destinatar și canal, din care se
calculează starea mesajului. Livrările intră într-o **coadă asincronă**: cererea
HTTP răspunde imediat cu `QUEUED`, iar un dispecer le trimite în fundal, cu
reîncercări și backoff la erorile trecătoare.

Providerul se alege per canal din configurație. **Emailul** poate pleca real prin
SMTP (Gmail), **Telegram** prin Bot API și **Discord** ca mesaj privat (DM) prin
Discord API, toate în spatele aceleiași interfețe `ChannelSender`. Mock-ul nu se înlocuiește — rămâne un provider de sine
stătător, disponibil pentru orice canal, ca trimiterile să poată fi oprite sau
simulate fără să se atingă codul.

```bash
# ca emailurile să plece cu adevărat (App password din contul Google, nu parola)
setx MESSAGING_EMAIL_PROVIDER "smtp"
setx MAIL_USERNAME "adresa@gmail.com"
setx MAIL_PASSWORD "parola-de-aplicatie"

# ca mesajele Telegram să plece cu adevărat (tokenul vine de la @BotFather)
setx MESSAGING_TELEGRAM_PROVIDER "telegram-bot"
setx TELEGRAM_BOT_TOKEN "123456789:AA..."

# ca mesajele Discord să plece cu adevărat (tokenul vine din Developer Portal)
setx MESSAGING_DISCORD_PROVIDER "discord-bot"
setx DISCORD_BOT_TOKEN "token-ul-botului"
setx DISCORD_GUILD_ID "id-ul-serverului"
```

Fără aceste variabile, canalele rămân pe mock — deliberat, ca un rând verde din
istoric să nu fie niciodată ambiguu. Iar cu un provider real și credențiale lipsă,
aplicația refuză să pornească și spune ce lipsește.

La Telegram și la Discord mai e un pas, care nu ține de configurare: **un bot nu
poate scrie primul**. La Telegram, fiecare destinatar trebuie să deschidă botul și
să apese *Start*, iar chat ID-ul lui se leagă din **Destinatari → Conectează
Telegram**. La Discord, botul poate trimite DM doar cuiva de pe același server —
destinatarul trebuie invitat pe server, iar user ID-ul lui se leagă din
**Destinatari → Conectează Discord**. Ambele ecrane arată linkul de trimis
oamenilor (invitație, respectiv `t.me/<bot>`) și lista celor deja disponibili de legat.

Detalii complete — toate cheile de configurare, API-ul REST, ciclul de viață al
unei livrări și cum adaugi un canal real: **[`backend/docs/notificari.md`](backend/docs/notificari.md)**.

## De făcut

- [ ] Index unic în baza de date pentru email/grup/șablon per organizație
      (verificarea există deja în service layer)
- [ ] Webhook-uri de la provideri pentru starea reală de livrare (`DELIVERED`)
