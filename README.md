# Practică — Aplicație de trimitere mesaje

Aplicație internă pentru trimiterea de anunțuri către angajați pe **email, Telegram
și WhatsApp**, dintr-un singur loc: șabloane reutilizabile, selecție manuală a
destinatarilor și istoric complet al trimiterilor.

Monorepo cu două proiecte independente:

| Folder | Ce e | Stare |
|---|---|---|
| [`backend/`](backend) | API REST — Spring Boot 4 + JHipster 9, PostgreSQL, JWT | funcțional |
| [`frontend/`](frontend) | Client Angular | de început |
| [`frontend/design/`](frontend/design) | Prototipul de UI și token-urile de design MUD | referință |

## Pornire rapidă

```bash
cd backend
./mvnw            # http://localhost:8080, cont admin/admin
```

Baza de date de dezvoltare este H2 pe disc — nu trebuie instalat nimic.
Pentru PostgreSQL: `docker compose -f backend/src/main/docker/postgresql.yml up -d`
și pornește cu profilul `prod`.

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

## De făcut

- [ ] CORS pentru Angular — `backend/src/main/resources/config/application-dev.yml`
      permite acum doar portul 8100
- [ ] Unicitate per organizație: email destinatar, nume grup, nume șablon
- [ ] Logica de business din finalul `app.jdl`: parser CSV, conflict de canale,
      substituția variabilelor, agregarea statusului de livrare
- [ ] Integrarea efectivă cu providerii de email / Telegram / WhatsApp
- [ ] Scheletul Angular în `frontend/`
