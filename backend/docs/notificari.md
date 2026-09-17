# Trimiterea notificărilor

Cum funcționează, cum se configurează și cum se extinde sistemul de trimitere a
anunțurilor către destinatari.

## Cuprins

1. [Ce face](#1-ce-face)
2. [Configurare](#2-configurare)
3. [API REST](#3-api-rest)
4. [Arhitectura și ciclul de viață](#4-arhitectura-și-ciclul-de-viață)
5. [Cum adaugi un canal real](#5-cum-adaugi-un-canal-real)
6. [Operare și depanare](#6-operare-și-depanare)

---

## 1. Ce face

Un mesaj compus în UI se transformă în **câte o livrare per (destinatar, canal)**.
Livrările intră într-o coadă persistată, iar un dispecer le trimite în fundal, cu
reîncercări la erorile trecătoare. Cererea HTTP nu așteaptă trimiterea.

```
  compunere (UI)
        │
        ▼
  POST /messages/send ──► Message (QUEUED)
        │                 + un rând MessageRecipient per (destinatar, canal)
        │                   PENDING (are adresă) sau SKIPPED (nu are)
        ▼
  răspuns imediat: { status: "QUEUED", queued: N }
                          │
                          │   la fiecare 5s, în fundal
                          ▼
                    DeliveryDispatcher
                          │  revendică un lot
                          ▼
                    ChannelSender (smtp / telegram-bot / mock)
                          │
                          ▼
                    SENT sau FAILED  ──►  statusul mesajului recalculat
                                          (SENT / PARTIAL / FAILED)
                          ▲
                          │
  GET /messages/{id}/status  ◄── UI reinteroghează până nu mai e QUEUED
```

Starea canalelor astăzi:

| Canal | Providere disponibile | Starea |
|---|---|---|
| `EMAIL` | `smtp`, `mock` | **poate trimite real**, prin SMTP |
| `TELEGRAM` | `telegram-bot`, `mock` | **poate trimite real**, prin Bot API |
| `DISCORD` | `discord-bot`, `mock` | **poate trimite real**, mesaj privat (DM) prin Discord API |

Mock-ul nu e un artificiu de dezvoltare pe care să-l ștergi: e un provider de
sine stătător, disponibil pentru **toate** canalele, inclusiv email. Trece tot
fluxul (coadă, retry, istoric) fără să iasă nimic din aplicație.

---

## 2. Configurare

Toate cheile stau sub `application.messaging` în
`src/main/resources/config/application.yml`, cu tipuri în
`config/ApplicationProperties.java`.

> **Atenție:** `ApplicationProperties` e `@ConfigurationProperties(prefix = "application",
> ignoreUnknownFields = false)`. O cheie `application.*` care nu are câmp Java
> corespunzător **oprește pornirea aplicației**. Dacă adaugi o cheie în yaml,
> adaug-o și în clasă.

### 2.1 Global

| Cheie | Implicit | Ce face |
|---|---|---|
| `enabled` | `true` | `false` → trimiterea e **refuzată** (409). Dispecerul stă. |
| `async` | `true` | `false` → se trimite inline, în cererea HTTP. |

### 2.2 Per canal — `channels.email.*`, `channels.telegram.*`, `channels.discord.*`

| Cheie | Implicit | Ce face |
|---|---|---|
| `provider` | `mock` | Id-ul providerului: `mock`, `smtp` (email), `telegram-bot` (Telegram), `discord-bot` (Discord). Trebuie să existe un `ChannelSender` cu acest `providerId()`, altfel aplicația nu pornește. |
| `batch-size` | `25` | Câte livrări ia dispecerul pe acest canal, la fiecare tick. |
| `min-interval` | `0` (dar `200ms` pentru email, `100ms` pentru Discord în `application.yml`) | Pauză între două trimiteri consecutive pe canal. Limitare de rată simplă. |
| `mock-failure-rate` | `0` | Doar pentru `provider: mock`: fracțiunea de trimiteri care eșuează, ca să poți exersa retry-ul. |
| `options.*` | — | Hartă liberă de setări specifice providerului. Pentru `telegram-bot`: `bot-token` și `base-url`. Pentru `discord-bot`: `bot-token`, `guild-id` și `base-url`. Un client nou nu cere câmpuri noi în `ApplicationProperties`. |

### 2.3 Dispecer — `dispatcher.*`

| Cheie | Implicit | Ce face |
|---|---|---|
| `poll-interval` | `5s` | Cât de des verifică dispecerul coada. |
| `visibility-timeout` | `5m` | Cât poate sta o livrare în `SENDING` până e considerată abandonată și reluată. |
| `max-attempts` | `5` | După câte încercări o eroare trecătoare devine `FAILED`. |
| `backoff-initial` | `30s` | Prima întârziere. |
| `backoff-multiplier` | `3` | Factorul de creștere. |
| `backoff-max` | `1h` | Plafonul întârzierii. |
| `backoff-jitter` | `0.2` | ±20% aleator peste întârziere. |

### 2.4 Cele două butoane — nu înseamnă același lucru

Aceasta e distincția de care depinde totul:

**`enabled: false` — oprit.**
`POST /messages/send` răspunde **409** cu `error.sendingdisabled`
(„Trimiterile sunt oprite din configurare."), iar dispecerul se întoarce imediat
din fiecare tick. Livrările deja `PENDING` **rămân în coadă** și se scurg când
repornești comutatorul — exact ce vrei pentru o fereastră de mentenanță.

**`channels.<canal>.provider: mock` — pornit, dar simulat.**
Trimiterea e acceptată, coada se scurge, istoricul se populează cu `SENT`, dar
nimic nu pleacă din aplicație. Util pentru demonstrații și pentru dezvoltare.

### 2.5 Pornirea emailului real

Sunt necesare **trei** variabile — nu e suficient să pui doar credențialele:

```bash
# Windows
setx MESSAGING_EMAIL_PROVIDER smtp
setx MAIL_USERNAME adresa@gmail.com
setx MAIL_PASSWORD parola-de-aplicatie   # App password din contul Google, nu parola contului
```

```bash
# Linux / macOS
export MESSAGING_EMAIL_PROVIDER=smtp
export MAIL_USERNAME=adresa@gmail.com
export MAIL_PASSWORD=parola-de-aplicatie
```

Apoi repornești aplicația. La pornire, logul spune ce e activ:

```
ChannelSenderRegistry : Canale de trimitere: EMAIL -> smtp · TELEGRAM -> mock · DISCORD -> mock
```

**Dacă pui `provider: smtp` fără `MAIL_USERNAME`, aplicația refuză să pornească**,
cu mesajul care spune exact ce lipsește. E deliberat. Varianta veche cădea tacit
pe mock când lipseau credențialele, iar rezultatul era că **nu puteai spune,
uitându-te la un rând verde „Livrat" din istoric, dacă emailul a plecat într-adevăr**.
`provider: mock` este fallback-ul — doar că acum e o alegere scrisă, nu un accident.

### 2.6 Pornirea Telegramului real

Două variabile, plus un pas care **nu e de configurare**: legarea chat-urilor.

```bash
# Windows
setx MESSAGING_TELEGRAM_PROVIDER telegram-bot
setx TELEGRAM_BOT_TOKEN 123456789:AA...   # tokenul primit de la @BotFather
```

```bash
# Linux / macOS
export MESSAGING_TELEGRAM_PROVIDER=telegram-bot
export TELEGRAM_BOT_TOKEN=123456789:AA...
```

Ca la email: `provider: telegram-bot` fără token **oprește pornirea**, cu mesajul
care spune ce lipsește.

**Pasul care surprinde: un bot nu poate scrie primul.** Telegram nu permite unui
bot să inițieze o conversație, iar Bot API **nu acceptă `@username`** pentru
persoane — are nevoie de `chat_id` numeric. Deci fiecare destinatar trebuie să
deschidă botul și să apese *Start*; abia atunci îi putem afla chat ID-ul.

Fluxul, din UI: **Destinatari → Conectează Telegram**. Ecranul arată linkul
`t.me/<bot>` de trimis oamenilor și lista celor care au scris botului, cu un
select pentru a lega fiecare chat la un destinatar. Legarea scrie
`RecipientChannel(TELEGRAM, address = chat_id, verified = true)`.

Două limite de platformă, vizibile în ecran:

- Telegram păstrează update-urile neconfirmate **~24h**. Cine nu mai apare în
  listă trebuie să scrie din nou botului.
- `getUpdates` e chemat **fără `offset`**, dinadins: un offset confirmă
  update-urile și Telegram nu le mai trimite niciodată, deci lista ar fi goală de
  a doua deschidere a ecranului.

Endpointurile din spate (vezi §3.6) răspund **409 `error.telegramnotactive`** cât
timp providerul activ pentru TELEGRAM nu e `telegram-bot` — mai bine o eroare
clară decât o listă goală care pare să spună „nimeni nu a scris".

| Opțiune | Implicit | Ce face |
|---|---|---|
| `options.bot-token` | `${TELEGRAM_BOT_TOKEN:}` | Tokenul de la @BotFather. Ajunge în calea URL-ului, deci nu se loghează niciodată. |
| `options.base-url` | `https://api.telegram.org` | Se schimbă doar pentru teste contra unui server fals. |
| `min-interval` | `50ms` în `application.yml` | ~20 mesaje/s, sub limita de ~30/s a Telegram. |

### 2.7 Pornirea Discordului real

Trei variabile, plus doi pași care **nu sunt de configurare**: invitarea botului
pe server și legarea user ID-urilor.

```bash
# Windows
setx MESSAGING_DISCORD_PROVIDER discord-bot
setx DISCORD_BOT_TOKEN token-ul-botului   # din Developer Portal -> Bot -> Reset Token
setx DISCORD_GUILD_ID id-ul-serverului    # Developer Mode -> click dreapta pe server -> Copy Server ID
```

```bash
# Linux / macOS
export MESSAGING_DISCORD_PROVIDER=discord-bot
export DISCORD_BOT_TOKEN=token-ul-botului
export DISCORD_GUILD_ID=id-ul-serverului
```

Ca la Telegram: `provider: discord-bot` fără token **oprește pornirea**, cu mesajul
care spune ce lipsește.

Pașii dintr-un Developer Portal Discord curat:

1. [discord.com/developers/applications](https://discord.com/developers/applications)
   → **New Application** → tab **Bot** → **Reset Token** (copiezi tokenul, apare o
   singură dată).
2. Tot pe tabul **Bot**: bifezi **Server Members Intent** — fără el,
   `GET /guilds/{id}/members` întoarce o listă goală sau eroare.
3. Tab **OAuth2 → URL Generator**: scope `bot`, permisiuni `0` (Send Messages nu e
   nevoie explicit — DM-urile nu cer permisiuni de canal), copiezi URL-ul generat și
   îl deschizi ca să inviți botul pe server.
4. În Discord (client, nu Developer Portal): **User Settings → Advanced → Developer
   Mode**, apoi click-dreapta pe server → **Copy Server ID** — ăsta e `DISCORD_GUILD_ID`.
5. Rulezi comenzile `setx`/`export` de mai sus și repornești aplicația.

**Pasul care surprinde, ca la Telegram: botul nu poate scrie primul cuiva de pe
alt server.** Discord permite DM doar între utilizatori care au un server comun cu
botul (regula de platformă, cod de eroare `50007` altfel). Deci fiecare destinatar
trebuie să fie deja pe serverul respectiv.

Fluxul, din UI: **Destinatari → Conectează Discord**. Ecranul arată numele
serverului, linkul de invitare al botului (pentru cine încă nu e pe server) și
lista membrilor (fără boți), cu un select pentru a lega fiecare membru la un
destinatar. Un user ID poate fi și scris manual, în formular sau în CSV, fără să
treci prin ecran — utile pentru cine ți-a dat ID-ul direct.

Legarea scrie `RecipientChannel(DISCORD, address = user_id, verified = true)`.

Endpointurile din spate (vezi §3.7) răspund **409 `error.discordnotactive`** cât
timp providerul activ pentru DISCORD nu e `discord-bot`, sau cât timp
`DISCORD_GUILD_ID` e gol.

| Opțiune | Implicit | Ce face |
|---|---|---|
| `options.bot-token` | `${DISCORD_BOT_TOKEN:}` | Tokenul botului, din Developer Portal. Ajunge în header-ul `Authorization`, nu se loghează niciodată. |
| `options.guild-id` | `${DISCORD_GUILD_ID:}` | Serverul ai cărui membri sunt candidați la legare. |
| `options.base-url` | `https://discord.com/api/v10` | Se schimbă doar pentru teste contra unui server fals. |
| `min-interval` | `100ms` în `application.yml` | Limitare de rată simplă, sub limita globală de rată a Discord API. |

### 2.8 Implicitele pe profil

| Profil | Email | Telegram | Discord | `async` |
|---|---|---|---|---|
| `dev` (`application-dev.yml`) | `${MESSAGING_EMAIL_PROVIDER:mock}` | `${MESSAGING_TELEGRAM_PROVIDER:mock}` | `${MESSAGING_DISCORD_PROVIDER:mock}` | `true` |
| `prod` (`application-prod.yml`) | `${MESSAGING_EMAIL_PROVIDER:smtp}` | `${MESSAGING_TELEGRAM_PROVIDER:mock}` | `${MESSAGING_DISCORD_PROVIDER:mock}` | `true` |
| teste (`src/test/resources/config/application.yml`) | `mock` | `mock` | `mock` | `false` |

Telegramul și Discordul rămân pe `mock` chiar și în `prod`: fiecare cere un bot și
adrese legate, deci se pornesc explicit, când sunt pregătite.

Un clone proaspăt, fără nicio variabilă de mediu, pornește și nu trimite nimic real.

Testele rulează pe `async: false` din două motive: răspunsul conține statusul
final (deci aserțiunile sunt simple) și nu se atinge interogarea de revendicare
cu `for update skip locked`, pe care H2 nu o poate parsa.

### 2.9 `async: false` pentru depanare

Cu `async: false`, `POST /messages/send` trimite pe loc și întoarce rezultatul
final (`SENT` / `PARTIAL` / `FAILED`), nu `QUEUED`. E comportamentul de dinaintea
cozii și e util când vrei să vezi eroarea unui provider imediat, în răspuns. Merge
prin exact aceleași clase ca dispecerul — doar declanșatorul diferă.

### 2.10 Timeout-uri de rețea

`application-dev.yml` și `application-prod.yml` setează
`mail.smtp.connectiontimeout`, `timeout` și `writetimeout`. **Nu le scoate.**
Fără ele JavaMail așteaptă la nesfârșit, iar un singur socket blocat oprește
dispecerul împreună cu restul task-urilor programate.

Aceeași grijă, în cod, la Telegram și Discord: `TelegramClient` și `DiscordClient`
își construiesc fabrica de cereri cu 10s la conectare și 20s la citire. Nu sunt
configurabile din yaml — sunt o măsură de protecție a dispecerului, nu un buton.

### 2.11 Ce e activ — linia de la pornire

```
ChannelSenderRegistry : Canale de trimitere: EMAIL -> smtp · TELEGRAM -> telegram-bot · DISCORD -> mock
```

Singura sursă de adevăr despre ce pleacă real și ce e simulat.

---

## 3. API REST

Ambele endpointuri cer autentificare (`/api/**` e `authenticated()` în
`config/SecurityConfiguration.java`). Sunt definite în `web/rest/AppResource.java`
și sunt scoped automat pe organizația utilizatorului autentificat.

### 3.1 Autentificare

```http
POST /api/authenticate
Content-Type: application/json

{ "username": "admin", "password": "admin", "rememberMe": false }
```

Răspuns: `{ "id_token": "eyJ..." }`. Token-ul se trimite apoi ca
`Authorization: Bearer <id_token>`.

### 3.2 `POST /api/app/messages/send`

Pune mesajul în coadă. Nu așteaptă trimiterea.

```http
POST /api/app/messages/send
Authorization: Bearer <id_token>
Content-Type: application/json

{
  "message": {
    "subject": "Ședință vineri",
    "bodyHtml": "<p>Bună, {{prenume}}! Ne vedem vineri la 10.</p>",
    "channels": ["EMAIL", "TELEGRAM"],
    "templateId": null,
    "attachments": [
      { "fileName": "agenda.pdf", "contentType": "application/pdf", "dataBase64": "JVBERi0..." }
    ]
  },
  "recipientIds": [12, 13, 14],
  "channelOverrides": { "14": ["EMAIL"] }
}
```

| Câmp | Obligatoriu | Note |
|---|---|---|
| `message.subject` | nu | Gol devine „Fara subiect". La Telegram/Discord devine prima linie a corpului. |
| `message.bodyHtml` | nu | HTML. Suportă variabilele `{{nume}}`, `{{prenume}}`, `{{grup}}`, `{{email}}`, substituite per destinatar. |
| `message.channels` | **da** | Cel puțin unul, altfel 400. |
| `message.templateId` | nu | Doar pentru legătura cu șablonul în istoric. |
| `message.attachments` | nu | `dataBase64` acceptă și forma `data:...;base64,...`. Limita de request e 10MB. |
| `recipientIds` | **da** | Cel puțin unul, altfel 400. |
| `channelOverrides` | nu | `id destinatar → subsetul de canale` pe care pleacă mesajul. Rezolvarea conflictelor de canal din pasul 3 al UI-ului. |

### 3.3 `GET /api/app/messages/{id}/status`

Starea trimiterii. Destinat poll-ului (ieftin: o singură interogare agregată).

```http
GET /api/app/messages/42/status
Authorization: Bearer <id_token>
```

### 3.4 Răspunsul — `SendResult`

Ambele endpointuri întorc aceeași structură:

```json
{
  "messageId": 42,
  "status": "QUEUED",
  "queued": 5,
  "delivered": 1,
  "failed": 0,
  "skipped": 2,
  "recipientCount": 3
}
```

| Câmp | Cât e `QUEUED` | După ce coada s-a scurs |
|---|---|---|
| `status` | `QUEUED` | `SENT`, `PARTIAL` sau `FAILED` |
| `queued` | livrări încă `PENDING` sau `SENDING` | `0` |
| `delivered` | livrări reușite **până acum** | total reușite |
| `failed` | eșecuri definitive până acum | total eșecuri |
| `skipped` | destinatari fără adresă pe canalul cerut (fix de la început) | același |
| `recipientCount` | destinatarii selectați | destinatari distincți **atinși** |

`skipped` nu e o eroare: e rezultatul selecției. Un destinatar fără chat id de
Telegram, inclus într-o trimitere pe Telegram, produce un rând `SKIPPED` care nu
contează nici ca reușită, nici ca eșec.

Statusul mesajului se calculează din livrări: fără eșecuri → `SENT`; fără reușite
→ `FAILED`; amestecat → `PARTIAL`.

### 3.5 Erori

| Cod | `message` | Când |
|---|---|---|
| 400 | `error.nochannel` | `channels` gol |
| 400 | `error.norecipient` | `recipientIds` gol |
| 404 | `error.notfound` | mesajul nu există sau e al altei organizații |
| 409 | `error.sendingdisabled` | `application.messaging.enabled: false` |

Corpul e `ProblemDetail` (RFC 7807), extins de JHipster:

```json
{
  "type": "https://www.jhipster.tech/problem/problem-with-message",
  "title": "Bad Request",
  "status": 400,
  "instance": "/api/app/messages/send",
  "detail": "Alege cel putin un canal.",
  "message": "error.nochannel",
  "params": "message",
  "path": "/api/app/messages/send"
}
```

> **De știut:** `message` e o **cheie**, nu text pentru om, iar `title` e rescris
> de `ExceptionTranslator` cu motivul standard HTTP („Bad Request"). Textul
> românesc ajunge la client prin **`detail`** — `EntityErrorException` îl pune
> acolo tocmai pentru că `detail` e singurul câmp pe care translatorul nu-l
> suprascrie. `core/toast.service.ts` citește `detail ?? message`, deci
> utilizatorul vede propoziția, nu cheia.

Validările de tip „câmp obligatoriu" din service layer aruncă
`IllegalArgumentException` și au **altă formă**, produsă de handler-ul din
`AppResource.java:181`: `{ "message": "<text românesc>" }`. Acolo textul *este*
afișabil.

### 3.6 Conectarea chat-urilor de Telegram

Două endpointuri, folosite de ecranul **Destinatari → Conectează Telegram**. Ambele
răspund **409 `error.telegramnotactive`** dacă providerul activ pe TELEGRAM nu e
`telegram-bot`.

**`GET /api/app/telegram/contacts`** — cine a scris botului:

```json
{
  "botUsername": "anunturi_mud_bot",
  "contacts": [
    {
      "chatId": "1001",
      "name": "Ion Popescu",
      "username": "ionp",
      "lastMessage": "/start",
      "lastAt": "2026-09-12T18:00:00Z",
      "recipientId": 1201,
      "recipientName": "Ion Popescu"
    }
  ]
}
```

`recipientId` e nenul când chat ID-ul e deja legat. Lista e dedublată pe `chatId`
(rămâne ultimul mesaj) și sortată descrescător după `lastAt`. `botUsername` vine
din `getMe`, pentru linkul `t.me/<bot>`.

**`POST /api/app/telegram/link`** — leagă un chat la un destinatar:

```json
{ "chatId": "1001", "recipientId": 1201 }
```

Răspunde **204**. Scrie sau actualizează `RecipientChannel(TELEGRAM, address,
active = true, verified = true)`. Erori: **409 `error.chatidexists`** dacă alt
destinatar din organizație are deja acel chat ID, **404 `error.notfound`** dacă
destinatarul nu există, **400 `error.invalidchatid`** dacă `chatId` lipsește,
**502 `error.telegramunavailable`** dacă Telegram nu răspunde.

### 3.7 Conectarea membrilor Discord

Două endpointuri, folosite de ecranul **Destinatari → Conectează Discord**. Ambele
răspund **409 `error.discordnotactive`** dacă providerul activ pe DISCORD nu e
`discord-bot`, sau dacă `DISCORD_GUILD_ID` e gol.

**`GET /api/app/discord/members`** — membrii serverului, fără boți:

```json
{
  "guildName": "Anunturi MUD",
  "inviteUrl": "https://discord.com/oauth2/authorize?client_id=1101&scope=bot&permissions=0",
  "members": [
    {
      "userId": "300100200300100200",
      "name": "Ion Popescu",
      "username": "ionp",
      "recipientId": 1201,
      "recipientName": "Ion Popescu"
    }
  ]
}
```

`recipientId` e nenul când user ID-ul e deja legat. `name` preferă porecla de pe
server (`nick`), apoi `global_name`, apoi `username`. `inviteUrl` e construit din
id-ul botului (`GET /users/@me`), pentru cine încă nu e pe server. Cere **Server
Members Intent** bifat în Developer Portal, altfel lista vine goală.

**`POST /api/app/discord/link`** — leagă un membru la un destinatar:

```json
{ "userId": "300100200300100200", "recipientId": 1201 }
```

Răspunde **204**. Scrie sau actualizează `RecipientChannel(DISCORD, address,
active = true, verified = true)`. Erori: **409 `error.discordidexists`** dacă alt
destinatar din organizație are deja acel user ID, **404 `error.notfound`** dacă
destinatarul nu există, **400 `error.invaliddiscordid`** dacă `userId` nu e un
snowflake valid (17-20 cifre), **502 `error.discordunavailable`** dacă Discord nu
răspunde.

### 3.8 Exemplu complet

```bash
BASE=http://localhost:8080

TOKEN=$(curl -s -X POST "$BASE/api/authenticate" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .id_token)

MSG=$(curl -s -X POST "$BASE/api/app/messages/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "message": { "subject": "Test", "bodyHtml": "<p>Salut, {{prenume}}!</p>",
                     "channels": ["EMAIL"], "templateId": null, "attachments": [] },
        "recipientIds": [1, 2],
        "channelOverrides": {}
      }')
echo "$MSG"        # => {"messageId":42,"status":"QUEUED","queued":2,...}

ID=$(echo "$MSG" | jq -r .messageId)

# poll până nu mai e QUEUED
while [ "$(curl -s -H "Authorization: Bearer $TOKEN" \
           "$BASE/api/app/messages/$ID/status" | jq -r .status)" = "QUEUED" ]; do
  sleep 2
done
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/app/messages/$ID/status"
```

---

## 4. Arhitectura și ciclul de viață

### 4.1 Stările unei livrări

`domain/enumeration/DeliveryStatus.java`:

```
        (enqueue, are adresă)              (enqueue, fără adresă pe canal)
                 │                                      │
                 ▼                                      ▼
              PENDING ◄──────────────┐               SKIPPED  (terminal)
                 │  revendicare      │ RETRY, mai are încercări
                 ▼                   │ (next_attempt_at = now + backoff)
              SENDING ───────────────┘
                 │
      OK ───────►│◄─────── eroare permanentă  |  încercări epuizate
      │                                   │
      ▼                                   ▼
    SENT                               FAILED
      │
      │ (doar viitoarele webhook-uri; nimic nu-l scrie acum)
      ▼
  DELIVERED
```

- **`SENT` e succesul terminal** cât timp nu avem webhook-uri de confirmare.
- **`DELIVERED`** e rezervat confirmării de la provider. Nimic nu-l scrie acum,
  **dar istoricul de dinaintea cozii e plin de el** — de aceea orice citire
  tratează `SENT` *sau* `DELIVERED` ca succes (`DeliveryStatus.isSuccess()`).
  Regula nu e defensivă, e portantă: fără ea, tot istoricul vechi ar apărea eșuat.
- **`SENDING`** folosește `next_attempt_at` ca termen de vizibilitate. Dacă
  procesul moare la mijloc, rândul redevine vizibil după `visibility-timeout` și
  se reia singur — fără job de curățare.

### 4.2 Clasele

`service/messaging/` — contractul și providerii:

| Clasă | Rol |
|---|---|
| `ChannelSender` | Interfața unui provider: `channel()`, `providerId()`, `send()`, `validateConfiguration()`. |
| `ChannelSenderRegistry` | Alege la pornire un provider per canal, după configurație. Validează și logează ce e activ. |
| `MessagingConfiguration` | Declară bean-urile mock, câte unul per canal. |
| `MockChannelSender` | Providerul `mock`: logează și raportează reușit. Opțional eșuează cu `mock-failure-rate`. |
| `EmailChannelSender` | Providerul `smtp`. Doar SMTP real, fără niciun fallback. |
| `EmailFailureClassifier` | Decide dacă un eșec SMTP e trecător sau definitiv. |
| `Delivery`, `SendOutcome` | Sarcina randată și rezultatul ei. `SendOutcome.retryAfter` e pauza cerută de provider. |
| `VariableRenderer` | Substituie `{{nume}}`, `{{prenume}}`, `{{grup}}`, `{{email}}`. |

`service/messaging/telegram/` — providerul `telegram-bot`:

| Clasă | Rol |
|---|---|
| `TelegramChannelSender` | Providerul. Subiectul devine prima linie, textul se taie în mesaje, atașamentele pleacă separat. |
| `TelegramClient` | Clientul HTTP (`sendMessage`, `sendDocument`, `getMe`, `getUpdates`). Timeout-uri proprii, tokenul nu se loghează. |
| `TelegramHtmlFormatter` | Reduce HTML-ul editorului la subsetul Telegram (cu jsoup) și îl împarte la 4096. |
| `TelegramFailureClassifier` | Traduce statusul HTTP + `description` în `retry` / `permanent`, cu `retry_after`. |
| `TelegramResponse`, `TelegramUpdates`, `TelegramApiException` | Forma răspunsurilor Bot API și refuzul lor. |

`service/app/TelegramLinkService` — contactele botului și legarea lor la
destinatari (ecranul de conectare). Nu ia parte la trimitere.

`service/messaging/discord/` — providerul `discord-bot`:

| Clasă | Rol |
|---|---|
| `DiscordChannelSender` | Providerul. Subiectul devine prima linie (îngroșată), textul se taie în mesaje, atașamentele pleacă separat, în loturi de 10. |
| `DiscordClient` | Clientul HTTP (`openDm`, `sendMessage`, `sendFiles`, `botUser`, `guildName`, `members`). Verbe HTTP reale, nu un singur plic ca la Telegram — reușita se vede din codul HTTP. |
| `DiscordMarkdownFormatter` | Reduce HTML-ul editorului la Markdown-ul Discord (cu jsoup) și îl împarte la 2000. |
| `DiscordFailureClassifier` | Traduce statusul HTTP + codul numeric Discord în `retry` / `permanent`, cu `retry_after`. |
| `DiscordResponses`, `DiscordApiException` | Forma răspunsurilor Discord API v10 și refuzul lor. |

`service/app/DiscordLinkService` — membrii serverului și legarea lor la
destinatari (ecranul de conectare). Nu ia parte la trimitere.

`service/messaging/dispatch/` — coada:

| Clasă | Rol |
|---|---|
| `DeliveryDispatcher` | `@Scheduled`: la fiecare tick, revendică și trimite. Există doar când `async: true`. |
| `DeliveryClaimService` | Revendică un lot și îl trece în `SENDING`. Tranzacție foarte scurtă. |
| `DeliveryExecutor` | Încarcă sarcina și scrie rezultatul. Două tranzacții separate. |
| `DeliveryRunner` | Bucla trimite-și-înregistrează. **Fără** tranzacție. |
| `MessageStatusRecalculator` | Recalculează statusul mesajului din livrările lui. |
| `InlineDeliveryRunner` | Calea sincronă: scurge coada unui mesaj pe loc. |
| `RetryPolicy` | Întârzierea dintre încercări și când se renunță. Clasă pură. |
| `DeliveryJob` | Sarcina detașată, complet materializată. |

`service/app/SendCoordinator` — punctul de intrare, și locul unde se decide
asincron vs. inline.

### 4.3 Granițele tranzacțiilor

```
DeliveryRunner.run()                       ◄── fără @Transactional
  │
  ├── DeliveryClaimService.claim...()        @Transactional(REQUIRES_NEW)  ~1ms
  │
  ├── DeliveryExecutor.load()               @Transactional(readOnly)      ~1ms
  │
  ├── ChannelSender.send()                  ◄── FĂRĂ TRANZACȚIE. I/O de rețea.
  │
  ├── DeliveryExecutor.complete()           @Transactional(REQUIRES_NEW)  ~1ms
  │
  └── MessageStatusRecalculator.refresh()   @Transactional(REQUIRES_NEW)
```

**Nicio conexiune JDBC nu e ținută peste apelul SMTP.** E proprietatea cea mai
importantă a întregii structuri: varianta veche făcea I/O de rețea în buclă
înăuntrul unei tranzacții, ținând o conexiune din pool ocupată pentru toată
trimiterea.

Despărțirea în bean-uri separate nu e de stil, e funcțională: `@Transactional` pe
o metodă a aceluiași bean **nu e interceptat** de proxy. Pentru același motiv,
`SendCoordinator` e o clasă aparte: `AppService` e `@Transactional` la nivel de
clasă, deci nu poate el să declanșeze trimiterea inline fără să readucă SMTP-ul
în tranzacție.

### 4.4 Retry și backoff

`EmailFailureClassifier` citește codul SMTP din textul erorii:

| Situație | Verdict |
|---|---|
| Autentificare eșuată (parolă greșită/revocată) | **definitiv** |
| Cod SMTP **5xx** (cutie inexistentă, mesaj respins) | **definitiv** |
| Adresă malformată sau respinsă explicit | **definitiv** |
| Cod SMTP **4xx** (greylisting, limită de rată) | reîncercabil |
| Conexiune refuzată, DNS, timeout, TLS | reîncercabil |
| Orice altceva | reîncercabil |

Codul se citește din mesaj, nu din `SMTPSendFailedException`: implementarea Angus
e dependință de **runtime** în Spring Boot 4, deci clasele ei nu sunt pe
classpath-ul de compilare. Textul începe însă mereu cu codul (`550-5.1.1 ...`).

`TelegramFailureClassifier` face același lucru pentru Bot API:

| Situație | Verdict |
|---|---|
| 401 token greșit/revocat | **definitiv** |
| 403 „bot was blocked", „user is deactivated", „bot can't initiate conversation" | **definitiv** |
| 400 „chat not found", „chat_id is empty" | **definitiv** |
| 400 „can't parse entities" | o reîncercare imediată în text curat; dacă nici aceea nu trece, **definitiv** |
| 413 / „file is too big" | **definitiv** |
| 429 | reîncercabil, **la momentul cerut în `retry_after`** |
| 5xx, conexiune refuzată, timeout | reîncercabil |
| Orice altceva | reîncercabil |

`DiscordFailureClassifier` face același lucru pentru Discord API, dar pe codul
numeric `code` din corp, nu pe text de căutat cu `contains` — Discord îl ține stabil:

| Situație | Verdict |
|---|---|
| 401 token greșit/revocat | **definitiv** |
| Cod `50007` (DM refuzate sau niciun server comun) | **definitiv** |
| Cod `10013` (Unknown User) / `10003` (Unknown Channel) | **definitiv** |
| Cod `50035` (Invalid Form Body) | **definitiv** |
| Cod `40005` / HTTP 413 (fișier prea mare) | **definitiv** |
| Alte 400 / 403 / 404 | **definitiv** |
| 429 | reîncercabil, **la momentul cerut în `retry_after`** (rotunjit în sus) |
| 5xx, conexiune refuzată, timeout | reîncercabil |
| Orice altceva | reîncercabil |

Un `retry_after` mai lung decât backoff-ul nostru îl înlocuiește
(`RetryPolicy.delayFor(attempt, retryAfter)`). Motivul: o pauză de flood poate cere
o oră, iar reîncercările din 30 în 30 de secunde ar consuma toate încercările pe
refuzuri. Mai scurt nu luăm — providerul nu știe cât ne permitem să așteptăm.

Întârzierea: `min(initial × multiplier^(încercare-1), max)`, plus jitter ±20%. Cu
implicitele: **~30s → ~90s → ~4.5min → ~13.5min → `FAILED`** (5 încercări, o
fereastră de ~19 minute). Jitter-ul contează pentru că atunci când pică rețeaua
tot lotul eșuează în aceeași secundă — fără el, tot lotul ar reîncerca simultan.

O eroare definitivă devine `FAILED` **din prima încercare**: nu are rost să
încerci o parolă greșită de cinci ori.

### 4.5 Ce ține coada în baza de date

Trei coloane noi pe `message_recipient`
(`config/liquibase/changelog/messaging/20260912100000_added_delivery_queue_to_MessageRecipient.xml`):

| Coloană | Rol |
|---|---|
| `attempt_count` | De câte ori livrarea a fost dată unui provider. |
| `next_attempt_at` | Când redevine disponibilă; cât e `SENDING`, e termenul de vizibilitate. |
| `last_attempt_at` | Diagnostic. |

**Nu există coloană de payload.** Subiectul și corpul se re-randează la trimitere
din `Message` + `Recipient` (substituția e deterministă, deci o reîncercare dă
același rezultat), iar atașamentele se citesc din rândurile `MessageAttachment`
deja persistate. Consecința acceptată: dacă un destinatar e redenumit între
punerea în coadă și trimitere, `{{nume}}` reflectă valoarea **de la trimitere**.

`MessageRecipient` **nu e cache-uit** în cache-ul de nivel 2 — tabelul e o coadă
scrisă cu `UPDATE`-uri în masă, care ocolesc cache-ul. Dacă reintroduci `@Cache`
pe entitate, trebuie adăugată și linia din `config/CacheConfiguration.java`,
altfel Hibernate pică la pornire cu „cache not found".

### 4.6 Limite asumate

Scrise explicit, ca să nu fie luate drept defecte:

- **Livrare „cel puțin o dată".** Dacă predarea către provider reușește dar
  înregistrarea rezultatului nu apucă să facă commit (proces oprit), livrarea se
  reia după `visibility-timeout` și mesajul pleacă a doua oară. E inerent oricărei
  cozi fără chei de idempotență la provider — nici SMTP, nici Bot API nu oferă
  așa ceva. Pe Telegram efectul e vizibil și într-un caz mai mic: un mesaj împărțit
  în mai multe bucăți, care eșuează la a doua, retrimite la reîncercare **toate**
  bucățile.
- **O singură instanță.** Revendicarea folosește `for update skip locked`, deci
  două instanțe nu ar lua același rând, dar restul nu a fost verificat pe mai
  multe noduri.
- **`for update skip locked` e doar PostgreSQL.** H2 nu îl parsează; de aceea
  calea inline folosește o interogare JPQL simplă și testele merg pe `async: false`.
- **Fără webhook-uri.** `SENT` înseamnă „providerul a acceptat mesajul", nu
  „destinatarul l-a primit".

---

## 5. Cum adaugi un canal real

Un provider nou e **o clasă nouă plus două linii de yaml**. Nu se modifică
`AppService`, `SendCoordinator`, dispecerul sau vreun DTO. Mock-ul existent rămâne
la locul lui — nu se înlocuiește, doar nu mai e selectat.

Modelul de urmat e `service/messaging/telegram/` — providerul `telegram-bot`, scris
exact pe tiparul de mai jos. Toate canalele din `Channel` au deja un client real;
tiparul rămâne valabil pentru un canal nou (adaugi și valoarea în enum).

### Pasul 1 — clasa

```java
@Service
public class NewChannelSender implements ChannelSender {

    private final NewChannelClient client;   // clientul HTTP, separat de sender

    @Override public Channel channel()   { return Channel.NEW_CHANNEL; }
    @Override public String providerId() { return "new-channel-api"; }

    @Override
    public void validateConfiguration() {
        // doar verificări offline: token prezent, numar configurat.
        // Fara apel de retea - o pana la provider nu trebuie sa blocheze pornirea.
    }

    @Override
    public SendOutcome send(Delivery delivery) {
        try {
            return SendOutcome.ok(String.valueOf(client.sendText(delivery.address(), text)));
        } catch (Exception e) {
            return NewChannelFailureClassifier.classify(e);   // retry / permanent
        }
    }
}
```

### Pasul 2 — configurarea

```yaml
application:
  messaging:
    channels:
      new-channel:
        provider: ${MESSAGING_NEW_CHANNEL_PROVIDER:mock}
        options:
          access-token: ${NEW_CHANNEL_TOKEN:}
```

Harta `options` e motivul pentru care acest pas **nu** cere un câmp nou în
`ApplicationProperties`.

### Pasul 3 — restart

Logul confirmă: `Canale de trimitere: ... · NEW_CHANNEL -> new-channel-api`.
Dacă `providerId()` nu corespunde cu `provider` din yaml, aplicația refuză să
pornească și listează ce e disponibil.

### Contractul pe care trebuie să-l respecți

1. **Clasifică erorile onest.** `retry` vs. `permanent` e ce decide dacă un mesaj
   mai are o șansă sau intră roșu în istoric. Un 429 e `retry`; „chat not found"
   e `permanent`.
2. **Întoarce un id real** în `SendOutcome.ok(...)` — ajunge în
   `provider_message_id` și e singura urmă a trimiterii.
3. **Nu arunca** dacă poți întoarce un `SendOutcome`. (Dacă totuși arunci,
   `DeliveryRunner` prinde și tratează ca `retry`, ca să nu cadă tot lotul.)
4. **Pune timeout-uri pe clientul HTTP.** Dispecerul rulează pe planificatorul de
   task-uri; un socket blocat îl oprește cu totul. Vezi `TelegramClient`.
5. **Nu loga tokenul.** La Telegram el stă chiar în calea URL-ului.

### Cum arată la Telegram — de citit înainte de orice canal de chat

`TelegramChannelSender` rezolvă deja trei probleme pe care orice canal de chat le
are, și merită copiate:

- **HTML-ul editorului nu e acceptat.** Telegram cunoaște doar `b i u s a code pre
  blockquote` și respinge tot mesajul cu 400 dacă apare un `<div>`.
  `TelegramHtmlFormatter` parcurge DOM-ul cu jsoup și traduce și stilurile CSS
  (`font-weight: 700` → `<b>`), fiindcă editorul scrie formatarea ca `style`.
  Plasa de siguranță: la 400 „can't parse entities" mesajul se retrimite o dată în
  text curat, cu un `WARN` care numește tag-ul vinovat.
- **Limită de lungime.** 4096 caractere per mesaj, deci textul se taie pe limite de
  paragraf. Invariantul care face tăierea sigură: fiecare linie produsă are
  tag-urile echilibrate în ea însăși.
- **Fișierele pleacă separat.** `sendMessage` nu poate purta atașamente; fiecare
  fișier se trimite cu `sendDocument` (multipart), după text.

Ce **nu** pleacă pe Telegram: imaginile inline din corp. `<img>` e aruncat de
formatter — Bot API nu are imagini în interiorul unui mesaj text.

### Ce cere un canal de chat nou

**Fără subiect.** Canalele de chat nu au subiect (regula 8 din `app.jdl`). Convenția
pe care mock-ul o stabilește deja și pe care `telegram-bot` o respectă: subiectul
devine prima linie a corpului. Păstrează-o.

**Consimțământ.** Un bot nu poate scrie primul: omul trebuie să-i scrie, iar adresa
se leagă abia după. Ecranul **Conectează Telegram** e modelul pentru asta.

---

## 6. Operare și depanare

### Ce e activ acum

Linia de la pornire e singura sursă de adevăr:

```
ChannelSenderRegistry : Canale de trimitere: EMAIL -> smtp · TELEGRAM -> telegram-bot · DISCORD -> discord-bot
```

### Ce s-a întâmplat cu o trimitere

```sql
select channel, status, attempt_count, next_attempt_at, error_message, provider_message_id
  from message_recipient
 where message_id = 42
 order by channel, id;
```

### Simptome și cauze

| Simptom | Cauză probabilă |
|---|---|
| Totul rămâne `QUEUED`, `attempt_count` = 0 | `enabled: false`, sau `async: true` fără scheduling activ, sau dispecerul blocat pe un socket SMTP fără timeout. |
| `FAILED` cu `attempt_count` = 1 | Eroare definitivă. Citește `error_message`: de obicei parolă de aplicație greșită sau adresă respinsă. |
| `attempt_count` crește, `next_attempt_at` se distanțează | Pană de transport (host/port greșit, rețea). Se reia singur dacă revine. |
| Rânduri `SKIPPED` neașteptate | Destinatarii nu au adresă activă pe canalul respectiv. Verifică `recipient_channel`. |
| Istoricul vechi apare eșuat | Cineva a scos tratarea lui `DELIVERED` ca succes. Vezi §4.1. |
| Aplicația nu pornește, eroare de configurare | Cheie `application.*` fără câmp în `ApplicationProperties`, sau `provider` inexistent. Mesajul spune care. |
| **Telegram:** toată lumea e `FAILED` cu „chat not found" | Chat ID-urile nu sunt legate, sau sunt scrise de mână și greșite. Destinatari → Conectează Telegram. |
| **Telegram:** `FAILED` cu „bot was blocked by the user" | Omul a blocat botul. Nu se rezolvă din aplicație. |
| **Telegram:** un `WARN` cu „a respins formatarea" | Editorul produce un tag nou, pe care formatter-ul nu-l traduce. Mesajul a plecat în text curat; tag-ul e numit în log, de adăugat în `TelegramHtmlFormatter`. |
| **Telegram:** `PENDING` cu „Limita de rată" și `next_attempt_at` departe | 429 cu `retry_after`. Se reia singur. Dacă se repetă, mărește `channels.telegram.min-interval`. |
| **Telegram:** „Telegramul real nu e pornit" în ecranul de conectare | `MESSAGING_TELEGRAM_PROVIDER` nu e `telegram-bot`. |
| **Telegram:** lista de contacte e goală deși omul a scris | Au trecut peste ~24h de la mesajul lui. Trebuie să scrie din nou. |
| **Discord:** `FAILED` cu codul `50007` | Destinatarul nu e pe același server cu botul, sau a dezactivat DM-urile de la membrii serverului. Nu se rezolvă din aplicație. |
| **Discord:** lista de membri e goală | **Server Members Intent** nu e bifat în Developer Portal, sau botul nu e (încă) invitat pe server. |
| **Discord:** „Discordul real nu e pornit" în ecranul de conectare | `MESSAGING_DISCORD_PROVIDER` nu e `discord-bot`, sau lipsește `DISCORD_GUILD_ID`. |
| **Discord:** `PENDING` cu „Limita de rată" și `next_attempt_at` departe | 429 cu `retry_after`. Se reia singur. Dacă se repetă, mărește `channels.discord.min-interval`. |

### Exersarea retry-ului fără rețea

```yaml
application:
  messaging:
    channels:
      telegram:
        provider: mock
        mock-failure-rate: 0.5
```

Jumătate din livrări se întorc `retry`, deci poți urmări backoff-ul și
`attempt_count` fără SMTP și fără internet.

### Telegram fără bot: `base-url` spre un server propriu

`options.base-url` există exact pentru asta. Pornești un server local care răspunde
la `/bot<token>/getMe`, `/sendMessage`, `/sendDocument`, `/getUpdates` și poate
întoarce erori la comandă, apoi:

```bash
MESSAGING_TELEGRAM_PROVIDER=telegram-bot \
TELEGRAM_BOT_TOKEN=orice \
TELEGRAM_BASE_URL=http://localhost:8099 \
./mvnw
```

Așa se pot exersa, fără token de la @BotFather, chiar și cazurile greu de provocat
pe viu: 429 cu `retry_after`, „chat not found", „can't parse entities" sau un corp
peste 4096 de caractere.

### Oprirea trimiterilor pentru mentenanță

```yaml
application:
  messaging:
    enabled: false
```

Trimiterile noi sunt refuzate cu 409; cele deja în coadă rămân și pleacă la
repornire.
