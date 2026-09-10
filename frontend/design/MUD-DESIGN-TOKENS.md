# MUD — Moldova UI Design System · Analiză + Design Tokens

> Sursă: <https://github.com/egov-moldova/design-system>
> Commit analizat: `36f0786c220f8660fed2343e8c651bad26239ba2` (branch implicit, HEAD)
> Pachet npm: `@egov-moldova/mud` v1.0.6 — „MUD Design System — Stencil web components”
> Document generat: 2026-09-06

Acest fișier conține (1) analiza sistemului de design și (2) **copia exactă a design tokens** din repo — tabele derivate + sursa JSON verbatim a fiecărui fișier de tokens din `tokens/core/` și `tokens/core.dark/`.

---

## 1. Ce este MUD

MUD (Moldova UI Design System) este sistemul de design oficial al serviciilor digitale guvernamentale ale Republicii Moldova, dezvoltat de Agenția de Guvernare Electronică (AGE). Problema pe care o rezolvă: fiecare platformă guvernamentală și-a construit propriul UI, deci cetățeanul reînvață tiparele de interacțiune la fiecare serviciu, echipele dublează munca, iar nivelul de accesibilitate variază.

**Stack tehnic**

| Aspect | Alegere |
| --- | --- |
| Componente | Stencil → Web Components cu Shadow DOM |
| Distribuție | `@egov-moldova/mud` (core) + `@egov-moldova/mud-react`, `web-components` (workspaces Yarn) |
| Tokens | JSON în format DTCG (W3C Design Tokens Community Group) |
| Build tokens | Style Dictionary v4.4+ (`usesDtcg: true`), orchestrat prin Wireit |
| Stilizare | CSS custom properties, PostCSS |
| Testare | Playwright, Vitest; Storybook pentru documentație |
| Font | Onest (variable font, livrat în `assets/font/Onest/`) |
| Runtime | Framework-agnostic (React 19+, Vue 3, Angular 14+, Svelte, HTML simplu) |

Consecința arhitecturală importantă: pentru că fiecare componentă este izolată în Shadow DOM, **singurul canal de stilizare care traversează granița** îl reprezintă variabilele CSS. Tokens nu sunt o convenție opțională aici, ci API-ul public de theming al sistemului.

**Structura repo-ului**

```text
src/components/     40 componente „core" (mud-*)
src/legacy/         componente vechi (cor-*), în curs de retragere
tokens/             sursa de adevăr pentru valorile vizuale
react/              adaptor React
web-components/     loader vanilla
.storybook/         documentație vie (inclusiv pagini de tokens)
scripts/            sync Tokenhaus, validare, lint, audit de contrast
.specs/             specificațiile arhitecturale
```

---

## 2. Arhitectura de tokens

### 2.1 Ierarhia pe 3 niveluri

```text
CSS de componentă  →  Component tokens  →  Semantic tokens  →  Palette primitives
                      (per-componentă)     (reutilizabile)     (culori brute, spații)
```

| Nivel | Tipar de cale | Exemplu | Variabilă CSS |
| --- | --- | --- | --- |
| **Palette** | `palette.{family}.{shade}` | `palette.blue-sky.600` | `--palette-blue-sky-600` |
| **Semantic** | `{category}.{type}.{role}.{variant}` | `color.background.brand.default` | `--color-background-brand-default` |
| **Component** | `{component}.{element}.{property}.{scale/state}` | `button.primary.background.hover` | `--button-primary-background-hover` |

Reguli impuse de repo (`.specs/TOKEN-ARCHITECTURE.md`, `tokens/AGENTS.md`):

1. CSS-ul de componentă folosește **doar** tokens de componentă (`var(--button-*)`) sau semantice (`var(--color-*)`).
2. Tokens de componentă referă **doar** tokens semantice via `{color.*}` — niciodată `{palette.*}`.
3. Tokens semantice referă primitive din paletă.
4. Nivelurile nu se sar „de jos în sus”: `var(--palette-*)` este interzis în CSS de componentă, inclusiv ca fallback.
5. Hex/px/rgb/hsl brut în CSS de componentă este interzis.

Motivul regulii 2 este dark mode: tema întunecată suprascrie exclusiv stratul semantic (`tokens/core.dark/color.tokens.json`), deci orice token de componentă legat direct de paletă rămâne „înghețat” pe valoarea light.

### 2.2 Formatul DTCG

Toate fișierele folosesc `$value` / `$type` (nu `value`/`type` legacy):

```json
{
  "color": {
    "background": {
      "base": {
        "default": { "$value": "{palette.white.1000}", "$type": "color" }
      }
    }
  },
  "spacing": {
    "12": { "$value": "12px", "$type": "dimension" }
  }
}
```

- Dimensiunile sunt string-uri cu unitate explicită: `"12px"`, `"0px"`, `"9999px"` — niciodată numere goale.
- `fontWeight` este numeric (`400`, `600`), nu string.
- `attributes.category` este obsolet — Style Dictionary v4 deduce CTI din calea tokenului.

### 2.3 Convenția de denumire

Tiparul canonic pentru variabile CSS de componentă:

```text
--{component}-{element}-{property}-{scale/state}
```

**Segmentul de scală/stare este întotdeauna ultimul.**

| ✅ Corect | ❌ Greșit |
| --- | --- |
| `--label-font-size-md` | `--label-md-font-size` |
| `--input-border-color-focus` | `--input-focus-border-color` |
| `--button-primary-background-hover` | `--button-primary-hover-background` |

În JSON, starea stă **sub** proprietate, nu deasupra ei; cheile compuse sunt camelCase (`fontSize`, `borderRadius`, `paddingInline`) și Style Dictionary le convertește în kebab-case. Cheia rădăcină este numele componentei (`{ "button": … }`) — un wrapper `{ "components": … }` ar genera prefixul parazit `--components-button-*` și ar rupe tăcut toate referințele.

Nomenclatura semantică urmează schema în 4 părți din Figma Foundations: **category · type · role · variant**, unde `type ∈ {background, text, border, icon}` și `role ∈ {base, brand, danger, positive, warning, info, disabled, …}`.

### 2.4 Pipeline de build

Configurațiile Style Dictionary (`tokens/core/style-dictionary.config.json`, `.prod.config.json`, plus perechea din `tokens/core.dark/`) folosesc transformările `attribute/cti`, `name/kebab`, `color/hex`.

| Configurație | Sursă | Ieșire |
| --- | --- | --- |
| `core` (dev) | `tokens/core/**/*.tokens.json` | `tokens/generated/core.tokens.css` |
| `core` (prod) | idem | `dist/mud/tokens/core.tokens.css` + JSON pentru Storybook și Tailwind |
| `core.dark` | `tokens/core/**` + `tokens/core.dark/**` | `core.dark.tokens.css`, selector `:root[data-theme="dark"]` |

Comenzi: `yarn tokens.build`, `yarn tokens.build.prod`, `yarn tokens.build.age`, `yarn tokens.watch`, `yarn tokens.audit`. După editarea tokens **nu** este nevoie de rebuild Stencil — CSS-ul de tokens este autonom, încărcat la runtime prin `<link>`.

Comutarea temei se face cu `<html data-theme="dark">` (respectiv `data-theme="age"` pentru tema AGE).

### 2.5 Sincronizarea cu Figma (Tokenhaus)

`scripts/sync-tokens-from-tokenhaus.mjs` transformă exportul Figma (`tokens-tokenhaus.json`, ~99 KB) în fișierele DTCG ale repo-ului, în două moduri:

- **staging** (implicit) — scrie în `tokens/figma-export/` pentru diff înainte de promovare;
- **`--apply`** (distructiv) — suprascrie `tokens/core/{palette,color,font,sizes}.tokens.json` și `tokens/core.dark/color.tokens.json` și șterge fișierele orfane vechi (`space`, `radius`, `border`, `lineHeight`, `letterSpacing`, `shadow`).

Flag-uri utile: `--dry-run` (previzualizare, mai ales combinat cu `--apply`), `--strict` (eșuează la secțiuni sărite / moduri lipsă / referințe nerezolvate), `--report <fișier>` (manifest mașină-lizibil).

---

## 3. Constatări din analiză

**Ce funcționează bine**

- **Integritate referențială completă**: zero referințe `{...}` nerezolvate în `tokens/core/` (2.833 tokens verificați).
- **Paritate light/dark perfectă la nivel semantic**: toate cele 89 de tokens semantice de culoare din `tokens/core/color.tokens.json` au corespondent 1:1 în `tokens/core.dark/color.tokens.json`. Tema întunecată nu are tokens orfani și nu ratează niciunul.
- **Paleta este disciplinată**: 12 familii (`blue-sky`, `lavender`, `purple`, `magenta`, `forest-green`, `red`, `apricot`, `green`, `gray`, `black`, `white`, `alpha`) cu trepte 100–900 (plus 50/150/250/350 acolo unde a fost nevoie de granularitate) și un set de alfa dedicat pentru overlay-uri.
- **Scale numerice, nu inventate**: `spacing`, `fontSize`, `lineHeight`, `borderRadius` folosesc valoarea în px drept cheie (`spacing.12` → `12px`), ceea ce elimină ambiguitatea „ce înseamnă `md`”.

**Riscuri și abateri de la propriile reguli**

1. **31 de tokens de componentă referă direct `{palette.*}`** — încălcare a regulii 2 din §2.1. Aproape toate sunt inele de focus (`input.default.focusRing → {palette.blue-sky.200}`, `menu.focusRing.color → {palette.blue-sky.500}` etc.). Consecință concretă: în tema întunecată aceste inele de focus păstrează valorile din light, pentru că dark mode suprascrie doar stratul semantic. Lista completă este în §4.3. Remediul natural: extinderea `tokens/core/focusRing.tokens.json` cu roluri semantice (`focusRing.color.danger`, `.warning`, `.positive`) și rescrierea celor 31 de referințe.
2. **223 de valori brute în tokens de componentă** (hex/px/ms fără referință) — cele mai multe în `button` (23), `modal` (15), `chip` (14), `footer` (13), `pagination` (13). Unele sunt legitime (durate de tranziție, `maxWidth: 400px`), altele sunt dimensiuni care ar trebui să intre în scala `spacing`/`sizes` (`52px`, `72px`).
3. **Acoperire dark incompletă la nivel de componentă**: `tokens/core.dark/` conține doar `color.tokens.json` și un singur override de componentă (`components/badge.tokens.json`, 1 token). Documentația internă marchează dark mode drept „DEFERRED”. Practic tema funcționează prin moștenire semantică, ceea ce este corect ca strategie, dar orice componentă cu valori legate de paletă (punctul 1) sau brute (punctul 2) va arăta greșit.
4. **Documentație desincronizată**: `tokens/_agents/semantic-tokens.md` conține un tabel de mapare palette→semantic care folosește nume inexistente în repo azi (`--palette-ui-gray-9`, `--color-neutral-text-weak`). Nomenclatura reală este `--palette-gray-400` → `--color-text-base-*`. La fel, `.specs/TOKEN-ARCHITECTURE.md` și `token-structure.md` listează fișiere care nu există (`spacing.tokens.json`, `fontSize.tokens.json`, `radius.tokens.json`, `lineHeight.tokens.json`) — în realitate scalele sunt consolidate în `sizes.tokens.json` și `font.tokens.json`. Cine urmează literal documentația va crea fișiere duplicate.
5. **Tema AGE este documentată dar absentă**: `tokens/age/` apare în ierarhia din specificații și în comanda `yarn tokens.build.age`, dar directorul nu există în repo la acest commit.
6. **`letterSpacing` este declarat gol** în `font.tokens.json` (`"letterSpacing": {}`) — nu generează nicio variabilă CSS.
7. **Cheie rădăcină inconsistentă la componentele cu nume compus**: 7 din 43 de fișiere folosesc kebab-case la rădăcină (`button-group`, `cookie-banner`, `progress-tracker`, `search-input-circular`, `search-input-rectangular`, `segmented-control`, `service-button`), restul folosesc camelCase (`dateInput`, `fileInput`, `inputChip`, `numericInput`, `phoneInput`, `selectInput`, `infoBox`, `inlineMessage`). Variabila CSS generată este identică în ambele cazuri, deci nu e un bug — dar rupe căutarea după calea JSON și contrazice regula „camelCase în JSON” din `tokens/AGENTS.md`.
8. **Datorie tehnică legacy**: `tokens/legacy/` mai conține 1.701 tokens pentru 52 de componente `cor-*`, cu o nomenclatură diferită. Nu sunt incluse în copia de mai jos (scop: core), dar rămân în build-ul repo-ului.

## 4. Inventar și statistici

### 4.1 Tokens per fișier

| Fișier | Tokens |
| --- | ---: |
| `tokens/core/color.tokens.json` | 89 |
| `tokens/core/effects.tokens.json` | 5 |
| `tokens/core/focusRing.tokens.json` | 4 |
| `tokens/core/font.tokens.json` | 32 |
| `tokens/core/palette.tokens.json` | 106 |
| `tokens/core/screen.tokens.json` | 16 |
| `tokens/core/sizes.tokens.json` | 31 |
| **subtotal fundamente** | **283** |
| `tokens/core/components/accordion.tokens.json` | 46 |
| `tokens/core/components/avatar.tokens.json` | 39 |
| `tokens/core/components/badge.tokens.json` | 43 |
| `tokens/core/components/breadcrumb.tokens.json` | 38 |
| `tokens/core/components/button-group.tokens.json` | 2 |
| `tokens/core/components/button.tokens.json` | 150 |
| `tokens/core/components/checkbox.tokens.json` | 50 |
| `tokens/core/components/chip.tokens.json` | 62 |
| `tokens/core/components/cookie-banner.tokens.json` | 75 |
| `tokens/core/components/date-input.tokens.json` | 56 |
| `tokens/core/components/date-picker.tokens.json` | 70 |
| `tokens/core/components/file-input.tokens.json` | 86 |
| `tokens/core/components/file-item.tokens.json` | 42 |
| `tokens/core/components/footer.tokens.json` | 102 |
| `tokens/core/components/header.tokens.json` | 112 |
| `tokens/core/components/info-box.tokens.json` | 54 |
| `tokens/core/components/inline-message.tokens.json` | 18 |
| `tokens/core/components/input-chip.tokens.json` | 86 |
| `tokens/core/components/input.tokens.json` | 79 |
| `tokens/core/components/link.tokens.json` | 45 |
| `tokens/core/components/menu.tokens.json` | 43 |
| `tokens/core/components/modal.tokens.json` | 59 |
| `tokens/core/components/notification.tokens.json` | 45 |
| `tokens/core/components/numeric-input.tokens.json` | 103 |
| `tokens/core/components/pagination.tokens.json` | 81 |
| `tokens/core/components/phone-input.tokens.json` | 142 |
| `tokens/core/components/progress-tracker.tokens.json` | 63 |
| `tokens/core/components/radio.tokens.json` | 42 |
| `tokens/core/components/receipt.tokens.json` | 64 |
| `tokens/core/components/search-input-circular.tokens.json` | 81 |
| `tokens/core/components/search-input-rectangular.tokens.json` | 81 |
| `tokens/core/components/segmented-control.tokens.json` | 38 |
| `tokens/core/components/select-input.tokens.json` | 93 |
| `tokens/core/components/separator.tokens.json` | 14 |
| `tokens/core/components/service-button.tokens.json` | 24 |
| `tokens/core/components/sidebar.tokens.json` | 52 |
| `tokens/core/components/spinner.tokens.json` | 14 |
| `tokens/core/components/switch.tokens.json` | 29 |
| `tokens/core/components/table.tokens.json` | 49 |
| `tokens/core/components/tabs.tokens.json` | 61 |
| `tokens/core/components/tag.tokens.json` | 59 |
| `tokens/core/components/textarea.tokens.json` | 18 |
| `tokens/core/components/tooltip.tokens.json` | 40 |
| **subtotal componente** | **2550** |
| `tokens/core.dark/color.tokens.json` | 89 |
| `tokens/core.dark/components/badge.tokens.json` | 1 |
| **subtotal dark** | **90** |
| **TOTAL (core + core.dark)** | **2923** |

### 4.2 Distribuția pe `$type`

| `$type` | Tokens |
| --- | ---: |
| `dimension` | 1327 |
| `color` | 1198 |
| `fontWeight` | 118 |
| `fontFamily` | 92 |
| `duration` | 35 |
| `cubicBezier` | 34 |
| `string` | 19 |
| `number` | 7 |
| `shadow` | 3 |

### 4.3 Verificări de consistență

- Referințe `{...}` nerezolvate în `tokens/core/`: **0**.
- Tokens de componentă care referă direct `{palette.*}` (încalcă regula celor 3 niveluri): **31**.
- Valori brute (hex/px, fără referință) în tokens de componentă: **223**.
- Semantice light: **89**; suprascrieri dark: **89** — acoperire 1:1.

#### Cele 31 de referințe directe la palette din tokens de componentă

| Fișier | Token | Valoare |
| --- | --- | --- |
| `date-input.tokens.json` | `dateInput.default.focusRing` | `{palette.blue-sky.200}` |
| `date-input.tokens.json` | `dateInput.destructive.focusRing` | `{palette.red.200}` |
| `file-input.tokens.json` | `fileInput.focusRing.default` | `{palette.blue-sky.200}` |
| `file-input.tokens.json` | `fileInput.focusRing.invalid` | `{palette.red.200}` |
| `file-item.tokens.json` | `fileItem.remove.focusRing` | `{palette.blue-sky.200}` |
| `input-chip.tokens.json` | `inputChip.chip.focusRingColor` | `{palette.blue-sky.200}` |
| `input-chip.tokens.json` | `inputChip.default.focusRing` | `{palette.blue-sky.200}` |
| `input-chip.tokens.json` | `inputChip.destructive.focusRing` | `{palette.red.200}` |
| `input.tokens.json` | `input.default.focusRing` | `{palette.blue-sky.200}` |
| `input.tokens.json` | `input.warning.focusRing` | `{palette.apricot.200}` |
| `input.tokens.json` | `input.destructive.focusRing` | `{palette.red.200}` |
| `input.tokens.json` | `input.success.focusRing` | `{palette.green.200}` |
| `menu.tokens.json` | `menu.focusRing.color` | `{palette.blue-sky.500}` |
| `numeric-input.tokens.json` | `numericInput.default.focusRing` | `{palette.blue-sky.200}` |
| `numeric-input.tokens.json` | `numericInput.destructive.focusRing` | `{palette.red.200}` |
| `numeric-input.tokens.json` | `numericInput.success.focusRing` | `{palette.green.200}` |
| `phone-input.tokens.json` | `phoneInput.default.focusRing` | `{palette.blue-sky.200}` |
| `phone-input.tokens.json` | `phoneInput.warning.focusRing` | `{palette.apricot.200}` |
| `phone-input.tokens.json` | `phoneInput.destructive.focusRing` | `{palette.red.200}` |
| `phone-input.tokens.json` | `phoneInput.success.focusRing` | `{palette.green.200}` |
| `receipt.tokens.json` | `receipt.qr.background` | `{palette.white.1000}` |
| `receipt.tokens.json` | `receipt.qr.moduleColor` | `{palette.black.1000}` |
| `search-input-circular.tokens.json` | `search-input-circular.default.focusRing` | `{palette.blue-sky.200}` |
| `search-input-circular.tokens.json` | `search-input-circular.destructive.focusRing` | `{palette.red.200}` |
| `search-input-rectangular.tokens.json` | `search-input-rectangular.default.focusRing` | `{palette.blue-sky.200}` |
| `search-input-rectangular.tokens.json` | `search-input-rectangular.destructive.focusRing` | `{palette.red.200}` |
| `select-input.tokens.json` | `selectInput.default.focusRing` | `{palette.blue-sky.200}` |
| `select-input.tokens.json` | `selectInput.destructive.focusRing` | `{palette.red.200}` |
| `sidebar.tokens.json` | `sidebar.focusRing.color` | `{palette.blue-sky.500}` |
| `switch.tokens.json` | `switch.background.off` | `{palette.gray.400}` |
| `switch.tokens.json` | `switch.background.offHover` | `{palette.gray.500}` |

#### Valori brute pe fișier (top 15)

| Fișier | Valori brute |
| --- | ---: |
| `button.tokens.json` | 23 |
| `modal.tokens.json` | 15 |
| `chip.tokens.json` | 14 |
| `footer.tokens.json` | 13 |
| `pagination.tokens.json` | 13 |
| `header.tokens.json` | 12 |
| `cookie-banner.tokens.json` | 11 |
| `tooltip.tokens.json` | 10 |
| `avatar.tokens.json` | 8 |
| `phone-input.tokens.json` | 7 |
| `numeric-input.tokens.json` | 6 |
| `select-input.tokens.json` | 6 |
| `date-picker.tokens.json` | 5 |
| `notification.tokens.json` | 5 |
| `radio.tokens.json` | 5 |

---

## 5. Tokens — copie exactă din repo

Toate valorile de mai jos sunt copiate verbatim din commit-ul `36f0786c220f8660fed2343e8c651bad26239ba2`. Coloana _Variabilă CSS_ este numele generat de Style Dictionary prin transformarea `name/kebab` (fără prefix, conform configurației din repo).

### 5.1 Fundamente — `tokens/core/`

#### `tokens/core/palette.tokens.json`

_106 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `palette.blue-sky.100` | `#e8f0fb` | `color` | `--palette-blue-sky-100` |
| `palette.blue-sky.150` | `#d6e5f8` | `color` | `--palette-blue-sky-150` |
| `palette.blue-sky.200` | `#ccdef6` | `color` | `--palette-blue-sky-200` |
| `palette.blue-sky.300` | `#99bced` | `color` | `--palette-blue-sky-300` |
| `palette.blue-sky.400` | `#669be4` | `color` | `--palette-blue-sky-400` |
| `palette.blue-sky.500` | `#3379db` | `color` | `--palette-blue-sky-500` |
| `palette.blue-sky.600` | `#0058d2` | `color` | `--palette-blue-sky-600` |
| `palette.blue-sky.700` | `#0046a8` | `color` | `--palette-blue-sky-700` |
| `palette.blue-sky.800` | `#00357e` | `color` | `--palette-blue-sky-800` |
| `palette.blue-sky.900` | `#00295a` | `color` | `--palette-blue-sky-900` |
| `palette.lavender.100` | `#efeafc` | `color` | `--palette-lavender-100` |
| `palette.lavender.200` | `#ddd2fa` | `color` | `--palette-lavender-200` |
| `palette.lavender.300` | `#bba5f5` | `color` | `--palette-lavender-300` |
| `palette.lavender.400` | `#9a79ef` | `color` | `--palette-lavender-400` |
| `palette.lavender.500` | `#784cea` | `color` | `--palette-lavender-500` |
| `palette.lavender.600` | `#561fe5` | `color` | `--palette-lavender-600` |
| `palette.lavender.700` | `#4519b7` | `color` | `--palette-lavender-700` |
| `palette.lavender.800` | `#341389` | `color` | `--palette-lavender-800` |
| `palette.lavender.900` | `#240c66` | `color` | `--palette-lavender-900` |
| `palette.purple.100` | `#efeafc` | `color` | `--palette-purple-100` |
| `palette.purple.200` | `#ddd2fa` | `color` | `--palette-purple-200` |
| `palette.purple.300` | `#cbaffd` | `color` | `--palette-purple-300` |
| `palette.purple.400` | `#b287fb` | `color` | `--palette-purple-400` |
| `palette.purple.500` | `#985ffa` | `color` | `--palette-purple-500` |
| `palette.purple.600` | `#7e37f9` | `color` | `--palette-purple-600` |
| `palette.purple.700` | `#652cc7` | `color` | `--palette-purple-700` |
| `palette.purple.800` | `#4c2195` | `color` | `--palette-purple-800` |
| `palette.purple.900` | `#36166a` | `color` | `--palette-purple-900` |
| `palette.magenta.100` | `#f7eafa` | `color` | `--palette-magenta-100` |
| `palette.magenta.200` | `#eed1f5` | `color` | `--palette-magenta-200` |
| `palette.magenta.300` | `#dda3eb` | `color` | `--palette-magenta-300` |
| `palette.magenta.400` | `#cc74e2` | `color` | `--palette-magenta-400` |
| `palette.magenta.500` | `#bb46d8` | `color` | `--palette-magenta-500` |
| `palette.magenta.600` | `#aa18ce` | `color` | `--palette-magenta-600` |
| `palette.magenta.700` | `#8813a5` | `color` | `--palette-magenta-700` |
| `palette.magenta.800` | `#660e7c` | `color` | `--palette-magenta-800` |
| `palette.magenta.900` | `#4a095a` | `color` | `--palette-magenta-900` |
| `palette.forest-green.100` | `#e8f1f1` | `color` | `--palette-forest-green-100` |
| `palette.forest-green.150` | `#d6e6e7` | `color` | `--palette-forest-green-150` |
| `palette.forest-green.200` | `#cce0e1` | `color` | `--palette-forest-green-200` |
| `palette.forest-green.300` | `#99c1c3` | `color` | `--palette-forest-green-300` |
| `palette.forest-green.400` | `#66a1a5` | `color` | `--palette-forest-green-400` |
| `palette.forest-green.500` | `#338287` | `color` | `--palette-forest-green-500` |
| `palette.forest-green.600` | `#006369` | `color` | `--palette-forest-green-600` |
| `palette.forest-green.700` | `#004f54` | `color` | `--palette-forest-green-700` |
| `palette.forest-green.800` | `#003b3f` | `color` | `--palette-forest-green-800` |
| `palette.forest-green.900` | `#00292c` | `color` | `--palette-forest-green-900` |
| `palette.red.50` | `#feefee` | `color` | `--palette-red-50` |
| `palette.red.100` | `#fee4e2` | `color` | `--palette-red-100` |
| `palette.red.200` | `#fecdc9` | `color` | `--palette-red-200` |
| `palette.red.300` | `#fda19b` | `color` | `--palette-red-300` |
| `palette.red.400` | `#f97066` | `color` | `--palette-red-400` |
| `palette.red.500` | `#f04438` | `color` | `--palette-red-500` |
| `palette.red.600` | `#d92d20` | `color` | `--palette-red-600` |
| `palette.red.700` | `#b32318` | `color` | `--palette-red-700` |
| `palette.red.800` | `#912018` | `color` | `--palette-red-800` |
| `palette.red.900` | `#7a271a` | `color` | `--palette-red-900` |
| `palette.apricot.50` | `#fef5dd` | `color` | `--palette-apricot-50` |
| `palette.apricot.100` | `#feefc6` | `color` | `--palette-apricot-100` |
| `palette.apricot.200` | `#fedf89` | `color` | `--palette-apricot-200` |
| `palette.apricot.300` | `#fec84b` | `color` | `--palette-apricot-300` |
| `palette.apricot.400` | `#fdb022` | `color` | `--palette-apricot-400` |
| `palette.apricot.500` | `#f79009` | `color` | `--palette-apricot-500` |
| `palette.apricot.600` | `#dc6803` | `color` | `--palette-apricot-600` |
| `palette.apricot.700` | `#b54708` | `color` | `--palette-apricot-700` |
| `palette.apricot.800` | `#93370d` | `color` | `--palette-apricot-800` |
| `palette.apricot.900` | `#792e0d` | `color` | `--palette-apricot-900` |
| `palette.green.50` | `#ebf7f1` | `color` | `--palette-green-50` |
| `palette.green.100` | `#e6f5ee` | `color` | `--palette-green-100` |
| `palette.green.200` | `#cdeadd` | `color` | `--palette-green-200` |
| `palette.green.300` | `#9ad6bb` | `color` | `--palette-green-300` |
| `palette.green.400` | `#68c199` | `color` | `--palette-green-400` |
| `palette.green.500` | `#35ad77` | `color` | `--palette-green-500` |
| `palette.green.600` | `#039855` | `color` | `--palette-green-600` |
| `palette.green.700` | `#027948` | `color` | `--palette-green-700` |
| `palette.green.800` | `#05603a` | `color` | `--palette-green-800` |
| `palette.green.900` | `#054f31` | `color` | `--palette-green-900` |
| `palette.gray.50` | `#f7f7f7` | `color` | `--palette-gray-50` |
| `palette.gray.100` | `#f5f5f5` | `color` | `--palette-gray-100` |
| `palette.gray.200` | `#f1f1f1` | `color` | `--palette-gray-200` |
| `palette.gray.250` | `#d9d9d9` | `color` | `--palette-gray-250` |
| `palette.gray.300` | `#b2b2b2` | `color` | `--palette-gray-300` |
| `palette.gray.350` | `#8a8a8a` | `color` | `--palette-gray-350` |
| `palette.gray.400` | `#757575` | `color` | `--palette-gray-400` |
| `palette.gray.500` | `#616161` | `color` | `--palette-gray-500` |
| `palette.gray.600` | `#444444` | `color` | `--palette-gray-600` |
| `palette.gray.700` | `#383838` | `color` | `--palette-gray-700` |
| `palette.gray.800` | `#2c2c2c` | `color` | `--palette-gray-800` |
| `palette.gray.900` | `#1e1e1e` | `color` | `--palette-gray-900` |
| `palette.black.1000` | `#121212` | `color` | `--palette-black-1000` |
| `palette.white.1000` | `#ffffff` | `color` | `--palette-white-1000` |
| `palette.alpha.black.100-alpha` | `#1212120d` | `color` | `--palette-alpha-black-100-alpha` |
| `palette.alpha.black.200-alpha` | `#1212121a` | `color` | `--palette-alpha-black-200-alpha` |
| `palette.alpha.black.300-alpha` | `#12121233` | `color` | `--palette-alpha-black-300-alpha` |
| `palette.alpha.black.400-alpha` | `#12121266` | `color` | `--palette-alpha-black-400-alpha` |
| `palette.alpha.black.500-alpha` | `#12121299` | `color` | `--palette-alpha-black-500-alpha` |
| `palette.alpha.white.100-alpha` | `#ffffff0d` | `color` | `--palette-alpha-white-100-alpha` |
| `palette.alpha.white.200-alpha` | `#ffffff1a` | `color` | `--palette-alpha-white-200-alpha` |
| `palette.alpha.white.300-alpha` | `#ffffff33` | `color` | `--palette-alpha-white-300-alpha` |
| `palette.alpha.white.400-alpha` | `#ffffff66` | `color` | `--palette-alpha-white-400-alpha` |
| `palette.alpha.white.500-alpha` | `#ffffff99` | `color` | `--palette-alpha-white-500-alpha` |
| `palette.alpha.gray.alpha-100` | `#44444408` | `color` | `--palette-alpha-gray-alpha-100` |
| `palette.alpha.gray.alpha-200` | `#4444441a` | `color` | `--palette-alpha-gray-alpha-200` |
| `palette.alpha.gray.alpha-300` | `#44444433` | `color` | `--palette-alpha-gray-alpha-300` |
| `palette.alpha.gray.alpha-400` | `#44444466` | `color` | `--palette-alpha-gray-alpha-400` |
| `palette.alpha.gray.alpha-500` | `#44444499` | `color` | `--palette-alpha-gray-alpha-500` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/palette.tokens.json</code></summary>

```json
{
  "palette": {
    "blue-sky": {
      "100": {
        "$value": "#e8f0fb",
        "$type": "color"
      },
      "150": {
        "$value": "#d6e5f8",
        "$type": "color"
      },
      "200": {
        "$value": "#ccdef6",
        "$type": "color"
      },
      "300": {
        "$value": "#99bced",
        "$type": "color"
      },
      "400": {
        "$value": "#669be4",
        "$type": "color"
      },
      "500": {
        "$value": "#3379db",
        "$type": "color"
      },
      "600": {
        "$value": "#0058d2",
        "$type": "color"
      },
      "700": {
        "$value": "#0046a8",
        "$type": "color"
      },
      "800": {
        "$value": "#00357e",
        "$type": "color"
      },
      "900": {
        "$value": "#00295a",
        "$type": "color"
      }
    },
    "lavender": {
      "100": {
        "$value": "#efeafc",
        "$type": "color"
      },
      "200": {
        "$value": "#ddd2fa",
        "$type": "color"
      },
      "300": {
        "$value": "#bba5f5",
        "$type": "color"
      },
      "400": {
        "$value": "#9a79ef",
        "$type": "color"
      },
      "500": {
        "$value": "#784cea",
        "$type": "color"
      },
      "600": {
        "$value": "#561fe5",
        "$type": "color"
      },
      "700": {
        "$value": "#4519b7",
        "$type": "color"
      },
      "800": {
        "$value": "#341389",
        "$type": "color"
      },
      "900": {
        "$value": "#240c66",
        "$type": "color"
      }
    },
    "purple": {
      "100": {
        "$value": "#efeafc",
        "$type": "color"
      },
      "200": {
        "$value": "#ddd2fa",
        "$type": "color"
      },
      "300": {
        "$value": "#cbaffd",
        "$type": "color"
      },
      "400": {
        "$value": "#b287fb",
        "$type": "color"
      },
      "500": {
        "$value": "#985ffa",
        "$type": "color"
      },
      "600": {
        "$value": "#7e37f9",
        "$type": "color"
      },
      "700": {
        "$value": "#652cc7",
        "$type": "color"
      },
      "800": {
        "$value": "#4c2195",
        "$type": "color"
      },
      "900": {
        "$value": "#36166a",
        "$type": "color"
      }
    },
    "magenta": {
      "100": {
        "$value": "#f7eafa",
        "$type": "color"
      },
      "200": {
        "$value": "#eed1f5",
        "$type": "color"
      },
      "300": {
        "$value": "#dda3eb",
        "$type": "color"
      },
      "400": {
        "$value": "#cc74e2",
        "$type": "color"
      },
      "500": {
        "$value": "#bb46d8",
        "$type": "color"
      },
      "600": {
        "$value": "#aa18ce",
        "$type": "color"
      },
      "700": {
        "$value": "#8813a5",
        "$type": "color"
      },
      "800": {
        "$value": "#660e7c",
        "$type": "color"
      },
      "900": {
        "$value": "#4a095a",
        "$type": "color"
      }
    },
    "forest-green": {
      "100": {
        "$value": "#e8f1f1",
        "$type": "color"
      },
      "150": {
        "$value": "#d6e6e7",
        "$type": "color"
      },
      "200": {
        "$value": "#cce0e1",
        "$type": "color"
      },
      "300": {
        "$value": "#99c1c3",
        "$type": "color"
      },
      "400": {
        "$value": "#66a1a5",
        "$type": "color"
      },
      "500": {
        "$value": "#338287",
        "$type": "color"
      },
      "600": {
        "$value": "#006369",
        "$type": "color"
      },
      "700": {
        "$value": "#004f54",
        "$type": "color"
      },
      "800": {
        "$value": "#003b3f",
        "$type": "color"
      },
      "900": {
        "$value": "#00292c",
        "$type": "color"
      }
    },
    "red": {
      "50": {
        "$value": "#feefee",
        "$type": "color"
      },
      "100": {
        "$value": "#fee4e2",
        "$type": "color"
      },
      "200": {
        "$value": "#fecdc9",
        "$type": "color"
      },
      "300": {
        "$value": "#fda19b",
        "$type": "color"
      },
      "400": {
        "$value": "#f97066",
        "$type": "color"
      },
      "500": {
        "$value": "#f04438",
        "$type": "color"
      },
      "600": {
        "$value": "#d92d20",
        "$type": "color"
      },
      "700": {
        "$value": "#b32318",
        "$type": "color"
      },
      "800": {
        "$value": "#912018",
        "$type": "color"
      },
      "900": {
        "$value": "#7a271a",
        "$type": "color"
      }
    },
    "apricot": {
      "50": {
        "$value": "#fef5dd",
        "$type": "color"
      },
      "100": {
        "$value": "#feefc6",
        "$type": "color"
      },
      "200": {
        "$value": "#fedf89",
        "$type": "color"
      },
      "300": {
        "$value": "#fec84b",
        "$type": "color"
      },
      "400": {
        "$value": "#fdb022",
        "$type": "color"
      },
      "500": {
        "$value": "#f79009",
        "$type": "color"
      },
      "600": {
        "$value": "#dc6803",
        "$type": "color"
      },
      "700": {
        "$value": "#b54708",
        "$type": "color"
      },
      "800": {
        "$value": "#93370d",
        "$type": "color"
      },
      "900": {
        "$value": "#792e0d",
        "$type": "color"
      }
    },
    "green": {
      "50": {
        "$value": "#ebf7f1",
        "$type": "color"
      },
      "100": {
        "$value": "#e6f5ee",
        "$type": "color"
      },
      "200": {
        "$value": "#cdeadd",
        "$type": "color"
      },
      "300": {
        "$value": "#9ad6bb",
        "$type": "color"
      },
      "400": {
        "$value": "#68c199",
        "$type": "color"
      },
      "500": {
        "$value": "#35ad77",
        "$type": "color"
      },
      "600": {
        "$value": "#039855",
        "$type": "color"
      },
      "700": {
        "$value": "#027948",
        "$type": "color"
      },
      "800": {
        "$value": "#05603a",
        "$type": "color"
      },
      "900": {
        "$value": "#054f31",
        "$type": "color"
      }
    },
    "gray": {
      "50": {
        "$value": "#f7f7f7",
        "$type": "color"
      },
      "100": {
        "$value": "#f5f5f5",
        "$type": "color"
      },
      "200": {
        "$value": "#f1f1f1",
        "$type": "color"
      },
      "250": {
        "$value": "#d9d9d9",
        "$type": "color"
      },
      "300": {
        "$value": "#b2b2b2",
        "$type": "color"
      },
      "350": {
        "$value": "#8a8a8a",
        "$type": "color"
      },
      "400": {
        "$value": "#757575",
        "$type": "color"
      },
      "500": {
        "$value": "#616161",
        "$type": "color"
      },
      "600": {
        "$value": "#444444",
        "$type": "color"
      },
      "700": {
        "$value": "#383838",
        "$type": "color"
      },
      "800": {
        "$value": "#2c2c2c",
        "$type": "color"
      },
      "900": {
        "$value": "#1e1e1e",
        "$type": "color"
      }
    },
    "black": {
      "1000": {
        "$value": "#121212",
        "$type": "color"
      }
    },
    "white": {
      "1000": {
        "$value": "#ffffff",
        "$type": "color"
      }
    },
    "alpha": {
      "black": {
        "100-alpha": {
          "$value": "#1212120d",
          "$type": "color"
        },
        "200-alpha": {
          "$value": "#1212121a",
          "$type": "color"
        },
        "300-alpha": {
          "$value": "#12121233",
          "$type": "color"
        },
        "400-alpha": {
          "$value": "#12121266",
          "$type": "color"
        },
        "500-alpha": {
          "$value": "#12121299",
          "$type": "color"
        }
      },
      "white": {
        "100-alpha": {
          "$value": "#ffffff0d",
          "$type": "color"
        },
        "200-alpha": {
          "$value": "#ffffff1a",
          "$type": "color"
        },
        "300-alpha": {
          "$value": "#ffffff33",
          "$type": "color"
        },
        "400-alpha": {
          "$value": "#ffffff66",
          "$type": "color"
        },
        "500-alpha": {
          "$value": "#ffffff99",
          "$type": "color"
        }
      },
      "gray": {
        "alpha-100": {
          "$value": "#44444408",
          "$type": "color"
        },
        "alpha-200": {
          "$value": "#4444441a",
          "$type": "color"
        },
        "alpha-300": {
          "$value": "#44444433",
          "$type": "color"
        },
        "alpha-400": {
          "$value": "#44444466",
          "$type": "color"
        },
        "alpha-500": {
          "$value": "#44444499",
          "$type": "color"
        }
      }
    }
  }
}
```

</details>

#### `tokens/core/color.tokens.json`

_89 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `color.background.base.default` | `{palette.white.1000}` | `color` | `--color-background-base-default` |
| `color.background.base.default-active` | `{palette.gray.250}` | `color` | `--color-background-base-default-active` |
| `color.background.base.secondary` | `{palette.gray.100}` | `color` | `--color-background-base-secondary` |
| `color.background.base.secondary-active` | `{palette.gray.250}` | `color` | `--color-background-base-secondary-active` |
| `color.background.base.tertiary` | `{palette.gray.200}` | `color` | `--color-background-base-tertiary` |
| `color.background.base.tertiary-active` | `{palette.gray.300}` | `color` | `--color-background-base-tertiary-active` |
| `color.background.base.tertiary-hover` | `{palette.gray.250}` | `color` | `--color-background-base-tertiary-hover` |
| `color.background.base.default-hover` | `{palette.gray.100}` | `color` | `--color-background-base-default-hover` |
| `color.background.base.secondary-hover` | `{palette.gray.200}` | `color` | `--color-background-base-secondary-hover` |
| `color.background.brand.default` | `{palette.blue-sky.600}` | `color` | `--color-background-brand-default` |
| `color.background.brand.default-active` | `{palette.blue-sky.800}` | `color` | `--color-background-brand-default-active` |
| `color.background.brand.secondary` | `{palette.blue-sky.100}` | `color` | `--color-background-brand-secondary` |
| `color.background.brand.secondary-active` | `{palette.blue-sky.300}` | `color` | `--color-background-brand-secondary-active` |
| `color.background.brand.default-hover` | `{palette.blue-sky.700}` | `color` | `--color-background-brand-default-hover` |
| `color.background.brand.secondary-hover` | `{palette.blue-sky.200}` | `color` | `--color-background-brand-secondary-hover` |
| `color.background.brand.tertiary` | `{palette.blue-sky.900}` | `color` | `--color-background-brand-tertiary` |
| `color.background.brand.focus-ring` | `{palette.blue-sky.500}` | `color` | `--color-background-brand-focus-ring` |
| `color.background.positive.default` | `{palette.green.600}` | `color` | `--color-background-positive-default` |
| `color.background.positive.default-active` | `{palette.green.800}` | `color` | `--color-background-positive-default-active` |
| `color.background.positive.secondary` | `{palette.green.100}` | `color` | `--color-background-positive-secondary` |
| `color.background.positive.secondary-active` | `{palette.green.200}` | `color` | `--color-background-positive-secondary-active` |
| `color.background.positive.default-hover` | `{palette.green.700}` | `color` | `--color-background-positive-default-hover` |
| `color.background.warning.default` | `{palette.apricot.400}` | `color` | `--color-background-warning-default` |
| `color.background.warning.default-active` | `{palette.apricot.600}` | `color` | `--color-background-warning-default-active` |
| `color.background.warning.secondary` | `{palette.apricot.100}` | `color` | `--color-background-warning-secondary` |
| `color.background.warning.secondary-active` | `{palette.apricot.200}` | `color` | `--color-background-warning-secondary-active` |
| `color.background.warning.accent` | `{palette.apricot.300}` | `color` | `--color-background-warning-accent` |
| `color.background.warning.default-hover` | `{palette.apricot.500}` | `color` | `--color-background-warning-default-hover` |
| `color.background.danger.default` | `{palette.red.600}` | `color` | `--color-background-danger-default` |
| `color.background.danger.default-active` | `{palette.red.800}` | `color` | `--color-background-danger-default-active` |
| `color.background.danger.secondary` | `{palette.red.100}` | `color` | `--color-background-danger-secondary` |
| `color.background.danger.secondary-active` | `{palette.red.300}` | `color` | `--color-background-danger-secondary-active` |
| `color.background.danger.default-hover` | `{palette.red.700}` | `color` | `--color-background-danger-default-hover` |
| `color.background.danger.secondary-hover` | `{palette.red.200}` | `color` | `--color-background-danger-secondary-hover` |
| `color.background.base-inverse.default` | `{palette.gray.900}` | `color` | `--color-background-base-inverse-default` |
| `color.background.base-inverse.default-active` | `{palette.gray.600}` | `color` | `--color-background-base-inverse-default-active` |
| `color.background.base-inverse.default-hover` | `{palette.gray.700}` | `color` | `--color-background-base-inverse-default-hover` |
| `color.background.disabled.default` | `{palette.gray.200}` | `color` | `--color-background-disabled-default` |
| `color.background.disabled.secondary` | `{palette.gray.250}` | `color` | `--color-background-disabled-secondary` |
| `color.background.alpha.overlay-dark` | `{palette.alpha.black.400-alpha}` | `color` | `--color-background-alpha-overlay-dark` |
| `color.background.alpha.overlay-light` | `{palette.alpha.white.400-alpha}` | `color` | `--color-background-alpha-overlay-light` |
| `color.background.alpha.large-surface` | `{palette.alpha.gray.alpha-100}` | `color` | `--color-background-alpha-large-surface` |
| `color.border.base.default` | `{palette.gray.250}` | `color` | `--color-border-base-default` |
| `color.border.base.secondary` | `{palette.gray.300}` | `color` | `--color-border-base-secondary` |
| `color.border.base.tertiary` | `{palette.gray.600}` | `color` | `--color-border-base-tertiary` |
| `color.border.base.strong` | `{palette.black.1000}` | `color` | `--color-border-base-strong` |
| `color.border.base.subtle` | `{palette.white.1000}` | `color` | `--color-border-base-subtle` |
| `color.border.brand.default` | `{palette.blue-sky.600}` | `color` | `--color-border-brand-default` |
| `color.border.disabled.default` | `{palette.gray.250}` | `color` | `--color-border-disabled-default` |
| `color.border.positive.default` | `{palette.green.700}` | `color` | `--color-border-positive-default` |
| `color.border.warning.default` | `{palette.apricot.600}` | `color` | `--color-border-warning-default` |
| `color.border.danger.default` | `{palette.red.600}` | `color` | `--color-border-danger-default` |
| `color.text.base.default` | `{palette.black.1000}` | `color` | `--color-text-base-default` |
| `color.text.base.secondary` | `{palette.gray.700}` | `color` | `--color-text-base-secondary` |
| `color.text.base.tertiary` | `{palette.gray.400}` | `color` | `--color-text-base-tertiary` |
| `color.text.base.default-on-color` | `{palette.black.1000}` | `color` | `--color-text-base-default-on-color` |
| `color.text.base.secondary-on-color` | `{palette.gray.600}` | `color` | `--color-text-base-secondary-on-color` |
| `color.text.base-inverse.default` | `{palette.white.1000}` | `color` | `--color-text-base-inverse-default` |
| `color.text.base-inverse.on-color` | `{palette.white.1000}` | `color` | `--color-text-base-inverse-on-color` |
| `color.text.brand.default` | `{palette.blue-sky.600}` | `color` | `--color-text-brand-default` |
| `color.text.brand.on-secondary` | `{palette.blue-sky.600}` | `color` | `--color-text-brand-on-secondary` |
| `color.text.brand.default-hover` | `{palette.blue-sky.600}` | `color` | `--color-text-brand-default-hover` |
| `color.text.brand.visited` | `{palette.magenta.600}` | `color` | `--color-text-brand-visited` |
| `color.text.disabled.default` | `{palette.gray.300}` | `color` | `--color-text-disabled-default` |
| `color.text.disabled.on-disabled` | `{palette.gray.300}` | `color` | `--color-text-disabled-on-disabled` |
| `color.text.positive.default` | `{palette.green.600}` | `color` | `--color-text-positive-default` |
| `color.text.positive.on-secondary` | `{palette.green.700}` | `color` | `--color-text-positive-on-secondary` |
| `color.text.warning.default` | `{palette.apricot.700}` | `color` | `--color-text-warning-default` |
| `color.text.warning.on-secondary` | `{palette.apricot.700}` | `color` | `--color-text-warning-on-secondary` |
| `color.text.danger.default` | `{palette.red.600}` | `color` | `--color-text-danger-default` |
| `color.text.danger.on-secondary` | `{palette.red.700}` | `color` | `--color-text-danger-on-secondary` |
| `color.icon.base.default` | `{palette.black.1000}` | `color` | `--color-icon-base-default` |
| `color.icon.base.secondary` | `{palette.gray.600}` | `color` | `--color-icon-base-secondary` |
| `color.icon.base.tertiary` | `{palette.gray.400}` | `color` | `--color-icon-base-tertiary` |
| `color.icon.base.default-on-color` | `{palette.black.1000}` | `color` | `--color-icon-base-default-on-color` |
| `color.icon.base.secondary-on-color` | `{palette.gray.600}` | `color` | `--color-icon-base-secondary-on-color` |
| `color.icon.brand.default` | `{palette.blue-sky.600}` | `color` | `--color-icon-brand-default` |
| `color.icon.brand.on-secondary` | `{palette.blue-sky.600}` | `color` | `--color-icon-brand-on-secondary` |
| `color.icon.brand.visited` | `{palette.magenta.600}` | `color` | `--color-icon-brand-visited` |
| `color.icon.disabled.default` | `{palette.gray.300}` | `color` | `--color-icon-disabled-default` |
| `color.icon.disabled.on-disabled` | `{palette.gray.300}` | `color` | `--color-icon-disabled-on-disabled` |
| `color.icon.positive.default` | `{palette.green.600}` | `color` | `--color-icon-positive-default` |
| `color.icon.positive.on-secondary` | `{palette.green.700}` | `color` | `--color-icon-positive-on-secondary` |
| `color.icon.warning.default` | `{palette.apricot.600}` | `color` | `--color-icon-warning-default` |
| `color.icon.warning.on-secondary` | `{palette.apricot.700}` | `color` | `--color-icon-warning-on-secondary` |
| `color.icon.danger.default` | `{palette.red.600}` | `color` | `--color-icon-danger-default` |
| `color.icon.danger.on-secondary` | `{palette.red.700}` | `color` | `--color-icon-danger-on-secondary` |
| `color.icon.base-inverse.default` | `{palette.white.1000}` | `color` | `--color-icon-base-inverse-default` |
| `color.icon.base-inverse.on-color` | `{palette.white.1000}` | `color` | `--color-icon-base-inverse-on-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/color.tokens.json</code></summary>

```json
{
  "color": {
    "background": {
      "base": {
        "default": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.100}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.200}",
          "$type": "color"
        },
        "tertiary-active": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        },
        "tertiary-hover": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.gray.100}",
          "$type": "color"
        },
        "secondary-hover": {
          "$value": "{palette.gray.200}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.blue-sky.800}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.blue-sky.100}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.blue-sky.300}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.blue-sky.700}",
          "$type": "color"
        },
        "secondary-hover": {
          "$value": "{palette.blue-sky.200}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.blue-sky.900}",
          "$type": "color"
        },
        "focus-ring": {
          "$value": "{palette.blue-sky.500}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.600}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.green.800}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.green.100}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.green.200}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.green.700}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.400}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.apricot.600}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.apricot.100}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.apricot.200}",
          "$type": "color"
        },
        "accent": {
          "$value": "{palette.apricot.300}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.apricot.500}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.600}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.red.800}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.red.100}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.red.300}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.red.700}",
          "$type": "color"
        },
        "secondary-hover": {
          "$value": "{palette.red.200}",
          "$type": "color"
        }
      },
      "base-inverse": {
        "default": {
          "$value": "{palette.gray.900}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.gray.700}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.200}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        }
      },
      "alpha": {
        "overlay-dark": {
          "$value": "{palette.alpha.black.400-alpha}",
          "$type": "color"
        },
        "overlay-light": {
          "$value": "{palette.alpha.white.400-alpha}",
          "$type": "color"
        },
        "large-surface": {
          "$value": "{palette.alpha.gray.alpha-100}",
          "$type": "color"
        }
      }
    },
    "border": {
      "base": {
        "default": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "strong": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "subtle": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.700}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.600}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.600}",
          "$type": "color"
        }
      }
    },
    "text": {
      "base": {
        "default": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.700}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.400}",
          "$type": "color"
        },
        "default-on-color": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "secondary-on-color": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        }
      },
      "base-inverse": {
        "default": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        },
        "on-color": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "visited": {
          "$value": "{palette.magenta.600}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        },
        "on-disabled": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.green.700}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.700}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.apricot.700}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.red.700}",
          "$type": "color"
        }
      }
    },
    "icon": {
      "base": {
        "default": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.400}",
          "$type": "color"
        },
        "default-on-color": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "secondary-on-color": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "visited": {
          "$value": "{palette.magenta.600}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        },
        "on-disabled": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.green.700}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.apricot.700}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.600}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.red.700}",
          "$type": "color"
        }
      },
      "base-inverse": {
        "default": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        },
        "on-color": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        }
      }
    }
  }
}
```

</details>

#### `tokens/core/font.tokens.json`

_32 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `fontFamily.primary` | `Onest` | `fontFamily` | `--font-family-primary` |
| `fontSize.10` | `10px` | `dimension` | `--font-size-10` |
| `fontSize.12` | `12px` | `dimension` | `--font-size-12` |
| `fontSize.14` | `14px` | `dimension` | `--font-size-14` |
| `fontSize.16` | `16px` | `dimension` | `--font-size-16` |
| `fontSize.18` | `18px` | `dimension` | `--font-size-18` |
| `fontSize.20` | `20px` | `dimension` | `--font-size-20` |
| `fontSize.22` | `22px` | `dimension` | `--font-size-22` |
| `fontSize.24` | `24px` | `dimension` | `--font-size-24` |
| `fontSize.28` | `28px` | `dimension` | `--font-size-28` |
| `fontSize.32` | `32px` | `dimension` | `--font-size-32` |
| `fontSize.40` | `40px` | `dimension` | `--font-size-40` |
| `fontSize.48` | `48px` | `dimension` | `--font-size-48` |
| `fontSize.56` | `56px` | `dimension` | `--font-size-56` |
| `fontSize.64` | `64px` | `dimension` | `--font-size-64` |
| `fontWeight.regular` | `400` | `fontWeight` | `--font-weight-regular` |
| `fontWeight.medium` | `500` | `fontWeight` | `--font-weight-medium` |
| `fontWeight.semibold` | `600` | `fontWeight` | `--font-weight-semibold` |
| `fontWeight.bold` | `700` | `fontWeight` | `--font-weight-bold` |
| `lineHeight.12` | `12px` | `dimension` | `--line-height-12` |
| `lineHeight.16` | `16px` | `dimension` | `--line-height-16` |
| `lineHeight.20` | `20px` | `dimension` | `--line-height-20` |
| `lineHeight.24` | `24px` | `dimension` | `--line-height-24` |
| `lineHeight.26` | `26px` | `dimension` | `--line-height-26` |
| `lineHeight.28` | `28px` | `dimension` | `--line-height-28` |
| `lineHeight.30` | `30px` | `dimension` | `--line-height-30` |
| `lineHeight.32` | `32px` | `dimension` | `--line-height-32` |
| `lineHeight.36` | `36px` | `dimension` | `--line-height-36` |
| `lineHeight.40` | `40px` | `dimension` | `--line-height-40` |
| `lineHeight.48` | `48px` | `dimension` | `--line-height-48` |
| `lineHeight.56` | `56px` | `dimension` | `--line-height-56` |
| `lineHeight.64` | `64px` | `dimension` | `--line-height-64` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/font.tokens.json</code></summary>

```json
{
  "fontFamily": {
    "primary": {
      "$value": "Onest",
      "$type": "fontFamily"
    }
  },
  "fontSize": {
    "10": {
      "$value": "10px",
      "$type": "dimension"
    },
    "12": {
      "$value": "12px",
      "$type": "dimension"
    },
    "14": {
      "$value": "14px",
      "$type": "dimension"
    },
    "16": {
      "$value": "16px",
      "$type": "dimension"
    },
    "18": {
      "$value": "18px",
      "$type": "dimension"
    },
    "20": {
      "$value": "20px",
      "$type": "dimension"
    },
    "22": {
      "$value": "22px",
      "$type": "dimension"
    },
    "24": {
      "$value": "24px",
      "$type": "dimension"
    },
    "28": {
      "$value": "28px",
      "$type": "dimension"
    },
    "32": {
      "$value": "32px",
      "$type": "dimension"
    },
    "40": {
      "$value": "40px",
      "$type": "dimension"
    },
    "48": {
      "$value": "48px",
      "$type": "dimension"
    },
    "56": {
      "$value": "56px",
      "$type": "dimension"
    },
    "64": {
      "$value": "64px",
      "$type": "dimension"
    }
  },
  "fontWeight": {
    "regular": {
      "$value": 400,
      "$type": "fontWeight"
    },
    "medium": {
      "$value": 500,
      "$type": "fontWeight"
    },
    "semibold": {
      "$value": 600,
      "$type": "fontWeight"
    },
    "bold": {
      "$value": 700,
      "$type": "fontWeight"
    }
  },
  "lineHeight": {
    "12": {
      "$value": "12px",
      "$type": "dimension"
    },
    "16": {
      "$value": "16px",
      "$type": "dimension"
    },
    "20": {
      "$value": "20px",
      "$type": "dimension"
    },
    "24": {
      "$value": "24px",
      "$type": "dimension"
    },
    "26": {
      "$value": "26px",
      "$type": "dimension"
    },
    "28": {
      "$value": "28px",
      "$type": "dimension"
    },
    "30": {
      "$value": "30px",
      "$type": "dimension"
    },
    "32": {
      "$value": "32px",
      "$type": "dimension"
    },
    "36": {
      "$value": "36px",
      "$type": "dimension"
    },
    "40": {
      "$value": "40px",
      "$type": "dimension"
    },
    "48": {
      "$value": "48px",
      "$type": "dimension"
    },
    "56": {
      "$value": "56px",
      "$type": "dimension"
    },
    "64": {
      "$value": "64px",
      "$type": "dimension"
    }
  },
  "letterSpacing": {}
}
```

</details>

#### `tokens/core/sizes.tokens.json`

_31 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `spacing.0` | `0px` | `dimension` | `--spacing-0` |
| `spacing.2` | `2px` | `dimension` | `--spacing-2` |
| `spacing.4` | `4px` | `dimension` | `--spacing-4` |
| `spacing.6` | `6px` | `dimension` | `--spacing-6` |
| `spacing.8` | `8px` | `dimension` | `--spacing-8` |
| `spacing.12` | `12px` | `dimension` | `--spacing-12` |
| `spacing.16` | `16px` | `dimension` | `--spacing-16` |
| `spacing.20` | `20px` | `dimension` | `--spacing-20` |
| `spacing.24` | `24px` | `dimension` | `--spacing-24` |
| `spacing.32` | `32px` | `dimension` | `--spacing-32` |
| `spacing.40` | `40px` | `dimension` | `--spacing-40` |
| `spacing.48` | `48px` | `dimension` | `--spacing-48` |
| `spacing.56` | `56px` | `dimension` | `--spacing-56` |
| `spacing.64` | `64px` | `dimension` | `--spacing-64` |
| `spacing.80` | `80px` | `dimension` | `--spacing-80` |
| `spacing.96` | `96px` | `dimension` | `--spacing-96` |
| `spacing.120` | `120px` | `dimension` | `--spacing-120` |
| `borderRadius.0` | `0px` | `dimension` | `--border-radius-0` |
| `borderRadius.4` | `4px` | `dimension` | `--border-radius-4` |
| `borderRadius.6` | `6px` | `dimension` | `--border-radius-6` |
| `borderRadius.8` | `8px` | `dimension` | `--border-radius-8` |
| `borderRadius.12` | `12px` | `dimension` | `--border-radius-12` |
| `borderRadius.16` | `16px` | `dimension` | `--border-radius-16` |
| `borderRadius.24` | `24px` | `dimension` | `--border-radius-24` |
| `borderRadius.32` | `32px` | `dimension` | `--border-radius-32` |
| `borderRadius.full` | `9999px` | `dimension` | `--border-radius-full` |
| `borderWidth.1` | `1px` | `dimension` | `--border-width-1` |
| `borderWidth.2` | `2px` | `dimension` | `--border-width-2` |
| `borderWidth.3` | `3px` | `dimension` | `--border-width-3` |
| `borderWidth.1-5` | `1.5px` | `dimension` | `--border-width-1-5` |
| `borderWidth.0-5` | `0.5px` | `dimension` | `--border-width-0-5` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/sizes.tokens.json</code></summary>

```json
{
  "spacing": {
    "0": {
      "$value": "0px",
      "$type": "dimension"
    },
    "2": {
      "$value": "2px",
      "$type": "dimension"
    },
    "4": {
      "$value": "4px",
      "$type": "dimension"
    },
    "6": {
      "$value": "6px",
      "$type": "dimension"
    },
    "8": {
      "$value": "8px",
      "$type": "dimension"
    },
    "12": {
      "$value": "12px",
      "$type": "dimension"
    },
    "16": {
      "$value": "16px",
      "$type": "dimension"
    },
    "20": {
      "$value": "20px",
      "$type": "dimension"
    },
    "24": {
      "$value": "24px",
      "$type": "dimension"
    },
    "32": {
      "$value": "32px",
      "$type": "dimension"
    },
    "40": {
      "$value": "40px",
      "$type": "dimension"
    },
    "48": {
      "$value": "48px",
      "$type": "dimension"
    },
    "56": {
      "$value": "56px",
      "$type": "dimension"
    },
    "64": {
      "$value": "64px",
      "$type": "dimension"
    },
    "80": {
      "$value": "80px",
      "$type": "dimension"
    },
    "96": {
      "$value": "96px",
      "$type": "dimension"
    },
    "120": {
      "$value": "120px",
      "$type": "dimension"
    }
  },
  "borderRadius": {
    "0": {
      "$value": "0px",
      "$type": "dimension"
    },
    "4": {
      "$value": "4px",
      "$type": "dimension"
    },
    "6": {
      "$value": "6px",
      "$type": "dimension"
    },
    "8": {
      "$value": "8px",
      "$type": "dimension"
    },
    "12": {
      "$value": "12px",
      "$type": "dimension"
    },
    "16": {
      "$value": "16px",
      "$type": "dimension"
    },
    "24": {
      "$value": "24px",
      "$type": "dimension"
    },
    "32": {
      "$value": "32px",
      "$type": "dimension"
    },
    "full": {
      "$value": "9999px",
      "$type": "dimension"
    }
  },
  "borderWidth": {
    "1": {
      "$value": "1px",
      "$type": "dimension"
    },
    "2": {
      "$value": "2px",
      "$type": "dimension"
    },
    "3": {
      "$value": "3px",
      "$type": "dimension"
    },
    "1-5": {
      "$value": "1.5px",
      "$type": "dimension"
    },
    "0-5": {
      "$value": "0.5px",
      "$type": "dimension"
    }
  }
}
```

</details>

#### `tokens/core/effects.tokens.json`

_5 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `dropShadow.100` | `0px 2px 8px 0px rgba(19, 22, 29, 0.06), 0px 4px 8px 1px rgba(19, 22, 29, 0.04)` | `string` | `--drop-shadow-100` |
| `dropShadow.200` | `0px 4px 8px 2px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.04)` | `string` | `--drop-shadow-200` |
| `dropShadow.300` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` | `string` | `--drop-shadow-300` |
| `dropShadow.400` | `0px 12px 16px 6px rgba(40, 46, 55, 0.06), 0px 4px 6px 0px rgba(40, 46, 55, 0.06)` | `string` | `--drop-shadow-400` |
| `dropShadow.500` | `0px 16px 20px 6px rgba(40, 46, 55, 0.06), 0px 4px 8px 0px rgba(40, 46, 55, 0.06)` | `string` | `--drop-shadow-500` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/effects.tokens.json</code></summary>

```json
{
  "dropShadow": {
    "100": {
      "$value": "0px 2px 8px 0px rgba(19, 22, 29, 0.06), 0px 4px 8px 1px rgba(19, 22, 29, 0.04)",
      "$type": "string"
    },
    "200": {
      "$value": "0px 4px 8px 2px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.04)",
      "$type": "string"
    },
    "300": {
      "$value": "0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)",
      "$type": "string"
    },
    "400": {
      "$value": "0px 12px 16px 6px rgba(40, 46, 55, 0.06), 0px 4px 6px 0px rgba(40, 46, 55, 0.06)",
      "$type": "string"
    },
    "500": {
      "$value": "0px 16px 20px 6px rgba(40, 46, 55, 0.06), 0px 4px 8px 0px rgba(40, 46, 55, 0.06)",
      "$type": "string"
    }
  }
}
```

</details>

#### `tokens/core/focusRing.tokens.json`

_4 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `focusRing.color.inner` | `{color.text.base-inverse.on-color}` | `color` | `--focus-ring-color-inner` |
| `focusRing.color.outer` | `{color.background.brand.focus-ring}` | `color` | `--focus-ring-color-outer` |
| `focusRing.width.inner` | `2px` | `dimension` | `--focus-ring-width-inner` |
| `focusRing.width.outer` | `3px` | `dimension` | `--focus-ring-width-outer` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/focusRing.tokens.json</code></summary>

```json
{
  "focusRing": {
    "color": {
      "inner": {
        "$value": "{color.text.base-inverse.on-color}",
        "$type": "color"
      },
      "outer": {
        "$value": "{color.background.brand.focus-ring}",
        "$type": "color"
      }
    },
    "width": {
      "inner": {
        "$value": "2px",
        "$type": "dimension"
      },
      "outer": {
        "$value": "3px",
        "$type": "dimension"
      }
    }
  }
}
```

</details>

#### `tokens/core/screen.tokens.json`

_16 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `screen.width.fixed.desktop` | `1440px` | `dimension` | `--screen-width-fixed-desktop` |
| `screen.width.fixed.laptop` | `1280px` | `dimension` | `--screen-width-fixed-laptop` |
| `screen.width.fixed.tablet` | `768px` | `dimension` | `--screen-width-fixed-tablet` |
| `screen.width.fixed.mobile` | `360px` | `dimension` | `--screen-width-fixed-mobile` |
| `screen.width.min-fluid.desktop` | `1440px` | `dimension` | `--screen-width-min-fluid-desktop` |
| `screen.width.min-fluid.laptop` | `1024px` | `dimension` | `--screen-width-min-fluid-laptop` |
| `screen.width.min-fluid.tablet` | `480px` | `dimension` | `--screen-width-min-fluid-tablet` |
| `screen.width.min-fluid.mobile` | `320px` | `dimension` | `--screen-width-min-fluid-mobile` |
| `screen.width.max-fluid.desktop` | `1920px` | `dimension` | `--screen-width-max-fluid-desktop` |
| `screen.width.max-fluid.laptop` | `1440px` | `dimension` | `--screen-width-max-fluid-laptop` |
| `screen.width.max-fluid.tablet` | `1024px` | `dimension` | `--screen-width-max-fluid-tablet` |
| `screen.width.max-fluid.mobile` | `480px` | `dimension` | `--screen-width-max-fluid-mobile` |
| `screen.height.fixed.desktop` | `1024px` | `dimension` | `--screen-height-fixed-desktop` |
| `screen.height.fixed.laptop` | `832px` | `dimension` | `--screen-height-fixed-laptop` |
| `screen.height.fixed.tablet` | `1024px` | `dimension` | `--screen-height-fixed-tablet` |
| `screen.height.fixed.mobile` | `800px` | `dimension` | `--screen-height-fixed-mobile` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/screen.tokens.json</code></summary>

```json
{
  "screen": {
    "width": {
      "fixed": {
        "desktop": {
          "$value": "1440px",
          "$type": "dimension"
        },
        "laptop": {
          "$value": "1280px",
          "$type": "dimension"
        },
        "tablet": {
          "$value": "768px",
          "$type": "dimension"
        },
        "mobile": {
          "$value": "360px",
          "$type": "dimension"
        }
      },
      "min-fluid": {
        "desktop": {
          "$value": "1440px",
          "$type": "dimension"
        },
        "laptop": {
          "$value": "1024px",
          "$type": "dimension"
        },
        "tablet": {
          "$value": "480px",
          "$type": "dimension"
        },
        "mobile": {
          "$value": "320px",
          "$type": "dimension"
        }
      },
      "max-fluid": {
        "desktop": {
          "$value": "1920px",
          "$type": "dimension"
        },
        "laptop": {
          "$value": "1440px",
          "$type": "dimension"
        },
        "tablet": {
          "$value": "1024px",
          "$type": "dimension"
        },
        "mobile": {
          "$value": "480px",
          "$type": "dimension"
        }
      }
    },
    "height": {
      "fixed": {
        "desktop": {
          "$value": "1024px",
          "$type": "dimension"
        },
        "laptop": {
          "$value": "832px",
          "$type": "dimension"
        },
        "tablet": {
          "$value": "1024px",
          "$type": "dimension"
        },
        "mobile": {
          "$value": "800px",
          "$type": "dimension"
        }
      }
    }
  }
}
```

</details>

### 5.2 Temă întunecată — `tokens/core.dark/`

#### `tokens/core.dark/color.tokens.json`

_89 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `color.background.base.default` | `{palette.gray.900}` | `color` | `--color-background-base-default` |
| `color.background.base.default-active` | `{palette.gray.700}` | `color` | `--color-background-base-default-active` |
| `color.background.base.secondary` | `{palette.gray.800}` | `color` | `--color-background-base-secondary` |
| `color.background.base.secondary-active` | `{palette.gray.600}` | `color` | `--color-background-base-secondary-active` |
| `color.background.base.tertiary` | `{palette.gray.700}` | `color` | `--color-background-base-tertiary` |
| `color.background.base.tertiary-active` | `{palette.gray.500}` | `color` | `--color-background-base-tertiary-active` |
| `color.background.base.tertiary-hover` | `{palette.gray.600}` | `color` | `--color-background-base-tertiary-hover` |
| `color.background.base.default-hover` | `{palette.gray.800}` | `color` | `--color-background-base-default-hover` |
| `color.background.base.secondary-hover` | `{palette.gray.700}` | `color` | `--color-background-base-secondary-hover` |
| `color.background.brand.default` | `{palette.blue-sky.600}` | `color` | `--color-background-brand-default` |
| `color.background.brand.default-active` | `{palette.blue-sky.800}` | `color` | `--color-background-brand-default-active` |
| `color.background.brand.secondary` | `{palette.blue-sky.200}` | `color` | `--color-background-brand-secondary` |
| `color.background.brand.secondary-active` | `{palette.blue-sky.400}` | `color` | `--color-background-brand-secondary-active` |
| `color.background.brand.default-hover` | `{palette.blue-sky.700}` | `color` | `--color-background-brand-default-hover` |
| `color.background.brand.secondary-hover` | `{palette.blue-sky.300}` | `color` | `--color-background-brand-secondary-hover` |
| `color.background.brand.tertiary` | `{palette.blue-sky.900}` | `color` | `--color-background-brand-tertiary` |
| `color.background.brand.focus-ring` | `{palette.blue-sky.500}` | `color` | `--color-background-brand-focus-ring` |
| `color.background.positive.default` | `{palette.green.700}` | `color` | `--color-background-positive-default` |
| `color.background.positive.default-active` | `{palette.green.900}` | `color` | `--color-background-positive-default-active` |
| `color.background.positive.secondary` | `{palette.green.200}` | `color` | `--color-background-positive-secondary` |
| `color.background.positive.secondary-active` | `{palette.green.300}` | `color` | `--color-background-positive-secondary-active` |
| `color.background.positive.default-hover` | `{palette.green.800}` | `color` | `--color-background-positive-default-hover` |
| `color.background.warning.default` | `{palette.apricot.500}` | `color` | `--color-background-warning-default` |
| `color.background.warning.default-active` | `{palette.apricot.700}` | `color` | `--color-background-warning-default-active` |
| `color.background.warning.secondary` | `{palette.apricot.200}` | `color` | `--color-background-warning-secondary` |
| `color.background.warning.secondary-active` | `{palette.apricot.300}` | `color` | `--color-background-warning-secondary-active` |
| `color.background.warning.accent` | `{palette.apricot.400}` | `color` | `--color-background-warning-accent` |
| `color.background.warning.default-hover` | `{palette.apricot.600}` | `color` | `--color-background-warning-default-hover` |
| `color.background.danger.default` | `{palette.red.700}` | `color` | `--color-background-danger-default` |
| `color.background.danger.default-active` | `{palette.red.900}` | `color` | `--color-background-danger-default-active` |
| `color.background.danger.secondary` | `{palette.red.200}` | `color` | `--color-background-danger-secondary` |
| `color.background.danger.secondary-active` | `{palette.red.400}` | `color` | `--color-background-danger-secondary-active` |
| `color.background.danger.default-hover` | `{palette.red.800}` | `color` | `--color-background-danger-default-hover` |
| `color.background.danger.secondary-hover` | `{palette.red.300}` | `color` | `--color-background-danger-secondary-hover` |
| `color.background.base-inverse.default` | `{palette.gray.100}` | `color` | `--color-background-base-inverse-default` |
| `color.background.base-inverse.default-active` | `{palette.gray.250}` | `color` | `--color-background-base-inverse-default-active` |
| `color.background.base-inverse.default-hover` | `{palette.gray.200}` | `color` | `--color-background-base-inverse-default-hover` |
| `color.background.disabled.default` | `{palette.gray.700}` | `color` | `--color-background-disabled-default` |
| `color.background.disabled.secondary` | `{palette.gray.400}` | `color` | `--color-background-disabled-secondary` |
| `color.background.alpha.overlay-dark` | `{palette.alpha.black.200-alpha}` | `color` | `--color-background-alpha-overlay-dark` |
| `color.background.alpha.overlay-light` | `{palette.alpha.white.200-alpha}` | `color` | `--color-background-alpha-overlay-light` |
| `color.background.alpha.large-surface` | `{palette.alpha.gray.alpha-200}` | `color` | `--color-background-alpha-large-surface` |
| `color.border.base.default` | `{palette.gray.600}` | `color` | `--color-border-base-default` |
| `color.border.base.secondary` | `{palette.gray.400}` | `color` | `--color-border-base-secondary` |
| `color.border.base.tertiary` | `{palette.gray.250}` | `color` | `--color-border-base-tertiary` |
| `color.border.base.strong` | `{palette.gray.100}` | `color` | `--color-border-base-strong` |
| `color.border.base.subtle` | `{palette.black.1000}` | `color` | `--color-border-base-subtle` |
| `color.border.brand.default` | `{palette.blue-sky.500}` | `color` | `--color-border-brand-default` |
| `color.border.disabled.default` | `{palette.gray.600}` | `color` | `--color-border-disabled-default` |
| `color.border.positive.default` | `{palette.green.500}` | `color` | `--color-border-positive-default` |
| `color.border.warning.default` | `{palette.apricot.500}` | `color` | `--color-border-warning-default` |
| `color.border.danger.default` | `{palette.red.500}` | `color` | `--color-border-danger-default` |
| `color.text.base.default` | `{palette.white.1000}` | `color` | `--color-text-base-default` |
| `color.text.base.secondary` | `{palette.gray.200}` | `color` | `--color-text-base-secondary` |
| `color.text.base.tertiary` | `{palette.gray.300}` | `color` | `--color-text-base-tertiary` |
| `color.text.base.default-on-color` | `{palette.black.1000}` | `color` | `--color-text-base-default-on-color` |
| `color.text.base.secondary-on-color` | `{palette.gray.600}` | `color` | `--color-text-base-secondary-on-color` |
| `color.text.base-inverse.default` | `{palette.black.1000}` | `color` | `--color-text-base-inverse-default` |
| `color.text.base-inverse.on-color` | `{palette.white.1000}` | `color` | `--color-text-base-inverse-on-color` |
| `color.text.brand.default` | `{palette.blue-sky.400}` | `color` | `--color-text-brand-default` |
| `color.text.brand.on-secondary` | `{palette.blue-sky.700}` | `color` | `--color-text-brand-on-secondary` |
| `color.text.brand.default-hover` | `{palette.blue-sky.700}` | `color` | `--color-text-brand-default-hover` |
| `color.text.brand.visited` | `{palette.magenta.400}` | `color` | `--color-text-brand-visited` |
| `color.text.disabled.default` | `{palette.gray.600}` | `color` | `--color-text-disabled-default` |
| `color.text.disabled.on-disabled` | `{palette.gray.400}` | `color` | `--color-text-disabled-on-disabled` |
| `color.text.positive.default` | `{palette.green.500}` | `color` | `--color-text-positive-default` |
| `color.text.positive.on-secondary` | `{palette.green.800}` | `color` | `--color-text-positive-on-secondary` |
| `color.text.warning.default` | `{palette.apricot.500}` | `color` | `--color-text-warning-default` |
| `color.text.warning.on-secondary` | `{palette.apricot.800}` | `color` | `--color-text-warning-on-secondary` |
| `color.text.danger.default` | `{palette.red.500}` | `color` | `--color-text-danger-default` |
| `color.text.danger.on-secondary` | `{palette.red.800}` | `color` | `--color-text-danger-on-secondary` |
| `color.icon.base.default` | `{palette.white.1000}` | `color` | `--color-icon-base-default` |
| `color.icon.base.secondary` | `{palette.gray.200}` | `color` | `--color-icon-base-secondary` |
| `color.icon.base.tertiary` | `{palette.gray.300}` | `color` | `--color-icon-base-tertiary` |
| `color.icon.base.default-on-color` | `{palette.black.1000}` | `color` | `--color-icon-base-default-on-color` |
| `color.icon.base.secondary-on-color` | `{palette.gray.600}` | `color` | `--color-icon-base-secondary-on-color` |
| `color.icon.brand.default` | `{palette.blue-sky.400}` | `color` | `--color-icon-brand-default` |
| `color.icon.brand.on-secondary` | `{palette.blue-sky.700}` | `color` | `--color-icon-brand-on-secondary` |
| `color.icon.brand.visited` | `{palette.magenta.400}` | `color` | `--color-icon-brand-visited` |
| `color.icon.disabled.default` | `{palette.gray.600}` | `color` | `--color-icon-disabled-default` |
| `color.icon.disabled.on-disabled` | `{palette.gray.400}` | `color` | `--color-icon-disabled-on-disabled` |
| `color.icon.positive.default` | `{palette.green.500}` | `color` | `--color-icon-positive-default` |
| `color.icon.positive.on-secondary` | `{palette.green.800}` | `color` | `--color-icon-positive-on-secondary` |
| `color.icon.warning.default` | `{palette.apricot.500}` | `color` | `--color-icon-warning-default` |
| `color.icon.warning.on-secondary` | `{palette.apricot.800}` | `color` | `--color-icon-warning-on-secondary` |
| `color.icon.danger.default` | `{palette.red.500}` | `color` | `--color-icon-danger-default` |
| `color.icon.danger.on-secondary` | `{palette.red.800}` | `color` | `--color-icon-danger-on-secondary` |
| `color.icon.base-inverse.default` | `{palette.black.1000}` | `color` | `--color-icon-base-inverse-default` |
| `color.icon.base-inverse.on-color` | `{palette.white.1000}` | `color` | `--color-icon-base-inverse-on-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core.dark/color.tokens.json</code></summary>

```json
{
  "color": {
    "background": {
      "base": {
        "default": {
          "$value": "{palette.gray.900}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.gray.700}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.800}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.700}",
          "$type": "color"
        },
        "tertiary-active": {
          "$value": "{palette.gray.500}",
          "$type": "color"
        },
        "tertiary-hover": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.gray.800}",
          "$type": "color"
        },
        "secondary-hover": {
          "$value": "{palette.gray.700}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.600}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.blue-sky.800}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.blue-sky.200}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.blue-sky.400}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.blue-sky.700}",
          "$type": "color"
        },
        "secondary-hover": {
          "$value": "{palette.blue-sky.300}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.blue-sky.900}",
          "$type": "color"
        },
        "focus-ring": {
          "$value": "{palette.blue-sky.500}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.700}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.green.900}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.green.200}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.green.300}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.green.800}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.500}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.apricot.700}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.apricot.200}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.apricot.300}",
          "$type": "color"
        },
        "accent": {
          "$value": "{palette.apricot.400}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.apricot.600}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.700}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.red.900}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.red.200}",
          "$type": "color"
        },
        "secondary-active": {
          "$value": "{palette.red.400}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.red.800}",
          "$type": "color"
        },
        "secondary-hover": {
          "$value": "{palette.red.300}",
          "$type": "color"
        }
      },
      "base-inverse": {
        "default": {
          "$value": "{palette.gray.100}",
          "$type": "color"
        },
        "default-active": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.gray.200}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.700}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.400}",
          "$type": "color"
        }
      },
      "alpha": {
        "overlay-dark": {
          "$value": "{palette.alpha.black.200-alpha}",
          "$type": "color"
        },
        "overlay-light": {
          "$value": "{palette.alpha.white.200-alpha}",
          "$type": "color"
        },
        "large-surface": {
          "$value": "{palette.alpha.gray.alpha-200}",
          "$type": "color"
        }
      }
    },
    "border": {
      "base": {
        "default": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.400}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.250}",
          "$type": "color"
        },
        "strong": {
          "$value": "{palette.gray.100}",
          "$type": "color"
        },
        "subtle": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.500}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.500}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.500}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.500}",
          "$type": "color"
        }
      }
    },
    "text": {
      "base": {
        "default": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.200}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        },
        "default-on-color": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "secondary-on-color": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        }
      },
      "base-inverse": {
        "default": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "on-color": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.400}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.blue-sky.700}",
          "$type": "color"
        },
        "default-hover": {
          "$value": "{palette.blue-sky.700}",
          "$type": "color"
        },
        "visited": {
          "$value": "{palette.magenta.400}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "on-disabled": {
          "$value": "{palette.gray.400}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.500}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.green.800}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.500}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.apricot.800}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.500}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.red.800}",
          "$type": "color"
        }
      }
    },
    "icon": {
      "base": {
        "default": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        },
        "secondary": {
          "$value": "{palette.gray.200}",
          "$type": "color"
        },
        "tertiary": {
          "$value": "{palette.gray.300}",
          "$type": "color"
        },
        "default-on-color": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "secondary-on-color": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        }
      },
      "brand": {
        "default": {
          "$value": "{palette.blue-sky.400}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.blue-sky.700}",
          "$type": "color"
        },
        "visited": {
          "$value": "{palette.magenta.400}",
          "$type": "color"
        }
      },
      "disabled": {
        "default": {
          "$value": "{palette.gray.600}",
          "$type": "color"
        },
        "on-disabled": {
          "$value": "{palette.gray.400}",
          "$type": "color"
        }
      },
      "positive": {
        "default": {
          "$value": "{palette.green.500}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.green.800}",
          "$type": "color"
        }
      },
      "warning": {
        "default": {
          "$value": "{palette.apricot.500}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.apricot.800}",
          "$type": "color"
        }
      },
      "danger": {
        "default": {
          "$value": "{palette.red.500}",
          "$type": "color"
        },
        "on-secondary": {
          "$value": "{palette.red.800}",
          "$type": "color"
        }
      },
      "base-inverse": {
        "default": {
          "$value": "{palette.black.1000}",
          "$type": "color"
        },
        "on-color": {
          "$value": "{palette.white.1000}",
          "$type": "color"
        }
      }
    }
  }
}
```

</details>

#### `tokens/core.dark/components/badge.tokens.json`

_1 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `badge.text.positive` | `{color.text.base-inverse.on-color}` | `color` | `--badge-text-positive` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core.dark/components/badge.tokens.json</code></summary>

```json
{
  "badge": {
    "text": {
      "positive": {
        "$value": "{color.text.base-inverse.on-color}",
        "$type": "color",
        "$comment": "Dark-theme override: the positive background drops to #027948, so white text gives ~5.5:1. (Light theme keeps #121212 dark text on the brighter #039855; dark text on #027948 would be 3.4:1 and fail WCAG AA.)"
      }
    }
  }
}
```

</details>

### 5.3 Tokens de componentă — `tokens/core/components/`

#### `tokens/core/components/accordion.tokens.json`

_46 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `accordion.item.borderColor.default` | `{color.border.base.default}` | `color` | `--accordion-item-border-color-default` |
| `accordion.item.borderWidth` | `{borderWidth.1}` | `dimension` | `--accordion-item-border-width` |
| `accordion.item.transitionDuration` | `150ms` | `duration` | `--accordion-item-transition-duration` |
| `accordion.item.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--accordion-item-transition-timing-function` |
| `accordion.item.paddingBlock.desktop` | `{spacing.32}` | `dimension` | `--accordion-item-padding-block-desktop` |
| `accordion.item.paddingBlock.mobile` | `{spacing.24}` | `dimension` | `--accordion-item-padding-block-mobile` |
| `accordion.item.paddingInline.tinted` | `{spacing.20}` | `dimension` | `--accordion-item-padding-inline-tinted` |
| `accordion.item.gap` | `{spacing.12}` | `dimension` | `--accordion-item-gap` |
| `accordion.item.container.background.default` | `{color.background.base.default}` | `color` | `--accordion-item-container-background-default` |
| `accordion.item.container.background.hover` | `{color.background.base.secondary}` | `color` | `--accordion-item-container-background-hover` |
| `accordion.item.container.background.trailSites` | `{color.background.brand.secondary}` | `color` | `--accordion-item-container-background-trail-sites` |
| `accordion.item.heading.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--accordion-item-heading-font-family` |
| `accordion.item.heading.fontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--accordion-item-heading-font-weight` |
| `accordion.item.heading.fontSize.desktop` | `{fontSize.24}` | `dimension` | `--accordion-item-heading-font-size-desktop` |
| `accordion.item.heading.fontSize.mobile` | `{fontSize.22}` | `dimension` | `--accordion-item-heading-font-size-mobile` |
| `accordion.item.heading.lineHeight.desktop` | `{lineHeight.32}` | `dimension` | `--accordion-item-heading-line-height-desktop` |
| `accordion.item.heading.lineHeight.mobile` | `{lineHeight.30}` | `dimension` | `--accordion-item-heading-line-height-mobile` |
| `accordion.item.heading.letterSpacing` | `-0.01em` | `dimension` | `--accordion-item-heading-letter-spacing` |
| `accordion.item.heading.color.default` | `{color.text.base.default}` | `color` | `--accordion-item-heading-color-default` |
| `accordion.item.heading.color.open` | `{color.text.brand.default}` | `color` | `--accordion-item-heading-color-open` |
| `accordion.item.heading.color.disabled` | `{color.text.disabled.default}` | `color` | `--accordion-item-heading-color-disabled` |
| `accordion.item.heading.gap` | `{spacing.8}` | `dimension` | `--accordion-item-heading-gap` |
| `accordion.item.supporting.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--accordion-item-supporting-font-family` |
| `accordion.item.supporting.fontSize` | `{fontSize.16}` | `dimension` | `--accordion-item-supporting-font-size` |
| `accordion.item.supporting.lineHeight` | `{lineHeight.24}` | `dimension` | `--accordion-item-supporting-line-height` |
| `accordion.item.supporting.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--accordion-item-supporting-font-weight` |
| `accordion.item.supporting.color.default` | `{color.text.base.tertiary}` | `color` | `--accordion-item-supporting-color-default` |
| `accordion.item.supporting.color.disabled` | `{color.text.disabled.default}` | `color` | `--accordion-item-supporting-color-disabled` |
| `accordion.item.trigger.size` | `{spacing.48}` | `dimension` | `--accordion-item-trigger-size` |
| `accordion.item.trigger.iconSize` | `{spacing.20}` | `dimension` | `--accordion-item-trigger-icon-size` |
| `accordion.item.trigger.borderRadius` | `{borderRadius.full}` | `dimension` | `--accordion-item-trigger-border-radius` |
| `accordion.item.trigger.color.default` | `{color.icon.base.default}` | `color` | `--accordion-item-trigger-color-default` |
| `accordion.item.trigger.color.open` | `{color.icon.brand.default}` | `color` | `--accordion-item-trigger-color-open` |
| `accordion.item.trigger.color.disabled` | `{color.icon.disabled.default}` | `color` | `--accordion-item-trigger-color-disabled` |
| `accordion.item.panel.paddingBlockEnd` | `{spacing.32}` | `dimension` | `--accordion-item-panel-padding-block-end` |
| `accordion.item.panel.paddingInlineStart` | `{spacing.24}` | `dimension` | `--accordion-item-panel-padding-inline-start` |
| `accordion.item.panel.paddingInlineEnd` | `{spacing.48}` | `dimension` | `--accordion-item-panel-padding-inline-end` |
| `accordion.item.panel.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--accordion-item-panel-font-family` |
| `accordion.item.panel.fontSize` | `{fontSize.16}` | `dimension` | `--accordion-item-panel-font-size` |
| `accordion.item.panel.lineHeight` | `{lineHeight.24}` | `dimension` | `--accordion-item-panel-line-height` |
| `accordion.item.panel.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--accordion-item-panel-font-weight` |
| `accordion.item.panel.color.default` | `{color.text.base.secondary}` | `color` | `--accordion-item-panel-color-default` |
| `accordion.item.panel.color.trailSites` | `{color.text.base.default-on-color}` | `color` | `--accordion-item-panel-color-trail-sites` |
| `accordion.item.trailSites.heading.color` | `{color.text.brand.on-secondary}` | `color` | `--accordion-item-trail-sites-heading-color` |
| `accordion.item.trailSites.supporting.color` | `{color.text.base.secondary-on-color}` | `color` | `--accordion-item-trail-sites-supporting-color` |
| `accordion.item.trailSites.trigger.color` | `{color.icon.brand.on-secondary}` | `color` | `--accordion-item-trail-sites-trigger-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/accordion.tokens.json</code></summary>

```json
{
  "accordion": {
    "item": {
      "borderColor": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" }
      },
      "borderWidth": {
        "$value": "{borderWidth.1}",
        "$type": "dimension"
      },
      "transitionDuration": {
        "$value": "150ms",
        "$type": "duration"
      },
      "transitionTimingFunction": {
        "$value": "ease-out",
        "$type": "cubicBezier"
      },
      "paddingBlock": {
        "desktop": { "$value": "{spacing.32}", "$type": "dimension" },
        "mobile": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "paddingInline": {
        "tinted": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "gap": {
        "$value": "{spacing.12}",
        "$type": "dimension"
      },
      "container": {
        "background": {
          "default": { "$value": "{color.background.base.default}", "$type": "color" },
          "hover": { "$value": "{color.background.base.secondary}", "$type": "color" },
          "trailSites": { "$value": "{color.background.brand.secondary}", "$type": "color" }
        }
      },
      "heading": {
        "fontFamily": {
          "$value": "{fontFamily.primary}",
          "$type": "fontFamily"
        },
        "fontWeight": {
          "$value": "{fontWeight.semibold}",
          "$type": "fontWeight"
        },
        "fontSize": {
          "desktop": { "$value": "{fontSize.24}", "$type": "dimension" },
          "mobile": { "$value": "{fontSize.22}", "$type": "dimension" }
        },
        "lineHeight": {
          "desktop": { "$value": "{lineHeight.32}", "$type": "dimension" },
          "mobile": { "$value": "{lineHeight.30}", "$type": "dimension" }
        },
        "letterSpacing": {
          "$value": "-0.01em",
          "$type": "dimension"
        },
        "color": {
          "default": { "$value": "{color.text.base.default}", "$type": "color" },
          "open": { "$value": "{color.text.brand.default}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "gap": {
          "$value": "{spacing.8}",
          "$type": "dimension"
        }
      },
      "supporting": {
        "fontFamily": {
          "$value": "{fontFamily.primary}",
          "$type": "fontFamily"
        },
        "fontSize": {
          "$value": "{fontSize.16}",
          "$type": "dimension"
        },
        "lineHeight": {
          "$value": "{lineHeight.24}",
          "$type": "dimension"
        },
        "fontWeight": {
          "$value": "{fontWeight.regular}",
          "$type": "fontWeight"
        },
        "color": {
          "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        }
      },
      "trigger": {
        "size": {
          "$value": "{spacing.48}",
          "$type": "dimension"
        },
        "iconSize": {
          "$value": "{spacing.20}",
          "$type": "dimension"
        },
        "borderRadius": {
          "$value": "{borderRadius.full}",
          "$type": "dimension"
        },
        "color": {
          "default": { "$value": "{color.icon.base.default}", "$type": "color" },
          "open": { "$value": "{color.icon.brand.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      },
      "panel": {
        "paddingBlockEnd": {
          "$value": "{spacing.32}",
          "$type": "dimension"
        },
        "paddingInlineStart": {
          "$value": "{spacing.24}",
          "$type": "dimension"
        },
        "paddingInlineEnd": {
          "$value": "{spacing.48}",
          "$type": "dimension"
        },
        "fontFamily": {
          "$value": "{fontFamily.primary}",
          "$type": "fontFamily"
        },
        "fontSize": {
          "$value": "{fontSize.16}",
          "$type": "dimension"
        },
        "lineHeight": {
          "$value": "{lineHeight.24}",
          "$type": "dimension"
        },
        "fontWeight": {
          "$value": "{fontWeight.regular}",
          "$type": "fontWeight"
        },
        "color": {
          "default": {
            "$value": "{color.text.base.secondary}",
            "$type": "color"
          },
          "trailSites": {
            "$value": "{color.text.base.default-on-color}",
            "$type": "color"
          }
        }
      },
      "trailSites": {
        "heading": {
          "color": {
            "$value": "{color.text.brand.on-secondary}",
            "$type": "color"
          }
        },
        "supporting": {
          "color": {
            "$value": "{color.text.base.secondary-on-color}",
            "$type": "color"
          }
        },
        "trigger": {
          "color": {
            "$value": "{color.icon.brand.on-secondary}",
            "$type": "color"
          }
        }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/avatar.tokens.json`

_39 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `avatar.container.size.xs` | `{spacing.24}` | `dimension` | `--avatar-container-size-xs` |
| `avatar.container.size.sm` | `{spacing.32}` | `dimension` | `--avatar-container-size-sm` |
| `avatar.container.size.md` | `{spacing.40}` | `dimension` | `--avatar-container-size-md` |
| `avatar.container.size.lg` | `{spacing.48}` | `dimension` | `--avatar-container-size-lg` |
| `avatar.container.size.xl` | `72px` | `dimension` | `--avatar-container-size-xl` |
| `avatar.container.borderRadius` | `{borderRadius.full}` | `dimension` | `--avatar-container-border-radius` |
| `avatar.container.background` | `{color.background.base.tertiary}` | `color` | `--avatar-container-background` |
| `avatar.container.transitionDuration` | `150ms` | `duration` | `--avatar-container-transition-duration` |
| `avatar.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--avatar-container-transition-timing-function` |
| `avatar.stack.borderWidth` | `{borderWidth.2}` | `dimension` | `--avatar-stack-border-width` |
| `avatar.stack.borderColor` | `{color.background.base.default}` | `color` | `--avatar-stack-border-color` |
| `avatar.stack.overlap.xs` | `-8px` | `dimension` | `--avatar-stack-overlap-xs` |
| `avatar.stack.overlap.sm` | `-10px` | `dimension` | `--avatar-stack-overlap-sm` |
| `avatar.stack.overlap.md` | `-12px` | `dimension` | `--avatar-stack-overlap-md` |
| `avatar.stack.overlap.lg` | `-14px` | `dimension` | `--avatar-stack-overlap-lg` |
| `avatar.stack.overlap.xl` | `-20px` | `dimension` | `--avatar-stack-overlap-xl` |
| `avatar.initials.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--avatar-initials-font-family` |
| `avatar.initials.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--avatar-initials-font-weight` |
| `avatar.initials.fontSize.xs` | `{fontSize.12}` | `dimension` | `--avatar-initials-font-size-xs` |
| `avatar.initials.fontSize.sm` | `{fontSize.14}` | `dimension` | `--avatar-initials-font-size-sm` |
| `avatar.initials.fontSize.md` | `{fontSize.14}` | `dimension` | `--avatar-initials-font-size-md` |
| `avatar.initials.fontSize.lg` | `{fontSize.16}` | `dimension` | `--avatar-initials-font-size-lg` |
| `avatar.initials.fontSize.xl` | `{fontSize.18}` | `dimension` | `--avatar-initials-font-size-xl` |
| `avatar.initials.lineHeight.xs` | `{lineHeight.16}` | `dimension` | `--avatar-initials-line-height-xs` |
| `avatar.initials.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--avatar-initials-line-height-sm` |
| `avatar.initials.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--avatar-initials-line-height-md` |
| `avatar.initials.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--avatar-initials-line-height-lg` |
| `avatar.initials.lineHeight.xl` | `{lineHeight.28}` | `dimension` | `--avatar-initials-line-height-xl` |
| `avatar.initials.color` | `{color.text.base.secondary}` | `color` | `--avatar-initials-color` |
| `avatar.icon.size.xs` | `{spacing.12}` | `dimension` | `--avatar-icon-size-xs` |
| `avatar.icon.size.sm` | `{spacing.16}` | `dimension` | `--avatar-icon-size-sm` |
| `avatar.icon.size.md` | `{spacing.20}` | `dimension` | `--avatar-icon-size-md` |
| `avatar.icon.size.lg` | `{spacing.24}` | `dimension` | `--avatar-icon-size-lg` |
| `avatar.icon.size.xl` | `{spacing.32}` | `dimension` | `--avatar-icon-size-xl` |
| `avatar.icon.color` | `{color.icon.base.secondary}` | `color` | `--avatar-icon-color` |
| `avatar.focus.ringInnerColor` | `{focusRing.color.inner}` | `color` | `--avatar-focus-ring-inner-color` |
| `avatar.focus.ringOuterColor` | `{focusRing.color.outer}` | `color` | `--avatar-focus-ring-outer-color` |
| `avatar.focus.ringInnerWidth` | `{focusRing.width.inner}` | `dimension` | `--avatar-focus-ring-inner-width` |
| `avatar.focus.ringOuterWidth` | `{focusRing.width.outer}` | `dimension` | `--avatar-focus-ring-outer-width` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/avatar.tokens.json</code></summary>

```json
{
  "avatar": {
    "container": {
      "size": {
        "xs": { "$value": "{spacing.24}", "$type": "dimension" },
        "sm": { "$value": "{spacing.32}", "$type": "dimension" },
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" },
        "xl": { "$value": "72px", "$type": "dimension" }
      },
      "borderRadius": {
        "$value": "{borderRadius.full}",
        "$type": "dimension"
      },
      "background": {
        "$value": "{color.background.base.tertiary}",
        "$type": "color"
      },
      "transitionDuration": {
        "$value": "150ms",
        "$type": "duration"
      },
      "transitionTimingFunction": {
        "$value": "ease-out",
        "$type": "cubicBezier"
      }
    },
    "stack": {
      "borderWidth": {
        "$value": "{borderWidth.2}",
        "$type": "dimension"
      },
      "borderColor": {
        "$value": "{color.background.base.default}",
        "$type": "color"
      },
      "overlap": {
        "xs": { "$value": "-8px", "$type": "dimension" },
        "sm": { "$value": "-10px", "$type": "dimension" },
        "md": { "$value": "-12px", "$type": "dimension" },
        "lg": { "$value": "-14px", "$type": "dimension" },
        "xl": { "$value": "-20px", "$type": "dimension" }
      }
    },
    "initials": {
      "fontFamily": {
        "$value": "{fontFamily.primary}",
        "$type": "fontFamily"
      },
      "fontWeight": {
        "$value": "{fontWeight.medium}",
        "$type": "fontWeight"
      },
      "fontSize": {
        "xs": { "$value": "{fontSize.12}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" },
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" },
        "xl": { "$value": "{fontSize.18}", "$type": "dimension" }
      },
      "lineHeight": {
        "xs": { "$value": "{lineHeight.16}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" },
        "xl": { "$value": "{lineHeight.28}", "$type": "dimension" }
      },
      "color": {
        "$value": "{color.text.base.secondary}",
        "$type": "color"
      }
    },
    "icon": {
      "size": {
        "xs": { "$value": "{spacing.12}", "$type": "dimension" },
        "sm": { "$value": "{spacing.16}", "$type": "dimension" },
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" },
        "xl": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "color": {
        "$value": "{color.icon.base.secondary}",
        "$type": "color"
      }
    },
    "focus": {
      "ringInnerColor": {
        "$value": "{focusRing.color.inner}",
        "$type": "color"
      },
      "ringOuterColor": {
        "$value": "{focusRing.color.outer}",
        "$type": "color"
      },
      "ringInnerWidth": {
        "$value": "{focusRing.width.inner}",
        "$type": "dimension"
      },
      "ringOuterWidth": {
        "$value": "{focusRing.width.outer}",
        "$type": "dimension"
      }
    }
  }
}
```

</details>

#### `tokens/core/components/badge.tokens.json`

_43 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `badge.size.xs` | `{spacing.8}` | `dimension` | `--badge-size-xs` |
| `badge.size.sm` | `{spacing.12}` | `dimension` | `--badge-size-sm` |
| `badge.size.md` | `{spacing.16}` | `dimension` | `--badge-size-md` |
| `badge.size.lg` | `{spacing.20}` | `dimension` | `--badge-size-lg` |
| `badge.size.xl` | `{spacing.24}` | `dimension` | `--badge-size-xl` |
| `badge.minWidth.xs` | `{spacing.8}` | `dimension` | `--badge-min-width-xs` |
| `badge.minWidth.sm` | `{spacing.12}` | `dimension` | `--badge-min-width-sm` |
| `badge.minWidth.md` | `{spacing.16}` | `dimension` | `--badge-min-width-md` |
| `badge.minWidth.lg` | `{spacing.20}` | `dimension` | `--badge-min-width-lg` |
| `badge.minWidth.xl` | `{spacing.24}` | `dimension` | `--badge-min-width-xl` |
| `badge.padding.inline.xs` | `{spacing.0}` | `dimension` | `--badge-padding-inline-xs` |
| `badge.padding.inline.sm` | `{spacing.2}` | `dimension` | `--badge-padding-inline-sm` |
| `badge.padding.inline.md` | `{spacing.2}` | `dimension` | `--badge-padding-inline-md` |
| `badge.padding.inline.lg` | `{spacing.4}` | `dimension` | `--badge-padding-inline-lg` |
| `badge.padding.inline.xl` | `{spacing.8}` | `dimension` | `--badge-padding-inline-xl` |
| `badge.borderRadius` | `{borderRadius.full}` | `dimension` | `--badge-border-radius` |
| `badge.dot.size.md` | `{spacing.2}` | `dimension` | `--badge-dot-size-md` |
| `badge.dot.size.lg` | `3px` | `dimension` | `--badge-dot-size-lg` |
| `badge.dot.size.xl` | `3px` | `dimension` | `--badge-dot-size-xl` |
| `badge.fontSize.xs` | `{fontSize.10}` | `dimension` | `--badge-font-size-xs` |
| `badge.fontSize.sm` | `{fontSize.10}` | `dimension` | `--badge-font-size-sm` |
| `badge.fontSize.md` | `{fontSize.12}` | `dimension` | `--badge-font-size-md` |
| `badge.fontSize.lg` | `{fontSize.12}` | `dimension` | `--badge-font-size-lg` |
| `badge.fontSize.xl` | `{fontSize.14}` | `dimension` | `--badge-font-size-xl` |
| `badge.lineHeight.xs` | `{lineHeight.12}` | `dimension` | `--badge-line-height-xs` |
| `badge.lineHeight.sm` | `{lineHeight.12}` | `dimension` | `--badge-line-height-sm` |
| `badge.lineHeight.md` | `{lineHeight.16}` | `dimension` | `--badge-line-height-md` |
| `badge.lineHeight.lg` | `{lineHeight.16}` | `dimension` | `--badge-line-height-lg` |
| `badge.lineHeight.xl` | `{lineHeight.20}` | `dimension` | `--badge-line-height-xl` |
| `badge.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--badge-font-weight` |
| `badge.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--badge-font-family` |
| `badge.background.default` | `{color.background.base-inverse.default}` | `color` | `--badge-background-default` |
| `badge.background.brand` | `{color.background.brand.default}` | `color` | `--badge-background-brand` |
| `badge.background.positive` | `{color.background.positive.default}` | `color` | `--badge-background-positive` |
| `badge.background.warning` | `{color.background.warning.default}` | `color` | `--badge-background-warning` |
| `badge.background.danger` | `{color.background.danger.default}` | `color` | `--badge-background-danger` |
| `badge.text.default` | `{color.text.base-inverse.default}` | `color` | `--badge-text-default` |
| `badge.text.brand` | `{color.text.base-inverse.on-color}` | `color` | `--badge-text-brand` |
| `badge.text.positive` | `{color.text.base.default-on-color}` | `color` | `--badge-text-positive` |
| `badge.text.warning` | `{color.text.base.default-on-color}` | `color` | `--badge-text-warning` |
| `badge.text.danger` | `{color.text.base-inverse.on-color}` | `color` | `--badge-text-danger` |
| `badge.offset.top` | `0px` | `dimension` | `--badge-offset-top` |
| `badge.offset.right` | `0px` | `dimension` | `--badge-offset-right` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/badge.tokens.json</code></summary>

```json
{
  "badge": {
    "size": {
      "xs": { "$value": "{spacing.8}", "$type": "dimension", "$comment": "Dot-only presence pip per Figma 551:18348." },
      "sm": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Compact dot / numbered per Figma 551:18343."
      },
      "md": { "$value": "{spacing.16}", "$type": "dimension", "$comment": "Default numbered/dot per Figma 797:43183." },
      "lg": { "$value": "{spacing.20}", "$type": "dimension", "$comment": "Emphasised numbered per Figma 797:43177." },
      "xl": { "$value": "{spacing.24}", "$type": "dimension", "$comment": "Large numbered per Figma 797:43169." }
    },
    "minWidth": {
      "xs": { "$value": "{spacing.8}", "$type": "dimension" },
      "sm": { "$value": "{spacing.12}", "$type": "dimension" },
      "md": { "$value": "{spacing.16}", "$type": "dimension" },
      "lg": { "$value": "{spacing.20}", "$type": "dimension" },
      "xl": { "$value": "{spacing.24}", "$type": "dimension" }
    },
    "padding": {
      "inline": {
        "xs": { "$value": "{spacing.0}", "$type": "dimension" },
        "sm": { "$value": "{spacing.2}", "$type": "dimension" },
        "md": { "$value": "{spacing.2}", "$type": "dimension", "$comment": "Per Figma 797:43183." },
        "lg": { "$value": "{spacing.4}", "$type": "dimension", "$comment": "Per Figma 797:43177." },
        "xl": { "$value": "{spacing.8}", "$type": "dimension", "$comment": "Per Figma 797:43169." }
      }
    },
    "borderRadius": {
      "$value": "{borderRadius.full}",
      "$type": "dimension"
    },
    "dot": {
      "size": {
        "md": {
          "$value": "{spacing.2}",
          "$type": "dimension",
          "$comment": "Inner presence pip, dot type only, per Figma 53:660 (2px)."
        },
        "lg": {
          "$value": "3px",
          "$type": "dimension",
          "$comment": "Inner presence pip per Figma 53:664 (3px; no 3px spacing primitive exists)."
        },
        "xl": { "$value": "3px", "$type": "dimension", "$comment": "Inner presence pip per Figma 797:43133 (3px)." }
      }
    },
    "fontSize": {
      "xs": {
        "$value": "{fontSize.10}",
        "$type": "dimension",
        "$comment": "Dot only — fontSize is unused but kept for API symmetry."
      },
      "sm": { "$value": "{fontSize.10}", "$type": "dimension" },
      "md": { "$value": "{fontSize.12}", "$type": "dimension", "$comment": "Figma Caption Medium 12/16." },
      "lg": { "$value": "{fontSize.12}", "$type": "dimension", "$comment": "Figma Caption Medium 12/16." },
      "xl": { "$value": "{fontSize.14}", "$type": "dimension", "$comment": "Figma Body/Small Medium 14/20." }
    },
    "lineHeight": {
      "xs": { "$value": "{lineHeight.12}", "$type": "dimension" },
      "sm": { "$value": "{lineHeight.12}", "$type": "dimension" },
      "md": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "lg": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "xl": { "$value": "{lineHeight.20}", "$type": "dimension" }
    },
    "fontWeight": {
      "$value": "{fontWeight.medium}",
      "$type": "fontWeight"
    },
    "fontFamily": {
      "$value": "{fontFamily.primary}",
      "$type": "fontFamily"
    },
    "background": {
      "default": {
        "$value": "{color.background.base-inverse.default}",
        "$type": "color"
      },
      "brand": {
        "$value": "{color.background.brand.default}",
        "$type": "color"
      },
      "positive": {
        "$value": "{color.background.positive.default}",
        "$type": "color"
      },
      "warning": {
        "$value": "{color.background.warning.default}",
        "$type": "color"
      },
      "danger": {
        "$value": "{color.background.danger.default}",
        "$type": "color"
      }
    },
    "text": {
      "default": {
        "$value": "{color.text.base-inverse.default}",
        "$type": "color"
      },
      "brand": {
        "$value": "{color.text.base-inverse.on-color}",
        "$type": "color"
      },
      "positive": {
        "$value": "{color.text.base.default-on-color}",
        "$type": "color",
        "$comment": "Dark text — white on #039855 is 3.73:1 (fails WCAG AA); #121212 gives ~4.7:1."
      },
      "warning": {
        "$value": "{color.text.base.default-on-color}",
        "$type": "color",
        "$comment": "Dark text on amber per Figma accent badge — white is 1.84:1 (fails AA); #121212 gives ~9.9:1."
      },
      "danger": {
        "$value": "{color.text.base-inverse.on-color}",
        "$type": "color"
      }
    },
    "offset": {
      "top": {
        "$value": "0px",
        "$type": "dimension"
      },
      "right": {
        "$value": "0px",
        "$type": "dimension"
      }
    }
  }
}
```

</details>

#### `tokens/core/components/breadcrumb.tokens.json`

_38 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `breadcrumb.gap.item` | `{spacing.4}` | `dimension` | `--breadcrumb-gap-item` |
| `breadcrumb.gap.menu` | `{spacing.4}` | `dimension` | `--breadcrumb-gap-menu` |
| `breadcrumb.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--breadcrumb-font-family` |
| `breadcrumb.fontSize` | `{fontSize.14}` | `dimension` | `--breadcrumb-font-size` |
| `breadcrumb.lineHeight` | `{lineHeight.20}` | `dimension` | `--breadcrumb-line-height` |
| `breadcrumb.fontWeight.default` | `{fontWeight.regular}` | `fontWeight` | `--breadcrumb-font-weight-default` |
| `breadcrumb.fontWeight.active` | `{fontWeight.medium}` | `fontWeight` | `--breadcrumb-font-weight-active` |
| `breadcrumb.item.text.default` | `{color.text.base.tertiary}` | `color` | `--breadcrumb-item-text-default` |
| `breadcrumb.item.text.hover` | `{color.text.base.secondary}` | `color` | `--breadcrumb-item-text-hover` |
| `breadcrumb.item.text.active` | `{color.text.base.default}` | `color` | `--breadcrumb-item-text-active` |
| `breadcrumb.item.text.visited` | `{color.text.brand.visited}` | `color` | `--breadcrumb-item-text-visited` |
| `breadcrumb.item.text.disabled` | `{color.text.disabled.default}` | `color` | `--breadcrumb-item-text-disabled` |
| `breadcrumb.item.borderRadius` | `{borderRadius.4}` | `dimension` | `--breadcrumb-item-border-radius` |
| `breadcrumb.item.padding.inline` | `{spacing.2}` | `dimension` | `--breadcrumb-item-padding-inline` |
| `breadcrumb.item.padding.block` | `{spacing.0}` | `dimension` | `--breadcrumb-item-padding-block` |
| `breadcrumb.separator.color` | `{color.icon.base.tertiary}` | `color` | `--breadcrumb-separator-color` |
| `breadcrumb.separator.size` | `{spacing.16}` | `dimension` | `--breadcrumb-separator-size` |
| `breadcrumb.icon.color` | `{color.icon.base.tertiary}` | `color` | `--breadcrumb-icon-color` |
| `breadcrumb.icon.colorActive` | `{color.icon.base.default}` | `color` | `--breadcrumb-icon-color-active` |
| `breadcrumb.icon.colorDisabled` | `{color.icon.disabled.default}` | `color` | `--breadcrumb-icon-color-disabled` |
| `breadcrumb.icon.size` | `{spacing.16}` | `dimension` | `--breadcrumb-icon-size` |
| `breadcrumb.back.icon.size` | `{spacing.20}` | `dimension` | `--breadcrumb-back-icon-size` |
| `breadcrumb.back.icon.color` | `{color.icon.base.tertiary}` | `color` | `--breadcrumb-back-icon-color` |
| `breadcrumb.back.gap` | `{spacing.4}` | `dimension` | `--breadcrumb-back-gap` |
| `breadcrumb.menu.background` | `{color.background.base.default}` | `color` | `--breadcrumb-menu-background` |
| `breadcrumb.menu.border.color` | `{color.border.base.default}` | `color` | `--breadcrumb-menu-border-color` |
| `breadcrumb.menu.border.width` | `1px` | `dimension` | `--breadcrumb-menu-border-width` |
| `breadcrumb.menu.border.radius` | `{borderRadius.8}` | `dimension` | `--breadcrumb-menu-border-radius` |
| `breadcrumb.menu.shadow` | `{dropShadow.200}` | `string` | `--breadcrumb-menu-shadow` |
| `breadcrumb.menu.minWidth` | `160px` | `dimension` | `--breadcrumb-menu-min-width` |
| `breadcrumb.menu.padding` | `{spacing.8}` | `dimension` | `--breadcrumb-menu-padding` |
| `breadcrumb.menu.item.padding.inline` | `{spacing.12}` | `dimension` | `--breadcrumb-menu-item-padding-inline` |
| `breadcrumb.menu.item.padding.block` | `{spacing.8}` | `dimension` | `--breadcrumb-menu-item-padding-block` |
| `breadcrumb.menu.item.borderRadius` | `{borderRadius.6}` | `dimension` | `--breadcrumb-menu-item-border-radius` |
| `breadcrumb.menu.item.text.default` | `{color.text.base.default}` | `color` | `--breadcrumb-menu-item-text-default` |
| `breadcrumb.menu.item.text.hover` | `{color.text.base.default}` | `color` | `--breadcrumb-menu-item-text-hover` |
| `breadcrumb.menu.item.background.default` | `{color.background.base.default}` | `color` | `--breadcrumb-menu-item-background-default` |
| `breadcrumb.menu.item.background.hover` | `{color.background.base.default-hover}` | `color` | `--breadcrumb-menu-item-background-hover` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/breadcrumb.tokens.json</code></summary>

```json
{
  "breadcrumb": {
    "gap": {
      "item": { "$value": "{spacing.4}", "$type": "dimension" },
      "menu": { "$value": "{spacing.4}", "$type": "dimension" }
    },
    "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
    "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
    "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
    "fontWeight": {
      "default": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "active": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "item": {
      "text": {
        "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "hover": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "active": { "$value": "{color.text.base.default}", "$type": "color" },
        "visited": { "$value": "{color.text.brand.visited}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
      },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "padding": {
        "inline": { "$value": "{spacing.2}", "$type": "dimension" },
        "block": { "$value": "{spacing.0}", "$type": "dimension" }
      }
    },
    "separator": {
      "color": { "$value": "{color.icon.base.tertiary}", "$type": "color" },
      "size": { "$value": "{spacing.16}", "$type": "dimension" }
    },
    "icon": {
      "color": { "$value": "{color.icon.base.tertiary}", "$type": "color" },
      "colorActive": { "$value": "{color.icon.base.default}", "$type": "color" },
      "colorDisabled": { "$value": "{color.icon.disabled.default}", "$type": "color" },
      "size": { "$value": "{spacing.16}", "$type": "dimension" }
    },
    "back": {
      "icon": {
        "size": { "$value": "{spacing.20}", "$type": "dimension" },
        "color": { "$value": "{color.icon.base.tertiary}", "$type": "color" }
      },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" }
    },
    "menu": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "border": {
        "color": { "$value": "{color.border.base.default}", "$type": "color" },
        "width": { "$value": "1px", "$type": "dimension" },
        "radius": { "$value": "{borderRadius.8}", "$type": "dimension" }
      },
      "shadow": { "$value": "{dropShadow.200}", "$type": "string" },
      "minWidth": { "$value": "160px", "$type": "dimension" },
      "padding": { "$value": "{spacing.8}", "$type": "dimension" },
      "item": {
        "padding": {
          "inline": { "$value": "{spacing.12}", "$type": "dimension" },
          "block": { "$value": "{spacing.8}", "$type": "dimension" }
        },
        "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
        "text": {
          "default": { "$value": "{color.text.base.default}", "$type": "color" },
          "hover": { "$value": "{color.text.base.default}", "$type": "color" }
        },
        "background": {
          "default": { "$value": "{color.background.base.default}", "$type": "color" },
          "hover": { "$value": "{color.background.base.default-hover}", "$type": "color" }
        }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/button-group.tokens.json`

_2 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `button-group.gap.horizontal` | `{spacing.12}` | `dimension` | `--button-group-gap-horizontal` |
| `button-group.gap.vertical` | `{spacing.12}` | `dimension` | `--button-group-gap-vertical` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/button-group.tokens.json</code></summary>

```json
{
  "button-group": {
    "gap": {
      "horizontal": { "$value": "{spacing.12}", "$type": "dimension" },
      "vertical": { "$value": "{spacing.12}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/button.tokens.json`

_150 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `button.container.height.sm` | `{spacing.32}` | `dimension` | `--button-container-height-sm` |
| `button.container.height.md` | `{spacing.40}` | `dimension` | `--button-container-height-md` |
| `button.container.height.lg` | `{spacing.48}` | `dimension` | `--button-container-height-lg` |
| `button.container.touchTarget.sm` | `{spacing.40}` | `dimension` | `--button-container-touch-target-sm` |
| `button.container.touchTarget.md` | `{spacing.48}` | `dimension` | `--button-container-touch-target-md` |
| `button.container.touchTarget.lg` | `{spacing.48}` | `dimension` | `--button-container-touch-target-lg` |
| `button.container.maxWidth` | `400px` | `dimension` | `--button-container-max-width` |
| `button.container.minWidth.sm` | `52px` | `dimension` | `--button-container-min-width-sm` |
| `button.container.minWidth.md` | `{spacing.56}` | `dimension` | `--button-container-min-width-md` |
| `button.container.minWidth.lg` | `72px` | `dimension` | `--button-container-min-width-lg` |
| `button.container.paddingInline.sm` | `{spacing.12}` | `dimension` | `--button-container-padding-inline-sm` |
| `button.container.paddingInline.md` | `{spacing.16}` | `dimension` | `--button-container-padding-inline-md` |
| `button.container.paddingInline.lg` | `{spacing.20}` | `dimension` | `--button-container-padding-inline-lg` |
| `button.container.paddingInlineIcon.sm` | `{spacing.8}` | `dimension` | `--button-container-padding-inline-icon-sm` |
| `button.container.paddingInlineIcon.md` | `{spacing.12}` | `dimension` | `--button-container-padding-inline-icon-md` |
| `button.container.paddingInlineIcon.lg` | `{spacing.16}` | `dimension` | `--button-container-padding-inline-icon-lg` |
| `button.container.gap.sm` | `{spacing.6}` | `dimension` | `--button-container-gap-sm` |
| `button.container.gap.md` | `{spacing.6}` | `dimension` | `--button-container-gap-md` |
| `button.container.gap.lg` | `{spacing.6}` | `dimension` | `--button-container-gap-lg` |
| `button.container.borderRadius.rectangular.sm` | `{borderRadius.6}` | `dimension` | `--button-container-border-radius-rectangular-sm` |
| `button.container.borderRadius.rectangular.md` | `{borderRadius.6}` | `dimension` | `--button-container-border-radius-rectangular-md` |
| `button.container.borderRadius.rectangular.lg` | `{borderRadius.8}` | `dimension` | `--button-container-border-radius-rectangular-lg` |
| `button.container.borderRadius.circular.sm` | `{borderRadius.full}` | `dimension` | `--button-container-border-radius-circular-sm` |
| `button.container.borderRadius.circular.md` | `{borderRadius.full}` | `dimension` | `--button-container-border-radius-circular-md` |
| `button.container.borderRadius.circular.lg` | `{borderRadius.full}` | `dimension` | `--button-container-border-radius-circular-lg` |
| `button.container.transitionDuration` | `150ms` | `duration` | `--button-container-transition-duration` |
| `button.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--button-container-transition-timing-function` |
| `button.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--button-label-font-family` |
| `button.label.fontSize.sm` | `{fontSize.14}` | `dimension` | `--button-label-font-size-sm` |
| `button.label.fontSize.md` | `{fontSize.14}` | `dimension` | `--button-label-font-size-md` |
| `button.label.fontSize.lg` | `{fontSize.16}` | `dimension` | `--button-label-font-size-lg` |
| `button.label.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--button-label-line-height-sm` |
| `button.label.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--button-label-line-height-md` |
| `button.label.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--button-label-line-height-lg` |
| `button.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--button-label-font-weight` |
| `button.icon.size.sm` | `{spacing.16}` | `dimension` | `--button-icon-size-sm` |
| `button.icon.size.md` | `{spacing.20}` | `dimension` | `--button-icon-size-md` |
| `button.icon.size.lg` | `{spacing.20}` | `dimension` | `--button-icon-size-lg` |
| `button.primary.background.default` | `{color.background.brand.default}` | `color` | `--button-primary-background-default` |
| `button.primary.background.hover` | `{color.background.brand.default-hover}` | `color` | `--button-primary-background-hover` |
| `button.primary.background.active` | `{color.background.brand.default-active}` | `color` | `--button-primary-background-active` |
| `button.primary.label` | `{color.text.base-inverse.on-color}` | `color` | `--button-primary-label` |
| `button.primary.icon` | `{color.icon.base-inverse.on-color}` | `color` | `--button-primary-icon` |
| `button.secondary.background.default` | `{color.background.brand.secondary}` | `color` | `--button-secondary-background-default` |
| `button.secondary.background.hover` | `{color.background.brand.secondary-hover}` | `color` | `--button-secondary-background-hover` |
| `button.secondary.background.active` | `{color.background.brand.secondary-active}` | `color` | `--button-secondary-background-active` |
| `button.secondary.label` | `{color.text.brand.on-secondary}` | `color` | `--button-secondary-label` |
| `button.secondary.icon` | `{color.icon.brand.on-secondary}` | `color` | `--button-secondary-icon` |
| `button.strict.background.default` | `{color.background.base-inverse.default}` | `color` | `--button-strict-background-default` |
| `button.strict.background.hover` | `{color.background.base-inverse.default-hover}` | `color` | `--button-strict-background-hover` |
| `button.strict.background.active` | `{color.background.base-inverse.default-active}` | `color` | `--button-strict-background-active` |
| `button.strict.label` | `{color.text.base-inverse.default}` | `color` | `--button-strict-label` |
| `button.strict.icon` | `{color.icon.base-inverse.default}` | `color` | `--button-strict-icon` |
| `button.neutral.background.default` | `{color.background.base.tertiary}` | `color` | `--button-neutral-background-default` |
| `button.neutral.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--button-neutral-background-hover` |
| `button.neutral.background.active` | `{color.background.base.tertiary-active}` | `color` | `--button-neutral-background-active` |
| `button.neutral.label` | `{color.text.base.default}` | `color` | `--button-neutral-label` |
| `button.neutral.icon` | `{color.icon.base.default}` | `color` | `--button-neutral-icon` |
| `button.destructive.background.default` | `{color.background.danger.default}` | `color` | `--button-destructive-background-default` |
| `button.destructive.background.hover` | `{color.background.danger.default-hover}` | `color` | `--button-destructive-background-hover` |
| `button.destructive.background.active` | `{color.background.danger.default-active}` | `color` | `--button-destructive-background-active` |
| `button.destructive.label` | `{color.text.base-inverse.on-color}` | `color` | `--button-destructive-label` |
| `button.destructive.icon` | `{color.icon.base-inverse.on-color}` | `color` | `--button-destructive-icon` |
| `button.disabled.background` | `{color.background.disabled.default}` | `color` | `--button-disabled-background` |
| `button.disabled.label` | `{color.text.disabled.on-disabled}` | `color` | `--button-disabled-label` |
| `button.disabled.icon` | `{color.icon.disabled.on-disabled}` | `color` | `--button-disabled-icon` |
| `button.outlined.primary.border.default` | `{color.border.brand.default}` | `color` | `--button-outlined-primary-border-default` |
| `button.outlined.primary.border.hover` | `transparent` | `color` | `--button-outlined-primary-border-hover` |
| `button.outlined.primary.border.active` | `transparent` | `color` | `--button-outlined-primary-border-active` |
| `button.outlined.primary.border.disabled` | `{color.border.disabled.default}` | `color` | `--button-outlined-primary-border-disabled` |
| `button.outlined.primary.background.default` | `transparent` | `color` | `--button-outlined-primary-background-default` |
| `button.outlined.primary.background.hover` | `{color.background.brand.default-hover}` | `color` | `--button-outlined-primary-background-hover` |
| `button.outlined.primary.background.active` | `{color.background.brand.default-active}` | `color` | `--button-outlined-primary-background-active` |
| `button.outlined.primary.background.disabled` | `transparent` | `color` | `--button-outlined-primary-background-disabled` |
| `button.outlined.primary.label.default` | `{color.text.brand.default}` | `color` | `--button-outlined-primary-label-default` |
| `button.outlined.primary.label.hover` | `{color.text.base-inverse.on-color}` | `color` | `--button-outlined-primary-label-hover` |
| `button.outlined.primary.label.active` | `{color.text.base-inverse.on-color}` | `color` | `--button-outlined-primary-label-active` |
| `button.outlined.primary.label.disabled` | `{color.text.disabled.default}` | `color` | `--button-outlined-primary-label-disabled` |
| `button.outlined.primary.icon.default` | `{color.icon.brand.default}` | `color` | `--button-outlined-primary-icon-default` |
| `button.outlined.primary.icon.hover` | `{color.icon.base-inverse.on-color}` | `color` | `--button-outlined-primary-icon-hover` |
| `button.outlined.primary.icon.active` | `{color.icon.base-inverse.on-color}` | `color` | `--button-outlined-primary-icon-active` |
| `button.outlined.primary.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--button-outlined-primary-icon-disabled` |
| `button.outlined.strict.border.default` | `{color.border.base.strong}` | `color` | `--button-outlined-strict-border-default` |
| `button.outlined.strict.border.hover` | `transparent` | `color` | `--button-outlined-strict-border-hover` |
| `button.outlined.strict.border.active` | `transparent` | `color` | `--button-outlined-strict-border-active` |
| `button.outlined.strict.border.disabled` | `{color.border.disabled.default}` | `color` | `--button-outlined-strict-border-disabled` |
| `button.outlined.strict.background.default` | `transparent` | `color` | `--button-outlined-strict-background-default` |
| `button.outlined.strict.background.hover` | `{color.background.base-inverse.default-hover}` | `color` | `--button-outlined-strict-background-hover` |
| `button.outlined.strict.background.active` | `{color.background.base-inverse.default-active}` | `color` | `--button-outlined-strict-background-active` |
| `button.outlined.strict.background.disabled` | `transparent` | `color` | `--button-outlined-strict-background-disabled` |
| `button.outlined.strict.label.default` | `{color.text.base.default}` | `color` | `--button-outlined-strict-label-default` |
| `button.outlined.strict.label.hover` | `{color.text.base-inverse.default}` | `color` | `--button-outlined-strict-label-hover` |
| `button.outlined.strict.label.active` | `{color.text.base-inverse.default}` | `color` | `--button-outlined-strict-label-active` |
| `button.outlined.strict.label.disabled` | `{color.text.disabled.default}` | `color` | `--button-outlined-strict-label-disabled` |
| `button.outlined.strict.icon.default` | `{color.icon.base.default}` | `color` | `--button-outlined-strict-icon-default` |
| `button.outlined.strict.icon.hover` | `{color.icon.base-inverse.default}` | `color` | `--button-outlined-strict-icon-hover` |
| `button.outlined.strict.icon.active` | `{color.icon.base-inverse.default}` | `color` | `--button-outlined-strict-icon-active` |
| `button.outlined.strict.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--button-outlined-strict-icon-disabled` |
| `button.outlined.destructive.border.default` | `{color.border.danger.default}` | `color` | `--button-outlined-destructive-border-default` |
| `button.outlined.destructive.border.hover` | `transparent` | `color` | `--button-outlined-destructive-border-hover` |
| `button.outlined.destructive.border.active` | `transparent` | `color` | `--button-outlined-destructive-border-active` |
| `button.outlined.destructive.border.disabled` | `{color.border.disabled.default}` | `color` | `--button-outlined-destructive-border-disabled` |
| `button.outlined.destructive.background.default` | `transparent` | `color` | `--button-outlined-destructive-background-default` |
| `button.outlined.destructive.background.hover` | `{color.background.danger.default-hover}` | `color` | `--button-outlined-destructive-background-hover` |
| `button.outlined.destructive.background.active` | `{color.background.danger.default-active}` | `color` | `--button-outlined-destructive-background-active` |
| `button.outlined.destructive.background.disabled` | `transparent` | `color` | `--button-outlined-destructive-background-disabled` |
| `button.outlined.destructive.label.default` | `{color.text.danger.default}` | `color` | `--button-outlined-destructive-label-default` |
| `button.outlined.destructive.label.hover` | `{color.text.base-inverse.on-color}` | `color` | `--button-outlined-destructive-label-hover` |
| `button.outlined.destructive.label.active` | `{color.text.base-inverse.on-color}` | `color` | `--button-outlined-destructive-label-active` |
| `button.outlined.destructive.label.disabled` | `{color.text.disabled.default}` | `color` | `--button-outlined-destructive-label-disabled` |
| `button.outlined.destructive.icon.default` | `{color.icon.danger.default}` | `color` | `--button-outlined-destructive-icon-default` |
| `button.outlined.destructive.icon.hover` | `{color.icon.base-inverse.on-color}` | `color` | `--button-outlined-destructive-icon-hover` |
| `button.outlined.destructive.icon.active` | `{color.icon.base-inverse.on-color}` | `color` | `--button-outlined-destructive-icon-active` |
| `button.outlined.destructive.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--button-outlined-destructive-icon-disabled` |
| `button.text.primary.background.default` | `transparent` | `color` | `--button-text-primary-background-default` |
| `button.text.primary.background.hover` | `{color.background.brand.secondary-hover}` | `color` | `--button-text-primary-background-hover` |
| `button.text.primary.background.active` | `{color.background.brand.secondary-active}` | `color` | `--button-text-primary-background-active` |
| `button.text.primary.background.disabled` | `transparent` | `color` | `--button-text-primary-background-disabled` |
| `button.text.primary.label.default` | `{color.text.brand.default}` | `color` | `--button-text-primary-label-default` |
| `button.text.primary.label.hover` | `{color.text.brand.on-secondary}` | `color` | `--button-text-primary-label-hover` |
| `button.text.primary.label.active` | `{color.text.brand.on-secondary}` | `color` | `--button-text-primary-label-active` |
| `button.text.primary.label.disabled` | `{color.text.disabled.default}` | `color` | `--button-text-primary-label-disabled` |
| `button.text.primary.icon.default` | `{color.icon.brand.default}` | `color` | `--button-text-primary-icon-default` |
| `button.text.primary.icon.hover` | `{color.icon.brand.on-secondary}` | `color` | `--button-text-primary-icon-hover` |
| `button.text.primary.icon.active` | `{color.icon.brand.on-secondary}` | `color` | `--button-text-primary-icon-active` |
| `button.text.primary.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--button-text-primary-icon-disabled` |
| `button.text.strict.background.default` | `transparent` | `color` | `--button-text-strict-background-default` |
| `button.text.strict.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--button-text-strict-background-hover` |
| `button.text.strict.background.active` | `{color.background.base.tertiary-active}` | `color` | `--button-text-strict-background-active` |
| `button.text.strict.background.disabled` | `transparent` | `color` | `--button-text-strict-background-disabled` |
| `button.text.strict.label.default` | `{color.text.base.default}` | `color` | `--button-text-strict-label-default` |
| `button.text.strict.label.hover` | `{color.text.base.default}` | `color` | `--button-text-strict-label-hover` |
| `button.text.strict.label.active` | `{color.text.base.default}` | `color` | `--button-text-strict-label-active` |
| `button.text.strict.label.disabled` | `{color.text.disabled.default}` | `color` | `--button-text-strict-label-disabled` |
| `button.text.strict.icon.default` | `{color.icon.base.default}` | `color` | `--button-text-strict-icon-default` |
| `button.text.strict.icon.hover` | `{color.icon.base.default}` | `color` | `--button-text-strict-icon-hover` |
| `button.text.strict.icon.active` | `{color.icon.base.default}` | `color` | `--button-text-strict-icon-active` |
| `button.text.strict.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--button-text-strict-icon-disabled` |
| `button.text.destructive.background.default` | `transparent` | `color` | `--button-text-destructive-background-default` |
| `button.text.destructive.background.hover` | `{color.background.danger.secondary-hover}` | `color` | `--button-text-destructive-background-hover` |
| `button.text.destructive.background.active` | `{color.background.danger.secondary-active}` | `color` | `--button-text-destructive-background-active` |
| `button.text.destructive.background.disabled` | `transparent` | `color` | `--button-text-destructive-background-disabled` |
| `button.text.destructive.label.default` | `{color.text.danger.default}` | `color` | `--button-text-destructive-label-default` |
| `button.text.destructive.label.hover` | `{color.text.danger.default}` | `color` | `--button-text-destructive-label-hover` |
| `button.text.destructive.label.active` | `{color.text.danger.default}` | `color` | `--button-text-destructive-label-active` |
| `button.text.destructive.label.disabled` | `{color.text.disabled.default}` | `color` | `--button-text-destructive-label-disabled` |
| `button.text.destructive.icon.default` | `{color.icon.danger.default}` | `color` | `--button-text-destructive-icon-default` |
| `button.text.destructive.icon.hover` | `{color.icon.danger.default}` | `color` | `--button-text-destructive-icon-hover` |
| `button.text.destructive.icon.active` | `{color.icon.danger.default}` | `color` | `--button-text-destructive-icon-active` |
| `button.text.destructive.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--button-text-destructive-icon-disabled` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/button.tokens.json</code></summary>

```json
{
  "button": {
    "container": {
      "height": {
        "sm": { "$value": "{spacing.32}", "$type": "dimension" },
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "touchTarget": {
        "sm": { "$value": "{spacing.40}", "$type": "dimension" },
        "md": { "$value": "{spacing.48}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "maxWidth": { "$value": "400px", "$type": "dimension" },
      "minWidth": {
        "sm": { "$value": "52px", "$type": "dimension" },
        "md": { "$value": "{spacing.56}", "$type": "dimension" },
        "lg": { "$value": "72px", "$type": "dimension" }
      },
      "paddingInline": {
        "sm": { "$value": "{spacing.12}", "$type": "dimension" },
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "paddingInlineIcon": {
        "sm": { "$value": "{spacing.8}", "$type": "dimension" },
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": {
        "sm": { "$value": "{spacing.6}", "$type": "dimension" },
        "md": { "$value": "{spacing.6}", "$type": "dimension" },
        "lg": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "borderRadius": {
        "rectangular": {
          "sm": { "$value": "{borderRadius.6}", "$type": "dimension" },
          "md": { "$value": "{borderRadius.6}", "$type": "dimension" },
          "lg": { "$value": "{borderRadius.8}", "$type": "dimension" }
        },
        "circular": {
          "sm": { "$value": "{borderRadius.full}", "$type": "dimension" },
          "md": { "$value": "{borderRadius.full}", "$type": "dimension" },
          "lg": { "$value": "{borderRadius.full}", "$type": "dimension" }
        }
      },
      "transitionDuration": {
        "$value": "150ms",
        "$type": "duration"
      },
      "transitionTimingFunction": {
        "$value": "ease-out",
        "$type": "cubicBezier"
      }
    },
    "label": {
      "fontFamily": {
        "$value": "{fontFamily.primary}",
        "$type": "fontFamily"
      },
      "fontSize": {
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" },
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "fontWeight": {
        "$value": "{fontWeight.medium}",
        "$type": "fontWeight"
      }
    },
    "icon": {
      "size": {
        "sm": { "$value": "{spacing.16}", "$type": "dimension" },
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    },
    "primary": {
      "background": {
        "default": {
          "$value": "{color.background.brand.default}",
          "$type": "color"
        },
        "hover": {
          "$value": "{color.background.brand.default-hover}",
          "$type": "color"
        },
        "active": {
          "$value": "{color.background.brand.default-active}",
          "$type": "color"
        }
      },
      "label": {
        "$value": "{color.text.base-inverse.on-color}",
        "$type": "color"
      },
      "icon": {
        "$value": "{color.icon.base-inverse.on-color}",
        "$type": "color"
      }
    },
    "secondary": {
      "background": {
        "default": {
          "$value": "{color.background.brand.secondary}",
          "$type": "color"
        },
        "hover": {
          "$value": "{color.background.brand.secondary-hover}",
          "$type": "color"
        },
        "active": {
          "$value": "{color.background.brand.secondary-active}",
          "$type": "color"
        }
      },
      "label": {
        "$value": "{color.text.brand.on-secondary}",
        "$type": "color"
      },
      "icon": {
        "$value": "{color.icon.brand.on-secondary}",
        "$type": "color"
      }
    },
    "strict": {
      "background": {
        "default": {
          "$value": "{color.background.base-inverse.default}",
          "$type": "color"
        },
        "hover": {
          "$value": "{color.background.base-inverse.default-hover}",
          "$type": "color"
        },
        "active": {
          "$value": "{color.background.base-inverse.default-active}",
          "$type": "color"
        }
      },
      "label": {
        "$value": "{color.text.base-inverse.default}",
        "$type": "color"
      },
      "icon": {
        "$value": "{color.icon.base-inverse.default}",
        "$type": "color"
      }
    },
    "neutral": {
      "background": {
        "default": {
          "$value": "{color.background.base.tertiary}",
          "$type": "color"
        },
        "hover": {
          "$value": "{color.background.base.tertiary-hover}",
          "$type": "color"
        },
        "active": {
          "$value": "{color.background.base.tertiary-active}",
          "$type": "color"
        }
      },
      "label": {
        "$value": "{color.text.base.default}",
        "$type": "color"
      },
      "icon": {
        "$value": "{color.icon.base.default}",
        "$type": "color"
      }
    },
    "destructive": {
      "background": {
        "default": {
          "$value": "{color.background.danger.default}",
          "$type": "color"
        },
        "hover": {
          "$value": "{color.background.danger.default-hover}",
          "$type": "color"
        },
        "active": {
          "$value": "{color.background.danger.default-active}",
          "$type": "color"
        }
      },
      "label": {
        "$value": "{color.text.base-inverse.on-color}",
        "$type": "color"
      },
      "icon": {
        "$value": "{color.icon.base-inverse.on-color}",
        "$type": "color"
      }
    },
    "disabled": {
      "background": {
        "$value": "{color.background.disabled.default}",
        "$type": "color"
      },
      "label": {
        "$value": "{color.text.disabled.on-disabled}",
        "$type": "color"
      },
      "icon": {
        "$value": "{color.icon.disabled.on-disabled}",
        "$type": "color"
      }
    },
    "outlined": {
      "primary": {
        "border": {
          "default": { "$value": "{color.border.brand.default}", "$type": "color" },
          "hover": { "$value": "transparent", "$type": "color" },
          "active": { "$value": "transparent", "$type": "color" },
          "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
        },
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
          "active": { "$value": "{color.background.brand.default-active}", "$type": "color" },
          "disabled": { "$value": "transparent", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.brand.default}", "$type": "color" },
          "hover": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
          "active": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "icon": {
          "default": { "$value": "{color.icon.brand.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
          "active": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      },
      "strict": {
        "border": {
          "default": { "$value": "{color.border.base.strong}", "$type": "color" },
          "hover": { "$value": "transparent", "$type": "color" },
          "active": { "$value": "transparent", "$type": "color" },
          "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
        },
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.base-inverse.default-hover}", "$type": "color" },
          "active": { "$value": "{color.background.base-inverse.default-active}", "$type": "color" },
          "disabled": { "$value": "transparent", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.base.default}", "$type": "color" },
          "hover": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
          "active": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "icon": {
          "default": { "$value": "{color.icon.base.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.base-inverse.default}", "$type": "color" },
          "active": { "$value": "{color.icon.base-inverse.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      },
      "destructive": {
        "border": {
          "default": { "$value": "{color.border.danger.default}", "$type": "color" },
          "hover": { "$value": "transparent", "$type": "color" },
          "active": { "$value": "transparent", "$type": "color" },
          "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
        },
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.danger.default-hover}", "$type": "color" },
          "active": { "$value": "{color.background.danger.default-active}", "$type": "color" },
          "disabled": { "$value": "transparent", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.danger.default}", "$type": "color" },
          "hover": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
          "active": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "icon": {
          "default": { "$value": "{color.icon.danger.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
          "active": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      }
    },
    "text": {
      "primary": {
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.brand.secondary-hover}", "$type": "color" },
          "active": { "$value": "{color.background.brand.secondary-active}", "$type": "color" },
          "disabled": { "$value": "transparent", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.brand.default}", "$type": "color" },
          "hover": { "$value": "{color.text.brand.on-secondary}", "$type": "color" },
          "active": { "$value": "{color.text.brand.on-secondary}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "icon": {
          "default": { "$value": "{color.icon.brand.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.brand.on-secondary}", "$type": "color" },
          "active": { "$value": "{color.icon.brand.on-secondary}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      },
      "strict": {
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
          "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" },
          "disabled": { "$value": "transparent", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.base.default}", "$type": "color" },
          "hover": { "$value": "{color.text.base.default}", "$type": "color" },
          "active": { "$value": "{color.text.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "icon": {
          "default": { "$value": "{color.icon.base.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.base.default}", "$type": "color" },
          "active": { "$value": "{color.icon.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      },
      "destructive": {
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.danger.secondary-hover}", "$type": "color" },
          "active": { "$value": "{color.background.danger.secondary-active}", "$type": "color" },
          "disabled": { "$value": "transparent", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.danger.default}", "$type": "color" },
          "hover": { "$value": "{color.text.danger.default}", "$type": "color" },
          "active": { "$value": "{color.text.danger.default}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
        },
        "icon": {
          "default": { "$value": "{color.icon.danger.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.danger.default}", "$type": "color" },
          "active": { "$value": "{color.icon.danger.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/checkbox.tokens.json`

_50 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `checkbox.container.size.sm` | `{spacing.20}` | `dimension` | `--checkbox-container-size-sm` |
| `checkbox.container.size.md` | `{spacing.24}` | `dimension` | `--checkbox-container-size-md` |
| `checkbox.container.touchTarget.sm` | `36px` | `dimension` | `--checkbox-container-touch-target-sm` |
| `checkbox.container.touchTarget.md` | `36px` | `dimension` | `--checkbox-container-touch-target-md` |
| `checkbox.container.borderRadius.sm` | `{borderRadius.6}` | `dimension` | `--checkbox-container-border-radius-sm` |
| `checkbox.container.borderRadius.md` | `{borderRadius.6}` | `dimension` | `--checkbox-container-border-radius-md` |
| `checkbox.container.borderWidth` | `{borderWidth.2}` | `dimension` | `--checkbox-container-border-width` |
| `checkbox.container.focusRingWidth` | `{borderWidth.3}` | `dimension` | `--checkbox-container-focus-ring-width` |
| `checkbox.container.focusRingOffset` | `{borderWidth.1}` | `dimension` | `--checkbox-container-focus-ring-offset` |
| `checkbox.container.gap.sm` | `{spacing.6}` | `dimension` | `--checkbox-container-gap-sm` |
| `checkbox.container.gap.md` | `{spacing.12}` | `dimension` | `--checkbox-container-gap-md` |
| `checkbox.container.transitionDuration` | `150ms` | `duration` | `--checkbox-container-transition-duration` |
| `checkbox.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--checkbox-container-transition-timing-function` |
| `checkbox.icon.size.sm` | `{spacing.12}` | `dimension` | `--checkbox-icon-size-sm` |
| `checkbox.icon.size.md` | `{spacing.16}` | `dimension` | `--checkbox-icon-size-md` |
| `checkbox.icon.color.default` | `{color.icon.base-inverse.on-color}` | `color` | `--checkbox-icon-color-default` |
| `checkbox.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--checkbox-icon-color-disabled` |
| `checkbox.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--checkbox-label-font-family` |
| `checkbox.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--checkbox-label-font-weight` |
| `checkbox.label.fontSize.sm` | `{fontSize.14}` | `dimension` | `--checkbox-label-font-size-sm` |
| `checkbox.label.fontSize.md` | `{fontSize.16}` | `dimension` | `--checkbox-label-font-size-md` |
| `checkbox.label.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--checkbox-label-line-height-sm` |
| `checkbox.label.lineHeight.md` | `{lineHeight.24}` | `dimension` | `--checkbox-label-line-height-md` |
| `checkbox.label.color.default` | `{color.text.base.default}` | `color` | `--checkbox-label-color-default` |
| `checkbox.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--checkbox-label-color-disabled` |
| `checkbox.text.gap` | `{spacing.2}` | `dimension` | `--checkbox-text-gap` |
| `checkbox.supporting.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--checkbox-supporting-font-family` |
| `checkbox.supporting.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--checkbox-supporting-font-weight` |
| `checkbox.supporting.fontSize.sm` | `{fontSize.12}` | `dimension` | `--checkbox-supporting-font-size-sm` |
| `checkbox.supporting.fontSize.md` | `{fontSize.14}` | `dimension` | `--checkbox-supporting-font-size-md` |
| `checkbox.supporting.lineHeight.sm` | `{lineHeight.16}` | `dimension` | `--checkbox-supporting-line-height-sm` |
| `checkbox.supporting.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--checkbox-supporting-line-height-md` |
| `checkbox.supporting.color.default` | `{color.text.base.tertiary}` | `color` | `--checkbox-supporting-color-default` |
| `checkbox.supporting.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--checkbox-supporting-color-disabled` |
| `checkbox.default.background.unchecked` | `{color.background.base.default}` | `color` | `--checkbox-default-background-unchecked` |
| `checkbox.default.background.checked` | `{color.background.brand.default}` | `color` | `--checkbox-default-background-checked` |
| `checkbox.default.border.unchecked` | `{color.border.base.secondary}` | `color` | `--checkbox-default-border-unchecked` |
| `checkbox.default.border.checked` | `{color.background.brand.default}` | `color` | `--checkbox-default-border-checked` |
| `checkbox.default.focusRing` | `{color.background.brand.focus-ring}` | `color` | `--checkbox-default-focus-ring` |
| `checkbox.destructive.background.unchecked` | `{color.background.base.default}` | `color` | `--checkbox-destructive-background-unchecked` |
| `checkbox.destructive.background.checked` | `{color.border.danger.default}` | `color` | `--checkbox-destructive-background-checked` |
| `checkbox.destructive.border.unchecked` | `{color.border.danger.default}` | `color` | `--checkbox-destructive-border-unchecked` |
| `checkbox.destructive.border.checked` | `{color.border.danger.default}` | `color` | `--checkbox-destructive-border-checked` |
| `checkbox.destructive.focusRing` | `{color.background.brand.focus-ring}` | `color` | `--checkbox-destructive-focus-ring` |
| `checkbox.destructive.label` | `{color.text.danger.default}` | `color` | `--checkbox-destructive-label` |
| `checkbox.destructive.supporting` | `{color.text.danger.default}` | `color` | `--checkbox-destructive-supporting` |
| `checkbox.disabled.background.unchecked` | `{color.background.disabled.default}` | `color` | `--checkbox-disabled-background-unchecked` |
| `checkbox.disabled.background.checked` | `{color.background.disabled.default}` | `color` | `--checkbox-disabled-background-checked` |
| `checkbox.disabled.border.unchecked` | `{color.border.disabled.default}` | `color` | `--checkbox-disabled-border-unchecked` |
| `checkbox.disabled.border.checked` | `{color.border.disabled.default}` | `color` | `--checkbox-disabled-border-checked` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/checkbox.tokens.json</code></summary>

```json
{
  "checkbox": {
    "container": {
      "size": {
        "sm": { "$value": "{spacing.20}", "$type": "dimension" },
        "md": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "touchTarget": {
        "sm": {
          "$value": "36px",
          "$type": "dimension",
          "$comment": "WCAG/Figma coarse-pointer minimum 36×36 (Figma 2822:1227/1230 Target). No spacing step maps to 36 (scale jumps 32→40), so a literal is used like transitionDuration below."
        },
        "md": {
          "$value": "36px",
          "$type": "dimension",
          "$comment": "WCAG/Figma coarse-pointer minimum 36×36 (Figma 2822:1230 Target)."
        }
      },
      "borderRadius": {
        "sm": {
          "$value": "{borderRadius.6}",
          "$type": "dimension",
          "$comment": "Both sizes share radius-6 per Figma 403:21816 (sm 20px box also binds --border-radius-6)."
        },
        "md": { "$value": "{borderRadius.6}", "$type": "dimension" }
      },
      "borderWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "focusRingWidth": { "$value": "{borderWidth.3}", "$type": "dimension" },
      "focusRingOffset": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "gap": {
        "sm": { "$value": "{spacing.6}", "$type": "dimension" },
        "md": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "icon": {
      "size": {
        "sm": { "$value": "{spacing.12}", "$type": "dimension" },
        "md": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": {
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" },
        "md": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "md": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      }
    },
    "text": {
      "gap": { "$value": "{spacing.2}", "$type": "dimension" }
    },
    "supporting": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "sm": { "$value": "{fontSize.12}", "$type": "dimension" },
        "md": { "$value": "{fontSize.14}", "$type": "dimension" }
      },
      "lineHeight": {
        "sm": { "$value": "{lineHeight.16}", "$type": "dimension" },
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      }
    },
    "default": {
      "background": {
        "unchecked": { "$value": "{color.background.base.default}", "$type": "color" },
        "checked": { "$value": "{color.background.brand.default}", "$type": "color" }
      },
      "border": {
        "unchecked": {
          "$value": "{color.border.base.secondary}",
          "$type": "color",
          "$comment": "#b2b2b2 light-gray unchecked border per Figma 403:21808 (was base.tertiary #444, too dark)."
        },
        "checked": { "$value": "{color.background.brand.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{color.background.brand.focus-ring}", "$type": "color" }
    },
    "destructive": {
      "background": {
        "unchecked": { "$value": "{color.background.base.default}", "$type": "color" },
        "checked": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "border": {
        "unchecked": { "$value": "{color.border.danger.default}", "$type": "color" },
        "checked": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{color.background.brand.focus-ring}", "$type": "color" },
      "label": { "$value": "{color.text.danger.default}", "$type": "color" },
      "supporting": { "$value": "{color.text.danger.default}", "$type": "color" }
    },
    "disabled": {
      "background": {
        "unchecked": { "$value": "{color.background.disabled.default}", "$type": "color" },
        "checked": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "unchecked": { "$value": "{color.border.disabled.default}", "$type": "color" },
        "checked": { "$value": "{color.border.disabled.default}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/chip.tokens.json`

_62 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `chip.container.height.md` | `36px` | `dimension` | `--chip-container-height-md` |
| `chip.container.height.sm` | `28px` | `dimension` | `--chip-container-height-sm` |
| `chip.container.touchTarget.md` | `44px` | `dimension` | `--chip-container-touch-target-md` |
| `chip.container.touchTarget.sm` | `{spacing.40}` | `dimension` | `--chip-container-touch-target-sm` |
| `chip.container.minWidth.md` | `60px` | `dimension` | `--chip-container-min-width-md` |
| `chip.container.minWidth.sm` | `52px` | `dimension` | `--chip-container-min-width-sm` |
| `chip.container.paddingInline.md` | `{spacing.16}` | `dimension` | `--chip-container-padding-inline-md` |
| `chip.container.paddingInline.sm` | `{spacing.12}` | `dimension` | `--chip-container-padding-inline-sm` |
| `chip.container.paddingInlineIcon.md` | `{spacing.12}` | `dimension` | `--chip-container-padding-inline-icon-md` |
| `chip.container.paddingInlineIcon.sm` | `{spacing.8}` | `dimension` | `--chip-container-padding-inline-icon-sm` |
| `chip.container.paddingInlineRemove.md` | `{spacing.12}` | `dimension` | `--chip-container-padding-inline-remove-md` |
| `chip.container.paddingInlineRemove.sm` | `{spacing.8}` | `dimension` | `--chip-container-padding-inline-remove-sm` |
| `chip.container.gap.md` | `{spacing.8}` | `dimension` | `--chip-container-gap-md` |
| `chip.container.gap.sm` | `{spacing.6}` | `dimension` | `--chip-container-gap-sm` |
| `chip.container.gapIcon.md` | `{spacing.4}` | `dimension` | `--chip-container-gap-icon-md` |
| `chip.container.gapIcon.sm` | `{spacing.4}` | `dimension` | `--chip-container-gap-icon-sm` |
| `chip.container.borderRadius` | `{borderRadius.full}` | `dimension` | `--chip-container-border-radius` |
| `chip.container.transitionDuration` | `150ms` | `duration` | `--chip-container-transition-duration` |
| `chip.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--chip-container-transition-timing-function` |
| `chip.avatar.size.md` | `28px` | `dimension` | `--chip-avatar-size-md` |
| `chip.avatar.size.sm` | `22px` | `dimension` | `--chip-avatar-size-sm` |
| `chip.count.size.md` | `18px` | `dimension` | `--chip-count-size-md` |
| `chip.count.size.sm` | `16px` | `dimension` | `--chip-count-size-sm` |
| `chip.count.fontSize.md` | `{fontSize.12}` | `dimension` | `--chip-count-font-size-md` |
| `chip.count.fontSize.sm` | `{fontSize.12}` | `dimension` | `--chip-count-font-size-sm` |
| `chip.count.paddingInline.md` | `{spacing.4}` | `dimension` | `--chip-count-padding-inline-md` |
| `chip.count.paddingInline.sm` | `{spacing.4}` | `dimension` | `--chip-count-padding-inline-sm` |
| `chip.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--chip-label-font-family` |
| `chip.label.fontSize.md` | `{fontSize.14}` | `dimension` | `--chip-label-font-size-md` |
| `chip.label.fontSize.sm` | `{fontSize.12}` | `dimension` | `--chip-label-font-size-sm` |
| `chip.label.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--chip-label-line-height-md` |
| `chip.label.lineHeight.sm` | `{lineHeight.16}` | `dimension` | `--chip-label-line-height-sm` |
| `chip.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--chip-label-font-weight` |
| `chip.icon.size.md` | `{spacing.20}` | `dimension` | `--chip-icon-size-md` |
| `chip.icon.size.sm` | `{spacing.16}` | `dimension` | `--chip-icon-size-sm` |
| `chip.filter.background.default` | `{color.background.base.tertiary}` | `color` | `--chip-filter-background-default` |
| `chip.filter.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--chip-filter-background-hover` |
| `chip.filter.background.selected` | `{color.background.base-inverse.default}` | `color` | `--chip-filter-background-selected` |
| `chip.filter.background.selectedHover` | `{color.background.base-inverse.default-hover}` | `color` | `--chip-filter-background-selected-hover` |
| `chip.filter.label.default` | `{color.text.base.default}` | `color` | `--chip-filter-label-default` |
| `chip.filter.label.selected` | `{color.text.base-inverse.default}` | `color` | `--chip-filter-label-selected` |
| `chip.filter.icon.default` | `{color.icon.base.default}` | `color` | `--chip-filter-icon-default` |
| `chip.filter.icon.selected` | `{color.icon.base-inverse.default}` | `color` | `--chip-filter-icon-selected` |
| `chip.input.minWidth.md` | `64px` | `dimension` | `--chip-input-min-width-md` |
| `chip.input.minWidth.sm` | `56px` | `dimension` | `--chip-input-min-width-sm` |
| `chip.input.background.default` | `{color.background.base.tertiary}` | `color` | `--chip-input-background-default` |
| `chip.input.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--chip-input-background-hover` |
| `chip.input.label.default` | `{color.text.base.default}` | `color` | `--chip-input-label-default` |
| `chip.input.icon.default` | `{color.icon.base.default}` | `color` | `--chip-input-icon-default` |
| `chip.input.remove.size.md` | `{spacing.20}` | `dimension` | `--chip-input-remove-size-md` |
| `chip.input.remove.size.sm` | `{spacing.16}` | `dimension` | `--chip-input-remove-size-sm` |
| `chip.input.remove.iconSize.md` | `{spacing.20}` | `dimension` | `--chip-input-remove-icon-size-md` |
| `chip.input.remove.iconSize.sm` | `{spacing.16}` | `dimension` | `--chip-input-remove-icon-size-sm` |
| `chip.input.remove.borderRadius` | `{borderRadius.full}` | `dimension` | `--chip-input-remove-border-radius` |
| `chip.input.remove.background.default` | `transparent` | `color` | `--chip-input-remove-background-default` |
| `chip.input.remove.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--chip-input-remove-background-hover` |
| `chip.input.remove.color.default` | `{color.icon.base.default}` | `color` | `--chip-input-remove-color-default` |
| `chip.input.remove.color.hover` | `{color.icon.base.default}` | `color` | `--chip-input-remove-color-hover` |
| `chip.input.remove.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--chip-input-remove-color-disabled` |
| `chip.disabled.background` | `{color.background.disabled.default}` | `color` | `--chip-disabled-background` |
| `chip.disabled.label` | `{color.text.disabled.on-disabled}` | `color` | `--chip-disabled-label` |
| `chip.disabled.icon` | `{color.icon.disabled.on-disabled}` | `color` | `--chip-disabled-icon` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/chip.tokens.json</code></summary>

```json
{
  "chip": {
    "container": {
      "height": {
        "md": { "$value": "36px", "$type": "dimension" },
        "sm": { "$value": "28px", "$type": "dimension" }
      },
      "touchTarget": {
        "md": { "$value": "44px", "$type": "dimension" },
        "sm": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "minWidth": {
        "md": { "$value": "60px", "$type": "dimension" },
        "sm": { "$value": "52px", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "sm": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "paddingInlineIcon": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "sm": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "paddingInlineRemove": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "sm": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "sm": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "gapIcon": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "sm": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "avatar": {
      "size": {
        "md": { "$value": "28px", "$type": "dimension" },
        "sm": { "$value": "22px", "$type": "dimension" }
      }
    },
    "count": {
      "size": {
        "md": { "$value": "18px", "$type": "dimension" },
        "sm": { "$value": "16px", "$type": "dimension" }
      },
      "fontSize": {
        "md": { "$value": "{fontSize.12}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.12}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "sm": { "$value": "{spacing.4}", "$type": "dimension" }
      }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.12}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.16}", "$type": "dimension" }
      },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "sm": { "$value": "{spacing.16}", "$type": "dimension" }
      }
    },
    "filter": {
      "background": {
        "default": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
        "selected": { "$value": "{color.background.base-inverse.default}", "$type": "color" },
        "selectedHover": { "$value": "{color.background.base-inverse.default-hover}", "$type": "color" }
      },
      "label": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "selected": { "$value": "{color.text.base-inverse.default}", "$type": "color" }
      },
      "icon": {
        "default": { "$value": "{color.icon.base.default}", "$type": "color" },
        "selected": { "$value": "{color.icon.base-inverse.default}", "$type": "color" }
      }
    },
    "input": {
      "minWidth": {
        "md": { "$value": "64px", "$type": "dimension" },
        "sm": { "$value": "56px", "$type": "dimension" }
      },
      "background": {
        "default": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" }
      },
      "label": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" }
      },
      "icon": {
        "default": { "$value": "{color.icon.base.default}", "$type": "color" }
      },
      "remove": {
        "size": {
          "md": { "$value": "{spacing.20}", "$type": "dimension" },
          "sm": { "$value": "{spacing.16}", "$type": "dimension" }
        },
        "iconSize": {
          "md": { "$value": "{spacing.20}", "$type": "dimension" },
          "sm": { "$value": "{spacing.16}", "$type": "dimension" }
        },
        "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" }
        },
        "color": {
          "default": { "$value": "{color.icon.base.default}", "$type": "color" },
          "hover": { "$value": "{color.icon.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
        }
      }
    },
    "disabled": {
      "background": { "$value": "{color.background.disabled.default}", "$type": "color" },
      "label": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" },
      "icon": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/cookie-banner.tokens.json`

_75 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `cookie-banner.container.background` | `{color.background.base.default}` | `color` | `--cookie-banner-container-background` |
| `cookie-banner.container.border` | `{color.border.base.default}` | `color` | `--cookie-banner-container-border` |
| `cookie-banner.container.borderWidth` | `1.5px` | `dimension` | `--cookie-banner-container-border-width` |
| `cookie-banner.container.borderRadius` | `{borderRadius.16}` | `dimension` | `--cookie-banner-container-border-radius` |
| `cookie-banner.container.borderRadiusMobile` | `{borderRadius.12}` | `dimension` | `--cookie-banner-container-border-radius-mobile` |
| `cookie-banner.container.shadowSmall` | `0 0 0.5px 0 rgba(0, 0, 0, 0.15), 0 1px 3px 0 rgba(0, 0, 0, 0.08), 0 5px 12px 0 rgba(0, 0, 0, 0.08)` | `shadow` | `--cookie-banner-container-shadow-small` |
| `cookie-banner.container.minWidth` | `280px` | `dimension` | `--cookie-banner-container-min-width` |
| `cookie-banner.container.maxWidth` | `792px` | `dimension` | `--cookie-banner-container-max-width` |
| `cookie-banner.container.mobileWidth` | `359px` | `dimension` | `--cookie-banner-container-mobile-width` |
| `cookie-banner.container.paddingInlineDesktop` | `{spacing.40}` | `dimension` | `--cookie-banner-container-padding-inline-desktop` |
| `cookie-banner.container.paddingInlineMobile` | `{spacing.16}` | `dimension` | `--cookie-banner-container-padding-inline-mobile` |
| `cookie-banner.container.paddingBlockDesktop` | `{spacing.20}` | `dimension` | `--cookie-banner-container-padding-block-desktop` |
| `cookie-banner.container.paddingBlockMobile` | `{spacing.16}` | `dimension` | `--cookie-banner-container-padding-block-mobile` |
| `cookie-banner.container.edgeOffset` | `{spacing.16}` | `dimension` | `--cookie-banner-container-edge-offset` |
| `cookie-banner.container.edgeOffsetMobile` | `{spacing.8}` | `dimension` | `--cookie-banner-container-edge-offset-mobile` |
| `cookie-banner.container.stackZIndex` | `1000` | `number` | `--cookie-banner-container-stack-z-index` |
| `cookie-banner.header.paddingTop` | `{spacing.20}` | `dimension` | `--cookie-banner-header-padding-top` |
| `cookie-banner.header.paddingTopMobile` | `{spacing.24}` | `dimension` | `--cookie-banner-header-padding-top-mobile` |
| `cookie-banner.header.paddingBottom` | `{spacing.0}` | `dimension` | `--cookie-banner-header-padding-bottom` |
| `cookie-banner.header.paddingInlineEndMobile` | `{spacing.12}` | `dimension` | `--cookie-banner-header-padding-inline-end-mobile` |
| `cookie-banner.header.gap` | `{spacing.12}` | `dimension` | `--cookie-banner-header-gap` |
| `cookie-banner.title.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--cookie-banner-title-font-family` |
| `cookie-banner.title.fontSize` | `{fontSize.20}` | `dimension` | `--cookie-banner-title-font-size` |
| `cookie-banner.title.fontSizeMobile` | `{fontSize.22}` | `dimension` | `--cookie-banner-title-font-size-mobile` |
| `cookie-banner.title.lineHeight` | `{lineHeight.28}` | `dimension` | `--cookie-banner-title-line-height` |
| `cookie-banner.title.lineHeightMobile` | `{lineHeight.30}` | `dimension` | `--cookie-banner-title-line-height-mobile` |
| `cookie-banner.title.fontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--cookie-banner-title-font-weight` |
| `cookie-banner.title.letterSpacing` | `-0.2px` | `dimension` | `--cookie-banner-title-letter-spacing` |
| `cookie-banner.title.letterSpacingMobile` | `-0.22px` | `dimension` | `--cookie-banner-title-letter-spacing-mobile` |
| `cookie-banner.title.color` | `{color.text.base.default}` | `color` | `--cookie-banner-title-color` |
| `cookie-banner.body.paddingTop` | `{spacing.8}` | `dimension` | `--cookie-banner-body-padding-top` |
| `cookie-banner.body.paddingTopMobile` | `{spacing.16}` | `dimension` | `--cookie-banner-body-padding-top-mobile` |
| `cookie-banner.body.paddingBottom` | `{spacing.16}` | `dimension` | `--cookie-banner-body-padding-bottom` |
| `cookie-banner.body.gap` | `{spacing.16}` | `dimension` | `--cookie-banner-body-gap` |
| `cookie-banner.body.gapExpanded` | `{spacing.24}` | `dimension` | `--cookie-banner-body-gap-expanded` |
| `cookie-banner.body.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--cookie-banner-body-font-family` |
| `cookie-banner.body.fontSize` | `{fontSize.14}` | `dimension` | `--cookie-banner-body-font-size` |
| `cookie-banner.body.fontSizeMobile` | `{fontSize.16}` | `dimension` | `--cookie-banner-body-font-size-mobile` |
| `cookie-banner.body.lineHeight` | `{lineHeight.20}` | `dimension` | `--cookie-banner-body-line-height` |
| `cookie-banner.body.lineHeightMobile` | `{lineHeight.24}` | `dimension` | `--cookie-banner-body-line-height-mobile` |
| `cookie-banner.body.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--cookie-banner-body-font-weight` |
| `cookie-banner.body.color` | `{color.text.base.secondary}` | `color` | `--cookie-banner-body-color` |
| `cookie-banner.footer.paddingTop` | `{spacing.12}` | `dimension` | `--cookie-banner-footer-padding-top` |
| `cookie-banner.footer.paddingBottom` | `{spacing.20}` | `dimension` | `--cookie-banner-footer-padding-bottom` |
| `cookie-banner.footer.paddingBottomMobile` | `{spacing.24}` | `dimension` | `--cookie-banner-footer-padding-bottom-mobile` |
| `cookie-banner.footer.gap` | `{spacing.12}` | `dimension` | `--cookie-banner-footer-gap` |
| `cookie-banner.close.size` | `32px` | `dimension` | `--cookie-banner-close-size` |
| `cookie-banner.close.iconSize` | `16px` | `dimension` | `--cookie-banner-close-icon-size` |
| `cookie-banner.close.borderRadius` | `{borderRadius.full}` | `dimension` | `--cookie-banner-close-border-radius` |
| `cookie-banner.close.background` | `{color.background.base.tertiary}` | `color` | `--cookie-banner-close-background` |
| `cookie-banner.close.backgroundHover` | `{color.background.base.tertiary-hover}` | `color` | `--cookie-banner-close-background-hover` |
| `cookie-banner.close.color` | `{color.icon.base.secondary}` | `color` | `--cookie-banner-close-color` |
| `cookie-banner.categories.background` | `{color.background.base.secondary}` | `color` | `--cookie-banner-categories-background` |
| `cookie-banner.categories.borderRadius` | `{borderRadius.12}` | `dimension` | `--cookie-banner-categories-border-radius` |
| `cookie-banner.categories.paddingInline` | `{spacing.20}` | `dimension` | `--cookie-banner-categories-padding-inline` |
| `cookie-banner.categories.paddingTop` | `{spacing.8}` | `dimension` | `--cookie-banner-categories-padding-top` |
| `cookie-banner.categories.paddingBottom` | `{spacing.12}` | `dimension` | `--cookie-banner-categories-padding-bottom` |
| `cookie-banner.categories.rowGap` | `{spacing.12}` | `dimension` | `--cookie-banner-categories-row-gap` |
| `cookie-banner.category.paddingBlock` | `{spacing.12}` | `dimension` | `--cookie-banner-category-padding-block` |
| `cookie-banner.category.gap` | `{spacing.40}` | `dimension` | `--cookie-banner-category-gap` |
| `cookie-banner.category.gapMobile` | `{spacing.16}` | `dimension` | `--cookie-banner-category-gap-mobile` |
| `cookie-banner.category.rowGap` | `{spacing.8}` | `dimension` | `--cookie-banner-category-row-gap` |
| `cookie-banner.category.headerGap` | `{spacing.16}` | `dimension` | `--cookie-banner-category-header-gap` |
| `cookie-banner.category.labelFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--cookie-banner-category-label-font-family` |
| `cookie-banner.category.labelFontSize` | `{fontSize.16}` | `dimension` | `--cookie-banner-category-label-font-size` |
| `cookie-banner.category.labelLineHeight` | `{lineHeight.24}` | `dimension` | `--cookie-banner-category-label-line-height` |
| `cookie-banner.category.labelFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--cookie-banner-category-label-font-weight` |
| `cookie-banner.category.labelColor` | `{color.text.base.default}` | `color` | `--cookie-banner-category-label-color` |
| `cookie-banner.category.descriptionFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--cookie-banner-category-description-font-family` |
| `cookie-banner.category.descriptionFontSize` | `{fontSize.14}` | `dimension` | `--cookie-banner-category-description-font-size` |
| `cookie-banner.category.descriptionLineHeight` | `{lineHeight.20}` | `dimension` | `--cookie-banner-category-description-line-height` |
| `cookie-banner.category.descriptionFontWeight` | `{fontWeight.regular}` | `fontWeight` | `--cookie-banner-category-description-font-weight` |
| `cookie-banner.category.descriptionColor` | `{color.text.base.tertiary}` | `color` | `--cookie-banner-category-description-color` |
| `cookie-banner.category.requiredIconColor` | `{color.icon.brand.default}` | `color` | `--cookie-banner-category-required-icon-color` |
| `cookie-banner.category.requiredIconSize` | `24px` | `dimension` | `--cookie-banner-category-required-icon-size` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/cookie-banner.tokens.json</code></summary>

```json
{
  "cookie-banner": {
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "border": { "$value": "{color.border.base.default}", "$type": "color" },
      "borderWidth": { "$value": "1.5px", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.16}", "$type": "dimension" },
      "borderRadiusMobile": { "$value": "{borderRadius.12}", "$type": "dimension" },
      "shadowSmall": {
        "$value": "0 0 0.5px 0 rgba(0, 0, 0, 0.15), 0 1px 3px 0 rgba(0, 0, 0, 0.08), 0 5px 12px 0 rgba(0, 0, 0, 0.08)",
        "$type": "shadow"
      },
      "minWidth": { "$value": "280px", "$type": "dimension" },
      "maxWidth": { "$value": "792px", "$type": "dimension" },
      "mobileWidth": { "$value": "359px", "$type": "dimension" },
      "paddingInlineDesktop": { "$value": "{spacing.40}", "$type": "dimension" },
      "paddingInlineMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlockDesktop": { "$value": "{spacing.20}", "$type": "dimension" },
      "paddingBlockMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "edgeOffset": { "$value": "{spacing.16}", "$type": "dimension" },
      "edgeOffsetMobile": { "$value": "{spacing.8}", "$type": "dimension" },
      "stackZIndex": { "$value": "1000", "$type": "number" }
    },
    "header": {
      "paddingTop": { "$value": "{spacing.20}", "$type": "dimension" },
      "paddingTopMobile": { "$value": "{spacing.24}", "$type": "dimension" },
      "paddingBottom": { "$value": "{spacing.0}", "$type": "dimension" },
      "paddingInlineEndMobile": { "$value": "{spacing.12}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" }
    },
    "title": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.20}", "$type": "dimension" },
      "fontSizeMobile": { "$value": "{fontSize.22}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.28}", "$type": "dimension" },
      "lineHeightMobile": { "$value": "{lineHeight.30}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "letterSpacing": { "$value": "-0.2px", "$type": "dimension" },
      "letterSpacingMobile": { "$value": "-0.22px", "$type": "dimension" },
      "color": { "$value": "{color.text.base.default}", "$type": "color" }
    },
    "body": {
      "paddingTop": { "$value": "{spacing.8}", "$type": "dimension" },
      "paddingTopMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBottom": { "$value": "{spacing.16}", "$type": "dimension" },
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "gapExpanded": { "$value": "{spacing.24}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "fontSizeMobile": { "$value": "{fontSize.16}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "lineHeightMobile": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "color": { "$value": "{color.text.base.secondary}", "$type": "color" }
    },
    "footer": {
      "paddingTop": { "$value": "{spacing.12}", "$type": "dimension" },
      "paddingBottom": { "$value": "{spacing.20}", "$type": "dimension" },
      "paddingBottomMobile": { "$value": "{spacing.24}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" }
    },
    "close": {
      "size": { "$value": "32px", "$type": "dimension" },
      "iconSize": { "$value": "16px", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "backgroundHover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
      "color": { "$value": "{color.icon.base.secondary}", "$type": "color" }
    },
    "categories": {
      "background": { "$value": "{color.background.base.secondary}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.12}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.20}", "$type": "dimension" },
      "paddingTop": { "$value": "{spacing.8}", "$type": "dimension" },
      "paddingBottom": { "$value": "{spacing.12}", "$type": "dimension" },
      "rowGap": { "$value": "{spacing.12}", "$type": "dimension" }
    },
    "category": {
      "paddingBlock": { "$value": "{spacing.12}", "$type": "dimension" },
      "gap": { "$value": "{spacing.40}", "$type": "dimension" },
      "gapMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "rowGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "headerGap": { "$value": "{spacing.16}", "$type": "dimension" },
      "labelFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "labelFontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "labelFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "labelColor": { "$value": "{color.text.base.default}", "$type": "color" },
      "descriptionFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "descriptionFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "descriptionLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "descriptionFontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "descriptionColor": { "$value": "{color.text.base.tertiary}", "$type": "color" },
      "requiredIconColor": { "$value": "{color.icon.brand.default}", "$type": "color" },
      "requiredIconSize": { "$value": "24px", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/date-input.tokens.json`

_56 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `dateInput.field.gap` | `{spacing.8}` | `dimension` | `--date-input-field-gap` |
| `dateInput.container.height.md` | `{spacing.40}` | `dimension` | `--date-input-container-height-md` |
| `dateInput.container.height.lg` | `{spacing.48}` | `dimension` | `--date-input-container-height-lg` |
| `dateInput.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--date-input-container-padding-inline-md` |
| `dateInput.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--date-input-container-padding-inline-lg` |
| `dateInput.container.gap.md` | `{spacing.8}` | `dimension` | `--date-input-container-gap-md` |
| `dateInput.container.gap.lg` | `{spacing.8}` | `dimension` | `--date-input-container-gap-lg` |
| `dateInput.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--date-input-container-border-radius` |
| `dateInput.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--date-input-container-border-width-default` |
| `dateInput.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--date-input-container-border-width-emphasized` |
| `dateInput.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--date-input-container-focus-ring-width` |
| `dateInput.container.transitionDuration` | `150ms` | `duration` | `--date-input-container-transition-duration` |
| `dateInput.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--date-input-container-transition-timing-function` |
| `dateInput.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--date-input-control-font-family` |
| `dateInput.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--date-input-control-font-weight` |
| `dateInput.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--date-input-control-font-size-md` |
| `dateInput.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--date-input-control-font-size-lg` |
| `dateInput.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--date-input-control-line-height-md` |
| `dateInput.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--date-input-control-line-height-lg` |
| `dateInput.control.caretColor` | `{color.text.brand.default}` | `color` | `--date-input-control-caret-color` |
| `dateInput.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--date-input-label-font-family` |
| `dateInput.label.fontSize` | `{fontSize.14}` | `dimension` | `--date-input-label-font-size` |
| `dateInput.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--date-input-label-line-height` |
| `dateInput.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--date-input-label-font-weight` |
| `dateInput.label.gap` | `{spacing.4}` | `dimension` | `--date-input-label-gap` |
| `dateInput.label.color.default` | `{color.text.base.secondary}` | `color` | `--date-input-label-color-default` |
| `dateInput.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--date-input-label-color-disabled` |
| `dateInput.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--date-input-label-required-mark-size` |
| `dateInput.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--date-input-label-required-mark-color` |
| `dateInput.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--date-input-assistive-font-family` |
| `dateInput.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--date-input-assistive-font-size` |
| `dateInput.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--date-input-assistive-line-height` |
| `dateInput.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--date-input-assistive-font-weight` |
| `dateInput.assistive.gap.default` | `{spacing.6}` | `dimension` | `--date-input-assistive-gap-default` |
| `dateInput.assistive.gap.error` | `{spacing.4}` | `dimension` | `--date-input-assistive-gap-error` |
| `dateInput.assistive.iconSize` | `{spacing.20}` | `dimension` | `--date-input-assistive-icon-size` |
| `dateInput.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--date-input-assistive-color-default` |
| `dateInput.assistive.color.error` | `{color.text.danger.default}` | `color` | `--date-input-assistive-color-error` |
| `dateInput.icon.size.md` | `{spacing.20}` | `dimension` | `--date-input-icon-size-md` |
| `dateInput.icon.size.lg` | `{spacing.24}` | `dimension` | `--date-input-icon-size-lg` |
| `dateInput.icon.color.default` | `{color.icon.base.secondary}` | `color` | `--date-input-icon-color-default` |
| `dateInput.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--date-input-icon-color-disabled` |
| `dateInput.default.background.default` | `{color.background.base.default}` | `color` | `--date-input-default-background-default` |
| `dateInput.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--date-input-default-background-disabled` |
| `dateInput.default.border.default` | `{color.border.base.default}` | `color` | `--date-input-default-border-default` |
| `dateInput.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--date-input-default-border-hover` |
| `dateInput.default.border.focus` | `{color.border.brand.default}` | `color` | `--date-input-default-border-focus` |
| `dateInput.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--date-input-default-border-disabled` |
| `dateInput.default.text.default` | `{color.text.base.default}` | `color` | `--date-input-default-text-default` |
| `dateInput.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--date-input-default-text-placeholder` |
| `dateInput.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--date-input-default-text-disabled` |
| `dateInput.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--date-input-default-focus-ring` |
| `dateInput.destructive.border.default` | `{color.border.danger.default}` | `color` | `--date-input-destructive-border-default` |
| `dateInput.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--date-input-destructive-border-hover` |
| `dateInput.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--date-input-destructive-border-focus` |
| `dateInput.destructive.focusRing` | `{palette.red.200}` | `color` | `--date-input-destructive-focus-ring` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/date-input.tokens.json</code></summary>

```json
{
  "dateInput": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "caretColor": { "$value": "{color.text.brand.default}", "$type": "color" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "destructive": {
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/date-picker.tokens.json`

_70 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `datePicker.container.width.desktop` | `320px` | `dimension` | `--date-picker-container-width-desktop` |
| `datePicker.container.width.mobile` | `100%` | `dimension` | `--date-picker-container-width-mobile` |
| `datePicker.container.width.docked` | `320px` | `dimension` | `--date-picker-container-width-docked` |
| `datePicker.container.padding.desktop` | `{spacing.12}` | `dimension` | `--date-picker-container-padding-desktop` |
| `datePicker.container.padding.mobile` | `{spacing.16}` | `dimension` | `--date-picker-container-padding-mobile` |
| `datePicker.container.padding.docked` | `{spacing.8}` | `dimension` | `--date-picker-container-padding-docked` |
| `datePicker.container.gap` | `{spacing.8}` | `dimension` | `--date-picker-container-gap` |
| `datePicker.container.background` | `{color.background.base.default}` | `color` | `--date-picker-container-background` |
| `datePicker.container.borderRadius` | `{borderRadius.12}` | `dimension` | `--date-picker-container-border-radius` |
| `datePicker.container.borderWidth` | `{borderWidth.1}` | `dimension` | `--date-picker-container-border-width` |
| `datePicker.container.borderColor` | `{color.border.base.default}` | `color` | `--date-picker-container-border-color` |
| `datePicker.container.shadow` | `{dropShadow.300}` | `string` | `--date-picker-container-shadow` |
| `datePicker.container.transitionDuration` | `150ms` | `duration` | `--date-picker-container-transition-duration` |
| `datePicker.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--date-picker-container-transition-timing-function` |
| `datePicker.header.height` | `{spacing.40}` | `dimension` | `--date-picker-header-height` |
| `datePicker.header.gap` | `{spacing.8}` | `dimension` | `--date-picker-header-gap` |
| `datePicker.header.title.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--date-picker-header-title-font-family` |
| `datePicker.header.title.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--date-picker-header-title-font-weight` |
| `datePicker.header.title.fontSize` | `{fontSize.16}` | `dimension` | `--date-picker-header-title-font-size` |
| `datePicker.header.title.lineHeight` | `{lineHeight.24}` | `dimension` | `--date-picker-header-title-line-height` |
| `datePicker.header.title.color` | `{color.text.base.default}` | `color` | `--date-picker-header-title-color` |
| `datePicker.header.navButton.size` | `{spacing.40}` | `dimension` | `--date-picker-header-nav-button-size` |
| `datePicker.header.navButton.borderRadius` | `{borderRadius.6}` | `dimension` | `--date-picker-header-nav-button-border-radius` |
| `datePicker.header.navButton.background.default` | `{color.background.base.tertiary}` | `color` | `--date-picker-header-nav-button-background-default` |
| `datePicker.header.navButton.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--date-picker-header-nav-button-background-hover` |
| `datePicker.header.navButton.background.active` | `{color.background.base.tertiary-active}` | `color` | `--date-picker-header-nav-button-background-active` |
| `datePicker.header.navButton.iconSize` | `{spacing.20}` | `dimension` | `--date-picker-header-nav-button-icon-size` |
| `datePicker.header.navButton.iconColor.default` | `{color.icon.base.default}` | `color` | `--date-picker-header-nav-button-icon-color-default` |
| `datePicker.header.navButton.iconColor.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--date-picker-header-nav-button-icon-color-disabled` |
| `datePicker.dayLabel.height` | `{spacing.32}` | `dimension` | `--date-picker-day-label-height` |
| `datePicker.dayLabel.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--date-picker-day-label-font-family` |
| `datePicker.dayLabel.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--date-picker-day-label-font-weight` |
| `datePicker.dayLabel.fontSize` | `{fontSize.14}` | `dimension` | `--date-picker-day-label-font-size` |
| `datePicker.dayLabel.lineHeight` | `{lineHeight.20}` | `dimension` | `--date-picker-day-label-line-height` |
| `datePicker.dayLabel.color` | `{color.text.base.tertiary}` | `color` | `--date-picker-day-label-color` |
| `datePicker.dayCell.height` | `{spacing.40}` | `dimension` | `--date-picker-day-cell-height` |
| `datePicker.dayCell.borderRadius` | `{borderRadius.6}` | `dimension` | `--date-picker-day-cell-border-radius` |
| `datePicker.dayCell.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--date-picker-day-cell-font-family` |
| `datePicker.dayCell.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--date-picker-day-cell-font-weight` |
| `datePicker.dayCell.fontSize` | `{fontSize.14}` | `dimension` | `--date-picker-day-cell-font-size` |
| `datePicker.dayCell.lineHeight` | `{lineHeight.20}` | `dimension` | `--date-picker-day-cell-line-height` |
| `datePicker.dayCell.borderWidth` | `{borderWidth.1-5}` | `dimension` | `--date-picker-day-cell-border-width` |
| `datePicker.dayCell.focusRingWidth` | `{borderWidth.3}` | `dimension` | `--date-picker-day-cell-focus-ring-width` |
| `datePicker.dayCell.focusRingOffset` | `{borderWidth.1}` | `dimension` | `--date-picker-day-cell-focus-ring-offset` |
| `datePicker.dayCell.default.color` | `{color.text.base.default}` | `color` | `--date-picker-day-cell-default-color` |
| `datePicker.dayCell.hover.background` | `{color.background.base.default-hover}` | `color` | `--date-picker-day-cell-hover-background` |
| `datePicker.dayCell.hover.color` | `{color.text.base.default}` | `color` | `--date-picker-day-cell-hover-color` |
| `datePicker.dayCell.selected.background` | `{color.background.brand.default}` | `color` | `--date-picker-day-cell-selected-background` |
| `datePicker.dayCell.selected.color` | `{color.text.base-inverse.default}` | `color` | `--date-picker-day-cell-selected-color` |
| `datePicker.dayCell.selectedHover.background` | `{color.background.brand.default-hover}` | `color` | `--date-picker-day-cell-selected-hover-background` |
| `datePicker.dayCell.selectedHover.color` | `{color.text.base-inverse.default}` | `color` | `--date-picker-day-cell-selected-hover-color` |
| `datePicker.dayCell.today.color` | `{color.text.brand.default}` | `color` | `--date-picker-day-cell-today-color` |
| `datePicker.dayCell.today.borderColor` | `{color.border.brand.default}` | `color` | `--date-picker-day-cell-today-border-color` |
| `datePicker.dayCell.todayHover.background` | `{color.background.brand.secondary}` | `color` | `--date-picker-day-cell-today-hover-background` |
| `datePicker.dayCell.todayHover.color` | `{color.text.brand.default}` | `color` | `--date-picker-day-cell-today-hover-color` |
| `datePicker.dayCell.inRange.background` | `{color.background.brand.secondary}` | `color` | `--date-picker-day-cell-in-range-background` |
| `datePicker.dayCell.inRange.color` | `{color.text.brand.default}` | `color` | `--date-picker-day-cell-in-range-color` |
| `datePicker.dayCell.outsideMonth.color` | `{color.text.base.tertiary}` | `color` | `--date-picker-day-cell-outside-month-color` |
| `datePicker.dayCell.disabled.color` | `{color.text.disabled.default}` | `color` | `--date-picker-day-cell-disabled-color` |
| `datePicker.dayCell.focusRing` | `{color.background.brand.focus-ring}` | `color` | `--date-picker-day-cell-focus-ring` |
| `datePicker.yearGrid.rowGap` | `{spacing.8}` | `dimension` | `--date-picker-year-grid-row-gap` |
| `datePicker.yearGrid.cellHeight` | `{spacing.40}` | `dimension` | `--date-picker-year-grid-cell-height` |
| `datePicker.yearGrid.cellBorderRadius` | `{borderRadius.8}` | `dimension` | `--date-picker-year-grid-cell-border-radius` |
| `datePicker.monthGrid.rowGap` | `{spacing.8}` | `dimension` | `--date-picker-month-grid-row-gap` |
| `datePicker.monthGrid.cellHeight` | `{spacing.40}` | `dimension` | `--date-picker-month-grid-cell-height` |
| `datePicker.monthGrid.cellBorderRadius` | `{borderRadius.8}` | `dimension` | `--date-picker-month-grid-cell-border-radius` |
| `datePicker.dragHandle.width` | `{spacing.48}` | `dimension` | `--date-picker-drag-handle-width` |
| `datePicker.dragHandle.height` | `{spacing.4}` | `dimension` | `--date-picker-drag-handle-height` |
| `datePicker.dragHandle.background` | `{color.background.base.tertiary}` | `color` | `--date-picker-drag-handle-background` |
| `datePicker.dragHandle.borderRadius` | `{borderRadius.full}` | `dimension` | `--date-picker-drag-handle-border-radius` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/date-picker.tokens.json</code></summary>

```json
{
  "datePicker": {
    "container": {
      "width": {
        "desktop": { "$value": "320px", "$type": "dimension" },
        "mobile": { "$value": "100%", "$type": "dimension" },
        "docked": { "$value": "320px", "$type": "dimension" }
      },
      "padding": {
        "desktop": { "$value": "{spacing.12}", "$type": "dimension" },
        "mobile": { "$value": "{spacing.16}", "$type": "dimension" },
        "docked": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.12}", "$type": "dimension" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "shadow": { "$value": "{dropShadow.300}", "$type": "string" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "header": {
      "height": { "$value": "{spacing.40}", "$type": "dimension" },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "title": {
        "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
        "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
        "fontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
        "lineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
        "color": { "$value": "{color.text.base.default}", "$type": "color" }
      },
      "navButton": {
        "size": { "$value": "{spacing.40}", "$type": "dimension" },
        "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
        "background": {
          "default": { "$value": "{color.background.base.tertiary}", "$type": "color" },
          "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
          "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
        },
        "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
        "iconColor": {
          "default": { "$value": "{color.icon.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
        }
      }
    },
    "dayLabel": {
      "height": { "$value": "{spacing.32}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    },
    "dayCell": {
      "height": { "$value": "{spacing.40}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "borderWidth": { "$value": "{borderWidth.1-5}", "$type": "dimension" },
      "focusRingWidth": {
        "$value": "{borderWidth.3}",
        "$type": "dimension",
        "$comment": "Figma 'Focus Ring/Small' blue spread = 3 (node 489:8302)."
      },
      "focusRingOffset": {
        "$value": "{borderWidth.1}",
        "$type": "dimension",
        "$comment": "Figma 'Focus Ring/Small' white halo spread = 1."
      },
      "default": {
        "color": { "$value": "{color.text.base.default}", "$type": "color" }
      },
      "hover": {
        "background": {
          "$value": "{color.background.base.default-hover}",
          "$type": "color",
          "$comment": "#f5f5f5 per Figma 489:9011 (was base.tertiary #f1f1f1)."
        },
        "color": { "$value": "{color.text.base.default}", "$type": "color" }
      },
      "selected": {
        "$comment": "Selected cell text sits on a saturated brand background — semantic `base.default-on-color` resolves to black which fails contrast (~3:1). Use the inverse text token (white) for WCAG 1.4.3 AA. Matches Figma 470:32032.",
        "background": { "$value": "{color.background.brand.default}", "$type": "color" },
        "color": { "$value": "{color.text.base-inverse.default}", "$type": "color" }
      },
      "selectedHover": {
        "background": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
        "color": { "$value": "{color.text.base-inverse.default}", "$type": "color" }
      },
      "today": {
        "color": { "$value": "{color.text.brand.default}", "$type": "color" },
        "borderColor": { "$value": "{color.border.brand.default}", "$type": "color" }
      },
      "todayHover": {
        "background": { "$value": "{color.background.brand.secondary}", "$type": "color" },
        "color": { "$value": "{color.text.brand.default}", "$type": "color" }
      },
      "inRange": {
        "background": { "$value": "{color.background.brand.secondary}", "$type": "color" },
        "color": { "$value": "{color.text.brand.default}", "$type": "color" }
      },
      "outsideMonth": {
        "$comment": "Outside-month cells are still clickable (they navigate to that month), so they must meet WCAG 1.4.3 AA contrast. The disabled token would fail (2.12:1 vs white).",
        "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
      },
      "disabled": {
        "color": { "$value": "{color.text.disabled.default}", "$type": "color" }
      },
      "focusRing": {
        "$value": "{color.background.brand.focus-ring}",
        "$type": "color",
        "$comment": "#3379db per Figma 'Focus Ring/Small' (node 489:8302); was palette.blue-sky.200 #ccdef6 — too pale (that's 'Focus Ring/Large') and a tier-purity violation. Semantic token matches mud-checkbox."
      }
    },
    "yearGrid": {
      "rowGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "cellHeight": { "$value": "{spacing.40}", "$type": "dimension" },
      "cellBorderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" }
    },
    "monthGrid": {
      "rowGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "cellHeight": { "$value": "{spacing.40}", "$type": "dimension" },
      "cellBorderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" }
    },
    "dragHandle": {
      "width": { "$value": "{spacing.48}", "$type": "dimension" },
      "height": { "$value": "{spacing.4}", "$type": "dimension" },
      "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/file-input.tokens.json`

_86 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `fileInput.field.gap` | `{spacing.8}` | `dimension` | `--file-input-field-gap` |
| `fileInput.dropzone.minHeight.md` | `{spacing.120}` | `dimension` | `--file-input-dropzone-min-height-md` |
| `fileInput.dropzone.minHeight.lg` | `{spacing.120}` | `dimension` | `--file-input-dropzone-min-height-lg` |
| `fileInput.dropzone.paddingBlock.md` | `{spacing.24}` | `dimension` | `--file-input-dropzone-padding-block-md` |
| `fileInput.dropzone.paddingBlock.lg` | `{spacing.32}` | `dimension` | `--file-input-dropzone-padding-block-lg` |
| `fileInput.dropzone.paddingInline.md` | `{spacing.20}` | `dimension` | `--file-input-dropzone-padding-inline-md` |
| `fileInput.dropzone.paddingInline.lg` | `{spacing.24}` | `dimension` | `--file-input-dropzone-padding-inline-lg` |
| `fileInput.dropzone.gap.md` | `{spacing.16}` | `dimension` | `--file-input-dropzone-gap-md` |
| `fileInput.dropzone.gap.lg` | `{spacing.20}` | `dimension` | `--file-input-dropzone-gap-lg` |
| `fileInput.dropzone.borderRadius` | `{borderRadius.8}` | `dimension` | `--file-input-dropzone-border-radius` |
| `fileInput.dropzone.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--file-input-dropzone-border-width-default` |
| `fileInput.dropzone.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--file-input-dropzone-border-width-emphasized` |
| `fileInput.dropzone.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--file-input-dropzone-focus-ring-width` |
| `fileInput.dropzone.transitionDuration` | `150ms` | `duration` | `--file-input-dropzone-transition-duration` |
| `fileInput.dropzone.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--file-input-dropzone-transition-timing-function` |
| `fileInput.dropzone.iconCircle.size.md` | `{spacing.40}` | `dimension` | `--file-input-dropzone-icon-circle-size-md` |
| `fileInput.dropzone.iconCircle.size.lg` | `{spacing.48}` | `dimension` | `--file-input-dropzone-icon-circle-size-lg` |
| `fileInput.dropzone.iconCircle.padding.md` | `{spacing.8}` | `dimension` | `--file-input-dropzone-icon-circle-padding-md` |
| `fileInput.dropzone.iconCircle.padding.lg` | `{spacing.12}` | `dimension` | `--file-input-dropzone-icon-circle-padding-lg` |
| `fileInput.dropzone.iconCircle.background` | `{color.background.base.tertiary}` | `color` | `--file-input-dropzone-icon-circle-background` |
| `fileInput.dropzone.iconCircle.backgroundDisabled` | `{color.background.disabled.default}` | `color` | `--file-input-dropzone-icon-circle-background-disabled` |
| `fileInput.dropzone.iconGlyph.size.md` | `{spacing.20}` | `dimension` | `--file-input-dropzone-icon-glyph-size-md` |
| `fileInput.dropzone.iconGlyph.size.lg` | `{spacing.24}` | `dimension` | `--file-input-dropzone-icon-glyph-size-lg` |
| `fileInput.dropzone.iconGlyph.color` | `{color.icon.base.default}` | `color` | `--file-input-dropzone-icon-glyph-color` |
| `fileInput.dropzone.iconGlyph.colorDisabled` | `{color.icon.disabled.on-disabled}` | `color` | `--file-input-dropzone-icon-glyph-color-disabled` |
| `fileInput.dropzone.cta.gap` | `{spacing.6}` | `dimension` | `--file-input-dropzone-cta-gap` |
| `fileInput.dropzone.cta.body.color` | `{color.text.base.default}` | `color` | `--file-input-dropzone-cta-body-color` |
| `fileInput.dropzone.cta.body.colorDisabled` | `{color.text.disabled.on-disabled}` | `color` | `--file-input-dropzone-cta-body-color-disabled` |
| `fileInput.dropzone.cta.link.color` | `{color.text.brand.default}` | `color` | `--file-input-dropzone-cta-link-color` |
| `fileInput.dropzone.cta.link.colorHover` | `{color.text.brand.default-hover}` | `color` | `--file-input-dropzone-cta-link-color-hover` |
| `fileInput.dropzone.cta.link.colorDisabled` | `{color.text.disabled.on-disabled}` | `color` | `--file-input-dropzone-cta-link-color-disabled` |
| `fileInput.caption.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--file-input-caption-font-family` |
| `fileInput.caption.fontSize` | `{fontSize.14}` | `dimension` | `--file-input-caption-font-size` |
| `fileInput.caption.lineHeight` | `{lineHeight.20}` | `dimension` | `--file-input-caption-line-height` |
| `fileInput.caption.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--file-input-caption-font-weight` |
| `fileInput.caption.gap` | `{spacing.24}` | `dimension` | `--file-input-caption-gap` |
| `fileInput.caption.marginBlockStart` | `{spacing.12}` | `dimension` | `--file-input-caption-margin-block-start` |
| `fileInput.caption.color.default` | `{color.text.base.tertiary}` | `color` | `--file-input-caption-color-default` |
| `fileInput.caption.color.disabled` | `{color.text.base.tertiary}` | `color` | `--file-input-caption-color-disabled` |
| `fileInput.primaryText.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--file-input-primary-text-font-family` |
| `fileInput.primaryText.fontSize.md` | `{fontSize.14}` | `dimension` | `--file-input-primary-text-font-size-md` |
| `fileInput.primaryText.fontSize.lg` | `{fontSize.16}` | `dimension` | `--file-input-primary-text-font-size-lg` |
| `fileInput.primaryText.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--file-input-primary-text-line-height-md` |
| `fileInput.primaryText.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--file-input-primary-text-line-height-lg` |
| `fileInput.primaryText.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--file-input-primary-text-font-weight` |
| `fileInput.secondaryText.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--file-input-secondary-text-font-family` |
| `fileInput.secondaryText.fontSize` | `{fontSize.14}` | `dimension` | `--file-input-secondary-text-font-size` |
| `fileInput.secondaryText.lineHeight` | `{lineHeight.20}` | `dimension` | `--file-input-secondary-text-line-height` |
| `fileInput.secondaryText.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--file-input-secondary-text-font-weight` |
| `fileInput.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--file-input-label-font-family` |
| `fileInput.label.fontSize` | `{fontSize.14}` | `dimension` | `--file-input-label-font-size` |
| `fileInput.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--file-input-label-line-height` |
| `fileInput.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--file-input-label-font-weight` |
| `fileInput.label.gap` | `{spacing.4}` | `dimension` | `--file-input-label-gap` |
| `fileInput.label.color.default` | `{color.text.base.secondary}` | `color` | `--file-input-label-color-default` |
| `fileInput.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--file-input-label-color-disabled` |
| `fileInput.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--file-input-label-required-mark-size` |
| `fileInput.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--file-input-label-required-mark-color` |
| `fileInput.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--file-input-assistive-font-family` |
| `fileInput.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--file-input-assistive-font-size` |
| `fileInput.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--file-input-assistive-line-height` |
| `fileInput.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--file-input-assistive-font-weight` |
| `fileInput.assistive.gap.default` | `{spacing.6}` | `dimension` | `--file-input-assistive-gap-default` |
| `fileInput.assistive.gap.error` | `{spacing.4}` | `dimension` | `--file-input-assistive-gap-error` |
| `fileInput.assistive.iconSize` | `{spacing.20}` | `dimension` | `--file-input-assistive-icon-size` |
| `fileInput.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--file-input-assistive-color-default` |
| `fileInput.assistive.color.error` | `{color.text.danger.default}` | `color` | `--file-input-assistive-color-error` |
| `fileInput.list.gap` | `{spacing.8}` | `dimension` | `--file-input-list-gap` |
| `fileInput.list.marginBlockStart` | `{spacing.12}` | `dimension` | `--file-input-list-margin-block-start` |
| `fileInput.background.default` | `{color.background.base.default}` | `color` | `--file-input-background-default` |
| `fileInput.background.active` | `{color.background.brand.secondary}` | `color` | `--file-input-background-active` |
| `fileInput.background.disabled` | `{color.background.disabled.default}` | `color` | `--file-input-background-disabled` |
| `fileInput.border.default` | `{color.border.base.default}` | `color` | `--file-input-border-default` |
| `fileInput.border.hover` | `{color.border.brand.default}` | `color` | `--file-input-border-hover` |
| `fileInput.border.focus` | `{color.border.brand.default}` | `color` | `--file-input-border-focus` |
| `fileInput.border.active` | `{color.border.brand.default}` | `color` | `--file-input-border-active` |
| `fileInput.border.disabled` | `{color.border.disabled.default}` | `color` | `--file-input-border-disabled` |
| `fileInput.border.invalid` | `{color.border.danger.default}` | `color` | `--file-input-border-invalid` |
| `fileInput.text.primary` | `{color.text.base.default}` | `color` | `--file-input-text-primary` |
| `fileInput.text.secondary` | `{color.text.base.tertiary}` | `color` | `--file-input-text-secondary` |
| `fileInput.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--file-input-text-disabled` |
| `fileInput.icon.default` | `{color.icon.base.secondary}` | `color` | `--file-input-icon-default` |
| `fileInput.icon.accent` | `{color.icon.brand.default}` | `color` | `--file-input-icon-accent` |
| `fileInput.icon.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--file-input-icon-disabled` |
| `fileInput.focusRing.default` | `{palette.blue-sky.200}` | `color` | `--file-input-focus-ring-default` |
| `fileInput.focusRing.invalid` | `{palette.red.200}` | `color` | `--file-input-focus-ring-invalid` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/file-input.tokens.json</code></summary>

```json
{
  "fileInput": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "dropzone": {
      "minHeight": {
        "md": { "$value": "{spacing.120}", "$type": "dimension" },
        "lg": { "$value": "{spacing.120}", "$type": "dimension" }
      },
      "paddingBlock": {
        "md": { "$value": "{spacing.24}", "$type": "dimension" },
        "lg": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" },
      "iconCircle": {
        "size": {
          "md": { "$value": "{spacing.40}", "$type": "dimension" },
          "lg": { "$value": "{spacing.48}", "$type": "dimension" }
        },
        "padding": {
          "md": { "$value": "{spacing.8}", "$type": "dimension" },
          "lg": { "$value": "{spacing.12}", "$type": "dimension" }
        },
        "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "backgroundDisabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "iconGlyph": {
        "size": {
          "md": { "$value": "{spacing.20}", "$type": "dimension" },
          "lg": { "$value": "{spacing.24}", "$type": "dimension" }
        },
        "color": { "$value": "{color.icon.base.default}", "$type": "color" },
        "colorDisabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "cta": {
        "gap": { "$value": "{spacing.6}", "$type": "dimension" },
        "body": {
          "color": { "$value": "{color.text.base.default}", "$type": "color" },
          "colorDisabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
        },
        "link": {
          "color": { "$value": "{color.text.brand.default}", "$type": "color" },
          "colorHover": { "$value": "{color.text.brand.default-hover}", "$type": "color" },
          "colorDisabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
        }
      }
    },
    "caption": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.24}", "$type": "dimension" },
      "marginBlockStart": { "$value": "{spacing.12}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "$comment": "Disabled captions remain informational text (supported formats, max size) — keep them at the same readable contrast as the default state. The semantic `color.text.disabled.on-disabled` token resolves to ~#b2b2b2 which fails WCAG 1.4.3 AA on white (2.12:1).",
        "disabled": { "$value": "{color.text.base.tertiary}", "$type": "color" }
      }
    },
    "primaryText": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "secondaryText": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "list": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "marginBlockStart": { "$value": "{spacing.12}", "$type": "dimension" }
    },
    "background": {
      "default": { "$value": "{color.background.base.default}", "$type": "color" },
      "active": { "$value": "{color.background.brand.secondary}", "$type": "color" },
      "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
    },
    "border": {
      "default": { "$value": "{color.border.base.default}", "$type": "color" },
      "hover": { "$value": "{color.border.brand.default}", "$type": "color" },
      "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
      "active": { "$value": "{color.border.brand.default}", "$type": "color" },
      "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" },
      "invalid": { "$value": "{color.border.danger.default}", "$type": "color" }
    },
    "text": {
      "primary": { "$value": "{color.text.base.default}", "$type": "color" },
      "secondary": { "$value": "{color.text.base.tertiary}", "$type": "color" },
      "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
    },
    "icon": {
      "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
      "accent": { "$value": "{color.icon.brand.default}", "$type": "color" },
      "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
    },
    "focusRing": {
      "default": { "$value": "{palette.blue-sky.200}", "$type": "color" },
      "invalid": { "$value": "{palette.red.200}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/file-item.tokens.json`

_42 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `fileItem.container.minHeight` | `{spacing.48}` | `dimension` | `--file-item-container-min-height` |
| `fileItem.container.paddingBlock` | `{spacing.8}` | `dimension` | `--file-item-container-padding-block` |
| `fileItem.container.paddingInline` | `{spacing.12}` | `dimension` | `--file-item-container-padding-inline` |
| `fileItem.container.gap` | `{spacing.12}` | `dimension` | `--file-item-container-gap` |
| `fileItem.container.borderRadius` | `{borderRadius.6}` | `dimension` | `--file-item-container-border-radius` |
| `fileItem.container.borderWidth` | `{borderWidth.1}` | `dimension` | `--file-item-container-border-width` |
| `fileItem.container.transitionDuration` | `150ms` | `duration` | `--file-item-container-transition-duration` |
| `fileItem.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--file-item-container-transition-timing-function` |
| `fileItem.icon.size` | `{spacing.24}` | `dimension` | `--file-item-icon-size` |
| `fileItem.icon.color.uploaded` | `{color.icon.base.secondary}` | `color` | `--file-item-icon-color-uploaded` |
| `fileItem.icon.color.uploading` | `{color.icon.brand.default}` | `color` | `--file-item-icon-color-uploading` |
| `fileItem.icon.color.success` | `{color.icon.positive.default}` | `color` | `--file-item-icon-color-success` |
| `fileItem.icon.color.error` | `{color.icon.danger.default}` | `color` | `--file-item-icon-color-error` |
| `fileItem.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--file-item-icon-color-disabled` |
| `fileItem.text.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--file-item-text-font-family` |
| `fileItem.text.filename.fontSize` | `{fontSize.14}` | `dimension` | `--file-item-text-filename-font-size` |
| `fileItem.text.filename.lineHeight` | `{lineHeight.20}` | `dimension` | `--file-item-text-filename-line-height` |
| `fileItem.text.filename.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--file-item-text-filename-font-weight` |
| `fileItem.text.filename.color.default` | `{color.text.base.default}` | `color` | `--file-item-text-filename-color-default` |
| `fileItem.text.filename.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--file-item-text-filename-color-disabled` |
| `fileItem.text.meta.fontSize` | `{fontSize.12}` | `dimension` | `--file-item-text-meta-font-size` |
| `fileItem.text.meta.lineHeight` | `{lineHeight.16}` | `dimension` | `--file-item-text-meta-line-height` |
| `fileItem.text.meta.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--file-item-text-meta-font-weight` |
| `fileItem.text.meta.color.default` | `{color.text.base.tertiary}` | `color` | `--file-item-text-meta-color-default` |
| `fileItem.text.meta.color.error` | `{color.text.danger.default}` | `color` | `--file-item-text-meta-color-error` |
| `fileItem.text.meta.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--file-item-text-meta-color-disabled` |
| `fileItem.remove.size` | `{spacing.32}` | `dimension` | `--file-item-remove-size` |
| `fileItem.remove.iconSize` | `{spacing.20}` | `dimension` | `--file-item-remove-icon-size` |
| `fileItem.remove.borderRadius` | `{borderRadius.6}` | `dimension` | `--file-item-remove-border-radius` |
| `fileItem.remove.color.default` | `{color.icon.base.secondary}` | `color` | `--file-item-remove-color-default` |
| `fileItem.remove.color.hover` | `{color.icon.danger.default}` | `color` | `--file-item-remove-color-hover` |
| `fileItem.remove.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--file-item-remove-color-disabled` |
| `fileItem.remove.background.hover` | `{color.background.danger.secondary}` | `color` | `--file-item-remove-background-hover` |
| `fileItem.remove.focusRing` | `{palette.blue-sky.200}` | `color` | `--file-item-remove-focus-ring` |
| `fileItem.background.default` | `{color.background.base.tertiary}` | `color` | `--file-item-background-default` |
| `fileItem.background.error` | `{color.background.base.default}` | `color` | `--file-item-background-error` |
| `fileItem.background.disabled` | `{color.background.disabled.default}` | `color` | `--file-item-background-disabled` |
| `fileItem.border.uploaded` | `{color.border.base.default}` | `color` | `--file-item-border-uploaded` |
| `fileItem.border.uploading` | `{color.border.brand.default}` | `color` | `--file-item-border-uploading` |
| `fileItem.border.success` | `{color.border.positive.default}` | `color` | `--file-item-border-success` |
| `fileItem.border.error` | `{color.border.danger.default}` | `color` | `--file-item-border-error` |
| `fileItem.border.disabled` | `{color.border.disabled.default}` | `color` | `--file-item-border-disabled` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/file-item.tokens.json</code></summary>

```json
{
  "fileItem": {
    "container": {
      "minHeight": { "$value": "{spacing.48}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.8}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.12}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "icon": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" },
      "color": {
        "uploaded": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "uploading": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "success": { "$value": "{color.icon.positive.default}", "$type": "color" },
        "error": { "$value": "{color.icon.danger.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "text": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "filename": {
        "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
        "color": {
          "default": { "$value": "{color.text.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
        }
      },
      "meta": {
        "fontSize": { "$value": "{fontSize.12}", "$type": "dimension" },
        "lineHeight": { "$value": "{lineHeight.16}", "$type": "dimension" },
        "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
        "color": {
          "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
          "error": { "$value": "{color.text.danger.default}", "$type": "color" },
          "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
        }
      }
    },
    "remove": {
      "size": { "$value": "{spacing.32}", "$type": "dimension" },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.icon.danger.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "background": {
        "hover": { "$value": "{color.background.danger.secondary}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "background": {
      "default": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "error": { "$value": "{color.background.base.default}", "$type": "color" },
      "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
    },
    "border": {
      "uploaded": { "$value": "{color.border.base.default}", "$type": "color" },
      "uploading": { "$value": "{color.border.brand.default}", "$type": "color" },
      "success": { "$value": "{color.border.positive.default}", "$type": "color" },
      "error": { "$value": "{color.border.danger.default}", "$type": "color" },
      "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/footer.tokens.json`

_102 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `footer.container.background` | `{color.background.base.default}` | `color` | `--footer-container-background` |
| `footer.container.color` | `{color.text.base.default}` | `color` | `--footer-container-color` |
| `footer.container.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--footer-container-font-family` |
| `footer.container.paddingInlineDesktop` | `{spacing.80}` | `dimension` | `--footer-container-padding-inline-desktop` |
| `footer.container.paddingInlineTablet` | `{spacing.48}` | `dimension` | `--footer-container-padding-inline-tablet` |
| `footer.container.paddingInlineMobile` | `{spacing.16}` | `dimension` | `--footer-container-padding-inline-mobile` |
| `footer.container.paddingBlockStartDesktop` | `{spacing.64}` | `dimension` | `--footer-container-padding-block-start-desktop` |
| `footer.container.paddingBlockStartMobile` | `{spacing.40}` | `dimension` | `--footer-container-padding-block-start-mobile` |
| `footer.container.paddingBlockEndDesktop` | `{spacing.40}` | `dimension` | `--footer-container-padding-block-end-desktop` |
| `footer.container.paddingBlockEndMobile` | `{spacing.24}` | `dimension` | `--footer-container-padding-block-end-mobile` |
| `footer.container.maxWidth` | `1920px` | `dimension` | `--footer-container-max-width` |
| `footer.primary.gapDesktop` | `{spacing.48}` | `dimension` | `--footer-primary-gap-desktop` |
| `footer.primary.gapMobile` | `{spacing.32}` | `dimension` | `--footer-primary-gap-mobile` |
| `footer.primary.paddingBlockEnd` | `{spacing.48}` | `dimension` | `--footer-primary-padding-block-end` |
| `footer.branding.gap` | `{spacing.20}` | `dimension` | `--footer-branding-gap` |
| `footer.branding.maxWidth` | `420px` | `dimension` | `--footer-branding-max-width` |
| `footer.branding.headlineFontSize` | `{fontSize.32}` | `dimension` | `--footer-branding-headline-font-size` |
| `footer.branding.headlineLineHeight` | `{lineHeight.40}` | `dimension` | `--footer-branding-headline-line-height` |
| `footer.branding.headlineFontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--footer-branding-headline-font-weight` |
| `footer.branding.headlineLetterSpacing` | `-1px` | `dimension` | `--footer-branding-headline-letter-spacing` |
| `footer.branding.headlineColor` | `{color.text.base.default}` | `color` | `--footer-branding-headline-color` |
| `footer.branding.taglineFontSize` | `{fontSize.14}` | `dimension` | `--footer-branding-tagline-font-size` |
| `footer.branding.taglineLineHeight` | `{lineHeight.20}` | `dimension` | `--footer-branding-tagline-line-height` |
| `footer.branding.taglineFontWeight` | `{fontWeight.regular}` | `fontWeight` | `--footer-branding-tagline-font-weight` |
| `footer.branding.taglineColor` | `{color.text.base.secondary}` | `color` | `--footer-branding-tagline-color` |
| `footer.divider.color` | `{color.border.base.default}` | `color` | `--footer-divider-color` |
| `footer.divider.width` | `{borderWidth.1}` | `dimension` | `--footer-divider-width` |
| `footer.divider.marginBlock` | `{spacing.32}` | `dimension` | `--footer-divider-margin-block` |
| `footer.columns.gapDesktop` | `{spacing.48}` | `dimension` | `--footer-columns-gap-desktop` |
| `footer.columns.gapTablet` | `{spacing.32}` | `dimension` | `--footer-columns-gap-tablet` |
| `footer.columns.gapMobile` | `{spacing.16}` | `dimension` | `--footer-columns-gap-mobile` |
| `footer.columns.minWidth` | `180px` | `dimension` | `--footer-columns-min-width` |
| `footer.column.gap` | `{spacing.16}` | `dimension` | `--footer-column-gap` |
| `footer.column.titleFontSize` | `{fontSize.16}` | `dimension` | `--footer-column-title-font-size` |
| `footer.column.titleLineHeight` | `{lineHeight.24}` | `dimension` | `--footer-column-title-line-height` |
| `footer.column.titleFontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--footer-column-title-font-weight` |
| `footer.column.titleColor` | `{color.text.base.default}` | `color` | `--footer-column-title-color` |
| `footer.column.linkGap` | `{spacing.12}` | `dimension` | `--footer-column-link-gap` |
| `footer.contact.gap` | `{spacing.16}` | `dimension` | `--footer-contact-gap` |
| `footer.contact.rowGap` | `{spacing.8}` | `dimension` | `--footer-contact-row-gap` |
| `footer.contact.iconSize` | `20px` | `dimension` | `--footer-contact-icon-size` |
| `footer.contact.iconColor` | `{color.icon.base.secondary}` | `color` | `--footer-contact-icon-color` |
| `footer.contact.labelFontSize` | `{fontSize.14}` | `dimension` | `--footer-contact-label-font-size` |
| `footer.contact.labelLineHeight` | `{lineHeight.20}` | `dimension` | `--footer-contact-label-line-height` |
| `footer.contact.labelColor` | `{color.text.base.secondary}` | `color` | `--footer-contact-label-color` |
| `footer.contact.addressMaxWidth` | `260px` | `dimension` | `--footer-contact-address-max-width` |
| `footer.social.gap` | `{spacing.12}` | `dimension` | `--footer-social-gap` |
| `footer.social.groupGap` | `{spacing.16}` | `dimension` | `--footer-social-group-gap` |
| `footer.social.iconSize` | `20px` | `dimension` | `--footer-social-icon-size` |
| `footer.social.labelFontSize` | `{fontSize.14}` | `dimension` | `--footer-social-label-font-size` |
| `footer.social.labelLineHeight` | `{lineHeight.20}` | `dimension` | `--footer-social-label-line-height` |
| `footer.social.labelColor` | `{color.text.base.tertiary}` | `color` | `--footer-social-label-color` |
| `footer.social.buttonSize` | `40px` | `dimension` | `--footer-social-button-size` |
| `footer.social.buttonBackground` | `{color.background.base.secondary}` | `color` | `--footer-social-button-background` |
| `footer.social.buttonBackgroundHover` | `{color.background.base.tertiary}` | `color` | `--footer-social-button-background-hover` |
| `footer.social.buttonBorderRadius` | `{borderRadius.full}` | `dimension` | `--footer-social-button-border-radius` |
| `footer.social.buttonColor` | `{color.icon.base.default}` | `color` | `--footer-social-button-color` |
| `footer.social.focusRing` | `{color.background.brand.focus-ring}` | `color` | `--footer-social-focus-ring` |
| `footer.partners.gap` | `{spacing.32}` | `dimension` | `--footer-partners-gap` |
| `footer.partners.rowGap` | `{spacing.16}` | `dimension` | `--footer-partners-row-gap` |
| `footer.partners.logoMaxHeight` | `40px` | `dimension` | `--footer-partners-logo-max-height` |
| `footer.partners.labelFontSize` | `{fontSize.10}` | `dimension` | `--footer-partners-label-font-size` |
| `footer.partners.labelLineHeight` | `{lineHeight.12}` | `dimension` | `--footer-partners-label-line-height` |
| `footer.partners.labelColor` | `{color.text.base.tertiary}` | `color` | `--footer-partners-label-color` |
| `footer.accessibility.marginBlockStart` | `{spacing.32}` | `dimension` | `--footer-accessibility-margin-block-start` |
| `footer.accessibility.fontSize` | `{fontSize.14}` | `dimension` | `--footer-accessibility-font-size` |
| `footer.accessibility.lineHeight` | `{lineHeight.20}` | `dimension` | `--footer-accessibility-line-height` |
| `footer.accessibility.color` | `{color.text.base.tertiary}` | `color` | `--footer-accessibility-color` |
| `footer.legal.background` | `{color.background.base-inverse.default}` | `color` | `--footer-legal-background` |
| `footer.legal.color` | `{color.text.base-inverse.default}` | `color` | `--footer-legal-color` |
| `footer.legal.paddingInlineDesktop` | `{spacing.80}` | `dimension` | `--footer-legal-padding-inline-desktop` |
| `footer.legal.paddingInlineMobile` | `{spacing.16}` | `dimension` | `--footer-legal-padding-inline-mobile` |
| `footer.legal.paddingBlockDesktop` | `{spacing.16}` | `dimension` | `--footer-legal-padding-block-desktop` |
| `footer.legal.paddingBlockMobile` | `{spacing.20}` | `dimension` | `--footer-legal-padding-block-mobile` |
| `footer.legal.gap` | `{spacing.24}` | `dimension` | `--footer-legal-gap` |
| `footer.legal.fontSize` | `{fontSize.14}` | `dimension` | `--footer-legal-font-size` |
| `footer.legal.lineHeight` | `{lineHeight.20}` | `dimension` | `--footer-legal-line-height` |
| `footer.legal.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--footer-legal-font-weight` |
| `footer.locale.background` | `transparent` | `color` | `--footer-locale-background` |
| `footer.locale.backgroundHover` | `{color.background.base.secondary}` | `color` | `--footer-locale-background-hover` |
| `footer.locale.color` | `{color.text.base.default}` | `color` | `--footer-locale-color` |
| `footer.locale.borderRadius` | `{borderRadius.8}` | `dimension` | `--footer-locale-border-radius` |
| `footer.locale.paddingInline` | `{spacing.12}` | `dimension` | `--footer-locale-padding-inline` |
| `footer.locale.paddingBlock` | `{spacing.8}` | `dimension` | `--footer-locale-padding-block` |
| `footer.locale.gap` | `{spacing.8}` | `dimension` | `--footer-locale-gap` |
| `footer.locale.fontSize` | `{fontSize.14}` | `dimension` | `--footer-locale-font-size` |
| `footer.locale.lineHeight` | `{lineHeight.20}` | `dimension` | `--footer-locale-line-height` |
| `footer.locale.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--footer-locale-font-weight` |
| `footer.locale.iconSize` | `16px` | `dimension` | `--footer-locale-icon-size` |
| `footer.locale.menuBackground` | `{color.background.base.default}` | `color` | `--footer-locale-menu-background` |
| `footer.locale.menuBorder` | `{color.border.base.default}` | `color` | `--footer-locale-menu-border` |
| `footer.locale.menuBorderRadius` | `{borderRadius.8}` | `dimension` | `--footer-locale-menu-border-radius` |
| `footer.locale.menuShadow` | `0 0 0.5px 0 rgba(0, 0, 0, 0.15), 0 1px 3px 0 rgba(0, 0, 0, 0.08), 0 5px 12px 0 rgba(0, 0, 0, 0.08)` | `shadow` | `--footer-locale-menu-shadow` |
| `footer.locale.menuMinWidth` | `140px` | `dimension` | `--footer-locale-menu-min-width` |
| `footer.locale.menuItemPaddingBlock` | `{spacing.8}` | `dimension` | `--footer-locale-menu-item-padding-block` |
| `footer.locale.menuItemPaddingInline` | `{spacing.12}` | `dimension` | `--footer-locale-menu-item-padding-inline` |
| `footer.locale.menuItemBackgroundHover` | `{color.background.base.secondary}` | `color` | `--footer-locale-menu-item-background-hover` |
| `footer.locale.menuItemActiveBackground` | `{color.background.brand.secondary}` | `color` | `--footer-locale-menu-item-active-background` |
| `footer.locale.menuItemActiveColor` | `{color.text.brand.default}` | `color` | `--footer-locale-menu-item-active-color` |
| `footer.focusRing.color` | `{color.background.brand.focus-ring}` | `color` | `--footer-focus-ring-color` |
| `footer.focusRing.width` | `{borderWidth.2}` | `dimension` | `--footer-focus-ring-width` |
| `footer.focusRing.offset` | `{spacing.2}` | `dimension` | `--footer-focus-ring-offset` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/footer.tokens.json</code></summary>

```json
{
  "footer": {
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "color": { "$value": "{color.text.base.default}", "$type": "color" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "paddingInlineDesktop": { "$value": "{spacing.80}", "$type": "dimension" },
      "paddingInlineTablet": { "$value": "{spacing.48}", "$type": "dimension" },
      "paddingInlineMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlockStartDesktop": { "$value": "{spacing.64}", "$type": "dimension" },
      "paddingBlockStartMobile": { "$value": "{spacing.40}", "$type": "dimension" },
      "paddingBlockEndDesktop": { "$value": "{spacing.40}", "$type": "dimension" },
      "paddingBlockEndMobile": { "$value": "{spacing.24}", "$type": "dimension" },
      "maxWidth": { "$value": "1920px", "$type": "dimension" }
    },
    "primary": {
      "gapDesktop": { "$value": "{spacing.48}", "$type": "dimension" },
      "gapMobile": { "$value": "{spacing.32}", "$type": "dimension" },
      "paddingBlockEnd": { "$value": "{spacing.48}", "$type": "dimension" }
    },
    "branding": {
      "gap": { "$value": "{spacing.20}", "$type": "dimension" },
      "maxWidth": { "$value": "420px", "$type": "dimension" },
      "headlineFontSize": { "$value": "{fontSize.32}", "$type": "dimension" },
      "headlineLineHeight": { "$value": "{lineHeight.40}", "$type": "dimension" },
      "headlineFontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "headlineLetterSpacing": { "$value": "-1px", "$type": "dimension" },
      "headlineColor": { "$value": "{color.text.base.default}", "$type": "color" },
      "taglineFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "taglineLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "taglineFontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "taglineColor": { "$value": "{color.text.base.secondary}", "$type": "color" }
    },
    "divider": {
      "color": { "$value": "{color.border.base.default}", "$type": "color" },
      "width": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "marginBlock": { "$value": "{spacing.32}", "$type": "dimension" }
    },
    "columns": {
      "gapDesktop": { "$value": "{spacing.48}", "$type": "dimension" },
      "gapTablet": { "$value": "{spacing.32}", "$type": "dimension" },
      "gapMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "minWidth": { "$value": "180px", "$type": "dimension" }
    },
    "column": {
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "titleFontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "titleLineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "titleFontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "titleColor": { "$value": "{color.text.base.default}", "$type": "color" },
      "linkGap": { "$value": "{spacing.12}", "$type": "dimension" }
    },
    "contact": {
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "rowGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "iconSize": { "$value": "20px", "$type": "dimension" },
      "iconColor": { "$value": "{color.icon.base.secondary}", "$type": "color" },
      "labelFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "labelColor": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "addressMaxWidth": { "$value": "260px", "$type": "dimension" }
    },
    "social": {
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "groupGap": { "$value": "{spacing.16}", "$type": "dimension" },
      "iconSize": { "$value": "20px", "$type": "dimension" },
      "labelFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "labelColor": { "$value": "{color.text.base.tertiary}", "$type": "color" },
      "buttonSize": { "$value": "40px", "$type": "dimension" },
      "buttonBackground": { "$value": "{color.background.base.secondary}", "$type": "color" },
      "buttonBackgroundHover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "buttonBorderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "buttonColor": { "$value": "{color.icon.base.default}", "$type": "color" },
      "focusRing": { "$value": "{color.background.brand.focus-ring}", "$type": "color" }
    },
    "partners": {
      "gap": { "$value": "{spacing.32}", "$type": "dimension" },
      "rowGap": { "$value": "{spacing.16}", "$type": "dimension" },
      "logoMaxHeight": { "$value": "40px", "$type": "dimension" },
      "labelFontSize": { "$value": "{fontSize.10}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.12}", "$type": "dimension" },
      "labelColor": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    },
    "accessibility": {
      "marginBlockStart": { "$value": "{spacing.32}", "$type": "dimension" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    },
    "legal": {
      "background": { "$value": "{color.background.base-inverse.default}", "$type": "color" },
      "color": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
      "paddingInlineDesktop": { "$value": "{spacing.80}", "$type": "dimension" },
      "paddingInlineMobile": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlockDesktop": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlockMobile": { "$value": "{spacing.20}", "$type": "dimension" },
      "gap": { "$value": "{spacing.24}", "$type": "dimension" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "locale": {
      "background": { "$value": "transparent", "$type": "color" },
      "backgroundHover": { "$value": "{color.background.base.secondary}", "$type": "color" },
      "color": { "$value": "{color.text.base.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.12}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.8}", "$type": "dimension" },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "iconSize": { "$value": "16px", "$type": "dimension" },
      "menuBackground": { "$value": "{color.background.base.default}", "$type": "color" },
      "menuBorder": { "$value": "{color.border.base.default}", "$type": "color" },
      "menuBorderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "menuShadow": {
        "$value": "0 0 0.5px 0 rgba(0, 0, 0, 0.15), 0 1px 3px 0 rgba(0, 0, 0, 0.08), 0 5px 12px 0 rgba(0, 0, 0, 0.08)",
        "$type": "shadow"
      },
      "menuMinWidth": { "$value": "140px", "$type": "dimension" },
      "menuItemPaddingBlock": { "$value": "{spacing.8}", "$type": "dimension" },
      "menuItemPaddingInline": { "$value": "{spacing.12}", "$type": "dimension" },
      "menuItemBackgroundHover": { "$value": "{color.background.base.secondary}", "$type": "color" },
      "menuItemActiveBackground": { "$value": "{color.background.brand.secondary}", "$type": "color" },
      "menuItemActiveColor": { "$value": "{color.text.brand.default}", "$type": "color" }
    },
    "focusRing": {
      "color": { "$value": "{color.background.brand.focus-ring}", "$type": "color" },
      "width": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "offset": { "$value": "{spacing.2}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/header.tokens.json`

_112 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `header.container.background` | `{color.background.base.default}` | `color` | `--header-container-background` |
| `header.container.borderColor` | `{color.border.base.default}` | `color` | `--header-container-border-color` |
| `header.container.borderWidth` | `{borderWidth.1}` | `dimension` | `--header-container-border-width` |
| `header.container.maxInlineSize` | `1280px` | `dimension` | `--header-container-max-inline-size` |
| `header.container.paddingInline` | `{spacing.24}` | `dimension` | `--header-container-padding-inline` |
| `header.preHeader.blockSize` | `{spacing.32}` | `dimension` | `--header-pre-header-block-size` |
| `header.preHeader.background` | `{color.background.base.tertiary}` | `color` | `--header-pre-header-background` |
| `header.preHeader.crestSize` | `{spacing.24}` | `dimension` | `--header-pre-header-crest-size` |
| `header.preHeader.labelFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-pre-header-label-font-family` |
| `header.preHeader.labelFontSize` | `{fontSize.12}` | `dimension` | `--header-pre-header-label-font-size` |
| `header.preHeader.labelLineHeight` | `{lineHeight.16}` | `dimension` | `--header-pre-header-label-line-height` |
| `header.preHeader.labelFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--header-pre-header-label-font-weight` |
| `header.preHeader.labelColor` | `{color.text.base.default}` | `color` | `--header-pre-header-label-color` |
| `header.preHeader.labelGap` | `{spacing.4}` | `dimension` | `--header-pre-header-label-gap` |
| `header.language.gap` | `{spacing.12}` | `dimension` | `--header-language-gap` |
| `header.language.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-language-font-family` |
| `header.language.fontSize` | `{fontSize.14}` | `dimension` | `--header-language-font-size` |
| `header.language.lineHeight` | `{lineHeight.20}` | `dimension` | `--header-language-line-height` |
| `header.language.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--header-language-font-weight` |
| `header.language.activeColor` | `{color.text.base.default}` | `color` | `--header-language-active-color` |
| `header.language.inactiveColor` | `{color.text.base.secondary}` | `color` | `--header-language-inactive-color` |
| `header.mainBar.blockSize` | `72px` | `dimension` | `--header-main-bar-block-size` |
| `header.mainBar.gap` | `{spacing.24}` | `dimension` | `--header-main-bar-gap` |
| `header.mainBar.leadingGap` | `{spacing.16}` | `dimension` | `--header-main-bar-leading-gap` |
| `header.mainBar.logoSize` | `52px` | `dimension` | `--header-main-bar-logo-size` |
| `header.navItem.blockSize` | `72px` | `dimension` | `--header-nav-item-block-size` |
| `header.navItem.paddingInlineStart` | `{spacing.16}` | `dimension` | `--header-nav-item-padding-inline-start` |
| `header.navItem.paddingInlineEnd` | `{spacing.8}` | `dimension` | `--header-nav-item-padding-inline-end` |
| `header.navItem.gap` | `{spacing.6}` | `dimension` | `--header-nav-item-gap` |
| `header.navItem.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-nav-item-font-family` |
| `header.navItem.fontSize` | `{fontSize.16}` | `dimension` | `--header-nav-item-font-size` |
| `header.navItem.lineHeight` | `{lineHeight.24}` | `dimension` | `--header-nav-item-line-height` |
| `header.navItem.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--header-nav-item-font-weight` |
| `header.navItem.color` | `{color.text.base.default}` | `color` | `--header-nav-item-color` |
| `header.navItem.disabledColor` | `{color.text.base.tertiary}` | `color` | `--header-nav-item-disabled-color` |
| `header.navItem.hoverBackground` | `{color.background.base.default-hover}` | `color` | `--header-nav-item-hover-background` |
| `header.navItem.chevronSize` | `16px` | `dimension` | `--header-nav-item-chevron-size` |
| `header.action.size` | `{spacing.40}` | `dimension` | `--header-action-size` |
| `header.action.iconSize` | `{spacing.24}` | `dimension` | `--header-action-icon-size` |
| `header.action.gap` | `{spacing.8}` | `dimension` | `--header-action-gap` |
| `header.action.borderRadius` | `{borderRadius.full}` | `dimension` | `--header-action-border-radius` |
| `header.action.iconColor` | `{color.icon.base.default}` | `color` | `--header-action-icon-color` |
| `header.action.hoverBackground` | `{color.background.base.default-hover}` | `color` | `--header-action-hover-background` |
| `header.action.buttonsGap` | `{spacing.12}` | `dimension` | `--header-action-buttons-gap` |
| `header.megaMenu.background` | `{color.background.base.default}` | `color` | `--header-mega-menu-background` |
| `header.megaMenu.padding` | `{spacing.40}` | `dimension` | `--header-mega-menu-padding` |
| `header.megaMenu.columnsGap` | `{spacing.24}` | `dimension` | `--header-mega-menu-columns-gap` |
| `header.megaMenu.columnGap` | `{spacing.16}` | `dimension` | `--header-mega-menu-column-gap` |
| `header.megaMenu.maxInlineSize` | `1168px` | `dimension` | `--header-mega-menu-max-inline-size` |
| `header.megaMenu.shadow` | `{dropShadow.200}` | `string` | `--header-mega-menu-shadow` |
| `header.megaMenu.headingFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-mega-menu-heading-font-family` |
| `header.megaMenu.headingFontSize` | `{fontSize.18}` | `dimension` | `--header-mega-menu-heading-font-size` |
| `header.megaMenu.headingLineHeight` | `{lineHeight.26}` | `dimension` | `--header-mega-menu-heading-line-height` |
| `header.megaMenu.headingFontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--header-mega-menu-heading-font-weight` |
| `header.megaMenu.headingLetterSpacing` | `-0.18px` | `dimension` | `--header-mega-menu-heading-letter-spacing` |
| `header.megaMenu.headingColor` | `{color.text.base.default}` | `color` | `--header-mega-menu-heading-color` |
| `header.megaMenu.optionPadding` | `{spacing.4}` | `dimension` | `--header-mega-menu-option-padding` |
| `header.megaMenu.optionGap` | `{spacing.12}` | `dimension` | `--header-mega-menu-option-gap` |
| `header.megaMenu.optionFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-mega-menu-option-font-family` |
| `header.megaMenu.optionFontSize` | `{fontSize.14}` | `dimension` | `--header-mega-menu-option-font-size` |
| `header.megaMenu.optionLineHeight` | `{lineHeight.20}` | `dimension` | `--header-mega-menu-option-line-height` |
| `header.megaMenu.optionFontWeight` | `{fontWeight.regular}` | `fontWeight` | `--header-mega-menu-option-font-weight` |
| `header.megaMenu.optionColor` | `{color.text.base.secondary}` | `color` | `--header-mega-menu-option-color` |
| `header.megaMenu.optionHoverColor` | `{color.text.base.default}` | `color` | `--header-mega-menu-option-hover-color` |
| `header.services.background` | `{color.background.base.default}` | `color` | `--header-services-background` |
| `header.services.borderRadius` | `{borderRadius.16}` | `dimension` | `--header-services-border-radius` |
| `header.services.paddingBlockStart` | `{spacing.20}` | `dimension` | `--header-services-padding-block-start` |
| `header.services.paddingBlockEnd` | `{spacing.16}` | `dimension` | `--header-services-padding-block-end` |
| `header.services.paddingInline` | `{spacing.16}` | `dimension` | `--header-services-padding-inline` |
| `header.services.gap` | `{spacing.12}` | `dimension` | `--header-services-gap` |
| `header.services.minInlineSize` | `348px` | `dimension` | `--header-services-min-inline-size` |
| `header.services.shadow` | `{dropShadow.300}` | `string` | `--header-services-shadow` |
| `header.services.titleFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-services-title-font-family` |
| `header.services.titleFontSize` | `{fontSize.16}` | `dimension` | `--header-services-title-font-size` |
| `header.services.titleLineHeight` | `{lineHeight.24}` | `dimension` | `--header-services-title-line-height` |
| `header.services.titleFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--header-services-title-font-weight` |
| `header.services.titleColor` | `{color.text.base.default}` | `color` | `--header-services-title-color` |
| `header.services.gridGap` | `{spacing.8}` | `dimension` | `--header-services-grid-gap` |
| `header.services.cardBackground` | `{color.background.base.secondary}` | `color` | `--header-services-card-background` |
| `header.services.cardHoverBackground` | `{color.background.base.tertiary}` | `color` | `--header-services-card-hover-background` |
| `header.services.cardBlockSize` | `60px` | `dimension` | `--header-services-card-block-size` |
| `header.services.cardBorderRadius` | `{borderRadius.12}` | `dimension` | `--header-services-card-border-radius` |
| `header.services.cardPaddingInlineStart` | `{spacing.12}` | `dimension` | `--header-services-card-padding-inline-start` |
| `header.services.cardPaddingInlineEnd` | `{spacing.8}` | `dimension` | `--header-services-card-padding-inline-end` |
| `header.services.cardLogoBlockSize` | `40px` | `dimension` | `--header-services-card-logo-block-size` |
| `header.mobile.background` | `{color.background.base.default}` | `color` | `--header-mobile-background` |
| `header.mobile.borderColor` | `{color.border.base.default}` | `color` | `--header-mobile-border-color` |
| `header.mobile.borderWidth` | `{borderWidth.1}` | `dimension` | `--header-mobile-border-width` |
| `header.mobile.barBlockSize` | `64px` | `dimension` | `--header-mobile-bar-block-size` |
| `header.mobile.barPaddingInlineStart` | `{spacing.16}` | `dimension` | `--header-mobile-bar-padding-inline-start` |
| `header.mobile.barPaddingInlineEnd` | `{spacing.12}` | `dimension` | `--header-mobile-bar-padding-inline-end` |
| `header.mobile.logoSize` | `40px` | `dimension` | `--header-mobile-logo-size` |
| `header.mobile.rightGap` | `{spacing.16}` | `dimension` | `--header-mobile-right-gap` |
| `header.mobile.actionsGap` | `{spacing.8}` | `dimension` | `--header-mobile-actions-gap` |
| `header.mobile.actionSize` | `{spacing.40}` | `dimension` | `--header-mobile-action-size` |
| `header.mobile.actionIconSize` | `{spacing.24}` | `dimension` | `--header-mobile-action-icon-size` |
| `header.mobile.actionIconColor` | `{color.icon.base.default}` | `color` | `--header-mobile-action-icon-color` |
| `header.mobile.actionHoverBackground` | `{color.background.base.default-hover}` | `color` | `--header-mobile-action-hover-background` |
| `header.mobile.drawerPadding` | `{spacing.16}` | `dimension` | `--header-mobile-drawer-padding` |
| `header.mobile.drawerSectionGap` | `{spacing.16}` | `dimension` | `--header-mobile-drawer-section-gap` |
| `header.mobile.drawerActionsGap` | `{spacing.12}` | `dimension` | `--header-mobile-drawer-actions-gap` |
| `header.mobile.language.gap` | `{spacing.6}` | `dimension` | `--header-mobile-language-gap` |
| `header.mobile.language.paddingInline` | `{spacing.12}` | `dimension` | `--header-mobile-language-padding-inline` |
| `header.mobile.language.blockSize` | `{spacing.32}` | `dimension` | `--header-mobile-language-block-size` |
| `header.mobile.language.background` | `{color.background.base.secondary}` | `color` | `--header-mobile-language-background` |
| `header.mobile.language.borderRadius` | `{borderRadius.full}` | `dimension` | `--header-mobile-language-border-radius` |
| `header.mobile.language.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--header-mobile-language-font-family` |
| `header.mobile.language.fontSize` | `{fontSize.14}` | `dimension` | `--header-mobile-language-font-size` |
| `header.mobile.language.lineHeight` | `{lineHeight.20}` | `dimension` | `--header-mobile-language-line-height` |
| `header.mobile.language.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--header-mobile-language-font-weight` |
| `header.mobile.language.color` | `{color.text.base.default}` | `color` | `--header-mobile-language-color` |
| `header.mobile.language.iconSize` | `{spacing.20}` | `dimension` | `--header-mobile-language-icon-size` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/header.tokens.json</code></summary>

```json
{
  "header": {
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "maxInlineSize": {
        "$value": "1280px",
        "$type": "dimension",
        "$comment": "Figma header inner content max-width 1280px."
      },
      "paddingInline": { "$value": "{spacing.24}", "$type": "dimension" }
    },
    "preHeader": {
      "blockSize": {
        "$value": "{spacing.32}",
        "$type": "dimension",
        "$comment": "Figma .top-section height 32px."
      },
      "background": {
        "$value": "{color.background.base.tertiary}",
        "$type": "color",
        "$comment": "Figma pre-header bg #f1f1f1."
      },
      "crestSize": {
        "$value": "{spacing.24}",
        "$type": "dimension",
        "$comment": "Figma AGE government crest 24px."
      },
      "labelFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "labelFontSize": { "$value": "{fontSize.12}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "labelFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "labelColor": {
        "$value": "{color.text.base.default}",
        "$type": "color",
        "$comment": "Figma 'Guvernul Republicii Moldova' #121212."
      },
      "labelGap": { "$value": "{spacing.4}", "$type": "dimension" }
    },
    "language": {
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "activeColor": {
        "$value": "{color.text.base.default}",
        "$type": "color",
        "$comment": "Figma active language (Ro) #121212, underlined."
      },
      "inactiveColor": {
        "$value": "{color.text.base.secondary}",
        "$type": "color",
        "$comment": "Figma inactive language (Ru/En) #383838."
      }
    },
    "mainBar": {
      "blockSize": {
        "$value": "72px",
        "$type": "dimension",
        "$comment": "Figma main navigation height 72px (not on the 4px spacing scale)."
      },
      "gap": {
        "$value": "{spacing.24}",
        "$type": "dimension",
        "$comment": "Figma gap between the action cluster and the button cluster."
      },
      "leadingGap": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma gap between the logo and the nav items."
      },
      "logoSize": {
        "$value": "52px",
        "$type": "dimension",
        "$comment": "Figma EVO main logo 52px."
      }
    },
    "navItem": {
      "blockSize": {
        "$value": "72px",
        "$type": "dimension",
        "$comment": "Figma navigation-item full bar height 72px."
      },
      "paddingInlineStart": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingInlineEnd": {
        "$value": "{spacing.8}",
        "$type": "dimension",
        "$comment": "Figma nav item pl-16 pr-8 (room for the trailing chevron)."
      },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "color": {
        "$value": "{color.text.base.default}",
        "$type": "color",
        "$comment": "Figma nav item label #121212."
      },
      "disabledColor": {
        "$value": "{color.text.base.tertiary}",
        "$type": "color",
        "$comment": "Figma disabled nav item label #757575."
      },
      "hoverBackground": {
        "$value": "{color.background.base.default-hover}",
        "$type": "color",
        "$comment": "Figma nav item hover/open bg #f5f5f5."
      },
      "chevronSize": {
        "$value": "16px",
        "$type": "dimension",
        "$comment": "Figma nav item trailing chevron-bottom-small 16px."
      }
    },
    "action": {
      "size": {
        "$value": "{spacing.40}",
        "$type": "dimension",
        "$comment": "Figma .header-action circular hit area 40px."
      },
      "iconSize": {
        "$value": "{spacing.24}",
        "$type": "dimension",
        "$comment": "Figma action icon 24px."
      },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "iconColor": {
        "$value": "{color.icon.base.default}",
        "$type": "color",
        "$comment": "Figma action icons #121212."
      },
      "hoverBackground": { "$value": "{color.background.base.default-hover}", "$type": "color" },
      "buttonsGap": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma gap between the two CTA buttons."
      }
    },
    "megaMenu": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "padding": {
        "$value": "{spacing.40}",
        "$type": "dimension",
        "$comment": "Figma header-menu Full padding 40px."
      },
      "columnsGap": {
        "$value": "{spacing.24}",
        "$type": "dimension",
        "$comment": "Figma gap between the 3 columns."
      },
      "columnGap": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma gap between heading / separator / options."
      },
      "maxInlineSize": {
        "$value": "1168px",
        "$type": "dimension",
        "$comment": "Figma inner container max-width 1168px."
      },
      "shadow": { "$value": "{dropShadow.200}", "$type": "string" },
      "headingFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "headingFontSize": { "$value": "{fontSize.18}", "$type": "dimension" },
      "headingLineHeight": { "$value": "{lineHeight.26}", "$type": "dimension" },
      "headingFontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "headingLetterSpacing": {
        "$value": "-0.18px",
        "$type": "dimension",
        "$comment": "Figma H5 tracking -0.18px."
      },
      "headingColor": {
        "$value": "{color.text.base.default}",
        "$type": "color",
        "$comment": "Figma mega-menu heading #121212."
      },
      "optionPadding": { "$value": "{spacing.4}", "$type": "dimension" },
      "optionGap": { "$value": "{spacing.12}", "$type": "dimension" },
      "optionFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "optionFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "optionLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "optionFontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "optionColor": {
        "$value": "{color.text.base.secondary}",
        "$type": "color",
        "$comment": "Figma mega-menu option label #383838."
      },
      "optionHoverColor": {
        "$value": "{color.text.base.default}",
        "$type": "color",
        "$comment": "Option link hover (darken to #121212; Figma shows no hover, this is a sensible link affordance)."
      }
    },
    "services": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.16}", "$type": "dimension" },
      "paddingBlockStart": {
        "$value": "{spacing.20}",
        "$type": "dimension",
        "$comment": "Figma services-dropdown pt-20."
      },
      "paddingBlockEnd": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma services-dropdown pb-16."
      },
      "paddingInline": { "$value": "{spacing.16}", "$type": "dimension" },
      "gap": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma gap title → grid → button."
      },
      "minInlineSize": {
        "$value": "348px",
        "$type": "dimension",
        "$comment": "Figma services-dropdown width 348px (parity with evo-web)."
      },
      "shadow": { "$value": "{dropShadow.300}", "$type": "string" },
      "titleFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "titleFontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "titleLineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "titleFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "titleColor": {
        "$value": "{color.text.base.default}",
        "$type": "color",
        "$comment": "Figma DS title 'Platforme utile' #121212 Onest Medium 16 (note: evo-web landing.css used 18/600 — DS Figma differs)."
      },
      "gridGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "cardBackground": {
        "$value": "{color.background.base.secondary}",
        "$type": "color",
        "$comment": "Figma platform card bg #f5f5f5."
      },
      "cardHoverBackground": {
        "$value": "{color.background.base.tertiary}",
        "$type": "color",
        "$comment": "Subtle darken on hover (#f1f1f1); evo-web parity uses gray-200."
      },
      "cardBlockSize": {
        "$value": "60px",
        "$type": "dimension",
        "$comment": "Figma platform card height 60px."
      },
      "cardBorderRadius": { "$value": "{borderRadius.12}", "$type": "dimension" },
      "cardPaddingInlineStart": { "$value": "{spacing.12}", "$type": "dimension" },
      "cardPaddingInlineEnd": { "$value": "{spacing.8}", "$type": "dimension" },
      "cardLogoBlockSize": {
        "$value": "40px",
        "$type": "dimension",
        "$comment": "Figma platform logo height 40px (with-verb logo)."
      }
    },
    "mobile": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "barBlockSize": {
        "$value": "64px",
        "$type": "dimension",
        "$comment": "Figma header-general-mobile bar height 64px."
      },
      "barPaddingInlineStart": { "$value": "{spacing.16}", "$type": "dimension" },
      "barPaddingInlineEnd": { "$value": "{spacing.12}", "$type": "dimension" },
      "logoSize": {
        "$value": "40px",
        "$type": "dimension",
        "$comment": "Figma mobile logo 40px."
      },
      "rightGap": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma gap between the auth button and the action cluster."
      },
      "actionsGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "actionSize": {
        "$value": "{spacing.40}",
        "$type": "dimension",
        "$comment": "Figma .header-action circular hit area 40px."
      },
      "actionIconSize": { "$value": "{spacing.24}", "$type": "dimension" },
      "actionIconColor": { "$value": "{color.icon.base.default}", "$type": "color" },
      "actionHoverBackground": { "$value": "{color.background.base.default-hover}", "$type": "color" },
      "drawerPadding": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma drawer content inline padding 16px."
      },
      "drawerSectionGap": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma gap between drawer sections (search / nav / secondary / actions)."
      },
      "drawerActionsGap": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma gap between the two bottom CTA buttons."
      },
      "language": {
        "gap": { "$value": "{spacing.6}", "$type": "dimension" },
        "paddingInline": { "$value": "{spacing.12}", "$type": "dimension" },
        "blockSize": {
          "$value": "{spacing.32}",
          "$type": "dimension",
          "$comment": "Figma drawer language pill height 32px."
        },
        "background": {
          "$value": "{color.background.base.secondary}",
          "$type": "color",
          "$comment": "Figma drawer language pill bg #f5f5f5."
        },
        "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
        "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
        "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
        "color": { "$value": "{color.text.base.default}", "$type": "color" },
        "iconSize": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/info-box.tokens.json`

_54 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `infoBox.container.paddingBlock` | `{spacing.12}` | `dimension` | `--info-box-container-padding-block` |
| `infoBox.container.paddingInline` | `{spacing.16}` | `dimension` | `--info-box-container-padding-inline` |
| `infoBox.container.gap` | `{spacing.12}` | `dimension` | `--info-box-container-gap` |
| `infoBox.container.stackGap` | `{spacing.8}` | `dimension` | `--info-box-container-stack-gap` |
| `infoBox.container.headingGap` | `{spacing.4}` | `dimension` | `--info-box-container-heading-gap` |
| `infoBox.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--info-box-container-border-radius` |
| `infoBox.container.transitionDuration` | `150ms` | `duration` | `--info-box-container-transition-duration` |
| `infoBox.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--info-box-container-transition-timing-function` |
| `infoBox.icon.size` | `{spacing.20}` | `dimension` | `--info-box-icon-size` |
| `infoBox.heading.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--info-box-heading-font-family` |
| `infoBox.heading.fontSize` | `{fontSize.16}` | `dimension` | `--info-box-heading-font-size` |
| `infoBox.heading.lineHeight` | `{lineHeight.24}` | `dimension` | `--info-box-heading-line-height` |
| `infoBox.heading.fontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--info-box-heading-font-weight` |
| `infoBox.body.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--info-box-body-font-family` |
| `infoBox.body.fontSize` | `{fontSize.14}` | `dimension` | `--info-box-body-font-size` |
| `infoBox.body.lineHeight` | `{lineHeight.20}` | `dimension` | `--info-box-body-line-height` |
| `infoBox.body.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--info-box-body-font-weight` |
| `infoBox.close.sizePointer` | `{spacing.32}` | `dimension` | `--info-box-close-size-pointer` |
| `infoBox.close.sizeTouch` | `{spacing.40}` | `dimension` | `--info-box-close-size-touch` |
| `infoBox.close.iconSize` | `{spacing.16}` | `dimension` | `--info-box-close-icon-size` |
| `infoBox.close.borderRadius` | `{borderRadius.4}` | `dimension` | `--info-box-close-border-radius` |
| `infoBox.close.backgroundHover` | `{color.background.base.tertiary-hover}` | `color` | `--info-box-close-background-hover` |
| `infoBox.strong.info.background` | `{color.background.brand.secondary}` | `color` | `--info-box-strong-info-background` |
| `infoBox.strong.info.text` | `{color.text.base.default}` | `color` | `--info-box-strong-info-text` |
| `infoBox.strong.info.icon` | `{color.icon.brand.default}` | `color` | `--info-box-strong-info-icon` |
| `infoBox.strong.info.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-strong-info-close-icon` |
| `infoBox.strong.info-moderate.background` | `{color.background.brand.secondary}` | `color` | `--info-box-strong-info-moderate-background` |
| `infoBox.strong.info-moderate.text` | `{color.text.base.default}` | `color` | `--info-box-strong-info-moderate-text` |
| `infoBox.strong.info-moderate.icon` | `{color.icon.brand.default}` | `color` | `--info-box-strong-info-moderate-icon` |
| `infoBox.strong.info-moderate.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-strong-info-moderate-close-icon` |
| `infoBox.strong.warning.background` | `{color.background.warning.secondary}` | `color` | `--info-box-strong-warning-background` |
| `infoBox.strong.warning.text` | `{color.text.base.default}` | `color` | `--info-box-strong-warning-text` |
| `infoBox.strong.warning.icon` | `{color.icon.warning.default}` | `color` | `--info-box-strong-warning-icon` |
| `infoBox.strong.warning.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-strong-warning-close-icon` |
| `infoBox.strong.error.background` | `{color.background.danger.secondary}` | `color` | `--info-box-strong-error-background` |
| `infoBox.strong.error.text` | `{color.text.base.default}` | `color` | `--info-box-strong-error-text` |
| `infoBox.strong.error.icon` | `{color.icon.danger.default}` | `color` | `--info-box-strong-error-icon` |
| `infoBox.strong.error.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-strong-error-close-icon` |
| `infoBox.subtle.info.background` | `{color.background.base.secondary}` | `color` | `--info-box-subtle-info-background` |
| `infoBox.subtle.info.text` | `{color.text.base.default}` | `color` | `--info-box-subtle-info-text` |
| `infoBox.subtle.info.icon` | `{color.icon.base.default}` | `color` | `--info-box-subtle-info-icon` |
| `infoBox.subtle.info.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-subtle-info-close-icon` |
| `infoBox.subtle.info-moderate.background` | `{color.background.base.secondary}` | `color` | `--info-box-subtle-info-moderate-background` |
| `infoBox.subtle.info-moderate.text` | `{color.text.base.default}` | `color` | `--info-box-subtle-info-moderate-text` |
| `infoBox.subtle.info-moderate.icon` | `{color.icon.brand.default}` | `color` | `--info-box-subtle-info-moderate-icon` |
| `infoBox.subtle.info-moderate.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-subtle-info-moderate-close-icon` |
| `infoBox.subtle.warning.background` | `{color.background.base.secondary}` | `color` | `--info-box-subtle-warning-background` |
| `infoBox.subtle.warning.text` | `{color.text.base.default}` | `color` | `--info-box-subtle-warning-text` |
| `infoBox.subtle.warning.icon` | `{color.icon.warning.default}` | `color` | `--info-box-subtle-warning-icon` |
| `infoBox.subtle.warning.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-subtle-warning-close-icon` |
| `infoBox.subtle.error.background` | `{color.background.base.secondary}` | `color` | `--info-box-subtle-error-background` |
| `infoBox.subtle.error.text` | `{color.text.base.default}` | `color` | `--info-box-subtle-error-text` |
| `infoBox.subtle.error.icon` | `{color.icon.danger.default}` | `color` | `--info-box-subtle-error-icon` |
| `infoBox.subtle.error.closeIcon` | `{color.icon.base.secondary}` | `color` | `--info-box-subtle-error-close-icon` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/info-box.tokens.json</code></summary>

```json
{
  "infoBox": {
    "container": {
      "paddingBlock": { "$value": "{spacing.12}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.16}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "stackGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "headingGap": { "$value": "{spacing.4}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "icon": {
      "size": { "$value": "{spacing.20}", "$type": "dimension" }
    },
    "heading": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" }
    },
    "body": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "close": {
      "sizePointer": {
        "$value": "{spacing.32}",
        "$type": "dimension",
        "$comment": "Interactive target per the Figma Target Sizes panel: 32px on pointer devices."
      },
      "sizeTouch": {
        "$value": "{spacing.40}",
        "$type": "dimension",
        "$comment": "40px target under `pointer: coarse` for touch ease-of-use."
      },
      "iconSize": { "$value": "{spacing.16}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "backgroundHover": {
        "$value": "{color.background.base.tertiary-hover}",
        "$type": "color",
        "$comment": "All variants carry a dark close × on a light surface, so a neutral light-gray hover keeps the icon legible (~6.9:1)."
      }
    },
    "strong": {
      "info": {
        "background": { "$value": "{color.background.brand.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      },
      "info-moderate": {
        "background": { "$value": "{color.background.brand.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      },
      "warning": {
        "background": { "$value": "{color.background.warning.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": { "$value": "{color.icon.warning.default}", "$type": "color" },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      },
      "error": {
        "background": { "$value": "{color.background.danger.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": { "$value": "{color.icon.danger.default}", "$type": "color" },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      }
    },
    "subtle": {
      "info": {
        "background": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": {
          "$value": "{color.icon.base.default}",
          "$type": "color",
          "$comment": "Neutral (non-brand) icon — the low-emphasis informational treatment."
        },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      },
      "info-moderate": {
        "background": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": {
          "$value": "{color.icon.brand.default}",
          "$type": "color",
          "$comment": "Brand-blue icon — the moderate-emphasis informational treatment."
        },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      },
      "warning": {
        "background": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": { "$value": "{color.icon.warning.default}", "$type": "color" },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      },
      "error": {
        "background": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "text": { "$value": "{color.text.base.default}", "$type": "color" },
        "icon": { "$value": "{color.icon.danger.default}", "$type": "color" },
        "closeIcon": { "$value": "{color.icon.base.secondary}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/inline-message.tokens.json`

_18 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `inlineMessage.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--inline-message-font-family` |
| `inlineMessage.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--inline-message-font-weight` |
| `inlineMessage.gap.small` | `{spacing.4}` | `dimension` | `--inline-message-gap-small` |
| `inlineMessage.gap.medium` | `{spacing.6}` | `dimension` | `--inline-message-gap-medium` |
| `inlineMessage.fontSize.small` | `{fontSize.12}` | `dimension` | `--inline-message-font-size-small` |
| `inlineMessage.fontSize.medium` | `{fontSize.14}` | `dimension` | `--inline-message-font-size-medium` |
| `inlineMessage.lineHeight.small` | `{lineHeight.16}` | `dimension` | `--inline-message-line-height-small` |
| `inlineMessage.lineHeight.medium` | `{lineHeight.20}` | `dimension` | `--inline-message-line-height-medium` |
| `inlineMessage.iconSize.small` | `{spacing.16}` | `dimension` | `--inline-message-icon-size-small` |
| `inlineMessage.iconSize.medium` | `{spacing.20}` | `dimension` | `--inline-message-icon-size-medium` |
| `inlineMessage.info.text` | `{color.text.base.secondary}` | `color` | `--inline-message-info-text` |
| `inlineMessage.info.icon` | `{color.icon.brand.default}` | `color` | `--inline-message-info-icon` |
| `inlineMessage.warning.text` | `{color.text.warning.default}` | `color` | `--inline-message-warning-text` |
| `inlineMessage.warning.icon` | `{color.icon.warning.default}` | `color` | `--inline-message-warning-icon` |
| `inlineMessage.success.text` | `{color.text.positive.on-secondary}` | `color` | `--inline-message-success-text` |
| `inlineMessage.success.icon` | `{color.icon.positive.on-secondary}` | `color` | `--inline-message-success-icon` |
| `inlineMessage.error.text` | `{color.text.danger.default}` | `color` | `--inline-message-error-text` |
| `inlineMessage.error.icon` | `{color.icon.danger.default}` | `color` | `--inline-message-error-icon` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/inline-message.tokens.json</code></summary>

```json
{
  "inlineMessage": {
    "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
    "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
    "gap": {
      "small": { "$value": "{spacing.4}", "$type": "dimension" },
      "medium": { "$value": "{spacing.6}", "$type": "dimension" }
    },
    "fontSize": {
      "small": { "$value": "{fontSize.12}", "$type": "dimension" },
      "medium": { "$value": "{fontSize.14}", "$type": "dimension" }
    },
    "lineHeight": {
      "small": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "medium": { "$value": "{lineHeight.20}", "$type": "dimension" }
    },
    "iconSize": {
      "small": { "$value": "{spacing.16}", "$type": "dimension" },
      "medium": { "$value": "{spacing.20}", "$type": "dimension" }
    },
    "info": {
      "text": {
        "$value": "{color.text.base.secondary}",
        "$type": "color",
        "$comment": "Default/info text is neutral (matches the form-control assistive text); only the icon carries the brand colour."
      },
      "icon": { "$value": "{color.icon.brand.default}", "$type": "color" }
    },
    "warning": {
      "text": { "$value": "{color.text.warning.default}", "$type": "color" },
      "icon": { "$value": "{color.icon.warning.default}", "$type": "color" }
    },
    "success": {
      "text": {
        "$value": "{color.text.positive.on-secondary}",
        "$type": "color",
        "$comment": "Darker green (#027948, ~5.2:1 on white). positive.default (#039855) is 3.73:1 and fails WCAG 1.4.3 AA as body text."
      },
      "icon": { "$value": "{color.icon.positive.on-secondary}", "$type": "color" }
    },
    "error": {
      "text": { "$value": "{color.text.danger.default}", "$type": "color" },
      "icon": { "$value": "{color.icon.danger.default}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/input-chip.tokens.json`

_86 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `inputChip.field.gap` | `{spacing.8}` | `dimension` | `--input-chip-field-gap` |
| `inputChip.container.minHeight.md` | `{spacing.40}` | `dimension` | `--input-chip-container-min-height-md` |
| `inputChip.container.minHeight.lg` | `{spacing.48}` | `dimension` | `--input-chip-container-min-height-lg` |
| `inputChip.container.paddingBlock.md` | `{spacing.4}` | `dimension` | `--input-chip-container-padding-block-md` |
| `inputChip.container.paddingBlock.lg` | `{spacing.6}` | `dimension` | `--input-chip-container-padding-block-lg` |
| `inputChip.container.paddingInline.md` | `{spacing.8}` | `dimension` | `--input-chip-container-padding-inline-md` |
| `inputChip.container.paddingInline.lg` | `{spacing.12}` | `dimension` | `--input-chip-container-padding-inline-lg` |
| `inputChip.container.gap.md` | `{spacing.6}` | `dimension` | `--input-chip-container-gap-md` |
| `inputChip.container.gap.lg` | `{spacing.8}` | `dimension` | `--input-chip-container-gap-lg` |
| `inputChip.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--input-chip-container-border-radius` |
| `inputChip.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--input-chip-container-border-width-default` |
| `inputChip.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--input-chip-container-border-width-emphasized` |
| `inputChip.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--input-chip-container-focus-ring-width` |
| `inputChip.container.transitionDuration` | `150ms` | `duration` | `--input-chip-container-transition-duration` |
| `inputChip.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--input-chip-container-transition-timing-function` |
| `inputChip.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-chip-control-font-family` |
| `inputChip.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--input-chip-control-font-weight` |
| `inputChip.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--input-chip-control-font-size-md` |
| `inputChip.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--input-chip-control-font-size-lg` |
| `inputChip.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--input-chip-control-line-height-md` |
| `inputChip.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--input-chip-control-line-height-lg` |
| `inputChip.control.minWidth` | `{spacing.80}` | `dimension` | `--input-chip-control-min-width` |
| `inputChip.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-chip-label-font-family` |
| `inputChip.label.fontSize` | `{fontSize.14}` | `dimension` | `--input-chip-label-font-size` |
| `inputChip.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--input-chip-label-line-height` |
| `inputChip.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--input-chip-label-font-weight` |
| `inputChip.label.gap` | `{spacing.4}` | `dimension` | `--input-chip-label-gap` |
| `inputChip.label.color.default` | `{color.text.base.secondary}` | `color` | `--input-chip-label-color-default` |
| `inputChip.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--input-chip-label-color-disabled` |
| `inputChip.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--input-chip-label-required-mark-size` |
| `inputChip.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--input-chip-label-required-mark-color` |
| `inputChip.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-chip-assistive-font-family` |
| `inputChip.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--input-chip-assistive-font-size` |
| `inputChip.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--input-chip-assistive-line-height` |
| `inputChip.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--input-chip-assistive-font-weight` |
| `inputChip.assistive.gap.default` | `{spacing.6}` | `dimension` | `--input-chip-assistive-gap-default` |
| `inputChip.assistive.gap.error` | `{spacing.4}` | `dimension` | `--input-chip-assistive-gap-error` |
| `inputChip.assistive.iconSize` | `{spacing.20}` | `dimension` | `--input-chip-assistive-icon-size` |
| `inputChip.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--input-chip-assistive-color-default` |
| `inputChip.assistive.color.error` | `{color.text.danger.default}` | `color` | `--input-chip-assistive-color-error` |
| `inputChip.chip.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-chip-chip-font-family` |
| `inputChip.chip.fontSize` | `{fontSize.14}` | `dimension` | `--input-chip-chip-font-size` |
| `inputChip.chip.lineHeight` | `{lineHeight.20}` | `dimension` | `--input-chip-chip-line-height` |
| `inputChip.chip.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--input-chip-chip-font-weight` |
| `inputChip.chip.height.md` | `{spacing.24}` | `dimension` | `--input-chip-chip-height-md` |
| `inputChip.chip.height.lg` | `{spacing.32}` | `dimension` | `--input-chip-chip-height-lg` |
| `inputChip.chip.paddingBlock.md` | `{spacing.2}` | `dimension` | `--input-chip-chip-padding-block-md` |
| `inputChip.chip.paddingBlock.lg` | `{spacing.4}` | `dimension` | `--input-chip-chip-padding-block-lg` |
| `inputChip.chip.paddingInlineStart.md` | `{spacing.8}` | `dimension` | `--input-chip-chip-padding-inline-start-md` |
| `inputChip.chip.paddingInlineStart.lg` | `{spacing.12}` | `dimension` | `--input-chip-chip-padding-inline-start-lg` |
| `inputChip.chip.paddingInlineEnd.md` | `{spacing.4}` | `dimension` | `--input-chip-chip-padding-inline-end-md` |
| `inputChip.chip.paddingInlineEnd.lg` | `{spacing.6}` | `dimension` | `--input-chip-chip-padding-inline-end-lg` |
| `inputChip.chip.gap.md` | `{spacing.4}` | `dimension` | `--input-chip-chip-gap-md` |
| `inputChip.chip.gap.lg` | `{spacing.6}` | `dimension` | `--input-chip-chip-gap-lg` |
| `inputChip.chip.borderRadius` | `{borderRadius.6}` | `dimension` | `--input-chip-chip-border-radius` |
| `inputChip.chip.maxInlineSize` | `100%` | `dimension` | `--input-chip-chip-max-inline-size` |
| `inputChip.chip.background.default` | `{color.background.base.secondary}` | `color` | `--input-chip-chip-background-default` |
| `inputChip.chip.background.hover` | `{color.background.base.tertiary}` | `color` | `--input-chip-chip-background-hover` |
| `inputChip.chip.background.disabled` | `{color.background.disabled.default}` | `color` | `--input-chip-chip-background-disabled` |
| `inputChip.chip.label.default` | `{color.text.base.default}` | `color` | `--input-chip-chip-label-default` |
| `inputChip.chip.label.disabled` | `{color.text.base.secondary}` | `color` | `--input-chip-chip-label-disabled` |
| `inputChip.chip.remove.size.md` | `{spacing.16}` | `dimension` | `--input-chip-chip-remove-size-md` |
| `inputChip.chip.remove.size.lg` | `{spacing.20}` | `dimension` | `--input-chip-chip-remove-size-lg` |
| `inputChip.chip.remove.iconSize.md` | `{spacing.12}` | `dimension` | `--input-chip-chip-remove-icon-size-md` |
| `inputChip.chip.remove.iconSize.lg` | `{spacing.16}` | `dimension` | `--input-chip-chip-remove-icon-size-lg` |
| `inputChip.chip.remove.color.default` | `{color.icon.base.secondary}` | `color` | `--input-chip-chip-remove-color-default` |
| `inputChip.chip.remove.color.hover` | `{color.icon.base.default}` | `color` | `--input-chip-chip-remove-color-hover` |
| `inputChip.chip.remove.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--input-chip-chip-remove-color-disabled` |
| `inputChip.chip.remove.background.hover` | `{color.background.base.tertiary}` | `color` | `--input-chip-chip-remove-background-hover` |
| `inputChip.chip.remove.background.focus` | `{color.background.brand.secondary}` | `color` | `--input-chip-chip-remove-background-focus` |
| `inputChip.chip.remove.borderRadius` | `{borderRadius.full}` | `dimension` | `--input-chip-chip-remove-border-radius` |
| `inputChip.chip.focusRingColor` | `{palette.blue-sky.200}` | `color` | `--input-chip-chip-focus-ring-color` |
| `inputChip.default.background.default` | `{color.background.base.default}` | `color` | `--input-chip-default-background-default` |
| `inputChip.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--input-chip-default-background-disabled` |
| `inputChip.default.border.default` | `{color.border.base.default}` | `color` | `--input-chip-default-border-default` |
| `inputChip.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--input-chip-default-border-hover` |
| `inputChip.default.border.focus` | `{color.border.brand.default}` | `color` | `--input-chip-default-border-focus` |
| `inputChip.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--input-chip-default-border-disabled` |
| `inputChip.default.text.default` | `{color.text.base.default}` | `color` | `--input-chip-default-text-default` |
| `inputChip.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--input-chip-default-text-placeholder` |
| `inputChip.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--input-chip-default-text-disabled` |
| `inputChip.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--input-chip-default-focus-ring` |
| `inputChip.destructive.border.default` | `{color.border.danger.default}` | `color` | `--input-chip-destructive-border-default` |
| `inputChip.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--input-chip-destructive-border-hover` |
| `inputChip.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--input-chip-destructive-border-focus` |
| `inputChip.destructive.focusRing` | `{palette.red.200}` | `color` | `--input-chip-destructive-focus-ring` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/input-chip.tokens.json</code></summary>

```json
{
  "inputChip": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "minHeight": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingBlock": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "lg": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.6}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "minWidth": { "$value": "{spacing.80}", "$type": "dimension" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "chip": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "height": {
        "md": { "$value": "{spacing.24}", "$type": "dimension" },
        "lg": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "paddingBlock": {
        "md": { "$value": "{spacing.2}", "$type": "dimension" },
        "lg": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "paddingInlineStart": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "paddingInlineEnd": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "lg": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "lg": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "maxInlineSize": { "$value": "100%", "$type": "dimension" },
      "background": {
        "default": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "label": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "$comment": "Disabled chips remain readable informational text — bumped to base.secondary to meet WCAG 1.4.3 AA (4.5:1) on the disabled chip background. `color.text.disabled.on-disabled` (~#b2b2b2) is 1.87:1 and `text.base.tertiary` (~#757575) is 4.07:1 — both fail.",
        "disabled": { "$value": "{color.text.base.secondary}", "$type": "color" }
      },
      "remove": {
        "size": {
          "md": { "$value": "{spacing.16}", "$type": "dimension" },
          "lg": { "$value": "{spacing.20}", "$type": "dimension" }
        },
        "iconSize": {
          "md": { "$value": "{spacing.12}", "$type": "dimension" },
          "lg": { "$value": "{spacing.16}", "$type": "dimension" }
        },
        "color": {
          "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
          "hover": { "$value": "{color.icon.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
        },
        "background": {
          "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
          "focus": { "$value": "{color.background.brand.secondary}", "$type": "color" }
        },
        "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" }
      },
      "focusRingColor": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "destructive": {
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/input.tokens.json`

_79 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `input.field.gap` | `{spacing.8}` | `dimension` | `--input-field-gap` |
| `input.container.height.md` | `{spacing.40}` | `dimension` | `--input-container-height-md` |
| `input.container.height.lg` | `{spacing.48}` | `dimension` | `--input-container-height-lg` |
| `input.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--input-container-padding-inline-md` |
| `input.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--input-container-padding-inline-lg` |
| `input.container.gap.md` | `{spacing.8}` | `dimension` | `--input-container-gap-md` |
| `input.container.gap.lg` | `{spacing.8}` | `dimension` | `--input-container-gap-lg` |
| `input.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--input-container-border-radius` |
| `input.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--input-container-border-width-default` |
| `input.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--input-container-border-width-emphasized` |
| `input.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--input-container-focus-ring-width` |
| `input.container.transitionDuration` | `150ms` | `duration` | `--input-container-transition-duration` |
| `input.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--input-container-transition-timing-function` |
| `input.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-control-font-family` |
| `input.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--input-control-font-weight` |
| `input.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--input-control-font-size-md` |
| `input.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--input-control-font-size-lg` |
| `input.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--input-control-line-height-md` |
| `input.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--input-control-line-height-lg` |
| `input.control.loadingOpacity` | `0.6` | `number` | `--input-control-loading-opacity` |
| `input.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-label-font-family` |
| `input.label.fontSize` | `{fontSize.14}` | `dimension` | `--input-label-font-size` |
| `input.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--input-label-line-height` |
| `input.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--input-label-font-weight` |
| `input.label.gap` | `{spacing.4}` | `dimension` | `--input-label-gap` |
| `input.label.color.default` | `{color.text.base.secondary}` | `color` | `--input-label-color-default` |
| `input.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--input-label-color-disabled` |
| `input.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--input-label-required-mark-size` |
| `input.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--input-label-required-mark-color` |
| `input.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--input-assistive-font-family` |
| `input.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--input-assistive-font-size` |
| `input.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--input-assistive-line-height` |
| `input.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--input-assistive-font-weight` |
| `input.assistive.gap.default` | `{spacing.6}` | `dimension` | `--input-assistive-gap-default` |
| `input.assistive.gap.error` | `{spacing.4}` | `dimension` | `--input-assistive-gap-error` |
| `input.assistive.iconSize` | `{spacing.20}` | `dimension` | `--input-assistive-icon-size` |
| `input.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--input-assistive-color-default` |
| `input.assistive.color.error` | `{color.text.danger.default}` | `color` | `--input-assistive-color-error` |
| `input.assistive.color.warning` | `{color.text.warning.default}` | `color` | `--input-assistive-color-warning` |
| `input.assistive.color.success` | `{color.text.positive.on-secondary}` | `color` | `--input-assistive-color-success` |
| `input.icon.size.md` | `{spacing.20}` | `dimension` | `--input-icon-size-md` |
| `input.icon.size.lg` | `{spacing.24}` | `dimension` | `--input-icon-size-lg` |
| `input.icon.color.default` | `{color.icon.base.secondary}` | `color` | `--input-icon-color-default` |
| `input.icon.color.loading` | `{color.icon.base.tertiary}` | `color` | `--input-icon-color-loading` |
| `input.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--input-icon-color-disabled` |
| `input.clearButton.size.md` | `{spacing.16}` | `dimension` | `--input-clear-button-size-md` |
| `input.clearButton.size.lg` | `{spacing.20}` | `dimension` | `--input-clear-button-size-lg` |
| `input.clearButton.borderRadius` | `{borderRadius.full}` | `dimension` | `--input-clear-button-border-radius` |
| `input.clearButton.color.default` | `{color.icon.base.secondary}` | `color` | `--input-clear-button-color-default` |
| `input.clearButton.color.hover` | `{color.icon.base.default}` | `color` | `--input-clear-button-color-hover` |
| `input.clearButton.background.hover` | `{color.background.base.tertiary}` | `color` | `--input-clear-button-background-hover` |
| `input.default.background.default` | `{color.background.base.default}` | `color` | `--input-default-background-default` |
| `input.default.background.readOnly` | `{color.background.base.secondary}` | `color` | `--input-default-background-read-only` |
| `input.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--input-default-background-disabled` |
| `input.default.border.default` | `{color.border.base.default}` | `color` | `--input-default-border-default` |
| `input.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--input-default-border-hover` |
| `input.default.border.focus` | `{color.border.brand.default}` | `color` | `--input-default-border-focus` |
| `input.default.border.readOnly` | `{color.border.base.default}` | `color` | `--input-default-border-read-only` |
| `input.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--input-default-border-disabled` |
| `input.default.text.default` | `{color.text.base.default}` | `color` | `--input-default-text-default` |
| `input.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--input-default-text-placeholder` |
| `input.default.text.readOnly` | `{color.text.base.default}` | `color` | `--input-default-text-read-only` |
| `input.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--input-default-text-disabled` |
| `input.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--input-default-focus-ring` |
| `input.warning.background.default` | `{color.background.base.default}` | `color` | `--input-warning-background-default` |
| `input.warning.border.default` | `{color.border.warning.default}` | `color` | `--input-warning-border-default` |
| `input.warning.border.hover` | `{color.border.warning.default}` | `color` | `--input-warning-border-hover` |
| `input.warning.border.focus` | `{color.border.warning.default}` | `color` | `--input-warning-border-focus` |
| `input.warning.focusRing` | `{palette.apricot.200}` | `color` | `--input-warning-focus-ring` |
| `input.destructive.background.default` | `{color.background.base.default}` | `color` | `--input-destructive-background-default` |
| `input.destructive.border.default` | `{color.border.danger.default}` | `color` | `--input-destructive-border-default` |
| `input.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--input-destructive-border-hover` |
| `input.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--input-destructive-border-focus` |
| `input.destructive.focusRing` | `{palette.red.200}` | `color` | `--input-destructive-focus-ring` |
| `input.success.background.default` | `{color.background.base.default}` | `color` | `--input-success-background-default` |
| `input.success.border.default` | `{color.border.positive.default}` | `color` | `--input-success-border-default` |
| `input.success.border.hover` | `{color.border.positive.default}` | `color` | `--input-success-border-hover` |
| `input.success.border.focus` | `{color.border.positive.default}` | `color` | `--input-success-border-focus` |
| `input.success.focusRing` | `{palette.green.200}` | `color` | `--input-success-focus-ring` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/input.tokens.json</code></summary>

```json
{
  "input": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "loadingOpacity": { "$value": "0.6", "$type": "number" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" },
        "warning": { "$value": "{color.text.warning.default}", "$type": "color" },
        "$comment": "Use `text.positive.on-secondary` (palette.green.700, ~5.2:1 on white) — `text.positive.default` (palette.green.600) is only 3.73:1 and fails WCAG 1.4.3 AA as body text.",
        "success": { "$value": "{color.text.positive.on-secondary}", "$type": "color" }
      }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "loading": { "$value": "{color.icon.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "clearButton": {
      "size": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.icon.base.default}", "$type": "color" }
      },
      "background": {
        "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" }
      }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "readOnly": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "readOnly": { "$value": "{color.border.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "readOnly": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "warning": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.warning.default}", "$type": "color" },
        "hover": { "$value": "{color.border.warning.default}", "$type": "color" },
        "focus": { "$value": "{color.border.warning.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.apricot.200}", "$type": "color" }
    },
    "destructive": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    },
    "success": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.positive.default}", "$type": "color" },
        "hover": { "$value": "{color.border.positive.default}", "$type": "color" },
        "focus": { "$value": "{color.border.positive.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.green.200}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/link.tokens.json`

_45 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `link.container.paddingInline` | `{spacing.0}` | `dimension` | `--link-container-padding-inline` |
| `link.container.paddingBlock` | `{spacing.0}` | `dimension` | `--link-container-padding-block` |
| `link.container.gap.xs` | `{spacing.2}` | `dimension` | `--link-container-gap-xs` |
| `link.container.gap.sm` | `{spacing.4}` | `dimension` | `--link-container-gap-sm` |
| `link.container.gap.md` | `{spacing.4}` | `dimension` | `--link-container-gap-md` |
| `link.container.gap.lg` | `{spacing.6}` | `dimension` | `--link-container-gap-lg` |
| `link.container.minHeightTouch` | `{spacing.40}` | `dimension` | `--link-container-min-height-touch` |
| `link.container.minHeightPointer` | `{spacing.32}` | `dimension` | `--link-container-min-height-pointer` |
| `link.container.borderRadius` | `{borderRadius.4}` | `dimension` | `--link-container-border-radius` |
| `link.container.transitionDuration` | `150ms` | `duration` | `--link-container-transition-duration` |
| `link.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--link-container-transition-timing-function` |
| `link.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--link-label-font-family` |
| `link.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--link-label-font-weight` |
| `link.label.fontSize.xs` | `{fontSize.12}` | `dimension` | `--link-label-font-size-xs` |
| `link.label.fontSize.sm` | `{fontSize.14}` | `dimension` | `--link-label-font-size-sm` |
| `link.label.fontSize.md` | `{fontSize.16}` | `dimension` | `--link-label-font-size-md` |
| `link.label.fontSize.lg` | `{fontSize.18}` | `dimension` | `--link-label-font-size-lg` |
| `link.label.lineHeight.xs` | `{lineHeight.16}` | `dimension` | `--link-label-line-height-xs` |
| `link.label.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--link-label-line-height-sm` |
| `link.label.lineHeight.md` | `{lineHeight.24}` | `dimension` | `--link-label-line-height-md` |
| `link.label.lineHeight.lg` | `{lineHeight.28}` | `dimension` | `--link-label-line-height-lg` |
| `link.label.underlineThickness` | `{borderWidth.1}` | `dimension` | `--link-label-underline-thickness` |
| `link.label.underlineThicknessHover` | `1.5px` | `dimension` | `--link-label-underline-thickness-hover` |
| `link.label.underlineOffset` | `{spacing.2}` | `dimension` | `--link-label-underline-offset` |
| `link.icon.size.xs` | `{spacing.12}` | `dimension` | `--link-icon-size-xs` |
| `link.icon.size.sm` | `{spacing.16}` | `dimension` | `--link-icon-size-sm` |
| `link.icon.size.md` | `{spacing.20}` | `dimension` | `--link-icon-size-md` |
| `link.icon.size.lg` | `{spacing.20}` | `dimension` | `--link-icon-size-lg` |
| `link.primary.label.default` | `{color.text.brand.default}` | `color` | `--link-primary-label-default` |
| `link.primary.label.hover` | `{color.text.brand.default-hover}` | `color` | `--link-primary-label-hover` |
| `link.primary.label.active` | `{color.background.brand.default-active}` | `color` | `--link-primary-label-active` |
| `link.primary.label.visited` | `{color.text.brand.visited}` | `color` | `--link-primary-label-visited` |
| `link.strict.label.default` | `{color.text.base.default}` | `color` | `--link-strict-label-default` |
| `link.strict.label.hover` | `{color.text.base.secondary}` | `color` | `--link-strict-label-hover` |
| `link.strict.label.active` | `{color.text.base.default}` | `color` | `--link-strict-label-active` |
| `link.strict.label.visited` | `{color.text.brand.visited}` | `color` | `--link-strict-label-visited` |
| `link.white.label.default` | `{color.text.base-inverse.default}` | `color` | `--link-white-label-default` |
| `link.white.label.hover` | `{color.text.base-inverse.on-color}` | `color` | `--link-white-label-hover` |
| `link.white.label.active` | `{color.text.base-inverse.default}` | `color` | `--link-white-label-active` |
| `link.white.label.visited` | `{color.text.brand.visited}` | `color` | `--link-white-label-visited` |
| `link.disabled.label` | `{color.text.disabled.default}` | `color` | `--link-disabled-label` |
| `link.focus.ringColor` | `{color.background.brand.focus-ring}` | `color` | `--link-focus-ring-color` |
| `link.focus.ringWidth` | `{borderWidth.3}` | `dimension` | `--link-focus-ring-width` |
| `link.focus.ringOffset` | `{borderWidth.1}` | `dimension` | `--link-focus-ring-offset` |
| `link.focus.ringOffsetColor` | `{color.background.base.default}` | `color` | `--link-focus-ring-offset-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/link.tokens.json</code></summary>

```json
{
  "link": {
    "container": {
      "paddingInline": {
        "$value": "{spacing.0}",
        "$type": "dimension"
      },
      "paddingBlock": {
        "$value": "{spacing.0}",
        "$type": "dimension"
      },
      "gap": {
        "xs": { "$value": "{spacing.2}", "$type": "dimension" },
        "sm": { "$value": "{spacing.4}", "$type": "dimension" },
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "lg": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "minHeightTouch": {
        "$value": "{spacing.40}",
        "$type": "dimension"
      },
      "minHeightPointer": {
        "$value": "{spacing.32}",
        "$type": "dimension"
      },
      "borderRadius": {
        "$value": "{borderRadius.4}",
        "$type": "dimension"
      },
      "transitionDuration": {
        "$value": "150ms",
        "$type": "duration"
      },
      "transitionTimingFunction": {
        "$value": "ease-out",
        "$type": "cubicBezier"
      }
    },
    "label": {
      "fontFamily": {
        "$value": "{fontFamily.primary}",
        "$type": "fontFamily"
      },
      "fontWeight": {
        "$value": "{fontWeight.regular}",
        "$type": "fontWeight"
      },
      "fontSize": {
        "xs": { "$value": "{fontSize.12}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" },
        "md": { "$value": "{fontSize.16}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.18}", "$type": "dimension" }
      },
      "lineHeight": {
        "xs": { "$value": "{lineHeight.16}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "md": { "$value": "{lineHeight.24}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.28}", "$type": "dimension" }
      },
      "underlineThickness": {
        "$value": "{borderWidth.1}",
        "$type": "dimension"
      },
      "underlineThicknessHover": {
        "$value": "1.5px",
        "$type": "dimension",
        "$comment": "Hover thickens the underline to 1.5px per Figma 661:7090 (no 1.5px borderWidth primitive exists)."
      },
      "underlineOffset": {
        "$value": "{spacing.2}",
        "$type": "dimension"
      }
    },
    "icon": {
      "size": {
        "xs": { "$value": "{spacing.12}", "$type": "dimension" },
        "sm": { "$value": "{spacing.16}", "$type": "dimension" },
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    },
    "primary": {
      "label": {
        "default": { "$value": "{color.text.brand.default}", "$type": "color" },
        "$comment": "hover matches Figma 661:7090 — text.brand.default-hover resolves to #0058d2 (same blue as default; hover affordance comes from the thickened underline). active uses background.brand.default-active because the text tier lacks an active step; Figma's link doc does not show an active state.",
        "hover": { "$value": "{color.text.brand.default-hover}", "$type": "color" },
        "active": { "$value": "{color.background.brand.default-active}", "$type": "color" },
        "visited": { "$value": "{color.text.brand.visited}", "$type": "color" }
      }
    },
    "strict": {
      "label": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "hover": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "active": { "$value": "{color.text.base.default}", "$type": "color" },
        "visited": { "$value": "{color.text.brand.visited}", "$type": "color" }
      }
    },
    "white": {
      "label": {
        "default": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
        "hover": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
        "active": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
        "visited": { "$value": "{color.text.brand.visited}", "$type": "color" }
      }
    },
    "disabled": {
      "label": {
        "$value": "{color.text.disabled.default}",
        "$type": "color"
      }
    },
    "focus": {
      "ringColor": {
        "$value": "{color.background.brand.focus-ring}",
        "$type": "color"
      },
      "ringWidth": {
        "$value": "{borderWidth.3}",
        "$type": "dimension"
      },
      "ringOffset": {
        "$value": "{borderWidth.1}",
        "$type": "dimension"
      },
      "ringOffsetColor": {
        "$value": "{color.background.base.default}",
        "$type": "color"
      }
    }
  }
}
```

</details>

#### `tokens/core/components/menu.tokens.json`

_43 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `menu.panel.background` | `{color.background.base.default}` | `color` | `--menu-panel-background` |
| `menu.panel.borderRadius` | `{borderRadius.16}` | `dimension` | `--menu-panel-border-radius` |
| `menu.panel.borderWidth` | `0px` | `dimension` | `--menu-panel-border-width` |
| `menu.panel.borderColor` | `{color.border.base.default}` | `color` | `--menu-panel-border-color` |
| `menu.panel.padding` | `{spacing.8}` | `dimension` | `--menu-panel-padding` |
| `menu.panel.gap` | `{spacing.4}` | `dimension` | `--menu-panel-gap` |
| `menu.panel.minInlineSize` | `270px` | `dimension` | `--menu-panel-min-inline-size` |
| `menu.panel.maxBlockSize` | `320px` | `dimension` | `--menu-panel-max-block-size` |
| `menu.panel.shadow` | `{dropShadow.300}` | `string` | `--menu-panel-shadow` |
| `menu.item.minBlockSize` | `{spacing.40}` | `dimension` | `--menu-item-min-block-size` |
| `menu.item.paddingBlock` | `{spacing.12}` | `dimension` | `--menu-item-padding-block` |
| `menu.item.paddingInlineStart` | `{spacing.16}` | `dimension` | `--menu-item-padding-inline-start` |
| `menu.item.paddingInlineEnd` | `{spacing.24}` | `dimension` | `--menu-item-padding-inline-end` |
| `menu.item.paddingInlineEndTrailing` | `{spacing.16}` | `dimension` | `--menu-item-padding-inline-end-trailing` |
| `menu.item.borderRadius` | `{borderRadius.8}` | `dimension` | `--menu-item-border-radius` |
| `menu.item.gap` | `{spacing.12}` | `dimension` | `--menu-item-gap` |
| `menu.item.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--menu-item-font-family` |
| `menu.item.fontSize` | `{fontSize.14}` | `dimension` | `--menu-item-font-size` |
| `menu.item.lineHeight` | `{lineHeight.20}` | `dimension` | `--menu-item-line-height` |
| `menu.item.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--menu-item-font-weight` |
| `menu.item.background.default` | `{color.background.base.default}` | `color` | `--menu-item-background-default` |
| `menu.item.background.hover` | `{color.background.base.default-hover}` | `color` | `--menu-item-background-hover` |
| `menu.item.background.active` | `{color.background.base.default-active}` | `color` | `--menu-item-background-active` |
| `menu.item.background.selected` | `{color.background.base.secondary}` | `color` | `--menu-item-background-selected` |
| `menu.item.text.default` | `{color.text.base.secondary}` | `color` | `--menu-item-text-default` |
| `menu.item.text.selected` | `{color.text.brand.default}` | `color` | `--menu-item-text-selected` |
| `menu.item.text.disabled` | `{color.text.disabled.default}` | `color` | `--menu-item-text-disabled` |
| `menu.item.icon.size` | `{spacing.20}` | `dimension` | `--menu-item-icon-size` |
| `menu.item.icon.color.default` | `{color.icon.base.default}` | `color` | `--menu-item-icon-color-default` |
| `menu.item.icon.color.disabled` | `{color.icon.disabled.default}` | `color` | `--menu-item-icon-color-disabled` |
| `menu.item.check.size` | `{spacing.20}` | `dimension` | `--menu-item-check-size` |
| `menu.item.check.color` | `{color.icon.brand.default}` | `color` | `--menu-item-check-color` |
| `menu.heading.paddingInline` | `{spacing.16}` | `dimension` | `--menu-heading-padding-inline` |
| `menu.heading.paddingBlock` | `{spacing.4}` | `dimension` | `--menu-heading-padding-block` |
| `menu.heading.gap` | `{spacing.4}` | `dimension` | `--menu-heading-gap` |
| `menu.heading.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--menu-heading-font-family` |
| `menu.heading.fontSize` | `{fontSize.14}` | `dimension` | `--menu-heading-font-size` |
| `menu.heading.lineHeight` | `{lineHeight.20}` | `dimension` | `--menu-heading-line-height` |
| `menu.heading.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--menu-heading-font-weight` |
| `menu.heading.color` | `{color.text.base.tertiary}` | `color` | `--menu-heading-color` |
| `menu.heading.separatorColor` | `{color.border.base.default}` | `color` | `--menu-heading-separator-color` |
| `menu.focusRing.color` | `{palette.blue-sky.500}` | `color` | `--menu-focus-ring-color` |
| `menu.focusRing.width` | `{borderWidth.2}` | `dimension` | `--menu-focus-ring-width` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/menu.tokens.json</code></summary>

```json
{
  "menu": {
    "panel": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": {
        "$value": "{borderRadius.16}",
        "$type": "dimension",
        "$comment": "Figma selection-menu 172:3093 / contextual-menu 409:22462 use border-radius/16."
      },
      "borderWidth": {
        "$value": "0px",
        "$type": "dimension",
        "$comment": "Figma menu has no border — depth comes from dropShadow.300 only."
      },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "padding": {
        "$value": "{spacing.8}",
        "$type": "dimension",
        "$comment": "Figma menu uses spacing/8 around the item list."
      },
      "gap": {
        "$value": "{spacing.4}",
        "$type": "dimension",
        "$comment": "Figma menu uses spacing/4 between items."
      },
      "minInlineSize": {
        "$value": "270px",
        "$type": "dimension",
        "$comment": "Figma menu fixed width 270px — used as the floor so the panel can grow with content."
      },
      "maxBlockSize": {
        "$value": "320px",
        "$type": "dimension",
        "$comment": "Scroll threshold; mirrors select-input listbox.maxBlockSize and the Figma scrollable-content example."
      },
      "shadow": { "$value": "{dropShadow.300}", "$type": "string" }
    },
    "item": {
      "minBlockSize": {
        "$value": "{spacing.40}",
        "$type": "dimension",
        "$comment": "Figma menu-item: 12px paddingBlock + 20px lh = 44px rendered; 40px floor on the clickable target."
      },
      "paddingBlock": { "$value": "{spacing.12}", "$type": "dimension" },
      "paddingInlineStart": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingInlineEnd": {
        "$value": "{spacing.24}",
        "$type": "dimension",
        "$comment": "Figma default item pr-24; collapses to spacing.16 when a trailing checkmark is present."
      },
      "paddingInlineEndTrailing": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma selected/selection item with trailing checkmark uses px-16."
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "gap": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma menu-item puts spacing/12 between leading/label/trailing."
      },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": {
        "$value": "{fontWeight.medium}",
        "$type": "fontWeight",
        "$comment": "Figma menu-item Default + Selected both render Onest Medium (500)."
      },
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "hover": {
          "$value": "{color.background.base.default-hover}",
          "$type": "color",
          "$comment": "Figma menu-item Hover bg #f5f5f5."
        },
        "active": {
          "$value": "{color.background.base.default-active}",
          "$type": "color",
          "$comment": "Figma menu-item Active bg #d9d9d9."
        },
        "selected": {
          "$value": "{color.background.base.secondary}",
          "$type": "color",
          "$comment": "Figma selection-menu Selected item bg #f5f5f5."
        }
      },
      "text": {
        "default": {
          "$value": "{color.text.base.secondary}",
          "$type": "color",
          "$comment": "Figma menu-item Default text #383838."
        },
        "selected": {
          "$value": "{color.text.brand.default}",
          "$type": "color",
          "$comment": "Figma selection-menu Selected text #0058d2."
        },
        "disabled": {
          "$value": "{color.text.disabled.default}",
          "$type": "color",
          "$comment": "Figma menu-item Disabled text #b2b2b2. Disabled text is exempt from WCAG 1.4.3 contrast (SC applies to enabled UI only)."
        }
      },
      "icon": {
        "size": {
          "$value": "{spacing.20}",
          "$type": "dimension",
          "$comment": "Leading icon glyph sized to 20px within the 24px Figma frame."
        },
        "color": {
          "default": {
            "$value": "{color.icon.base.default}",
            "$type": "color",
            "$comment": "Figma leading icon #121212."
          },
          "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
        }
      },
      "check": {
        "size": {
          "$value": "{spacing.20}",
          "$type": "dimension",
          "$comment": "Trailing 24/checkmark-small glyph for a selected selection-item; verify 20 vs 24 in pixel QA."
        },
        "color": { "$value": "{color.icon.brand.default}", "$type": "color" }
      }
    },
    "heading": {
      "paddingInline": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.4}", "$type": "dimension" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "color": {
        "$value": "{color.text.base.tertiary}",
        "$type": "color",
        "$comment": "Figma Section Heading label #757575."
      },
      "separatorColor": {
        "$value": "{color.border.base.default}",
        "$type": "color",
        "$comment": "Figma Section Heading separator (4px tall divider above the label)."
      }
    },
    "focusRing": {
      "color": {
        "$value": "{palette.blue-sky.500}",
        "$type": "color",
        "$comment": "Figma 'Focus Ring/Small: Inner' = inner shadow blue-sky/500 (#3379db), spread 2. Reconcile vs focusRing.tokens.json in CSS."
      },
      "width": {
        "$value": "{borderWidth.2}",
        "$type": "dimension",
        "$comment": "Figma focus ring spread = 2px."
      }
    }
  }
}
```

</details>

#### `tokens/core/components/modal.tokens.json`

_59 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `modal.backdrop.background` | `{color.background.alpha.overlay-dark}` | `color` | `--modal-backdrop-background` |
| `modal.backdrop.blur` | `0px` | `dimension` | `--modal-backdrop-blur` |
| `modal.backdrop.transitionDuration` | `150ms` | `duration` | `--modal-backdrop-transition-duration` |
| `modal.backdrop.transitionTimingFunction` | `cubic-bezier(0, 0, 0.2, 1)` | `cubicBezier` | `--modal-backdrop-transition-timing-function` |
| `modal.container.background` | `{color.background.base.default}` | `color` | `--modal-container-background` |
| `modal.container.borderRadius` | `{borderRadius.16}` | `dimension` | `--modal-container-border-radius` |
| `modal.container.boxShadow` | `{dropShadow.500}` | `string` | `--modal-container-box-shadow` |
| `modal.container.minWidth` | `280px` | `dimension` | `--modal-container-min-width` |
| `modal.container.maxWidthSm` | `480px` | `dimension` | `--modal-container-max-width-sm` |
| `modal.container.maxWidthMd` | `588px` | `dimension` | `--modal-container-max-width-md` |
| `modal.container.maxWidthLg` | `720px` | `dimension` | `--modal-container-max-width-lg` |
| `modal.container.maxHeight` | `calc(100dvh - 64px)` | `dimension` | `--modal-container-max-height` |
| `modal.container.maxHeightMobile` | `calc(100dvh - 32px)` | `dimension` | `--modal-container-max-height-mobile` |
| `modal.container.marginInline` | `{spacing.16}` | `dimension` | `--modal-container-margin-inline` |
| `modal.container.transitionDuration` | `200ms` | `duration` | `--modal-container-transition-duration` |
| `modal.container.transitionTimingFunction` | `cubic-bezier(0, 0, 0.2, 1)` | `cubicBezier` | `--modal-container-transition-timing-function` |
| `modal.header.paddingBlockStart` | `{spacing.24}` | `dimension` | `--modal-header-padding-block-start` |
| `modal.header.paddingInlineStart` | `{spacing.32}` | `dimension` | `--modal-header-padding-inline-start` |
| `modal.header.paddingInlineEnd` | `{spacing.20}` | `dimension` | `--modal-header-padding-inline-end` |
| `modal.header.gap` | `{spacing.12}` | `dimension` | `--modal-header-gap` |
| `modal.header.imageHeight` | `280px` | `dimension` | `--modal-header-image-height` |
| `modal.header.imageHeightSm` | `180px` | `dimension` | `--modal-header-image-height-sm` |
| `modal.title.color` | `{color.text.base.default}` | `color` | `--modal-title-color` |
| `modal.title.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--modal-title-font-family` |
| `modal.title.fontSizeMd` | `{fontSize.24}` | `dimension` | `--modal-title-font-size-md` |
| `modal.title.fontSizeSm` | `{fontSize.20}` | `dimension` | `--modal-title-font-size-sm` |
| `modal.title.lineHeightMd` | `{lineHeight.32}` | `dimension` | `--modal-title-line-height-md` |
| `modal.title.lineHeightSm` | `{lineHeight.28}` | `dimension` | `--modal-title-line-height-sm` |
| `modal.title.fontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--modal-title-font-weight` |
| `modal.title.letterSpacingMd` | `-0.24px` | `dimension` | `--modal-title-letter-spacing-md` |
| `modal.title.letterSpacingSm` | `-0.2px` | `dimension` | `--modal-title-letter-spacing-sm` |
| `modal.body.color` | `{color.text.base.secondary}` | `color` | `--modal-body-color` |
| `modal.body.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--modal-body-font-family` |
| `modal.body.fontSizeMd` | `{fontSize.16}` | `dimension` | `--modal-body-font-size-md` |
| `modal.body.fontSizeSm` | `{fontSize.14}` | `dimension` | `--modal-body-font-size-sm` |
| `modal.body.lineHeightMd` | `{lineHeight.24}` | `dimension` | `--modal-body-line-height-md` |
| `modal.body.lineHeightSm` | `{lineHeight.20}` | `dimension` | `--modal-body-line-height-sm` |
| `modal.body.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--modal-body-font-weight` |
| `modal.body.paddingBlockStartMd` | `{spacing.24}` | `dimension` | `--modal-body-padding-block-start-md` |
| `modal.body.paddingBlockStartSm` | `{spacing.16}` | `dimension` | `--modal-body-padding-block-start-sm` |
| `modal.body.paddingBlockEndMd` | `{spacing.40}` | `dimension` | `--modal-body-padding-block-end-md` |
| `modal.body.paddingBlockEndSm` | `{spacing.20}` | `dimension` | `--modal-body-padding-block-end-sm` |
| `modal.body.paddingInline` | `{spacing.32}` | `dimension` | `--modal-body-padding-inline` |
| `modal.body.gap` | `{spacing.16}` | `dimension` | `--modal-body-gap` |
| `modal.icon.size` | `{spacing.48}` | `dimension` | `--modal-icon-size` |
| `modal.icon.color` | `{color.icon.base.default}` | `color` | `--modal-icon-color` |
| `modal.footer.paddingBlockStart` | `{spacing.12}` | `dimension` | `--modal-footer-padding-block-start` |
| `modal.footer.paddingBlockEndMd` | `{spacing.24}` | `dimension` | `--modal-footer-padding-block-end-md` |
| `modal.footer.paddingBlockEndSm` | `{spacing.20}` | `dimension` | `--modal-footer-padding-block-end-sm` |
| `modal.footer.paddingInline` | `{spacing.20}` | `dimension` | `--modal-footer-padding-inline` |
| `modal.footer.gap` | `{spacing.8}` | `dimension` | `--modal-footer-gap` |
| `modal.close.size` | `{spacing.32}` | `dimension` | `--modal-close-size` |
| `modal.close.iconSize` | `{spacing.16}` | `dimension` | `--modal-close-icon-size` |
| `modal.close.padding` | `{spacing.8}` | `dimension` | `--modal-close-padding` |
| `modal.close.background` | `{color.background.base.tertiary}` | `color` | `--modal-close-background` |
| `modal.close.backgroundHover` | `{color.background.base.tertiary-hover}` | `color` | `--modal-close-background-hover` |
| `modal.close.backgroundOnImage` | `{color.background.base.tertiary}` | `color` | `--modal-close-background-on-image` |
| `modal.close.iconColor` | `{color.icon.base.secondary}` | `color` | `--modal-close-icon-color` |
| `modal.close.borderRadius` | `{borderRadius.full}` | `dimension` | `--modal-close-border-radius` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/modal.tokens.json</code></summary>

```json
{
  "modal": {
    "backdrop": {
      "background": { "$value": "{color.background.alpha.overlay-dark}", "$type": "color" },
      "blur": { "$value": "0px", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "cubic-bezier(0, 0, 0.2, 1)", "$type": "cubicBezier" }
    },
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.16}", "$type": "dimension" },
      "boxShadow": { "$value": "{dropShadow.500}", "$type": "string" },
      "minWidth": { "$value": "280px", "$type": "dimension" },
      "maxWidthSm": { "$value": "480px", "$type": "dimension" },
      "maxWidthMd": { "$value": "588px", "$type": "dimension" },
      "maxWidthLg": { "$value": "720px", "$type": "dimension" },
      "maxHeight": { "$value": "calc(100dvh - 64px)", "$type": "dimension" },
      "maxHeightMobile": { "$value": "calc(100dvh - 32px)", "$type": "dimension" },
      "marginInline": { "$value": "{spacing.16}", "$type": "dimension" },
      "transitionDuration": { "$value": "200ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "cubic-bezier(0, 0, 0.2, 1)", "$type": "cubicBezier" }
    },
    "header": {
      "paddingBlockStart": { "$value": "{spacing.24}", "$type": "dimension" },
      "paddingInlineStart": { "$value": "{spacing.32}", "$type": "dimension" },
      "paddingInlineEnd": { "$value": "{spacing.20}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "imageHeight": { "$value": "280px", "$type": "dimension" },
      "imageHeightSm": { "$value": "180px", "$type": "dimension" }
    },
    "title": {
      "color": { "$value": "{color.text.base.default}", "$type": "color" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSizeMd": { "$value": "{fontSize.24}", "$type": "dimension" },
      "fontSizeSm": { "$value": "{fontSize.20}", "$type": "dimension" },
      "lineHeightMd": { "$value": "{lineHeight.32}", "$type": "dimension" },
      "lineHeightSm": { "$value": "{lineHeight.28}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "letterSpacingMd": { "$value": "-0.24px", "$type": "dimension" },
      "letterSpacingSm": { "$value": "-0.2px", "$type": "dimension" }
    },
    "body": {
      "color": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSizeMd": { "$value": "{fontSize.16}", "$type": "dimension" },
      "fontSizeSm": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeightMd": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "lineHeightSm": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "paddingBlockStartMd": { "$value": "{spacing.24}", "$type": "dimension" },
      "paddingBlockStartSm": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlockEndMd": { "$value": "{spacing.40}", "$type": "dimension" },
      "paddingBlockEndSm": { "$value": "{spacing.20}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.32}", "$type": "dimension" },
      "gap": { "$value": "{spacing.16}", "$type": "dimension" }
    },
    "icon": {
      "size": { "$value": "{spacing.48}", "$type": "dimension" },
      "color": { "$value": "{color.icon.base.default}", "$type": "color" }
    },
    "footer": {
      "paddingBlockStart": { "$value": "{spacing.12}", "$type": "dimension" },
      "paddingBlockEndMd": { "$value": "{spacing.24}", "$type": "dimension" },
      "paddingBlockEndSm": { "$value": "{spacing.20}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.20}", "$type": "dimension" },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "close": {
      "size": { "$value": "{spacing.32}", "$type": "dimension" },
      "iconSize": { "$value": "{spacing.16}", "$type": "dimension" },
      "padding": { "$value": "{spacing.8}", "$type": "dimension" },
      "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "backgroundHover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
      "backgroundOnImage": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "iconColor": { "$value": "{color.icon.base.secondary}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/notification.tokens.json`

_45 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `notification.container.width` | `350px` | `dimension` | `--notification-container-width` |
| `notification.container.paddingTop` | `{spacing.12}` | `dimension` | `--notification-container-padding-top` |
| `notification.container.paddingBottom` | `{spacing.16}` | `dimension` | `--notification-container-padding-bottom` |
| `notification.container.paddingInline` | `{spacing.12}` | `dimension` | `--notification-container-padding-inline` |
| `notification.container.gap` | `{spacing.16}` | `dimension` | `--notification-container-gap` |
| `notification.container.contentGap` | `{spacing.8}` | `dimension` | `--notification-container-content-gap` |
| `notification.container.stackGap` | `{spacing.8}` | `dimension` | `--notification-container-stack-gap` |
| `notification.container.headingGap` | `{spacing.6}` | `dimension` | `--notification-container-heading-gap` |
| `notification.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--notification-container-border-radius` |
| `notification.container.transitionDuration` | `150ms` | `duration` | `--notification-container-transition-duration` |
| `notification.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--notification-container-transition-timing-function` |
| `notification.container.appearanceDuration` | `250ms` | `duration` | `--notification-container-appearance-duration` |
| `notification.icon.size` | `{spacing.24}` | `dimension` | `--notification-icon-size` |
| `notification.icon.padding` | `{spacing.2}` | `dimension` | `--notification-icon-padding` |
| `notification.title.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--notification-title-font-family` |
| `notification.title.fontSize` | `{fontSize.18}` | `dimension` | `--notification-title-font-size` |
| `notification.title.lineHeight` | `{lineHeight.26}` | `dimension` | `--notification-title-line-height` |
| `notification.title.fontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--notification-title-font-weight` |
| `notification.title.letterSpacing` | `-0.18px` | `dimension` | `--notification-title-letter-spacing` |
| `notification.body.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--notification-body-font-family` |
| `notification.body.fontSize` | `{fontSize.14}` | `dimension` | `--notification-body-font-size` |
| `notification.body.lineHeight` | `{lineHeight.20}` | `dimension` | `--notification-body-line-height` |
| `notification.body.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--notification-body-font-weight` |
| `notification.close.sizePointer` | `{spacing.32}` | `dimension` | `--notification-close-size-pointer` |
| `notification.close.sizeTouch` | `{spacing.40}` | `dimension` | `--notification-close-size-touch` |
| `notification.close.iconSize` | `{spacing.16}` | `dimension` | `--notification-close-icon-size` |
| `notification.close.borderRadius` | `{borderRadius.4}` | `dimension` | `--notification-close-border-radius` |
| `notification.close.backgroundHover` | `{color.background.base.tertiary-hover}` | `color` | `--notification-close-background-hover` |
| `notification.close.backgroundHoverOnColor` | `{color.background.alpha.overlay-dark}` | `color` | `--notification-close-background-hover-on-color` |
| `notification.info.background` | `{color.background.brand.default}` | `color` | `--notification-info-background` |
| `notification.info.text` | `{color.text.base-inverse.on-color}` | `color` | `--notification-info-text` |
| `notification.info.icon` | `{color.icon.base-inverse.on-color}` | `color` | `--notification-info-icon` |
| `notification.info.closeIcon` | `{color.icon.base-inverse.on-color}` | `color` | `--notification-info-close-icon` |
| `notification.warning.background` | `{color.background.warning.accent}` | `color` | `--notification-warning-background` |
| `notification.warning.text` | `{color.text.base.default-on-color}` | `color` | `--notification-warning-text` |
| `notification.warning.icon` | `{color.icon.warning.on-secondary}` | `color` | `--notification-warning-icon` |
| `notification.warning.closeIcon` | `{color.icon.base.secondary-on-color}` | `color` | `--notification-warning-close-icon` |
| `notification.success.background` | `{color.background.positive.default-hover}` | `color` | `--notification-success-background` |
| `notification.success.text` | `{color.text.base-inverse.on-color}` | `color` | `--notification-success-text` |
| `notification.success.icon` | `{color.icon.base-inverse.on-color}` | `color` | `--notification-success-icon` |
| `notification.success.closeIcon` | `{color.icon.base-inverse.on-color}` | `color` | `--notification-success-close-icon` |
| `notification.error.background` | `{color.background.danger.default}` | `color` | `--notification-error-background` |
| `notification.error.text` | `{color.text.base-inverse.on-color}` | `color` | `--notification-error-text` |
| `notification.error.icon` | `{color.icon.base-inverse.on-color}` | `color` | `--notification-error-icon` |
| `notification.error.closeIcon` | `{color.icon.base-inverse.on-color}` | `color` | `--notification-error-close-icon` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/notification.tokens.json</code></summary>

```json
{
  "notification": {
    "container": {
      "width": { "$value": "350px", "$type": "dimension" },
      "paddingTop": { "$value": "{spacing.12}", "$type": "dimension" },
      "paddingBottom": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.12}", "$type": "dimension" },
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "contentGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "stackGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "headingGap": { "$value": "{spacing.6}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" },
      "appearanceDuration": {
        "$value": "250ms",
        "$type": "duration",
        "$comment": "Entrance animation (slide-down + fade-in) per the Figma Behavior panel; slightly longer than the 150ms hover transition so the motion reads."
      }
    },
    "icon": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" },
      "padding": { "$value": "{spacing.2}", "$type": "dimension" }
    },
    "title": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.18}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.26}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "letterSpacing": { "$value": "-0.18px", "$type": "dimension" }
    },
    "body": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "close": {
      "sizePointer": {
        "$value": "{spacing.32}",
        "$type": "dimension",
        "$comment": "Interactive target per the Figma Target Sizes panel: 32px on pointer devices."
      },
      "sizeTouch": {
        "$value": "{spacing.40}",
        "$type": "dimension",
        "$comment": "40px target under `pointer: coarse` for touch ease-of-use."
      },
      "iconSize": { "$value": "{spacing.16}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "backgroundHover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
      "backgroundHoverOnColor": {
        "$value": "{color.background.alpha.overlay-dark}",
        "$type": "color",
        "$comment": "Translucent dark veil on the filled info/success/error surfaces: darkening (not lightening) on hover keeps the white × crisp (~7-10:1). A light fill drops it to ~1.1:1; a white veil only reaches ~2.7:1 — both below the 3:1 non-text-contrast floor."
      }
    },
    "info": {
      "background": { "$value": "{color.background.brand.default}", "$type": "color" },
      "text": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
      "icon": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
      "closeIcon": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" }
    },
    "warning": {
      "background": { "$value": "{color.background.warning.accent}", "$type": "color" },
      "text": { "$value": "{color.text.base.default-on-color}", "$type": "color" },
      "icon": { "$value": "{color.icon.warning.on-secondary}", "$type": "color" },
      "closeIcon": { "$value": "{color.icon.base.secondary-on-color}", "$type": "color" }
    },
    "success": {
      "background": {
        "$value": "{color.background.positive.default-hover}",
        "$type": "color",
        "$comment": "Darker green (#027948) instead of positive.default (#039855): white on-color text yields 3.73:1 against the lighter green (fails WCAG AA at the 14px body), but 5.48:1 against this rung. Figma's filled-success uses the lighter green; we darken it to keep the on-color text AA-compliant."
      },
      "text": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
      "icon": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
      "closeIcon": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" }
    },
    "error": {
      "background": { "$value": "{color.background.danger.default}", "$type": "color" },
      "text": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
      "icon": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
      "closeIcon": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/numeric-input.tokens.json`

_103 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `numericInput.field.gap` | `{spacing.8}` | `dimension` | `--numeric-input-field-gap` |
| `numericInput.container.height.md` | `{spacing.40}` | `dimension` | `--numeric-input-container-height-md` |
| `numericInput.container.height.lg` | `{spacing.48}` | `dimension` | `--numeric-input-container-height-lg` |
| `numericInput.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--numeric-input-container-padding-inline-md` |
| `numericInput.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--numeric-input-container-padding-inline-lg` |
| `numericInput.container.paddingInlineEndWithStepper.md` | `{spacing.4}` | `dimension` | `--numeric-input-container-padding-inline-end-with-stepper-md` |
| `numericInput.container.paddingInlineEndWithStepper.lg` | `{spacing.4}` | `dimension` | `--numeric-input-container-padding-inline-end-with-stepper-lg` |
| `numericInput.container.gap.md` | `{spacing.8}` | `dimension` | `--numeric-input-container-gap-md` |
| `numericInput.container.gap.lg` | `{spacing.8}` | `dimension` | `--numeric-input-container-gap-lg` |
| `numericInput.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--numeric-input-container-border-radius` |
| `numericInput.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--numeric-input-container-border-width-default` |
| `numericInput.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--numeric-input-container-border-width-emphasized` |
| `numericInput.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--numeric-input-container-focus-ring-width` |
| `numericInput.container.transitionDuration` | `150ms` | `duration` | `--numeric-input-container-transition-duration` |
| `numericInput.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--numeric-input-container-transition-timing-function` |
| `numericInput.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--numeric-input-control-font-family` |
| `numericInput.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--numeric-input-control-font-weight` |
| `numericInput.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--numeric-input-control-font-size-md` |
| `numericInput.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--numeric-input-control-font-size-lg` |
| `numericInput.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--numeric-input-control-line-height-md` |
| `numericInput.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--numeric-input-control-line-height-lg` |
| `numericInput.control.textAlign` | `start` | `string` | `--numeric-input-control-text-align` |
| `numericInput.control.loadingOpacity` | `0.6` | `number` | `--numeric-input-control-loading-opacity` |
| `numericInput.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--numeric-input-label-font-family` |
| `numericInput.label.fontSize` | `{fontSize.14}` | `dimension` | `--numeric-input-label-font-size` |
| `numericInput.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--numeric-input-label-line-height` |
| `numericInput.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--numeric-input-label-font-weight` |
| `numericInput.label.gap` | `{spacing.4}` | `dimension` | `--numeric-input-label-gap` |
| `numericInput.label.color.default` | `{color.text.base.secondary}` | `color` | `--numeric-input-label-color-default` |
| `numericInput.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--numeric-input-label-color-disabled` |
| `numericInput.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--numeric-input-label-required-mark-size` |
| `numericInput.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--numeric-input-label-required-mark-color` |
| `numericInput.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--numeric-input-assistive-font-family` |
| `numericInput.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--numeric-input-assistive-font-size` |
| `numericInput.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--numeric-input-assistive-line-height` |
| `numericInput.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--numeric-input-assistive-font-weight` |
| `numericInput.assistive.gap.default` | `{spacing.6}` | `dimension` | `--numeric-input-assistive-gap-default` |
| `numericInput.assistive.gap.error` | `{spacing.4}` | `dimension` | `--numeric-input-assistive-gap-error` |
| `numericInput.assistive.iconSize` | `{spacing.20}` | `dimension` | `--numeric-input-assistive-icon-size` |
| `numericInput.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--numeric-input-assistive-color-default` |
| `numericInput.assistive.color.error` | `{color.text.danger.default}` | `color` | `--numeric-input-assistive-color-error` |
| `numericInput.assistive.color.success` | `{color.text.positive.default}` | `color` | `--numeric-input-assistive-color-success` |
| `numericInput.icon.size.md` | `{spacing.20}` | `dimension` | `--numeric-input-icon-size-md` |
| `numericInput.icon.size.lg` | `{spacing.24}` | `dimension` | `--numeric-input-icon-size-lg` |
| `numericInput.icon.color.default` | `{color.icon.base.secondary}` | `color` | `--numeric-input-icon-color-default` |
| `numericInput.icon.color.loading` | `{color.icon.brand.default}` | `color` | `--numeric-input-icon-color-loading` |
| `numericInput.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--numeric-input-icon-color-disabled` |
| `numericInput.suffix.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--numeric-input-suffix-font-family` |
| `numericInput.suffix.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--numeric-input-suffix-font-weight` |
| `numericInput.suffix.fontSize.md` | `{fontSize.14}` | `dimension` | `--numeric-input-suffix-font-size-md` |
| `numericInput.suffix.fontSize.lg` | `{fontSize.16}` | `dimension` | `--numeric-input-suffix-font-size-lg` |
| `numericInput.suffix.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--numeric-input-suffix-line-height-md` |
| `numericInput.suffix.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--numeric-input-suffix-line-height-lg` |
| `numericInput.suffix.color.default` | `{color.text.base.tertiary}` | `color` | `--numeric-input-suffix-color-default` |
| `numericInput.suffix.color.readOnly` | `{color.text.base.secondary}` | `color` | `--numeric-input-suffix-color-read-only` |
| `numericInput.suffix.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--numeric-input-suffix-color-disabled` |
| `numericInput.suffix.marginInlineStart` | `{spacing.4}` | `dimension` | `--numeric-input-suffix-margin-inline-start` |
| `numericInput.stepper.width.md` | `{spacing.24}` | `dimension` | `--numeric-input-stepper-width-md` |
| `numericInput.stepper.width.lg` | `{spacing.32}` | `dimension` | `--numeric-input-stepper-width-lg` |
| `numericInput.stepper.gap` | `{spacing.4}` | `dimension` | `--numeric-input-stepper-gap` |
| `numericInput.stepper.borderRadius` | `{borderRadius.4}` | `dimension` | `--numeric-input-stepper-border-radius` |
| `numericInput.stepper.borderWidth` | `{borderWidth.1}` | `dimension` | `--numeric-input-stepper-border-width` |
| `numericInput.stepper.borderColor` | `{color.border.base.default}` | `color` | `--numeric-input-stepper-border-color` |
| `numericInput.stepper.background.default` | `{color.background.base.default}` | `color` | `--numeric-input-stepper-background-default` |
| `numericInput.stepper.background.hover` | `{color.background.base.secondary}` | `color` | `--numeric-input-stepper-background-hover` |
| `numericInput.stepper.background.active` | `{color.background.base.tertiary}` | `color` | `--numeric-input-stepper-background-active` |
| `numericInput.stepper.background.disabled` | `{color.background.disabled.default}` | `color` | `--numeric-input-stepper-background-disabled` |
| `numericInput.stepper.icon.size.md` | `{spacing.16}` | `dimension` | `--numeric-input-stepper-icon-size-md` |
| `numericInput.stepper.icon.size.lg` | `{spacing.20}` | `dimension` | `--numeric-input-stepper-icon-size-lg` |
| `numericInput.stepper.icon.color.default` | `{color.icon.base.secondary}` | `color` | `--numeric-input-stepper-icon-color-default` |
| `numericInput.stepper.icon.color.hover` | `{color.icon.base.default}` | `color` | `--numeric-input-stepper-icon-color-hover` |
| `numericInput.stepper.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--numeric-input-stepper-icon-color-disabled` |
| `numericInput.stepper.divider.color` | `{color.border.base.default}` | `color` | `--numeric-input-stepper-divider-color` |
| `numericInput.stepper.divider.width` | `{borderWidth.1}` | `dimension` | `--numeric-input-stepper-divider-width` |
| `numericInput.stepper.transitionDuration` | `150ms` | `duration` | `--numeric-input-stepper-transition-duration` |
| `numericInput.stepper.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--numeric-input-stepper-transition-timing-function` |
| `numericInput.loadingSpinner.size.md` | `{spacing.16}` | `dimension` | `--numeric-input-loading-spinner-size-md` |
| `numericInput.loadingSpinner.size.lg` | `{spacing.20}` | `dimension` | `--numeric-input-loading-spinner-size-lg` |
| `numericInput.default.background.default` | `{color.background.base.default}` | `color` | `--numeric-input-default-background-default` |
| `numericInput.default.background.readOnly` | `{color.background.base.secondary}` | `color` | `--numeric-input-default-background-read-only` |
| `numericInput.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--numeric-input-default-background-disabled` |
| `numericInput.default.border.default` | `{color.border.base.default}` | `color` | `--numeric-input-default-border-default` |
| `numericInput.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--numeric-input-default-border-hover` |
| `numericInput.default.border.focus` | `{color.border.brand.default}` | `color` | `--numeric-input-default-border-focus` |
| `numericInput.default.border.readOnly` | `{color.border.base.default}` | `color` | `--numeric-input-default-border-read-only` |
| `numericInput.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--numeric-input-default-border-disabled` |
| `numericInput.default.text.default` | `{color.text.base.default}` | `color` | `--numeric-input-default-text-default` |
| `numericInput.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--numeric-input-default-text-placeholder` |
| `numericInput.default.text.readOnly` | `{color.text.base.default}` | `color` | `--numeric-input-default-text-read-only` |
| `numericInput.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--numeric-input-default-text-disabled` |
| `numericInput.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--numeric-input-default-focus-ring` |
| `numericInput.destructive.background.default` | `{color.background.base.default}` | `color` | `--numeric-input-destructive-background-default` |
| `numericInput.destructive.background.filled` | `{color.background.danger.secondary}` | `color` | `--numeric-input-destructive-background-filled` |
| `numericInput.destructive.border.default` | `{color.border.danger.default}` | `color` | `--numeric-input-destructive-border-default` |
| `numericInput.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--numeric-input-destructive-border-hover` |
| `numericInput.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--numeric-input-destructive-border-focus` |
| `numericInput.destructive.focusRing` | `{palette.red.200}` | `color` | `--numeric-input-destructive-focus-ring` |
| `numericInput.success.background.default` | `{color.background.base.default}` | `color` | `--numeric-input-success-background-default` |
| `numericInput.success.background.filled` | `{color.background.positive.secondary}` | `color` | `--numeric-input-success-background-filled` |
| `numericInput.success.border.default` | `{color.border.positive.default}` | `color` | `--numeric-input-success-border-default` |
| `numericInput.success.border.hover` | `{color.border.positive.default}` | `color` | `--numeric-input-success-border-hover` |
| `numericInput.success.border.focus` | `{color.border.positive.default}` | `color` | `--numeric-input-success-border-focus` |
| `numericInput.success.focusRing` | `{palette.green.200}` | `color` | `--numeric-input-success-focus-ring` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/numeric-input.tokens.json</code></summary>

```json
{
  "numericInput": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "paddingInlineEndWithStepper": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "lg": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "textAlign": { "$value": "start", "$type": "string" },
      "loadingOpacity": { "$value": "0.6", "$type": "number" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" },
        "success": { "$value": "{color.text.positive.default}", "$type": "color" }
      }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "loading": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "suffix": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "$comment": "On the read-only gray surface (#f5f5f5), text.base.tertiary (#757575) is 4.22:1 and fails WCAG 1.4.3 AA — bump to text.base.secondary (#383838, ~10:1) for read-only.",
        "readOnly": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "marginInlineStart": { "$value": "{spacing.4}", "$type": "dimension" }
    },
    "stepper": {
      "width": {
        "md": { "$value": "{spacing.24}", "$type": "dimension" },
        "lg": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "hover": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "active": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "icon": {
        "size": {
          "md": { "$value": "{spacing.16}", "$type": "dimension" },
          "lg": { "$value": "{spacing.20}", "$type": "dimension" }
        },
        "color": {
          "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
          "hover": { "$value": "{color.icon.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
        }
      },
      "divider": {
        "color": { "$value": "{color.border.base.default}", "$type": "color" },
        "width": { "$value": "{borderWidth.1}", "$type": "dimension" }
      },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "loadingSpinner": {
      "size": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "readOnly": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "readOnly": { "$value": "{color.border.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "readOnly": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "destructive": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "filled": { "$value": "{color.background.danger.secondary}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    },
    "success": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "filled": { "$value": "{color.background.positive.secondary}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.positive.default}", "$type": "color" },
        "hover": { "$value": "{color.border.positive.default}", "$type": "color" },
        "focus": { "$value": "{color.border.positive.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.green.200}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/pagination.tokens.json`

_81 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `pagination.container.gap.sm` | `{spacing.4}` | `dimension` | `--pagination-container-gap-sm` |
| `pagination.container.gap.md` | `{spacing.24}` | `dimension` | `--pagination-container-gap-md` |
| `pagination.container.innerGap.sm` | `{spacing.8}` | `dimension` | `--pagination-container-inner-gap-sm` |
| `pagination.container.innerGap.md` | `{spacing.16}` | `dimension` | `--pagination-container-inner-gap-md` |
| `pagination.container.transitionDuration` | `150ms` | `duration` | `--pagination-container-transition-duration` |
| `pagination.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--pagination-container-transition-timing-function` |
| `pagination.item.size.sm` | `{spacing.32}` | `dimension` | `--pagination-item-size-sm` |
| `pagination.item.size.md` | `{spacing.40}` | `dimension` | `--pagination-item-size-md` |
| `pagination.item.borderRadius` | `{borderRadius.4}` | `dimension` | `--pagination-item-border-radius` |
| `pagination.item.borderWidth.default` | `0` | `dimension` | `--pagination-item-border-width-default` |
| `pagination.item.borderWidth.focus` | `{borderWidth.1-5}` | `dimension` | `--pagination-item-border-width-focus` |
| `pagination.item.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--pagination-item-font-family` |
| `pagination.item.fontSize` | `{fontSize.14}` | `dimension` | `--pagination-item-font-size` |
| `pagination.item.lineHeight` | `{lineHeight.20}` | `dimension` | `--pagination-item-line-height` |
| `pagination.item.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--pagination-item-font-weight` |
| `pagination.item.unselected.background.default` | `transparent` | `color` | `--pagination-item-unselected-background-default` |
| `pagination.item.unselected.background.hover` | `{color.background.base.tertiary}` | `color` | `--pagination-item-unselected-background-hover` |
| `pagination.item.unselected.background.active` | `{color.background.base.tertiary-active}` | `color` | `--pagination-item-unselected-background-active` |
| `pagination.item.unselected.label.default` | `{color.text.base.default}` | `color` | `--pagination-item-unselected-label-default` |
| `pagination.item.unselected.label.hover` | `{color.text.base.default}` | `color` | `--pagination-item-unselected-label-hover` |
| `pagination.item.unselected.label.active` | `{color.text.base.default}` | `color` | `--pagination-item-unselected-label-active` |
| `pagination.item.unselected.border.default` | `transparent` | `color` | `--pagination-item-unselected-border-default` |
| `pagination.item.unselected.border.focus` | `{color.border.brand.default}` | `color` | `--pagination-item-unselected-border-focus` |
| `pagination.item.selected.background.default` | `{color.background.brand.default}` | `color` | `--pagination-item-selected-background-default` |
| `pagination.item.selected.background.hover` | `{color.background.brand.default-hover}` | `color` | `--pagination-item-selected-background-hover` |
| `pagination.item.selected.background.active` | `{color.background.brand.default-active}` | `color` | `--pagination-item-selected-background-active` |
| `pagination.item.selected.label.default` | `{color.text.base-inverse.on-color}` | `color` | `--pagination-item-selected-label-default` |
| `pagination.item.selected.label.hover` | `{color.text.base-inverse.on-color}` | `color` | `--pagination-item-selected-label-hover` |
| `pagination.item.selected.label.active` | `{color.text.base-inverse.on-color}` | `color` | `--pagination-item-selected-label-active` |
| `pagination.item.disabled.background` | `transparent` | `color` | `--pagination-item-disabled-background` |
| `pagination.item.disabled.label` | `{color.text.disabled.default}` | `color` | `--pagination-item-disabled-label` |
| `pagination.ellipsis.size.sm` | `{spacing.32}` | `dimension` | `--pagination-ellipsis-size-sm` |
| `pagination.ellipsis.size.md` | `{spacing.40}` | `dimension` | `--pagination-ellipsis-size-md` |
| `pagination.ellipsis.label` | `{color.text.base.tertiary}` | `color` | `--pagination-ellipsis-label` |
| `pagination.overflow.trigger.background.default` | `transparent` | `color` | `--pagination-overflow-trigger-background-default` |
| `pagination.overflow.trigger.background.hover` | `{color.background.base.tertiary}` | `color` | `--pagination-overflow-trigger-background-hover` |
| `pagination.overflow.trigger.background.active` | `{color.background.base.tertiary-active}` | `color` | `--pagination-overflow-trigger-background-active` |
| `pagination.overflow.trigger.background.open` | `{color.background.base.tertiary}` | `color` | `--pagination-overflow-trigger-background-open` |
| `pagination.overflow.trigger.label` | `{color.text.base.tertiary}` | `color` | `--pagination-overflow-trigger-label` |
| `pagination.overflow.trigger.border.default` | `transparent` | `color` | `--pagination-overflow-trigger-border-default` |
| `pagination.overflow.trigger.border.focus` | `{color.border.brand.default}` | `color` | `--pagination-overflow-trigger-border-focus` |
| `pagination.overflow.menu.background` | `{color.background.base.default}` | `color` | `--pagination-overflow-menu-background` |
| `pagination.overflow.menu.border.color` | `{color.border.base.default}` | `color` | `--pagination-overflow-menu-border-color` |
| `pagination.overflow.menu.border.width` | `1px` | `dimension` | `--pagination-overflow-menu-border-width` |
| `pagination.overflow.menu.border.radius` | `{borderRadius.6}` | `dimension` | `--pagination-overflow-menu-border-radius` |
| `pagination.overflow.menu.shadow` | `{dropShadow.200}` | `string` | `--pagination-overflow-menu-shadow` |
| `pagination.overflow.menu.padding.inline` | `{spacing.4}` | `dimension` | `--pagination-overflow-menu-padding-inline` |
| `pagination.overflow.menu.padding.block` | `{spacing.4}` | `dimension` | `--pagination-overflow-menu-padding-block` |
| `pagination.overflow.menu.gap` | `{spacing.2}` | `dimension` | `--pagination-overflow-menu-gap` |
| `pagination.overflow.menu.minWidth` | `{spacing.56}` | `dimension` | `--pagination-overflow-menu-min-width` |
| `pagination.overflow.menu.offset` | `{spacing.4}` | `dimension` | `--pagination-overflow-menu-offset` |
| `pagination.overflow.menu.maxHeight` | `264px` | `dimension` | `--pagination-overflow-menu-max-height` |
| `pagination.overflow.menuItem.size.sm` | `{spacing.32}` | `dimension` | `--pagination-overflow-menu-item-size-sm` |
| `pagination.overflow.menuItem.size.md` | `{spacing.32}` | `dimension` | `--pagination-overflow-menu-item-size-md` |
| `pagination.overflow.menuItem.padding.inline` | `{spacing.12}` | `dimension` | `--pagination-overflow-menu-item-padding-inline` |
| `pagination.overflow.menuItem.borderRadius` | `{borderRadius.4}` | `dimension` | `--pagination-overflow-menu-item-border-radius` |
| `pagination.overflow.menuItem.background.default` | `transparent` | `color` | `--pagination-overflow-menu-item-background-default` |
| `pagination.overflow.menuItem.background.hover` | `{color.background.base.tertiary}` | `color` | `--pagination-overflow-menu-item-background-hover` |
| `pagination.overflow.menuItem.background.active` | `{color.background.base.tertiary-active}` | `color` | `--pagination-overflow-menu-item-background-active` |
| `pagination.overflow.menuItem.label` | `{color.text.base.default}` | `color` | `--pagination-overflow-menu-item-label` |
| `pagination.nav.height.sm` | `{spacing.32}` | `dimension` | `--pagination-nav-height-sm` |
| `pagination.nav.height.md` | `{spacing.40}` | `dimension` | `--pagination-nav-height-md` |
| `pagination.nav.minWidth.sm` | `{spacing.32}` | `dimension` | `--pagination-nav-min-width-sm` |
| `pagination.nav.minWidth.md` | `{spacing.56}` | `dimension` | `--pagination-nav-min-width-md` |
| `pagination.nav.paddingInlineStart.sm` | `{spacing.0}` | `dimension` | `--pagination-nav-padding-inline-start-sm` |
| `pagination.nav.paddingInlineStart.md` | `{spacing.12}` | `dimension` | `--pagination-nav-padding-inline-start-md` |
| `pagination.nav.paddingInlineEnd.sm` | `{spacing.0}` | `dimension` | `--pagination-nav-padding-inline-end-sm` |
| `pagination.nav.paddingInlineEnd.md` | `{spacing.16}` | `dimension` | `--pagination-nav-padding-inline-end-md` |
| `pagination.nav.gap` | `{spacing.6}` | `dimension` | `--pagination-nav-gap` |
| `pagination.nav.borderRadius` | `{borderRadius.6}` | `dimension` | `--pagination-nav-border-radius` |
| `pagination.nav.iconSize.sm` | `{spacing.16}` | `dimension` | `--pagination-nav-icon-size-sm` |
| `pagination.nav.iconSize.md` | `{spacing.20}` | `dimension` | `--pagination-nav-icon-size-md` |
| `pagination.nav.background.default` | `transparent` | `color` | `--pagination-nav-background-default` |
| `pagination.nav.background.hover` | `{color.background.base.tertiary}` | `color` | `--pagination-nav-background-hover` |
| `pagination.nav.background.active` | `{color.background.base.tertiary-active}` | `color` | `--pagination-nav-background-active` |
| `pagination.nav.label.default` | `{color.text.base.default}` | `color` | `--pagination-nav-label-default` |
| `pagination.nav.label.disabled` | `{color.text.disabled.default}` | `color` | `--pagination-nav-label-disabled` |
| `pagination.nav.icon.default` | `{color.icon.base.default}` | `color` | `--pagination-nav-icon-default` |
| `pagination.nav.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--pagination-nav-icon-disabled` |
| `pagination.nav.border.default` | `transparent` | `color` | `--pagination-nav-border-default` |
| `pagination.nav.border.focus` | `{color.border.brand.default}` | `color` | `--pagination-nav-border-focus` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/pagination.tokens.json</code></summary>

```json
{
  "pagination": {
    "container": {
      "gap": {
        "sm": { "$value": "{spacing.4}", "$type": "dimension" },
        "md": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "innerGap": {
        "sm": { "$value": "{spacing.8}", "$type": "dimension" },
        "md": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "item": {
      "size": {
        "sm": { "$value": "{spacing.32}", "$type": "dimension" },
        "md": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "0", "$type": "dimension" },
        "focus": { "$value": "{borderWidth.1-5}", "$type": "dimension" }
      },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "unselected": {
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
          "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.base.default}", "$type": "color" },
          "hover": { "$value": "{color.text.base.default}", "$type": "color" },
          "active": { "$value": "{color.text.base.default}", "$type": "color" }
        },
        "border": {
          "default": { "$value": "transparent", "$type": "color" },
          "focus": { "$value": "{color.border.brand.default}", "$type": "color" }
        }
      },
      "selected": {
        "background": {
          "default": { "$value": "{color.background.brand.default}", "$type": "color" },
          "hover": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
          "active": { "$value": "{color.background.brand.default-active}", "$type": "color" }
        },
        "label": {
          "default": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
          "hover": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
          "active": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" }
        }
      },
      "disabled": {
        "background": { "$value": "transparent", "$type": "color" },
        "label": { "$value": "{color.text.disabled.default}", "$type": "color" }
      }
    },
    "ellipsis": {
      "size": {
        "sm": { "$value": "{spacing.32}", "$type": "dimension" },
        "md": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "label": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    },
    "overflow": {
      "trigger": {
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
          "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" },
          "open": { "$value": "{color.background.base.tertiary}", "$type": "color" }
        },
        "label": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "border": {
          "default": { "$value": "transparent", "$type": "color" },
          "focus": { "$value": "{color.border.brand.default}", "$type": "color" }
        }
      },
      "menu": {
        "background": { "$value": "{color.background.base.default}", "$type": "color" },
        "border": {
          "color": { "$value": "{color.border.base.default}", "$type": "color" },
          "width": { "$value": "1px", "$type": "dimension" },
          "radius": { "$value": "{borderRadius.6}", "$type": "dimension" }
        },
        "shadow": { "$value": "{dropShadow.200}", "$type": "string" },
        "padding": {
          "inline": { "$value": "{spacing.4}", "$type": "dimension" },
          "block": { "$value": "{spacing.4}", "$type": "dimension" }
        },
        "gap": { "$value": "{spacing.2}", "$type": "dimension" },
        "minWidth": { "$value": "{spacing.56}", "$type": "dimension" },
        "offset": { "$value": "{spacing.4}", "$type": "dimension" },
        "maxHeight": { "$value": "264px", "$type": "dimension" }
      },
      "menuItem": {
        "size": {
          "sm": { "$value": "{spacing.32}", "$type": "dimension" },
          "md": { "$value": "{spacing.32}", "$type": "dimension" }
        },
        "padding": {
          "inline": { "$value": "{spacing.12}", "$type": "dimension" }
        },
        "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
        "background": {
          "default": { "$value": "transparent", "$type": "color" },
          "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
          "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
        },
        "label": { "$value": "{color.text.base.default}", "$type": "color" }
      }
    },
    "nav": {
      "height": {
        "sm": { "$value": "{spacing.32}", "$type": "dimension" },
        "md": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "minWidth": {
        "sm": { "$value": "{spacing.32}", "$type": "dimension" },
        "md": { "$value": "{spacing.56}", "$type": "dimension" }
      },
      "paddingInlineStart": {
        "sm": { "$value": "{spacing.0}", "$type": "dimension" },
        "md": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "paddingInlineEnd": {
        "sm": { "$value": "{spacing.0}", "$type": "dimension" },
        "md": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "iconSize": {
        "sm": { "$value": "{spacing.16}", "$type": "dimension" },
        "md": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "background": {
        "default": { "$value": "transparent", "$type": "color" },
        "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
      },
      "label": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
      },
      "icon": {
        "default": { "$value": "{color.icon.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "transparent", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/phone-input.tokens.json`

_142 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `phoneInput.field.gap` | `{spacing.8}` | `dimension` | `--phone-input-field-gap` |
| `phoneInput.container.height.md` | `{spacing.40}` | `dimension` | `--phone-input-container-height-md` |
| `phoneInput.container.height.lg` | `{spacing.48}` | `dimension` | `--phone-input-container-height-lg` |
| `phoneInput.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--phone-input-container-padding-inline-md` |
| `phoneInput.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--phone-input-container-padding-inline-lg` |
| `phoneInput.container.gap.md` | `{spacing.8}` | `dimension` | `--phone-input-container-gap-md` |
| `phoneInput.container.gap.lg` | `{spacing.8}` | `dimension` | `--phone-input-container-gap-lg` |
| `phoneInput.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--phone-input-container-border-radius` |
| `phoneInput.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--phone-input-container-border-width-default` |
| `phoneInput.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--phone-input-container-border-width-emphasized` |
| `phoneInput.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--phone-input-container-focus-ring-width` |
| `phoneInput.container.transitionDuration` | `150ms` | `duration` | `--phone-input-container-transition-duration` |
| `phoneInput.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--phone-input-container-transition-timing-function` |
| `phoneInput.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--phone-input-control-font-family` |
| `phoneInput.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--phone-input-control-font-weight` |
| `phoneInput.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--phone-input-control-font-size-md` |
| `phoneInput.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--phone-input-control-font-size-lg` |
| `phoneInput.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--phone-input-control-line-height-md` |
| `phoneInput.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--phone-input-control-line-height-lg` |
| `phoneInput.control.loadingOpacity` | `0.6` | `number` | `--phone-input-control-loading-opacity` |
| `phoneInput.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--phone-input-label-font-family` |
| `phoneInput.label.fontSize` | `{fontSize.14}` | `dimension` | `--phone-input-label-font-size` |
| `phoneInput.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--phone-input-label-line-height` |
| `phoneInput.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--phone-input-label-font-weight` |
| `phoneInput.label.gap` | `{spacing.4}` | `dimension` | `--phone-input-label-gap` |
| `phoneInput.label.color.default` | `{color.text.base.secondary}` | `color` | `--phone-input-label-color-default` |
| `phoneInput.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--phone-input-label-color-disabled` |
| `phoneInput.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--phone-input-label-required-mark-size` |
| `phoneInput.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--phone-input-label-required-mark-color` |
| `phoneInput.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--phone-input-assistive-font-family` |
| `phoneInput.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--phone-input-assistive-font-size` |
| `phoneInput.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--phone-input-assistive-line-height` |
| `phoneInput.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--phone-input-assistive-font-weight` |
| `phoneInput.assistive.gap.default` | `{spacing.6}` | `dimension` | `--phone-input-assistive-gap-default` |
| `phoneInput.assistive.gap.error` | `{spacing.4}` | `dimension` | `--phone-input-assistive-gap-error` |
| `phoneInput.assistive.iconSize` | `{spacing.20}` | `dimension` | `--phone-input-assistive-icon-size` |
| `phoneInput.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--phone-input-assistive-color-default` |
| `phoneInput.assistive.color.error` | `{color.text.danger.default}` | `color` | `--phone-input-assistive-color-error` |
| `phoneInput.assistive.color.warning` | `{color.text.warning.default}` | `color` | `--phone-input-assistive-color-warning` |
| `phoneInput.assistive.color.success` | `{color.text.positive.default}` | `color` | `--phone-input-assistive-color-success` |
| `phoneInput.icon.size.md` | `{spacing.20}` | `dimension` | `--phone-input-icon-size-md` |
| `phoneInput.icon.size.lg` | `{spacing.24}` | `dimension` | `--phone-input-icon-size-lg` |
| `phoneInput.icon.color.default` | `{color.icon.base.secondary}` | `color` | `--phone-input-icon-color-default` |
| `phoneInput.icon.color.loading` | `{color.icon.brand.default}` | `color` | `--phone-input-icon-color-loading` |
| `phoneInput.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--phone-input-icon-color-disabled` |
| `phoneInput.loadingSpinner.size.md` | `{spacing.16}` | `dimension` | `--phone-input-loading-spinner-size-md` |
| `phoneInput.loadingSpinner.size.lg` | `{spacing.20}` | `dimension` | `--phone-input-loading-spinner-size-lg` |
| `phoneInput.default.background.default` | `{color.background.base.default}` | `color` | `--phone-input-default-background-default` |
| `phoneInput.default.background.readOnly` | `{color.background.base.secondary}` | `color` | `--phone-input-default-background-read-only` |
| `phoneInput.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--phone-input-default-background-disabled` |
| `phoneInput.default.border.default` | `{color.border.base.default}` | `color` | `--phone-input-default-border-default` |
| `phoneInput.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--phone-input-default-border-hover` |
| `phoneInput.default.border.focus` | `{color.border.brand.default}` | `color` | `--phone-input-default-border-focus` |
| `phoneInput.default.border.readOnly` | `{color.border.base.default}` | `color` | `--phone-input-default-border-read-only` |
| `phoneInput.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--phone-input-default-border-disabled` |
| `phoneInput.default.text.default` | `{color.text.base.default}` | `color` | `--phone-input-default-text-default` |
| `phoneInput.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--phone-input-default-text-placeholder` |
| `phoneInput.default.text.readOnly` | `{color.text.base.default}` | `color` | `--phone-input-default-text-read-only` |
| `phoneInput.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--phone-input-default-text-disabled` |
| `phoneInput.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--phone-input-default-focus-ring` |
| `phoneInput.warning.background.default` | `{color.background.base.default}` | `color` | `--phone-input-warning-background-default` |
| `phoneInput.warning.border.default` | `{color.border.warning.default}` | `color` | `--phone-input-warning-border-default` |
| `phoneInput.warning.border.hover` | `{color.border.warning.default}` | `color` | `--phone-input-warning-border-hover` |
| `phoneInput.warning.border.focus` | `{color.border.warning.default}` | `color` | `--phone-input-warning-border-focus` |
| `phoneInput.warning.focusRing` | `{palette.apricot.200}` | `color` | `--phone-input-warning-focus-ring` |
| `phoneInput.destructive.background.default` | `{color.background.base.default}` | `color` | `--phone-input-destructive-background-default` |
| `phoneInput.destructive.border.default` | `{color.border.danger.default}` | `color` | `--phone-input-destructive-border-default` |
| `phoneInput.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--phone-input-destructive-border-hover` |
| `phoneInput.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--phone-input-destructive-border-focus` |
| `phoneInput.destructive.focusRing` | `{palette.red.200}` | `color` | `--phone-input-destructive-focus-ring` |
| `phoneInput.success.background.default` | `{color.background.base.default}` | `color` | `--phone-input-success-background-default` |
| `phoneInput.success.border.default` | `{color.border.positive.default}` | `color` | `--phone-input-success-border-default` |
| `phoneInput.success.border.hover` | `{color.border.positive.default}` | `color` | `--phone-input-success-border-hover` |
| `phoneInput.success.border.focus` | `{color.border.positive.default}` | `color` | `--phone-input-success-border-focus` |
| `phoneInput.success.focusRing` | `{palette.green.200}` | `color` | `--phone-input-success-focus-ring` |
| `phoneInput.countryTrigger.gap` | `{spacing.6}` | `dimension` | `--phone-input-country-trigger-gap` |
| `phoneInput.countryTrigger.paddingInline` | `{spacing.8}` | `dimension` | `--phone-input-country-trigger-padding-inline` |
| `phoneInput.countryTrigger.paddingBlock` | `{spacing.4}` | `dimension` | `--phone-input-country-trigger-padding-block` |
| `phoneInput.countryTrigger.borderRadius` | `{borderRadius.6}` | `dimension` | `--phone-input-country-trigger-border-radius` |
| `phoneInput.countryTrigger.background.default` | `{color.background.base.secondary}` | `color` | `--phone-input-country-trigger-background-default` |
| `phoneInput.countryTrigger.background.hover` | `{color.background.base.tertiary}` | `color` | `--phone-input-country-trigger-background-hover` |
| `phoneInput.countryTrigger.background.disabled` | `{color.background.disabled.default}` | `color` | `--phone-input-country-trigger-background-disabled` |
| `phoneInput.countryTrigger.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--phone-input-country-trigger-font-family` |
| `phoneInput.countryTrigger.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--phone-input-country-trigger-font-weight` |
| `phoneInput.countryTrigger.fontSize.md` | `{fontSize.14}` | `dimension` | `--phone-input-country-trigger-font-size-md` |
| `phoneInput.countryTrigger.fontSize.lg` | `{fontSize.16}` | `dimension` | `--phone-input-country-trigger-font-size-lg` |
| `phoneInput.countryTrigger.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--phone-input-country-trigger-line-height-md` |
| `phoneInput.countryTrigger.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--phone-input-country-trigger-line-height-lg` |
| `phoneInput.countryTrigger.color.default` | `{color.text.base.default}` | `color` | `--phone-input-country-trigger-color-default` |
| `phoneInput.countryTrigger.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--phone-input-country-trigger-color-disabled` |
| `phoneInput.countryTrigger.chevron.size.md` | `{spacing.16}` | `dimension` | `--phone-input-country-trigger-chevron-size-md` |
| `phoneInput.countryTrigger.chevron.size.lg` | `{spacing.16}` | `dimension` | `--phone-input-country-trigger-chevron-size-lg` |
| `phoneInput.countryTrigger.chevron.color.default` | `{color.icon.base.default}` | `color` | `--phone-input-country-trigger-chevron-color-default` |
| `phoneInput.countryTrigger.chevron.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--phone-input-country-trigger-chevron-color-disabled` |
| `phoneInput.countryTrigger.chevron.transitionDuration` | `150ms` | `duration` | `--phone-input-country-trigger-chevron-transition-duration` |
| `phoneInput.countryTrigger.chevron.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--phone-input-country-trigger-chevron-transition-timing-function` |
| `phoneInput.flag.width.md` | `{spacing.20}` | `dimension` | `--phone-input-flag-width-md` |
| `phoneInput.flag.width.lg` | `{spacing.24}` | `dimension` | `--phone-input-flag-width-lg` |
| `phoneInput.flag.height.md` | `{spacing.16}` | `dimension` | `--phone-input-flag-height-md` |
| `phoneInput.flag.height.lg` | `{spacing.16}` | `dimension` | `--phone-input-flag-height-lg` |
| `phoneInput.flag.borderRadius` | `{borderRadius.4}` | `dimension` | `--phone-input-flag-border-radius` |
| `phoneInput.flag.borderColor` | `{color.border.base.default}` | `color` | `--phone-input-flag-border-color` |
| `phoneInput.flag.borderWidth` | `{borderWidth.1}` | `dimension` | `--phone-input-flag-border-width` |
| `phoneInput.listbox.background` | `{color.background.base.default}` | `color` | `--phone-input-listbox-background` |
| `phoneInput.listbox.borderRadius` | `{borderRadius.8}` | `dimension` | `--phone-input-listbox-border-radius` |
| `phoneInput.listbox.borderWidth` | `{borderWidth.1}` | `dimension` | `--phone-input-listbox-border-width` |
| `phoneInput.listbox.borderColor` | `{color.border.base.default}` | `color` | `--phone-input-listbox-border-color` |
| `phoneInput.listbox.padding` | `{spacing.8}` | `dimension` | `--phone-input-listbox-padding` |
| `phoneInput.listbox.gap` | `{spacing.8}` | `dimension` | `--phone-input-listbox-gap` |
| `phoneInput.listbox.marginBlockStart` | `{spacing.8}` | `dimension` | `--phone-input-listbox-margin-block-start` |
| `phoneInput.listbox.shadow` | `{dropShadow.300}` | `string` | `--phone-input-listbox-shadow` |
| `phoneInput.listbox.maxBlockSize` | `320px` | `dimension` | `--phone-input-listbox-max-block-size` |
| `phoneInput.option.minBlockSize.md` | `{spacing.32}` | `dimension` | `--phone-input-option-min-block-size-md` |
| `phoneInput.option.minBlockSize.lg` | `{spacing.40}` | `dimension` | `--phone-input-option-min-block-size-lg` |
| `phoneInput.option.paddingInline` | `{spacing.8}` | `dimension` | `--phone-input-option-padding-inline` |
| `phoneInput.option.paddingBlock` | `{spacing.6}` | `dimension` | `--phone-input-option-padding-block` |
| `phoneInput.option.borderRadius` | `{borderRadius.4}` | `dimension` | `--phone-input-option-border-radius` |
| `phoneInput.option.gap` | `{spacing.8}` | `dimension` | `--phone-input-option-gap` |
| `phoneInput.option.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--phone-input-option-font-family` |
| `phoneInput.option.fontSize.md` | `{fontSize.14}` | `dimension` | `--phone-input-option-font-size-md` |
| `phoneInput.option.fontSize.lg` | `{fontSize.16}` | `dimension` | `--phone-input-option-font-size-lg` |
| `phoneInput.option.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--phone-input-option-line-height-md` |
| `phoneInput.option.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--phone-input-option-line-height-lg` |
| `phoneInput.option.fontWeight.default` | `{fontWeight.regular}` | `fontWeight` | `--phone-input-option-font-weight-default` |
| `phoneInput.option.fontWeight.selected` | `{fontWeight.medium}` | `fontWeight` | `--phone-input-option-font-weight-selected` |
| `phoneInput.option.background.default` | `{color.background.base.default}` | `color` | `--phone-input-option-background-default` |
| `phoneInput.option.background.hover` | `{color.background.base.secondary}` | `color` | `--phone-input-option-background-hover` |
| `phoneInput.option.background.selected` | `{color.background.base.secondary}` | `color` | `--phone-input-option-background-selected` |
| `phoneInput.option.background.active` | `{color.background.base.secondary}` | `color` | `--phone-input-option-background-active` |
| `phoneInput.option.text.default` | `{color.text.base.default}` | `color` | `--phone-input-option-text-default` |
| `phoneInput.option.text.secondary` | `{color.text.base.tertiary}` | `color` | `--phone-input-option-text-secondary` |
| `phoneInput.option.text.selected` | `{color.text.brand.default}` | `color` | `--phone-input-option-text-selected` |
| `phoneInput.option.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--phone-input-option-text-disabled` |
| `phoneInput.option.dialCode.minInlineSize` | `{spacing.48}` | `dimension` | `--phone-input-option-dial-code-min-inline-size` |
| `phoneInput.option.dialCode.color` | `{color.text.base.tertiary}` | `color` | `--phone-input-option-dial-code-color` |
| `phoneInput.option.checkColor` | `{color.icon.brand.default}` | `color` | `--phone-input-option-check-color` |
| `phoneInput.option.iconSize.md` | `{spacing.16}` | `dimension` | `--phone-input-option-icon-size-md` |
| `phoneInput.option.iconSize.lg` | `{spacing.20}` | `dimension` | `--phone-input-option-icon-size-lg` |
| `phoneInput.validIcon.size.md` | `{spacing.20}` | `dimension` | `--phone-input-valid-icon-size-md` |
| `phoneInput.validIcon.size.lg` | `{spacing.24}` | `dimension` | `--phone-input-valid-icon-size-lg` |
| `phoneInput.validIcon.color` | `{color.icon.positive.default}` | `color` | `--phone-input-valid-icon-color` |
| `phoneInput.liveRegion.position` | `absolute` | `string` | `--phone-input-live-region-position` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/phone-input.tokens.json</code></summary>

```json
{
  "phoneInput": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "loadingOpacity": { "$value": "0.6", "$type": "number" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" },
        "warning": { "$value": "{color.text.warning.default}", "$type": "color" },
        "success": { "$value": "{color.text.positive.default}", "$type": "color" }
      }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "loading": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "loadingSpinner": {
      "size": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "readOnly": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "readOnly": { "$value": "{color.border.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "readOnly": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "warning": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.warning.default}", "$type": "color" },
        "hover": { "$value": "{color.border.warning.default}", "$type": "color" },
        "focus": { "$value": "{color.border.warning.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.apricot.200}", "$type": "color" }
    },
    "destructive": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    },
    "success": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.positive.default}", "$type": "color" },
        "hover": { "$value": "{color.border.positive.default}", "$type": "color" },
        "focus": { "$value": "{color.border.positive.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.green.200}", "$type": "color" }
    },
    "countryTrigger": {
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "paddingInline": { "$value": "{spacing.8}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.4}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "background": {
        "default": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "chevron": {
        "size": {
          "md": { "$value": "{spacing.16}", "$type": "dimension" },
          "lg": { "$value": "{spacing.16}", "$type": "dimension" }
        },
        "color": {
          "default": { "$value": "{color.icon.base.default}", "$type": "color" },
          "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
        },
        "transitionDuration": { "$value": "150ms", "$type": "duration" },
        "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
      }
    },
    "flag": {
      "width": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "height": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" }
    },
    "listbox": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "padding": { "$value": "{spacing.8}", "$type": "dimension" },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "marginBlockStart": { "$value": "{spacing.8}", "$type": "dimension" },
      "shadow": { "$value": "{dropShadow.300}", "$type": "string" },
      "maxBlockSize": { "$value": "320px", "$type": "dimension" }
    },
    "option": {
      "minBlockSize": {
        "md": { "$value": "{spacing.32}", "$type": "dimension" },
        "lg": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "paddingInline": { "$value": "{spacing.8}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.6}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      },
      "fontWeight": {
        "default": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
        "selected": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
      },
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "hover": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "selected": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "active": { "$value": "{color.background.base.secondary}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "secondary": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "selected": { "$value": "{color.text.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "dialCode": {
        "minInlineSize": { "$value": "{spacing.48}", "$type": "dimension" },
        "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
      },
      "checkColor": { "$value": "{color.icon.brand.default}", "$type": "color" },
      "iconSize": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    },
    "validIcon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": { "$value": "{color.icon.positive.default}", "$type": "color" }
    },
    "liveRegion": {
      "position": { "$value": "absolute", "$type": "string" }
    }
  }
}
```

</details>

#### `tokens/core/components/progress-tracker.tokens.json`

_63 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `progress-tracker.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--progress-tracker-font-family` |
| `progress-tracker.fontSize` | `{fontSize.14}` | `dimension` | `--progress-tracker-font-size` |
| `progress-tracker.lineHeight` | `{lineHeight.20}` | `dimension` | `--progress-tracker-line-height` |
| `progress-tracker.fontWeight.label` | `{fontWeight.regular}` | `fontWeight` | `--progress-tracker-font-weight-label` |
| `progress-tracker.fontWeight.indicator` | `{fontWeight.medium}` | `fontWeight` | `--progress-tracker-font-weight-indicator` |
| `progress-tracker.supportingText.fontSize` | `{fontSize.12}` | `dimension` | `--progress-tracker-supporting-text-font-size` |
| `progress-tracker.supportingText.lineHeight` | `{lineHeight.20}` | `dimension` | `--progress-tracker-supporting-text-line-height` |
| `progress-tracker.supportingText.color` | `{color.text.base.tertiary}` | `color` | `--progress-tracker-supporting-text-color` |
| `progress-tracker.container.gap.horizontal` | `{spacing.12}` | `dimension` | `--progress-tracker-container-gap-horizontal` |
| `progress-tracker.container.gap.vertical` | `{spacing.12}` | `dimension` | `--progress-tracker-container-gap-vertical` |
| `progress-tracker.container.labelGap` | `{spacing.8}` | `dimension` | `--progress-tracker-container-label-gap` |
| `progress-tracker.container.maxWidth` | `996px` | `dimension` | `--progress-tracker-container-max-width` |
| `progress-tracker.container.verticalStepMinHeight` | `84px` | `dimension` | `--progress-tracker-container-vertical-step-min-height` |
| `progress-tracker.container.verticalStepMinHeightCompact` | `{spacing.56}` | `dimension` | `--progress-tracker-container-vertical-step-min-height-compact` |
| `progress-tracker.indicator.size` | `{spacing.24}` | `dimension` | `--progress-tracker-indicator-size` |
| `progress-tracker.indicator.borderWidth` | `{borderWidth.1-5}` | `dimension` | `--progress-tracker-indicator-border-width` |
| `progress-tracker.indicator.borderRadius` | `{borderRadius.full}` | `dimension` | `--progress-tracker-indicator-border-radius` |
| `progress-tracker.indicator.background` | `{color.background.base.default}` | `color` | `--progress-tracker-indicator-background` |
| `progress-tracker.indicator.transitionDuration` | `150ms` | `duration` | `--progress-tracker-indicator-transition-duration` |
| `progress-tracker.indicator.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--progress-tracker-indicator-transition-timing-function` |
| `progress-tracker.connector.thickness` | `{borderWidth.1-5}` | `dimension` | `--progress-tracker-connector-thickness` |
| `progress-tracker.connector.thicknessPending` | `{borderWidth.1}` | `dimension` | `--progress-tracker-connector-thickness-pending` |
| `progress-tracker.connector.minLength` | `{spacing.16}` | `dimension` | `--progress-tracker-connector-min-length` |
| `progress-tracker.label.color` | `{color.text.base.secondary}` | `color` | `--progress-tracker-label-color` |
| `progress-tracker.label.colorDisabled` | `{color.text.disabled.default}` | `color` | `--progress-tracker-label-color-disabled` |
| `progress-tracker.pending.indicator.border` | `{color.border.base.secondary}` | `color` | `--progress-tracker-pending-indicator-border` |
| `progress-tracker.pending.indicator.text` | `{color.text.disabled.default}` | `color` | `--progress-tracker-pending-indicator-text` |
| `progress-tracker.pending.indicator.background` | `{color.background.base.default}` | `color` | `--progress-tracker-pending-indicator-background` |
| `progress-tracker.pending.connector` | `{color.border.base.secondary}` | `color` | `--progress-tracker-pending-connector` |
| `progress-tracker.pending.label` | `{color.text.base.secondary}` | `color` | `--progress-tracker-pending-label` |
| `progress-tracker.current.indicator.border` | `{color.border.brand.default}` | `color` | `--progress-tracker-current-indicator-border` |
| `progress-tracker.current.indicator.text` | `{color.text.brand.default}` | `color` | `--progress-tracker-current-indicator-text` |
| `progress-tracker.current.indicator.background` | `{color.background.base.default}` | `color` | `--progress-tracker-current-indicator-background` |
| `progress-tracker.current.connector` | `{color.border.brand.default}` | `color` | `--progress-tracker-current-connector` |
| `progress-tracker.current.label` | `{color.text.base.secondary}` | `color` | `--progress-tracker-current-label` |
| `progress-tracker.current.labelFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--progress-tracker-current-label-font-weight` |
| `progress-tracker.completed.indicator.border` | `{color.border.brand.default}` | `color` | `--progress-tracker-completed-indicator-border` |
| `progress-tracker.completed.indicator.text` | `{color.text.base-inverse.on-color}` | `color` | `--progress-tracker-completed-indicator-text` |
| `progress-tracker.completed.indicator.background` | `{color.background.brand.default}` | `color` | `--progress-tracker-completed-indicator-background` |
| `progress-tracker.completed.connector` | `{color.border.brand.default}` | `color` | `--progress-tracker-completed-connector` |
| `progress-tracker.completed.label` | `{color.text.base.secondary}` | `color` | `--progress-tracker-completed-label` |
| `progress-tracker.completed.labelActionable` | `{color.text.brand.default}` | `color` | `--progress-tracker-completed-label-actionable` |
| `progress-tracker.error.indicator.border` | `{color.border.danger.default}` | `color` | `--progress-tracker-error-indicator-border` |
| `progress-tracker.error.indicator.text` | `{color.text.danger.default}` | `color` | `--progress-tracker-error-indicator-text` |
| `progress-tracker.error.indicator.background` | `{color.background.base.default}` | `color` | `--progress-tracker-error-indicator-background` |
| `progress-tracker.error.connector` | `{color.border.danger.default}` | `color` | `--progress-tracker-error-connector` |
| `progress-tracker.error.label` | `{color.text.base.secondary}` | `color` | `--progress-tracker-error-label` |
| `progress-tracker.available.indicator.border` | `{color.border.brand.default}` | `color` | `--progress-tracker-available-indicator-border` |
| `progress-tracker.available.indicator.text` | `{color.text.brand.default}` | `color` | `--progress-tracker-available-indicator-text` |
| `progress-tracker.available.indicator.background` | `{color.background.base.default}` | `color` | `--progress-tracker-available-indicator-background` |
| `progress-tracker.available.label` | `{color.text.brand.default}` | `color` | `--progress-tracker-available-label` |
| `progress-tracker.interactive.padding.inline` | `{spacing.2}` | `dimension` | `--progress-tracker-interactive-padding-inline` |
| `progress-tracker.interactive.padding.block` | `{spacing.2}` | `dimension` | `--progress-tracker-interactive-padding-block` |
| `progress-tracker.interactive.borderRadius` | `{borderRadius.6}` | `dimension` | `--progress-tracker-interactive-border-radius` |
| `progress-tracker.interactive.hover.background` | `{color.background.base.tertiary}` | `color` | `--progress-tracker-interactive-hover-background` |
| `progress-tracker.interactive.disabled.indicatorBorder` | `{color.border.disabled.default}` | `color` | `--progress-tracker-interactive-disabled-indicator-border` |
| `progress-tracker.interactive.disabled.indicatorText` | `{color.text.disabled.default}` | `color` | `--progress-tracker-interactive-disabled-indicator-text` |
| `progress-tracker.interactive.disabled.label` | `{color.text.disabled.default}` | `color` | `--progress-tracker-interactive-disabled-label` |
| `progress-tracker.stepIndicator.size` | `{spacing.24}` | `dimension` | `--progress-tracker-step-indicator-size` |
| `progress-tracker.stepIndicator.gap` | `{spacing.12}` | `dimension` | `--progress-tracker-step-indicator-gap` |
| `progress-tracker.stepIndicator.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--progress-tracker-step-indicator-label-font-weight` |
| `progress-tracker.stepIndicator.label.color` | `{color.text.base.default}` | `color` | `--progress-tracker-step-indicator-label-color` |
| `progress-tracker.stepIndicator.supportingText.color` | `{color.text.base.tertiary}` | `color` | `--progress-tracker-step-indicator-supporting-text-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/progress-tracker.tokens.json</code></summary>

```json
{
  "progress-tracker": {
    "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
    "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
    "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
    "fontWeight": {
      "label": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "indicator": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "supportingText": {
      "fontSize": { "$value": "{fontSize.12}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    },
    "container": {
      "gap": {
        "horizontal": { "$value": "{spacing.12}", "$type": "dimension" },
        "vertical": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "labelGap": { "$value": "{spacing.8}", "$type": "dimension" },
      "maxWidth": { "$value": "996px", "$type": "dimension" },
      "verticalStepMinHeight": { "$value": "84px", "$type": "dimension" },
      "verticalStepMinHeightCompact": {
        "$value": "{spacing.56}",
        "$type": "dimension",
        "$comment": "Tighter vertical rhythm for the compact dot rail (mobile) — still > indicator + 2× labelGap so the connector stays visible."
      }
    },
    "indicator": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" },
      "borderWidth": { "$value": "{borderWidth.1-5}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "connector": {
      "thickness": {
        "$value": "{borderWidth.1-5}",
        "$type": "dimension",
        "$comment": "Traveled/coloured rail (completed → brand, error → danger): 1.5px, slightly heavier than the untraveled grey."
      },
      "thicknessPending": {
        "$value": "{borderWidth.1}",
        "$type": "dimension",
        "$comment": "Untraveled grey rail (pending / current): 1px — thinner than the filled 1.5px bar per Figma."
      },
      "minLength": { "$value": "{spacing.16}", "$type": "dimension" }
    },
    "label": {
      "color": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "colorDisabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
    },
    "pending": {
      "indicator": {
        "border": { "$value": "{color.border.base.secondary}", "$type": "color" },
        "text": {
          "$value": "{color.text.disabled.default}",
          "$type": "color",
          "$comment": "#b2b2b2 per Figma 639:11186 (faded, not-yet-reached step). 2.12:1 on white is below WCAG AA 4.5:1, but the digit is aria-hidden/decorative — the step's status is announced via the <li> aria-label — and a pending step is an inactive UI component. Accepted exception (axe rates it 'needs review', not a violation). Do not re-flag."
        },
        "background": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "connector": { "$value": "{color.border.base.secondary}", "$type": "color" },
      "label": { "$value": "{color.text.base.secondary}", "$type": "color" }
    },
    "current": {
      "indicator": {
        "border": { "$value": "{color.border.brand.default}", "$type": "color" },
        "text": { "$value": "{color.text.brand.default}", "$type": "color" },
        "background": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "connector": { "$value": "{color.border.brand.default}", "$type": "color" },
      "label": {
        "$value": "{color.text.base.secondary}",
        "$type": "color",
        "$comment": "#383838 per Figma 639:11161 (was base.default #121212)."
      },
      "labelFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "completed": {
      "indicator": {
        "border": { "$value": "{color.border.brand.default}", "$type": "color" },
        "text": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
        "background": { "$value": "{color.background.brand.default}", "$type": "color" }
      },
      "connector": { "$value": "{color.border.brand.default}", "$type": "color" },
      "label": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "labelActionable": {
        "$value": "{color.text.brand.default}",
        "$type": "color",
        "$comment": "Interactive/clickable completed step → brand underlined link per Figma 639:11170."
      }
    },
    "error": {
      "indicator": {
        "border": { "$value": "{color.border.danger.default}", "$type": "color" },
        "text": { "$value": "{color.text.danger.default}", "$type": "color" },
        "background": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "connector": { "$value": "{color.border.danger.default}", "$type": "color" },
      "label": {
        "$value": "{color.text.base.secondary}",
        "$type": "color",
        "$comment": "#383838 per Figma 639:11202 — only the indicator is danger-red; the label stays neutral (was text.danger #d92d20)."
      }
    },
    "available": {
      "indicator": {
        "border": { "$value": "{color.border.brand.default}", "$type": "color" },
        "text": { "$value": "{color.text.brand.default}", "$type": "color" },
        "background": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "label": {
        "$value": "{color.text.brand.default}",
        "$type": "color",
        "$comment": "Navigable future step → brand label (underlined when interactive). Figma 639:11225. Connector inherits the pending grey default."
      }
    },
    "interactive": {
      "padding": {
        "inline": { "$value": "{spacing.2}", "$type": "dimension" },
        "block": { "$value": "{spacing.2}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "hover": {
        "background": { "$value": "{color.background.base.tertiary}", "$type": "color" }
      },
      "disabled": {
        "indicatorBorder": { "$value": "{color.border.disabled.default}", "$type": "color" },
        "indicatorText": { "$value": "{color.text.disabled.default}", "$type": "color" },
        "label": { "$value": "{color.text.disabled.default}", "$type": "color" }
      }
    },
    "stepIndicator": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "label": {
        "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
        "color": { "$value": "{color.text.base.default}", "$type": "color" }
      },
      "supportingText": {
        "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/radio.tokens.json`

_42 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `radio.container.size.md` | `{spacing.24}` | `dimension` | `--radio-container-size-md` |
| `radio.container.size.sm` | `{spacing.20}` | `dimension` | `--radio-container-size-sm` |
| `radio.container.borderWidth` | `{borderWidth.2}` | `dimension` | `--radio-container-border-width` |
| `radio.container.borderRadius` | `{borderRadius.full}` | `dimension` | `--radio-container-border-radius` |
| `radio.container.transitionDuration` | `150ms` | `duration` | `--radio-container-transition-duration` |
| `radio.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--radio-container-transition-timing-function` |
| `radio.dot.size.md` | `{spacing.12}` | `dimension` | `--radio-dot-size-md` |
| `radio.dot.size.sm` | `10px` | `dimension` | `--radio-dot-size-sm` |
| `radio.dot.color.default` | `{color.background.brand.default}` | `color` | `--radio-dot-color-default` |
| `radio.dot.color.error` | `{color.background.danger.default}` | `color` | `--radio-dot-color-error` |
| `radio.dot.color.disabled` | `{color.background.disabled.secondary}` | `color` | `--radio-dot-color-disabled` |
| `radio.border.default` | `{color.border.base.secondary}` | `color` | `--radio-border-default` |
| `radio.border.hover` | `{color.border.base.secondary}` | `color` | `--radio-border-hover` |
| `radio.border.checked` | `{color.border.brand.default}` | `color` | `--radio-border-checked` |
| `radio.border.error` | `{color.border.danger.default}` | `color` | `--radio-border-error` |
| `radio.border.disabled` | `{color.border.disabled.default}` | `color` | `--radio-border-disabled` |
| `radio.background.default` | `{color.background.base.default}` | `color` | `--radio-background-default` |
| `radio.background.disabled` | `{color.background.disabled.default}` | `color` | `--radio-background-disabled` |
| `radio.focusRing.innerWidth` | `{focusRing.width.inner}` | `dimension` | `--radio-focus-ring-inner-width` |
| `radio.focusRing.outerWidth` | `{focusRing.width.outer}` | `dimension` | `--radio-focus-ring-outer-width` |
| `radio.focusRing.innerColor` | `{focusRing.color.inner}` | `color` | `--radio-focus-ring-inner-color` |
| `radio.focusRing.outerColor` | `{focusRing.color.outer}` | `color` | `--radio-focus-ring-outer-color` |
| `radio.touchTarget.size.md` | `36px` | `dimension` | `--radio-touch-target-size-md` |
| `radio.touchTarget.size.sm` | `36px` | `dimension` | `--radio-touch-target-size-sm` |
| `radio.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--radio-label-font-family` |
| `radio.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--radio-label-font-weight` |
| `radio.label.fontSize.md` | `{fontSize.16}` | `dimension` | `--radio-label-font-size-md` |
| `radio.label.fontSize.sm` | `{fontSize.14}` | `dimension` | `--radio-label-font-size-sm` |
| `radio.label.lineHeight.md` | `{lineHeight.24}` | `dimension` | `--radio-label-line-height-md` |
| `radio.label.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--radio-label-line-height-sm` |
| `radio.label.color.default` | `{color.text.base.default}` | `color` | `--radio-label-color-default` |
| `radio.label.color.disabled` | `{color.text.disabled.default}` | `color` | `--radio-label-color-disabled` |
| `radio.supportingText.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--radio-supporting-text-font-family` |
| `radio.supportingText.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--radio-supporting-text-font-weight` |
| `radio.supportingText.fontSize.md` | `{fontSize.14}` | `dimension` | `--radio-supporting-text-font-size-md` |
| `radio.supportingText.fontSize.sm` | `{fontSize.12}` | `dimension` | `--radio-supporting-text-font-size-sm` |
| `radio.supportingText.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--radio-supporting-text-line-height-md` |
| `radio.supportingText.lineHeight.sm` | `{lineHeight.16}` | `dimension` | `--radio-supporting-text-line-height-sm` |
| `radio.supportingText.color.default` | `{color.text.base.tertiary}` | `color` | `--radio-supporting-text-color-default` |
| `radio.supportingText.color.disabled` | `{color.text.disabled.default}` | `color` | `--radio-supporting-text-color-disabled` |
| `radio.supportingText.marginTop` | `{spacing.2}` | `dimension` | `--radio-supporting-text-margin-top` |
| `radio.gap.radioLabel` | `{spacing.12}` | `dimension` | `--radio-gap-radio-label` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/radio.tokens.json</code></summary>

```json
{
  "radio": {
    "container": {
      "size": {
        "md": { "$value": "{spacing.24}", "$type": "dimension" },
        "sm": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "borderWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "dot": {
      "size": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "sm": { "$value": "10px", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.background.brand.default}", "$type": "color" },
        "error": { "$value": "{color.background.danger.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.secondary}", "$type": "color" }
      }
    },
    "border": {
      "default": {
        "$value": "{color.border.base.secondary}",
        "$type": "color",
        "$comment": "#b2b2b2 unselected border per Figma 578:34822 (was base.tertiary #444, too dark — same fix as mud-checkbox)."
      },
      "hover": {
        "$value": "{color.border.base.secondary}",
        "$type": "color",
        "$comment": "Matches default — Figma defines no distinct hover border for the unselected radio."
      },
      "checked": { "$value": "{color.border.brand.default}", "$type": "color" },
      "error": { "$value": "{color.border.danger.default}", "$type": "color" },
      "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
    },
    "background": {
      "default": { "$value": "{color.background.base.default}", "$type": "color" },
      "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
    },
    "focusRing": {
      "innerWidth": { "$value": "{focusRing.width.inner}", "$type": "dimension" },
      "outerWidth": { "$value": "{focusRing.width.outer}", "$type": "dimension" },
      "innerColor": { "$value": "{focusRing.color.inner}", "$type": "color" },
      "outerColor": { "$value": "{focusRing.color.outer}", "$type": "color" }
    },
    "touchTarget": {
      "size": {
        "md": {
          "$value": "36px",
          "$type": "dimension",
          "$comment": "WCAG/Figma coarse-pointer minimum 36×36 (Figma 2814:40915 Target). No spacing step maps to 36 (scale jumps 32→40); matches mud-checkbox touch target."
        },
        "sm": {
          "$value": "36px",
          "$type": "dimension",
          "$comment": "WCAG/Figma coarse-pointer minimum 36×36 (Figma 2813:40908 Target)."
        }
      }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.16}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.24}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
      }
    },
    "supportingText": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.12}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.16}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
      },
      "marginTop": { "$value": "{spacing.2}", "$type": "dimension" }
    },
    "gap": {
      "radioLabel": { "$value": "{spacing.12}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/receipt.tokens.json`

_64 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `receipt.container.background` | `{color.background.base.default}` | `color` | `--receipt-container-background` |
| `receipt.container.border` | `{color.border.base.tertiary}` | `color` | `--receipt-container-border` |
| `receipt.container.borderWidth` | `{borderWidth.1}` | `dimension` | `--receipt-container-border-width` |
| `receipt.container.borderRadius` | `{borderRadius.12}` | `dimension` | `--receipt-container-border-radius` |
| `receipt.container.shadow` | `{dropShadow.100}` | `string` | `--receipt-container-shadow` |
| `receipt.container.padding` | `{spacing.32}` | `dimension` | `--receipt-container-padding` |
| `receipt.container.paddingMobile` | `{spacing.20}` | `dimension` | `--receipt-container-padding-mobile` |
| `receipt.container.gap` | `{spacing.24}` | `dimension` | `--receipt-container-gap` |
| `receipt.container.maxWidth` | `640px` | `dimension` | `--receipt-container-max-width` |
| `receipt.divider.color` | `{color.border.base.tertiary}` | `color` | `--receipt-divider-color` |
| `receipt.divider.width` | `{borderWidth.1}` | `dimension` | `--receipt-divider-width` |
| `receipt.header.gap` | `{spacing.16}` | `dimension` | `--receipt-header-gap` |
| `receipt.header.logoSize` | `{spacing.32}` | `dimension` | `--receipt-header-logo-size` |
| `receipt.title.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--receipt-title-font-family` |
| `receipt.title.fontSize` | `{fontSize.20}` | `dimension` | `--receipt-title-font-size` |
| `receipt.title.lineHeight` | `{lineHeight.28}` | `dimension` | `--receipt-title-line-height` |
| `receipt.title.fontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--receipt-title-font-weight` |
| `receipt.title.color` | `{color.text.base.default}` | `color` | `--receipt-title-color` |
| `receipt.amount.background` | `{color.background.base.secondary}` | `color` | `--receipt-amount-background` |
| `receipt.amount.padding` | `{spacing.20}` | `dimension` | `--receipt-amount-padding` |
| `receipt.amount.borderRadius` | `{borderRadius.8}` | `dimension` | `--receipt-amount-border-radius` |
| `receipt.amount.gap` | `{spacing.4}` | `dimension` | `--receipt-amount-gap` |
| `receipt.amount.labelFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--receipt-amount-label-font-family` |
| `receipt.amount.labelFontSize` | `{fontSize.14}` | `dimension` | `--receipt-amount-label-font-size` |
| `receipt.amount.labelLineHeight` | `{lineHeight.20}` | `dimension` | `--receipt-amount-label-line-height` |
| `receipt.amount.labelFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--receipt-amount-label-font-weight` |
| `receipt.amount.labelColor` | `{color.text.base.secondary}` | `color` | `--receipt-amount-label-color` |
| `receipt.amount.valueFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--receipt-amount-value-font-family` |
| `receipt.amount.valueFontSize` | `{fontSize.28}` | `dimension` | `--receipt-amount-value-font-size` |
| `receipt.amount.valueLineHeight` | `{lineHeight.36}` | `dimension` | `--receipt-amount-value-line-height` |
| `receipt.amount.valueFontWeight` | `{fontWeight.semibold}` | `fontWeight` | `--receipt-amount-value-font-weight` |
| `receipt.amount.valueColor` | `{color.text.base.default}` | `color` | `--receipt-amount-value-color` |
| `receipt.amount.currencyFontSize` | `{fontSize.18}` | `dimension` | `--receipt-amount-currency-font-size` |
| `receipt.amount.currencyFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--receipt-amount-currency-font-weight` |
| `receipt.amount.currencyColor` | `{color.text.base.secondary}` | `color` | `--receipt-amount-currency-color` |
| `receipt.row.gap` | `{spacing.16}` | `dimension` | `--receipt-row-gap` |
| `receipt.row.paddingBlock` | `{spacing.8}` | `dimension` | `--receipt-row-padding-block` |
| `receipt.row.labelFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--receipt-row-label-font-family` |
| `receipt.row.labelFontSize` | `{fontSize.14}` | `dimension` | `--receipt-row-label-font-size` |
| `receipt.row.labelLineHeight` | `{lineHeight.20}` | `dimension` | `--receipt-row-label-line-height` |
| `receipt.row.labelFontWeight` | `{fontWeight.regular}` | `fontWeight` | `--receipt-row-label-font-weight` |
| `receipt.row.labelColor` | `{color.text.base.secondary}` | `color` | `--receipt-row-label-color` |
| `receipt.row.valueFontFamily` | `{fontFamily.primary}` | `fontFamily` | `--receipt-row-value-font-family` |
| `receipt.row.valueFontSize` | `{fontSize.14}` | `dimension` | `--receipt-row-value-font-size` |
| `receipt.row.valueLineHeight` | `{lineHeight.20}` | `dimension` | `--receipt-row-value-line-height` |
| `receipt.row.valueFontWeight` | `{fontWeight.medium}` | `fontWeight` | `--receipt-row-value-font-weight` |
| `receipt.row.valueColor` | `{color.text.base.default}` | `color` | `--receipt-row-value-color` |
| `receipt.qr.size` | `128px` | `dimension` | `--receipt-qr-size` |
| `receipt.qr.background` | `{palette.white.1000}` | `color` | `--receipt-qr-background` |
| `receipt.qr.moduleColor` | `{palette.black.1000}` | `color` | `--receipt-qr-module-color` |
| `receipt.qr.padding` | `{spacing.12}` | `dimension` | `--receipt-qr-padding` |
| `receipt.qr.borderColor` | `{color.border.base.tertiary}` | `color` | `--receipt-qr-border-color` |
| `receipt.qr.borderRadius` | `{borderRadius.8}` | `dimension` | `--receipt-qr-border-radius` |
| `receipt.qr.captionFontSize` | `{fontSize.12}` | `dimension` | `--receipt-qr-caption-font-size` |
| `receipt.qr.captionLineHeight` | `{lineHeight.16}` | `dimension` | `--receipt-qr-caption-line-height` |
| `receipt.qr.captionFontWeight` | `{fontWeight.regular}` | `fontWeight` | `--receipt-qr-caption-font-weight` |
| `receipt.qr.captionColor` | `{color.text.base.tertiary}` | `color` | `--receipt-qr-caption-color` |
| `receipt.actions.gap` | `{spacing.8}` | `dimension` | `--receipt-actions-gap` |
| `receipt.actions.paddingTop` | `{spacing.20}` | `dimension` | `--receipt-actions-padding-top` |
| `receipt.transactionId.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--receipt-transaction-id-font-family` |
| `receipt.transactionId.fontSize` | `{fontSize.12}` | `dimension` | `--receipt-transaction-id-font-size` |
| `receipt.transactionId.lineHeight` | `{lineHeight.16}` | `dimension` | `--receipt-transaction-id-line-height` |
| `receipt.transactionId.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--receipt-transaction-id-font-weight` |
| `receipt.transactionId.color` | `{color.text.base.tertiary}` | `color` | `--receipt-transaction-id-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/receipt.tokens.json</code></summary>

```json
{
  "receipt": {
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "border": { "$value": "{color.border.base.tertiary}", "$type": "color" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.12}", "$type": "dimension" },
      "shadow": { "$value": "{dropShadow.100}", "$type": "string" },
      "padding": { "$value": "{spacing.32}", "$type": "dimension" },
      "paddingMobile": { "$value": "{spacing.20}", "$type": "dimension" },
      "gap": { "$value": "{spacing.24}", "$type": "dimension" },
      "maxWidth": { "$value": "640px", "$type": "dimension" }
    },
    "divider": {
      "color": { "$value": "{color.border.base.tertiary}", "$type": "color" },
      "width": { "$value": "{borderWidth.1}", "$type": "dimension" }
    },
    "header": {
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "logoSize": { "$value": "{spacing.32}", "$type": "dimension" }
    },
    "title": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.20}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.28}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "color": { "$value": "{color.text.base.default}", "$type": "color" }
    },
    "amount": {
      "background": { "$value": "{color.background.base.secondary}", "$type": "color" },
      "padding": { "$value": "{spacing.20}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "labelFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "labelFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "labelFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "labelColor": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "valueFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "valueFontSize": { "$value": "{fontSize.28}", "$type": "dimension" },
      "valueLineHeight": { "$value": "{lineHeight.36}", "$type": "dimension" },
      "valueFontWeight": { "$value": "{fontWeight.semibold}", "$type": "fontWeight" },
      "valueColor": { "$value": "{color.text.base.default}", "$type": "color" },
      "currencyFontSize": { "$value": "{fontSize.18}", "$type": "dimension" },
      "currencyFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "currencyColor": { "$value": "{color.text.base.secondary}", "$type": "color" }
    },
    "row": {
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.8}", "$type": "dimension" },
      "labelFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "labelFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "labelLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "labelFontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "labelColor": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "valueFontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "valueFontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "valueLineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "valueFontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "valueColor": { "$value": "{color.text.base.default}", "$type": "color" }
    },
    "qr": {
      "size": { "$value": "128px", "$type": "dimension" },
      "_comment": "QR background + module color must be theme-locked (light surface, dark modules) for reliable scanning by phone cameras — even when the surrounding receipt is in dark mode. We anchor to palette primitives intentionally so semantic theme overrides cannot invert them.",
      "background": { "$value": "{palette.white.1000}", "$type": "color" },
      "moduleColor": { "$value": "{palette.black.1000}", "$type": "color" },
      "padding": { "$value": "{spacing.12}", "$type": "dimension" },
      "borderColor": { "$value": "{color.border.base.tertiary}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "captionFontSize": { "$value": "{fontSize.12}", "$type": "dimension" },
      "captionLineHeight": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "captionFontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "captionColor": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    },
    "actions": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "paddingTop": { "$value": "{spacing.20}", "$type": "dimension" }
    },
    "transactionId": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.12}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "color": { "$value": "{color.text.base.tertiary}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/search-input-circular.tokens.json`

_81 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `search-input-circular.field.gap` | `{spacing.8}` | `dimension` | `--search-input-circular-field-gap` |
| `search-input-circular.container.height.md` | `{spacing.40}` | `dimension` | `--search-input-circular-container-height-md` |
| `search-input-circular.container.height.lg` | `{spacing.48}` | `dimension` | `--search-input-circular-container-height-lg` |
| `search-input-circular.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--search-input-circular-container-padding-inline-md` |
| `search-input-circular.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--search-input-circular-container-padding-inline-lg` |
| `search-input-circular.container.gap.md` | `{spacing.8}` | `dimension` | `--search-input-circular-container-gap-md` |
| `search-input-circular.container.gap.lg` | `{spacing.8}` | `dimension` | `--search-input-circular-container-gap-lg` |
| `search-input-circular.container.borderRadius` | `{borderRadius.full}` | `dimension` | `--search-input-circular-container-border-radius` |
| `search-input-circular.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--search-input-circular-container-border-width-default` |
| `search-input-circular.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--search-input-circular-container-border-width-emphasized` |
| `search-input-circular.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--search-input-circular-container-focus-ring-width` |
| `search-input-circular.container.transitionDuration` | `150ms` | `duration` | `--search-input-circular-container-transition-duration` |
| `search-input-circular.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--search-input-circular-container-transition-timing-function` |
| `search-input-circular.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--search-input-circular-control-font-family` |
| `search-input-circular.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--search-input-circular-control-font-weight` |
| `search-input-circular.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--search-input-circular-control-font-size-md` |
| `search-input-circular.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--search-input-circular-control-font-size-lg` |
| `search-input-circular.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--search-input-circular-control-line-height-md` |
| `search-input-circular.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--search-input-circular-control-line-height-lg` |
| `search-input-circular.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--search-input-circular-label-font-family` |
| `search-input-circular.label.fontSize` | `{fontSize.14}` | `dimension` | `--search-input-circular-label-font-size` |
| `search-input-circular.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--search-input-circular-label-line-height` |
| `search-input-circular.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--search-input-circular-label-font-weight` |
| `search-input-circular.label.gap` | `{spacing.4}` | `dimension` | `--search-input-circular-label-gap` |
| `search-input-circular.label.color.default` | `{color.text.base.secondary}` | `color` | `--search-input-circular-label-color-default` |
| `search-input-circular.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--search-input-circular-label-color-disabled` |
| `search-input-circular.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--search-input-circular-assistive-font-family` |
| `search-input-circular.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--search-input-circular-assistive-font-size` |
| `search-input-circular.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--search-input-circular-assistive-line-height` |
| `search-input-circular.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--search-input-circular-assistive-font-weight` |
| `search-input-circular.assistive.gap.default` | `{spacing.6}` | `dimension` | `--search-input-circular-assistive-gap-default` |
| `search-input-circular.assistive.gap.error` | `{spacing.4}` | `dimension` | `--search-input-circular-assistive-gap-error` |
| `search-input-circular.assistive.iconSize` | `{spacing.20}` | `dimension` | `--search-input-circular-assistive-icon-size` |
| `search-input-circular.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--search-input-circular-assistive-color-default` |
| `search-input-circular.assistive.color.error` | `{color.text.danger.default}` | `color` | `--search-input-circular-assistive-color-error` |
| `search-input-circular.iconStart.size.md` | `{spacing.20}` | `dimension` | `--search-input-circular-icon-start-size-md` |
| `search-input-circular.iconStart.size.lg` | `{spacing.24}` | `dimension` | `--search-input-circular-icon-start-size-lg` |
| `search-input-circular.iconStart.color.default` | `{color.icon.base.secondary}` | `color` | `--search-input-circular-icon-start-color-default` |
| `search-input-circular.iconStart.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-circular-icon-start-color-disabled` |
| `search-input-circular.iconEndClear.size.md` | `{spacing.20}` | `dimension` | `--search-input-circular-icon-end-clear-size-md` |
| `search-input-circular.iconEndClear.size.lg` | `{spacing.24}` | `dimension` | `--search-input-circular-icon-end-clear-size-lg` |
| `search-input-circular.iconEndClear.padding` | `{spacing.4}` | `dimension` | `--search-input-circular-icon-end-clear-padding` |
| `search-input-circular.iconEndClear.borderRadius` | `{borderRadius.full}` | `dimension` | `--search-input-circular-icon-end-clear-border-radius` |
| `search-input-circular.iconEndClear.color.default` | `{color.icon.base.secondary}` | `color` | `--search-input-circular-icon-end-clear-color-default` |
| `search-input-circular.iconEndClear.color.hover` | `{color.icon.base.default}` | `color` | `--search-input-circular-icon-end-clear-color-hover` |
| `search-input-circular.iconEndClear.color.active` | `{color.icon.base.default}` | `color` | `--search-input-circular-icon-end-clear-color-active` |
| `search-input-circular.iconEndClear.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-circular-icon-end-clear-color-disabled` |
| `search-input-circular.iconEndClear.background.default` | `{color.background.base.tertiary}` | `color` | `--search-input-circular-icon-end-clear-background-default` |
| `search-input-circular.iconEndClear.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--search-input-circular-icon-end-clear-background-hover` |
| `search-input-circular.iconEndClear.background.active` | `{color.background.base.tertiary-active}` | `color` | `--search-input-circular-icon-end-clear-background-active` |
| `search-input-circular.default.background.default` | `{color.background.base.default}` | `color` | `--search-input-circular-default-background-default` |
| `search-input-circular.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--search-input-circular-default-background-disabled` |
| `search-input-circular.default.border.default` | `{color.border.base.default}` | `color` | `--search-input-circular-default-border-default` |
| `search-input-circular.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--search-input-circular-default-border-hover` |
| `search-input-circular.default.border.focus` | `{color.border.brand.default}` | `color` | `--search-input-circular-default-border-focus` |
| `search-input-circular.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--search-input-circular-default-border-disabled` |
| `search-input-circular.default.text.default` | `{color.text.base.default}` | `color` | `--search-input-circular-default-text-default` |
| `search-input-circular.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--search-input-circular-default-text-placeholder` |
| `search-input-circular.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--search-input-circular-default-text-disabled` |
| `search-input-circular.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--search-input-circular-default-focus-ring` |
| `search-input-circular.destructive.border.default` | `{color.border.danger.default}` | `color` | `--search-input-circular-destructive-border-default` |
| `search-input-circular.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--search-input-circular-destructive-border-hover` |
| `search-input-circular.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--search-input-circular-destructive-border-focus` |
| `search-input-circular.destructive.focusRing` | `{palette.red.200}` | `color` | `--search-input-circular-destructive-focus-ring` |
| `search-input-circular.submitButton.size.md` | `{spacing.32}` | `dimension` | `--search-input-circular-submit-button-size-md` |
| `search-input-circular.submitButton.size.lg` | `{spacing.40}` | `dimension` | `--search-input-circular-submit-button-size-lg` |
| `search-input-circular.submitButton.iconSize.md` | `{spacing.16}` | `dimension` | `--search-input-circular-submit-button-icon-size-md` |
| `search-input-circular.submitButton.iconSize.lg` | `{spacing.20}` | `dimension` | `--search-input-circular-submit-button-icon-size-lg` |
| `search-input-circular.submitButton.borderRadius` | `{borderRadius.full}` | `dimension` | `--search-input-circular-submit-button-border-radius` |
| `search-input-circular.submitButton.background.default` | `{color.background.brand.default}` | `color` | `--search-input-circular-submit-button-background-default` |
| `search-input-circular.submitButton.background.hover` | `{color.background.brand.default-hover}` | `color` | `--search-input-circular-submit-button-background-hover` |
| `search-input-circular.submitButton.background.active` | `{color.background.brand.default-active}` | `color` | `--search-input-circular-submit-button-background-active` |
| `search-input-circular.submitButton.background.disabled` | `{color.background.disabled.default}` | `color` | `--search-input-circular-submit-button-background-disabled` |
| `search-input-circular.submitButton.icon.default` | `{color.icon.base-inverse.on-color}` | `color` | `--search-input-circular-submit-button-icon-default` |
| `search-input-circular.submitButton.icon.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-circular-submit-button-icon-disabled` |
| `search-input-circular.submitButton.focusRingOffset` | `{borderWidth.2}` | `dimension` | `--search-input-circular-submit-button-focus-ring-offset` |
| `search-input-circular.submitButton.containerPaddingInlineEnd` | `{spacing.4}` | `dimension` | `--search-input-circular-submit-button-container-padding-inline-end` |
| `search-input-circular.loadingSpinner.size.md` | `{spacing.20}` | `dimension` | `--search-input-circular-loading-spinner-size-md` |
| `search-input-circular.loadingSpinner.size.lg` | `{spacing.24}` | `dimension` | `--search-input-circular-loading-spinner-size-lg` |
| `search-input-circular.loadingSpinner.color.default` | `{color.icon.brand.default}` | `color` | `--search-input-circular-loading-spinner-color-default` |
| `search-input-circular.loadingSpinner.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-circular-loading-spinner-color-disabled` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/search-input-circular.tokens.json</code></summary>

```json
{
  "search-input-circular": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": {
          "$value": "{spacing.12}",
          "$type": "dimension",
          "$comment": "Figma master 933:29731 uses spacing.12 at md (same as rectangular sibling) — the pill silhouette doesn't warrant extra inset."
        },
        "lg": {
          "$value": "{spacing.16}",
          "$type": "dimension",
          "$comment": "Figma master 933:29722 uses spacing.16 at lg (same as rectangular sibling)."
        }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "iconStart": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "iconEndClear": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "padding": { "$value": "{spacing.4}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.icon.base.default}", "$type": "color" },
        "active": { "$value": "{color.icon.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "background": {
        "default": {
          "$value": "{color.background.base.tertiary}",
          "$type": "color",
          "$comment": "Per Figma 933:29806 — the clear button carries a subtle gray oval at rest in the Filled state. Hardcoded color in Figma is #1212121a; base.tertiary (#f1f1f1) is the closest design-system semantic."
        },
        "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
        "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
      }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "destructive": {
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    },
    "submitButton": {
      "size": {
        "md": { "$value": "{spacing.32}", "$type": "dimension" },
        "lg": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "iconSize": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "background": {
        "default": { "$value": "{color.background.brand.default}", "$type": "color" },
        "hover": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
        "active": { "$value": "{color.background.brand.default-active}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "icon": {
        "default": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "focusRingOffset": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "containerPaddingInlineEnd": { "$value": "{spacing.4}", "$type": "dimension" }
    },
    "loadingSpinner": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/search-input-rectangular.tokens.json`

_81 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `search-input-rectangular.field.gap` | `{spacing.8}` | `dimension` | `--search-input-rectangular-field-gap` |
| `search-input-rectangular.container.height.md` | `{spacing.40}` | `dimension` | `--search-input-rectangular-container-height-md` |
| `search-input-rectangular.container.height.lg` | `{spacing.48}` | `dimension` | `--search-input-rectangular-container-height-lg` |
| `search-input-rectangular.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--search-input-rectangular-container-padding-inline-md` |
| `search-input-rectangular.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--search-input-rectangular-container-padding-inline-lg` |
| `search-input-rectangular.container.gap.md` | `{spacing.8}` | `dimension` | `--search-input-rectangular-container-gap-md` |
| `search-input-rectangular.container.gap.lg` | `{spacing.8}` | `dimension` | `--search-input-rectangular-container-gap-lg` |
| `search-input-rectangular.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--search-input-rectangular-container-border-radius` |
| `search-input-rectangular.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--search-input-rectangular-container-border-width-default` |
| `search-input-rectangular.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--search-input-rectangular-container-border-width-emphasized` |
| `search-input-rectangular.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--search-input-rectangular-container-focus-ring-width` |
| `search-input-rectangular.container.transitionDuration` | `150ms` | `duration` | `--search-input-rectangular-container-transition-duration` |
| `search-input-rectangular.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--search-input-rectangular-container-transition-timing-function` |
| `search-input-rectangular.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--search-input-rectangular-control-font-family` |
| `search-input-rectangular.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--search-input-rectangular-control-font-weight` |
| `search-input-rectangular.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--search-input-rectangular-control-font-size-md` |
| `search-input-rectangular.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--search-input-rectangular-control-font-size-lg` |
| `search-input-rectangular.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--search-input-rectangular-control-line-height-md` |
| `search-input-rectangular.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--search-input-rectangular-control-line-height-lg` |
| `search-input-rectangular.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--search-input-rectangular-label-font-family` |
| `search-input-rectangular.label.fontSize` | `{fontSize.14}` | `dimension` | `--search-input-rectangular-label-font-size` |
| `search-input-rectangular.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--search-input-rectangular-label-line-height` |
| `search-input-rectangular.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--search-input-rectangular-label-font-weight` |
| `search-input-rectangular.label.gap` | `{spacing.4}` | `dimension` | `--search-input-rectangular-label-gap` |
| `search-input-rectangular.label.color.default` | `{color.text.base.secondary}` | `color` | `--search-input-rectangular-label-color-default` |
| `search-input-rectangular.label.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--search-input-rectangular-label-color-disabled` |
| `search-input-rectangular.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--search-input-rectangular-assistive-font-family` |
| `search-input-rectangular.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--search-input-rectangular-assistive-font-size` |
| `search-input-rectangular.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--search-input-rectangular-assistive-line-height` |
| `search-input-rectangular.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--search-input-rectangular-assistive-font-weight` |
| `search-input-rectangular.assistive.gap.default` | `{spacing.6}` | `dimension` | `--search-input-rectangular-assistive-gap-default` |
| `search-input-rectangular.assistive.gap.error` | `{spacing.4}` | `dimension` | `--search-input-rectangular-assistive-gap-error` |
| `search-input-rectangular.assistive.iconSize` | `{spacing.20}` | `dimension` | `--search-input-rectangular-assistive-icon-size` |
| `search-input-rectangular.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--search-input-rectangular-assistive-color-default` |
| `search-input-rectangular.assistive.color.error` | `{color.text.danger.default}` | `color` | `--search-input-rectangular-assistive-color-error` |
| `search-input-rectangular.iconStart.size.md` | `{spacing.20}` | `dimension` | `--search-input-rectangular-icon-start-size-md` |
| `search-input-rectangular.iconStart.size.lg` | `{spacing.24}` | `dimension` | `--search-input-rectangular-icon-start-size-lg` |
| `search-input-rectangular.iconStart.color.default` | `{color.icon.base.secondary}` | `color` | `--search-input-rectangular-icon-start-color-default` |
| `search-input-rectangular.iconStart.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-rectangular-icon-start-color-disabled` |
| `search-input-rectangular.iconEndClear.size.md` | `{spacing.20}` | `dimension` | `--search-input-rectangular-icon-end-clear-size-md` |
| `search-input-rectangular.iconEndClear.size.lg` | `{spacing.24}` | `dimension` | `--search-input-rectangular-icon-end-clear-size-lg` |
| `search-input-rectangular.iconEndClear.padding` | `{spacing.4}` | `dimension` | `--search-input-rectangular-icon-end-clear-padding` |
| `search-input-rectangular.iconEndClear.borderRadius` | `{borderRadius.full}` | `dimension` | `--search-input-rectangular-icon-end-clear-border-radius` |
| `search-input-rectangular.iconEndClear.color.default` | `{color.icon.base.secondary}` | `color` | `--search-input-rectangular-icon-end-clear-color-default` |
| `search-input-rectangular.iconEndClear.color.hover` | `{color.icon.base.default}` | `color` | `--search-input-rectangular-icon-end-clear-color-hover` |
| `search-input-rectangular.iconEndClear.color.active` | `{color.icon.base.default}` | `color` | `--search-input-rectangular-icon-end-clear-color-active` |
| `search-input-rectangular.iconEndClear.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-rectangular-icon-end-clear-color-disabled` |
| `search-input-rectangular.iconEndClear.background.default` | `{color.background.base.tertiary}` | `color` | `--search-input-rectangular-icon-end-clear-background-default` |
| `search-input-rectangular.iconEndClear.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--search-input-rectangular-icon-end-clear-background-hover` |
| `search-input-rectangular.iconEndClear.background.active` | `{color.background.base.tertiary-active}` | `color` | `--search-input-rectangular-icon-end-clear-background-active` |
| `search-input-rectangular.default.background.default` | `{color.background.base.default}` | `color` | `--search-input-rectangular-default-background-default` |
| `search-input-rectangular.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--search-input-rectangular-default-background-disabled` |
| `search-input-rectangular.default.border.default` | `{color.border.base.default}` | `color` | `--search-input-rectangular-default-border-default` |
| `search-input-rectangular.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--search-input-rectangular-default-border-hover` |
| `search-input-rectangular.default.border.focus` | `{color.border.brand.default}` | `color` | `--search-input-rectangular-default-border-focus` |
| `search-input-rectangular.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--search-input-rectangular-default-border-disabled` |
| `search-input-rectangular.default.text.default` | `{color.text.base.default}` | `color` | `--search-input-rectangular-default-text-default` |
| `search-input-rectangular.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--search-input-rectangular-default-text-placeholder` |
| `search-input-rectangular.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--search-input-rectangular-default-text-disabled` |
| `search-input-rectangular.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--search-input-rectangular-default-focus-ring` |
| `search-input-rectangular.destructive.border.default` | `{color.border.danger.default}` | `color` | `--search-input-rectangular-destructive-border-default` |
| `search-input-rectangular.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--search-input-rectangular-destructive-border-hover` |
| `search-input-rectangular.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--search-input-rectangular-destructive-border-focus` |
| `search-input-rectangular.destructive.focusRing` | `{palette.red.200}` | `color` | `--search-input-rectangular-destructive-focus-ring` |
| `search-input-rectangular.submitButton.size.md` | `{spacing.32}` | `dimension` | `--search-input-rectangular-submit-button-size-md` |
| `search-input-rectangular.submitButton.size.lg` | `{spacing.40}` | `dimension` | `--search-input-rectangular-submit-button-size-lg` |
| `search-input-rectangular.submitButton.iconSize.md` | `{spacing.16}` | `dimension` | `--search-input-rectangular-submit-button-icon-size-md` |
| `search-input-rectangular.submitButton.iconSize.lg` | `{spacing.20}` | `dimension` | `--search-input-rectangular-submit-button-icon-size-lg` |
| `search-input-rectangular.submitButton.borderRadius` | `{borderRadius.6}` | `dimension` | `--search-input-rectangular-submit-button-border-radius` |
| `search-input-rectangular.submitButton.background.default` | `{color.background.brand.default}` | `color` | `--search-input-rectangular-submit-button-background-default` |
| `search-input-rectangular.submitButton.background.hover` | `{color.background.brand.default-hover}` | `color` | `--search-input-rectangular-submit-button-background-hover` |
| `search-input-rectangular.submitButton.background.active` | `{color.background.brand.default-active}` | `color` | `--search-input-rectangular-submit-button-background-active` |
| `search-input-rectangular.submitButton.background.disabled` | `{color.background.disabled.default}` | `color` | `--search-input-rectangular-submit-button-background-disabled` |
| `search-input-rectangular.submitButton.icon.default` | `{color.icon.base-inverse.on-color}` | `color` | `--search-input-rectangular-submit-button-icon-default` |
| `search-input-rectangular.submitButton.icon.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-rectangular-submit-button-icon-disabled` |
| `search-input-rectangular.submitButton.focusRingOffset` | `{borderWidth.2}` | `dimension` | `--search-input-rectangular-submit-button-focus-ring-offset` |
| `search-input-rectangular.submitButton.containerPaddingInlineEnd` | `{spacing.4}` | `dimension` | `--search-input-rectangular-submit-button-container-padding-inline-end` |
| `search-input-rectangular.loadingSpinner.size.md` | `{spacing.20}` | `dimension` | `--search-input-rectangular-loading-spinner-size-md` |
| `search-input-rectangular.loadingSpinner.size.lg` | `{spacing.24}` | `dimension` | `--search-input-rectangular-loading-spinner-size-lg` |
| `search-input-rectangular.loadingSpinner.color.default` | `{color.icon.brand.default}` | `color` | `--search-input-rectangular-loading-spinner-color-default` |
| `search-input-rectangular.loadingSpinner.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--search-input-rectangular-loading-spinner-color-disabled` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/search-input-rectangular.tokens.json</code></summary>

```json
{
  "search-input-rectangular": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "iconStart": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "iconEndClear": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "padding": { "$value": "{spacing.4}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.icon.base.default}", "$type": "color" },
        "active": { "$value": "{color.icon.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "background": {
        "default": {
          "$value": "{color.background.base.tertiary}",
          "$type": "color",
          "$comment": "Per Figma 933:29146 — the clear button carries a subtle gray oval at rest in the Filled state. Hardcoded color in Figma is #1212121a; base.tertiary (#f1f1f1) is the closest design-system semantic."
        },
        "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
        "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
      }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "destructive": {
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    },
    "submitButton": {
      "size": {
        "md": { "$value": "{spacing.32}", "$type": "dimension" },
        "lg": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "iconSize": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "lg": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.6}", "$type": "dimension" },
      "background": {
        "default": { "$value": "{color.background.brand.default}", "$type": "color" },
        "hover": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
        "active": { "$value": "{color.background.brand.default-active}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "icon": {
        "default": { "$value": "{color.icon.base-inverse.on-color}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "focusRingOffset": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "containerPaddingInlineEnd": { "$value": "{spacing.4}", "$type": "dimension" }
    },
    "loadingSpinner": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/segmented-control.tokens.json`

_38 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `segmented-control.container.background` | `{color.background.base.tertiary}` | `color` | `--segmented-control-container-background` |
| `segmented-control.container.background-disabled` | `{color.background.disabled.default}` | `color` | `--segmented-control-container-background-disabled` |
| `segmented-control.container.borderRadius` | `{borderRadius.full}` | `dimension` | `--segmented-control-container-border-radius` |
| `segmented-control.container.padding` | `{spacing.6}` | `dimension` | `--segmented-control-container-padding` |
| `segmented-control.container.gap` | `{spacing.6}` | `dimension` | `--segmented-control-container-gap` |
| `segmented-control.container.transitionDuration` | `150ms` | `duration` | `--segmented-control-container-transition-duration` |
| `segmented-control.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--segmented-control-container-transition-timing-function` |
| `segmented-control.segment.height.md` | `{spacing.40}` | `dimension` | `--segmented-control-segment-height-md` |
| `segmented-control.segment.height.sm` | `{spacing.32}` | `dimension` | `--segmented-control-segment-height-sm` |
| `segmented-control.segment.minWidth` | `68px` | `dimension` | `--segmented-control-segment-min-width` |
| `segmented-control.segment.paddingInline.md` | `{spacing.16}` | `dimension` | `--segmented-control-segment-padding-inline-md` |
| `segmented-control.segment.paddingInline.sm` | `{spacing.12}` | `dimension` | `--segmented-control-segment-padding-inline-sm` |
| `segmented-control.segment.gap` | `{spacing.6}` | `dimension` | `--segmented-control-segment-gap` |
| `segmented-control.segment.borderRadius` | `{borderRadius.full}` | `dimension` | `--segmented-control-segment-border-radius` |
| `segmented-control.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--segmented-control-label-font-family` |
| `segmented-control.label.fontSize.md` | `{fontSize.14}` | `dimension` | `--segmented-control-label-font-size-md` |
| `segmented-control.label.fontSize.sm` | `{fontSize.14}` | `dimension` | `--segmented-control-label-font-size-sm` |
| `segmented-control.label.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--segmented-control-label-line-height-md` |
| `segmented-control.label.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--segmented-control-label-line-height-sm` |
| `segmented-control.label.fontWeightSelected` | `{fontWeight.medium}` | `fontWeight` | `--segmented-control-label-font-weight-selected` |
| `segmented-control.label.fontWeightUnselected` | `{fontWeight.regular}` | `fontWeight` | `--segmented-control-label-font-weight-unselected` |
| `segmented-control.selected.background` | `{color.background.base-inverse.default}` | `color` | `--segmented-control-selected-background` |
| `segmented-control.selected.backgroundHover` | `{color.background.base-inverse.default-hover}` | `color` | `--segmented-control-selected-background-hover` |
| `segmented-control.selected.color` | `{color.text.base-inverse.default}` | `color` | `--segmented-control-selected-color` |
| `segmented-control.selected.shadow` | `0px 0px 0.25px rgba(0, 0, 0, 0.30), 0px 1px 1.5px rgba(0, 0, 0, 0.16)` | `string` | `--segmented-control-selected-shadow` |
| `segmented-control.unselected.background` | `{color.background.base.tertiary}` | `color` | `--segmented-control-unselected-background` |
| `segmented-control.unselected.backgroundHover` | `{color.background.base.tertiary-hover}` | `color` | `--segmented-control-unselected-background-hover` |
| `segmented-control.unselected.color` | `{color.text.base.secondary}` | `color` | `--segmented-control-unselected-color` |
| `segmented-control.unselected.colorHover` | `{color.text.base.default}` | `color` | `--segmented-control-unselected-color-hover` |
| `segmented-control.disabled.background` | `{color.background.disabled.default}` | `color` | `--segmented-control-disabled-background` |
| `segmented-control.disabled.color` | `{color.text.disabled.default}` | `color` | `--segmented-control-disabled-color` |
| `segmented-control.separator.color` | `{color.border.base.default}` | `color` | `--segmented-control-separator-color` |
| `segmented-control.separator.width` | `{borderWidth.1}` | `dimension` | `--segmented-control-separator-width` |
| `segmented-control.separator.height` | `{spacing.16}` | `dimension` | `--segmented-control-separator-height` |
| `segmented-control.focusRing.innerWidth` | `{focusRing.width.inner}` | `dimension` | `--segmented-control-focus-ring-inner-width` |
| `segmented-control.focusRing.outerWidth` | `{focusRing.width.outer}` | `dimension` | `--segmented-control-focus-ring-outer-width` |
| `segmented-control.focusRing.innerColor` | `{focusRing.color.inner}` | `color` | `--segmented-control-focus-ring-inner-color` |
| `segmented-control.focusRing.outerColor` | `{focusRing.color.outer}` | `color` | `--segmented-control-focus-ring-outer-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/segmented-control.tokens.json</code></summary>

```json
{
  "segmented-control": {
    "container": {
      "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "background-disabled": { "$value": "{color.background.disabled.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "padding": { "$value": "{spacing.6}", "$type": "dimension" },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "segment": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "sm": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "minWidth": { "$value": "68px", "$type": "dimension" },
      "paddingInline": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "sm": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" }
      },
      "fontWeightSelected": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontWeightUnselected": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "selected": {
      "background": { "$value": "{color.background.base-inverse.default}", "$type": "color" },
      "backgroundHover": { "$value": "{color.background.base-inverse.default-hover}", "$type": "color" },
      "color": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
      "shadow": { "$value": "0px 0px 0.25px rgba(0, 0, 0, 0.30), 0px 1px 1.5px rgba(0, 0, 0, 0.16)", "$type": "string" }
    },
    "unselected": {
      "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "backgroundHover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
      "color": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "colorHover": { "$value": "{color.text.base.default}", "$type": "color" }
    },
    "disabled": {
      "background": { "$value": "{color.background.disabled.default}", "$type": "color" },
      "color": { "$value": "{color.text.disabled.default}", "$type": "color" }
    },
    "separator": {
      "color": { "$value": "{color.border.base.default}", "$type": "color" },
      "width": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "height": { "$value": "{spacing.16}", "$type": "dimension" }
    },
    "focusRing": {
      "innerWidth": { "$value": "{focusRing.width.inner}", "$type": "dimension" },
      "outerWidth": { "$value": "{focusRing.width.outer}", "$type": "dimension" },
      "innerColor": { "$value": "{focusRing.color.inner}", "$type": "color" },
      "outerColor": { "$value": "{focusRing.color.outer}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/select-input.tokens.json`

_93 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `selectInput.field.gap` | `{spacing.8}` | `dimension` | `--select-input-field-gap` |
| `selectInput.container.height.md` | `{spacing.40}` | `dimension` | `--select-input-container-height-md` |
| `selectInput.container.height.lg` | `{spacing.48}` | `dimension` | `--select-input-container-height-lg` |
| `selectInput.container.paddingInline.md` | `{spacing.12}` | `dimension` | `--select-input-container-padding-inline-md` |
| `selectInput.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--select-input-container-padding-inline-lg` |
| `selectInput.container.gap.md` | `{spacing.8}` | `dimension` | `--select-input-container-gap-md` |
| `selectInput.container.gap.lg` | `{spacing.8}` | `dimension` | `--select-input-container-gap-lg` |
| `selectInput.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--select-input-container-border-radius` |
| `selectInput.container.borderWidth.default` | `{borderWidth.1}` | `dimension` | `--select-input-container-border-width-default` |
| `selectInput.container.borderWidth.emphasized` | `{borderWidth.2}` | `dimension` | `--select-input-container-border-width-emphasized` |
| `selectInput.container.focusRingWidth` | `{borderWidth.2}` | `dimension` | `--select-input-container-focus-ring-width` |
| `selectInput.container.transitionDuration` | `150ms` | `duration` | `--select-input-container-transition-duration` |
| `selectInput.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--select-input-container-transition-timing-function` |
| `selectInput.control.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--select-input-control-font-family` |
| `selectInput.control.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--select-input-control-font-weight` |
| `selectInput.control.fontSize.md` | `{fontSize.14}` | `dimension` | `--select-input-control-font-size-md` |
| `selectInput.control.fontSize.lg` | `{fontSize.16}` | `dimension` | `--select-input-control-font-size-lg` |
| `selectInput.control.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--select-input-control-line-height-md` |
| `selectInput.control.lineHeight.lg` | `{lineHeight.24}` | `dimension` | `--select-input-control-line-height-lg` |
| `selectInput.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--select-input-label-font-family` |
| `selectInput.label.fontSize` | `{fontSize.14}` | `dimension` | `--select-input-label-font-size` |
| `selectInput.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--select-input-label-line-height` |
| `selectInput.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--select-input-label-font-weight` |
| `selectInput.label.gap` | `{spacing.4}` | `dimension` | `--select-input-label-gap` |
| `selectInput.label.color.default` | `{color.text.base.secondary}` | `color` | `--select-input-label-color-default` |
| `selectInput.label.color.disabled` | `{color.text.base.secondary}` | `color` | `--select-input-label-color-disabled` |
| `selectInput.label.requiredMark.size` | `{spacing.12}` | `dimension` | `--select-input-label-required-mark-size` |
| `selectInput.label.requiredMark.color` | `{color.text.danger.default}` | `color` | `--select-input-label-required-mark-color` |
| `selectInput.assistive.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--select-input-assistive-font-family` |
| `selectInput.assistive.fontSize` | `{fontSize.14}` | `dimension` | `--select-input-assistive-font-size` |
| `selectInput.assistive.lineHeight` | `{lineHeight.20}` | `dimension` | `--select-input-assistive-line-height` |
| `selectInput.assistive.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--select-input-assistive-font-weight` |
| `selectInput.assistive.gap.default` | `{spacing.6}` | `dimension` | `--select-input-assistive-gap-default` |
| `selectInput.assistive.gap.error` | `{spacing.4}` | `dimension` | `--select-input-assistive-gap-error` |
| `selectInput.assistive.iconSize` | `{spacing.20}` | `dimension` | `--select-input-assistive-icon-size` |
| `selectInput.assistive.color.default` | `{color.text.base.secondary}` | `color` | `--select-input-assistive-color-default` |
| `selectInput.assistive.color.error` | `{color.text.danger.default}` | `color` | `--select-input-assistive-color-error` |
| `selectInput.icon.size.md` | `{spacing.20}` | `dimension` | `--select-input-icon-size-md` |
| `selectInput.icon.size.lg` | `{spacing.24}` | `dimension` | `--select-input-icon-size-lg` |
| `selectInput.icon.color.default` | `{color.icon.base.secondary}` | `color` | `--select-input-icon-color-default` |
| `selectInput.icon.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--select-input-icon-color-disabled` |
| `selectInput.chevron.size.md` | `{spacing.20}` | `dimension` | `--select-input-chevron-size-md` |
| `selectInput.chevron.size.lg` | `{spacing.24}` | `dimension` | `--select-input-chevron-size-lg` |
| `selectInput.chevron.color.default` | `{color.icon.base.secondary}` | `color` | `--select-input-chevron-color-default` |
| `selectInput.chevron.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--select-input-chevron-color-disabled` |
| `selectInput.chevron.transitionDuration` | `150ms` | `duration` | `--select-input-chevron-transition-duration` |
| `selectInput.chevron.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--select-input-chevron-transition-timing-function` |
| `selectInput.default.background.default` | `{color.background.base.default}` | `color` | `--select-input-default-background-default` |
| `selectInput.default.background.disabled` | `{color.background.disabled.default}` | `color` | `--select-input-default-background-disabled` |
| `selectInput.default.border.default` | `{color.border.base.default}` | `color` | `--select-input-default-border-default` |
| `selectInput.default.border.hover` | `{color.border.base.tertiary}` | `color` | `--select-input-default-border-hover` |
| `selectInput.default.border.focus` | `{color.border.brand.default}` | `color` | `--select-input-default-border-focus` |
| `selectInput.default.border.disabled` | `{color.border.disabled.default}` | `color` | `--select-input-default-border-disabled` |
| `selectInput.default.text.default` | `{color.text.base.default}` | `color` | `--select-input-default-text-default` |
| `selectInput.default.text.placeholder` | `{color.text.base.tertiary}` | `color` | `--select-input-default-text-placeholder` |
| `selectInput.default.text.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--select-input-default-text-disabled` |
| `selectInput.default.focusRing` | `{palette.blue-sky.200}` | `color` | `--select-input-default-focus-ring` |
| `selectInput.destructive.border.default` | `{color.border.danger.default}` | `color` | `--select-input-destructive-border-default` |
| `selectInput.destructive.border.hover` | `{color.border.danger.default}` | `color` | `--select-input-destructive-border-hover` |
| `selectInput.destructive.border.focus` | `{color.border.danger.default}` | `color` | `--select-input-destructive-border-focus` |
| `selectInput.destructive.focusRing` | `{palette.red.200}` | `color` | `--select-input-destructive-focus-ring` |
| `selectInput.listbox.background` | `{color.background.base.default}` | `color` | `--select-input-listbox-background` |
| `selectInput.listbox.borderRadius` | `{borderRadius.16}` | `dimension` | `--select-input-listbox-border-radius` |
| `selectInput.listbox.borderWidth` | `0px` | `dimension` | `--select-input-listbox-border-width` |
| `selectInput.listbox.borderColor` | `{color.border.base.default}` | `color` | `--select-input-listbox-border-color` |
| `selectInput.listbox.padding` | `{spacing.8}` | `dimension` | `--select-input-listbox-padding` |
| `selectInput.listbox.gap` | `{spacing.4}` | `dimension` | `--select-input-listbox-gap` |
| `selectInput.listbox.marginBlockStart` | `{spacing.4}` | `dimension` | `--select-input-listbox-margin-block-start` |
| `selectInput.listbox.shadow` | `{dropShadow.300}` | `string` | `--select-input-listbox-shadow` |
| `selectInput.listbox.maxBlockSize` | `320px` | `dimension` | `--select-input-listbox-max-block-size` |
| `selectInput.option.minBlockSize.md` | `{spacing.40}` | `dimension` | `--select-input-option-min-block-size-md` |
| `selectInput.option.minBlockSize.lg` | `{spacing.40}` | `dimension` | `--select-input-option-min-block-size-lg` |
| `selectInput.option.paddingInline` | `{spacing.16}` | `dimension` | `--select-input-option-padding-inline` |
| `selectInput.option.paddingBlock` | `{spacing.12}` | `dimension` | `--select-input-option-padding-block` |
| `selectInput.option.borderRadius` | `{borderRadius.8}` | `dimension` | `--select-input-option-border-radius` |
| `selectInput.option.gap` | `{spacing.12}` | `dimension` | `--select-input-option-gap` |
| `selectInput.option.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--select-input-option-font-family` |
| `selectInput.option.fontSize.md` | `{fontSize.14}` | `dimension` | `--select-input-option-font-size-md` |
| `selectInput.option.fontSize.lg` | `{fontSize.14}` | `dimension` | `--select-input-option-font-size-lg` |
| `selectInput.option.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--select-input-option-line-height-md` |
| `selectInput.option.lineHeight.lg` | `{lineHeight.20}` | `dimension` | `--select-input-option-line-height-lg` |
| `selectInput.option.fontWeight.default` | `{fontWeight.medium}` | `fontWeight` | `--select-input-option-font-weight-default` |
| `selectInput.option.fontWeight.selected` | `{fontWeight.medium}` | `fontWeight` | `--select-input-option-font-weight-selected` |
| `selectInput.option.iconSize.md` | `{spacing.20}` | `dimension` | `--select-input-option-icon-size-md` |
| `selectInput.option.iconSize.lg` | `{spacing.24}` | `dimension` | `--select-input-option-icon-size-lg` |
| `selectInput.option.background.default` | `{color.background.base.default}` | `color` | `--select-input-option-background-default` |
| `selectInput.option.background.hover` | `{color.background.base.secondary}` | `color` | `--select-input-option-background-hover` |
| `selectInput.option.background.active` | `{color.background.base.secondary}` | `color` | `--select-input-option-background-active` |
| `selectInput.option.background.selected` | `{color.background.base.secondary}` | `color` | `--select-input-option-background-selected` |
| `selectInput.option.text.default` | `{color.text.base.secondary}` | `color` | `--select-input-option-text-default` |
| `selectInput.option.text.selected` | `{color.text.brand.default}` | `color` | `--select-input-option-text-selected` |
| `selectInput.option.text.disabled` | `{color.text.base.tertiary}` | `color` | `--select-input-option-text-disabled` |
| `selectInput.option.checkColor` | `{color.icon.brand.default}` | `color` | `--select-input-option-check-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/select-input.tokens.json</code></summary>

```json
{
  "selectInput": {
    "field": {
      "gap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "container": {
      "height": {
        "md": { "$value": "{spacing.40}", "$type": "dimension" },
        "lg": { "$value": "{spacing.48}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.8}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "borderWidth": {
        "default": { "$value": "{borderWidth.1}", "$type": "dimension" },
        "emphasized": { "$value": "{borderWidth.2}", "$type": "dimension" }
      },
      "focusRingWidth": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "control": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": { "$value": "{fontSize.16}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.24}", "$type": "dimension" }
      }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": {
        "$value": "{fontWeight.regular}",
        "$type": "fontWeight",
        "$comment": "Per Figma 454:4192 — Label uses Onest Regular (fw-regular 400), not medium."
      },
      "gap": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "disabled": {
          "$value": "{color.text.base.secondary}",
          "$type": "color",
          "$comment": "Stays at base.secondary in disabled state; on-disabled (#b2b2b2) fails WCAG AA on the disabled grey surface. Figma's Disabled SelectInput keeps the label dark."
        }
      },
      "requiredMark": {
        "size": { "$value": "{spacing.12}", "$type": "dimension" },
        "color": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "assistive": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": {
        "default": { "$value": "{spacing.6}", "$type": "dimension" },
        "error": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "iconSize": { "$value": "{spacing.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.secondary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" }
      }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "chevron": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": { "$value": "{spacing.24}", "$type": "dimension" }
      },
      "color": {
        "default": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "default": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
      },
      "border": {
        "default": { "$value": "{color.border.base.default}", "$type": "color" },
        "hover": { "$value": "{color.border.base.tertiary}", "$type": "color" },
        "focus": { "$value": "{color.border.brand.default}", "$type": "color" },
        "disabled": { "$value": "{color.border.disabled.default}", "$type": "color" }
      },
      "text": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "placeholder": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.blue-sky.200}", "$type": "color" }
    },
    "destructive": {
      "border": {
        "default": { "$value": "{color.border.danger.default}", "$type": "color" },
        "hover": { "$value": "{color.border.danger.default}", "$type": "color" },
        "focus": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "focusRing": { "$value": "{palette.red.200}", "$type": "color" }
    },
    "listbox": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": {
        "$value": "{borderRadius.16}",
        "$type": "dimension",
        "$comment": "Figma selection-menu 172:3093 uses border-radius/16."
      },
      "borderWidth": {
        "$value": "0px",
        "$type": "dimension",
        "$comment": "Figma selection-menu has no border — depth comes from dropShadow.300 only."
      },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "padding": {
        "$value": "{spacing.8}",
        "$type": "dimension",
        "$comment": "Figma selection-menu uses spacing/8 around the option list."
      },
      "gap": {
        "$value": "{spacing.4}",
        "$type": "dimension",
        "$comment": "Figma selection-menu uses spacing/4 between options."
      },
      "marginBlockStart": { "$value": "{spacing.4}", "$type": "dimension" },
      "shadow": { "$value": "{dropShadow.300}", "$type": "string" },
      "maxBlockSize": { "$value": "320px", "$type": "dimension" }
    },
    "option": {
      "minBlockSize": {
        "md": {
          "$value": "{spacing.40}",
          "$type": "dimension",
          "$comment": "Figma menu-item-selection: 12px paddingBlock + 16px lh = 40px clickable target."
        },
        "lg": {
          "$value": "{spacing.40}",
          "$type": "dimension",
          "$comment": "Floor on the clickable target — intrinsic content (12px paddingBlock × 2 + 20px lh) drives the rendered height to ~44px."
        }
      },
      "paddingInline": {
        "$value": "{spacing.16}",
        "$type": "dimension",
        "$comment": "Figma menu-item-selection uses spacing/16 left/right."
      },
      "paddingBlock": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma menu-item-selection uses spacing/12 top/bottom."
      },
      "borderRadius": {
        "$value": "{borderRadius.8}",
        "$type": "dimension",
        "$comment": "Figma menu-item-selection uses border-radius/8."
      },
      "gap": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma menu-item-selection puts spacing/12 between label and check icon."
      },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lg": {
          "$value": "{fontSize.14}",
          "$type": "dimension",
          "$comment": "Figma menu-item-selection renders Body/Small 500 (14px) at both rungs."
        }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "lg": { "$value": "{lineHeight.20}", "$type": "dimension" }
      },
      "fontWeight": {
        "default": {
          "$value": "{fontWeight.medium}",
          "$type": "fontWeight",
          "$comment": "Figma menu-item-selection Default state uses Onest Medium (500), matching the Selected weight."
        },
        "selected": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
      },
      "iconSize": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "lg": {
          "$value": "{spacing.24}",
          "$type": "dimension",
          "$comment": "Figma menu-item-selection renders the 24/checkmark-small icon at 24px."
        }
      },
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "hover": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "active": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "selected": { "$value": "{color.background.base.secondary}", "$type": "color" }
      },
      "text": {
        "default": {
          "$value": "{color.text.base.secondary}",
          "$type": "color",
          "$comment": "Figma menu-item-selection Default text is color.text.base.secondary (#383838)."
        },
        "selected": { "$value": "{color.text.brand.default}", "$type": "color" },
        "disabled": {
          "$value": "{color.text.base.tertiary}",
          "$type": "color",
          "$comment": "Disabled options keep dark-enough contrast on the white surface; text.disabled.on-disabled fails AA on white."
        }
      },
      "checkColor": { "$value": "{color.icon.brand.default}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/separator.tokens.json`

_14 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `separator.color.subtle` | `{color.border.base.default}` | `color` | `--separator-color-subtle` |
| `separator.color.mild` | `{color.border.base.secondary}` | `color` | `--separator-color-mild` |
| `separator.color.strong` | `{color.border.base.tertiary}` | `color` | `--separator-color-strong` |
| `separator.size.extra-thin` | `{borderWidth.0-5}` | `dimension` | `--separator-size-extra-thin` |
| `separator.size.thin` | `{borderWidth.1}` | `dimension` | `--separator-size-thin` |
| `separator.size.medium` | `{borderWidth.1-5}` | `dimension` | `--separator-size-medium` |
| `separator.size.thick` | `{borderWidth.2}` | `dimension` | `--separator-size-thick` |
| `separator.label.gap` | `{spacing.8}` | `dimension` | `--separator-label-gap` |
| `separator.label.color` | `{color.text.base.tertiary}` | `color` | `--separator-label-color` |
| `separator.label.fontSize` | `{fontSize.14}` | `dimension` | `--separator-label-font-size` |
| `separator.label.fontWeight` | `400` | `fontWeight` | `--separator-label-font-weight` |
| `separator.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--separator-label-line-height` |
| `separator.label.padding-inline` | `{spacing.8}` | `dimension` | `--separator-label-padding-inline` |
| `separator.inset.spacing` | `{spacing.8}` | `dimension` | `--separator-inset-spacing` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/separator.tokens.json</code></summary>

```json
{
  "separator": {
    "color": {
      "subtle": {
        "$value": "{color.border.base.default}",
        "$type": "color"
      },
      "mild": {
        "$value": "{color.border.base.secondary}",
        "$type": "color"
      },
      "strong": {
        "$value": "{color.border.base.tertiary}",
        "$type": "color"
      }
    },
    "size": {
      "extra-thin": {
        "$value": "{borderWidth.0-5}",
        "$type": "dimension"
      },
      "thin": {
        "$value": "{borderWidth.1}",
        "$type": "dimension"
      },
      "medium": {
        "$value": "{borderWidth.1-5}",
        "$type": "dimension"
      },
      "thick": {
        "$value": "{borderWidth.2}",
        "$type": "dimension"
      }
    },
    "label": {
      "gap": {
        "$value": "{spacing.8}",
        "$type": "dimension"
      },
      "color": {
        "$value": "{color.text.base.tertiary}",
        "$type": "color"
      },
      "fontSize": {
        "$value": "{fontSize.14}",
        "$type": "dimension"
      },
      "fontWeight": {
        "$value": 400,
        "$type": "fontWeight"
      },
      "lineHeight": {
        "$value": "{lineHeight.20}",
        "$type": "dimension"
      },
      "padding-inline": {
        "$value": "{spacing.8}",
        "$type": "dimension"
      }
    },
    "inset": {
      "spacing": {
        "$value": "{spacing.8}",
        "$type": "dimension"
      }
    }
  }
}
```

</details>

#### `tokens/core/components/service-button.tokens.json`

_24 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `service-button.container.height` | `{spacing.48}` | `dimension` | `--service-button-container-height` |
| `service-button.container.minWidth` | `72px` | `dimension` | `--service-button-container-min-width` |
| `service-button.container.paddingInlineStart` | `{spacing.16}` | `dimension` | `--service-button-container-padding-inline-start` |
| `service-button.container.paddingInlineEnd` | `{spacing.20}` | `dimension` | `--service-button-container-padding-inline-end` |
| `service-button.container.gap` | `{spacing.6}` | `dimension` | `--service-button-container-gap` |
| `service-button.container.borderRadius` | `{borderRadius.8}` | `dimension` | `--service-button-container-border-radius` |
| `service-button.container.transitionDuration` | `150ms` | `duration` | `--service-button-container-transition-duration` |
| `service-button.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--service-button-container-transition-timing-function` |
| `service-button.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--service-button-label-font-family` |
| `service-button.label.fontSize` | `{fontSize.16}` | `dimension` | `--service-button-label-font-size` |
| `service-button.label.lineHeight` | `{lineHeight.24}` | `dimension` | `--service-button-label-line-height` |
| `service-button.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--service-button-label-font-weight` |
| `service-button.badge.size` | `{spacing.24}` | `dimension` | `--service-button-badge-size` |
| `service-button.primary.background.default` | `{color.background.brand.default}` | `color` | `--service-button-primary-background-default` |
| `service-button.primary.background.hover` | `{color.background.brand.default-hover}` | `color` | `--service-button-primary-background-hover` |
| `service-button.primary.background.active` | `{color.background.brand.default-active}` | `color` | `--service-button-primary-background-active` |
| `service-button.primary.label` | `{color.text.base-inverse.on-color}` | `color` | `--service-button-primary-label` |
| `service-button.neutral.background.default` | `{color.background.base.secondary}` | `color` | `--service-button-neutral-background-default` |
| `service-button.neutral.background.hover` | `{color.background.base.tertiary-hover}` | `color` | `--service-button-neutral-background-hover` |
| `service-button.neutral.background.active` | `{color.background.base.tertiary-active}` | `color` | `--service-button-neutral-background-active` |
| `service-button.neutral.label` | `{color.text.base.default}` | `color` | `--service-button-neutral-label` |
| `service-button.disabled.background` | `{color.background.disabled.default}` | `color` | `--service-button-disabled-background` |
| `service-button.disabled.label` | `{color.text.disabled.on-disabled}` | `color` | `--service-button-disabled-label` |
| `service-button.disabled.badgeOpacity` | `0.3` | `number` | `--service-button-disabled-badge-opacity` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/service-button.tokens.json</code></summary>

```json
{
  "service-button": {
    "container": {
      "height": { "$value": "{spacing.48}", "$type": "dimension" },
      "minWidth": { "$value": "72px", "$type": "dimension" },
      "paddingInlineStart": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingInlineEnd": { "$value": "{spacing.20}", "$type": "dimension" },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.8}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "badge": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" }
    },
    "primary": {
      "background": {
        "default": { "$value": "{color.background.brand.default}", "$type": "color" },
        "hover": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
        "active": { "$value": "{color.background.brand.default-active}", "$type": "color" }
      },
      "label": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" }
    },
    "neutral": {
      "background": {
        "default": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.background.base.tertiary-hover}", "$type": "color" },
        "active": { "$value": "{color.background.base.tertiary-active}", "$type": "color" }
      },
      "label": { "$value": "{color.text.base.default}", "$type": "color" }
    },
    "disabled": {
      "background": { "$value": "{color.background.disabled.default}", "$type": "color" },
      "label": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" },
      "badgeOpacity": { "$value": "0.3", "$type": "number" }
    }
  }
}
```

</details>

#### `tokens/core/components/sidebar.tokens.json`

_52 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `sidebar.container.background` | `{color.background.base.default}` | `color` | `--sidebar-container-background` |
| `sidebar.container.borderColor` | `{color.border.base.default}` | `color` | `--sidebar-container-border-color` |
| `sidebar.container.borderWidth` | `{borderWidth.1}` | `dimension` | `--sidebar-container-border-width` |
| `sidebar.container.borderRadius` | `{borderRadius.16}` | `dimension` | `--sidebar-container-border-radius` |
| `sidebar.container.inlineSize` | `368px` | `dimension` | `--sidebar-container-inline-size` |
| `sidebar.container.collapsedInlineSize` | `68px` | `dimension` | `--sidebar-container-collapsed-inline-size` |
| `sidebar.section.paddingBlock` | `{spacing.32}` | `dimension` | `--sidebar-section-padding-block` |
| `sidebar.section.gap` | `{spacing.12}` | `dimension` | `--sidebar-section-gap` |
| `sidebar.heading.paddingInline` | `{spacing.24}` | `dimension` | `--sidebar-heading-padding-inline` |
| `sidebar.heading.separatorGap` | `{spacing.20}` | `dimension` | `--sidebar-heading-separator-gap` |
| `sidebar.heading.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--sidebar-heading-font-family` |
| `sidebar.heading.fontSize` | `{fontSize.14}` | `dimension` | `--sidebar-heading-font-size` |
| `sidebar.heading.lineHeight` | `{lineHeight.20}` | `dimension` | `--sidebar-heading-line-height` |
| `sidebar.heading.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--sidebar-heading-font-weight` |
| `sidebar.heading.color` | `{color.text.base.tertiary}` | `color` | `--sidebar-heading-color` |
| `sidebar.item.minBlockSize` | `{spacing.48}` | `dimension` | `--sidebar-item-min-block-size` |
| `sidebar.item.paddingInlineStart` | `{spacing.24}` | `dimension` | `--sidebar-item-padding-inline-start` |
| `sidebar.item.paddingInlineEnd` | `{spacing.16}` | `dimension` | `--sidebar-item-padding-inline-end` |
| `sidebar.item.paddingBlock` | `{spacing.12}` | `dimension` | `--sidebar-item-padding-block` |
| `sidebar.item.gap` | `{spacing.12}` | `dimension` | `--sidebar-item-gap` |
| `sidebar.item.iconSize` | `{spacing.20}` | `dimension` | `--sidebar-item-icon-size` |
| `sidebar.item.chevronSize` | `{spacing.24}` | `dimension` | `--sidebar-item-chevron-size` |
| `sidebar.item.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--sidebar-item-font-family` |
| `sidebar.item.fontSize` | `{fontSize.16}` | `dimension` | `--sidebar-item-font-size` |
| `sidebar.item.lineHeight` | `{lineHeight.24}` | `dimension` | `--sidebar-item-line-height` |
| `sidebar.item.fontWeight.default` | `{fontWeight.regular}` | `fontWeight` | `--sidebar-item-font-weight-default` |
| `sidebar.item.fontWeight.active` | `{fontWeight.medium}` | `fontWeight` | `--sidebar-item-font-weight-active` |
| `sidebar.item.activeBorderWidth` | `{borderWidth.3}` | `dimension` | `--sidebar-item-active-border-width` |
| `sidebar.item.activeBorderColor` | `{color.border.brand.default}` | `color` | `--sidebar-item-active-border-color` |
| `sidebar.item.background.default` | `{color.background.base.default}` | `color` | `--sidebar-item-background-default` |
| `sidebar.item.background.hover` | `{color.background.base.default-hover}` | `color` | `--sidebar-item-background-hover` |
| `sidebar.item.background.active` | `{color.background.brand.secondary}` | `color` | `--sidebar-item-background-active` |
| `sidebar.item.text.default` | `{color.text.base.secondary}` | `color` | `--sidebar-item-text-default` |
| `sidebar.item.text.active` | `{color.text.brand.default}` | `color` | `--sidebar-item-text-active` |
| `sidebar.item.text.disabled` | `{color.text.disabled.default}` | `color` | `--sidebar-item-text-disabled` |
| `sidebar.item.icon.default` | `{color.icon.base.default}` | `color` | `--sidebar-item-icon-default` |
| `sidebar.item.icon.active` | `{color.icon.brand.default}` | `color` | `--sidebar-item-icon-active` |
| `sidebar.item.icon.disabled` | `{color.icon.disabled.default}` | `color` | `--sidebar-item-icon-disabled` |
| `sidebar.item.secondary.fontSize` | `{fontSize.14}` | `dimension` | `--sidebar-item-secondary-font-size` |
| `sidebar.item.secondary.lineHeight` | `{lineHeight.20}` | `dimension` | `--sidebar-item-secondary-line-height` |
| `sidebar.item.secondary.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--sidebar-item-secondary-font-weight` |
| `sidebar.item.secondary.color` | `{color.text.base.tertiary}` | `color` | `--sidebar-item-secondary-color` |
| `sidebar.item.badge.minSize` | `{spacing.24}` | `dimension` | `--sidebar-item-badge-min-size` |
| `sidebar.item.badge.paddingInline` | `{spacing.8}` | `dimension` | `--sidebar-item-badge-padding-inline` |
| `sidebar.item.badge.borderRadius` | `{borderRadius.full}` | `dimension` | `--sidebar-item-badge-border-radius` |
| `sidebar.item.badge.background` | `{color.background.base.tertiary}` | `color` | `--sidebar-item-badge-background` |
| `sidebar.item.badge.color` | `{color.text.base.default}` | `color` | `--sidebar-item-badge-color` |
| `sidebar.item.badge.fontSize` | `{fontSize.14}` | `dimension` | `--sidebar-item-badge-font-size` |
| `sidebar.item.badge.lineHeight` | `{lineHeight.20}` | `dimension` | `--sidebar-item-badge-line-height` |
| `sidebar.item.badge.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--sidebar-item-badge-font-weight` |
| `sidebar.focusRing.color` | `{palette.blue-sky.500}` | `color` | `--sidebar-focus-ring-color` |
| `sidebar.focusRing.width` | `{borderWidth.2}` | `dimension` | `--sidebar-focus-ring-width` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/sidebar.tokens.json</code></summary>

```json
{
  "sidebar": {
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "borderRadius": {
        "$value": "{borderRadius.16}",
        "$type": "dimension",
        "$comment": "Figma sidebar 1236:6302 uses border-radius/16."
      },
      "inlineSize": {
        "$value": "368px",
        "$type": "dimension",
        "$comment": "Figma sidebar fixed width 368px (expanded)."
      },
      "collapsedInlineSize": {
        "$value": "68px",
        "$type": "dimension",
        "$comment": "Figma sidebar-mpower Type=Compact width 68px (icon-only)."
      }
    },
    "section": {
      "paddingBlock": {
        "$value": "{spacing.32}",
        "$type": "dimension",
        "$comment": "Figma sidebar section container uses 32px top/bottom padding."
      },
      "gap": {
        "$value": "{spacing.12}",
        "$type": "dimension",
        "$comment": "Figma gap between a section heading block and its list."
      }
    },
    "heading": {
      "paddingInline": { "$value": "{spacing.24}", "$type": "dimension" },
      "separatorGap": {
        "$value": "{spacing.20}",
        "$type": "dimension",
        "$comment": "Figma gap between the section separator and the heading label."
      },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "color": {
        "$value": "{color.text.base.tertiary}",
        "$type": "color",
        "$comment": "Figma section heading label #757575."
      }
    },
    "item": {
      "minBlockSize": {
        "$value": "{spacing.48}",
        "$type": "dimension",
        "$comment": "Figma sidebar-list-item height 48px."
      },
      "paddingInlineStart": { "$value": "{spacing.24}", "$type": "dimension" },
      "paddingInlineEnd": { "$value": "{spacing.16}", "$type": "dimension" },
      "paddingBlock": { "$value": "{spacing.12}", "$type": "dimension" },
      "gap": { "$value": "{spacing.12}", "$type": "dimension" },
      "iconSize": {
        "$value": "{spacing.20}",
        "$type": "dimension",
        "$comment": "Figma leading icon 20px."
      },
      "chevronSize": {
        "$value": "{spacing.24}",
        "$type": "dimension",
        "$comment": "Figma expandable chevron 24px."
      },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.16}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.24}", "$type": "dimension" },
      "fontWeight": {
        "default": {
          "$value": "{fontWeight.regular}",
          "$type": "fontWeight",
          "$comment": "Figma default item label Onest Regular (400)."
        },
        "active": {
          "$value": "{fontWeight.medium}",
          "$type": "fontWeight",
          "$comment": "Figma active item label Onest Medium (500)."
        }
      },
      "activeBorderWidth": {
        "$value": "{borderWidth.3}",
        "$type": "dimension",
        "$comment": "Figma active item left rail 3px."
      },
      "activeBorderColor": { "$value": "{color.border.brand.default}", "$type": "color" },
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "hover": {
          "$value": "{color.background.base.default-hover}",
          "$type": "color",
          "$comment": "Figma sidebar-list-item Hover bg #f5f5f5."
        },
        "active": {
          "$value": "{color.background.brand.secondary}",
          "$type": "color",
          "$comment": "Figma sidebar-list-item Active bg #e8f0fb."
        }
      },
      "text": {
        "default": {
          "$value": "{color.text.base.secondary}",
          "$type": "color",
          "$comment": "Figma item label #383838."
        },
        "active": {
          "$value": "{color.text.brand.default}",
          "$type": "color",
          "$comment": "Figma active item label #0058d2."
        },
        "disabled": {
          "$value": "{color.text.disabled.default}",
          "$type": "color",
          "$comment": "Disabled nav item (functional addition; not a Figma-specified state). Disabled text exempt from WCAG 1.4.3."
        }
      },
      "icon": {
        "default": {
          "$value": "{color.icon.base.default}",
          "$type": "color",
          "$comment": "Figma leading icon #121212."
        },
        "active": {
          "$value": "{color.icon.brand.default}",
          "$type": "color",
          "$comment": "Figma active leading icon #0058d2 (filled glyph)."
        },
        "disabled": { "$value": "{color.icon.disabled.default}", "$type": "color" }
      },
      "secondary": {
        "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
        "color": {
          "$value": "{color.text.base.tertiary}",
          "$type": "color",
          "$comment": "Figma secondary label #757575, right-aligned."
        }
      },
      "badge": {
        "minSize": { "$value": "{spacing.24}", "$type": "dimension" },
        "paddingInline": { "$value": "{spacing.8}", "$type": "dimension" },
        "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
        "background": {
          "$value": "{color.background.base.tertiary}",
          "$type": "color",
          "$comment": "Figma numbered-badge bg #f1f1f1 (neutral grey, not the dark mud-badge default)."
        },
        "color": {
          "$value": "{color.text.base.default}",
          "$type": "color",
          "$comment": "Figma numbered-badge text #121212."
        },
        "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
        "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
      }
    },
    "focusRing": {
      "color": {
        "$value": "{palette.blue-sky.500}",
        "$type": "color",
        "$comment": "Inset keyboard focus ring, matching the project focus treatment; reconcile in pixel QA against the hidden Figma Focus state."
      },
      "width": { "$value": "{borderWidth.2}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/spinner.tokens.json`

_14 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `spinner.size.xs` | `{spacing.16}` | `dimension` | `--spinner-size-xs` |
| `spinner.size.sm` | `{spacing.20}` | `dimension` | `--spinner-size-sm` |
| `spinner.size.md` | `{spacing.32}` | `dimension` | `--spinner-size-md` |
| `spinner.size.lg` | `{spacing.40}` | `dimension` | `--spinner-size-lg` |
| `spinner.stroke.width.xs` | `{spacing.2}` | `dimension` | `--spinner-stroke-width-xs` |
| `spinner.stroke.width.sm` | `{spacing.2}` | `dimension` | `--spinner-stroke-width-sm` |
| `spinner.stroke.width.md` | `3px` | `dimension` | `--spinner-stroke-width-md` |
| `spinner.stroke.width.lg` | `{spacing.4}` | `dimension` | `--spinner-stroke-width-lg` |
| `spinner.arc.color.brand` | `{color.background.brand.default}` | `color` | `--spinner-arc-color-brand` |
| `spinner.arc.color.dark` | `{color.background.base-inverse.default}` | `color` | `--spinner-arc-color-dark` |
| `spinner.arc.color.light` | `{color.text.base-inverse.default}` | `color` | `--spinner-arc-color-light` |
| `spinner.arc.color.light-on-color` | `{color.text.base-inverse.on-color}` | `color` | `--spinner-arc-color-light-on-color` |
| `spinner.transitionDuration` | `800ms` | `duration` | `--spinner-transition-duration` |
| `spinner.transitionTimingFunction` | `linear` | `cubicBezier` | `--spinner-transition-timing-function` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/spinner.tokens.json</code></summary>

```json
{
  "spinner": {
    "size": {
      "xs": {
        "$value": "{spacing.16}",
        "$type": "dimension"
      },
      "sm": {
        "$value": "{spacing.20}",
        "$type": "dimension"
      },
      "md": {
        "$value": "{spacing.32}",
        "$type": "dimension"
      },
      "lg": {
        "$value": "{spacing.40}",
        "$type": "dimension"
      }
    },
    "stroke": {
      "width": {
        "xs": {
          "$value": "{spacing.2}",
          "$type": "dimension"
        },
        "sm": {
          "$value": "{spacing.2}",
          "$type": "dimension"
        },
        "md": {
          "$value": "3px",
          "$type": "dimension"
        },
        "lg": {
          "$value": "{spacing.4}",
          "$type": "dimension"
        }
      }
    },
    "arc": {
      "color": {
        "brand": {
          "$value": "{color.background.brand.default}",
          "$type": "color"
        },
        "dark": {
          "$value": "{color.background.base-inverse.default}",
          "$type": "color"
        },
        "light": {
          "$value": "{color.text.base-inverse.default}",
          "$type": "color"
        },
        "light-on-color": {
          "$value": "{color.text.base-inverse.on-color}",
          "$type": "color"
        }
      }
    },
    "transitionDuration": {
      "$value": "800ms",
      "$type": "duration"
    },
    "transitionTimingFunction": {
      "$value": "linear",
      "$type": "cubicBezier"
    }
  }
}
```

</details>

#### `tokens/core/components/switch.tokens.json`

_29 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `switch.track.width` | `{spacing.48}` | `dimension` | `--switch-track-width` |
| `switch.track.height` | `28px` | `dimension` | `--switch-track-height` |
| `switch.track.borderRadius` | `{borderRadius.full}` | `dimension` | `--switch-track-border-radius` |
| `switch.track.padding` | `{spacing.2}` | `dimension` | `--switch-track-padding` |
| `switch.track.transitionDuration` | `150ms` | `duration` | `--switch-track-transition-duration` |
| `switch.track.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--switch-track-transition-timing-function` |
| `switch.thumb.size` | `{spacing.24}` | `dimension` | `--switch-thumb-size` |
| `switch.thumb.borderRadius` | `{borderRadius.full}` | `dimension` | `--switch-thumb-border-radius` |
| `switch.thumb.color.default` | `{color.background.base.default}` | `color` | `--switch-thumb-color-default` |
| `switch.thumb.color.disabled` | `{color.background.base.default}` | `color` | `--switch-thumb-color-disabled` |
| `switch.thumb.shadow` | `0 0 0.5px 0 rgba(0, 0, 0, 0.3), 0 1px 3px 0 rgba(0, 0, 0, 0.16)` | `shadow` | `--switch-thumb-shadow` |
| `switch.background.off` | `{palette.gray.400}` | `color` | `--switch-background-off` |
| `switch.background.on` | `{color.background.brand.default}` | `color` | `--switch-background-on` |
| `switch.background.offHover` | `{palette.gray.500}` | `color` | `--switch-background-off-hover` |
| `switch.background.onHover` | `{color.background.brand.default-hover}` | `color` | `--switch-background-on-hover` |
| `switch.background.disabled` | `{color.background.disabled.default}` | `color` | `--switch-background-disabled` |
| `switch.focusRing.innerWidth` | `{borderWidth.1}` | `dimension` | `--switch-focus-ring-inner-width` |
| `switch.focusRing.outerWidth` | `{focusRing.width.outer}` | `dimension` | `--switch-focus-ring-outer-width` |
| `switch.focusRing.innerColor` | `{focusRing.color.inner}` | `color` | `--switch-focus-ring-inner-color` |
| `switch.focusRing.outerColor` | `{focusRing.color.outer}` | `color` | `--switch-focus-ring-outer-color` |
| `switch.touchTarget.pointer` | `{spacing.32}` | `dimension` | `--switch-touch-target-pointer` |
| `switch.touchTarget.touch` | `{spacing.40}` | `dimension` | `--switch-touch-target-touch` |
| `switch.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--switch-label-font-family` |
| `switch.label.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--switch-label-font-weight` |
| `switch.label.fontSize` | `{fontSize.14}` | `dimension` | `--switch-label-font-size` |
| `switch.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--switch-label-line-height` |
| `switch.label.color.default` | `{color.text.base.default}` | `color` | `--switch-label-color-default` |
| `switch.label.color.disabled` | `{color.text.disabled.default}` | `color` | `--switch-label-color-disabled` |
| `switch.gap.switchLabel` | `{spacing.12}` | `dimension` | `--switch-gap-switch-label` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/switch.tokens.json</code></summary>

```json
{
  "switch": {
    "track": {
      "width": { "$value": "{spacing.48}", "$type": "dimension" },
      "height": { "$value": "28px", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "padding": { "$value": "{spacing.2}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "thumb": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.background.base.default}", "$type": "color" }
      },
      "shadow": {
        "$value": "0 0 0.5px 0 rgba(0, 0, 0, 0.3), 0 1px 3px 0 rgba(0, 0, 0, 0.16)",
        "$type": "shadow"
      }
    },
    "background": {
      "$comment": "off/offHover use palette.gray.400 (#757575, ~4.8:1 on white) because the semantic tier has no background token in the gray.350-500 range that satisfies WCAG 1.4.11 against the page (and against the white thumb). tracked in docs/backlog/2026-05-27-text-brand-hover-active-semantic-gap.md as the broader semantic-tier gap.",
      "off": { "$value": "{palette.gray.400}", "$type": "color" },
      "on": { "$value": "{color.background.brand.default}", "$type": "color" },
      "offHover": { "$value": "{palette.gray.500}", "$type": "color" },
      "onHover": { "$value": "{color.background.brand.default-hover}", "$type": "color" },
      "disabled": { "$value": "{color.background.disabled.default}", "$type": "color" }
    },
    "focusRing": {
      "innerWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "outerWidth": { "$value": "{focusRing.width.outer}", "$type": "dimension" },
      "innerColor": { "$value": "{focusRing.color.inner}", "$type": "color" },
      "outerColor": { "$value": "{focusRing.color.outer}", "$type": "color" }
    },
    "touchTarget": {
      "pointer": { "$value": "{spacing.32}", "$type": "dimension" },
      "touch": { "$value": "{spacing.40}", "$type": "dimension" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.default}", "$type": "color" }
      }
    },
    "gap": {
      "switchLabel": { "$value": "{spacing.12}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/table.tokens.json`

_49 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `table.container.background` | `{color.background.base.default}` | `color` | `--table-container-background` |
| `table.container.borderRadius` | `{borderRadius.0}` | `dimension` | `--table-container-border-radius` |
| `table.container.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--table-container-font-family` |
| `table.header.minHeight` | `{spacing.48}` | `dimension` | `--table-header-min-height` |
| `table.header.paddingInline.desktop` | `{spacing.24}` | `dimension` | `--table-header-padding-inline-desktop` |
| `table.header.paddingInline.mobile` | `{spacing.16}` | `dimension` | `--table-header-padding-inline-mobile` |
| `table.header.paddingBlock` | `{spacing.8}` | `dimension` | `--table-header-padding-block` |
| `table.header.gap` | `{spacing.6}` | `dimension` | `--table-header-gap` |
| `table.header.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--table-header-font-weight` |
| `table.header.fontSize` | `{fontSize.14}` | `dimension` | `--table-header-font-size` |
| `table.header.lineHeight` | `{lineHeight.20}` | `dimension` | `--table-header-line-height` |
| `table.header.default.background` | `{color.background.base.tertiary}` | `color` | `--table-header-default-background` |
| `table.header.default.color` | `{color.text.base.default}` | `color` | `--table-header-default-color` |
| `table.header.default.borderBottom` | `{color.border.base.default}` | `color` | `--table-header-default-border-bottom` |
| `table.header.inverted.background` | `{color.background.base-inverse.default}` | `color` | `--table-header-inverted-background` |
| `table.header.inverted.color` | `{color.text.base-inverse.default}` | `color` | `--table-header-inverted-color` |
| `table.header.inverted.borderBottom` | `{color.background.base-inverse.default}` | `color` | `--table-header-inverted-border-bottom` |
| `table.header.white.background` | `{color.background.base.default}` | `color` | `--table-header-white-background` |
| `table.header.white.color` | `{color.text.base.default}` | `color` | `--table-header-white-color` |
| `table.header.white.borderBottom` | `{color.border.base.default}` | `color` | `--table-header-white-border-bottom` |
| `table.cell.minHeight` | `{spacing.48}` | `dimension` | `--table-cell-min-height` |
| `table.cell.paddingInline.desktop` | `{spacing.24}` | `dimension` | `--table-cell-padding-inline-desktop` |
| `table.cell.paddingInline.mobile` | `{spacing.16}` | `dimension` | `--table-cell-padding-inline-mobile` |
| `table.cell.paddingBlock` | `{spacing.8}` | `dimension` | `--table-cell-padding-block` |
| `table.cell.gap` | `{spacing.6}` | `dimension` | `--table-cell-gap` |
| `table.cell.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--table-cell-font-weight` |
| `table.cell.fontSize` | `{fontSize.14}` | `dimension` | `--table-cell-font-size` |
| `table.cell.lineHeight` | `{lineHeight.20}` | `dimension` | `--table-cell-line-height` |
| `table.cell.color` | `{color.text.base.secondary}` | `color` | `--table-cell-color` |
| `table.cell.borderBottom` | `{color.border.base.default}` | `color` | `--table-cell-border-bottom` |
| `table.row.background.default` | `{color.background.base.default}` | `color` | `--table-row-background-default` |
| `table.row.background.zebra` | `{color.background.base.secondary}` | `color` | `--table-row-background-zebra` |
| `table.row.background.hover` | `{color.background.base.secondary}` | `color` | `--table-row-background-hover` |
| `table.row.background.selected` | `{color.background.brand.secondary}` | `color` | `--table-row-background-selected` |
| `table.row.background.selectedHover` | `{color.background.brand.secondary-hover}` | `color` | `--table-row-background-selected-hover` |
| `table.row.transitionDuration` | `120ms` | `duration` | `--table-row-transition-duration` |
| `table.row.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--table-row-transition-timing-function` |
| `table.sortIcon.size` | `{spacing.16}` | `dimension` | `--table-sort-icon-size` |
| `table.sortIcon.color.inactive` | `{color.icon.base.tertiary}` | `color` | `--table-sort-icon-color-inactive` |
| `table.sortIcon.color.active` | `{color.icon.base.default}` | `color` | `--table-sort-icon-color-active` |
| `table.sortIcon.color.inverted` | `{color.text.base-inverse.default}` | `color` | `--table-sort-icon-color-inverted` |
| `table.focusRing.width` | `{borderWidth.3}` | `dimension` | `--table-focus-ring-width` |
| `table.focusRing.offset` | `{borderWidth.1}` | `dimension` | `--table-focus-ring-offset` |
| `table.focusRing.color` | `{color.background.brand.focus-ring}` | `color` | `--table-focus-ring-color` |
| `table.selection.columnWidth` | `{spacing.56}` | `dimension` | `--table-selection-column-width` |
| `table.mobile.breakpoint` | `640px` | `dimension` | `--table-mobile-breakpoint` |
| `table.empty.padding` | `{spacing.48}` | `dimension` | `--table-empty-padding` |
| `table.empty.color` | `{color.text.base.tertiary}` | `color` | `--table-empty-color` |
| `table.empty.fontSize` | `{fontSize.14}` | `dimension` | `--table-empty-font-size` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/table.tokens.json</code></summary>

```json
{
  "table": {
    "container": {
      "background": { "$value": "{color.background.base.default}", "$type": "color" },
      "borderRadius": { "$value": "{borderRadius.0}", "$type": "dimension" },
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" }
    },
    "header": {
      "minHeight": { "$value": "{spacing.48}", "$type": "dimension" },
      "paddingInline": {
        "desktop": {
          "$value": "{spacing.24}",
          "$type": "dimension",
          "$comment": "Per Figma table-header master (4930:14358) Desktop breakpoint."
        },
        "mobile": {
          "$value": "{spacing.16}",
          "$type": "dimension",
          "$comment": "Per Figma table-header master (4930:14358) Mobile breakpoint."
        }
      },
      "paddingBlock": { "$value": "{spacing.8}", "$type": "dimension" },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "default": {
        "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "color": { "$value": "{color.text.base.default}", "$type": "color" },
        "borderBottom": { "$value": "{color.border.base.default}", "$type": "color" }
      },
      "inverted": {
        "background": { "$value": "{color.background.base-inverse.default}", "$type": "color" },
        "color": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
        "borderBottom": { "$value": "{color.background.base-inverse.default}", "$type": "color" }
      },
      "white": {
        "background": {
          "$value": "{color.background.base.default}",
          "$type": "color",
          "$comment": "Per Figma 4946:23845 (Header Styles / white) — header background matches body. The bottom border is the only delimiter."
        },
        "color": { "$value": "{color.text.base.default}", "$type": "color" },
        "borderBottom": { "$value": "{color.border.base.default}", "$type": "color" }
      }
    },
    "cell": {
      "minHeight": { "$value": "{spacing.48}", "$type": "dimension" },
      "paddingInline": {
        "desktop": {
          "$value": "{spacing.24}",
          "$type": "dimension",
          "$comment": "Per Figma table-cell master (649:4296) Desktop breakpoint."
        },
        "mobile": {
          "$value": "{spacing.16}",
          "$type": "dimension",
          "$comment": "Per Figma table-cell master (649:4296) Mobile breakpoint."
        }
      },
      "paddingBlock": { "$value": "{spacing.8}", "$type": "dimension" },
      "gap": { "$value": "{spacing.6}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "color": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "borderBottom": { "$value": "{color.border.base.default}", "$type": "color" }
    },
    "row": {
      "background": {
        "default": { "$value": "{color.background.base.default}", "$type": "color" },
        "zebra": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "hover": { "$value": "{color.background.base.secondary}", "$type": "color" },
        "selected": { "$value": "{color.background.brand.secondary}", "$type": "color" },
        "selectedHover": { "$value": "{color.background.brand.secondary-hover}", "$type": "color" }
      },
      "transitionDuration": { "$value": "120ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "sortIcon": {
      "size": { "$value": "{spacing.16}", "$type": "dimension" },
      "color": {
        "inactive": { "$value": "{color.icon.base.tertiary}", "$type": "color" },
        "active": { "$value": "{color.icon.base.default}", "$type": "color" },
        "inverted": { "$value": "{color.text.base-inverse.default}", "$type": "color" }
      }
    },
    "focusRing": {
      "width": { "$value": "{borderWidth.3}", "$type": "dimension" },
      "offset": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "color": { "$value": "{color.background.brand.focus-ring}", "$type": "color" }
    },
    "selection": {
      "columnWidth": {
        "$value": "{spacing.56}",
        "$type": "dimension",
        "$comment": "Fixed width of the leading checkbox column when `selectable` is set. 56px = 16+24+16 (paddingInline + checkbox box + paddingInline)."
      }
    },
    "mobile": {
      "breakpoint": {
        "$value": "640px",
        "$type": "dimension",
        "$comment": "Below this container-query width the table inline padding shrinks per Figma's Mobile breakpoint (24 → 16). Documented as a token for single-source-of-truth; container queries can't read CSS variables, so the literal lives in the CSS file as well."
      }
    },
    "empty": {
      "padding": { "$value": "{spacing.48}", "$type": "dimension" },
      "color": { "$value": "{color.text.base.tertiary}", "$type": "color" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" }
    }
  }
}
```

</details>

#### `tokens/core/components/tabs.tokens.json`

_61 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `tabs.container.gap` | `{spacing.0}` | `dimension` | `--tabs-container-gap` |
| `tabs.container.borderColor` | `{color.border.base.default}` | `color` | `--tabs-container-border-color` |
| `tabs.container.borderWidth` | `{borderWidth.1}` | `dimension` | `--tabs-container-border-width` |
| `tabs.container.transitionDuration` | `150ms` | `duration` | `--tabs-container-transition-duration` |
| `tabs.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--tabs-container-transition-timing-function` |
| `tabs.tab.height.md` | `{spacing.48}` | `dimension` | `--tabs-tab-height-md` |
| `tabs.tab.height.sm` | `{spacing.40}` | `dimension` | `--tabs-tab-height-sm` |
| `tabs.tab.paddingInline.md` | `{spacing.16}` | `dimension` | `--tabs-tab-padding-inline-md` |
| `tabs.tab.paddingInline.sm` | `{spacing.12}` | `dimension` | `--tabs-tab-padding-inline-sm` |
| `tabs.tab.gap` | `{spacing.8}` | `dimension` | `--tabs-tab-gap` |
| `tabs.tab.innerGap` | `{spacing.6}` | `dimension` | `--tabs-tab-inner-gap` |
| `tabs.tab.indicatorHeight` | `{borderWidth.2}` | `dimension` | `--tabs-tab-indicator-height` |
| `tabs.tab.indicatorColor` | `{color.border.brand.default}` | `color` | `--tabs-tab-indicator-color` |
| `tabs.tab.minWidth` | `{spacing.40}` | `dimension` | `--tabs-tab-min-width` |
| `tabs.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--tabs-label-font-family` |
| `tabs.label.fontSize.md` | `{fontSize.16}` | `dimension` | `--tabs-label-font-size-md` |
| `tabs.label.fontSize.sm` | `{fontSize.14}` | `dimension` | `--tabs-label-font-size-sm` |
| `tabs.label.lineHeight.md` | `{lineHeight.24}` | `dimension` | `--tabs-label-line-height-md` |
| `tabs.label.lineHeight.sm` | `{lineHeight.20}` | `dimension` | `--tabs-label-line-height-sm` |
| `tabs.label.fontWeightSelected` | `{fontWeight.medium}` | `fontWeight` | `--tabs-label-font-weight-selected` |
| `tabs.label.fontWeightUnselected` | `{fontWeight.regular}` | `fontWeight` | `--tabs-label-font-weight-unselected` |
| `tabs.selected.color` | `{color.text.brand.default}` | `color` | `--tabs-selected-color` |
| `tabs.selected.iconColor` | `{color.icon.brand.default}` | `color` | `--tabs-selected-icon-color` |
| `tabs.unselected.color` | `{color.text.base.secondary}` | `color` | `--tabs-unselected-color` |
| `tabs.unselected.colorHover` | `{color.text.base.default}` | `color` | `--tabs-unselected-color-hover` |
| `tabs.unselected.iconColor` | `{color.icon.base.secondary}` | `color` | `--tabs-unselected-icon-color` |
| `tabs.unselected.iconColorHover` | `{color.icon.base.default}` | `color` | `--tabs-unselected-icon-color-hover` |
| `tabs.disabled.color` | `{color.text.disabled.default}` | `color` | `--tabs-disabled-color` |
| `tabs.disabled.iconColor` | `{color.icon.disabled.default}` | `color` | `--tabs-disabled-icon-color` |
| `tabs.icon.size.md` | `{spacing.20}` | `dimension` | `--tabs-icon-size-md` |
| `tabs.icon.size.sm` | `{spacing.20}` | `dimension` | `--tabs-icon-size-sm` |
| `tabs.badge.background` | `{color.background.base.tertiary}` | `color` | `--tabs-badge-background` |
| `tabs.badge.color` | `{color.text.base.default}` | `color` | `--tabs-badge-color` |
| `tabs.badge.minSize.md` | `{spacing.24}` | `dimension` | `--tabs-badge-min-size-md` |
| `tabs.badge.minSize.sm` | `{spacing.20}` | `dimension` | `--tabs-badge-min-size-sm` |
| `tabs.badge.paddingInline.md` | `{spacing.8}` | `dimension` | `--tabs-badge-padding-inline-md` |
| `tabs.badge.paddingInline.sm` | `{spacing.4}` | `dimension` | `--tabs-badge-padding-inline-sm` |
| `tabs.badge.borderRadius` | `{borderRadius.full}` | `dimension` | `--tabs-badge-border-radius` |
| `tabs.badge.fontSize.md` | `{fontSize.14}` | `dimension` | `--tabs-badge-font-size-md` |
| `tabs.badge.fontSize.sm` | `{fontSize.12}` | `dimension` | `--tabs-badge-font-size-sm` |
| `tabs.badge.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--tabs-badge-line-height-md` |
| `tabs.badge.lineHeight.sm` | `{lineHeight.16}` | `dimension` | `--tabs-badge-line-height-sm` |
| `tabs.badge.fontWeight` | `{fontWeight.medium}` | `fontWeight` | `--tabs-badge-font-weight` |
| `tabs.overflow.fadeWidth.md` | `{spacing.48}` | `dimension` | `--tabs-overflow-fade-width-md` |
| `tabs.overflow.fadeWidth.sm` | `{spacing.32}` | `dimension` | `--tabs-overflow-fade-width-sm` |
| `tabs.overflow.chevron.size.md` | `{spacing.24}` | `dimension` | `--tabs-overflow-chevron-size-md` |
| `tabs.overflow.chevron.size.sm` | `{spacing.20}` | `dimension` | `--tabs-overflow-chevron-size-sm` |
| `tabs.overflow.chevron.color` | `{color.icon.base.secondary}` | `color` | `--tabs-overflow-chevron-color` |
| `tabs.overflow.chevron.colorHover` | `{color.icon.base.default}` | `color` | `--tabs-overflow-chevron-color-hover` |
| `tabs.overflow.chevron.colorDisabled` | `{color.icon.disabled.default}` | `color` | `--tabs-overflow-chevron-color-disabled` |
| `tabs.overflow.chevron.background` | `{color.background.base.default}` | `color` | `--tabs-overflow-chevron-background` |
| `tabs.overflow.chevron.paddingBlock.md` | `{spacing.12}` | `dimension` | `--tabs-overflow-chevron-padding-block-md` |
| `tabs.overflow.chevron.paddingBlock.sm` | `{spacing.8}` | `dimension` | `--tabs-overflow-chevron-padding-block-sm` |
| `tabs.overflow.chevron.paddingInline.md` | `{spacing.4}` | `dimension` | `--tabs-overflow-chevron-padding-inline-md` |
| `tabs.overflow.chevron.paddingInline.sm` | `{spacing.4}` | `dimension` | `--tabs-overflow-chevron-padding-inline-sm` |
| `tabs.panel.paddingBlock` | `{spacing.16}` | `dimension` | `--tabs-panel-padding-block` |
| `tabs.panel.color` | `{color.text.base.default}` | `color` | `--tabs-panel-color` |
| `tabs.focusRing.innerWidth` | `{focusRing.width.inner}` | `dimension` | `--tabs-focus-ring-inner-width` |
| `tabs.focusRing.outerWidth` | `{focusRing.width.outer}` | `dimension` | `--tabs-focus-ring-outer-width` |
| `tabs.focusRing.innerColor` | `{focusRing.color.inner}` | `color` | `--tabs-focus-ring-inner-color` |
| `tabs.focusRing.outerColor` | `{focusRing.color.outer}` | `color` | `--tabs-focus-ring-outer-color` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/tabs.tokens.json</code></summary>

```json
{
  "tabs": {
    "container": {
      "gap": { "$value": "{spacing.0}", "$type": "dimension" },
      "borderColor": { "$value": "{color.border.base.default}", "$type": "color" },
      "borderWidth": { "$value": "{borderWidth.1}", "$type": "dimension" },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" }
    },
    "tab": {
      "height": {
        "md": { "$value": "{spacing.48}", "$type": "dimension" },
        "sm": { "$value": "{spacing.40}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "sm": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "gap": { "$value": "{spacing.8}", "$type": "dimension" },
      "innerGap": { "$value": "{spacing.6}", "$type": "dimension" },
      "indicatorHeight": { "$value": "{borderWidth.2}", "$type": "dimension" },
      "indicatorColor": { "$value": "{color.border.brand.default}", "$type": "color" },
      "minWidth": { "$value": "{spacing.40}", "$type": "dimension" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.16}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.14}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.24}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.20}", "$type": "dimension" }
      },
      "fontWeightSelected": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontWeightUnselected": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "selected": {
      "color": { "$value": "{color.text.brand.default}", "$type": "color" },
      "iconColor": { "$value": "{color.icon.brand.default}", "$type": "color" }
    },
    "unselected": {
      "color": { "$value": "{color.text.base.secondary}", "$type": "color" },
      "colorHover": { "$value": "{color.text.base.default}", "$type": "color" },
      "iconColor": { "$value": "{color.icon.base.secondary}", "$type": "color" },
      "iconColorHover": { "$value": "{color.icon.base.default}", "$type": "color" }
    },
    "disabled": {
      "color": { "$value": "{color.text.disabled.default}", "$type": "color" },
      "iconColor": { "$value": "{color.icon.disabled.default}", "$type": "color" }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.20}", "$type": "dimension" },
        "sm": { "$value": "{spacing.20}", "$type": "dimension" }
      }
    },
    "badge": {
      "background": { "$value": "{color.background.base.tertiary}", "$type": "color" },
      "color": { "$value": "{color.text.base.default}", "$type": "color" },
      "minSize": {
        "md": { "$value": "{spacing.24}", "$type": "dimension" },
        "sm": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "sm": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.full}", "$type": "dimension" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.12}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.16}", "$type": "dimension" }
      },
      "fontWeight": { "$value": "{fontWeight.medium}", "$type": "fontWeight" }
    },
    "overflow": {
      "fadeWidth": {
        "md": { "$value": "{spacing.48}", "$type": "dimension" },
        "sm": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "chevron": {
        "size": {
          "md": { "$value": "{spacing.24}", "$type": "dimension" },
          "sm": { "$value": "{spacing.20}", "$type": "dimension" }
        },
        "color": { "$value": "{color.icon.base.secondary}", "$type": "color" },
        "colorHover": { "$value": "{color.icon.base.default}", "$type": "color" },
        "colorDisabled": { "$value": "{color.icon.disabled.default}", "$type": "color" },
        "background": { "$value": "{color.background.base.default}", "$type": "color" },
        "paddingBlock": {
          "md": { "$value": "{spacing.12}", "$type": "dimension" },
          "sm": { "$value": "{spacing.8}", "$type": "dimension" }
        },
        "paddingInline": {
          "md": { "$value": "{spacing.4}", "$type": "dimension" },
          "sm": { "$value": "{spacing.4}", "$type": "dimension" }
        }
      }
    },
    "panel": {
      "paddingBlock": { "$value": "{spacing.16}", "$type": "dimension" },
      "color": { "$value": "{color.text.base.default}", "$type": "color" }
    },
    "focusRing": {
      "innerWidth": { "$value": "{focusRing.width.inner}", "$type": "dimension" },
      "outerWidth": { "$value": "{focusRing.width.outer}", "$type": "dimension" },
      "innerColor": { "$value": "{focusRing.color.inner}", "$type": "color" },
      "outerColor": { "$value": "{focusRing.color.outer}", "$type": "color" }
    }
  }
}
```

</details>

#### `tokens/core/components/tag.tokens.json`

_59 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `tag.container.height.md` | `{spacing.24}` | `dimension` | `--tag-container-height-md` |
| `tag.container.height.sm` | `{spacing.20}` | `dimension` | `--tag-container-height-sm` |
| `tag.container.minWidth.md` | `{spacing.32}` | `dimension` | `--tag-container-min-width-md` |
| `tag.container.minWidth.sm` | `{spacing.32}` | `dimension` | `--tag-container-min-width-sm` |
| `tag.container.paddingInline.md` | `{spacing.8}` | `dimension` | `--tag-container-padding-inline-md` |
| `tag.container.paddingInline.sm` | `{spacing.6}` | `dimension` | `--tag-container-padding-inline-sm` |
| `tag.container.paddingInlineIcon.md` | `{spacing.6}` | `dimension` | `--tag-container-padding-inline-icon-md` |
| `tag.container.paddingInlineIcon.sm` | `{spacing.4}` | `dimension` | `--tag-container-padding-inline-icon-sm` |
| `tag.container.gap.md` | `{spacing.4}` | `dimension` | `--tag-container-gap-md` |
| `tag.container.gap.sm` | `{spacing.4}` | `dimension` | `--tag-container-gap-sm` |
| `tag.container.borderRadius` | `{borderRadius.4}` | `dimension` | `--tag-container-border-radius` |
| `tag.container.borderWidth` | `1px` | `dimension` | `--tag-container-border-width` |
| `tag.container.groupGap` | `{spacing.8}` | `dimension` | `--tag-container-group-gap` |
| `tag.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--tag-label-font-family` |
| `tag.label.fontSize.md` | `{fontSize.14}` | `dimension` | `--tag-label-font-size-md` |
| `tag.label.fontSize.sm` | `{fontSize.12}` | `dimension` | `--tag-label-font-size-sm` |
| `tag.label.lineHeight.md` | `{lineHeight.20}` | `dimension` | `--tag-label-line-height-md` |
| `tag.label.lineHeight.sm` | `{lineHeight.16}` | `dimension` | `--tag-label-line-height-sm` |
| `tag.label.fontWeightStatus` | `{fontWeight.medium}` | `fontWeight` | `--tag-label-font-weight-status` |
| `tag.label.fontWeightInfo` | `{fontWeight.regular}` | `fontWeight` | `--tag-label-font-weight-info` |
| `tag.icon.size.md` | `{spacing.16}` | `dimension` | `--tag-icon-size-md` |
| `tag.icon.size.sm` | `{spacing.12}` | `dimension` | `--tag-icon-size-sm` |
| `tag.subtle.background.muted` | `{color.background.base.default}` | `color` | `--tag-subtle-background-muted` |
| `tag.subtle.background.neutral` | `{color.background.base.tertiary}` | `color` | `--tag-subtle-background-neutral` |
| `tag.subtle.background.accent` | `{color.background.warning.secondary}` | `color` | `--tag-subtle-background-accent` |
| `tag.subtle.background.success` | `{color.background.positive.secondary}` | `color` | `--tag-subtle-background-success` |
| `tag.subtle.background.brand` | `{color.background.brand.secondary}` | `color` | `--tag-subtle-background-brand` |
| `tag.subtle.background.danger` | `{color.background.danger.secondary}` | `color` | `--tag-subtle-background-danger` |
| `tag.subtle.label.muted` | `{color.text.base.tertiary}` | `color` | `--tag-subtle-label-muted` |
| `tag.subtle.label.neutral` | `{color.text.base.default}` | `color` | `--tag-subtle-label-neutral` |
| `tag.subtle.label.accent` | `{color.text.warning.on-secondary}` | `color` | `--tag-subtle-label-accent` |
| `tag.subtle.label.success` | `{color.text.positive.on-secondary}` | `color` | `--tag-subtle-label-success` |
| `tag.subtle.label.brand` | `{color.text.brand.on-secondary}` | `color` | `--tag-subtle-label-brand` |
| `tag.subtle.label.danger` | `{color.text.danger.on-secondary}` | `color` | `--tag-subtle-label-danger` |
| `tag.strong.background.muted` | `{color.background.base-inverse.default-active}` | `color` | `--tag-strong-background-muted` |
| `tag.strong.background.neutral` | `{color.background.base-inverse.default}` | `color` | `--tag-strong-background-neutral` |
| `tag.strong.background.accent` | `{color.background.warning.accent}` | `color` | `--tag-strong-background-accent` |
| `tag.strong.background.success` | `{color.background.positive.default-hover}` | `color` | `--tag-strong-background-success` |
| `tag.strong.background.brand` | `{color.background.brand.default}` | `color` | `--tag-strong-background-brand` |
| `tag.strong.background.danger` | `{color.background.danger.default}` | `color` | `--tag-strong-background-danger` |
| `tag.strong.label.muted` | `{color.text.base-inverse.on-color}` | `color` | `--tag-strong-label-muted` |
| `tag.strong.label.neutral` | `{color.text.base-inverse.default}` | `color` | `--tag-strong-label-neutral` |
| `tag.strong.label.accent` | `{color.text.base.default-on-color}` | `color` | `--tag-strong-label-accent` |
| `tag.strong.label.success` | `{color.text.base-inverse.on-color}` | `color` | `--tag-strong-label-success` |
| `tag.strong.label.brand` | `{color.text.base-inverse.on-color}` | `color` | `--tag-strong-label-brand` |
| `tag.strong.label.danger` | `{color.text.base-inverse.on-color}` | `color` | `--tag-strong-label-danger` |
| `tag.outlined.background` | `transparent` | `color` | `--tag-outlined-background` |
| `tag.outlined.border.muted` | `{color.border.base.default}` | `color` | `--tag-outlined-border-muted` |
| `tag.outlined.border.neutral` | `{color.border.base.default}` | `color` | `--tag-outlined-border-neutral` |
| `tag.outlined.border.accent` | `{color.border.warning.default}` | `color` | `--tag-outlined-border-accent` |
| `tag.outlined.border.success` | `{color.border.positive.default}` | `color` | `--tag-outlined-border-success` |
| `tag.outlined.border.brand` | `{color.border.brand.default}` | `color` | `--tag-outlined-border-brand` |
| `tag.outlined.border.danger` | `{color.border.danger.default}` | `color` | `--tag-outlined-border-danger` |
| `tag.outlined.label.muted` | `{color.text.base.tertiary}` | `color` | `--tag-outlined-label-muted` |
| `tag.outlined.label.neutral` | `{color.text.base.default}` | `color` | `--tag-outlined-label-neutral` |
| `tag.outlined.label.accent` | `{color.text.warning.on-secondary}` | `color` | `--tag-outlined-label-accent` |
| `tag.outlined.label.success` | `{color.text.positive.on-secondary}` | `color` | `--tag-outlined-label-success` |
| `tag.outlined.label.brand` | `{color.text.brand.on-secondary}` | `color` | `--tag-outlined-label-brand` |
| `tag.outlined.label.danger` | `{color.text.danger.on-secondary}` | `color` | `--tag-outlined-label-danger` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/tag.tokens.json</code></summary>

```json
{
  "tag": {
    "container": {
      "height": {
        "md": { "$value": "{spacing.24}", "$type": "dimension" },
        "sm": { "$value": "{spacing.20}", "$type": "dimension" }
      },
      "minWidth": {
        "md": { "$value": "{spacing.32}", "$type": "dimension" },
        "sm": { "$value": "{spacing.32}", "$type": "dimension" }
      },
      "paddingInline": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "sm": { "$value": "{spacing.6}", "$type": "dimension" }
      },
      "paddingInlineIcon": {
        "md": { "$value": "{spacing.6}", "$type": "dimension" },
        "sm": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "gap": {
        "md": { "$value": "{spacing.4}", "$type": "dimension" },
        "sm": { "$value": "{spacing.4}", "$type": "dimension" }
      },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "borderWidth": { "$value": "1px", "$type": "dimension" },
      "groupGap": { "$value": "{spacing.8}", "$type": "dimension" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": {
        "md": { "$value": "{fontSize.14}", "$type": "dimension" },
        "sm": { "$value": "{fontSize.12}", "$type": "dimension" }
      },
      "lineHeight": {
        "md": { "$value": "{lineHeight.20}", "$type": "dimension" },
        "sm": { "$value": "{lineHeight.16}", "$type": "dimension" }
      },
      "fontWeightStatus": { "$value": "{fontWeight.medium}", "$type": "fontWeight" },
      "fontWeightInfo": { "$value": "{fontWeight.regular}", "$type": "fontWeight" }
    },
    "icon": {
      "size": {
        "md": { "$value": "{spacing.16}", "$type": "dimension" },
        "sm": { "$value": "{spacing.12}", "$type": "dimension" }
      }
    },
    "subtle": {
      "background": {
        "muted": { "$value": "{color.background.base.default}", "$type": "color" },
        "neutral": { "$value": "{color.background.base.tertiary}", "$type": "color" },
        "accent": { "$value": "{color.background.warning.secondary}", "$type": "color" },
        "success": { "$value": "{color.background.positive.secondary}", "$type": "color" },
        "brand": { "$value": "{color.background.brand.secondary}", "$type": "color" },
        "danger": { "$value": "{color.background.danger.secondary}", "$type": "color" }
      },
      "label": {
        "muted": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "neutral": { "$value": "{color.text.base.default}", "$type": "color" },
        "accent": { "$value": "{color.text.warning.on-secondary}", "$type": "color" },
        "success": { "$value": "{color.text.positive.on-secondary}", "$type": "color" },
        "brand": { "$value": "{color.text.brand.on-secondary}", "$type": "color" },
        "danger": { "$value": "{color.text.danger.on-secondary}", "$type": "color" }
      }
    },
    "strong": {
      "background": {
        "muted": { "$value": "{color.background.base-inverse.default-active}", "$type": "color" },
        "neutral": { "$value": "{color.background.base-inverse.default}", "$type": "color" },
        "accent": { "$value": "{color.background.warning.accent}", "$type": "color" },
        "success": {
          "$value": "{color.background.positive.default-hover}",
          "$type": "color",
          "$comment": "Darker green (#027948) instead of positive.default (#039855): white on-color label is 3.73:1 on the lighter green (fails WCAG AA at 14px medium) but ~5.5:1 here. Matches mud-notification / mud-badge."
        },
        "brand": { "$value": "{color.background.brand.default}", "$type": "color" },
        "danger": { "$value": "{color.background.danger.default}", "$type": "color" }
      },
      "label": {
        "muted": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
        "neutral": { "$value": "{color.text.base-inverse.default}", "$type": "color" },
        "accent": { "$value": "{color.text.base.default-on-color}", "$type": "color" },
        "success": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
        "brand": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" },
        "danger": { "$value": "{color.text.base-inverse.on-color}", "$type": "color" }
      }
    },
    "outlined": {
      "background": { "$value": "transparent", "$type": "color" },
      "border": {
        "muted": { "$value": "{color.border.base.default}", "$type": "color" },
        "neutral": { "$value": "{color.border.base.default}", "$type": "color" },
        "accent": { "$value": "{color.border.warning.default}", "$type": "color" },
        "success": { "$value": "{color.border.positive.default}", "$type": "color" },
        "brand": { "$value": "{color.border.brand.default}", "$type": "color" },
        "danger": { "$value": "{color.border.danger.default}", "$type": "color" }
      },
      "label": {
        "muted": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "neutral": { "$value": "{color.text.base.default}", "$type": "color" },
        "accent": { "$value": "{color.text.warning.on-secondary}", "$type": "color" },
        "success": { "$value": "{color.text.positive.on-secondary}", "$type": "color" },
        "brand": { "$value": "{color.text.brand.on-secondary}", "$type": "color" },
        "danger": { "$value": "{color.text.danger.on-secondary}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/textarea.tokens.json`

_18 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `textarea.container.minHeight.md` | `120px` | `dimension` | `--textarea-container-min-height-md` |
| `textarea.container.minHeight.lg` | `150px` | `dimension` | `--textarea-container-min-height-lg` |
| `textarea.container.paddingBlock.md` | `{spacing.8}` | `dimension` | `--textarea-container-padding-block-md` |
| `textarea.container.paddingBlock.lg` | `{spacing.12}` | `dimension` | `--textarea-container-padding-block-lg` |
| `textarea.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--textarea-label-font-weight` |
| `textarea.resize.size` | `{spacing.24}` | `dimension` | `--textarea-resize-size` |
| `textarea.resize.offset` | `{spacing.4}` | `dimension` | `--textarea-resize-offset` |
| `textarea.resize.color.default` | `{color.icon.base.tertiary}` | `color` | `--textarea-resize-color-default` |
| `textarea.resize.color.disabled` | `{color.icon.disabled.on-disabled}` | `color` | `--textarea-resize-color-disabled` |
| `textarea.counter.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--textarea-counter-font-family` |
| `textarea.counter.fontSize` | `{fontSize.14}` | `dimension` | `--textarea-counter-font-size` |
| `textarea.counter.lineHeight` | `{lineHeight.20}` | `dimension` | `--textarea-counter-line-height` |
| `textarea.counter.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--textarea-counter-font-weight` |
| `textarea.counter.gap` | `{spacing.16}` | `dimension` | `--textarea-counter-gap` |
| `textarea.counter.marginBlockStart` | `{spacing.4}` | `dimension` | `--textarea-counter-margin-block-start` |
| `textarea.counter.color.default` | `{color.text.base.tertiary}` | `color` | `--textarea-counter-color-default` |
| `textarea.counter.color.error` | `{color.text.danger.default}` | `color` | `--textarea-counter-color-error` |
| `textarea.counter.color.disabled` | `{color.text.disabled.on-disabled}` | `color` | `--textarea-counter-color-disabled` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/textarea.tokens.json</code></summary>

```json
{
  "textarea": {
    "container": {
      "minHeight": {
        "md": { "$value": "120px", "$type": "dimension" },
        "lg": { "$value": "150px", "$type": "dimension" }
      },
      "paddingBlock": {
        "md": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.12}", "$type": "dimension" }
      }
    },
    "label": {
      "fontWeight": {
        "$value": "{fontWeight.regular}",
        "$type": "fontWeight",
        "$comment": "Per Figma 378:17677 — Textarea label uses Onest Regular (Desktop/Body/Small), overriding mud-input's medium default."
      }
    },
    "resize": {
      "size": { "$value": "{spacing.24}", "$type": "dimension" },
      "offset": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.icon.base.tertiary}", "$type": "color" },
        "disabled": { "$value": "{color.icon.disabled.on-disabled}", "$type": "color" }
      }
    },
    "counter": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "gap": { "$value": "{spacing.16}", "$type": "dimension" },
      "marginBlockStart": { "$value": "{spacing.4}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.text.base.tertiary}", "$type": "color" },
        "error": { "$value": "{color.text.danger.default}", "$type": "color" },
        "disabled": { "$value": "{color.text.disabled.on-disabled}", "$type": "color" }
      }
    }
  }
}
```

</details>

#### `tokens/core/components/tooltip.tokens.json`

_40 tokens._

| Token (JSON path) | `$value` | `$type` | Variabilă CSS |
| --- | --- | --- | --- |
| `tooltip.container.borderRadius.sm` | `{borderRadius.4}` | `dimension` | `--tooltip-container-border-radius-sm` |
| `tooltip.container.borderRadius.lg` | `{borderRadius.6}` | `dimension` | `--tooltip-container-border-radius-lg` |
| `tooltip.container.paddingBlock.sm` | `{spacing.8}` | `dimension` | `--tooltip-container-padding-block-sm` |
| `tooltip.container.paddingBlock.lg` | `{spacing.12}` | `dimension` | `--tooltip-container-padding-block-lg` |
| `tooltip.container.paddingInline.sm` | `{spacing.12}` | `dimension` | `--tooltip-container-padding-inline-sm` |
| `tooltip.container.paddingInline.lg` | `{spacing.16}` | `dimension` | `--tooltip-container-padding-inline-lg` |
| `tooltip.container.paddingInlineEnd.lg` | `{spacing.12}` | `dimension` | `--tooltip-container-padding-inline-end-lg` |
| `tooltip.container.gap.sm` | `{spacing.8}` | `dimension` | `--tooltip-container-gap-sm` |
| `tooltip.container.gap.lg` | `{spacing.12}` | `dimension` | `--tooltip-container-gap-lg` |
| `tooltip.container.maxWidth` | `200px` | `dimension` | `--tooltip-container-max-width` |
| `tooltip.container.shadow` | `{dropShadow.100}` | `string` | `--tooltip-container-shadow` |
| `tooltip.container.transitionDuration` | `150ms` | `duration` | `--tooltip-container-transition-duration` |
| `tooltip.container.transitionTimingFunction` | `ease-out` | `cubicBezier` | `--tooltip-container-transition-timing-function` |
| `tooltip.container.background.default` | `{color.background.base-inverse.default}` | `color` | `--tooltip-container-background-default` |
| `tooltip.container.zIndex` | `9999` | `number` | `--tooltip-container-z-index` |
| `tooltip.label.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--tooltip-label-font-family` |
| `tooltip.label.fontSize` | `{fontSize.14}` | `dimension` | `--tooltip-label-font-size` |
| `tooltip.label.lineHeight` | `{lineHeight.20}` | `dimension` | `--tooltip-label-line-height` |
| `tooltip.label.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--tooltip-label-font-weight` |
| `tooltip.label.color.default` | `{color.text.base-inverse.default}` | `color` | `--tooltip-label-color-default` |
| `tooltip.hint.fontFamily` | `{fontFamily.primary}` | `fontFamily` | `--tooltip-hint-font-family` |
| `tooltip.hint.fontSize` | `{fontSize.12}` | `dimension` | `--tooltip-hint-font-size` |
| `tooltip.hint.lineHeight` | `{lineHeight.16}` | `dimension` | `--tooltip-hint-line-height` |
| `tooltip.hint.fontWeight` | `{fontWeight.regular}` | `fontWeight` | `--tooltip-hint-font-weight` |
| `tooltip.hint.color.default` | `{color.text.base-inverse.default}` | `color` | `--tooltip-hint-color-default` |
| `tooltip.hint.opacity` | `0.72` | `number` | `--tooltip-hint-opacity` |
| `tooltip.hint.marginTop` | `{spacing.6}` | `dimension` | `--tooltip-hint-margin-top` |
| `tooltip.arrow.size.sm` | `8px` | `dimension` | `--tooltip-arrow-size-sm` |
| `tooltip.arrow.size.lg` | `12px` | `dimension` | `--tooltip-arrow-size-lg` |
| `tooltip.arrow.width.sm` | `16px` | `dimension` | `--tooltip-arrow-width-sm` |
| `tooltip.arrow.width.lg` | `24px` | `dimension` | `--tooltip-arrow-width-lg` |
| `tooltip.arrow.edgeMargin` | `{spacing.12}` | `dimension` | `--tooltip-arrow-edge-margin` |
| `tooltip.close.size` | `{spacing.16}` | `dimension` | `--tooltip-close-size` |
| `tooltip.close.color.default` | `{color.icon.base-inverse.default}` | `color` | `--tooltip-close-color-default` |
| `tooltip.close.color.hover` | `{color.icon.base-inverse.default}` | `color` | `--tooltip-close-color-hover` |
| `tooltip.close.background.default` | `transparent` | `color` | `--tooltip-close-background-default` |
| `tooltip.close.background.hover` | `{color.background.base-inverse.default-hover}` | `color` | `--tooltip-close-background-hover` |
| `tooltip.close.borderRadius` | `{borderRadius.4}` | `dimension` | `--tooltip-close-border-radius` |
| `tooltip.close.touchTarget` | `{spacing.24}` | `dimension` | `--tooltip-close-touch-target` |
| `tooltip.offset.trigger` | `{spacing.4}` | `dimension` | `--tooltip-offset-trigger` |

<details>
<summary>Sursa JSON exactă — <code>tokens/core/components/tooltip.tokens.json</code></summary>

```json
{
  "tooltip": {
    "container": {
      "borderRadius": {
        "sm": { "$value": "{borderRadius.4}", "$type": "dimension" },
        "lg": { "$value": "{borderRadius.6}", "$type": "dimension" }
      },
      "paddingBlock": {
        "sm": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "paddingInline": {
        "sm": { "$value": "{spacing.12}", "$type": "dimension" },
        "lg": { "$value": "{spacing.16}", "$type": "dimension" }
      },
      "paddingInlineEnd": {
        "lg": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "gap": {
        "sm": { "$value": "{spacing.8}", "$type": "dimension" },
        "lg": { "$value": "{spacing.12}", "$type": "dimension" }
      },
      "maxWidth": { "$value": "200px", "$type": "dimension" },
      "shadow": {
        "$value": "{dropShadow.100}",
        "$type": "string",
        "$comment": "Lightest elevation rung. dropShadow.200 carries a 2px spread that pushes a faint grey halo ~2px beyond the bubble edges; on the dark bubble against a light page that halo reads as a thin border. dropShadow.100 has no spread on its primary layer and is the correct elevation for a small floating tooltip."
      },
      "transitionDuration": { "$value": "150ms", "$type": "duration" },
      "transitionTimingFunction": { "$value": "ease-out", "$type": "cubicBezier" },
      "background": {
        "default": { "$value": "{color.background.base-inverse.default}", "$type": "color" }
      },
      "zIndex": { "$value": "9999", "$type": "number" }
    },
    "label": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.14}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.20}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "color": {
        "default": { "$value": "{color.text.base-inverse.default}", "$type": "color" }
      }
    },
    "hint": {
      "fontFamily": { "$value": "{fontFamily.primary}", "$type": "fontFamily" },
      "fontSize": { "$value": "{fontSize.12}", "$type": "dimension" },
      "lineHeight": { "$value": "{lineHeight.16}", "$type": "dimension" },
      "fontWeight": { "$value": "{fontWeight.regular}", "$type": "fontWeight" },
      "color": {
        "default": { "$value": "{color.text.base-inverse.default}", "$type": "color" }
      },
      "opacity": { "$value": "0.72", "$type": "number" },
      "marginTop": { "$value": "{spacing.6}", "$type": "dimension" }
    },
    "arrow": {
      "size": {
        "sm": { "$value": "8px", "$type": "dimension" },
        "lg": { "$value": "12px", "$type": "dimension" }
      },
      "width": {
        "sm": { "$value": "16px", "$type": "dimension" },
        "lg": { "$value": "24px", "$type": "dimension" }
      },
      "edgeMargin": { "$value": "{spacing.12}", "$type": "dimension" }
    },
    "close": {
      "size": { "$value": "{spacing.16}", "$type": "dimension" },
      "color": {
        "default": { "$value": "{color.icon.base-inverse.default}", "$type": "color" },
        "hover": { "$value": "{color.icon.base-inverse.default}", "$type": "color" }
      },
      "background": {
        "default": { "$value": "transparent", "$type": "color" },
        "hover": { "$value": "{color.background.base-inverse.default-hover}", "$type": "color" }
      },
      "borderRadius": { "$value": "{borderRadius.4}", "$type": "dimension" },
      "touchTarget": { "$value": "{spacing.24}", "$type": "dimension" }
    },
    "offset": {
      "trigger": { "$value": "{spacing.4}", "$type": "dimension" }
    }
  }
}
```

</details>

