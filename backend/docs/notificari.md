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
                    ChannelSender (smtp / mock)
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
| `TELEGRAM` | `mock` | doar simulat — clientul real nu există încă |
| `WHATSAPP` | `mock` | doar simulat — clientul real nu există încă |

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

### 2.2 Per canal — `channels.email.*`, `channels.telegram.*`, `channels.whatsapp.*`

| Cheie | Implicit | Ce face |
|---|---|---|
| `provider` | `mock` | Id-ul providerului: `mock`, `smtp`. Trebuie să existe un `ChannelSender` cu acest `providerId()`, altfel aplicația nu pornește. |
| `batch-size` | `25` | Câte livrări ia dispecerul pe acest canal, la fiecare tick. |
| `min-interval` | `0` (dar `200ms` pentru email în `application.yml`) | Pauză între două trimiteri consecutive pe canal. Limitare de rată simplă. |
| `mock-failure-rate` | `0` | Doar pentru `provider: mock`: fracțiunea de trimiteri care eșuează, ca să poți exersa retry-ul. |
| `options.*` | — | Hartă liberă de setări specifice providerului (token, URL). Un client nou nu cere câmpuri noi în `ApplicationProperties`. |

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
ChannelSenderRegistry : Canale de trimitere: EMAIL -> smtp · TELEGRAM -> mock · WHATSAPP -> mock
```

**Dacă pui `provider: smtp` fără `MAIL_USERNAME`, aplicația refuză să pornească**,
cu mesajul care spune exact ce lipsește. E deliberat. Varianta veche cădea tacit
pe mock când lipseau credențialele, iar rezultatul era că **nu puteai spune,
uitându-te la un rând verde „Livrat" din istoric, dacă emailul a plecat într-adevăr**.
`provider: mock` este fallback-ul — doar că acum e o alegere scrisă, nu un accident.

### 2.6 Implicitele pe profil

| Profil | Email | `async` |
|---|---|---|
| `dev` (`application-dev.yml`) | `${MESSAGING_EMAIL_PROVIDER:mock}` | `true` |
| `prod` (`application-prod.yml`) | `${MESSAGING_EMAIL_PROVIDER:smtp}` | `true` |
| teste (`src/test/resources/config/application.yml`) | `mock` | `false` |

Un clone proaspăt, fără nicio variabilă de mediu, pornește și nu trimite nimic real.

Testele rulează pe `async: false` din două motive: răspunsul conține statusul
final (deci aserțiunile sunt simple) și nu se atinge interogarea de revendicare
cu `for update skip locked`, pe care H2 nu o poate parsa.

### 2.7 `async: false` pentru depanare

Cu `async: false`, `POST /messages/send` trimite pe loc și întoarce rezultatul
final (`SENT` / `PARTIAL` / `FAILED`), nu `QUEUED`. E comportamentul de dinaintea
cozii și e util când vrei să vezi eroarea unui provider imediat, în răspuns. Merge
prin exact aceleași clase ca dispecerul — doar declanșatorul diferă.

### 2.8 Timeout-uri SMTP

`application-dev.yml` și `application-prod.yml` setează
`mail.smtp.connectiontimeout`, `timeout` și `writetimeout`. **Nu le scoate.**
Fără ele JavaMail așteaptă la nesfârșit, iar un singur socket blocat oprește
dispecerul împreună cu restul task-urilor programate.

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
| `message.subject` | nu | Gol devine „Fara subiect". La Telegram/WhatsApp devine prima linie a corpului. |
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
  "message": "error.nochannel",
  "params": "message",
  "path": "/api/app/messages/send"
}
```

> **De știut:** `message` e o **cheie de traducere**, nu text pentru om. Textul
> românesc din `errors/MessageException.java` nu ajunge în răspuns. Iar
> `core/toast.service.ts` afișează `message` ca atare, deci utilizatorul vede
> literal „error.sendingdisabled". Comportamentul e vechi (la fel se întâmplă
> deja cu `error.nochannel` și `error.norecipient`) și nu l-am schimbat, dar se
> vede mai des acum că există un comutator de oprire. Rezolvarea e o hartă
> cheie → text în frontend, sau trecerea textului în `detail`.

Validările de tip „câmp obligatoriu" din service layer aruncă
`IllegalArgumentException` și au **altă formă**, produsă de handler-ul din
`AppResource.java:181`: `{ "message": "<text românesc>" }`. Acolo textul *este*
afișabil.

### 3.6 Exemplu complet

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
| `Delivery`, `SendOutcome` | Sarcina randată și rezultatul ei. |
| `VariableRenderer` | Substituie `{{nume}}`, `{{prenume}}`, `{{grup}}`, `{{email}}`. |

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
  cozi fără chei de idempotență la provider — SMTP nu oferă așa ceva.
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

### Pasul 1 — clasa

`service/messaging/telegram/TelegramChannelSender.java`:

```java
@Service
public class TelegramChannelSender implements ChannelSender {

    private final RestClient http;          // deja în spring-boot-starter-web
    private final String token;

    public TelegramChannelSender(RestClient.Builder builder, ApplicationProperties properties) {
        this.http = builder.baseUrl("https://api.telegram.org").build();
        this.token = properties.getMessaging().settingsFor(Channel.TELEGRAM).getOptions().get("bot-token");
    }

    @Override public Channel channel()   { return Channel.TELEGRAM; }
    @Override public String providerId() { return "telegram-bot"; }

    @Override
    public void validateConfiguration() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Lipseste application.messaging.channels.telegram.options.bot-token");
        }
    }

    @Override
    public SendOutcome send(Delivery delivery) {
        // 429 -> SendOutcome.retry(...)   (Telegram trimite si retry_after)
        // 4xx -> SendOutcome.permanent(...) ("chat not found")
        // 5xx / IO -> SendOutcome.retry(...)
        // la reusita: SendOutcome.ok(<id-ul real de la provider>)
    }
}
```

### Pasul 2 — configurarea

```yaml
application:
  messaging:
    channels:
      telegram:
        provider: telegram-bot
        options:
          bot-token: ${TELEGRAM_BOT_TOKEN:}
```

Harta `options` e motivul pentru care acest pas **nu** cere un câmp nou în
`ApplicationProperties`.

### Pasul 3 — restart

Logul confirmă: `Canale de trimitere: ... · TELEGRAM -> telegram-bot · ...`.
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

### Capcanele cunoscute

**Telegram.** Acceptă doar un subset mic de HTML — `b`, `i`, `u`, `s`, `a`,
`code`, `pre`, `blockquote`. HTML-ul venit din editorul bogat al aplicației
**trebuie redus**, nu trimis ca atare. Limita e 4096 caractere per mesaj.

**WhatsApp (Cloud API).** Text liber e permis **doar în fereastra de 24h** de la
ultimul mesaj al utilizatorului. În afara ei se pot trimite doar șabloane aprobate
de Meta, cu parametri. Asta e o constrângere de **produs** asupra ecranului de
compunere, nu un detaliu de implementare al clientului — de discutat înainte de
a scrie codul.

**Ambele.** Nu au subiect (regula 8 din `app.jdl`). Convenția pe care mock-ul o
stabilește deja: subiectul devine prima linie a corpului. Păstrează-o.

---

## 6. Operare și depanare

### Ce e activ acum

Linia de la pornire e singura sursă de adevăr:

```
ChannelSenderRegistry : Canale de trimitere: EMAIL -> smtp · TELEGRAM -> mock · WHATSAPP -> mock
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

### Oprirea trimiterilor pentru mentenanță

```yaml
application:
  messaging:
    enabled: false
```

Trimiterile noi sunt refuzate cu 409; cele deja în coadă rămân și pleacă la
repornire.
