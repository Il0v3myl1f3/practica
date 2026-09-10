# Backend — notificariMud

API REST generat cu JHipster 9.1.0: Spring Boot 4.0.6, Java 17, Maven, JWT,
Hibernate + Liquibase, MapStruct.

Fără frontend — clientul Angular este un proiect separat, în `../frontend`.

## Rulare

```bash
docker compose -f src/main/docker/postgresql.yml up -d
./mvnw
```

Pornește pe `http://localhost:8080`. Conturi: `admin` / `admin` și `user` / `user`.

Baza de date e PostgreSQL 18.4 în Docker, pe ambele profiluri. Containerul
trebuie pornit înainte de aplicație — altfel pornirea eșuează la conectare.

Datele stau în volumul Docker `notificari-mud-db`. `down` oprește containerul și
păstrează datele; doar `down -v` le șterge. Atenție dacă umbli la montare:
Postgres 18 ține datele în `/var/lib/postgresql/18/docker`, nu în
`/var/lib/postgresql/data` ca versiunile anterioare — de asta montăm directorul
părinte, care e și volumul declarat de imagine.

Pe o bază goală, `DevDataSeeder` încarcă datele din prototip la prima pornire.

## API

- `POST /api/authenticate` — login, întoarce JWT
- `/api/organizations`, `/api/recipients`, `/api/recipient-groups`,
  `/api/recipient-channels`, `/api/message-templates`, `/api/messages`,
  `/api/message-channels`, `/api/message-recipients`, `/api/message-attachments`
- `/v3/api-docs` — OpenAPI; `/management/health` — health check

`Recipient` și `Message` au filtrare pe criterii (`?email.contains=`,
`?status.equals=SENT`) prin query service-urile generate.

## Modelul de date

`app.jdl` este sursa de adevăr. După ce îl modifici:

```bash
npx jhipster jdl app.jdl
```

Regenerarea suprascrie entitățile, DTO-urile și mapper-ele, dar **nu** șterge
codul scris de tine în alte clase. Fișierele din `.jhipster/` țin starea
entităților — nu le șterge.

La finalul `app.jdl` sunt listate regulile de business care nu se pot exprima în
JDL și trebuie scrise manual în service layer.

## Ce s-a scos față de generarea implicită

Proiect de practică, fără deploy real: am eliminat Prometheus, Grafana, JHipster
Control Center, Sonar, JaCoCo, checkstyle/nohttp, jib (build de imagini Docker),
`.devcontainer/` și tot stratul npm (husky, prettier, lint-staged).

Testele generate au fost șterse, dar **infrastructura de test a rămas**:
adnotarea `@IntegrationTest`, testcontainers și `src/test/resources/`. Poți scrie
teste noi fără nicio reconfigurare.
