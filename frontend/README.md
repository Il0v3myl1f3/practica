# Frontend

Clientul Angular al aplicației. **Încă nu este generat.**

Backend-ul expune deja API-ul complet la `http://localhost:8080` — vezi
`/v3/api-docs` pentru contractul OpenAPI, din care se poate genera clientul
TypeScript.

Autentificarea este pe JWT: `POST /api/authenticate` cu `{username, password}`
întoarce un token care se trimite mai departe ca `Authorization: Bearer <token>`.

CORS este deja configurat pentru `http://localhost:4200` pe profilul `dev`
(`backend/src/main/resources/config/application-dev.yml`). Dacă pornești Angular
pe alt port, actualizează `jhipster.cors.allowed-origins` acolo.

## design/

Prototipul de UI și specificațiile de design:

- `Trimitere Mesaje.dc.html` — prototipul complet, se deschide direct în browser.
  Conține toate ecranele și toată logica de interacțiune.
- `MUD-DESIGN-TOKENS.md`, `MUD-TABLE-FILTERS-TOKENS.md` — token-urile sistemului
  de design MUD: culori, spațiere, tipografie, componente de tabel și filtre.
