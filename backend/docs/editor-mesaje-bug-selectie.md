# Bug: selecția se pierde parțial la formatare pe conținut mixt (text + chip)

Raport din testare manuală cu Playwright MCP pe `/mesaj/compune`
(`frontend/src/app/shared/editor.component.ts`), 2026-09-16.

**Status:** bug-urile 1-3 sunt rezolvate și fix-ul e activ în cod.
**Actualizare (2026-09-17):** fix-ul pentru bug-ul 4 (Firefox — navigare cu
săgeata lângă chip-uri) **e aplicat și activ în cod acum**, la cererea
utilizatorului — vezi finalul secțiunii 4 pentru ce anume s-a adus din
`git stash` și ce s-a schimbat față de varianta stash-uită. Bug-ul 7
(`Ctrl+B/I/U` nu fac nimic/ocolesc `exec()`) **tot NU e rezolvat** — deși
`onKeyDown()` există din nou în cod (readus pentru bug-ul 4), varianta adusă
înapoi tratează *doar* `ArrowLeft`/`ArrowRight`; interceptarea
`Ctrl+B/I/U` → `exec()` din bug-ul 7 (alt fragment de cod, dintr-un stash
mai vechi) nu a fost adusă înapoi — nu a fost cerută. Retestat live în
Chromium în timpul stress-test-ului din secțiunea 9: confirmat, ocolirea
`Ctrl+B` e reală și rămâne activă. Bug-ul 6 (găsit la testarea cu mesaje
lungi) e nerezolvat. Bug-ul 8 (Firefox) are acum un **fix aplicat și activ în
cod** (a patra încercare, de data asta bazată pe un diagnostic real din
Firefox, nu pe o presupunere) — verificat riguros în Chromium, dar **încă
neconfirmat de utilizator în Firefox real**; vezi finalul secțiunii 8. Vezi
secțiunea 9 pentru un stress-test suplimentar pe un mesaj lung cu multe
variabile și text editat, care a reconfirmat bug-ul 7 și a găsit un efect
secundar nou (dublă-împachetare + tag gol orfan) neconsemnat anterior.

## Cuprins

1. [Bug principal — selecție pierdută după formatare](#1-bug-principal--selecție-pierdută-după-formatare)
2. [Bug minor — resturi ZWSP la ștergerea unui chip](#2-bug-minor--resturi-zwsp-la-ștergerea-unui-chip)
3. [Bug — paragrafele se lipesc la numărătoare/validare pe texte lungi](#3-bug--paragrafele-se-lipesc-la-numărătoarevalidare-pe-texte-lungi)
4. [Bug (Firefox) — cursorul rămâne blocat în interiorul unui chip](#4-bucdg-firefox--cursorul-rămâne-blocat-în-interiorul-unui-chip)
5. [Comportamente verificate ca fiind corecte](#5-comportamente-verificate-ca-fiind-corecte)
6. [Testare cu mesaje lungi și formatare complexă (150+ cuvinte)](#6-testare-cu-mesaje-lungi-și-formatare-complexă-150-cuvinte)
7. [Bug (Firefox) — Ctrl+B/Ctrl+I/Ctrl+U nu fac nimic](#7-bug-firefox--ctrlbctrlictrlu-nu-fac-nimic)
8. [Bug (Firefox) — formatarea nu se aplică deloc când selecția include un chip](#8-bug-firefox--formatarea-nu-se-aplică-deloc-când-selecția-include-un-chip)
9. [Stress-test — mesaj lung cu multe variabile și text editat](#9-stress-test--mesaj-lung-cu-multe-variabile-și-text-editat)

---

## 1. Bug principal — selecție pierdută după formatare

**Severitate:** medie — vizibil utilizatorului, dar nu corupe conținutul salvat.

### Reproducere

1. Editor cu conținutul `Salut {{nume}} ce mai faci` (chip inserat din meniul
   "Inserează variabilă").
2. Selectează un fragment care traversează chip-ul: `"ut {{nume}} ce "`
   (parte din „Salut”, tot chip-ul, parte din textul următor).
3. Apasă **Bold**. Rezultat corect — toate cele trei bucăți devin bold:
   ```html
   Sal<span style="font-weight: bold;">ut ​</span
     ><span data-var="nume" style="font-weight: bold;">{{nume}}</span
     ><span style="font-weight: bold;">​ ce </span>mai faci
   ```
4. Fără să reselectezi manual, apasă imediat **Italic**. Rezultat:
   ```html
   Sal<span style="font-weight: bold;">ut ​</span
     ><span data-var="nume" style="font-weight: bold; font-style: italic;">{{nume}}</span
     ><span style="font-weight: bold; font-style: italic;">​ ce </span>mai faci
   ```
   Bucata **„ut ”** nu a primit italic, deși vizual selecția arăta neschimbată
   față de pasul 3.

### Cauză

`restore()` (`editor.component.ts`, ~L533-550) decide ce Range să reaplice
selecției curente:

```ts
const corrupted = !!live && !!this.sel && this.sel[0] !== this.sel[1] && live.collapsed;
const r = live && !corrupted && el.contains(live.commonAncestorContainer)
  ? live.cloneRange()
  : this.rangeAt(this.sel);
```

Preferă `liveRange` (Range-ul salvat pe nodurile DOM *vechi*, în `saveRange()`)
față de reconstrucția pe bază de offset-uri de caractere (`this.sel`), și îl
consideră valid câtă vreme nu e `collapsed`.

Problema: `execCommand('bold')` desparte nodul text `"Salut ​"` exact la
marginea selecției, via `splitText(3)`. `splitText` **păstrează nodul original**
ca prima bucată (`"Sal"`, lungime 3) și creează un nod nou pentru a doua bucată
(`"ut ​"`). `liveRange.startContainer` continuă să indice spre nodul original —
dar acesta a devenit acum `"Sal"`, nu `"ut ​"`. Boundary-ul `offset=3` e încă
tehnic valid (= capătul nodului), deci `live.collapsed` rămâne `false` și
verificarea de corupție nu detectează nimic — dar punctul de start real s-a
mutat, ducând la o selecție refăcută mai mică decât cea originală.

`this.rangeAt(this.sel)` (varianta pe offset-uri, calculată independent prin
`TreeWalker` peste toate nodurile text din editor) **reconstruiește corect**
intervalul original în acest caz — a fost verificat manual că recalculează
exact `[3, 20]` → `"ut ​{{nume}}​ ce "`. Problema e doar în alegerea dintre cele
două surse.

### Fix aplicat

În `exec()` și `applySize()`, imediat după ce DOM-ul a fost rescris
(`execCommand`, respectiv `range.extractContents()`/`insertNode()`), se
invalidează `this.liveRange = null` înainte de `reselect()`. Asta forțează
`restore()` să cadă pe `rangeAt(this.sel)` (reconstrucția din offset-uri de
caractere), care s-a verificat că reconstruiește corect intervalul original.

Verificat prin reluarea exactă a pașilor de reproducere: Bold urmat imediat de
Italic pe aceeași selecție mixtă aplică acum ambele formatări pe tot
intervalul (`"ut ​"` primește și `font-weight: bold`, și `font-style: italic`,
nu doar chip-ul și textul de după).

---

## 2. Bug minor — resturi ZWSP la ștergerea unui chip

**Severitate:** cosmetică (invizibilă), dar acumulează caractere fantomă în
HTML-ul salvat.

### Reproducere

1. Inserează un chip: `insertVar()` adaugă `&#8203;` (ZWSP) de-o parte și de
   alta a chip-ului, ca să existe o poziție de cursor între chip-uri lipite.
2. Poziționează cursorul imediat după chip și apasă **Backspace**.
3. Chip-ul e șters corect dintr-o singură apăsare (comportamentul dorit), dar
   cele două ZWSP rămân în DOM, lipite:
   ```
   Salut ​​ ce mai faci
   ```
   (două `​` consecutive, invizibile).

`stats()` le filtrează din numărătoarea de cuvinte/caractere
(`.replace(/​/g, '')`), deci nu afectează UI-ul vizibil — dar la inserări și
ștergeri repetate de chip-uri, HTML-ul salvat acumulează tot mai multe
caractere invizibile orfane.

### Fix aplicat

`onInput()` apelează acum și `cleanupZwsp()`: după `el.normalize()` (ca să
lipească în același nod text perechile împărțite între noduri vecine),
orice rulaj de 2+ ZWSP consecutive e redus la unul singur — suficient pentru
un punct de cursor între elementele vecine, fără resturile fantomă.

Verificat: după inserare chip + Backspace, `innerHTML` conține un singur
ZWSP (`Salut ​`), nu doi lipiți.

---

## 3. Bug — paragrafele se lipesc la numărătoare/validare pe texte lungi

**Severitate:** medie — apare la orice mesaj cu `Enter` (testat cu un mesaj de
~90 de cuvinte, pe două paragrafe): numărătoarea de cuvinte e greșită, iar
textul simplu produs de `plainText()` devine ilizibil la graniță.

### Reproducere

1. Editor cu un mesaj de-o singură linie (fără `Enter`), de exemplu terminat
   în `"...mereu la timp."`.
2. Apasă `Enter` și continuă pe altă linie, de exemplu `"Dacă aveți
   întrebări..."`.
3. Chrome inserează un `<div>` nou pentru fiecare linie de după primul
   `Enter` (comportamentul implicit de `contenteditable`, nu ceva scris de
   editor) — **dar prima linie rămâne text simplu, fără niciun tag**:
   ```html
   ...mereu la timp.<div>Dacă aveți întrebări...</div>
   ```
4. `stats()` și `plainText()` (`editor.component.ts`) construiau textul cu
   `.textContent` direct pe acest HTML. `.textContent` nu introduce niciun
   spațiu la granițele de bloc — rezultatul era `"timp.Dacă"`, cele două
   paragrafe lipite fără niciun separator.
5. Efect măsurat: pentru mesajul de test (2 paragrafe, ~90 de cuvinte),
   bara de jos arăta **„70 cuvinte”** în loc de 71 — cuvântul de la graniță
   se pierdea din numărătoare la fiecare paragraf nou.

### Cauză

Prima linie a unui `contenteditable` nu e niciodată învelită într-un tag —
doar liniile adăugate după un `Enter` primesc `<div>` (sau `<li>` într-o
listă, `<br>` cu Shift+Enter). Deci separatorul lipsă trebuie adăugat fie la
*deschiderea* unui bloc, fie la *închiderea* lui — depinde ce vine înainte.
Codul vechi nu adăuga niciunul.

### Fix aplicat

Funcție nouă `parseForText()` (lângă `varChip`/`templateToHtml`, la începutul
fișierului): înainte de a parsa HTML-ul într-un `<div>` detașat, introduce un
spațiu la fiecare `<br>` și la deschiderea **și** închiderea fiecărui
`<div>`/`<p>`/`<li>`. `stats()` și `plainText()` folosesc acum această funcție
în loc să parseze HTML-ul brut direct.

Verificat: același mesaj de test arată acum **„71 cuvinte · 450 caractere”**
(un caracter în plus față de 449 — exact spațiul de separare introdus la
granița celor două paragrafe), corect corelat cu numărătoarea manuală.

*Notă:* backend-ul (`TelegramHtmlFormatter.java`) nu a fost afectat — acolo
conversia HTML→text parcurge DOM-ul cu jsoup și tratează explicit `<div>`
(și `<li>`, `<p>` etc.) ca element de bloc, inserând linie nouă corect,
indiferent de adâncimea de imbricare. Doar codul din frontend, care citea
`.textContent` direct, avea problema.

---

## 4. Bug (Firefox) — cursorul rămâne blocat în interiorul unui chip

**Notă:** fix-ul descris în această secțiune (v1-v3) există doar în
`git stash` — ținut acolo la cererea explicită a utilizatorului, nu e activ
în `editor.component.ts` la ora actuală. Secțiunea rămâne ca document al
investigației, nu ca status curent al codului.

**Severitate:** mare — utilizatorul nu mai poate ieși din chip cu tastatura;
raportat de utilizator cu două screenshot-uri din Firefox, la navigare prin
`Salut, {{nume}} {{prenume}}.`.

**Nu s-a putut reproduce direct**: sesiunea Playwright MCP disponibilă rulează
doar Chromium (`navigator.userAgent` confirmat), unde comportamentul nativ e
deja corect. Fix-ul de mai jos e aplicat pe baza analizei codului + a
comportamentului cunoscut al Firefox, apoi verificat prin regresie în
Chromium — inclusiv forțând programatic exact starea "cursor prins în chip"
raportată, ca să testez recuperarea din ea.

### Cauză

Navigarea cu săgeata peste chip-uri se baza integral pe comportamentul nativ
al browserului pentru `contenteditable="false"`: niciun cod din editor nu
intercepta `ArrowLeft`/`ArrowRight`/click, doar ZWSP-urile puse de o parte și
de alta a chip-ului dădeau caret-ului un punct de oprire. Chrome tratează un
`contenteditable="false"` ca bloc atomic; Firefox (Gecko) e cunoscut ca
inconsecvent — caret-ul poate ajunge (la un click direct pe chip, sau la
navigare) în interiorul span-ului, unde nu mai există niciun punct de ieșire
valid pentru motorul de randare.

**Al doilea screenshot a arătat că primul fix (v1) nu era suficient**: acela
doar preveia *intrarea* într-un chip aflat lângă cursor (`adjacentChip()`),
presupunând că vecinătatea e mereu detectabilă din poziția curentă. Dar dacă
Firefox lăsase deja caret-ul *în interiorul* chip-ului — de exemplu printr-un
click direct pe pastilă, ceva ce doar `(keydown)` nu poate prinde — nicio
verificare de vecinătate nu se mai potrivea, iar săgeata rămânea blocată.

### Fix aplicat (v2)

Funcție nouă `chipAtCaret()`: verifică direct dacă nodul cursorului e *în
interiorul* unui `[data-var]` (`closest()`), indiferent cum a ajuns acolo.
Folosită în două locuri:

- **`onKeyDown`** — verifică întâi `chipAtCaret()` (scapă imediat dacă
  cursorul e deja prins), abia apoi `adjacentChip()` (previne intrarea).
- **`onSelect`** (legat acum și de `mouseup`, cu evenimentul) — dacă selecția
  de după un click a ajuns în interiorul unui chip, îl scoate imediat pe
  partea cea mai apropiată de `clientX`-ul click-ului (stânga/dreapta),
  folosind noul `exitChip()` (extras din logica comună de salt).

### Verificare

Nu am acces la Firefox în acest mediu, așa că am forțat programatic exact
starea din screenshot — cursor cu `Range` plasat manual în nodul text din
interiorul `{{prenume}}`, confirmat `insideChip: true` — apoi am apăsat
`ArrowRight`: cursorul a ieșit corect chiar după chip (`insideChip: false`).
Regresie completă reluată după: mers înainte/înapoi prin `Salut,
{{nume}} {{prenume}}.` tot atomic, într-o singură apăsare per chip, fără
erori în consolă.

*Recomandare:* dacă tot persistă ceva în Firefox după acest fix, ar trebui
retestat manual acolo — mediul de testare automată curent nu are acces la
Firefox.

### Corecție ulterioară (v3) — chenar de „selecție” în jurul chip-urilor

Utilizatorul a raportat, tot din Firefox, un efect secundar al fix-ului v2:
la navigare spre stânga printre cele două chip-uri, la un moment dat ambele
`{{nume}}` și `{{prenume}}` apăreau cu un chenar roșu/portocaliu, ca și cum
ar fi *selectate* — nu un caret normal, clipitor.

**Cauză:** `exitChip()` (v2) poziționa cursorul cu `Range.setStartBefore`/
`setStartAfter` direct pe elementul chip-ului (o graniță "la marginea unui
element", nu "în interiorul unui nod text"). Chrome randează identic ambele
forme de graniță — un caret normal — dar Firefox, când caret-ul se oprește
chiar la marginea unui `contenteditable=false`, îl desenează uneori ca
"element selectat" (chenar), la fel cum ar trata un `<img>` selectat, în loc
de un caret obișnuit.

**Fix:** `exitChip()` aterizează acum în nodul text ZWSP deja existent lângă
chip (`chip.nextSibling`/`previousSibling`, cu `Range.setStart` la începutul
sau finalul acelui nod text) — exact tipul de graniță pe care navigarea
nativă, funcțională, o produce oricum. Cade pe varianta veche
(`setStartBefore`/`setStartAfter`) doar dacă, neobișnuit, chip-ul n-ar avea
niciun nod text vecin.

Verificat în Chromium: reluând mersul complet stânga/dreapta prin `Salut,
{{nume}} {{prenume}}.`, la fiecare oprire cursorul e acum `nodeType: 3`
(nod text), niciodată `nodeType: 1` (element) — inclusiv la salvarea din
starea forțată "prins în chip". Zero erori în consolă.

### Fix-ul adus înapoi din stash (2026-09-17), la cererea utilizatorului

Utilizatorul a raportat separat (independent de acest bug) că trebuie să
apese săgeata stânga/dreapta de mai multe ori lângă un chip ca să treacă de
el. Analiza a arătat că simptomul are aceeași rădăcină documentată mai sus
(ZWSP-urile din jurul chip-ului costă câte o apăsare invizibilă fiecare), și
că fix-ul pentru bug-ul 4 de mai sus (ținut până acum doar în `git stash`,
niciodată activ în cod) e exact răspunsul potrivit. Utilizatorul a cerut
explicit să fie readus în cod, „foarte atent să nu strici fluxurile și
fixurile existente".

**Important — `git stash` conținea două variante diferite, nu una singură**,
scrise în sesiuni diferite:

- O variantă mai veche (`chipInDirection`/`selectedChip`), care sare peste
  chip aterizând direct pe marginea elementului (`setStartBefore`/
  `setStartAfter`) — **exact greșeala pe care v3 de mai sus a găsit-o și a
  reparat-o** (desenează chenar de "selecție" în Firefox în loc de caret).
  Nu tratează deloc cazul „cursor deja blocat în interiorul chip-ului".
- Varianta finală, descrisă integral mai sus (v1-v3: `chipAtCaret`/
  `adjacentChip`/`exitChip`), deja verificată prin trei runde de bug-uri
  găsite și reparate.

Am adus-o pe a doua (cea corectă/completă), **nu** pe prima — le-am găsit pe
amândouă abia citind `git stash show` pe fiecare intrare din stivă, după ce
inițial aplicasem din greșeală prima variantă (mai veche, mai simplă) și am
observat abia la revizuire că repeta exact bug-ul v2→v3 de mai sus. A fost
înlocuită înainte de orice verificare finală.

**O singură îmbunătățire față de varianta din stash:** `adjacentChip()`
original verifică un singur vecin — dacă acela e un ZWSP (nu chiar chip-ul),
renunță și lasă apăsarea următoare să treacă prin el nativ. Cu două chip-uri
inserate unul după altul (`{{nume}}{{prenume}}`), rezultă doi ZWSP separați
lipiți (câte unul de fiecare parte), nu unul singur — `adjacentChip()`
original ar fi lăsat tot un ZWSP „mort" între cele două salturi. I-am extins
verificarea ca să treacă (fără să se oprească) peste orice șir de noduri
text formate exclusiv din ZWSP înainte de a decide dacă următorul lucru
real e un chip — exact algoritmul din varianta veche (`chipInDirection`),
dar **doar pentru detectare**; aterizarea rămâne tot prin `exitChip()` (cel
din nodul text vecin, care evită chenarul din Firefox), nu prin
`setStartBefore`/`setStartAfter` cum făcea varianta veche.

**Verificare (Chromium), pe `Salut ​{{nume}}​​{{prenume}}​ test`** (chip-uri
alăturate, cursor pornind imediat înainte de cluster):

- Înainte de fix: 4 apăsări `ArrowRight` ca să treacă complet de ambele
  chip-uri (fiecare chip se sărea deja atomic dintr-o apăsare, nativ, în
  Chromium — costul de 4 venea integral din cele două ZWSP invizibile).
- După fix: **3 apăsări** — o apăsare nativă ireductibilă pentru ZWSP-ul de
  la marginea de start, apoi câte o singură apăsare per chip, indiferent
  câte ZWSP-uri separă doi chip-uri alăturate.
- Recuperare din "cursor forțat în interiorul chip-ului" (simulând bug-ul
  raportat inițial din Firefox): confirmat, o singură apăsare scoate
  cursorul curat, în nodul text vecin.
- `Shift+ArrowRight` lângă un chip: neatins, extinde selecția nativ (gardă
  explicită `e.shiftKey || e.altKey || e.ctrlKey || e.metaKey` la începutul
  lui `onKeyDown`, ca să nu deturneze extinderea de selecție).
- Regresie pe formatare: Bold pe o selecție text+chip, comutat de două ori
  (on → off) — text și chip intacte, identic cu înainte de acest fix.
- Zero erori noi în consolă (doar cele 5 preexistente, de la backend-ul
  inaccesibil pe `:8080`, nelegate de editor).

*Notă:* interceptarea `Ctrl+B/I/U` din bug-ul 7 (alt cod, dintr-un stash mai
vechi) **nu** a fost adusă — nu a fost cerută în acest schimb, rămâne
deschisă separat.

### Corecție a corecției — un chip izolat tot costa 2 apăsări

Utilizatorul a semnalat, imediat după fix-ul de mai sus: lângă un chip *între
alte două chip-uri* mersul e corect (o apăsare), dar lângă un chip *izolat*
(înconjurat de text normal) tot sunt necesare două apăsări ca să treacă de
el — inconsecvent.

**Cauză:** `adjacentChip()`, așa cum a fost adus din stash, avea două
condiții diferite pentru „sunt la marginea dinspre chip, pot sări":
- pentru un nod text făcut *în întregime* din ZWSP (cazul dintre doi
  chip-uri alăturate, fără text între ele) — orice poziție din el conta
  direct ca „la margine", indiferent de offset;
- pentru un nod text *mixt*, cu ZWSP lipit de text real (cazul unui chip
  izolat: `"Salut "` + ZWSP e un singur nod text) — trebuia să fii la
  offset-ul *exact* egal cu lungimea nodului, adică să fi trecut deja și de
  ultimul ZWSP printr-o apăsare separată, native, înainte ca funcția să-l
  recunoască drept "la margine" și să sară peste chip.

Diferența dintre cele două reguli explică exact ce a raportat utilizatorul:
între chip-uri (regula 1) saltul e imediat; lângă un chip izolat (regula 2)
mai era nevoie de o apăsare invizibilă în plus, doar ca să treacă de ZWSP,
înainte de saltul propriu-zis.

**Fix:** o singură regulă, care le acoperă pe amândouă. În loc de „sunt
*exact* la capătul nodului" sau „nodul e *numai* ZWSP", verificăm „*ce mai
rămâne* de parcurs din acest nod, în direcția de mers, pornind de la poziția
curentă, e numai ZWSP?" (`value.slice(idx)` pentru dreapta, `value.slice(0,
idx)` pentru stânga, testat cu `/^​*$/`). Un nod în întregime ZWSP trece
testul de la orice offset (ca înainte); un nod mixt trece testul din
momentul în care singurul lucru rămas de traversat e coada/capul lui de
ZWSP, nu doar la capătul absolut — deci nu se mai pierde o apăsare separată
doar pentru caracterul invizibil de dinaintea saltului.

**Verificare (Chromium):**

- Chip izolat (`Salut ​{{nume}}​ test`, cursor imediat înainte de ZWSP-ul de
  dinaintea chip-ului): **o singură apăsare** `ArrowRight` sare direct peste
  chip, aterizând la începutul textului rămas de partea cealaltă (`​ test`).
  Înainte de această corecție erau necesare 2. Simetric pe `ArrowLeft`.
- Cluster de 2 chip-uri alăturate (`Salut ​{{nume}}​​{{prenume}}​ test`):
  acum **2 apăsări** pentru tot clusterul (înainte de această corecție
  erau 3 — chiar ZWSP-ul de la marginea de intrare beneficiază acum de
  aceeași regulă, nu doar cele dintre chip-uri).
- Retestat, neschimbate: recuperarea din „cursor prins în chip", extinderea
  cu `Shift+ArrowRight`, toggle Bold pe selecție text+chip (on→off, text și
  chip intacte). Zero erori noi în consolă.

---

## 5. Comportamente verificate ca fiind corecte

Testate explicit, fără reproduceri de bug:

- **Navigare cu săgeți peste chip** — o singură apăsare `ArrowRight`/`ArrowLeft`
  sare atomic peste tot chip-ul (datorită ZWSP-urilor din jur), fără ca
  cursorul să rămână "blocat" în interior.
- **Backspace pe chip** — șterge tot chip-ul dintr-o singură apăsare, nu
  caracter cu caracter.
- **Selecție doar-pe-chip** — dublu-click pe textul intern al chip-ului (ex.
  „nume” din `{{nume}}`) selectează tehnic doar cuvântul, dar `snapOut()`
  extinde corect selecția la tot span-ul `[data-var]` înainte de a aplica
  mărime/stil — comportamentul e atomic din punct de vedere funcțional.
- **Toggle Bold pe chip selectat singur** — pornit → oprit funcționează corect
  în ambele sensuri (verificat prin dublu-click + Bold, apoi din nou
  dublu-click + Bold).
- **Mărime text (14/16/20/24px)** — testată separat pe text simplu, separat pe
  chip, și pe o selecție mixtă (text 14px + chip 24px + text implicit, toate
  într-o singură selecție): rezultatul e mereu uniform corect pe toată
  selecția, restul conținutului rămâne neschimbat.
- **Nicio eroare în consola browserului** pe parcursul tuturor testelor
  (inserare, ștergere, formatare, navigare).
- **Text lung (~90 cuvinte, 2 paragrafe, 3 variabile)** — scris caracter cu
  caracter (nu `.fill()`), cu spații duble intenționate în mijlocul
  textului: se păstrează exact ca spații normale (cod 32, nu spațiu
  insecabil 160), afișate vizibil datorită `white-space: pre-wrap`.
  Împachetarea pe rânduri (word-wrap) e corectă, chip-urile rămân pastile
  intacte la capăt de rând, fără să se rupă vizual.
- **Listă cu buline + chip + ieșire din listă cu Enter dublu** — chip-ul
  dintr-un `<li>` funcționează identic cu unul din text simplu; al doilea
  `Enter` pe un rând gol iese corect din listă, textul următor rămâne text
  simplu (nu `<li>` gol). Structura DOM rezultată imbrică un `<div>` în plus
  în jurul listei (particularitate cunoscută a `contenteditable` din Chrome,
  nu ceva scris de editor) — nu are niciun efect vizual sau funcțional
  observat.

---

## 6. Testare cu mesaje lungi și formatare complexă (150+ cuvinte)

Mesaj de test scris caracter cu caracter în `/mesaj/compune`: titlu, paragraf
cu 2 variabile inserate prin tastare directă (`autoChip`) și porțiune bold,
listă cu buline (3 puncte), listă numerotată (3 puncte, cu o variabilă
inserată de data asta din meniu, nu prin tastare), paragraf final colorat cu
o propoziție subliniată — 146 cuvinte, 4 variabile, 3 mărimi de text diferite
aplicate pe fragmente separate.

### Comportamente verificate ca fiind corecte

- **Numărătoare de cuvinte/caractere/variabile** — corectă pe tot parcursul
  construcției mesajului (verificat la fiecare pas, de la 19 la 146 cuvinte),
  inclusiv după inserarea de span-uri imbricate pentru mărime de text.
- **Fără lipire de cuvinte la graniță** — verificat explicit cu `plainText()`
  pe documentul final: titlu -> paragraf -> listă cu buline -> listă
  numerotată -> paragraf final, toate granițele au spațiu corect, nicio
  pereche de cuvinte lipite (fix-ul de la secțiunea 3 ține și pe un document
  cu 4 nivele de blocuri imbricate, nu doar pe 2 paragrafe simple).
- **Variabile prin tastare directă vs. prin meniu** — ambele căi de inserare
  produc chip-uri identice structural (ZWSP pe ambele părți la inserarea din
  meniu; un singur ZWSP, după chip, la conversia automată din tastare — o
  asimetrie minoră, dar fără efect vizibil, pentru că spațiul obișnuit dintre
  cuvinte oricum oferă un punct de cursor unde ar fi nevoie).
- **Bold/Culoare/Subliniat pe fragmente imbricate** — paragraful final are
  culoare pe tot textul și subliniere doar pe ultima propoziție, corect
  imbricate (`<span color><span underline>...</span></span>`), fără să
  "scape" formatarea în afara fragmentului dorit.
- **Mărime de text pe selecții precise** — testat cu selecție simplă (un
  cuvânt), cu selecție ce traversează un `<span style="font-weight: bold">`
  existent (rezultă corect un `<span font-size>` imbricat în interiorul celui
  de bold, nu-l rupe), și pe titlu întreg — toate au aplicat mărimea exact pe
  fragmentul selectat, fără să atingă restul documentului.
- **Zero erori/avertismente în consolă** pe parcursul întregii construcții a
  mesajului (scris, formatat, listă, variabile, redimensionări).

### Bug — Ctrl+Z nu anulează schimbarea de mărime a textului

**Severitate:** medie — nu corupe nimic, dar comportament surprinzător și
inconsecvent față de restul comenzilor de formatare.

**Reproducere:**

1. Selectează un cuvânt și aplică o mărime de text din meniul "Mărime text"
   (ex. "Text mare" 20px). Rezultat corect: `<span style="font-size: 20px;">cuvânt</span>`.
2. Apasă `Ctrl+Z`. Nimic nu se întâmplă — span-ul cu mărimea rămâne neschimbat.
3. Pentru comparație: selectează alt cuvânt, aplică **Bold** (`Ctrl+B` sau
   butonul din bară), apoi `Ctrl+Z` — de data asta `<b>` dispare corect,
   undo funcționează.

**Cauză:** `applySize()` (`editor.component.ts`, ~L426-449) nu folosește
`document.execCommand` — construiește manual span-ul de mărime cu
`range.extractContents()` / `range.insertNode()`, ca să evite efectele
secundare ale lui `execCommand('fontSize')` (acceptă doar valori 1-7, șterge
noduri golite între chip-uri etc., vezi comentariul din cod). Dar orice
manipulare manuală de DOM ocolește complet stiva nativă de undo a
browserului pentru `contenteditable` — acea stivă se populează doar din
comenzi trecute prin `execCommand` (Bold/Italic/Underline/culoare/liste,
toate folosite prin `exec()`, care apelează `execCommand`). Rezultatul:
utilizatorul poate anula orice altă formatare cu Ctrl+Z, dar nu și o
schimbare de mărime — inconsecvență ușor de observat la un mesaj lung, unde
mărimea de text e printre operațiile cele mai probabil folosite (titluri,
evidențieri).

**Verificat și separat:** Ctrl+Z nu produce nicio corupere sau efect
secundar vizibil — pur și simplu ignoră schimbarea de mărime și trece la
următoarea acțiune din stivă (sau nu face nimic, dacă nu mai e nimic de
anulat). Nu am aplicat un fix — las decizia la utilizator, pentru că
soluțiile posibile au compromisuri: fie se reimplementează `applySize()` cu
`execCommand('fontSize')` + curățare manuală a valorii numerice (1-7 -> px),
fie se ține propria stivă de undo doar pentru mărime, ambele fiind mai mult
decât o corecție minimă.

### Notă — incident izolat, neconcludent, la o secvență de testare atipică

La un moment dat, în timpul testării (după o secvență atipică: o selecție
imprecisă dintr-un locator Playwright ambiguu a dus la aplicarea mărimii pe
niște chip-uri nelegate de selecție, am încercat Ctrl+Z ca să anulez — fără
efect, vezi bug-ul de mai sus — apoi am reparat manual DOM-ul direct din
consolă, cu `element.style.removeProperty()` și un eveniment `input`
sintetic trimis manual, nu tastat), documentul întreg a apărut încapsulat
într-un singur `<span style="font-size: 14px;">`, cu un
`<span style="font-size: 24px;"></span>` gol la început. Nu am reușit să
reproduc acest efect cu o secvență curată (build documentul de la zero,
aceleași operații de redimensionare, fără intervenție manuală prin
`dispatchEvent`/editare directă de DOM din consolă) — e foarte probabil un
artefact al reparației manuale prin JavaScript direct în consolă (ceva ce
niciun utilizator real n-ar face din interfață), nu un bug reproductibil
prin interacțiune normală. Semnalez totuși, pentru completitudine: dacă
apare din nou fără o astfel de intervenție manuală, merită reinvestigat.

---

## 7. Bug (Firefox) — Ctrl+B/Ctrl+I/Ctrl+U nu fac nimic

**Severitate:** medie — utilizatorul poate formata doar din bara de sus, nu
și cu scurtăturile de tastatură standard, doar în Firefox.

**Raportat de utilizator.** Nu s-a putut reproduce direct (mediul de
testare automată e Chromium, vezi nota de la bug-ul 4), dar cauza e clară
din cod, fără ambiguitate.

### Cauză

Editorul nu lega nimic explicit de `Ctrl+B`/`Ctrl+I`/`Ctrl+U` — butoanele din
bară apelau `exec(e, 'bold'|'italic'|'underline')` la `(mousedown)`, dar
scurtăturile de tastatură se bazau 100% pe comportamentul implicit al
browserului pentru `contenteditable`. Chrome/Blink și Safari/WebKit leagă
implicit aceste taste de `execCommand` pentru orice `contenteditable`;
Firefox/Gecko nu face asta pe un `contenteditable` simplu — o diferență
cunoscută între motoare, nu ceva specific acestui editor. De-aici și
inconsecvența: „merge" în Chrome din întâmplare, nu pentru că editorul ar
face ceva anume.

Efect secundar, valabil chiar și acolo unde scurtătura „mergea" (Chrome):
pentru că ocolea complet `exec()`, o selecție care traversa și un chip
(`{{nume}}`) nu primea `font-weight: bold` pe chip prin scurtătură — doar
clicul pe buton trecea prin `styleChip()`. Scurtăturile erau deci
inconsecvente cu bara chiar și în browserul unde funcționau.

### Fix aplicat (v1) — și regresia găsită de utilizator

Handler nou `onKeyDown()`, legat de `(keydown)` pe canvas: intercepteaza
`Ctrl+B`/`Ctrl+I`/`Ctrl+U` (verifică `ctrlKey`/`metaKey`, ignoră
`altKey`/`shiftKey`), face `preventDefault()` și apelează direct
`this.exec(e, cmd)` — exact codul folosit de butoanele din bară.

**Regresie raportată:** după apăsări repetate de `Ctrl+B`, bold-ul nu se mai
punea/scotea deloc de pe selecție — rămânea blocat.

**Cauză:** `keydown` se repetă automat cât timp tasta rămâne apăsată
(`KeyboardEvent.repeat`). O apăsare ținută chiar și puțin mai mult declanșa
zeci de apeluri `exec('bold')` în rafală — fiecare rescrie DOM-ul (span nou/
`extractContents`/`insertNode`) și reciteste `document.queryCommandState`
pentru următorul toggle. La ritmul ăsta, span-urile fragmentate rămase din
apelurile anterioare confundau `queryCommandState`, care ajungea să nu mai
reflecte corect starea reală — bold-ul se bloca. Browserul nativ nu are
problema asta pentru că nu tratează `Ctrl+B` ca pe o comandă „care se repetă
la ținere" — dar handler-ul custom, fără nicio gardă, da.

### Fix aplicat (v2)

O singură linie în plus, la începutul lui `onKeyDown()`: `if (e.repeat) return;`
— ignoră orice `keydown` care vine din auto-repeat, acționează doar la
apăsarea fizică inițială a combinației. Restul (interceptare, `preventDefault`,
apel `exec()`) neschimbat.

### Verificare

Testat în Chromium: selecție text+chip, `Ctrl+B` prin tastatură → chip-ul
primește `font-weight: bold` corect (înainte se întâmpla doar la clic pe
buton). Apoi, specific pentru regresie: 15 evenimente `keydown` sintetice cu
`repeat: true` trimise consecutiv pe aceeași selecție (simulând o tastă
ținută apăsată) — zero efect, starea bold rămâne neschimbată; o apăsare
reală ulterioară (`repeat: false`) comută corect starea. Toggle on/off
funcționează în ambele sensuri, `Ctrl+I`+`Ctrl+U` combinate se aplică
amândouă corect, chip inclus. Zero erori în consolă.

*Recomandare:* la fel ca la bug-ul 4, ar merita o confirmare rapidă direct
în Firefox când aveți ocazia — mediul curent nu poate testa asta direct.

---

## 8. Bug (Firefox) — formatarea nu se aplică deloc când selecția include un chip

**Severitate:** mare — nu doar Ctrl+B, ci și **butonul Bold din bară** pică
identic pe aceeași selecție; afectează probabil Italic/Underline/Culoare/
Liste la fel, pentru că toate trec prin același `exec()`.

**Raportat de utilizator**, cu screenshot: selecție `"lut, {{nume}}
{{prenume}}"` (text + 2 chip-uri), `Ctrl+B` nu face nimic. Am întrebat
explicit dacă butonul din bară (cod neatins de fix-urile recente de
tastatură) are aceeași problemă — **da, la fel**. Deci nu ține de
`onKeyDown()`, ci de `exec()` însuși, cod preexistent, neschimbat în acest
raport.

**Nu s-a putut reproduce în Chromium**: aceeași selecție exactă, aplicată
prin `Ctrl+B` sau prin butonul din bară, formatează corect atât textul cât
și ambele chip-uri (`font-weight: bold` pe toate trei bucățile). Mediul de
testare automată disponibil e doar Chromium (vezi nota de la bug-ul 4).

### Cauză probabilă (neconfirmată)

`exec()` rulează un singur `document.execCommand(cmd, ...)` peste tot
Range-ul selectat, inclusiv peste chip-urile `contenteditable="false"` din
interior. Chrome tratează un `contenteditable="false"` ca pe un bloc atomic
pe care execCommand îl „sare" — formatează normal textul din jur. Gecko
(Firefox) e cunoscut ca fiind mai strict: poate refuza să execute comanda
**deloc** când Range-ul traversează o graniță `contenteditable="false"`, nu
doar pe chip, ci pe toată selecția — ceea ce ar explica exact raportul: nici
textul, nici chip-ul nu primesc formatare, indiferent de calea (tastatură
sau buton) prin care se declanșează comanda.

### Fix aplicat

`exec()` nu mai rulează un singur `execCommand` peste tot Range-ul cînd
selecția include chip-uri. Funcție nouă `execAroundChips()`: desparte
selecția în bucăți care sar peste fiecare chip (text-înainte-de-primul-chip,
text-între-chip-uri, text-după-ultimul-chip, în ordinea din document — deja
garantată de `chipsInSelection()`) și rulează `execCommand` separat pe
fiecare bucată editabilă. Firefox nu mai primește niciodată un Range care
traversează un `contenteditable="false"`. Chip-urile rămân stilate manual
prin `styleChip()`, neschimbat.

Pentru comenzile de comutare (Bold/Italic/Underline), fiecare bucată își
poate porni dintr-o stare diferită de restul selecției — `execCommand` fără
valoare ar comuta orbește bucata respectivă. De-aia, starea dorită (`on`,
calculată o singură dată, înainte de despărțire, la fel ca înainte) e
verificată per-bucată cu `queryCommandState`, iar `execCommand` se cheamă
doar dacă bucata nu e deja în starea cerută — ca toată selecția să ajungă
uniform bold/nu-bold, nu pe bucăți amestecate.

Când selecția n-are niciun chip, comportamentul e neschimbat: un singur
`execCommand` pe tot Range-ul, ca înainte — fix-ul nu atinge calea comună.

### Verificare

Nu am acces la Firefox (vezi nota de la bug-ul 4), deci verificarea directă
a cauzei rămâne imposibilă în acest mediu — dar am reprodus exact scenariul
raportat (inclusiv reordonarea punctului) în Chromium ca test de regresie
pentru noul cod:

- Selecție identică cu cea din screenshot (`"ut, {{nume}}​ {{prenume}}​ ."`,
  pornind din mijlocul „Salut,"), Bold din bara de sus: toate cele trei
  bucăți de text și ambele chip-uri devin `font-weight: bold`, punctul
  rămâne exact la final, nicio reordonare de noduri.
- Aceeași selecție, Bold din nou (toggle off): totul revine curat la
  neformat, ordinea intactă.
- Selecție fără niciun chip (`"Salut"` dintr-un text simplu): Bold produce
  exact `<span style="font-weight: bold;">Salut</span>` ca înainte —
  calea comună (fără chip-uri) e neatinsă de fix.
- Zero erori/avertismente în consolă în toate cazurile.

*Recomandare:* la fel ca la celelalte bug-uri de Firefox din acest raport,
merită o confirmare directă în Firefox — mediul curent nu poate testa asta.

### Dovadă suplimentară — nu doar „nu face nimic", ci reordonare de conținut

**Raportat de utilizator**, cu screenshot, al doilea caz observat pe aceeași
cauză. De data asta selecția nu include doar chip-uri, ci și un `.` (punct)
imediat lângă un chip: `Salut, {{nume}}​ {{prenume}}​ .`. La `Bold` pe
această selecție, în Firefox, punctul **sare peste ambele chip-uri** și
ajunge lipit imediat după „Salut,", înainte de `{{nume}}`:

```
Înainte:  Salut, {{nume}} {{prenume}} .
După Bold: Salut, . {{nume}} {{prenume}}    (doar "Salut," + "." sunt bold)
```

Deci nu doar chip-urile refuză formatarea (ca la raportul inițial de mai
sus) — de data asta `execCommand('bold')` chiar **rearanjează noduri text în
DOM**, mutând punctul înaintea celor două chip-uri. Confirmă că problema nu
e doar „execCommand nu face nimic peste graniță de `contenteditable=false`",
ci că manipularea internă a lui Gecko (probabil `splitText`/reinserare de
noduri în jurul chip-urilor atomice) poate produce efectiv conținut corupt,
nu doar formatare lipsă.

**Observație suplimentară a utilizatorului, relevantă pentru cauză:** dacă
textul e mai întâi copiat din Chrome (unde fusese scris corect) și lipit
direct în editorul din Firefox, apoi Bold aplicat pe aceeași selecție —
punctul **rămâne la locul lui**, doar formatarea se aplică. Diferența dintre
cele două căi nu e conținutul vizibil (identic), ci structura DOM din spate:
textul „proaspăt" din editor conține ZWSP-urile puse de `insertVar()`/
`autoChip()` de o parte și de alta a fiecărui chip; e posibil ca paste-ul să
normalizeze/simplifice acea structură (browserul curăță HTML-ul lipit),
eliminând exact granițele care declanșează bug-ul din Gecko. Susține în
continuare aceeași cauză probabilă de mai sus — problema pare legată strict
de interacțiunea `execCommand` cu granițele `contenteditable="false"` +
ZWSP din jurul lor, nu de conținut în sine.

Acest caz nu schimbă recomandarea de mai sus (fix-ul cu despărțirea
Range-ului pe bucăți editabile) — dar crește severitatea reală a bug-ului: nu
e doar o formatare care lipsește, ci conținut care își schimbă ordinea sub
ochii utilizatorului.

### Fix-ul cu despărțirea Range-ului, retestat în Firefox real — nu a rezolvat

Fix-ul descris mai sus (`execAroundChips()`) a fost aplicat efectiv în cod și
retestat de utilizator direct în Firefox (nu doar în Chromium, ca la
verificarea inițială). **Rezultat: aceeași reordonare, identică** — punctul
tot sare înaintea celor două chip-uri. Utilizatorul a pus fix-ul înapoi în
`git stash`.

Asta e o dovadă importantă: fix-ul garanta explicit că niciun apel
`execCommand` nu primea vreodată un Range care traversează un chip (fiecare
bucată editabilă era procesată separat, cu chip-urile excluse din orice
Range pasat comenzii). Faptul că reordonarea a persistut identic înseamnă că
**ipoteza inițială era greșită** — nu Range-ul care traversează chip-ul e
declanșatorul. Probabil algoritmul intern al lui Gecko pentru comenzile de
stil (bold/italic/underline) rearanjează noduri `contenteditable="false"`
aflate oriunde în apropiere în blocul editat, ca parte a unei etape interne
de "curățare"/împachetare a formatării — indiferent ce Range exact i-a fost
pasat lui `execCommand`. Despărțirea Range-ului pe bucăți nu are cum să evite
asta, pentru că problema nu e în Range-ul pasat, ci în ce face Gecko cu restul
DOM-ului din jur după aceea.

### A doua încercare — `user-select: all` pe chip-uri — nici asta nu a rezolvat

Ipoteză nouă: `[data-var]` (`frontend/src/styles.scss`) nu avea niciodată
`user-select: all` — proprietate CSS al cărei rol exact e să spună
browserului "tratează acest element ca o unitate atomică, indivizibilă,
pentru selecție". Prototipul static de design (`frontend/design/Trimitere
Mesaje.dc.html`, fișier HTML separat, nu parte din aplicația reală) *avea*
`user-select:all` pe marcajul lui de chip — proprietatea s-a pierdut pe
undeva între prototip și implementarea reală (`varChip()` +
`styles.scss`). Absența ei era un candidat plauzibil pentru toate cele trei
bug-uri Firefox legate de chip-uri din acest raport (cursor blocat în chip —
bug 4 — și reordonarea de aici), pentru că toate țin, la bază, de cum tratează
motorul de Selection/Range al browserului granița chip-ului.

**Aplicat**: adăugat `user-select: all;` / `-webkit-user-select: all;` pe
regula `[data-var]`. Verificat fără regresii în Chromium (dublu-click pe
textul din interiorul unui chip tot selectează tot elementul ca unitate,
Bold cu chip în selecție tot funcționează corect, navigare cu săgeți
neschimbată, zero erori consolă) — dar Chromium nu reproduce oricum bug-ul.

**Retestat de utilizator în Firefox real: aceeași reordonare, neschimbată**
(screenshot: „Sa" neformatat, apoi „ut, ." selectat/bold, apoi cele două
chip-uri — punctul tot ajunge lipit de „Sa", înaintea chip-urilor). CSS-ul nu
a rezolvat cauza reală. **Revenit** — proprietatea a fost scoasă din
`styles.scss`, ca să nu rămână o modificare nedovedită în cod.

### Diagnostic direct din Firefox — `innerHTML` înainte/după, de la utilizator

Trei încercări la rând (despărțire de Range, `user-select: all`, plus
raționamentul inițial despre chip-uri „sărite" de execCommand) au eșuat la
testarea reală în Firefox. În loc de o a patra ghicire, utilizatorul a rulat
în consola Firefox (F12), chiar pe reproducerea bug-ului, `copy(document.
querySelector('.canvas').innerHTML)` înainte și după apăsarea Bold:

```html
<!-- Inainte -->
Salut, ​<span data-var="nume" contenteditable="false">{{nume}}</span>​<span data-var="prenume" contenteditable="false">{{prenume}}</span>​,

<!-- Dupa Bold pe selectia "lut, ...," -->
Sa<span style="font-weight: bold;">lut, ​.</span><span data-var="nume" contenteditable="false" style="font-weight: bold;">{{nume}}</span><span data-var="prenume" contenteditable="false" style="font-weight: bold;">{{prenume}}</span>
```

Asta a fost concludent, nu încă o presupunere. Ce face de fapt Gecko:
1. Desparte textul dinainte de selecție („Sa" rămâne simplu, în afara oricărui span).
2. Ia **tot textul/ZWSP editabil din selecție** — „lut, ", ZWSP-ul dintre chip-uri,
   ZWSP-ul + virgula finală — și le **concatenează într-un singur span bold nou**,
   ca și cum chip-urile n-ar fi existat deloc între ele.
3. **Mută ambele chip-uri după acel span consolidat**, în loc să le lase
   intercalate în pozițiile lor originale.

Deci ipoteza „nu lăsa Range-ul să traverseze un chip" era categoric greșită —
problema nu ține de Range-ul pasat lui `execCommand`, ci de faptul că
implementarea lui Gecko pentru comenzile de stil, când întâlnește insule
needitabile în selecție, adună textul editabil din jurul lor într-un span unic
și împinge insulele deoparte. Nicio despărțire de Range client-side sau vreun
indiciu CSS nu poate preveni asta, pentru că se întâmplă în interiorul
algoritmului propriu al lui `execCommand`, indiferent de forma exactă a
Range-ului.

### Fix aplicat — abandonarea completă a `execCommand` pentru Bold/Italic/Underline/Culoare

`applySize()` (aceeași filă) demonstrează deja calea de ieșire: nu apelează
niciodată `execCommand` — folosește direct `Range.extractContents()`/
`insertNode()`, un API DOM standard fără conceptul de „adună textul editabil
și mută insulele" — mută exact nodurile din Range, în ordinea lor originală
din document, indiferent de `contenteditable`. Bold/Italic/Underline/Culoare
folosesc acum același tipar, printr-o metodă nouă `toggleFormat()`, apelată
din `exec()` doar pentru `bold`/`italic`/`underline`/`foreColor` și doar când
selecția nu e goală — o selecție goală (ex. „apasă Bold, apoi scrie") rămâne
pe `execCommand`, ca să păstreze stilul de scriere „în așteptare" la cursor,
nativ browserului.

`exec()` mai calculează acum singur starea „e totul deja formatat" (funcție
nouă `allFormatted()`), în loc să se bazeze pe `document.queryCommandState`,
ca să nu depindă de `execCommand` nici măcar pentru citit starea.

### Trei bug-uri reale găsite și reparate în timpul verificării în Chromium

Implementarea inițială a acestui fix, testată riguros (nu doar „merge o
dată"), a scos la iveală trei probleme reale, toate reparate înainte de a
considera fix-ul gata:

1. **`Range.intersectsNode()`/`compareBoundaryPoints()` sunt „generoase" la
   graniță.** `allFormatted()` folosea inițial `intersectsNode()` (apoi o
   variantă cu `compareBoundaryPoints()`) ca să vadă ce noduri de text sunt
   „în" selecție — dar un nod care doar **atinge** granița selecției (ex.
   textul de dinainte de început, sau un nod text gol lăsat de
   `extractContents()` la graniță) putea ieși „inclus", ducând la un
   fals-negativ: `allFormatted()` raporta „nu-i totul formatat" chiar și
   după ce chiar acea comandă tocmai formatase totul corect, iar un al doilea
   clic pe Bold **aplica din nou** (împachetare imbricată), în loc să scoată
   formatarea. **Fix:** `allFormatted()` recalculează acum offsete de
   caracter pentru Range (exact ca `saveRange()`/`rangeAt()`, deja dovedite
   corecte în restul fișierului) și verifică suprapunere aritmetică
   (`seen < end && seen + len > start`), nu comparații de graniță pe noduri.
2. **Wrapper-ul nou nu ștergea proprietatea conflictuală de pe descendenți.**
   La al treilea clic (Italic, apoi din nou Italic, apoi Italic) dintr-o
   secvență de teste, textul din stânga selecției rămânea vizual ne-italic
   deși wrapper-ul nou avea `font-style: italic` — pentru că un `<span>`
   rămas dintr-un toggle anterior avea `font-style: normal` setat direct pe
   el, iar o proprietate setată direct pe un element bate mereu una moștenită
   de la un ancestor, oricât de „din afară" ar fi acesta. **Fix:**
   `toggleFormat()` șterge acum proprietatea `cmd`-ului curent de pe toți
   descendenții fragmentului extras înainte de a-l împacheta în wrapper-ul
   nou (același tipar pe care `applySize()` îl folosește deja pentru
   `font-size`).
3. **Toggle-uri repetate acumulau `<span>`-uri goale/redundante imbricate.**
   Fiecare comutare putea lăsa un `<span style="">` (fără nicio proprietate)
   sau un clon complet gol la granița Range-ului, din cauza modului cum
   `extractContents()` tratează un capăt de Range aflat exact la marginea
   unui nod. Peste multe comutări, acestea se imbricau tot mai adânc. **Fix:**
   `toggleFormat()` rulează acum, după fiecare comandă: șterge span-urile
   complet goale (fără text și fără chip înăuntru), desface (unwrap, păstrând
   conținutul) span-urile cu `style=""` fără alt atribut, apoi `normalize()`
   pe canvas ca să lipească nodurile text vecine. Verificat cu 8 comutări
   Italic consecutive pe o selecție text+chip: starea finală e identică celei
   de la 4 comutări, fără nicio acumulare de span-uri goale.

### Verificare (Chromium)

- Reproducerea exactă din diagnosticul de mai sus (`"lut, {{nume}}​{{prenume}}​,"`,
  pornind din mijlocul „Salut,"), Bold din bara de sus: toate cele trei bucăți
  de text și ambele chip-uri devin `font-weight: bold`, ordinea intactă — apoi
  toggle off (al doilea clic, fără reselectare manuală, exact fluxul unui
  utilizator real): revine curat, fără span-uri goale rămase.
- Text simplu fără niciun chip: comportament identic cu cel dinainte de fix
  (`<span style="font-weight: bold;">Salut</span>`), calea comună neatinsă.
- Selecție text+chip, 8 comutări Italic consecutive (click, fără reselectare
  între ele): stare finală corectă la fiecare pas (on/off alternând corect),
  fără acumulare de `<span>`-uri goale imbricate.
- `foreColor` (Roșu) pe selecție text+chip: culoare aplicată uniform pe tot
  textul și ambele chip-uri, ordine intactă.
- Selecție goală (doar cursor) + Bold + tastare: rămâne pe calea nativă
  `execCommand`, stilul „în așteptare" la cursor funcționează neschimbat.
- Zero erori/avertismente în consolă în toate cazurile de mai sus.

**Nu s-a putut verifica din nou direct în Firefox** (mediul de testare automată
rămâne exclusiv Chromium) — dar de data asta fix-ul e derivat dintr-un diff
real de `innerHTML` produs chiar de Gecko, nu dintr-o presupunere. Rămâne de
confirmat de utilizator la următoarea testare manuală în Firefox.

---

## 9. Stress-test — mesaj lung cu multe variabile și text editat

Testare cerută explicit: un mesaj de ~150 cuvinte, cu multe variabile
repetate și mult text deja formatat (simulând un mesaj editat manual de mai
multe ori), ca să scoată la iveală bug-uri pe care mesajele scurte de test nu
le ating. Rulat 2026-09-17, în Chromium, pe `/mesaj/compune`.

### Conținutul de test

63 de cuvinte vizibile, 377 caractere de text propriu-zis, **10 variabile**
(`{{prenume}}`, `{{nume}}`, `{{grup}}` ×4, `{{email}}` ×2 etc.), cu porțiuni
deja înfășurate în **bold**, *italic*, <u>underline</u>, culoare roșie și
combinații bold+italic — imitând un mesaj ajuns la a treia-a patra editare,
nu unul scris o singură dată.

### Ce a fost testat și a ieșit curat

Toate prin `exec()` (fie apelat direct pe componentă cu selecție salvată
corect prin `saveRange()`, fie prin clic real pe butoanele din bară — ambele
căi testate):

- Bold pe o selecție care traversează un span deja bold+italic, un chip, și
  intră într-un span colorat — comutat **6 ori consecutiv** pe aceeași
  selecție: text și chip-uri intacte (484 caractere, 10 variabile, la fiecare
  pas), zero `<span>`-uri goale.
- Selecție „mixtă" (parte deja bold, parte nu) de la începutul mesajului
  până în mijloc — comutare on/off: nicio pierdere de text, nicio dublare.
- Selecție pe **tot mesajul** (0 → 484 caractere), underline on/off: la fel,
  curat.
- `foreColor` (verde) peste o regiune care avea deja **două culori diferite**
  (roșu și albastru) — se aplică uniform, fără resturi din culorile vechi.
- Selecție de exact un singur chip (`selectNode` pe `{{email}}`), italic
  on/off: chip-ul își schimbă `font-style` fără să atingă restul.
- Zero erori/avertismente în consolă pe parcursul întregului test.

Concluzie: fix-ul din secțiunea 8 (bypass-ul `execCommand` pentru
Bold/Italic/Underline/Culoare) rezistă și pe conținut mult mai complex decât
reproducerile minimale de mai sus — nu doar pe cazul exact raportat.

### Descoperire minoră — span-uri adiacente identice nu se contopesc

După un toggle off care taie un span colorat exact la mijloc (limita
selecției cade în interiorul textului, nu la marginea unui nod), rezultă
**două `<span>` frați cu exact același `style`** în loc de unul singur —
ex. `<span style="color: rgb(220, 38, 38);">...co</span><span
style="color: rgb(220, 38, 38);">ntului</span>` (text-ul era, înainte de
comutare, un singur span). Vizual identic, deci nu-i vizibil utilizatorului,
dar `toggleFormat()` nu are un pas de "merge adjacent siblings with
identical style", spre deosebire de curățarea span-urilor goale pe care o
are deja. Pe un mesaj editat des, DOM-ul crește nejustificat de mult.
**Severitate:** cosmetică/eficiență, nu corupe nimic. Neremediat — nu face
parte din bug-ul raportat, semnalat aici ca să nu se piardă.

### Observație — `Ctrl+Z` (undo nativ) nu mai are efect pe aceste comenzi

`toggleFormat()` scrie direct în DOM prin `Range.extractContents()`/
`insertNode()`, fără `execCommand`, deci browserul nu mai înregistrează
schimbarea în stiva lui de undo intern pentru `contenteditable`. Testat:
Italic aplicat prin clic pe buton, apoi `Ctrl+Z` → niciun efect, DOM identic
înainte/după. **Asta nu-i o regresie nouă** — `applySize()` (mărimea
textului) are exact aceeași limitare dinainte de sesiunea asta, fix-ul din
secțiunea 8 doar o extinde acum și la Bold/Italic/Underline/Culoare, care
sunt folosite mult mai des. Nemenționat până acum în raport. Nu exista un
undo custom la nivel de aplicație (`grep` pentru `undo`/`history` în
`editor.component.ts` nu găsește nimic) — deci comportamentul de dinainte de
tot fix-ul din secțiunea 8 venea 100% din `execCommand`, iar acum comenzile
astea patru au ieșit din el.

### Reconfirmare bug 7 (`Ctrl+B` ocolește `exec()`) — direct în Chromium, cu un efect secundar nou

Bug-ul 7 era documentat doar din citirea codului (cauza „clară din cod, fără
ambiguitate", dar niciodată reprodusă live — mediul de testare era Chromium,
unde raportul zicea că scurtătura „merge din întâmplare"). Testat acum
direct: focus pe canvas, selecție reală de text ("text simplu"), apoi
`Ctrl+B` prin tastatură reală (nu clic):

```html
<!-- înainte -->
Salut, ​<span data-var="nume" ...>{{nume}}</span>​ text simplu de test.
<!-- după Ctrl+B -->
Salut, ​<span data-var="nume" ...>{{nume}}</span>​ <b>text simplu</b> de test.
```

Confirmă cauza documentată în secțiunea 7: fără handler pe `(keydown)`,
`Ctrl+B` ajunge direct la comportamentul implicit al Chromium pentru
`contenteditable`, care produce un tag `<b>` brut — niciodată produs de
codul acestui editor (care scrie mereu `<span style="font-weight: bold;">`).
Asta confirmă și corecția din capul documentului: fix-ul bug-ului 7 (handler
`onKeyDown` care interceptează `Ctrl+B/I/U` și cheamă `exec()`) trăiește în
același `onKeyDown()` cu fix-ul bug-ului 4, deci a fost scos din cod odată cu
el, prin același `git stash` — statusul vechi ("bug 7 rezolvat") era greșit.

**Efect secundar nou, nedocumentat până acum:** pentru că tag-ul `<b>`
rezultat nu e niciodată recunoscut de `hasFormat()`/`isFormatted()`
(verifică doar `el.style.fontWeight`, nu numele tag-ului), un clic ulterior
pe butonul Bold din bară pe același text **nu-l recunoaște ca fiind deja
bold** — `allFormatted()` întoarce `false`, deci `toggleFormat()` îl
**împachetează a doua oară** în loc să-l scoată:

```html
<!-- după Ctrl+B, apoi clic pe butonul Bold din bară pe aceeași selecție -->
<span style="font-weight: bold;"><b>Cu stimă</b></span><b></b>,
```

Rezultat: dublă-împachetare (`<span style="font-weight:bold"><b>...</b></span>`)
plus un `<b></b>` gol, orfan, rămas din tăierea nodului original de
`extractContents()`. Text-ul rămâne corect vizual (tot bold, dublu chiar),
dar starea internă a formatării e coruptă — un toggle ulterior nu mai poate
scoate bold-ul din regiunea asta curat, pentru că jumătate din informație
(`<b>`) e invizibilă logicii de detectare. Reprodus determinist, de două ori.

**Nu a fost reparat** — fix-ul corect (handler `onKeyDown`) există deja,
verificat, dar e ținut deliberat în stash alături de fix-ul bug-ului 4;
aplicarea lui e o decizie a utilizatorului, nu ceva de rezolvat pe loc aici.
Semnalat ca să fie clar că, atât timp cât rămâne în stash, problema nu-i doar
„scurtăturile nu merg" (cum suna raportul inițial) — e și „scurtăturile merg,
dar corup silențios starea de formatare pentru clicurile ulterioare din
bară", ceva mai grav decât credeam.
