# MUD — Design tokens pentru tabele, coloane, sortare și filtre

> Sursă: <https://github.com/egov-moldova/design-system> · commit `36f0786c220f8660fed2343e8c651bad26239ba2`
> Pachet: `@egov-moldova/mud` v1.0.6 · Font: Onest
> Document generat: 2026-09-06

**Fișier autonom.** Fiecare valoare de aici este rezolvată până la capăt — hex, px, ms — atât
pentru tema deschisă cât și pentru cea întunecată. Lanțul de referințe
(component → semantic → paletă) este păstrat vizibil în coloana _Lanț_, ca să știi ce token
semantic să modifici dacă vrei să schimbi ceva global, dar nu trebuie să deschizi alt fișier ca
să scrii CSS.

Pentru sistemul complet (toate cele 46 de componente, 2.923 de tokens), vezi `MUD-DESIGN-TOKENS.md`.

---

## 1. Cum se folosesc

Tokenii sunt variabile CSS globale, generate cu Style Dictionary și livrate ca fișiere CSS
independente. Componentele sunt Web Components cu Shadow DOM — variabilele CSS sunt **singurul
canal** prin care stilizarea traversează granița shadow root-ului.

```html
<link rel="stylesheet" href="node_modules/@egov-moldova/mud/dist/mud/tokens/core.tokens.css" />
<link rel="stylesheet" href="node_modules/@egov-moldova/mud/dist/mud/tokens/core.dark.tokens.css" />
```

`core.tokens.css` definește totul pe `:root`. `core.dark.tokens.css` conține doar suprascrierile
de culoare, pe selectorul `:root[data-theme="dark"]` — deci comutarea temei se face cu:

```html
<html data-theme="dark">
```

Ca să suprascrii o valoare, redefinește variabila **după** încărcarea fișierelor de tokens:

```css
:root {
  --table-header-min-height: 56px;
  --table-row-background-zebra: var(--palette-blue-sky-100);
}
```

Regula internă a sistemului este că CSS-ul de componentă nu referă niciodată direct
`--palette-*` — folosește tokens de componentă (`--table-*`) sau semantici (`--color-*`).
Pentru suprascrieri din afara sistemului regula nu se aplică, dar dacă folosești `--palette-*`
direct pierzi comutarea automată light/dark, fiindcă doar stratul semantic are suprascrieri
pentru temă.

---

## 2. Harta ecosistemului

`mud-table` este o moleculă: randează un `<table>` nativ în shadow DOM (pentru semantică de
accesibilitate completă — `role="table"`, `role="columnheader"`, `aria-sort`, `aria-selected`)
și compune alte componente pentru părțile interactive.

| Rol în interfață | Componentă | Tokens | Secțiune |
| --- | --- | --- | --- |
| Container, antet, celule, rânduri, stare goală | `mud-table` | 49 | §4 |
| Coloana de selecție | `mud-checkbox` | 50 | §5 |
| Status / etichete în celulă | `mud-tag` | 59 | §5 |
| Acțiuni pe rând | `mud-button` | 150 | §5 |
| Paginare sub tabel | `mud-pagination` | 81 | §5 |
| Explicații pe antet sau celulă | `mud-tooltip` | 40 | §5 |
| Chip-uri de filtrare | `mud-chip` (`type="filter"`) | 62 | §6 |
| Filtre multiple ca etichete | `mud-input-chip` | 86 | §6 |
| Căutare liberă | `mud-search-input-rectangular` / `-circular` | 81 / 81 | §6 |
| Filtru dropdown | `mud-select-input` | 93 | §6 |
| Meniu de coloană / acțiuni | `mud-menu` | 43 | §6 |
| Filtre exclusive | `mud-segmented-control` | 38 | §6 |
| Filtre pe categorii | `mud-tabs` | 61 | §6 |
| Filtru pe dată / interval | `mud-date-input` / `mud-date-picker` | 56 / 70 | §6 |

Total: **1.100 tokens de componentă** + **283 tokens de fundament** (§3).

### Două lucruri de știut înainte de a începe

**1. `mud-table` nu are filtrare.** API-ul componentei
(`src/components/mud-table/mud-table.types.ts`) acoperă sortare (`sortable`, `sortColumn`,
`sortDirection`, evenimentul `mudSort`) și selecție (`selectable`, `selectedRows`,
`mudSelectionChange`) — nimic pentru filtre. Nu există componentă de tip toolbar sau
filter-bar în tot sistemul, iar în `README.md`, `.specs/` și `docs/backlog/` nu apare nicio
mențiune de „filter”, „toolbar” sau „data grid”. Filtrarea o implementezi tu, deasupra
tabelului, din componentele listate în §6. Secțiunea §7 propune tokenii de layout care lipsesc.

**2. Iconurile nu au fișier de tokens propriu.** Nu există
`tokens/core/components/icon.tokens.json`. `mud-icon` expune două variabile locale pe care le
setezi tu — `--icon-size` și `--icon-color` — și folosește intern semanticele `--color-icon-*`
(§3.2) plus primitivele `--spacing-12/16/20/24` pentru dimensiuni. O scală de mărimi denumită
(`3xs`…`xl`) există doar în `tokens/legacy/components/icon.tokens.json`, care **nu se
compilează** — nicio configurație Style Dictionary din repo nu include `tokens/legacy/**` în
`source`, deci acele variabile nu ajung în niciun CSS livrat. Nu te baza pe ele.

Excepție: iconul de sortare din tabel **are** tokens dedicați — `table.sortIcon.*` (§4.3).

---

## 3. Fundamente — valori rezolvate

Toate valorile de mai jos sunt copiate din commit-ul `36f0786c220f8660fed2343e8c651bad26239ba2`. Paleta nu este suprascrisă de tema întunecată — doar stratul semantic este.

### 3.1 Paletă (primitive) — `tokens/core/palette.tokens.json`

_106 tokens._

| Variabilă CSS | Token | Valoare |
| --- | --- | --- |
| `--palette-blue-sky-100` | `palette.blue-sky.100` | `#e8f0fb` |
| `--palette-blue-sky-150` | `palette.blue-sky.150` | `#d6e5f8` |
| `--palette-blue-sky-200` | `palette.blue-sky.200` | `#ccdef6` |
| `--palette-blue-sky-300` | `palette.blue-sky.300` | `#99bced` |
| `--palette-blue-sky-400` | `palette.blue-sky.400` | `#669be4` |
| `--palette-blue-sky-500` | `palette.blue-sky.500` | `#3379db` |
| `--palette-blue-sky-600` | `palette.blue-sky.600` | `#0058d2` |
| `--palette-blue-sky-700` | `palette.blue-sky.700` | `#0046a8` |
| `--palette-blue-sky-800` | `palette.blue-sky.800` | `#00357e` |
| `--palette-blue-sky-900` | `palette.blue-sky.900` | `#00295a` |
| `--palette-lavender-100` | `palette.lavender.100` | `#efeafc` |
| `--palette-lavender-200` | `palette.lavender.200` | `#ddd2fa` |
| `--palette-lavender-300` | `palette.lavender.300` | `#bba5f5` |
| `--palette-lavender-400` | `palette.lavender.400` | `#9a79ef` |
| `--palette-lavender-500` | `palette.lavender.500` | `#784cea` |
| `--palette-lavender-600` | `palette.lavender.600` | `#561fe5` |
| `--palette-lavender-700` | `palette.lavender.700` | `#4519b7` |
| `--palette-lavender-800` | `palette.lavender.800` | `#341389` |
| `--palette-lavender-900` | `palette.lavender.900` | `#240c66` |
| `--palette-purple-100` | `palette.purple.100` | `#efeafc` |
| `--palette-purple-200` | `palette.purple.200` | `#ddd2fa` |
| `--palette-purple-300` | `palette.purple.300` | `#cbaffd` |
| `--palette-purple-400` | `palette.purple.400` | `#b287fb` |
| `--palette-purple-500` | `palette.purple.500` | `#985ffa` |
| `--palette-purple-600` | `palette.purple.600` | `#7e37f9` |
| `--palette-purple-700` | `palette.purple.700` | `#652cc7` |
| `--palette-purple-800` | `palette.purple.800` | `#4c2195` |
| `--palette-purple-900` | `palette.purple.900` | `#36166a` |
| `--palette-magenta-100` | `palette.magenta.100` | `#f7eafa` |
| `--palette-magenta-200` | `palette.magenta.200` | `#eed1f5` |
| `--palette-magenta-300` | `palette.magenta.300` | `#dda3eb` |
| `--palette-magenta-400` | `palette.magenta.400` | `#cc74e2` |
| `--palette-magenta-500` | `palette.magenta.500` | `#bb46d8` |
| `--palette-magenta-600` | `palette.magenta.600` | `#aa18ce` |
| `--palette-magenta-700` | `palette.magenta.700` | `#8813a5` |
| `--palette-magenta-800` | `palette.magenta.800` | `#660e7c` |
| `--palette-magenta-900` | `palette.magenta.900` | `#4a095a` |
| `--palette-forest-green-100` | `palette.forest-green.100` | `#e8f1f1` |
| `--palette-forest-green-150` | `palette.forest-green.150` | `#d6e6e7` |
| `--palette-forest-green-200` | `palette.forest-green.200` | `#cce0e1` |
| `--palette-forest-green-300` | `palette.forest-green.300` | `#99c1c3` |
| `--palette-forest-green-400` | `palette.forest-green.400` | `#66a1a5` |
| `--palette-forest-green-500` | `palette.forest-green.500` | `#338287` |
| `--palette-forest-green-600` | `palette.forest-green.600` | `#006369` |
| `--palette-forest-green-700` | `palette.forest-green.700` | `#004f54` |
| `--palette-forest-green-800` | `palette.forest-green.800` | `#003b3f` |
| `--palette-forest-green-900` | `palette.forest-green.900` | `#00292c` |
| `--palette-red-50` | `palette.red.50` | `#feefee` |
| `--palette-red-100` | `palette.red.100` | `#fee4e2` |
| `--palette-red-200` | `palette.red.200` | `#fecdc9` |
| `--palette-red-300` | `palette.red.300` | `#fda19b` |
| `--palette-red-400` | `palette.red.400` | `#f97066` |
| `--palette-red-500` | `palette.red.500` | `#f04438` |
| `--palette-red-600` | `palette.red.600` | `#d92d20` |
| `--palette-red-700` | `palette.red.700` | `#b32318` |
| `--palette-red-800` | `palette.red.800` | `#912018` |
| `--palette-red-900` | `palette.red.900` | `#7a271a` |
| `--palette-apricot-50` | `palette.apricot.50` | `#fef5dd` |
| `--palette-apricot-100` | `palette.apricot.100` | `#feefc6` |
| `--palette-apricot-200` | `palette.apricot.200` | `#fedf89` |
| `--palette-apricot-300` | `palette.apricot.300` | `#fec84b` |
| `--palette-apricot-400` | `palette.apricot.400` | `#fdb022` |
| `--palette-apricot-500` | `palette.apricot.500` | `#f79009` |
| `--palette-apricot-600` | `palette.apricot.600` | `#dc6803` |
| `--palette-apricot-700` | `palette.apricot.700` | `#b54708` |
| `--palette-apricot-800` | `palette.apricot.800` | `#93370d` |
| `--palette-apricot-900` | `palette.apricot.900` | `#792e0d` |
| `--palette-green-50` | `palette.green.50` | `#ebf7f1` |
| `--palette-green-100` | `palette.green.100` | `#e6f5ee` |
| `--palette-green-200` | `palette.green.200` | `#cdeadd` |
| `--palette-green-300` | `palette.green.300` | `#9ad6bb` |
| `--palette-green-400` | `palette.green.400` | `#68c199` |
| `--palette-green-500` | `palette.green.500` | `#35ad77` |
| `--palette-green-600` | `palette.green.600` | `#039855` |
| `--palette-green-700` | `palette.green.700` | `#027948` |
| `--palette-green-800` | `palette.green.800` | `#05603a` |
| `--palette-green-900` | `palette.green.900` | `#054f31` |
| `--palette-gray-50` | `palette.gray.50` | `#f7f7f7` |
| `--palette-gray-100` | `palette.gray.100` | `#f5f5f5` |
| `--palette-gray-200` | `palette.gray.200` | `#f1f1f1` |
| `--palette-gray-250` | `palette.gray.250` | `#d9d9d9` |
| `--palette-gray-300` | `palette.gray.300` | `#b2b2b2` |
| `--palette-gray-350` | `palette.gray.350` | `#8a8a8a` |
| `--palette-gray-400` | `palette.gray.400` | `#757575` |
| `--palette-gray-500` | `palette.gray.500` | `#616161` |
| `--palette-gray-600` | `palette.gray.600` | `#444444` |
| `--palette-gray-700` | `palette.gray.700` | `#383838` |
| `--palette-gray-800` | `palette.gray.800` | `#2c2c2c` |
| `--palette-gray-900` | `palette.gray.900` | `#1e1e1e` |
| `--palette-black-1000` | `palette.black.1000` | `#121212` |
| `--palette-white-1000` | `palette.white.1000` | `#ffffff` |
| `--palette-alpha-black-100-alpha` | `palette.alpha.black.100-alpha` | `#1212120d` |
| `--palette-alpha-black-200-alpha` | `palette.alpha.black.200-alpha` | `#1212121a` |
| `--palette-alpha-black-300-alpha` | `palette.alpha.black.300-alpha` | `#12121233` |
| `--palette-alpha-black-400-alpha` | `palette.alpha.black.400-alpha` | `#12121266` |
| `--palette-alpha-black-500-alpha` | `palette.alpha.black.500-alpha` | `#12121299` |
| `--palette-alpha-white-100-alpha` | `palette.alpha.white.100-alpha` | `#ffffff0d` |
| `--palette-alpha-white-200-alpha` | `palette.alpha.white.200-alpha` | `#ffffff1a` |
| `--palette-alpha-white-300-alpha` | `palette.alpha.white.300-alpha` | `#ffffff33` |
| `--palette-alpha-white-400-alpha` | `palette.alpha.white.400-alpha` | `#ffffff66` |
| `--palette-alpha-white-500-alpha` | `palette.alpha.white.500-alpha` | `#ffffff99` |
| `--palette-alpha-gray-alpha-100` | `palette.alpha.gray.alpha-100` | `#44444408` |
| `--palette-alpha-gray-alpha-200` | `palette.alpha.gray.alpha-200` | `#4444441a` |
| `--palette-alpha-gray-alpha-300` | `palette.alpha.gray.alpha-300` | `#44444433` |
| `--palette-alpha-gray-alpha-400` | `palette.alpha.gray.alpha-400` | `#44444466` |
| `--palette-alpha-gray-alpha-500` | `palette.alpha.gray.alpha-500` | `#44444499` |

### 3.2 Culori semantice — `tokens/core/color.tokens.json`

_89 tokens._

| Variabilă CSS | Token | Light | Dark |
| --- | --- | --- | --- |
| `--color-background-base-default` | `color.background.base.default` | `{palette.white.1000}` → `#ffffff` | `{palette.gray.900}` → `#1e1e1e` |
| `--color-background-base-default-active` | `color.background.base.default-active` | `{palette.gray.250}` → `#d9d9d9` | `{palette.gray.700}` → `#383838` |
| `--color-background-base-secondary` | `color.background.base.secondary` | `{palette.gray.100}` → `#f5f5f5` | `{palette.gray.800}` → `#2c2c2c` |
| `--color-background-base-secondary-active` | `color.background.base.secondary-active` | `{palette.gray.250}` → `#d9d9d9` | `{palette.gray.600}` → `#444444` |
| `--color-background-base-tertiary` | `color.background.base.tertiary` | `{palette.gray.200}` → `#f1f1f1` | `{palette.gray.700}` → `#383838` |
| `--color-background-base-tertiary-active` | `color.background.base.tertiary-active` | `{palette.gray.300}` → `#b2b2b2` | `{palette.gray.500}` → `#616161` |
| `--color-background-base-tertiary-hover` | `color.background.base.tertiary-hover` | `{palette.gray.250}` → `#d9d9d9` | `{palette.gray.600}` → `#444444` |
| `--color-background-base-default-hover` | `color.background.base.default-hover` | `{palette.gray.100}` → `#f5f5f5` | `{palette.gray.800}` → `#2c2c2c` |
| `--color-background-base-secondary-hover` | `color.background.base.secondary-hover` | `{palette.gray.200}` → `#f1f1f1` | `{palette.gray.700}` → `#383838` |
| `--color-background-brand-default` | `color.background.brand.default` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.600}` → `#0058d2` |
| `--color-background-brand-default-active` | `color.background.brand.default-active` | `{palette.blue-sky.800}` → `#00357e` | `{palette.blue-sky.800}` → `#00357e` |
| `--color-background-brand-secondary` | `color.background.brand.secondary` | `{palette.blue-sky.100}` → `#e8f0fb` | `{palette.blue-sky.200}` → `#ccdef6` |
| `--color-background-brand-secondary-active` | `color.background.brand.secondary-active` | `{palette.blue-sky.300}` → `#99bced` | `{palette.blue-sky.400}` → `#669be4` |
| `--color-background-brand-default-hover` | `color.background.brand.default-hover` | `{palette.blue-sky.700}` → `#0046a8` | `{palette.blue-sky.700}` → `#0046a8` |
| `--color-background-brand-secondary-hover` | `color.background.brand.secondary-hover` | `{palette.blue-sky.200}` → `#ccdef6` | `{palette.blue-sky.300}` → `#99bced` |
| `--color-background-brand-tertiary` | `color.background.brand.tertiary` | `{palette.blue-sky.900}` → `#00295a` | `{palette.blue-sky.900}` → `#00295a` |
| `--color-background-brand-focus-ring` | `color.background.brand.focus-ring` | `{palette.blue-sky.500}` → `#3379db` | `{palette.blue-sky.500}` → `#3379db` |
| `--color-background-positive-default` | `color.background.positive.default` | `{palette.green.600}` → `#039855` | `{palette.green.700}` → `#027948` |
| `--color-background-positive-default-active` | `color.background.positive.default-active` | `{palette.green.800}` → `#05603a` | `{palette.green.900}` → `#054f31` |
| `--color-background-positive-secondary` | `color.background.positive.secondary` | `{palette.green.100}` → `#e6f5ee` | `{palette.green.200}` → `#cdeadd` |
| `--color-background-positive-secondary-active` | `color.background.positive.secondary-active` | `{palette.green.200}` → `#cdeadd` | `{palette.green.300}` → `#9ad6bb` |
| `--color-background-positive-default-hover` | `color.background.positive.default-hover` | `{palette.green.700}` → `#027948` | `{palette.green.800}` → `#05603a` |
| `--color-background-warning-default` | `color.background.warning.default` | `{palette.apricot.400}` → `#fdb022` | `{palette.apricot.500}` → `#f79009` |
| `--color-background-warning-default-active` | `color.background.warning.default-active` | `{palette.apricot.600}` → `#dc6803` | `{palette.apricot.700}` → `#b54708` |
| `--color-background-warning-secondary` | `color.background.warning.secondary` | `{palette.apricot.100}` → `#feefc6` | `{palette.apricot.200}` → `#fedf89` |
| `--color-background-warning-secondary-active` | `color.background.warning.secondary-active` | `{palette.apricot.200}` → `#fedf89` | `{palette.apricot.300}` → `#fec84b` |
| `--color-background-warning-accent` | `color.background.warning.accent` | `{palette.apricot.300}` → `#fec84b` | `{palette.apricot.400}` → `#fdb022` |
| `--color-background-warning-default-hover` | `color.background.warning.default-hover` | `{palette.apricot.500}` → `#f79009` | `{palette.apricot.600}` → `#dc6803` |
| `--color-background-danger-default` | `color.background.danger.default` | `{palette.red.600}` → `#d92d20` | `{palette.red.700}` → `#b32318` |
| `--color-background-danger-default-active` | `color.background.danger.default-active` | `{palette.red.800}` → `#912018` | `{palette.red.900}` → `#7a271a` |
| `--color-background-danger-secondary` | `color.background.danger.secondary` | `{palette.red.100}` → `#fee4e2` | `{palette.red.200}` → `#fecdc9` |
| `--color-background-danger-secondary-active` | `color.background.danger.secondary-active` | `{palette.red.300}` → `#fda19b` | `{palette.red.400}` → `#f97066` |
| `--color-background-danger-default-hover` | `color.background.danger.default-hover` | `{palette.red.700}` → `#b32318` | `{palette.red.800}` → `#912018` |
| `--color-background-danger-secondary-hover` | `color.background.danger.secondary-hover` | `{palette.red.200}` → `#fecdc9` | `{palette.red.300}` → `#fda19b` |
| `--color-background-base-inverse-default` | `color.background.base-inverse.default` | `{palette.gray.900}` → `#1e1e1e` | `{palette.gray.100}` → `#f5f5f5` |
| `--color-background-base-inverse-default-active` | `color.background.base-inverse.default-active` | `{palette.gray.600}` → `#444444` | `{palette.gray.250}` → `#d9d9d9` |
| `--color-background-base-inverse-default-hover` | `color.background.base-inverse.default-hover` | `{palette.gray.700}` → `#383838` | `{palette.gray.200}` → `#f1f1f1` |
| `--color-background-disabled-default` | `color.background.disabled.default` | `{palette.gray.200}` → `#f1f1f1` | `{palette.gray.700}` → `#383838` |
| `--color-background-disabled-secondary` | `color.background.disabled.secondary` | `{palette.gray.250}` → `#d9d9d9` | `{palette.gray.400}` → `#757575` |
| `--color-background-alpha-overlay-dark` | `color.background.alpha.overlay-dark` | `{palette.alpha.black.400-alpha}` → `#12121266` | `{palette.alpha.black.200-alpha}` → `#1212121a` |
| `--color-background-alpha-overlay-light` | `color.background.alpha.overlay-light` | `{palette.alpha.white.400-alpha}` → `#ffffff66` | `{palette.alpha.white.200-alpha}` → `#ffffff1a` |
| `--color-background-alpha-large-surface` | `color.background.alpha.large-surface` | `{palette.alpha.gray.alpha-100}` → `#44444408` | `{palette.alpha.gray.alpha-200}` → `#4444441a` |
| `--color-border-base-default` | `color.border.base.default` | `{palette.gray.250}` → `#d9d9d9` | `{palette.gray.600}` → `#444444` |
| `--color-border-base-secondary` | `color.border.base.secondary` | `{palette.gray.300}` → `#b2b2b2` | `{palette.gray.400}` → `#757575` |
| `--color-border-base-tertiary` | `color.border.base.tertiary` | `{palette.gray.600}` → `#444444` | `{palette.gray.250}` → `#d9d9d9` |
| `--color-border-base-strong` | `color.border.base.strong` | `{palette.black.1000}` → `#121212` | `{palette.gray.100}` → `#f5f5f5` |
| `--color-border-base-subtle` | `color.border.base.subtle` | `{palette.white.1000}` → `#ffffff` | `{palette.black.1000}` → `#121212` |
| `--color-border-brand-default` | `color.border.brand.default` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.500}` → `#3379db` |
| `--color-border-disabled-default` | `color.border.disabled.default` | `{palette.gray.250}` → `#d9d9d9` | `{palette.gray.600}` → `#444444` |
| `--color-border-positive-default` | `color.border.positive.default` | `{palette.green.700}` → `#027948` | `{palette.green.500}` → `#35ad77` |
| `--color-border-warning-default` | `color.border.warning.default` | `{palette.apricot.600}` → `#dc6803` | `{palette.apricot.500}` → `#f79009` |
| `--color-border-danger-default` | `color.border.danger.default` | `{palette.red.600}` → `#d92d20` | `{palette.red.500}` → `#f04438` |
| `--color-text-base-default` | `color.text.base.default` | `{palette.black.1000}` → `#121212` | `{palette.white.1000}` → `#ffffff` |
| `--color-text-base-secondary` | `color.text.base.secondary` | `{palette.gray.700}` → `#383838` | `{palette.gray.200}` → `#f1f1f1` |
| `--color-text-base-tertiary` | `color.text.base.tertiary` | `{palette.gray.400}` → `#757575` | `{palette.gray.300}` → `#b2b2b2` |
| `--color-text-base-default-on-color` | `color.text.base.default-on-color` | `{palette.black.1000}` → `#121212` | `{palette.black.1000}` → `#121212` |
| `--color-text-base-secondary-on-color` | `color.text.base.secondary-on-color` | `{palette.gray.600}` → `#444444` | `{palette.gray.600}` → `#444444` |
| `--color-text-base-inverse-default` | `color.text.base-inverse.default` | `{palette.white.1000}` → `#ffffff` | `{palette.black.1000}` → `#121212` |
| `--color-text-base-inverse-on-color` | `color.text.base-inverse.on-color` | `{palette.white.1000}` → `#ffffff` | `{palette.white.1000}` → `#ffffff` |
| `--color-text-brand-default` | `color.text.brand.default` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.400}` → `#669be4` |
| `--color-text-brand-on-secondary` | `color.text.brand.on-secondary` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.700}` → `#0046a8` |
| `--color-text-brand-default-hover` | `color.text.brand.default-hover` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.700}` → `#0046a8` |
| `--color-text-brand-visited` | `color.text.brand.visited` | `{palette.magenta.600}` → `#aa18ce` | `{palette.magenta.400}` → `#cc74e2` |
| `--color-text-disabled-default` | `color.text.disabled.default` | `{palette.gray.300}` → `#b2b2b2` | `{palette.gray.600}` → `#444444` |
| `--color-text-disabled-on-disabled` | `color.text.disabled.on-disabled` | `{palette.gray.300}` → `#b2b2b2` | `{palette.gray.400}` → `#757575` |
| `--color-text-positive-default` | `color.text.positive.default` | `{palette.green.600}` → `#039855` | `{palette.green.500}` → `#35ad77` |
| `--color-text-positive-on-secondary` | `color.text.positive.on-secondary` | `{palette.green.700}` → `#027948` | `{palette.green.800}` → `#05603a` |
| `--color-text-warning-default` | `color.text.warning.default` | `{palette.apricot.700}` → `#b54708` | `{palette.apricot.500}` → `#f79009` |
| `--color-text-warning-on-secondary` | `color.text.warning.on-secondary` | `{palette.apricot.700}` → `#b54708` | `{palette.apricot.800}` → `#93370d` |
| `--color-text-danger-default` | `color.text.danger.default` | `{palette.red.600}` → `#d92d20` | `{palette.red.500}` → `#f04438` |
| `--color-text-danger-on-secondary` | `color.text.danger.on-secondary` | `{palette.red.700}` → `#b32318` | `{palette.red.800}` → `#912018` |
| `--color-icon-base-default` | `color.icon.base.default` | `{palette.black.1000}` → `#121212` | `{palette.white.1000}` → `#ffffff` |
| `--color-icon-base-secondary` | `color.icon.base.secondary` | `{palette.gray.600}` → `#444444` | `{palette.gray.200}` → `#f1f1f1` |
| `--color-icon-base-tertiary` | `color.icon.base.tertiary` | `{palette.gray.400}` → `#757575` | `{palette.gray.300}` → `#b2b2b2` |
| `--color-icon-base-default-on-color` | `color.icon.base.default-on-color` | `{palette.black.1000}` → `#121212` | `{palette.black.1000}` → `#121212` |
| `--color-icon-base-secondary-on-color` | `color.icon.base.secondary-on-color` | `{palette.gray.600}` → `#444444` | `{palette.gray.600}` → `#444444` |
| `--color-icon-brand-default` | `color.icon.brand.default` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.400}` → `#669be4` |
| `--color-icon-brand-on-secondary` | `color.icon.brand.on-secondary` | `{palette.blue-sky.600}` → `#0058d2` | `{palette.blue-sky.700}` → `#0046a8` |
| `--color-icon-brand-visited` | `color.icon.brand.visited` | `{palette.magenta.600}` → `#aa18ce` | `{palette.magenta.400}` → `#cc74e2` |
| `--color-icon-disabled-default` | `color.icon.disabled.default` | `{palette.gray.300}` → `#b2b2b2` | `{palette.gray.600}` → `#444444` |
| `--color-icon-disabled-on-disabled` | `color.icon.disabled.on-disabled` | `{palette.gray.300}` → `#b2b2b2` | `{palette.gray.400}` → `#757575` |
| `--color-icon-positive-default` | `color.icon.positive.default` | `{palette.green.600}` → `#039855` | `{palette.green.500}` → `#35ad77` |
| `--color-icon-positive-on-secondary` | `color.icon.positive.on-secondary` | `{palette.green.700}` → `#027948` | `{palette.green.800}` → `#05603a` |
| `--color-icon-warning-default` | `color.icon.warning.default` | `{palette.apricot.600}` → `#dc6803` | `{palette.apricot.500}` → `#f79009` |
| `--color-icon-warning-on-secondary` | `color.icon.warning.on-secondary` | `{palette.apricot.700}` → `#b54708` | `{palette.apricot.800}` → `#93370d` |
| `--color-icon-danger-default` | `color.icon.danger.default` | `{palette.red.600}` → `#d92d20` | `{palette.red.500}` → `#f04438` |
| `--color-icon-danger-on-secondary` | `color.icon.danger.on-secondary` | `{palette.red.700}` → `#b32318` | `{palette.red.800}` → `#912018` |
| `--color-icon-base-inverse-default` | `color.icon.base-inverse.default` | `{palette.white.1000}` → `#ffffff` | `{palette.black.1000}` → `#121212` |
| `--color-icon-base-inverse-on-color` | `color.icon.base-inverse.on-color` | `{palette.white.1000}` → `#ffffff` | `{palette.white.1000}` → `#ffffff` |

### 3.3 Tipografie — `tokens/core/font.tokens.json`

_32 tokens._

| Variabilă CSS | Token | Valoare |
| --- | --- | --- |
| `--font-family-primary` | `fontFamily.primary` | `Onest` |
| `--font-size-10` | `fontSize.10` | `10px` |
| `--font-size-12` | `fontSize.12` | `12px` |
| `--font-size-14` | `fontSize.14` | `14px` |
| `--font-size-16` | `fontSize.16` | `16px` |
| `--font-size-18` | `fontSize.18` | `18px` |
| `--font-size-20` | `fontSize.20` | `20px` |
| `--font-size-22` | `fontSize.22` | `22px` |
| `--font-size-24` | `fontSize.24` | `24px` |
| `--font-size-28` | `fontSize.28` | `28px` |
| `--font-size-32` | `fontSize.32` | `32px` |
| `--font-size-40` | `fontSize.40` | `40px` |
| `--font-size-48` | `fontSize.48` | `48px` |
| `--font-size-56` | `fontSize.56` | `56px` |
| `--font-size-64` | `fontSize.64` | `64px` |
| `--font-weight-regular` | `fontWeight.regular` | `400` |
| `--font-weight-medium` | `fontWeight.medium` | `500` |
| `--font-weight-semibold` | `fontWeight.semibold` | `600` |
| `--font-weight-bold` | `fontWeight.bold` | `700` |
| `--line-height-12` | `lineHeight.12` | `12px` |
| `--line-height-16` | `lineHeight.16` | `16px` |
| `--line-height-20` | `lineHeight.20` | `20px` |
| `--line-height-24` | `lineHeight.24` | `24px` |
| `--line-height-26` | `lineHeight.26` | `26px` |
| `--line-height-28` | `lineHeight.28` | `28px` |
| `--line-height-30` | `lineHeight.30` | `30px` |
| `--line-height-32` | `lineHeight.32` | `32px` |
| `--line-height-36` | `lineHeight.36` | `36px` |
| `--line-height-40` | `lineHeight.40` | `40px` |
| `--line-height-48` | `lineHeight.48` | `48px` |
| `--line-height-56` | `lineHeight.56` | `56px` |
| `--line-height-64` | `lineHeight.64` | `64px` |

### 3.4 Spații, radius, border-width — `tokens/core/sizes.tokens.json`

_31 tokens._

| Variabilă CSS | Token | Valoare |
| --- | --- | --- |
| `--spacing-0` | `spacing.0` | `0px` |
| `--spacing-2` | `spacing.2` | `2px` |
| `--spacing-4` | `spacing.4` | `4px` |
| `--spacing-6` | `spacing.6` | `6px` |
| `--spacing-8` | `spacing.8` | `8px` |
| `--spacing-12` | `spacing.12` | `12px` |
| `--spacing-16` | `spacing.16` | `16px` |
| `--spacing-20` | `spacing.20` | `20px` |
| `--spacing-24` | `spacing.24` | `24px` |
| `--spacing-32` | `spacing.32` | `32px` |
| `--spacing-40` | `spacing.40` | `40px` |
| `--spacing-48` | `spacing.48` | `48px` |
| `--spacing-56` | `spacing.56` | `56px` |
| `--spacing-64` | `spacing.64` | `64px` |
| `--spacing-80` | `spacing.80` | `80px` |
| `--spacing-96` | `spacing.96` | `96px` |
| `--spacing-120` | `spacing.120` | `120px` |
| `--border-radius-0` | `borderRadius.0` | `0px` |
| `--border-radius-4` | `borderRadius.4` | `4px` |
| `--border-radius-6` | `borderRadius.6` | `6px` |
| `--border-radius-8` | `borderRadius.8` | `8px` |
| `--border-radius-12` | `borderRadius.12` | `12px` |
| `--border-radius-16` | `borderRadius.16` | `16px` |
| `--border-radius-24` | `borderRadius.24` | `24px` |
| `--border-radius-32` | `borderRadius.32` | `32px` |
| `--border-radius-full` | `borderRadius.full` | `9999px` |
| `--border-width-1` | `borderWidth.1` | `1px` |
| `--border-width-2` | `borderWidth.2` | `2px` |
| `--border-width-3` | `borderWidth.3` | `3px` |
| `--border-width-1-5` | `borderWidth.1-5` | `1.5px` |
| `--border-width-0-5` | `borderWidth.0-5` | `0.5px` |

### 3.5 Umbre — `tokens/core/effects.tokens.json`

_5 tokens._

| Variabilă CSS | Token | Valoare |
| --- | --- | --- |
| `--drop-shadow-100` | `dropShadow.100` | `0px 2px 8px 0px rgba(19, 22, 29, 0.06), 0px 4px 8px 1px rgba(19, 22, 29, 0.04)` |
| `--drop-shadow-200` | `dropShadow.200` | `0px 4px 8px 2px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.04)` |
| `--drop-shadow-300` | `dropShadow.300` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` |
| `--drop-shadow-400` | `dropShadow.400` | `0px 12px 16px 6px rgba(40, 46, 55, 0.06), 0px 4px 6px 0px rgba(40, 46, 55, 0.06)` |
| `--drop-shadow-500` | `dropShadow.500` | `0px 16px 20px 6px rgba(40, 46, 55, 0.06), 0px 4px 8px 0px rgba(40, 46, 55, 0.06)` |

### 3.6 Inel de focus — `tokens/core/focusRing.tokens.json`

_4 tokens._

| Variabilă CSS | Token | Light | Dark |
| --- | --- | --- | --- |
| `--focus-ring-color-inner` | `focusRing.color.inner` | `{color.text.base-inverse.on-color}` → `#ffffff` | `{color.text.base-inverse.on-color}` → `#ffffff` |
| `--focus-ring-color-outer` | `focusRing.color.outer` | `{color.background.brand.focus-ring}` → `#3379db` | `{color.background.brand.focus-ring}` → `#3379db` |
| `--focus-ring-width-inner` | `focusRing.width.inner` | `2px` | `2px` |
| `--focus-ring-width-outer` | `focusRing.width.outer` | `3px` | `3px` |

### 3.7 Breakpoint-uri — `tokens/core/screen.tokens.json`

_16 tokens._

| Variabilă CSS | Token | Valoare |
| --- | --- | --- |
| `--screen-width-fixed-desktop` | `screen.width.fixed.desktop` | `1440px` |
| `--screen-width-fixed-laptop` | `screen.width.fixed.laptop` | `1280px` |
| `--screen-width-fixed-tablet` | `screen.width.fixed.tablet` | `768px` |
| `--screen-width-fixed-mobile` | `screen.width.fixed.mobile` | `360px` |
| `--screen-width-min-fluid-desktop` | `screen.width.min-fluid.desktop` | `1440px` |
| `--screen-width-min-fluid-laptop` | `screen.width.min-fluid.laptop` | `1024px` |
| `--screen-width-min-fluid-tablet` | `screen.width.min-fluid.tablet` | `480px` |
| `--screen-width-min-fluid-mobile` | `screen.width.min-fluid.mobile` | `320px` |
| `--screen-width-max-fluid-desktop` | `screen.width.max-fluid.desktop` | `1920px` |
| `--screen-width-max-fluid-laptop` | `screen.width.max-fluid.laptop` | `1440px` |
| `--screen-width-max-fluid-tablet` | `screen.width.max-fluid.tablet` | `1024px` |
| `--screen-width-max-fluid-mobile` | `screen.width.max-fluid.mobile` | `480px` |
| `--screen-height-fixed-desktop` | `screen.height.fixed.desktop` | `1024px` |
| `--screen-height-fixed-laptop` | `screen.height.fixed.laptop` | `832px` |
| `--screen-height-fixed-tablet` | `screen.height.fixed.tablet` | `1024px` |
| `--screen-height-fixed-mobile` | `screen.height.fixed.mobile` | `800px` |

---

## 4. Tabel — `mud-table`

### 4.1 Cum se mapează props-urile pe tokens

`src/components/mud-table/mud-table.css` (315 linii) leagă fiecare prop de un grup de tokens.
Asta îți spune ce variabilă să atingi ca să schimbi un anumit aspect.

| Prop | Valoare | Tokens activați |
| --- | --- | --- |
| `headerStyle` | `default` | `--table-header-default-background` / `-color` / `-border-bottom` |
| | `inverted` | `--table-header-inverted-*` + `--table-sort-icon-color-inverted` (cu `opacity: .7` pe iconul inactiv, hardcodat în CSS) |
| | `white` | `--table-header-white-background` — **doar fundalul**; vezi §4.2 |
| `rowStyle` | `divided` (implicit) | `--table-cell-border-bottom` pe fiecare `.td` |
| | `zebra` | `--table-row-background-zebra` pe rândurile impare, fără linii despărțitoare |
| | `borderless` | niciun token de bordură — rânduri plate |
| `hoverable` | `true` | `--table-row-background-hover`, cu tranziția `--table-row-transition-duration` / `-timing-function` |
| `selectable` | `true` | `--table-selection-column-width` pe coloana de checkbox; padding-ul ei vine din `--spacing-16`, nu dintr-un token de tabel |
| rând selectat | — | `--table-row-background-selected`; la hover, `--table-row-background-selected-hover` |
| `rows` gol | — | `--table-empty-padding` / `-color` / `-font-size` |

**Sortare pe coloane.** Un antet cu `sortable: true` devine focusabil și se comportă ca un buton:

- iconul folosește `--table-sort-icon-size` (16px) și trece de la
  `--table-sort-icon-color-inactive` la `--table-sort-icon-color-active` — la hover pe antet și
  când coloana e sortată activ;
- focusul desenează `outline: var(--table-focus-ring-width) solid var(--table-focus-ring-color)`
  cu `outline-offset: calc(-1 * var(--table-focus-ring-offset))` — offset negativ, ca inelul să
  rămână în interiorul celulei;
- `aria-sort` este setat de componentă; `disableSort` dezactivează sortarea pentru tot tabelul,
  ignorând flagul `sortable` al fiecărei coloane.

**Lățimea coloanelor** nu are token. Se trimite prin proprietatea inline `--col-width`, generată
de `columnWidthStyle()` din valoarea `width` a definiției de coloană; CSS-ul aplică
`width: var(--col-width, auto)` pe `.th`. Coloana de selecție e fixă la
`--table-selection-column-width` = 56px (16 + 24 + 16: padding + caseta checkbox + padding).

**Comportamentul responsiv** se face prin container query, nu prin media query:

```css
:host { container-type: inline-size; container-name: mud-table; }

@container mud-table (max-width: 640px) {
  .th { padding-inline: var(--table-header-padding-inline-mobile); }  /* 24 → 16 */
  .td { padding-inline: var(--table-cell-padding-inline-mobile); }
}
```

Pragul `640px` apare ca literal în CSS pentru că **container queries nu pot citi variabile CSS**.
Tokenul `--table-mobile-breakpoint` există exact ca să documenteze valoarea într-un singur loc,
dar nu poate fi consumat de regula `@container` — dacă îl schimbi, trebuie să schimbi și
literalul din CSS.

### 4.2 Ce lipsește sau e inconsistent

**Patru tokens definiți dar neconsumați** de `mud-table.css`:

| Token | De ce |
| --- | --- |
| `--table-header-white-color` | `.table--header-white .th` nu suprascrie culoarea textului — rămâne pe `--table-header-default-color`. Valorile coincid (`{color.text.base.default}`), deci vizual nu se vede, dar tokenul e mort. |
| `--table-header-white-border-bottom` | Idem, pentru bordura de jos. |
| `--table-cell-gap` | Celulele nu folosesc flex gap; doar antetul are `.th-inner { gap: var(--table-header-gap) }`. |
| `--table-mobile-breakpoint` | Documentar — vezi mai sus. |

**Valori hardcodate în CSS**, fără token:

- `border-bottom: 1px solid …` la antet și la celule — lățimea de 1px nu are token
  (`--border-width-1` există în fundamente, dar nu e folosit aici);
- `opacity: 0.7` pe iconul de sortare inactiv în varianta `inverted`;
- `--spacing-16` folosit direct pentru padding-ul coloanei de selecție, în loc de un token
  `table.selection.paddingInline`.

**Ce nu există deloc**: tokens pentru rând dezactivat, pentru coloană fixată (sticky), pentru
antet lipicios la scroll, pentru rând expandabil sau pentru densitate (compact / confortabil).
Tabelul are o singură densitate: `minHeight` 48px pentru antet și celule.

### 4.3 Tokens — `tokens/core/components/table.tokens.json`

_49 tokens._

**`table.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-container-background` | `table.container.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--table-container-border-radius` | `table.container.borderRadius` | `{borderRadius.0}` | `0px` | = light |
| `--table-container-font-family` | `table.container.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |

**`table.header`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-header-min-height` | `table.header.minHeight` | `{spacing.48}` | `48px` | = light |
| `--table-header-padding-inline-desktop` | `table.header.paddingInline.desktop` | `{spacing.24}` | `24px` | = light |
| `--table-header-padding-inline-mobile` | `table.header.paddingInline.mobile` | `{spacing.16}` | `16px` | = light |
| `--table-header-padding-block` | `table.header.paddingBlock` | `{spacing.8}` | `8px` | = light |
| `--table-header-gap` | `table.header.gap` | `{spacing.6}` | `6px` | = light |
| `--table-header-font-weight` | `table.header.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--table-header-font-size` | `table.header.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--table-header-line-height` | `table.header.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--table-header-default-background` | `table.header.default.background` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--table-header-default-color` | `table.header.default.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--table-header-default-border-bottom` | `table.header.default.borderBottom` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--table-header-inverted-background` | `table.header.inverted.background` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--table-header-inverted-color` | `table.header.inverted.color` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--table-header-inverted-border-bottom` | `table.header.inverted.borderBottom` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--table-header-white-background` | `table.header.white.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--table-header-white-color` | `table.header.white.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--table-header-white-border-bottom` | `table.header.white.borderBottom` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |

**`table.cell`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-cell-min-height` | `table.cell.minHeight` | `{spacing.48}` | `48px` | = light |
| `--table-cell-padding-inline-desktop` | `table.cell.paddingInline.desktop` | `{spacing.24}` | `24px` | = light |
| `--table-cell-padding-inline-mobile` | `table.cell.paddingInline.mobile` | `{spacing.16}` | `16px` | = light |
| `--table-cell-padding-block` | `table.cell.paddingBlock` | `{spacing.8}` | `8px` | = light |
| `--table-cell-gap` | `table.cell.gap` | `{spacing.6}` | `6px` | = light |
| `--table-cell-font-weight` | `table.cell.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--table-cell-font-size` | `table.cell.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--table-cell-line-height` | `table.cell.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--table-cell-color` | `table.cell.color` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--table-cell-border-bottom` | `table.cell.borderBottom` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |

**`table.row`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-row-background-default` | `table.row.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--table-row-background-zebra` | `table.row.background.zebra` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--table-row-background-hover` | `table.row.background.hover` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--table-row-background-selected` | `table.row.background.selected` | `{color.background.brand.secondary}` → `{palette.blue-sky.100}` | `#e8f0fb` | `#ccdef6` |
| `--table-row-background-selected-hover` | `table.row.background.selectedHover` | `{color.background.brand.secondary-hover}` → `{palette.blue-sky.200}` | `#ccdef6` | `#99bced` |
| `--table-row-transition-duration` | `table.row.transitionDuration` | — | `120ms` | = light |
| `--table-row-transition-timing-function` | `table.row.transitionTimingFunction` | — | `ease-out` | = light |

**`table.sortIcon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-sort-icon-size` | `table.sortIcon.size` | `{spacing.16}` | `16px` | = light |
| `--table-sort-icon-color-inactive` | `table.sortIcon.color.inactive` | `{color.icon.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--table-sort-icon-color-active` | `table.sortIcon.color.active` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--table-sort-icon-color-inverted` | `table.sortIcon.color.inverted` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |

**`table.focusRing`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-focus-ring-width` | `table.focusRing.width` | `{borderWidth.3}` | `3px` | = light |
| `--table-focus-ring-offset` | `table.focusRing.offset` | `{borderWidth.1}` | `1px` | = light |
| `--table-focus-ring-color` | `table.focusRing.color` | `{color.background.brand.focus-ring}` → `{palette.blue-sky.500}` | `#3379db` | = light |

**`table.selection`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-selection-column-width` | `table.selection.columnWidth` | `{spacing.56}` | `56px` | = light |

**`table.mobile`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-mobile-breakpoint` | `table.mobile.breakpoint` | — | `640px` | = light |

**`table.empty`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--table-empty-padding` | `table.empty.padding` | `{spacing.48}` | `48px` | = light |
| `--table-empty-color` | `table.empty.color` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--table-empty-font-size` | `table.empty.fontSize` | `{fontSize.14}` | `14px` | = light |

Note din sursă (`$comment`):

- `--table-header-padding-inline-desktop` — Per Figma table-header master (4930:14358) Desktop breakpoint.
- `--table-header-padding-inline-mobile` — Per Figma table-header master (4930:14358) Mobile breakpoint.
- `--table-header-white-background` — Per Figma 4946:23845 (Header Styles / white) — header background matches body. The bottom border is the only delimiter.
- `--table-cell-padding-inline-desktop` — Per Figma table-cell master (649:4296) Desktop breakpoint.
- `--table-cell-padding-inline-mobile` — Per Figma table-cell master (649:4296) Mobile breakpoint.
- `--table-selection-column-width` — Fixed width of the leading checkbox column when `selectable` is set. 56px = 16+24+16 (paddingInline + checkbox box + paddingInline).
- `--table-mobile-breakpoint` — Below this container-query width the table inline padding shrinks per Figma's Mobile breakpoint (24 → 16). Documented as a token for single-source-of-truth; container queries can't read CSS variables, so the literal lives in the CSS file as well.

---

## 5. Compoziția tabelului — selecție, conținut de celulă, paginare

`mud-table` randează singur doar structura: antet, celule, rânduri, starea goală. Restul îl
compune sau îl primește prin sloturi.

- **Coloana de selecție** — `mud-checkbox` este randat intern de `mud-table` când `selectable`
  este activ, inclusiv checkbox-ul „selectează tot” din antet. Tokenii lui `checkbox.*` de mai
  jos controlează caseta, bifa, starea indeterminată și inelul de focus.
- **Conținutul celulelor** — prin sloturile `cell-{key}` și `header-cell-{key}` proiectezi orice:
  `mud-tag` pentru status, `mud-button` pentru acțiuni pe rând, sau markup propriu. Consumatorul
  e responsabil să furnizeze câte un element slotat per rând, potrivit în ordinea din `rows`.
  Textul simplu de celulă e trunchiat cu elipsă (`.cell-text`, `text-overflow: ellipsis`).
- **Paginarea** — `mud-pagination` este o componentă separată, pe care o pui sub tabel. Nu are
  nicio legătură automată cu `mud-table`; sincronizarea stării o faci tu.
- **Tooltip** — `mud-tooltip` pentru explicații pe antet sau pe valori trunchiate.

### Checkbox (coloana de selecție) — `tokens/core/components/checkbox.tokens.json`

_50 tokens._

**`checkbox.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-container-size-sm` | `checkbox.container.size.sm` | `{spacing.20}` | `20px` | = light |
| `--checkbox-container-size-md` | `checkbox.container.size.md` | `{spacing.24}` | `24px` | = light |
| `--checkbox-container-touch-target-sm` | `checkbox.container.touchTarget.sm` | — | `36px` | = light |
| `--checkbox-container-touch-target-md` | `checkbox.container.touchTarget.md` | — | `36px` | = light |
| `--checkbox-container-border-radius-sm` | `checkbox.container.borderRadius.sm` | `{borderRadius.6}` | `6px` | = light |
| `--checkbox-container-border-radius-md` | `checkbox.container.borderRadius.md` | `{borderRadius.6}` | `6px` | = light |
| `--checkbox-container-border-width` | `checkbox.container.borderWidth` | `{borderWidth.2}` | `2px` | = light |
| `--checkbox-container-focus-ring-width` | `checkbox.container.focusRingWidth` | `{borderWidth.3}` | `3px` | = light |
| `--checkbox-container-focus-ring-offset` | `checkbox.container.focusRingOffset` | `{borderWidth.1}` | `1px` | = light |
| `--checkbox-container-gap-sm` | `checkbox.container.gap.sm` | `{spacing.6}` | `6px` | = light |
| `--checkbox-container-gap-md` | `checkbox.container.gap.md` | `{spacing.12}` | `12px` | = light |
| `--checkbox-container-transition-duration` | `checkbox.container.transitionDuration` | — | `150ms` | = light |
| `--checkbox-container-transition-timing-function` | `checkbox.container.transitionTimingFunction` | — | `ease-out` | = light |

**`checkbox.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-icon-size-sm` | `checkbox.icon.size.sm` | `{spacing.12}` | `12px` | = light |
| `--checkbox-icon-size-md` | `checkbox.icon.size.md` | `{spacing.16}` | `16px` | = light |
| `--checkbox-icon-color-default` | `checkbox.icon.color.default` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--checkbox-icon-color-disabled` | `checkbox.icon.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`checkbox.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-label-font-family` | `checkbox.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--checkbox-label-font-weight` | `checkbox.label.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--checkbox-label-font-size-sm` | `checkbox.label.fontSize.sm` | `{fontSize.14}` | `14px` | = light |
| `--checkbox-label-font-size-md` | `checkbox.label.fontSize.md` | `{fontSize.16}` | `16px` | = light |
| `--checkbox-label-line-height-sm` | `checkbox.label.lineHeight.sm` | `{lineHeight.20}` | `20px` | = light |
| `--checkbox-label-line-height-md` | `checkbox.label.lineHeight.md` | `{lineHeight.24}` | `24px` | = light |
| `--checkbox-label-color-default` | `checkbox.label.color.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--checkbox-label-color-disabled` | `checkbox.label.color.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`checkbox.text`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-text-gap` | `checkbox.text.gap` | `{spacing.2}` | `2px` | = light |

**`checkbox.supporting`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-supporting-font-family` | `checkbox.supporting.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--checkbox-supporting-font-weight` | `checkbox.supporting.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--checkbox-supporting-font-size-sm` | `checkbox.supporting.fontSize.sm` | `{fontSize.12}` | `12px` | = light |
| `--checkbox-supporting-font-size-md` | `checkbox.supporting.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--checkbox-supporting-line-height-sm` | `checkbox.supporting.lineHeight.sm` | `{lineHeight.16}` | `16px` | = light |
| `--checkbox-supporting-line-height-md` | `checkbox.supporting.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--checkbox-supporting-color-default` | `checkbox.supporting.color.default` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--checkbox-supporting-color-disabled` | `checkbox.supporting.color.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`checkbox.default`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-default-background-unchecked` | `checkbox.default.background.unchecked` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--checkbox-default-background-checked` | `checkbox.default.background.checked` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--checkbox-default-border-unchecked` | `checkbox.default.border.unchecked` | `{color.border.base.secondary}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--checkbox-default-border-checked` | `checkbox.default.border.checked` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--checkbox-default-focus-ring` | `checkbox.default.focusRing` | `{color.background.brand.focus-ring}` → `{palette.blue-sky.500}` | `#3379db` | = light |

**`checkbox.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-destructive-background-unchecked` | `checkbox.destructive.background.unchecked` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--checkbox-destructive-background-checked` | `checkbox.destructive.background.checked` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--checkbox-destructive-border-unchecked` | `checkbox.destructive.border.unchecked` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--checkbox-destructive-border-checked` | `checkbox.destructive.border.checked` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--checkbox-destructive-focus-ring` | `checkbox.destructive.focusRing` | `{color.background.brand.focus-ring}` → `{palette.blue-sky.500}` | `#3379db` | = light |
| `--checkbox-destructive-label` | `checkbox.destructive.label` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--checkbox-destructive-supporting` | `checkbox.destructive.supporting` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`checkbox.disabled`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--checkbox-disabled-background-unchecked` | `checkbox.disabled.background.unchecked` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--checkbox-disabled-background-checked` | `checkbox.disabled.background.checked` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--checkbox-disabled-border-unchecked` | `checkbox.disabled.border.unchecked` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--checkbox-disabled-border-checked` | `checkbox.disabled.border.checked` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |

Note din sursă (`$comment`):

- `--checkbox-container-touch-target-sm` — WCAG/Figma coarse-pointer minimum 36×36 (Figma 2822:1227/1230 Target). No spacing step maps to 36 (scale jumps 32→40), so a literal is used like transitionDuration below.
- `--checkbox-container-touch-target-md` — WCAG/Figma coarse-pointer minimum 36×36 (Figma 2822:1230 Target).
- `--checkbox-container-border-radius-sm` — Both sizes share radius-6 per Figma 403:21816 (sm 20px box also binds --border-radius-6).
- `--checkbox-default-border-unchecked` — #b2b2b2 light-gray unchecked border per Figma 403:21808 (was base.tertiary #444, too dark).

### Tag (status în celulă) — `tokens/core/components/tag.tokens.json`

_59 tokens._

**`tag.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tag-container-height-md` | `tag.container.height.md` | `{spacing.24}` | `24px` | = light |
| `--tag-container-height-sm` | `tag.container.height.sm` | `{spacing.20}` | `20px` | = light |
| `--tag-container-min-width-md` | `tag.container.minWidth.md` | `{spacing.32}` | `32px` | = light |
| `--tag-container-min-width-sm` | `tag.container.minWidth.sm` | `{spacing.32}` | `32px` | = light |
| `--tag-container-padding-inline-md` | `tag.container.paddingInline.md` | `{spacing.8}` | `8px` | = light |
| `--tag-container-padding-inline-sm` | `tag.container.paddingInline.sm` | `{spacing.6}` | `6px` | = light |
| `--tag-container-padding-inline-icon-md` | `tag.container.paddingInlineIcon.md` | `{spacing.6}` | `6px` | = light |
| `--tag-container-padding-inline-icon-sm` | `tag.container.paddingInlineIcon.sm` | `{spacing.4}` | `4px` | = light |
| `--tag-container-gap-md` | `tag.container.gap.md` | `{spacing.4}` | `4px` | = light |
| `--tag-container-gap-sm` | `tag.container.gap.sm` | `{spacing.4}` | `4px` | = light |
| `--tag-container-border-radius` | `tag.container.borderRadius` | `{borderRadius.4}` | `4px` | = light |
| `--tag-container-border-width` | `tag.container.borderWidth` | — | `1px` | = light |
| `--tag-container-group-gap` | `tag.container.groupGap` | `{spacing.8}` | `8px` | = light |

**`tag.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tag-label-font-family` | `tag.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--tag-label-font-size-md` | `tag.label.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--tag-label-font-size-sm` | `tag.label.fontSize.sm` | `{fontSize.12}` | `12px` | = light |
| `--tag-label-line-height-md` | `tag.label.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--tag-label-line-height-sm` | `tag.label.lineHeight.sm` | `{lineHeight.16}` | `16px` | = light |
| `--tag-label-font-weight-status` | `tag.label.fontWeightStatus` | `{fontWeight.medium}` | `500` | = light |
| `--tag-label-font-weight-info` | `tag.label.fontWeightInfo` | `{fontWeight.regular}` | `400` | = light |

**`tag.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tag-icon-size-md` | `tag.icon.size.md` | `{spacing.16}` | `16px` | = light |
| `--tag-icon-size-sm` | `tag.icon.size.sm` | `{spacing.12}` | `12px` | = light |

**`tag.subtle`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tag-subtle-background-muted` | `tag.subtle.background.muted` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--tag-subtle-background-neutral` | `tag.subtle.background.neutral` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--tag-subtle-background-accent` | `tag.subtle.background.accent` | `{color.background.warning.secondary}` → `{palette.apricot.100}` | `#feefc6` | `#fedf89` |
| `--tag-subtle-background-success` | `tag.subtle.background.success` | `{color.background.positive.secondary}` → `{palette.green.100}` | `#e6f5ee` | `#cdeadd` |
| `--tag-subtle-background-brand` | `tag.subtle.background.brand` | `{color.background.brand.secondary}` → `{palette.blue-sky.100}` | `#e8f0fb` | `#ccdef6` |
| `--tag-subtle-background-danger` | `tag.subtle.background.danger` | `{color.background.danger.secondary}` → `{palette.red.100}` | `#fee4e2` | `#fecdc9` |
| `--tag-subtle-label-muted` | `tag.subtle.label.muted` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--tag-subtle-label-neutral` | `tag.subtle.label.neutral` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--tag-subtle-label-accent` | `tag.subtle.label.accent` | `{color.text.warning.on-secondary}` → `{palette.apricot.700}` | `#b54708` | `#93370d` |
| `--tag-subtle-label-success` | `tag.subtle.label.success` | `{color.text.positive.on-secondary}` → `{palette.green.700}` | `#027948` | `#05603a` |
| `--tag-subtle-label-brand` | `tag.subtle.label.brand` | `{color.text.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--tag-subtle-label-danger` | `tag.subtle.label.danger` | `{color.text.danger.on-secondary}` → `{palette.red.700}` | `#b32318` | `#912018` |

**`tag.strong`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tag-strong-background-muted` | `tag.strong.background.muted` | `{color.background.base-inverse.default-active}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--tag-strong-background-neutral` | `tag.strong.background.neutral` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--tag-strong-background-accent` | `tag.strong.background.accent` | `{color.background.warning.accent}` → `{palette.apricot.300}` | `#fec84b` | `#fdb022` |
| `--tag-strong-background-success` | `tag.strong.background.success` | `{color.background.positive.default-hover}` → `{palette.green.700}` | `#027948` | `#05603a` |
| `--tag-strong-background-brand` | `tag.strong.background.brand` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--tag-strong-background-danger` | `tag.strong.background.danger` | `{color.background.danger.default}` → `{palette.red.600}` | `#d92d20` | `#b32318` |
| `--tag-strong-label-muted` | `tag.strong.label.muted` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--tag-strong-label-neutral` | `tag.strong.label.neutral` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--tag-strong-label-accent` | `tag.strong.label.accent` | `{color.text.base.default-on-color}` → `{palette.black.1000}` | `#121212` | = light |
| `--tag-strong-label-success` | `tag.strong.label.success` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--tag-strong-label-brand` | `tag.strong.label.brand` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--tag-strong-label-danger` | `tag.strong.label.danger` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |

**`tag.outlined`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tag-outlined-background` | `tag.outlined.background` | — | `transparent` | = light |
| `--tag-outlined-border-muted` | `tag.outlined.border.muted` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--tag-outlined-border-neutral` | `tag.outlined.border.neutral` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--tag-outlined-border-accent` | `tag.outlined.border.accent` | `{color.border.warning.default}` → `{palette.apricot.600}` | `#dc6803` | `#f79009` |
| `--tag-outlined-border-success` | `tag.outlined.border.success` | `{color.border.positive.default}` → `{palette.green.700}` | `#027948` | `#35ad77` |
| `--tag-outlined-border-brand` | `tag.outlined.border.brand` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--tag-outlined-border-danger` | `tag.outlined.border.danger` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--tag-outlined-label-muted` | `tag.outlined.label.muted` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--tag-outlined-label-neutral` | `tag.outlined.label.neutral` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--tag-outlined-label-accent` | `tag.outlined.label.accent` | `{color.text.warning.on-secondary}` → `{palette.apricot.700}` | `#b54708` | `#93370d` |
| `--tag-outlined-label-success` | `tag.outlined.label.success` | `{color.text.positive.on-secondary}` → `{palette.green.700}` | `#027948` | `#05603a` |
| `--tag-outlined-label-brand` | `tag.outlined.label.brand` | `{color.text.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--tag-outlined-label-danger` | `tag.outlined.label.danger` | `{color.text.danger.on-secondary}` → `{palette.red.700}` | `#b32318` | `#912018` |

Note din sursă (`$comment`):

- `--tag-strong-background-success` — Darker green (#027948) instead of positive.default (#039855): white on-color label is 3.73:1 on the lighter green (fails WCAG AA at 14px medium) but ~5.5:1 here. Matches mud-notification / mud-badge.

### Buton (acțiuni pe rând) — `tokens/core/components/button.tokens.json`

_150 tokens._

**`button.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-container-height-sm` | `button.container.height.sm` | `{spacing.32}` | `32px` | = light |
| `--button-container-height-md` | `button.container.height.md` | `{spacing.40}` | `40px` | = light |
| `--button-container-height-lg` | `button.container.height.lg` | `{spacing.48}` | `48px` | = light |
| `--button-container-touch-target-sm` | `button.container.touchTarget.sm` | `{spacing.40}` | `40px` | = light |
| `--button-container-touch-target-md` | `button.container.touchTarget.md` | `{spacing.48}` | `48px` | = light |
| `--button-container-touch-target-lg` | `button.container.touchTarget.lg` | `{spacing.48}` | `48px` | = light |
| `--button-container-max-width` | `button.container.maxWidth` | — | `400px` | = light |
| `--button-container-min-width-sm` | `button.container.minWidth.sm` | — | `52px` | = light |
| `--button-container-min-width-md` | `button.container.minWidth.md` | `{spacing.56}` | `56px` | = light |
| `--button-container-min-width-lg` | `button.container.minWidth.lg` | — | `72px` | = light |
| `--button-container-padding-inline-sm` | `button.container.paddingInline.sm` | `{spacing.12}` | `12px` | = light |
| `--button-container-padding-inline-md` | `button.container.paddingInline.md` | `{spacing.16}` | `16px` | = light |
| `--button-container-padding-inline-lg` | `button.container.paddingInline.lg` | `{spacing.20}` | `20px` | = light |
| `--button-container-padding-inline-icon-sm` | `button.container.paddingInlineIcon.sm` | `{spacing.8}` | `8px` | = light |
| `--button-container-padding-inline-icon-md` | `button.container.paddingInlineIcon.md` | `{spacing.12}` | `12px` | = light |
| `--button-container-padding-inline-icon-lg` | `button.container.paddingInlineIcon.lg` | `{spacing.16}` | `16px` | = light |
| `--button-container-gap-sm` | `button.container.gap.sm` | `{spacing.6}` | `6px` | = light |
| `--button-container-gap-md` | `button.container.gap.md` | `{spacing.6}` | `6px` | = light |
| `--button-container-gap-lg` | `button.container.gap.lg` | `{spacing.6}` | `6px` | = light |
| `--button-container-border-radius-rectangular-sm` | `button.container.borderRadius.rectangular.sm` | `{borderRadius.6}` | `6px` | = light |
| `--button-container-border-radius-rectangular-md` | `button.container.borderRadius.rectangular.md` | `{borderRadius.6}` | `6px` | = light |
| `--button-container-border-radius-rectangular-lg` | `button.container.borderRadius.rectangular.lg` | `{borderRadius.8}` | `8px` | = light |
| `--button-container-border-radius-circular-sm` | `button.container.borderRadius.circular.sm` | `{borderRadius.full}` | `9999px` | = light |
| `--button-container-border-radius-circular-md` | `button.container.borderRadius.circular.md` | `{borderRadius.full}` | `9999px` | = light |
| `--button-container-border-radius-circular-lg` | `button.container.borderRadius.circular.lg` | `{borderRadius.full}` | `9999px` | = light |
| `--button-container-transition-duration` | `button.container.transitionDuration` | — | `150ms` | = light |
| `--button-container-transition-timing-function` | `button.container.transitionTimingFunction` | — | `ease-out` | = light |

**`button.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-label-font-family` | `button.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--button-label-font-size-sm` | `button.label.fontSize.sm` | `{fontSize.14}` | `14px` | = light |
| `--button-label-font-size-md` | `button.label.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--button-label-font-size-lg` | `button.label.fontSize.lg` | `{fontSize.16}` | `16px` | = light |
| `--button-label-line-height-sm` | `button.label.lineHeight.sm` | `{lineHeight.20}` | `20px` | = light |
| `--button-label-line-height-md` | `button.label.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--button-label-line-height-lg` | `button.label.lineHeight.lg` | `{lineHeight.24}` | `24px` | = light |
| `--button-label-font-weight` | `button.label.fontWeight` | `{fontWeight.medium}` | `500` | = light |

**`button.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-icon-size-sm` | `button.icon.size.sm` | `{spacing.16}` | `16px` | = light |
| `--button-icon-size-md` | `button.icon.size.md` | `{spacing.20}` | `20px` | = light |
| `--button-icon-size-lg` | `button.icon.size.lg` | `{spacing.20}` | `20px` | = light |

**`button.primary`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-primary-background-default` | `button.primary.background.default` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--button-primary-background-hover` | `button.primary.background.hover` | `{color.background.brand.default-hover}` → `{palette.blue-sky.700}` | `#0046a8` | = light |
| `--button-primary-background-active` | `button.primary.background.active` | `{color.background.brand.default-active}` → `{palette.blue-sky.800}` | `#00357e` | = light |
| `--button-primary-label` | `button.primary.label` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-primary-icon` | `button.primary.icon` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |

**`button.secondary`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-secondary-background-default` | `button.secondary.background.default` | `{color.background.brand.secondary}` → `{palette.blue-sky.100}` | `#e8f0fb` | `#ccdef6` |
| `--button-secondary-background-hover` | `button.secondary.background.hover` | `{color.background.brand.secondary-hover}` → `{palette.blue-sky.200}` | `#ccdef6` | `#99bced` |
| `--button-secondary-background-active` | `button.secondary.background.active` | `{color.background.brand.secondary-active}` → `{palette.blue-sky.300}` | `#99bced` | `#669be4` |
| `--button-secondary-label` | `button.secondary.label` | `{color.text.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--button-secondary-icon` | `button.secondary.icon` | `{color.icon.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |

**`button.strict`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-strict-background-default` | `button.strict.background.default` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--button-strict-background-hover` | `button.strict.background.hover` | `{color.background.base-inverse.default-hover}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--button-strict-background-active` | `button.strict.background.active` | `{color.background.base-inverse.default-active}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--button-strict-label` | `button.strict.label` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--button-strict-icon` | `button.strict.icon` | `{color.icon.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |

**`button.neutral`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-neutral-background-default` | `button.neutral.background.default` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--button-neutral-background-hover` | `button.neutral.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--button-neutral-background-active` | `button.neutral.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--button-neutral-label` | `button.neutral.label` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-neutral-icon` | `button.neutral.icon` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |

**`button.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-destructive-background-default` | `button.destructive.background.default` | `{color.background.danger.default}` → `{palette.red.600}` | `#d92d20` | `#b32318` |
| `--button-destructive-background-hover` | `button.destructive.background.hover` | `{color.background.danger.default-hover}` → `{palette.red.700}` | `#b32318` | `#912018` |
| `--button-destructive-background-active` | `button.destructive.background.active` | `{color.background.danger.default-active}` → `{palette.red.800}` | `#912018` | `#7a271a` |
| `--button-destructive-label` | `button.destructive.label` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-destructive-icon` | `button.destructive.icon` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |

**`button.disabled`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-disabled-background` | `button.disabled.background` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--button-disabled-label` | `button.disabled.label` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--button-disabled-icon` | `button.disabled.icon` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`button.outlined`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-outlined-primary-border-default` | `button.outlined.primary.border.default` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--button-outlined-primary-border-hover` | `button.outlined.primary.border.hover` | — | `transparent` | = light |
| `--button-outlined-primary-border-active` | `button.outlined.primary.border.active` | — | `transparent` | = light |
| `--button-outlined-primary-border-disabled` | `button.outlined.primary.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--button-outlined-primary-background-default` | `button.outlined.primary.background.default` | — | `transparent` | = light |
| `--button-outlined-primary-background-hover` | `button.outlined.primary.background.hover` | `{color.background.brand.default-hover}` → `{palette.blue-sky.700}` | `#0046a8` | = light |
| `--button-outlined-primary-background-active` | `button.outlined.primary.background.active` | `{color.background.brand.default-active}` → `{palette.blue-sky.800}` | `#00357e` | = light |
| `--button-outlined-primary-background-disabled` | `button.outlined.primary.background.disabled` | — | `transparent` | = light |
| `--button-outlined-primary-label-default` | `button.outlined.primary.label.default` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--button-outlined-primary-label-hover` | `button.outlined.primary.label.hover` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-primary-label-active` | `button.outlined.primary.label.active` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-primary-label-disabled` | `button.outlined.primary.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-outlined-primary-icon-default` | `button.outlined.primary.icon.default` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--button-outlined-primary-icon-hover` | `button.outlined.primary.icon.hover` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-primary-icon-active` | `button.outlined.primary.icon.active` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-primary-icon-disabled` | `button.outlined.primary.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-outlined-strict-border-default` | `button.outlined.strict.border.default` | `{color.border.base.strong}` → `{palette.black.1000}` | `#121212` | `#f5f5f5` |
| `--button-outlined-strict-border-hover` | `button.outlined.strict.border.hover` | — | `transparent` | = light |
| `--button-outlined-strict-border-active` | `button.outlined.strict.border.active` | — | `transparent` | = light |
| `--button-outlined-strict-border-disabled` | `button.outlined.strict.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--button-outlined-strict-background-default` | `button.outlined.strict.background.default` | — | `transparent` | = light |
| `--button-outlined-strict-background-hover` | `button.outlined.strict.background.hover` | `{color.background.base-inverse.default-hover}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--button-outlined-strict-background-active` | `button.outlined.strict.background.active` | `{color.background.base-inverse.default-active}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--button-outlined-strict-background-disabled` | `button.outlined.strict.background.disabled` | — | `transparent` | = light |
| `--button-outlined-strict-label-default` | `button.outlined.strict.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-outlined-strict-label-hover` | `button.outlined.strict.label.hover` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--button-outlined-strict-label-active` | `button.outlined.strict.label.active` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--button-outlined-strict-label-disabled` | `button.outlined.strict.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-outlined-strict-icon-default` | `button.outlined.strict.icon.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-outlined-strict-icon-hover` | `button.outlined.strict.icon.hover` | `{color.icon.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--button-outlined-strict-icon-active` | `button.outlined.strict.icon.active` | `{color.icon.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--button-outlined-strict-icon-disabled` | `button.outlined.strict.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-outlined-destructive-border-default` | `button.outlined.destructive.border.default` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-outlined-destructive-border-hover` | `button.outlined.destructive.border.hover` | — | `transparent` | = light |
| `--button-outlined-destructive-border-active` | `button.outlined.destructive.border.active` | — | `transparent` | = light |
| `--button-outlined-destructive-border-disabled` | `button.outlined.destructive.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--button-outlined-destructive-background-default` | `button.outlined.destructive.background.default` | — | `transparent` | = light |
| `--button-outlined-destructive-background-hover` | `button.outlined.destructive.background.hover` | `{color.background.danger.default-hover}` → `{palette.red.700}` | `#b32318` | `#912018` |
| `--button-outlined-destructive-background-active` | `button.outlined.destructive.background.active` | `{color.background.danger.default-active}` → `{palette.red.800}` | `#912018` | `#7a271a` |
| `--button-outlined-destructive-background-disabled` | `button.outlined.destructive.background.disabled` | — | `transparent` | = light |
| `--button-outlined-destructive-label-default` | `button.outlined.destructive.label.default` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-outlined-destructive-label-hover` | `button.outlined.destructive.label.hover` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-destructive-label-active` | `button.outlined.destructive.label.active` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-destructive-label-disabled` | `button.outlined.destructive.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-outlined-destructive-icon-default` | `button.outlined.destructive.icon.default` | `{color.icon.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-outlined-destructive-icon-hover` | `button.outlined.destructive.icon.hover` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-destructive-icon-active` | `button.outlined.destructive.icon.active` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--button-outlined-destructive-icon-disabled` | `button.outlined.destructive.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |

**`button.text`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--button-text-primary-background-default` | `button.text.primary.background.default` | — | `transparent` | = light |
| `--button-text-primary-background-hover` | `button.text.primary.background.hover` | `{color.background.brand.secondary-hover}` → `{palette.blue-sky.200}` | `#ccdef6` | `#99bced` |
| `--button-text-primary-background-active` | `button.text.primary.background.active` | `{color.background.brand.secondary-active}` → `{palette.blue-sky.300}` | `#99bced` | `#669be4` |
| `--button-text-primary-background-disabled` | `button.text.primary.background.disabled` | — | `transparent` | = light |
| `--button-text-primary-label-default` | `button.text.primary.label.default` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--button-text-primary-label-hover` | `button.text.primary.label.hover` | `{color.text.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--button-text-primary-label-active` | `button.text.primary.label.active` | `{color.text.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--button-text-primary-label-disabled` | `button.text.primary.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-text-primary-icon-default` | `button.text.primary.icon.default` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--button-text-primary-icon-hover` | `button.text.primary.icon.hover` | `{color.icon.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--button-text-primary-icon-active` | `button.text.primary.icon.active` | `{color.icon.brand.on-secondary}` → `{palette.blue-sky.600}` | `#0058d2` | `#0046a8` |
| `--button-text-primary-icon-disabled` | `button.text.primary.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-text-strict-background-default` | `button.text.strict.background.default` | — | `transparent` | = light |
| `--button-text-strict-background-hover` | `button.text.strict.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--button-text-strict-background-active` | `button.text.strict.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--button-text-strict-background-disabled` | `button.text.strict.background.disabled` | — | `transparent` | = light |
| `--button-text-strict-label-default` | `button.text.strict.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-text-strict-label-hover` | `button.text.strict.label.hover` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-text-strict-label-active` | `button.text.strict.label.active` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-text-strict-label-disabled` | `button.text.strict.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-text-strict-icon-default` | `button.text.strict.icon.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-text-strict-icon-hover` | `button.text.strict.icon.hover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-text-strict-icon-active` | `button.text.strict.icon.active` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--button-text-strict-icon-disabled` | `button.text.strict.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-text-destructive-background-default` | `button.text.destructive.background.default` | — | `transparent` | = light |
| `--button-text-destructive-background-hover` | `button.text.destructive.background.hover` | `{color.background.danger.secondary-hover}` → `{palette.red.200}` | `#fecdc9` | `#fda19b` |
| `--button-text-destructive-background-active` | `button.text.destructive.background.active` | `{color.background.danger.secondary-active}` → `{palette.red.300}` | `#fda19b` | `#f97066` |
| `--button-text-destructive-background-disabled` | `button.text.destructive.background.disabled` | — | `transparent` | = light |
| `--button-text-destructive-label-default` | `button.text.destructive.label.default` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-text-destructive-label-hover` | `button.text.destructive.label.hover` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-text-destructive-label-active` | `button.text.destructive.label.active` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-text-destructive-label-disabled` | `button.text.destructive.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--button-text-destructive-icon-default` | `button.text.destructive.icon.default` | `{color.icon.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-text-destructive-icon-hover` | `button.text.destructive.icon.hover` | `{color.icon.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-text-destructive-icon-active` | `button.text.destructive.icon.active` | `{color.icon.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--button-text-destructive-icon-disabled` | `button.text.destructive.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |

### Paginare — `tokens/core/components/pagination.tokens.json`

_81 tokens._

**`pagination.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--pagination-container-gap-sm` | `pagination.container.gap.sm` | `{spacing.4}` | `4px` | = light |
| `--pagination-container-gap-md` | `pagination.container.gap.md` | `{spacing.24}` | `24px` | = light |
| `--pagination-container-inner-gap-sm` | `pagination.container.innerGap.sm` | `{spacing.8}` | `8px` | = light |
| `--pagination-container-inner-gap-md` | `pagination.container.innerGap.md` | `{spacing.16}` | `16px` | = light |
| `--pagination-container-transition-duration` | `pagination.container.transitionDuration` | — | `150ms` | = light |
| `--pagination-container-transition-timing-function` | `pagination.container.transitionTimingFunction` | — | `ease-out` | = light |

**`pagination.item`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--pagination-item-size-sm` | `pagination.item.size.sm` | `{spacing.32}` | `32px` | = light |
| `--pagination-item-size-md` | `pagination.item.size.md` | `{spacing.40}` | `40px` | = light |
| `--pagination-item-border-radius` | `pagination.item.borderRadius` | `{borderRadius.4}` | `4px` | = light |
| `--pagination-item-border-width-default` | `pagination.item.borderWidth.default` | — | `0` | = light |
| `--pagination-item-border-width-focus` | `pagination.item.borderWidth.focus` | `{borderWidth.1-5}` | `1.5px` | = light |
| `--pagination-item-font-family` | `pagination.item.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--pagination-item-font-size` | `pagination.item.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--pagination-item-line-height` | `pagination.item.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--pagination-item-font-weight` | `pagination.item.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--pagination-item-unselected-background-default` | `pagination.item.unselected.background.default` | — | `transparent` | = light |
| `--pagination-item-unselected-background-hover` | `pagination.item.unselected.background.hover` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--pagination-item-unselected-background-active` | `pagination.item.unselected.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--pagination-item-unselected-label-default` | `pagination.item.unselected.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--pagination-item-unselected-label-hover` | `pagination.item.unselected.label.hover` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--pagination-item-unselected-label-active` | `pagination.item.unselected.label.active` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--pagination-item-unselected-border-default` | `pagination.item.unselected.border.default` | — | `transparent` | = light |
| `--pagination-item-unselected-border-focus` | `pagination.item.unselected.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--pagination-item-selected-background-default` | `pagination.item.selected.background.default` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--pagination-item-selected-background-hover` | `pagination.item.selected.background.hover` | `{color.background.brand.default-hover}` → `{palette.blue-sky.700}` | `#0046a8` | = light |
| `--pagination-item-selected-background-active` | `pagination.item.selected.background.active` | `{color.background.brand.default-active}` → `{palette.blue-sky.800}` | `#00357e` | = light |
| `--pagination-item-selected-label-default` | `pagination.item.selected.label.default` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--pagination-item-selected-label-hover` | `pagination.item.selected.label.hover` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--pagination-item-selected-label-active` | `pagination.item.selected.label.active` | `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--pagination-item-disabled-background` | `pagination.item.disabled.background` | — | `transparent` | = light |
| `--pagination-item-disabled-label` | `pagination.item.disabled.label` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |

**`pagination.ellipsis`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--pagination-ellipsis-size-sm` | `pagination.ellipsis.size.sm` | `{spacing.32}` | `32px` | = light |
| `--pagination-ellipsis-size-md` | `pagination.ellipsis.size.md` | `{spacing.40}` | `40px` | = light |
| `--pagination-ellipsis-label` | `pagination.ellipsis.label` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |

**`pagination.overflow`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--pagination-overflow-trigger-background-default` | `pagination.overflow.trigger.background.default` | — | `transparent` | = light |
| `--pagination-overflow-trigger-background-hover` | `pagination.overflow.trigger.background.hover` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--pagination-overflow-trigger-background-active` | `pagination.overflow.trigger.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--pagination-overflow-trigger-background-open` | `pagination.overflow.trigger.background.open` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--pagination-overflow-trigger-label` | `pagination.overflow.trigger.label` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--pagination-overflow-trigger-border-default` | `pagination.overflow.trigger.border.default` | — | `transparent` | = light |
| `--pagination-overflow-trigger-border-focus` | `pagination.overflow.trigger.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--pagination-overflow-menu-background` | `pagination.overflow.menu.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--pagination-overflow-menu-border-color` | `pagination.overflow.menu.border.color` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--pagination-overflow-menu-border-width` | `pagination.overflow.menu.border.width` | — | `1px` | = light |
| `--pagination-overflow-menu-border-radius` | `pagination.overflow.menu.border.radius` | `{borderRadius.6}` | `6px` | = light |
| `--pagination-overflow-menu-shadow` | `pagination.overflow.menu.shadow` | `{dropShadow.200}` | `0px 4px 8px 2px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.04)` | = light |
| `--pagination-overflow-menu-padding-inline` | `pagination.overflow.menu.padding.inline` | `{spacing.4}` | `4px` | = light |
| `--pagination-overflow-menu-padding-block` | `pagination.overflow.menu.padding.block` | `{spacing.4}` | `4px` | = light |
| `--pagination-overflow-menu-gap` | `pagination.overflow.menu.gap` | `{spacing.2}` | `2px` | = light |
| `--pagination-overflow-menu-min-width` | `pagination.overflow.menu.minWidth` | `{spacing.56}` | `56px` | = light |
| `--pagination-overflow-menu-offset` | `pagination.overflow.menu.offset` | `{spacing.4}` | `4px` | = light |
| `--pagination-overflow-menu-max-height` | `pagination.overflow.menu.maxHeight` | — | `264px` | = light |
| `--pagination-overflow-menu-item-size-sm` | `pagination.overflow.menuItem.size.sm` | `{spacing.32}` | `32px` | = light |
| `--pagination-overflow-menu-item-size-md` | `pagination.overflow.menuItem.size.md` | `{spacing.32}` | `32px` | = light |
| `--pagination-overflow-menu-item-padding-inline` | `pagination.overflow.menuItem.padding.inline` | `{spacing.12}` | `12px` | = light |
| `--pagination-overflow-menu-item-border-radius` | `pagination.overflow.menuItem.borderRadius` | `{borderRadius.4}` | `4px` | = light |
| `--pagination-overflow-menu-item-background-default` | `pagination.overflow.menuItem.background.default` | — | `transparent` | = light |
| `--pagination-overflow-menu-item-background-hover` | `pagination.overflow.menuItem.background.hover` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--pagination-overflow-menu-item-background-active` | `pagination.overflow.menuItem.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--pagination-overflow-menu-item-label` | `pagination.overflow.menuItem.label` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |

**`pagination.nav`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--pagination-nav-height-sm` | `pagination.nav.height.sm` | `{spacing.32}` | `32px` | = light |
| `--pagination-nav-height-md` | `pagination.nav.height.md` | `{spacing.40}` | `40px` | = light |
| `--pagination-nav-min-width-sm` | `pagination.nav.minWidth.sm` | `{spacing.32}` | `32px` | = light |
| `--pagination-nav-min-width-md` | `pagination.nav.minWidth.md` | `{spacing.56}` | `56px` | = light |
| `--pagination-nav-padding-inline-start-sm` | `pagination.nav.paddingInlineStart.sm` | `{spacing.0}` | `0px` | = light |
| `--pagination-nav-padding-inline-start-md` | `pagination.nav.paddingInlineStart.md` | `{spacing.12}` | `12px` | = light |
| `--pagination-nav-padding-inline-end-sm` | `pagination.nav.paddingInlineEnd.sm` | `{spacing.0}` | `0px` | = light |
| `--pagination-nav-padding-inline-end-md` | `pagination.nav.paddingInlineEnd.md` | `{spacing.16}` | `16px` | = light |
| `--pagination-nav-gap` | `pagination.nav.gap` | `{spacing.6}` | `6px` | = light |
| `--pagination-nav-border-radius` | `pagination.nav.borderRadius` | `{borderRadius.6}` | `6px` | = light |
| `--pagination-nav-icon-size-sm` | `pagination.nav.iconSize.sm` | `{spacing.16}` | `16px` | = light |
| `--pagination-nav-icon-size-md` | `pagination.nav.iconSize.md` | `{spacing.20}` | `20px` | = light |
| `--pagination-nav-background-default` | `pagination.nav.background.default` | — | `transparent` | = light |
| `--pagination-nav-background-hover` | `pagination.nav.background.hover` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--pagination-nav-background-active` | `pagination.nav.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--pagination-nav-label-default` | `pagination.nav.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--pagination-nav-label-disabled` | `pagination.nav.label.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--pagination-nav-icon-default` | `pagination.nav.icon.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--pagination-nav-icon-disabled` | `pagination.nav.icon.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--pagination-nav-border-default` | `pagination.nav.border.default` | — | `transparent` | = light |
| `--pagination-nav-border-focus` | `pagination.nav.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |

### Tooltip — `tokens/core/components/tooltip.tokens.json`

_40 tokens._

**`tooltip.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tooltip-container-border-radius-sm` | `tooltip.container.borderRadius.sm` | `{borderRadius.4}` | `4px` | = light |
| `--tooltip-container-border-radius-lg` | `tooltip.container.borderRadius.lg` | `{borderRadius.6}` | `6px` | = light |
| `--tooltip-container-padding-block-sm` | `tooltip.container.paddingBlock.sm` | `{spacing.8}` | `8px` | = light |
| `--tooltip-container-padding-block-lg` | `tooltip.container.paddingBlock.lg` | `{spacing.12}` | `12px` | = light |
| `--tooltip-container-padding-inline-sm` | `tooltip.container.paddingInline.sm` | `{spacing.12}` | `12px` | = light |
| `--tooltip-container-padding-inline-lg` | `tooltip.container.paddingInline.lg` | `{spacing.16}` | `16px` | = light |
| `--tooltip-container-padding-inline-end-lg` | `tooltip.container.paddingInlineEnd.lg` | `{spacing.12}` | `12px` | = light |
| `--tooltip-container-gap-sm` | `tooltip.container.gap.sm` | `{spacing.8}` | `8px` | = light |
| `--tooltip-container-gap-lg` | `tooltip.container.gap.lg` | `{spacing.12}` | `12px` | = light |
| `--tooltip-container-max-width` | `tooltip.container.maxWidth` | — | `200px` | = light |
| `--tooltip-container-shadow` | `tooltip.container.shadow` | `{dropShadow.100}` | `0px 2px 8px 0px rgba(19, 22, 29, 0.06), 0px 4px 8px 1px rgba(19, 22, 29, 0.04)` | = light |
| `--tooltip-container-transition-duration` | `tooltip.container.transitionDuration` | — | `150ms` | = light |
| `--tooltip-container-transition-timing-function` | `tooltip.container.transitionTimingFunction` | — | `ease-out` | = light |
| `--tooltip-container-background-default` | `tooltip.container.background.default` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--tooltip-container-z-index` | `tooltip.container.zIndex` | — | `9999` | = light |

**`tooltip.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tooltip-label-font-family` | `tooltip.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--tooltip-label-font-size` | `tooltip.label.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--tooltip-label-line-height` | `tooltip.label.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--tooltip-label-font-weight` | `tooltip.label.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--tooltip-label-color-default` | `tooltip.label.color.default` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |

**`tooltip.hint`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tooltip-hint-font-family` | `tooltip.hint.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--tooltip-hint-font-size` | `tooltip.hint.fontSize` | `{fontSize.12}` | `12px` | = light |
| `--tooltip-hint-line-height` | `tooltip.hint.lineHeight` | `{lineHeight.16}` | `16px` | = light |
| `--tooltip-hint-font-weight` | `tooltip.hint.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--tooltip-hint-color-default` | `tooltip.hint.color.default` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--tooltip-hint-opacity` | `tooltip.hint.opacity` | — | `0.72` | = light |
| `--tooltip-hint-margin-top` | `tooltip.hint.marginTop` | `{spacing.6}` | `6px` | = light |

**`tooltip.arrow`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tooltip-arrow-size-sm` | `tooltip.arrow.size.sm` | — | `8px` | = light |
| `--tooltip-arrow-size-lg` | `tooltip.arrow.size.lg` | — | `12px` | = light |
| `--tooltip-arrow-width-sm` | `tooltip.arrow.width.sm` | — | `16px` | = light |
| `--tooltip-arrow-width-lg` | `tooltip.arrow.width.lg` | — | `24px` | = light |
| `--tooltip-arrow-edge-margin` | `tooltip.arrow.edgeMargin` | `{spacing.12}` | `12px` | = light |

**`tooltip.close`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tooltip-close-size` | `tooltip.close.size` | `{spacing.16}` | `16px` | = light |
| `--tooltip-close-color-default` | `tooltip.close.color.default` | `{color.icon.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--tooltip-close-color-hover` | `tooltip.close.color.hover` | `{color.icon.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--tooltip-close-background-default` | `tooltip.close.background.default` | — | `transparent` | = light |
| `--tooltip-close-background-hover` | `tooltip.close.background.hover` | `{color.background.base-inverse.default-hover}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--tooltip-close-border-radius` | `tooltip.close.borderRadius` | `{borderRadius.4}` | `4px` | = light |
| `--tooltip-close-touch-target` | `tooltip.close.touchTarget` | `{spacing.24}` | `24px` | = light |

**`tooltip.offset`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tooltip-offset-trigger` | `tooltip.offset.trigger` | `{spacing.4}` | `4px` | = light |

Note din sursă (`$comment`):

- `--tooltip-container-shadow` — Lightest elevation rung. dropShadow.200 carries a 2px spread that pushes a faint grey halo ~2px beyond the bubble edges; on the dark bubble against a light page that halo reads as a thin border. dropShadow.100 has no spread on its primary layer and is the correct elevation for a small floating tooltip.

---

## 6. Filtre

Sistemul nu are componentă de filtrare pentru tabele și nici tokens de layout pentru o bară de
filtre. Ce are sunt controalele individuale din care îți asamblezi filtrarea. Tokenii lor sunt
mai jos, integral.

**Singurul set de tokens numit explicit „filtru” din tot design system-ul** este `chip.filter.*`
— 8 tokens în `mud-chip`, pentru `type="filter"`:

| Variabilă CSS | Rol |
| --- | --- |
| `--chip-filter-background-default` / `-hover` | chip nefiltrat, în repaus și la hover |
| `--chip-filter-background-selected` / `-selected-hover` | chip activ — fundal inversat (închis) |
| `--chip-filter-label-default` / `-selected` | culoarea textului în cele două stări |
| `--chip-filter-icon-default` / `-selected` | culoarea iconului în cele două stări |

Comportamentul de selecție se alege din `selectionMode`
(`src/components/mud-chip/mud-chip.types.ts`):

- `mono` — alegere unică, ca un radio; chipul selectat doar își schimbă culoarea;
- `multi` — alegere multiplă, ca un checkbox; la selecție apare o bifă în fața etichetei.

Restul grupurilor din `mud-chip` (`container`, `avatar`, `count`, `label`, `icon`, `input`,
`disabled`) se aplică indiferent de tip — `count` e util pentru „câte rezultate” pe un filtru
activ, iar `disabled` pentru filtre indisponibile.

**Ce alegi pentru ce fel de filtru:**

| Ai nevoie de | Folosește |
| --- | --- |
| Filtre rapide, vizibile, cu una sau mai multe selecții | `mud-chip type="filter"` |
| Filtre acumulate ca etichete într-un câmp | `mud-input-chip` |
| Căutare text liberă deasupra tabelului | `mud-search-input-rectangular` |
| Căutare compactă, în bară de instrumente | `mud-search-input-circular` |
| Listă lungă de opțiuni | `mud-select-input` |
| Meniu de coloană (sortare, ascundere, filtrare pe coloană) | `mud-menu` |
| Două-trei opțiuni exclusive (ex. Toate / Active / Arhivate) | `mud-segmented-control` |
| Categorii principale, ca navigare | `mud-tabs` |
| Filtru pe o dată sau un interval | `mud-date-input` / `mud-date-picker` |

### Chip — **filtre** — `tokens/core/components/chip.tokens.json`

_62 tokens._

**`chip.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-container-height-md` | `chip.container.height.md` | — | `36px` | = light |
| `--chip-container-height-sm` | `chip.container.height.sm` | — | `28px` | = light |
| `--chip-container-touch-target-md` | `chip.container.touchTarget.md` | — | `44px` | = light |
| `--chip-container-touch-target-sm` | `chip.container.touchTarget.sm` | `{spacing.40}` | `40px` | = light |
| `--chip-container-min-width-md` | `chip.container.minWidth.md` | — | `60px` | = light |
| `--chip-container-min-width-sm` | `chip.container.minWidth.sm` | — | `52px` | = light |
| `--chip-container-padding-inline-md` | `chip.container.paddingInline.md` | `{spacing.16}` | `16px` | = light |
| `--chip-container-padding-inline-sm` | `chip.container.paddingInline.sm` | `{spacing.12}` | `12px` | = light |
| `--chip-container-padding-inline-icon-md` | `chip.container.paddingInlineIcon.md` | `{spacing.12}` | `12px` | = light |
| `--chip-container-padding-inline-icon-sm` | `chip.container.paddingInlineIcon.sm` | `{spacing.8}` | `8px` | = light |
| `--chip-container-padding-inline-remove-md` | `chip.container.paddingInlineRemove.md` | `{spacing.12}` | `12px` | = light |
| `--chip-container-padding-inline-remove-sm` | `chip.container.paddingInlineRemove.sm` | `{spacing.8}` | `8px` | = light |
| `--chip-container-gap-md` | `chip.container.gap.md` | `{spacing.8}` | `8px` | = light |
| `--chip-container-gap-sm` | `chip.container.gap.sm` | `{spacing.6}` | `6px` | = light |
| `--chip-container-gap-icon-md` | `chip.container.gapIcon.md` | `{spacing.4}` | `4px` | = light |
| `--chip-container-gap-icon-sm` | `chip.container.gapIcon.sm` | `{spacing.4}` | `4px` | = light |
| `--chip-container-border-radius` | `chip.container.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--chip-container-transition-duration` | `chip.container.transitionDuration` | — | `150ms` | = light |
| `--chip-container-transition-timing-function` | `chip.container.transitionTimingFunction` | — | `ease-out` | = light |

**`chip.avatar`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-avatar-size-md` | `chip.avatar.size.md` | — | `28px` | = light |
| `--chip-avatar-size-sm` | `chip.avatar.size.sm` | — | `22px` | = light |

**`chip.count`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-count-size-md` | `chip.count.size.md` | — | `18px` | = light |
| `--chip-count-size-sm` | `chip.count.size.sm` | — | `16px` | = light |
| `--chip-count-font-size-md` | `chip.count.fontSize.md` | `{fontSize.12}` | `12px` | = light |
| `--chip-count-font-size-sm` | `chip.count.fontSize.sm` | `{fontSize.12}` | `12px` | = light |
| `--chip-count-padding-inline-md` | `chip.count.paddingInline.md` | `{spacing.4}` | `4px` | = light |
| `--chip-count-padding-inline-sm` | `chip.count.paddingInline.sm` | `{spacing.4}` | `4px` | = light |

**`chip.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-label-font-family` | `chip.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--chip-label-font-size-md` | `chip.label.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--chip-label-font-size-sm` | `chip.label.fontSize.sm` | `{fontSize.12}` | `12px` | = light |
| `--chip-label-line-height-md` | `chip.label.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--chip-label-line-height-sm` | `chip.label.lineHeight.sm` | `{lineHeight.16}` | `16px` | = light |
| `--chip-label-font-weight` | `chip.label.fontWeight` | `{fontWeight.medium}` | `500` | = light |

**`chip.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-icon-size-md` | `chip.icon.size.md` | `{spacing.20}` | `20px` | = light |
| `--chip-icon-size-sm` | `chip.icon.size.sm` | `{spacing.16}` | `16px` | = light |

**`chip.filter`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-filter-background-default` | `chip.filter.background.default` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--chip-filter-background-hover` | `chip.filter.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--chip-filter-background-selected` | `chip.filter.background.selected` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--chip-filter-background-selected-hover` | `chip.filter.background.selectedHover` | `{color.background.base-inverse.default-hover}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--chip-filter-label-default` | `chip.filter.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--chip-filter-label-selected` | `chip.filter.label.selected` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--chip-filter-icon-default` | `chip.filter.icon.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--chip-filter-icon-selected` | `chip.filter.icon.selected` | `{color.icon.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |

**`chip.input`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-input-min-width-md` | `chip.input.minWidth.md` | — | `64px` | = light |
| `--chip-input-min-width-sm` | `chip.input.minWidth.sm` | — | `56px` | = light |
| `--chip-input-background-default` | `chip.input.background.default` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--chip-input-background-hover` | `chip.input.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--chip-input-label-default` | `chip.input.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--chip-input-icon-default` | `chip.input.icon.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--chip-input-remove-size-md` | `chip.input.remove.size.md` | `{spacing.20}` | `20px` | = light |
| `--chip-input-remove-size-sm` | `chip.input.remove.size.sm` | `{spacing.16}` | `16px` | = light |
| `--chip-input-remove-icon-size-md` | `chip.input.remove.iconSize.md` | `{spacing.20}` | `20px` | = light |
| `--chip-input-remove-icon-size-sm` | `chip.input.remove.iconSize.sm` | `{spacing.16}` | `16px` | = light |
| `--chip-input-remove-border-radius` | `chip.input.remove.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--chip-input-remove-background-default` | `chip.input.remove.background.default` | — | `transparent` | = light |
| `--chip-input-remove-background-hover` | `chip.input.remove.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--chip-input-remove-color-default` | `chip.input.remove.color.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--chip-input-remove-color-hover` | `chip.input.remove.color.hover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--chip-input-remove-color-disabled` | `chip.input.remove.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`chip.disabled`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--chip-disabled-background` | `chip.disabled.background` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--chip-disabled-label` | `chip.disabled.label` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--chip-disabled-icon` | `chip.disabled.icon` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

### Input chip (filtre multiple ca etichete) — `tokens/core/components/input-chip.tokens.json`

_86 tokens._

**`inputChip.field`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-field-gap` | `inputChip.field.gap` | `{spacing.8}` | `8px` | = light |

**`inputChip.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-container-min-height-md` | `inputChip.container.minHeight.md` | `{spacing.40}` | `40px` | = light |
| `--input-chip-container-min-height-lg` | `inputChip.container.minHeight.lg` | `{spacing.48}` | `48px` | = light |
| `--input-chip-container-padding-block-md` | `inputChip.container.paddingBlock.md` | `{spacing.4}` | `4px` | = light |
| `--input-chip-container-padding-block-lg` | `inputChip.container.paddingBlock.lg` | `{spacing.6}` | `6px` | = light |
| `--input-chip-container-padding-inline-md` | `inputChip.container.paddingInline.md` | `{spacing.8}` | `8px` | = light |
| `--input-chip-container-padding-inline-lg` | `inputChip.container.paddingInline.lg` | `{spacing.12}` | `12px` | = light |
| `--input-chip-container-gap-md` | `inputChip.container.gap.md` | `{spacing.6}` | `6px` | = light |
| `--input-chip-container-gap-lg` | `inputChip.container.gap.lg` | `{spacing.8}` | `8px` | = light |
| `--input-chip-container-border-radius` | `inputChip.container.borderRadius` | `{borderRadius.8}` | `8px` | = light |
| `--input-chip-container-border-width-default` | `inputChip.container.borderWidth.default` | `{borderWidth.1}` | `1px` | = light |
| `--input-chip-container-border-width-emphasized` | `inputChip.container.borderWidth.emphasized` | `{borderWidth.2}` | `2px` | = light |
| `--input-chip-container-focus-ring-width` | `inputChip.container.focusRingWidth` | `{borderWidth.2}` | `2px` | = light |
| `--input-chip-container-transition-duration` | `inputChip.container.transitionDuration` | — | `150ms` | = light |
| `--input-chip-container-transition-timing-function` | `inputChip.container.transitionTimingFunction` | — | `ease-out` | = light |

**`inputChip.control`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-control-font-family` | `inputChip.control.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--input-chip-control-font-weight` | `inputChip.control.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--input-chip-control-font-size-md` | `inputChip.control.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--input-chip-control-font-size-lg` | `inputChip.control.fontSize.lg` | `{fontSize.16}` | `16px` | = light |
| `--input-chip-control-line-height-md` | `inputChip.control.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--input-chip-control-line-height-lg` | `inputChip.control.lineHeight.lg` | `{lineHeight.24}` | `24px` | = light |
| `--input-chip-control-min-width` | `inputChip.control.minWidth` | `{spacing.80}` | `80px` | = light |

**`inputChip.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-label-font-family` | `inputChip.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--input-chip-label-font-size` | `inputChip.label.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--input-chip-label-line-height` | `inputChip.label.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--input-chip-label-font-weight` | `inputChip.label.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--input-chip-label-gap` | `inputChip.label.gap` | `{spacing.4}` | `4px` | = light |
| `--input-chip-label-color-default` | `inputChip.label.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--input-chip-label-color-disabled` | `inputChip.label.color.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--input-chip-label-required-mark-size` | `inputChip.label.requiredMark.size` | `{spacing.12}` | `12px` | = light |
| `--input-chip-label-required-mark-color` | `inputChip.label.requiredMark.color` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`inputChip.assistive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-assistive-font-family` | `inputChip.assistive.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--input-chip-assistive-font-size` | `inputChip.assistive.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--input-chip-assistive-line-height` | `inputChip.assistive.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--input-chip-assistive-font-weight` | `inputChip.assistive.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--input-chip-assistive-gap-default` | `inputChip.assistive.gap.default` | `{spacing.6}` | `6px` | = light |
| `--input-chip-assistive-gap-error` | `inputChip.assistive.gap.error` | `{spacing.4}` | `4px` | = light |
| `--input-chip-assistive-icon-size` | `inputChip.assistive.iconSize` | `{spacing.20}` | `20px` | = light |
| `--input-chip-assistive-color-default` | `inputChip.assistive.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--input-chip-assistive-color-error` | `inputChip.assistive.color.error` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`inputChip.chip`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-chip-font-family` | `inputChip.chip.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--input-chip-chip-font-size` | `inputChip.chip.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--input-chip-chip-line-height` | `inputChip.chip.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--input-chip-chip-font-weight` | `inputChip.chip.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--input-chip-chip-height-md` | `inputChip.chip.height.md` | `{spacing.24}` | `24px` | = light |
| `--input-chip-chip-height-lg` | `inputChip.chip.height.lg` | `{spacing.32}` | `32px` | = light |
| `--input-chip-chip-padding-block-md` | `inputChip.chip.paddingBlock.md` | `{spacing.2}` | `2px` | = light |
| `--input-chip-chip-padding-block-lg` | `inputChip.chip.paddingBlock.lg` | `{spacing.4}` | `4px` | = light |
| `--input-chip-chip-padding-inline-start-md` | `inputChip.chip.paddingInlineStart.md` | `{spacing.8}` | `8px` | = light |
| `--input-chip-chip-padding-inline-start-lg` | `inputChip.chip.paddingInlineStart.lg` | `{spacing.12}` | `12px` | = light |
| `--input-chip-chip-padding-inline-end-md` | `inputChip.chip.paddingInlineEnd.md` | `{spacing.4}` | `4px` | = light |
| `--input-chip-chip-padding-inline-end-lg` | `inputChip.chip.paddingInlineEnd.lg` | `{spacing.6}` | `6px` | = light |
| `--input-chip-chip-gap-md` | `inputChip.chip.gap.md` | `{spacing.4}` | `4px` | = light |
| `--input-chip-chip-gap-lg` | `inputChip.chip.gap.lg` | `{spacing.6}` | `6px` | = light |
| `--input-chip-chip-border-radius` | `inputChip.chip.borderRadius` | `{borderRadius.6}` | `6px` | = light |
| `--input-chip-chip-max-inline-size` | `inputChip.chip.maxInlineSize` | — | `100%` | = light |
| `--input-chip-chip-background-default` | `inputChip.chip.background.default` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--input-chip-chip-background-hover` | `inputChip.chip.background.hover` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--input-chip-chip-background-disabled` | `inputChip.chip.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--input-chip-chip-label-default` | `inputChip.chip.label.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--input-chip-chip-label-disabled` | `inputChip.chip.label.disabled` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--input-chip-chip-remove-size-md` | `inputChip.chip.remove.size.md` | `{spacing.16}` | `16px` | = light |
| `--input-chip-chip-remove-size-lg` | `inputChip.chip.remove.size.lg` | `{spacing.20}` | `20px` | = light |
| `--input-chip-chip-remove-icon-size-md` | `inputChip.chip.remove.iconSize.md` | `{spacing.12}` | `12px` | = light |
| `--input-chip-chip-remove-icon-size-lg` | `inputChip.chip.remove.iconSize.lg` | `{spacing.16}` | `16px` | = light |
| `--input-chip-chip-remove-color-default` | `inputChip.chip.remove.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--input-chip-chip-remove-color-hover` | `inputChip.chip.remove.color.hover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--input-chip-chip-remove-color-disabled` | `inputChip.chip.remove.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--input-chip-chip-remove-background-hover` | `inputChip.chip.remove.background.hover` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--input-chip-chip-remove-background-focus` | `inputChip.chip.remove.background.focus` | `{color.background.brand.secondary}` → `{palette.blue-sky.100}` | `#e8f0fb` | `#ccdef6` |
| `--input-chip-chip-remove-border-radius` | `inputChip.chip.remove.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--input-chip-chip-focus-ring-color` | `inputChip.chip.focusRingColor` | `{palette.blue-sky.200}` | `#ccdef6` | = light |

**`inputChip.default`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-default-background-default` | `inputChip.default.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--input-chip-default-background-disabled` | `inputChip.default.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--input-chip-default-border-default` | `inputChip.default.border.default` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--input-chip-default-border-hover` | `inputChip.default.border.hover` | `{color.border.base.tertiary}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--input-chip-default-border-focus` | `inputChip.default.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--input-chip-default-border-disabled` | `inputChip.default.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--input-chip-default-text-default` | `inputChip.default.text.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--input-chip-default-text-placeholder` | `inputChip.default.text.placeholder` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--input-chip-default-text-disabled` | `inputChip.default.text.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--input-chip-default-focus-ring` | `inputChip.default.focusRing` | `{palette.blue-sky.200}` | `#ccdef6` | = light |

**`inputChip.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--input-chip-destructive-border-default` | `inputChip.destructive.border.default` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--input-chip-destructive-border-hover` | `inputChip.destructive.border.hover` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--input-chip-destructive-border-focus` | `inputChip.destructive.border.focus` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--input-chip-destructive-focus-ring` | `inputChip.destructive.focusRing` | `{palette.red.200}` | `#fecdc9` | = light |

### Search input dreptunghiular — `tokens/core/components/search-input-rectangular.tokens.json`

_81 tokens._

**`search-input-rectangular.field`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-field-gap` | `search-input-rectangular.field.gap` | `{spacing.8}` | `8px` | = light |

**`search-input-rectangular.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-container-height-md` | `search-input-rectangular.container.height.md` | `{spacing.40}` | `40px` | = light |
| `--search-input-rectangular-container-height-lg` | `search-input-rectangular.container.height.lg` | `{spacing.48}` | `48px` | = light |
| `--search-input-rectangular-container-padding-inline-md` | `search-input-rectangular.container.paddingInline.md` | `{spacing.12}` | `12px` | = light |
| `--search-input-rectangular-container-padding-inline-lg` | `search-input-rectangular.container.paddingInline.lg` | `{spacing.16}` | `16px` | = light |
| `--search-input-rectangular-container-gap-md` | `search-input-rectangular.container.gap.md` | `{spacing.8}` | `8px` | = light |
| `--search-input-rectangular-container-gap-lg` | `search-input-rectangular.container.gap.lg` | `{spacing.8}` | `8px` | = light |
| `--search-input-rectangular-container-border-radius` | `search-input-rectangular.container.borderRadius` | `{borderRadius.8}` | `8px` | = light |
| `--search-input-rectangular-container-border-width-default` | `search-input-rectangular.container.borderWidth.default` | `{borderWidth.1}` | `1px` | = light |
| `--search-input-rectangular-container-border-width-emphasized` | `search-input-rectangular.container.borderWidth.emphasized` | `{borderWidth.2}` | `2px` | = light |
| `--search-input-rectangular-container-focus-ring-width` | `search-input-rectangular.container.focusRingWidth` | `{borderWidth.2}` | `2px` | = light |
| `--search-input-rectangular-container-transition-duration` | `search-input-rectangular.container.transitionDuration` | — | `150ms` | = light |
| `--search-input-rectangular-container-transition-timing-function` | `search-input-rectangular.container.transitionTimingFunction` | — | `ease-out` | = light |

**`search-input-rectangular.control`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-control-font-family` | `search-input-rectangular.control.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--search-input-rectangular-control-font-weight` | `search-input-rectangular.control.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--search-input-rectangular-control-font-size-md` | `search-input-rectangular.control.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--search-input-rectangular-control-font-size-lg` | `search-input-rectangular.control.fontSize.lg` | `{fontSize.16}` | `16px` | = light |
| `--search-input-rectangular-control-line-height-md` | `search-input-rectangular.control.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--search-input-rectangular-control-line-height-lg` | `search-input-rectangular.control.lineHeight.lg` | `{lineHeight.24}` | `24px` | = light |

**`search-input-rectangular.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-label-font-family` | `search-input-rectangular.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--search-input-rectangular-label-font-size` | `search-input-rectangular.label.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--search-input-rectangular-label-line-height` | `search-input-rectangular.label.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--search-input-rectangular-label-font-weight` | `search-input-rectangular.label.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--search-input-rectangular-label-gap` | `search-input-rectangular.label.gap` | `{spacing.4}` | `4px` | = light |
| `--search-input-rectangular-label-color-default` | `search-input-rectangular.label.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--search-input-rectangular-label-color-disabled` | `search-input-rectangular.label.color.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`search-input-rectangular.assistive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-assistive-font-family` | `search-input-rectangular.assistive.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--search-input-rectangular-assistive-font-size` | `search-input-rectangular.assistive.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--search-input-rectangular-assistive-line-height` | `search-input-rectangular.assistive.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--search-input-rectangular-assistive-font-weight` | `search-input-rectangular.assistive.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--search-input-rectangular-assistive-gap-default` | `search-input-rectangular.assistive.gap.default` | `{spacing.6}` | `6px` | = light |
| `--search-input-rectangular-assistive-gap-error` | `search-input-rectangular.assistive.gap.error` | `{spacing.4}` | `4px` | = light |
| `--search-input-rectangular-assistive-icon-size` | `search-input-rectangular.assistive.iconSize` | `{spacing.20}` | `20px` | = light |
| `--search-input-rectangular-assistive-color-default` | `search-input-rectangular.assistive.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--search-input-rectangular-assistive-color-error` | `search-input-rectangular.assistive.color.error` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`search-input-rectangular.iconStart`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-icon-start-size-md` | `search-input-rectangular.iconStart.size.md` | `{spacing.20}` | `20px` | = light |
| `--search-input-rectangular-icon-start-size-lg` | `search-input-rectangular.iconStart.size.lg` | `{spacing.24}` | `24px` | = light |
| `--search-input-rectangular-icon-start-color-default` | `search-input-rectangular.iconStart.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--search-input-rectangular-icon-start-color-disabled` | `search-input-rectangular.iconStart.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`search-input-rectangular.iconEndClear`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-icon-end-clear-size-md` | `search-input-rectangular.iconEndClear.size.md` | `{spacing.20}` | `20px` | = light |
| `--search-input-rectangular-icon-end-clear-size-lg` | `search-input-rectangular.iconEndClear.size.lg` | `{spacing.24}` | `24px` | = light |
| `--search-input-rectangular-icon-end-clear-padding` | `search-input-rectangular.iconEndClear.padding` | `{spacing.4}` | `4px` | = light |
| `--search-input-rectangular-icon-end-clear-border-radius` | `search-input-rectangular.iconEndClear.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--search-input-rectangular-icon-end-clear-color-default` | `search-input-rectangular.iconEndClear.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--search-input-rectangular-icon-end-clear-color-hover` | `search-input-rectangular.iconEndClear.color.hover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--search-input-rectangular-icon-end-clear-color-active` | `search-input-rectangular.iconEndClear.color.active` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--search-input-rectangular-icon-end-clear-color-disabled` | `search-input-rectangular.iconEndClear.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--search-input-rectangular-icon-end-clear-background-default` | `search-input-rectangular.iconEndClear.background.default` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--search-input-rectangular-icon-end-clear-background-hover` | `search-input-rectangular.iconEndClear.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--search-input-rectangular-icon-end-clear-background-active` | `search-input-rectangular.iconEndClear.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |

**`search-input-rectangular.default`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-default-background-default` | `search-input-rectangular.default.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--search-input-rectangular-default-background-disabled` | `search-input-rectangular.default.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--search-input-rectangular-default-border-default` | `search-input-rectangular.default.border.default` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--search-input-rectangular-default-border-hover` | `search-input-rectangular.default.border.hover` | `{color.border.base.tertiary}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--search-input-rectangular-default-border-focus` | `search-input-rectangular.default.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--search-input-rectangular-default-border-disabled` | `search-input-rectangular.default.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--search-input-rectangular-default-text-default` | `search-input-rectangular.default.text.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--search-input-rectangular-default-text-placeholder` | `search-input-rectangular.default.text.placeholder` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--search-input-rectangular-default-text-disabled` | `search-input-rectangular.default.text.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--search-input-rectangular-default-focus-ring` | `search-input-rectangular.default.focusRing` | `{palette.blue-sky.200}` | `#ccdef6` | = light |

**`search-input-rectangular.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-destructive-border-default` | `search-input-rectangular.destructive.border.default` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--search-input-rectangular-destructive-border-hover` | `search-input-rectangular.destructive.border.hover` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--search-input-rectangular-destructive-border-focus` | `search-input-rectangular.destructive.border.focus` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--search-input-rectangular-destructive-focus-ring` | `search-input-rectangular.destructive.focusRing` | `{palette.red.200}` | `#fecdc9` | = light |

**`search-input-rectangular.submitButton`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-submit-button-size-md` | `search-input-rectangular.submitButton.size.md` | `{spacing.32}` | `32px` | = light |
| `--search-input-rectangular-submit-button-size-lg` | `search-input-rectangular.submitButton.size.lg` | `{spacing.40}` | `40px` | = light |
| `--search-input-rectangular-submit-button-icon-size-md` | `search-input-rectangular.submitButton.iconSize.md` | `{spacing.16}` | `16px` | = light |
| `--search-input-rectangular-submit-button-icon-size-lg` | `search-input-rectangular.submitButton.iconSize.lg` | `{spacing.20}` | `20px` | = light |
| `--search-input-rectangular-submit-button-border-radius` | `search-input-rectangular.submitButton.borderRadius` | `{borderRadius.6}` | `6px` | = light |
| `--search-input-rectangular-submit-button-background-default` | `search-input-rectangular.submitButton.background.default` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--search-input-rectangular-submit-button-background-hover` | `search-input-rectangular.submitButton.background.hover` | `{color.background.brand.default-hover}` → `{palette.blue-sky.700}` | `#0046a8` | = light |
| `--search-input-rectangular-submit-button-background-active` | `search-input-rectangular.submitButton.background.active` | `{color.background.brand.default-active}` → `{palette.blue-sky.800}` | `#00357e` | = light |
| `--search-input-rectangular-submit-button-background-disabled` | `search-input-rectangular.submitButton.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--search-input-rectangular-submit-button-icon-default` | `search-input-rectangular.submitButton.icon.default` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--search-input-rectangular-submit-button-icon-disabled` | `search-input-rectangular.submitButton.icon.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--search-input-rectangular-submit-button-focus-ring-offset` | `search-input-rectangular.submitButton.focusRingOffset` | `{borderWidth.2}` | `2px` | = light |
| `--search-input-rectangular-submit-button-container-padding-inline-end` | `search-input-rectangular.submitButton.containerPaddingInlineEnd` | `{spacing.4}` | `4px` | = light |

**`search-input-rectangular.loadingSpinner`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-rectangular-loading-spinner-size-md` | `search-input-rectangular.loadingSpinner.size.md` | `{spacing.20}` | `20px` | = light |
| `--search-input-rectangular-loading-spinner-size-lg` | `search-input-rectangular.loadingSpinner.size.lg` | `{spacing.24}` | `24px` | = light |
| `--search-input-rectangular-loading-spinner-color-default` | `search-input-rectangular.loadingSpinner.color.default` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--search-input-rectangular-loading-spinner-color-disabled` | `search-input-rectangular.loadingSpinner.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

Note din sursă (`$comment`):

- `--search-input-rectangular-icon-end-clear-background-default` — Per Figma 933:29146 — the clear button carries a subtle gray oval at rest in the Filled state. Hardcoded color in Figma is #1212121a; base.tertiary (#f1f1f1) is the closest design-system semantic.

### Search input circular — `tokens/core/components/search-input-circular.tokens.json`

_81 tokens._

**`search-input-circular.field`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-field-gap` | `search-input-circular.field.gap` | `{spacing.8}` | `8px` | = light |

**`search-input-circular.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-container-height-md` | `search-input-circular.container.height.md` | `{spacing.40}` | `40px` | = light |
| `--search-input-circular-container-height-lg` | `search-input-circular.container.height.lg` | `{spacing.48}` | `48px` | = light |
| `--search-input-circular-container-padding-inline-md` | `search-input-circular.container.paddingInline.md` | `{spacing.12}` | `12px` | = light |
| `--search-input-circular-container-padding-inline-lg` | `search-input-circular.container.paddingInline.lg` | `{spacing.16}` | `16px` | = light |
| `--search-input-circular-container-gap-md` | `search-input-circular.container.gap.md` | `{spacing.8}` | `8px` | = light |
| `--search-input-circular-container-gap-lg` | `search-input-circular.container.gap.lg` | `{spacing.8}` | `8px` | = light |
| `--search-input-circular-container-border-radius` | `search-input-circular.container.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--search-input-circular-container-border-width-default` | `search-input-circular.container.borderWidth.default` | `{borderWidth.1}` | `1px` | = light |
| `--search-input-circular-container-border-width-emphasized` | `search-input-circular.container.borderWidth.emphasized` | `{borderWidth.2}` | `2px` | = light |
| `--search-input-circular-container-focus-ring-width` | `search-input-circular.container.focusRingWidth` | `{borderWidth.2}` | `2px` | = light |
| `--search-input-circular-container-transition-duration` | `search-input-circular.container.transitionDuration` | — | `150ms` | = light |
| `--search-input-circular-container-transition-timing-function` | `search-input-circular.container.transitionTimingFunction` | — | `ease-out` | = light |

**`search-input-circular.control`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-control-font-family` | `search-input-circular.control.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--search-input-circular-control-font-weight` | `search-input-circular.control.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--search-input-circular-control-font-size-md` | `search-input-circular.control.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--search-input-circular-control-font-size-lg` | `search-input-circular.control.fontSize.lg` | `{fontSize.16}` | `16px` | = light |
| `--search-input-circular-control-line-height-md` | `search-input-circular.control.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--search-input-circular-control-line-height-lg` | `search-input-circular.control.lineHeight.lg` | `{lineHeight.24}` | `24px` | = light |

**`search-input-circular.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-label-font-family` | `search-input-circular.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--search-input-circular-label-font-size` | `search-input-circular.label.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--search-input-circular-label-line-height` | `search-input-circular.label.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--search-input-circular-label-font-weight` | `search-input-circular.label.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--search-input-circular-label-gap` | `search-input-circular.label.gap` | `{spacing.4}` | `4px` | = light |
| `--search-input-circular-label-color-default` | `search-input-circular.label.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--search-input-circular-label-color-disabled` | `search-input-circular.label.color.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`search-input-circular.assistive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-assistive-font-family` | `search-input-circular.assistive.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--search-input-circular-assistive-font-size` | `search-input-circular.assistive.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--search-input-circular-assistive-line-height` | `search-input-circular.assistive.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--search-input-circular-assistive-font-weight` | `search-input-circular.assistive.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--search-input-circular-assistive-gap-default` | `search-input-circular.assistive.gap.default` | `{spacing.6}` | `6px` | = light |
| `--search-input-circular-assistive-gap-error` | `search-input-circular.assistive.gap.error` | `{spacing.4}` | `4px` | = light |
| `--search-input-circular-assistive-icon-size` | `search-input-circular.assistive.iconSize` | `{spacing.20}` | `20px` | = light |
| `--search-input-circular-assistive-color-default` | `search-input-circular.assistive.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--search-input-circular-assistive-color-error` | `search-input-circular.assistive.color.error` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`search-input-circular.iconStart`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-icon-start-size-md` | `search-input-circular.iconStart.size.md` | `{spacing.20}` | `20px` | = light |
| `--search-input-circular-icon-start-size-lg` | `search-input-circular.iconStart.size.lg` | `{spacing.24}` | `24px` | = light |
| `--search-input-circular-icon-start-color-default` | `search-input-circular.iconStart.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--search-input-circular-icon-start-color-disabled` | `search-input-circular.iconStart.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`search-input-circular.iconEndClear`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-icon-end-clear-size-md` | `search-input-circular.iconEndClear.size.md` | `{spacing.20}` | `20px` | = light |
| `--search-input-circular-icon-end-clear-size-lg` | `search-input-circular.iconEndClear.size.lg` | `{spacing.24}` | `24px` | = light |
| `--search-input-circular-icon-end-clear-padding` | `search-input-circular.iconEndClear.padding` | `{spacing.4}` | `4px` | = light |
| `--search-input-circular-icon-end-clear-border-radius` | `search-input-circular.iconEndClear.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--search-input-circular-icon-end-clear-color-default` | `search-input-circular.iconEndClear.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--search-input-circular-icon-end-clear-color-hover` | `search-input-circular.iconEndClear.color.hover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--search-input-circular-icon-end-clear-color-active` | `search-input-circular.iconEndClear.color.active` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--search-input-circular-icon-end-clear-color-disabled` | `search-input-circular.iconEndClear.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--search-input-circular-icon-end-clear-background-default` | `search-input-circular.iconEndClear.background.default` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--search-input-circular-icon-end-clear-background-hover` | `search-input-circular.iconEndClear.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--search-input-circular-icon-end-clear-background-active` | `search-input-circular.iconEndClear.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |

**`search-input-circular.default`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-default-background-default` | `search-input-circular.default.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--search-input-circular-default-background-disabled` | `search-input-circular.default.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--search-input-circular-default-border-default` | `search-input-circular.default.border.default` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--search-input-circular-default-border-hover` | `search-input-circular.default.border.hover` | `{color.border.base.tertiary}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--search-input-circular-default-border-focus` | `search-input-circular.default.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--search-input-circular-default-border-disabled` | `search-input-circular.default.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--search-input-circular-default-text-default` | `search-input-circular.default.text.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--search-input-circular-default-text-placeholder` | `search-input-circular.default.text.placeholder` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--search-input-circular-default-text-disabled` | `search-input-circular.default.text.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--search-input-circular-default-focus-ring` | `search-input-circular.default.focusRing` | `{palette.blue-sky.200}` | `#ccdef6` | = light |

**`search-input-circular.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-destructive-border-default` | `search-input-circular.destructive.border.default` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--search-input-circular-destructive-border-hover` | `search-input-circular.destructive.border.hover` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--search-input-circular-destructive-border-focus` | `search-input-circular.destructive.border.focus` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--search-input-circular-destructive-focus-ring` | `search-input-circular.destructive.focusRing` | `{palette.red.200}` | `#fecdc9` | = light |

**`search-input-circular.submitButton`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-submit-button-size-md` | `search-input-circular.submitButton.size.md` | `{spacing.32}` | `32px` | = light |
| `--search-input-circular-submit-button-size-lg` | `search-input-circular.submitButton.size.lg` | `{spacing.40}` | `40px` | = light |
| `--search-input-circular-submit-button-icon-size-md` | `search-input-circular.submitButton.iconSize.md` | `{spacing.16}` | `16px` | = light |
| `--search-input-circular-submit-button-icon-size-lg` | `search-input-circular.submitButton.iconSize.lg` | `{spacing.20}` | `20px` | = light |
| `--search-input-circular-submit-button-border-radius` | `search-input-circular.submitButton.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--search-input-circular-submit-button-background-default` | `search-input-circular.submitButton.background.default` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--search-input-circular-submit-button-background-hover` | `search-input-circular.submitButton.background.hover` | `{color.background.brand.default-hover}` → `{palette.blue-sky.700}` | `#0046a8` | = light |
| `--search-input-circular-submit-button-background-active` | `search-input-circular.submitButton.background.active` | `{color.background.brand.default-active}` → `{palette.blue-sky.800}` | `#00357e` | = light |
| `--search-input-circular-submit-button-background-disabled` | `search-input-circular.submitButton.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--search-input-circular-submit-button-icon-default` | `search-input-circular.submitButton.icon.default` | `{color.icon.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--search-input-circular-submit-button-icon-disabled` | `search-input-circular.submitButton.icon.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--search-input-circular-submit-button-focus-ring-offset` | `search-input-circular.submitButton.focusRingOffset` | `{borderWidth.2}` | `2px` | = light |
| `--search-input-circular-submit-button-container-padding-inline-end` | `search-input-circular.submitButton.containerPaddingInlineEnd` | `{spacing.4}` | `4px` | = light |

**`search-input-circular.loadingSpinner`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--search-input-circular-loading-spinner-size-md` | `search-input-circular.loadingSpinner.size.md` | `{spacing.20}` | `20px` | = light |
| `--search-input-circular-loading-spinner-size-lg` | `search-input-circular.loadingSpinner.size.lg` | `{spacing.24}` | `24px` | = light |
| `--search-input-circular-loading-spinner-color-default` | `search-input-circular.loadingSpinner.color.default` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--search-input-circular-loading-spinner-color-disabled` | `search-input-circular.loadingSpinner.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

Note din sursă (`$comment`):

- `--search-input-circular-container-padding-inline-md` — Figma master 933:29731 uses spacing.12 at md (same as rectangular sibling) — the pill silhouette doesn't warrant extra inset.
- `--search-input-circular-container-padding-inline-lg` — Figma master 933:29722 uses spacing.16 at lg (same as rectangular sibling).
- `--search-input-circular-icon-end-clear-background-default` — Per Figma 933:29806 — the clear button carries a subtle gray oval at rest in the Filled state. Hardcoded color in Figma is #1212121a; base.tertiary (#f1f1f1) is the closest design-system semantic.

### Select input (filtru dropdown) — `tokens/core/components/select-input.tokens.json`

_93 tokens._

**`selectInput.field`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-field-gap` | `selectInput.field.gap` | `{spacing.8}` | `8px` | = light |

**`selectInput.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-container-height-md` | `selectInput.container.height.md` | `{spacing.40}` | `40px` | = light |
| `--select-input-container-height-lg` | `selectInput.container.height.lg` | `{spacing.48}` | `48px` | = light |
| `--select-input-container-padding-inline-md` | `selectInput.container.paddingInline.md` | `{spacing.12}` | `12px` | = light |
| `--select-input-container-padding-inline-lg` | `selectInput.container.paddingInline.lg` | `{spacing.16}` | `16px` | = light |
| `--select-input-container-gap-md` | `selectInput.container.gap.md` | `{spacing.8}` | `8px` | = light |
| `--select-input-container-gap-lg` | `selectInput.container.gap.lg` | `{spacing.8}` | `8px` | = light |
| `--select-input-container-border-radius` | `selectInput.container.borderRadius` | `{borderRadius.8}` | `8px` | = light |
| `--select-input-container-border-width-default` | `selectInput.container.borderWidth.default` | `{borderWidth.1}` | `1px` | = light |
| `--select-input-container-border-width-emphasized` | `selectInput.container.borderWidth.emphasized` | `{borderWidth.2}` | `2px` | = light |
| `--select-input-container-focus-ring-width` | `selectInput.container.focusRingWidth` | `{borderWidth.2}` | `2px` | = light |
| `--select-input-container-transition-duration` | `selectInput.container.transitionDuration` | — | `150ms` | = light |
| `--select-input-container-transition-timing-function` | `selectInput.container.transitionTimingFunction` | — | `ease-out` | = light |

**`selectInput.control`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-control-font-family` | `selectInput.control.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--select-input-control-font-weight` | `selectInput.control.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--select-input-control-font-size-md` | `selectInput.control.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--select-input-control-font-size-lg` | `selectInput.control.fontSize.lg` | `{fontSize.16}` | `16px` | = light |
| `--select-input-control-line-height-md` | `selectInput.control.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--select-input-control-line-height-lg` | `selectInput.control.lineHeight.lg` | `{lineHeight.24}` | `24px` | = light |

**`selectInput.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-label-font-family` | `selectInput.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--select-input-label-font-size` | `selectInput.label.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--select-input-label-line-height` | `selectInput.label.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--select-input-label-font-weight` | `selectInput.label.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--select-input-label-gap` | `selectInput.label.gap` | `{spacing.4}` | `4px` | = light |
| `--select-input-label-color-default` | `selectInput.label.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--select-input-label-color-disabled` | `selectInput.label.color.disabled` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--select-input-label-required-mark-size` | `selectInput.label.requiredMark.size` | `{spacing.12}` | `12px` | = light |
| `--select-input-label-required-mark-color` | `selectInput.label.requiredMark.color` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`selectInput.assistive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-assistive-font-family` | `selectInput.assistive.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--select-input-assistive-font-size` | `selectInput.assistive.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--select-input-assistive-line-height` | `selectInput.assistive.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--select-input-assistive-font-weight` | `selectInput.assistive.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--select-input-assistive-gap-default` | `selectInput.assistive.gap.default` | `{spacing.6}` | `6px` | = light |
| `--select-input-assistive-gap-error` | `selectInput.assistive.gap.error` | `{spacing.4}` | `4px` | = light |
| `--select-input-assistive-icon-size` | `selectInput.assistive.iconSize` | `{spacing.20}` | `20px` | = light |
| `--select-input-assistive-color-default` | `selectInput.assistive.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--select-input-assistive-color-error` | `selectInput.assistive.color.error` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`selectInput.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-icon-size-md` | `selectInput.icon.size.md` | `{spacing.20}` | `20px` | = light |
| `--select-input-icon-size-lg` | `selectInput.icon.size.lg` | `{spacing.24}` | `24px` | = light |
| `--select-input-icon-color-default` | `selectInput.icon.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--select-input-icon-color-disabled` | `selectInput.icon.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`selectInput.chevron`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-chevron-size-md` | `selectInput.chevron.size.md` | `{spacing.20}` | `20px` | = light |
| `--select-input-chevron-size-lg` | `selectInput.chevron.size.lg` | `{spacing.24}` | `24px` | = light |
| `--select-input-chevron-color-default` | `selectInput.chevron.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--select-input-chevron-color-disabled` | `selectInput.chevron.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--select-input-chevron-transition-duration` | `selectInput.chevron.transitionDuration` | — | `150ms` | = light |
| `--select-input-chevron-transition-timing-function` | `selectInput.chevron.transitionTimingFunction` | — | `ease-out` | = light |

**`selectInput.default`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-default-background-default` | `selectInput.default.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--select-input-default-background-disabled` | `selectInput.default.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--select-input-default-border-default` | `selectInput.default.border.default` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--select-input-default-border-hover` | `selectInput.default.border.hover` | `{color.border.base.tertiary}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--select-input-default-border-focus` | `selectInput.default.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--select-input-default-border-disabled` | `selectInput.default.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--select-input-default-text-default` | `selectInput.default.text.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--select-input-default-text-placeholder` | `selectInput.default.text.placeholder` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--select-input-default-text-disabled` | `selectInput.default.text.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--select-input-default-focus-ring` | `selectInput.default.focusRing` | `{palette.blue-sky.200}` | `#ccdef6` | = light |

**`selectInput.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-destructive-border-default` | `selectInput.destructive.border.default` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--select-input-destructive-border-hover` | `selectInput.destructive.border.hover` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--select-input-destructive-border-focus` | `selectInput.destructive.border.focus` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--select-input-destructive-focus-ring` | `selectInput.destructive.focusRing` | `{palette.red.200}` | `#fecdc9` | = light |

**`selectInput.listbox`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-listbox-background` | `selectInput.listbox.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--select-input-listbox-border-radius` | `selectInput.listbox.borderRadius` | `{borderRadius.16}` | `16px` | = light |
| `--select-input-listbox-border-width` | `selectInput.listbox.borderWidth` | — | `0px` | = light |
| `--select-input-listbox-border-color` | `selectInput.listbox.borderColor` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--select-input-listbox-padding` | `selectInput.listbox.padding` | `{spacing.8}` | `8px` | = light |
| `--select-input-listbox-gap` | `selectInput.listbox.gap` | `{spacing.4}` | `4px` | = light |
| `--select-input-listbox-margin-block-start` | `selectInput.listbox.marginBlockStart` | `{spacing.4}` | `4px` | = light |
| `--select-input-listbox-shadow` | `selectInput.listbox.shadow` | `{dropShadow.300}` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` | = light |
| `--select-input-listbox-max-block-size` | `selectInput.listbox.maxBlockSize` | — | `320px` | = light |

**`selectInput.option`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--select-input-option-min-block-size-md` | `selectInput.option.minBlockSize.md` | `{spacing.40}` | `40px` | = light |
| `--select-input-option-min-block-size-lg` | `selectInput.option.minBlockSize.lg` | `{spacing.40}` | `40px` | = light |
| `--select-input-option-padding-inline` | `selectInput.option.paddingInline` | `{spacing.16}` | `16px` | = light |
| `--select-input-option-padding-block` | `selectInput.option.paddingBlock` | `{spacing.12}` | `12px` | = light |
| `--select-input-option-border-radius` | `selectInput.option.borderRadius` | `{borderRadius.8}` | `8px` | = light |
| `--select-input-option-gap` | `selectInput.option.gap` | `{spacing.12}` | `12px` | = light |
| `--select-input-option-font-family` | `selectInput.option.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--select-input-option-font-size-md` | `selectInput.option.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--select-input-option-font-size-lg` | `selectInput.option.fontSize.lg` | `{fontSize.14}` | `14px` | = light |
| `--select-input-option-line-height-md` | `selectInput.option.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--select-input-option-line-height-lg` | `selectInput.option.lineHeight.lg` | `{lineHeight.20}` | `20px` | = light |
| `--select-input-option-font-weight-default` | `selectInput.option.fontWeight.default` | `{fontWeight.medium}` | `500` | = light |
| `--select-input-option-font-weight-selected` | `selectInput.option.fontWeight.selected` | `{fontWeight.medium}` | `500` | = light |
| `--select-input-option-icon-size-md` | `selectInput.option.iconSize.md` | `{spacing.20}` | `20px` | = light |
| `--select-input-option-icon-size-lg` | `selectInput.option.iconSize.lg` | `{spacing.24}` | `24px` | = light |
| `--select-input-option-background-default` | `selectInput.option.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--select-input-option-background-hover` | `selectInput.option.background.hover` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--select-input-option-background-active` | `selectInput.option.background.active` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--select-input-option-background-selected` | `selectInput.option.background.selected` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--select-input-option-text-default` | `selectInput.option.text.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--select-input-option-text-selected` | `selectInput.option.text.selected` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--select-input-option-text-disabled` | `selectInput.option.text.disabled` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--select-input-option-check-color` | `selectInput.option.checkColor` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |

Note din sursă (`$comment`):

- `--select-input-label-font-weight` — Per Figma 454:4192 — Label uses Onest Regular (fw-regular 400), not medium.
- `--select-input-label-color-disabled` — Stays at base.secondary in disabled state; on-disabled (#b2b2b2) fails WCAG AA on the disabled grey surface. Figma's Disabled SelectInput keeps the label dark.
- `--select-input-listbox-border-radius` — Figma selection-menu 172:3093 uses border-radius/16.
- `--select-input-listbox-border-width` — Figma selection-menu has no border — depth comes from dropShadow.300 only.
- `--select-input-listbox-padding` — Figma selection-menu uses spacing/8 around the option list.
- `--select-input-listbox-gap` — Figma selection-menu uses spacing/4 between options.
- `--select-input-option-min-block-size-md` — Figma menu-item-selection: 12px paddingBlock + 16px lh = 40px clickable target.
- `--select-input-option-min-block-size-lg` — Floor on the clickable target — intrinsic content (12px paddingBlock × 2 + 20px lh) drives the rendered height to ~44px.
- `--select-input-option-padding-inline` — Figma menu-item-selection uses spacing/16 left/right.
- `--select-input-option-padding-block` — Figma menu-item-selection uses spacing/12 top/bottom.
- `--select-input-option-border-radius` — Figma menu-item-selection uses border-radius/8.
- `--select-input-option-gap` — Figma menu-item-selection puts spacing/12 between label and check icon.
- `--select-input-option-font-size-lg` — Figma menu-item-selection renders Body/Small 500 (14px) at both rungs.
- `--select-input-option-font-weight-default` — Figma menu-item-selection Default state uses Onest Medium (500), matching the Selected weight.
- `--select-input-option-icon-size-lg` — Figma menu-item-selection renders the 24/checkmark-small icon at 24px.
- `--select-input-option-text-default` — Figma menu-item-selection Default text is color.text.base.secondary (#383838).
- `--select-input-option-text-disabled` — Disabled options keep dark-enough contrast on the white surface; text.disabled.on-disabled fails AA on white.

### Meniu (meniu de coloană / acțiuni) — `tokens/core/components/menu.tokens.json`

_43 tokens._

**`menu.panel`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--menu-panel-background` | `menu.panel.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--menu-panel-border-radius` | `menu.panel.borderRadius` | `{borderRadius.16}` | `16px` | = light |
| `--menu-panel-border-width` | `menu.panel.borderWidth` | — | `0px` | = light |
| `--menu-panel-border-color` | `menu.panel.borderColor` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--menu-panel-padding` | `menu.panel.padding` | `{spacing.8}` | `8px` | = light |
| `--menu-panel-gap` | `menu.panel.gap` | `{spacing.4}` | `4px` | = light |
| `--menu-panel-min-inline-size` | `menu.panel.minInlineSize` | — | `270px` | = light |
| `--menu-panel-max-block-size` | `menu.panel.maxBlockSize` | — | `320px` | = light |
| `--menu-panel-shadow` | `menu.panel.shadow` | `{dropShadow.300}` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` | = light |

**`menu.item`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--menu-item-min-block-size` | `menu.item.minBlockSize` | `{spacing.40}` | `40px` | = light |
| `--menu-item-padding-block` | `menu.item.paddingBlock` | `{spacing.12}` | `12px` | = light |
| `--menu-item-padding-inline-start` | `menu.item.paddingInlineStart` | `{spacing.16}` | `16px` | = light |
| `--menu-item-padding-inline-end` | `menu.item.paddingInlineEnd` | `{spacing.24}` | `24px` | = light |
| `--menu-item-padding-inline-end-trailing` | `menu.item.paddingInlineEndTrailing` | `{spacing.16}` | `16px` | = light |
| `--menu-item-border-radius` | `menu.item.borderRadius` | `{borderRadius.8}` | `8px` | = light |
| `--menu-item-gap` | `menu.item.gap` | `{spacing.12}` | `12px` | = light |
| `--menu-item-font-family` | `menu.item.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--menu-item-font-size` | `menu.item.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--menu-item-line-height` | `menu.item.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--menu-item-font-weight` | `menu.item.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--menu-item-background-default` | `menu.item.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--menu-item-background-hover` | `menu.item.background.hover` | `{color.background.base.default-hover}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--menu-item-background-active` | `menu.item.background.active` | `{color.background.base.default-active}` → `{palette.gray.250}` | `#d9d9d9` | `#383838` |
| `--menu-item-background-selected` | `menu.item.background.selected` | `{color.background.base.secondary}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--menu-item-text-default` | `menu.item.text.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--menu-item-text-selected` | `menu.item.text.selected` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--menu-item-text-disabled` | `menu.item.text.disabled` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--menu-item-icon-size` | `menu.item.icon.size` | `{spacing.20}` | `20px` | = light |
| `--menu-item-icon-color-default` | `menu.item.icon.color.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--menu-item-icon-color-disabled` | `menu.item.icon.color.disabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--menu-item-check-size` | `menu.item.check.size` | `{spacing.20}` | `20px` | = light |
| `--menu-item-check-color` | `menu.item.check.color` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |

**`menu.heading`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--menu-heading-padding-inline` | `menu.heading.paddingInline` | `{spacing.16}` | `16px` | = light |
| `--menu-heading-padding-block` | `menu.heading.paddingBlock` | `{spacing.4}` | `4px` | = light |
| `--menu-heading-gap` | `menu.heading.gap` | `{spacing.4}` | `4px` | = light |
| `--menu-heading-font-family` | `menu.heading.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--menu-heading-font-size` | `menu.heading.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--menu-heading-line-height` | `menu.heading.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--menu-heading-font-weight` | `menu.heading.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--menu-heading-color` | `menu.heading.color` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--menu-heading-separator-color` | `menu.heading.separatorColor` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |

**`menu.focusRing`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--menu-focus-ring-color` | `menu.focusRing.color` | `{palette.blue-sky.500}` | `#3379db` | = light |
| `--menu-focus-ring-width` | `menu.focusRing.width` | `{borderWidth.2}` | `2px` | = light |

Note din sursă (`$comment`):

- `--menu-panel-border-radius` — Figma selection-menu 172:3093 / contextual-menu 409:22462 use border-radius/16.
- `--menu-panel-border-width` — Figma menu has no border — depth comes from dropShadow.300 only.
- `--menu-panel-padding` — Figma menu uses spacing/8 around the item list.
- `--menu-panel-gap` — Figma menu uses spacing/4 between items.
- `--menu-panel-min-inline-size` — Figma menu fixed width 270px — used as the floor so the panel can grow with content.
- `--menu-panel-max-block-size` — Scroll threshold; mirrors select-input listbox.maxBlockSize and the Figma scrollable-content example.
- `--menu-item-min-block-size` — Figma menu-item: 12px paddingBlock + 20px lh = 44px rendered; 40px floor on the clickable target.
- `--menu-item-padding-inline-end` — Figma default item pr-24; collapses to spacing.16 when a trailing checkmark is present.
- `--menu-item-padding-inline-end-trailing` — Figma selected/selection item with trailing checkmark uses px-16.
- `--menu-item-gap` — Figma menu-item puts spacing/12 between leading/label/trailing.
- `--menu-item-font-weight` — Figma menu-item Default + Selected both render Onest Medium (500).
- `--menu-item-background-hover` — Figma menu-item Hover bg #f5f5f5.
- `--menu-item-background-active` — Figma menu-item Active bg #d9d9d9.
- `--menu-item-background-selected` — Figma selection-menu Selected item bg #f5f5f5.
- `--menu-item-text-default` — Figma menu-item Default text #383838.
- `--menu-item-text-selected` — Figma selection-menu Selected text #0058d2.
- `--menu-item-text-disabled` — Figma menu-item Disabled text #b2b2b2. Disabled text is exempt from WCAG 1.4.3 contrast (SC applies to enabled UI only).
- `--menu-item-icon-size` — Leading icon glyph sized to 20px within the 24px Figma frame.
- `--menu-item-icon-color-default` — Figma leading icon #121212.
- `--menu-item-check-size` — Trailing 24/checkmark-small glyph for a selected selection-item; verify 20 vs 24 in pixel QA.
- `--menu-heading-color` — Figma Section Heading label #757575.
- `--menu-heading-separator-color` — Figma Section Heading separator (4px tall divider above the label).
- `--menu-focus-ring-color` — Figma 'Focus Ring/Small: Inner' = inner shadow blue-sky/500 (#3379db), spread 2. Reconcile vs focusRing.tokens.json in CSS.
- `--menu-focus-ring-width` — Figma focus ring spread = 2px.

### Segmented control (filtre exclusive) — `tokens/core/components/segmented-control.tokens.json`

_38 tokens._

**`segmented-control.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-container-background` | `segmented-control.container.background` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--segmented-control-container-background-disabled` | `segmented-control.container.background-disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--segmented-control-container-border-radius` | `segmented-control.container.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--segmented-control-container-padding` | `segmented-control.container.padding` | `{spacing.6}` | `6px` | = light |
| `--segmented-control-container-gap` | `segmented-control.container.gap` | `{spacing.6}` | `6px` | = light |
| `--segmented-control-container-transition-duration` | `segmented-control.container.transitionDuration` | — | `150ms` | = light |
| `--segmented-control-container-transition-timing-function` | `segmented-control.container.transitionTimingFunction` | — | `ease-out` | = light |

**`segmented-control.segment`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-segment-height-md` | `segmented-control.segment.height.md` | `{spacing.40}` | `40px` | = light |
| `--segmented-control-segment-height-sm` | `segmented-control.segment.height.sm` | `{spacing.32}` | `32px` | = light |
| `--segmented-control-segment-min-width` | `segmented-control.segment.minWidth` | — | `68px` | = light |
| `--segmented-control-segment-padding-inline-md` | `segmented-control.segment.paddingInline.md` | `{spacing.16}` | `16px` | = light |
| `--segmented-control-segment-padding-inline-sm` | `segmented-control.segment.paddingInline.sm` | `{spacing.12}` | `12px` | = light |
| `--segmented-control-segment-gap` | `segmented-control.segment.gap` | `{spacing.6}` | `6px` | = light |
| `--segmented-control-segment-border-radius` | `segmented-control.segment.borderRadius` | `{borderRadius.full}` | `9999px` | = light |

**`segmented-control.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-label-font-family` | `segmented-control.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--segmented-control-label-font-size-md` | `segmented-control.label.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--segmented-control-label-font-size-sm` | `segmented-control.label.fontSize.sm` | `{fontSize.14}` | `14px` | = light |
| `--segmented-control-label-line-height-md` | `segmented-control.label.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--segmented-control-label-line-height-sm` | `segmented-control.label.lineHeight.sm` | `{lineHeight.20}` | `20px` | = light |
| `--segmented-control-label-font-weight-selected` | `segmented-control.label.fontWeightSelected` | `{fontWeight.medium}` | `500` | = light |
| `--segmented-control-label-font-weight-unselected` | `segmented-control.label.fontWeightUnselected` | `{fontWeight.regular}` | `400` | = light |

**`segmented-control.selected`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-selected-background` | `segmented-control.selected.background` | `{color.background.base-inverse.default}` → `{palette.gray.900}` | `#1e1e1e` | `#f5f5f5` |
| `--segmented-control-selected-background-hover` | `segmented-control.selected.backgroundHover` | `{color.background.base-inverse.default-hover}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--segmented-control-selected-color` | `segmented-control.selected.color` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--segmented-control-selected-shadow` | `segmented-control.selected.shadow` | — | `0px 0px 0.25px rgba(0, 0, 0, 0.30), 0px 1px 1.5px rgba(0, 0, 0, 0.16)` | = light |

**`segmented-control.unselected`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-unselected-background` | `segmented-control.unselected.background` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--segmented-control-unselected-background-hover` | `segmented-control.unselected.backgroundHover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--segmented-control-unselected-color` | `segmented-control.unselected.color` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--segmented-control-unselected-color-hover` | `segmented-control.unselected.colorHover` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |

**`segmented-control.disabled`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-disabled-background` | `segmented-control.disabled.background` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--segmented-control-disabled-color` | `segmented-control.disabled.color` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |

**`segmented-control.separator`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-separator-color` | `segmented-control.separator.color` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--segmented-control-separator-width` | `segmented-control.separator.width` | `{borderWidth.1}` | `1px` | = light |
| `--segmented-control-separator-height` | `segmented-control.separator.height` | `{spacing.16}` | `16px` | = light |

**`segmented-control.focusRing`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--segmented-control-focus-ring-inner-width` | `segmented-control.focusRing.innerWidth` | `{focusRing.width.inner}` | `2px` | = light |
| `--segmented-control-focus-ring-outer-width` | `segmented-control.focusRing.outerWidth` | `{focusRing.width.outer}` | `3px` | = light |
| `--segmented-control-focus-ring-inner-color` | `segmented-control.focusRing.innerColor` | `{focusRing.color.inner}` → `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--segmented-control-focus-ring-outer-color` | `segmented-control.focusRing.outerColor` | `{focusRing.color.outer}` → `{color.background.brand.focus-ring}` → `{palette.blue-sky.500}` | `#3379db` | = light |

### Tabs (filtre pe categorii) — `tokens/core/components/tabs.tokens.json`

_61 tokens._

**`tabs.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-container-gap` | `tabs.container.gap` | `{spacing.0}` | `0px` | = light |
| `--tabs-container-border-color` | `tabs.container.borderColor` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--tabs-container-border-width` | `tabs.container.borderWidth` | `{borderWidth.1}` | `1px` | = light |
| `--tabs-container-transition-duration` | `tabs.container.transitionDuration` | — | `150ms` | = light |
| `--tabs-container-transition-timing-function` | `tabs.container.transitionTimingFunction` | — | `ease-out` | = light |

**`tabs.tab`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-tab-height-md` | `tabs.tab.height.md` | `{spacing.48}` | `48px` | = light |
| `--tabs-tab-height-sm` | `tabs.tab.height.sm` | `{spacing.40}` | `40px` | = light |
| `--tabs-tab-padding-inline-md` | `tabs.tab.paddingInline.md` | `{spacing.16}` | `16px` | = light |
| `--tabs-tab-padding-inline-sm` | `tabs.tab.paddingInline.sm` | `{spacing.12}` | `12px` | = light |
| `--tabs-tab-gap` | `tabs.tab.gap` | `{spacing.8}` | `8px` | = light |
| `--tabs-tab-inner-gap` | `tabs.tab.innerGap` | `{spacing.6}` | `6px` | = light |
| `--tabs-tab-indicator-height` | `tabs.tab.indicatorHeight` | `{borderWidth.2}` | `2px` | = light |
| `--tabs-tab-indicator-color` | `tabs.tab.indicatorColor` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--tabs-tab-min-width` | `tabs.tab.minWidth` | `{spacing.40}` | `40px` | = light |

**`tabs.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-label-font-family` | `tabs.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--tabs-label-font-size-md` | `tabs.label.fontSize.md` | `{fontSize.16}` | `16px` | = light |
| `--tabs-label-font-size-sm` | `tabs.label.fontSize.sm` | `{fontSize.14}` | `14px` | = light |
| `--tabs-label-line-height-md` | `tabs.label.lineHeight.md` | `{lineHeight.24}` | `24px` | = light |
| `--tabs-label-line-height-sm` | `tabs.label.lineHeight.sm` | `{lineHeight.20}` | `20px` | = light |
| `--tabs-label-font-weight-selected` | `tabs.label.fontWeightSelected` | `{fontWeight.medium}` | `500` | = light |
| `--tabs-label-font-weight-unselected` | `tabs.label.fontWeightUnselected` | `{fontWeight.regular}` | `400` | = light |

**`tabs.selected`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-selected-color` | `tabs.selected.color` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--tabs-selected-icon-color` | `tabs.selected.iconColor` | `{color.icon.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |

**`tabs.unselected`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-unselected-color` | `tabs.unselected.color` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--tabs-unselected-color-hover` | `tabs.unselected.colorHover` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--tabs-unselected-icon-color` | `tabs.unselected.iconColor` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--tabs-unselected-icon-color-hover` | `tabs.unselected.iconColorHover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |

**`tabs.disabled`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-disabled-color` | `tabs.disabled.color` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--tabs-disabled-icon-color` | `tabs.disabled.iconColor` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |

**`tabs.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-icon-size-md` | `tabs.icon.size.md` | `{spacing.20}` | `20px` | = light |
| `--tabs-icon-size-sm` | `tabs.icon.size.sm` | `{spacing.20}` | `20px` | = light |

**`tabs.badge`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-badge-background` | `tabs.badge.background` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--tabs-badge-color` | `tabs.badge.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--tabs-badge-min-size-md` | `tabs.badge.minSize.md` | `{spacing.24}` | `24px` | = light |
| `--tabs-badge-min-size-sm` | `tabs.badge.minSize.sm` | `{spacing.20}` | `20px` | = light |
| `--tabs-badge-padding-inline-md` | `tabs.badge.paddingInline.md` | `{spacing.8}` | `8px` | = light |
| `--tabs-badge-padding-inline-sm` | `tabs.badge.paddingInline.sm` | `{spacing.4}` | `4px` | = light |
| `--tabs-badge-border-radius` | `tabs.badge.borderRadius` | `{borderRadius.full}` | `9999px` | = light |
| `--tabs-badge-font-size-md` | `tabs.badge.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--tabs-badge-font-size-sm` | `tabs.badge.fontSize.sm` | `{fontSize.12}` | `12px` | = light |
| `--tabs-badge-line-height-md` | `tabs.badge.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--tabs-badge-line-height-sm` | `tabs.badge.lineHeight.sm` | `{lineHeight.16}` | `16px` | = light |
| `--tabs-badge-font-weight` | `tabs.badge.fontWeight` | `{fontWeight.medium}` | `500` | = light |

**`tabs.overflow`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-overflow-fade-width-md` | `tabs.overflow.fadeWidth.md` | `{spacing.48}` | `48px` | = light |
| `--tabs-overflow-fade-width-sm` | `tabs.overflow.fadeWidth.sm` | `{spacing.32}` | `32px` | = light |
| `--tabs-overflow-chevron-size-md` | `tabs.overflow.chevron.size.md` | `{spacing.24}` | `24px` | = light |
| `--tabs-overflow-chevron-size-sm` | `tabs.overflow.chevron.size.sm` | `{spacing.20}` | `20px` | = light |
| `--tabs-overflow-chevron-color` | `tabs.overflow.chevron.color` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--tabs-overflow-chevron-color-hover` | `tabs.overflow.chevron.colorHover` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--tabs-overflow-chevron-color-disabled` | `tabs.overflow.chevron.colorDisabled` | `{color.icon.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--tabs-overflow-chevron-background` | `tabs.overflow.chevron.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--tabs-overflow-chevron-padding-block-md` | `tabs.overflow.chevron.paddingBlock.md` | `{spacing.12}` | `12px` | = light |
| `--tabs-overflow-chevron-padding-block-sm` | `tabs.overflow.chevron.paddingBlock.sm` | `{spacing.8}` | `8px` | = light |
| `--tabs-overflow-chevron-padding-inline-md` | `tabs.overflow.chevron.paddingInline.md` | `{spacing.4}` | `4px` | = light |
| `--tabs-overflow-chevron-padding-inline-sm` | `tabs.overflow.chevron.paddingInline.sm` | `{spacing.4}` | `4px` | = light |

**`tabs.panel`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-panel-padding-block` | `tabs.panel.paddingBlock` | `{spacing.16}` | `16px` | = light |
| `--tabs-panel-color` | `tabs.panel.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |

**`tabs.focusRing`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--tabs-focus-ring-inner-width` | `tabs.focusRing.innerWidth` | `{focusRing.width.inner}` | `2px` | = light |
| `--tabs-focus-ring-outer-width` | `tabs.focusRing.outerWidth` | `{focusRing.width.outer}` | `3px` | = light |
| `--tabs-focus-ring-inner-color` | `tabs.focusRing.innerColor` | `{focusRing.color.inner}` → `{color.text.base-inverse.on-color}` → `{palette.white.1000}` | `#ffffff` | = light |
| `--tabs-focus-ring-outer-color` | `tabs.focusRing.outerColor` | `{focusRing.color.outer}` → `{color.background.brand.focus-ring}` → `{palette.blue-sky.500}` | `#3379db` | = light |

### Date input (filtru pe dată) — `tokens/core/components/date-input.tokens.json`

_56 tokens._

**`dateInput.field`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-field-gap` | `dateInput.field.gap` | `{spacing.8}` | `8px` | = light |

**`dateInput.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-container-height-md` | `dateInput.container.height.md` | `{spacing.40}` | `40px` | = light |
| `--date-input-container-height-lg` | `dateInput.container.height.lg` | `{spacing.48}` | `48px` | = light |
| `--date-input-container-padding-inline-md` | `dateInput.container.paddingInline.md` | `{spacing.12}` | `12px` | = light |
| `--date-input-container-padding-inline-lg` | `dateInput.container.paddingInline.lg` | `{spacing.16}` | `16px` | = light |
| `--date-input-container-gap-md` | `dateInput.container.gap.md` | `{spacing.8}` | `8px` | = light |
| `--date-input-container-gap-lg` | `dateInput.container.gap.lg` | `{spacing.8}` | `8px` | = light |
| `--date-input-container-border-radius` | `dateInput.container.borderRadius` | `{borderRadius.8}` | `8px` | = light |
| `--date-input-container-border-width-default` | `dateInput.container.borderWidth.default` | `{borderWidth.1}` | `1px` | = light |
| `--date-input-container-border-width-emphasized` | `dateInput.container.borderWidth.emphasized` | `{borderWidth.2}` | `2px` | = light |
| `--date-input-container-focus-ring-width` | `dateInput.container.focusRingWidth` | `{borderWidth.2}` | `2px` | = light |
| `--date-input-container-transition-duration` | `dateInput.container.transitionDuration` | — | `150ms` | = light |
| `--date-input-container-transition-timing-function` | `dateInput.container.transitionTimingFunction` | — | `ease-out` | = light |

**`dateInput.control`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-control-font-family` | `dateInput.control.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--date-input-control-font-weight` | `dateInput.control.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--date-input-control-font-size-md` | `dateInput.control.fontSize.md` | `{fontSize.14}` | `14px` | = light |
| `--date-input-control-font-size-lg` | `dateInput.control.fontSize.lg` | `{fontSize.16}` | `16px` | = light |
| `--date-input-control-line-height-md` | `dateInput.control.lineHeight.md` | `{lineHeight.20}` | `20px` | = light |
| `--date-input-control-line-height-lg` | `dateInput.control.lineHeight.lg` | `{lineHeight.24}` | `24px` | = light |
| `--date-input-control-caret-color` | `dateInput.control.caretColor` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |

**`dateInput.label`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-label-font-family` | `dateInput.label.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--date-input-label-font-size` | `dateInput.label.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--date-input-label-line-height` | `dateInput.label.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--date-input-label-font-weight` | `dateInput.label.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--date-input-label-gap` | `dateInput.label.gap` | `{spacing.4}` | `4px` | = light |
| `--date-input-label-color-default` | `dateInput.label.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--date-input-label-color-disabled` | `dateInput.label.color.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--date-input-label-required-mark-size` | `dateInput.label.requiredMark.size` | `{spacing.12}` | `12px` | = light |
| `--date-input-label-required-mark-color` | `dateInput.label.requiredMark.color` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`dateInput.assistive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-assistive-font-family` | `dateInput.assistive.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--date-input-assistive-font-size` | `dateInput.assistive.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--date-input-assistive-line-height` | `dateInput.assistive.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--date-input-assistive-font-weight` | `dateInput.assistive.fontWeight` | `{fontWeight.regular}` | `400` | = light |
| `--date-input-assistive-gap-default` | `dateInput.assistive.gap.default` | `{spacing.6}` | `6px` | = light |
| `--date-input-assistive-gap-error` | `dateInput.assistive.gap.error` | `{spacing.4}` | `4px` | = light |
| `--date-input-assistive-icon-size` | `dateInput.assistive.iconSize` | `{spacing.20}` | `20px` | = light |
| `--date-input-assistive-color-default` | `dateInput.assistive.color.default` | `{color.text.base.secondary}` → `{palette.gray.700}` | `#383838` | `#f1f1f1` |
| `--date-input-assistive-color-error` | `dateInput.assistive.color.error` | `{color.text.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |

**`dateInput.icon`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-icon-size-md` | `dateInput.icon.size.md` | `{spacing.20}` | `20px` | = light |
| `--date-input-icon-size-lg` | `dateInput.icon.size.lg` | `{spacing.24}` | `24px` | = light |
| `--date-input-icon-color-default` | `dateInput.icon.color.default` | `{color.icon.base.secondary}` → `{palette.gray.600}` | `#444444` | `#f1f1f1` |
| `--date-input-icon-color-disabled` | `dateInput.icon.color.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`dateInput.default`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-default-background-default` | `dateInput.default.background.default` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--date-input-default-background-disabled` | `dateInput.default.background.disabled` | `{color.background.disabled.default}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--date-input-default-border-default` | `dateInput.default.border.default` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--date-input-default-border-hover` | `dateInput.default.border.hover` | `{color.border.base.tertiary}` → `{palette.gray.600}` | `#444444` | `#d9d9d9` |
| `--date-input-default-border-focus` | `dateInput.default.border.focus` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--date-input-default-border-disabled` | `dateInput.default.border.disabled` | `{color.border.disabled.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--date-input-default-text-default` | `dateInput.default.text.default` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--date-input-default-text-placeholder` | `dateInput.default.text.placeholder` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--date-input-default-text-disabled` | `dateInput.default.text.disabled` | `{color.text.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |
| `--date-input-default-focus-ring` | `dateInput.default.focusRing` | `{palette.blue-sky.200}` | `#ccdef6` | = light |

**`dateInput.destructive`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-input-destructive-border-default` | `dateInput.destructive.border.default` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--date-input-destructive-border-hover` | `dateInput.destructive.border.hover` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--date-input-destructive-border-focus` | `dateInput.destructive.border.focus` | `{color.border.danger.default}` → `{palette.red.600}` | `#d92d20` | `#f04438` |
| `--date-input-destructive-focus-ring` | `dateInput.destructive.focusRing` | `{palette.red.200}` | `#fecdc9` | = light |

### Date picker (filtru pe interval) — `tokens/core/components/date-picker.tokens.json`

_70 tokens._

**`datePicker.container`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-container-width-desktop` | `datePicker.container.width.desktop` | — | `320px` | = light |
| `--date-picker-container-width-mobile` | `datePicker.container.width.mobile` | — | `100%` | = light |
| `--date-picker-container-width-docked` | `datePicker.container.width.docked` | — | `320px` | = light |
| `--date-picker-container-padding-desktop` | `datePicker.container.padding.desktop` | `{spacing.12}` | `12px` | = light |
| `--date-picker-container-padding-mobile` | `datePicker.container.padding.mobile` | `{spacing.16}` | `16px` | = light |
| `--date-picker-container-padding-docked` | `datePicker.container.padding.docked` | `{spacing.8}` | `8px` | = light |
| `--date-picker-container-gap` | `datePicker.container.gap` | `{spacing.8}` | `8px` | = light |
| `--date-picker-container-background` | `datePicker.container.background` | `{color.background.base.default}` → `{palette.white.1000}` | `#ffffff` | `#1e1e1e` |
| `--date-picker-container-border-radius` | `datePicker.container.borderRadius` | `{borderRadius.12}` | `12px` | = light |
| `--date-picker-container-border-width` | `datePicker.container.borderWidth` | `{borderWidth.1}` | `1px` | = light |
| `--date-picker-container-border-color` | `datePicker.container.borderColor` | `{color.border.base.default}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--date-picker-container-shadow` | `datePicker.container.shadow` | `{dropShadow.300}` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` | = light |
| `--date-picker-container-transition-duration` | `datePicker.container.transitionDuration` | — | `150ms` | = light |
| `--date-picker-container-transition-timing-function` | `datePicker.container.transitionTimingFunction` | — | `ease-out` | = light |

**`datePicker.header`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-header-height` | `datePicker.header.height` | `{spacing.40}` | `40px` | = light |
| `--date-picker-header-gap` | `datePicker.header.gap` | `{spacing.8}` | `8px` | = light |
| `--date-picker-header-title-font-family` | `datePicker.header.title.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--date-picker-header-title-font-weight` | `datePicker.header.title.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--date-picker-header-title-font-size` | `datePicker.header.title.fontSize` | `{fontSize.16}` | `16px` | = light |
| `--date-picker-header-title-line-height` | `datePicker.header.title.lineHeight` | `{lineHeight.24}` | `24px` | = light |
| `--date-picker-header-title-color` | `datePicker.header.title.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--date-picker-header-nav-button-size` | `datePicker.header.navButton.size` | `{spacing.40}` | `40px` | = light |
| `--date-picker-header-nav-button-border-radius` | `datePicker.header.navButton.borderRadius` | `{borderRadius.6}` | `6px` | = light |
| `--date-picker-header-nav-button-background-default` | `datePicker.header.navButton.background.default` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--date-picker-header-nav-button-background-hover` | `datePicker.header.navButton.background.hover` | `{color.background.base.tertiary-hover}` → `{palette.gray.250}` | `#d9d9d9` | `#444444` |
| `--date-picker-header-nav-button-background-active` | `datePicker.header.navButton.background.active` | `{color.background.base.tertiary-active}` → `{palette.gray.300}` | `#b2b2b2` | `#616161` |
| `--date-picker-header-nav-button-icon-size` | `datePicker.header.navButton.iconSize` | `{spacing.20}` | `20px` | = light |
| `--date-picker-header-nav-button-icon-color-default` | `datePicker.header.navButton.iconColor.default` | `{color.icon.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--date-picker-header-nav-button-icon-color-disabled` | `datePicker.header.navButton.iconColor.disabled` | `{color.icon.disabled.on-disabled}` → `{palette.gray.300}` | `#b2b2b2` | `#757575` |

**`datePicker.dayLabel`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-day-label-height` | `datePicker.dayLabel.height` | `{spacing.32}` | `32px` | = light |
| `--date-picker-day-label-font-family` | `datePicker.dayLabel.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--date-picker-day-label-font-weight` | `datePicker.dayLabel.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--date-picker-day-label-font-size` | `datePicker.dayLabel.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--date-picker-day-label-line-height` | `datePicker.dayLabel.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--date-picker-day-label-color` | `datePicker.dayLabel.color` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |

**`datePicker.dayCell`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-day-cell-height` | `datePicker.dayCell.height` | `{spacing.40}` | `40px` | = light |
| `--date-picker-day-cell-border-radius` | `datePicker.dayCell.borderRadius` | `{borderRadius.6}` | `6px` | = light |
| `--date-picker-day-cell-font-family` | `datePicker.dayCell.fontFamily` | `{fontFamily.primary}` | `Onest` | = light |
| `--date-picker-day-cell-font-weight` | `datePicker.dayCell.fontWeight` | `{fontWeight.medium}` | `500` | = light |
| `--date-picker-day-cell-font-size` | `datePicker.dayCell.fontSize` | `{fontSize.14}` | `14px` | = light |
| `--date-picker-day-cell-line-height` | `datePicker.dayCell.lineHeight` | `{lineHeight.20}` | `20px` | = light |
| `--date-picker-day-cell-border-width` | `datePicker.dayCell.borderWidth` | `{borderWidth.1-5}` | `1.5px` | = light |
| `--date-picker-day-cell-focus-ring-width` | `datePicker.dayCell.focusRingWidth` | `{borderWidth.3}` | `3px` | = light |
| `--date-picker-day-cell-focus-ring-offset` | `datePicker.dayCell.focusRingOffset` | `{borderWidth.1}` | `1px` | = light |
| `--date-picker-day-cell-default-color` | `datePicker.dayCell.default.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--date-picker-day-cell-hover-background` | `datePicker.dayCell.hover.background` | `{color.background.base.default-hover}` → `{palette.gray.100}` | `#f5f5f5` | `#2c2c2c` |
| `--date-picker-day-cell-hover-color` | `datePicker.dayCell.hover.color` | `{color.text.base.default}` → `{palette.black.1000}` | `#121212` | `#ffffff` |
| `--date-picker-day-cell-selected-background` | `datePicker.dayCell.selected.background` | `{color.background.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | = light |
| `--date-picker-day-cell-selected-color` | `datePicker.dayCell.selected.color` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--date-picker-day-cell-selected-hover-background` | `datePicker.dayCell.selectedHover.background` | `{color.background.brand.default-hover}` → `{palette.blue-sky.700}` | `#0046a8` | = light |
| `--date-picker-day-cell-selected-hover-color` | `datePicker.dayCell.selectedHover.color` | `{color.text.base-inverse.default}` → `{palette.white.1000}` | `#ffffff` | `#121212` |
| `--date-picker-day-cell-today-color` | `datePicker.dayCell.today.color` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--date-picker-day-cell-today-border-color` | `datePicker.dayCell.today.borderColor` | `{color.border.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#3379db` |
| `--date-picker-day-cell-today-hover-background` | `datePicker.dayCell.todayHover.background` | `{color.background.brand.secondary}` → `{palette.blue-sky.100}` | `#e8f0fb` | `#ccdef6` |
| `--date-picker-day-cell-today-hover-color` | `datePicker.dayCell.todayHover.color` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--date-picker-day-cell-in-range-background` | `datePicker.dayCell.inRange.background` | `{color.background.brand.secondary}` → `{palette.blue-sky.100}` | `#e8f0fb` | `#ccdef6` |
| `--date-picker-day-cell-in-range-color` | `datePicker.dayCell.inRange.color` | `{color.text.brand.default}` → `{palette.blue-sky.600}` | `#0058d2` | `#669be4` |
| `--date-picker-day-cell-outside-month-color` | `datePicker.dayCell.outsideMonth.color` | `{color.text.base.tertiary}` → `{palette.gray.400}` | `#757575` | `#b2b2b2` |
| `--date-picker-day-cell-disabled-color` | `datePicker.dayCell.disabled.color` | `{color.text.disabled.default}` → `{palette.gray.300}` | `#b2b2b2` | `#444444` |
| `--date-picker-day-cell-focus-ring` | `datePicker.dayCell.focusRing` | `{color.background.brand.focus-ring}` → `{palette.blue-sky.500}` | `#3379db` | = light |

**`datePicker.yearGrid`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-year-grid-row-gap` | `datePicker.yearGrid.rowGap` | `{spacing.8}` | `8px` | = light |
| `--date-picker-year-grid-cell-height` | `datePicker.yearGrid.cellHeight` | `{spacing.40}` | `40px` | = light |
| `--date-picker-year-grid-cell-border-radius` | `datePicker.yearGrid.cellBorderRadius` | `{borderRadius.8}` | `8px` | = light |

**`datePicker.monthGrid`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-month-grid-row-gap` | `datePicker.monthGrid.rowGap` | `{spacing.8}` | `8px` | = light |
| `--date-picker-month-grid-cell-height` | `datePicker.monthGrid.cellHeight` | `{spacing.40}` | `40px` | = light |
| `--date-picker-month-grid-cell-border-radius` | `datePicker.monthGrid.cellBorderRadius` | `{borderRadius.8}` | `8px` | = light |

**`datePicker.dragHandle`**

| Variabilă CSS | Token | Lanț de referințe | Light | Dark |
| --- | --- | --- | --- | --- |
| `--date-picker-drag-handle-width` | `datePicker.dragHandle.width` | `{spacing.48}` | `48px` | = light |
| `--date-picker-drag-handle-height` | `datePicker.dragHandle.height` | `{spacing.4}` | `4px` | = light |
| `--date-picker-drag-handle-background` | `datePicker.dragHandle.background` | `{color.background.base.tertiary}` → `{palette.gray.200}` | `#f1f1f1` | `#383838` |
| `--date-picker-drag-handle-border-radius` | `datePicker.dragHandle.borderRadius` | `{borderRadius.full}` | `9999px` | = light |

Note din sursă (`$comment`):

- `--date-picker-day-cell-focus-ring-width` — Figma 'Focus Ring/Small' blue spread = 3 (node 489:8302).
- `--date-picker-day-cell-focus-ring-offset` — Figma 'Focus Ring/Small' white halo spread = 1.
- `--date-picker-day-cell-hover-background` — #f5f5f5 per Figma 489:9011 (was base.tertiary #f1f1f1).
- `--date-picker-day-cell-focus-ring` — #3379db per Figma 'Focus Ring/Small' (node 489:8302); was palette.blue-sky.200 #ccdef6 — too pale (that's 'Focus Ring/Large') and a tier-purity violation. Semantic token matches mud-checkbox.

---

## 7. Bară de filtre — rețetă de asamblare

> ⚠️ **PROPUNERE. Tokenii din această secțiune NU există în repo.** Tot ce e mai sus e copiat
> din commit-ul `36f0786`; ce urmează e derivat de mine din scalele existente, ca punct de
> plecare. Dacă îi adopți, locul lor firesc e `tokens/core/components/table.tokens.json`, sub
> cheia `table.toolbar`, iar apoi rulezi `yarn tokens.build`.

Ce lipsește concret ca să pui filtre deasupra unui `mud-table`: distanțele dintre controale,
padding-ul barei, alinierea marginii ei cu marginea celulelor și comportamentul la lățime mică.
Toate se pot exprima prin scalele care există deja.

### 7.1 Tokens propuși

Valorile sunt alese ca bara să se alinieze cu tabelul: `paddingInline` copiază exact
`table.cell.paddingInline` (24 pe desktop, 16 sub 640px), astfel încât prima literă a unui filtru
să cadă pe aceeași verticală cu prima literă din prima coloană.

| Token propus | Referință | Valoare light | Valoare dark | Rol |
| --- | --- | --- | --- | --- |
| `table.toolbar.background` | `{color.background.base.default}` | `#ffffff` | `#1e1e1e` | fundalul barei — identic cu al tabelului |
| `table.toolbar.borderBottom` | `{color.border.base.default}` | `#d9d9d9` | `#444444` | linie de separare față de antet |
| `table.toolbar.minHeight` | `{spacing.64}` | `64px` | = light | înălțimea minimă a barei |
| `table.toolbar.paddingBlock` | `{spacing.12}` | `12px` | = light | spațiu vertical interior |
| `table.toolbar.paddingInline.desktop` | `{spacing.24}` | `24px` | = light | aliniere cu `table.cell.paddingInline.desktop` |
| `table.toolbar.paddingInline.mobile` | `{spacing.16}` | `16px` | = light | aliniere cu `table.cell.paddingInline.mobile` |
| `table.toolbar.gap` | `{spacing.16}` | `16px` | = light | între grupuri de controale (căutare ↔ filtre ↔ acțiuni) |
| `table.toolbar.gapItem` | `{spacing.8}` | `8px` | = light | între chip-uri vecine |
| `table.toolbar.gapRow` | `{spacing.8}` | `8px` | = light | între rânduri, când chip-urile se rup pe mai multe linii |

Ca fișier DTCG, de lipit în `tokens/core/components/table.tokens.json` lângă celelalte grupuri:

```json
"toolbar": {
  "background": { "$value": "{color.background.base.default}", "$type": "color" },
  "borderBottom": { "$value": "{color.border.base.default}", "$type": "color" },
  "minHeight": { "$value": "{spacing.64}", "$type": "dimension" },
  "paddingBlock": { "$value": "{spacing.12}", "$type": "dimension" },
  "paddingInline": {
    "desktop": { "$value": "{spacing.24}", "$type": "dimension" },
    "mobile": { "$value": "{spacing.16}", "$type": "dimension" }
  },
  "gap": { "$value": "{spacing.16}", "$type": "dimension" },
  "gapItem": { "$value": "{spacing.8}", "$type": "dimension" },
  "gapRow": { "$value": "{spacing.8}", "$type": "dimension" }
}
```

Cheia rădăcină rămâne `table` — un wrapper `"components"` ar genera prefixul parazit
`--components-table-*` și ar rupe toate referințele. Starea/scala stă întotdeauna ultima în
nume: `--table-toolbar-padding-inline-desktop`, nu `--table-toolbar-desktop-padding-inline`.

### 7.2 CSS-ul barei

Bara stă în lumina zilei (light DOM), nu în shadow root-ul tabelului, deci container query-ul
trebuie declarat pe wrapper-ul tău, nu pe `mud-table`.

```css
.table-shell {
  container-type: inline-size;
  container-name: table-shell;
  background: var(--table-container-background);
  border-radius: var(--table-container-border-radius);
}

.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--table-toolbar-gap-row) var(--table-toolbar-gap);
  min-height: var(--table-toolbar-min-height);
  padding-block: var(--table-toolbar-padding-block);
  padding-inline: var(--table-toolbar-padding-inline-desktop);
  background: var(--table-toolbar-background);
  border-bottom: 1px solid var(--table-toolbar-border-bottom);
  font-family: var(--table-container-font-family);
}

.table-toolbar__filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--table-toolbar-gap-item);
}

.table-toolbar__search { flex: 1 1 280px; min-width: 0; }
.table-toolbar__actions { margin-inline-start: auto; }

/* Același prag ca al tabelului — 640px. Literal, fiindcă @container nu citește variabile. */
@container table-shell (max-width: 640px) {
  .table-toolbar { padding-inline: var(--table-toolbar-padding-inline-mobile); }
  .table-toolbar__actions { margin-inline-start: 0; }
}
```

### 7.3 Markup

```html
<div class="table-shell">
  <div class="table-toolbar" role="search">
    <mud-search-input-rectangular
      class="table-toolbar__search"
      placeholder="Caută în tabel"
    ></mud-search-input-rectangular>

    <div class="table-toolbar__filters">
      <mud-chip type="filter" selection-mode="multi" size="md">Active</mud-chip>
      <mud-chip type="filter" selection-mode="multi" size="md">În procesare</mud-chip>
      <mud-chip type="filter" selection-mode="multi" size="md">Arhivate</mud-chip>
    </div>

    <mud-select-input label="Instituție"></mud-select-input>
    <mud-date-input label="De la"></mud-date-input>

    <div class="table-toolbar__actions">
      <mud-button variant="secondary" size="md">Resetează</mud-button>
    </div>
  </div>

  <mud-table
    selectable
    hoverable
    header-style="default"
    row-style="divided"
  ></mud-table>
</div>
```

Filtrarea propriu-zisă rămâne la tine: ascultă evenimentele controalelor, filtrează masivul de
date și trimite rezultatul înapoi în `rows`. `mud-table` nu are stare internă de filtrare.

### 7.4 Recomandări de accesibilitate

- Bara primește `role="search"` doar dacă include un câmp de căutare; altfel folosește
  `role="group"` cu `aria-label`.
- Chip-urile de filtrare cu `selectionMode="multi"` trebuie să expună starea prin
  `aria-pressed`; verifică ce emite `mud-chip` în versiunea ta înainte să adaugi tu atribute.
- După aplicarea unui filtru, anunță numărul de rezultate într-o regiune `aria-live="polite"` —
  altfel utilizatorii de cititor de ecran nu află că tabelul s-a schimbat.
- Inelul de focus al controalelor din bară folosește deja `--focus-ring-*` (§3.6); nu-l
  suprascrie cu `outline: none`.

---

## 8. Anexă — index alfabetic al variabilelor CSS

_1383 variabile._

| Variabilă CSS | Sursă | Light |
| --- | --- | --- |
| `--border-radius-0` | `fundament/sizes` | `0px` |
| `--border-radius-12` | `fundament/sizes` | `12px` |
| `--border-radius-16` | `fundament/sizes` | `16px` |
| `--border-radius-24` | `fundament/sizes` | `24px` |
| `--border-radius-32` | `fundament/sizes` | `32px` |
| `--border-radius-4` | `fundament/sizes` | `4px` |
| `--border-radius-6` | `fundament/sizes` | `6px` |
| `--border-radius-8` | `fundament/sizes` | `8px` |
| `--border-radius-full` | `fundament/sizes` | `9999px` |
| `--border-width-0-5` | `fundament/sizes` | `0.5px` |
| `--border-width-1` | `fundament/sizes` | `1px` |
| `--border-width-1-5` | `fundament/sizes` | `1.5px` |
| `--border-width-2` | `fundament/sizes` | `2px` |
| `--border-width-3` | `fundament/sizes` | `3px` |
| `--button-container-border-radius-circular-lg` | `button` | `9999px` |
| `--button-container-border-radius-circular-md` | `button` | `9999px` |
| `--button-container-border-radius-circular-sm` | `button` | `9999px` |
| `--button-container-border-radius-rectangular-lg` | `button` | `8px` |
| `--button-container-border-radius-rectangular-md` | `button` | `6px` |
| `--button-container-border-radius-rectangular-sm` | `button` | `6px` |
| `--button-container-gap-lg` | `button` | `6px` |
| `--button-container-gap-md` | `button` | `6px` |
| `--button-container-gap-sm` | `button` | `6px` |
| `--button-container-height-lg` | `button` | `48px` |
| `--button-container-height-md` | `button` | `40px` |
| `--button-container-height-sm` | `button` | `32px` |
| `--button-container-max-width` | `button` | `400px` |
| `--button-container-min-width-lg` | `button` | `72px` |
| `--button-container-min-width-md` | `button` | `56px` |
| `--button-container-min-width-sm` | `button` | `52px` |
| `--button-container-padding-inline-icon-lg` | `button` | `16px` |
| `--button-container-padding-inline-icon-md` | `button` | `12px` |
| `--button-container-padding-inline-icon-sm` | `button` | `8px` |
| `--button-container-padding-inline-lg` | `button` | `20px` |
| `--button-container-padding-inline-md` | `button` | `16px` |
| `--button-container-padding-inline-sm` | `button` | `12px` |
| `--button-container-touch-target-lg` | `button` | `48px` |
| `--button-container-touch-target-md` | `button` | `48px` |
| `--button-container-touch-target-sm` | `button` | `40px` |
| `--button-container-transition-duration` | `button` | `150ms` |
| `--button-container-transition-timing-function` | `button` | `ease-out` |
| `--button-destructive-background-active` | `button` | `#912018` |
| `--button-destructive-background-default` | `button` | `#d92d20` |
| `--button-destructive-background-hover` | `button` | `#b32318` |
| `--button-destructive-icon` | `button` | `#ffffff` |
| `--button-destructive-label` | `button` | `#ffffff` |
| `--button-disabled-background` | `button` | `#f1f1f1` |
| `--button-disabled-icon` | `button` | `#b2b2b2` |
| `--button-disabled-label` | `button` | `#b2b2b2` |
| `--button-icon-size-lg` | `button` | `20px` |
| `--button-icon-size-md` | `button` | `20px` |
| `--button-icon-size-sm` | `button` | `16px` |
| `--button-label-font-family` | `button` | `Onest` |
| `--button-label-font-size-lg` | `button` | `16px` |
| `--button-label-font-size-md` | `button` | `14px` |
| `--button-label-font-size-sm` | `button` | `14px` |
| `--button-label-font-weight` | `button` | `500` |
| `--button-label-line-height-lg` | `button` | `24px` |
| `--button-label-line-height-md` | `button` | `20px` |
| `--button-label-line-height-sm` | `button` | `20px` |
| `--button-neutral-background-active` | `button` | `#b2b2b2` |
| `--button-neutral-background-default` | `button` | `#f1f1f1` |
| `--button-neutral-background-hover` | `button` | `#d9d9d9` |
| `--button-neutral-icon` | `button` | `#121212` |
| `--button-neutral-label` | `button` | `#121212` |
| `--button-outlined-destructive-background-active` | `button` | `#912018` |
| `--button-outlined-destructive-background-default` | `button` | `transparent` |
| `--button-outlined-destructive-background-disabled` | `button` | `transparent` |
| `--button-outlined-destructive-background-hover` | `button` | `#b32318` |
| `--button-outlined-destructive-border-active` | `button` | `transparent` |
| `--button-outlined-destructive-border-default` | `button` | `#d92d20` |
| `--button-outlined-destructive-border-disabled` | `button` | `#d9d9d9` |
| `--button-outlined-destructive-border-hover` | `button` | `transparent` |
| `--button-outlined-destructive-icon-active` | `button` | `#ffffff` |
| `--button-outlined-destructive-icon-default` | `button` | `#d92d20` |
| `--button-outlined-destructive-icon-disabled` | `button` | `#b2b2b2` |
| `--button-outlined-destructive-icon-hover` | `button` | `#ffffff` |
| `--button-outlined-destructive-label-active` | `button` | `#ffffff` |
| `--button-outlined-destructive-label-default` | `button` | `#d92d20` |
| `--button-outlined-destructive-label-disabled` | `button` | `#b2b2b2` |
| `--button-outlined-destructive-label-hover` | `button` | `#ffffff` |
| `--button-outlined-primary-background-active` | `button` | `#00357e` |
| `--button-outlined-primary-background-default` | `button` | `transparent` |
| `--button-outlined-primary-background-disabled` | `button` | `transparent` |
| `--button-outlined-primary-background-hover` | `button` | `#0046a8` |
| `--button-outlined-primary-border-active` | `button` | `transparent` |
| `--button-outlined-primary-border-default` | `button` | `#0058d2` |
| `--button-outlined-primary-border-disabled` | `button` | `#d9d9d9` |
| `--button-outlined-primary-border-hover` | `button` | `transparent` |
| `--button-outlined-primary-icon-active` | `button` | `#ffffff` |
| `--button-outlined-primary-icon-default` | `button` | `#0058d2` |
| `--button-outlined-primary-icon-disabled` | `button` | `#b2b2b2` |
| `--button-outlined-primary-icon-hover` | `button` | `#ffffff` |
| `--button-outlined-primary-label-active` | `button` | `#ffffff` |
| `--button-outlined-primary-label-default` | `button` | `#0058d2` |
| `--button-outlined-primary-label-disabled` | `button` | `#b2b2b2` |
| `--button-outlined-primary-label-hover` | `button` | `#ffffff` |
| `--button-outlined-strict-background-active` | `button` | `#444444` |
| `--button-outlined-strict-background-default` | `button` | `transparent` |
| `--button-outlined-strict-background-disabled` | `button` | `transparent` |
| `--button-outlined-strict-background-hover` | `button` | `#383838` |
| `--button-outlined-strict-border-active` | `button` | `transparent` |
| `--button-outlined-strict-border-default` | `button` | `#121212` |
| `--button-outlined-strict-border-disabled` | `button` | `#d9d9d9` |
| `--button-outlined-strict-border-hover` | `button` | `transparent` |
| `--button-outlined-strict-icon-active` | `button` | `#ffffff` |
| `--button-outlined-strict-icon-default` | `button` | `#121212` |
| `--button-outlined-strict-icon-disabled` | `button` | `#b2b2b2` |
| `--button-outlined-strict-icon-hover` | `button` | `#ffffff` |
| `--button-outlined-strict-label-active` | `button` | `#ffffff` |
| `--button-outlined-strict-label-default` | `button` | `#121212` |
| `--button-outlined-strict-label-disabled` | `button` | `#b2b2b2` |
| `--button-outlined-strict-label-hover` | `button` | `#ffffff` |
| `--button-primary-background-active` | `button` | `#00357e` |
| `--button-primary-background-default` | `button` | `#0058d2` |
| `--button-primary-background-hover` | `button` | `#0046a8` |
| `--button-primary-icon` | `button` | `#ffffff` |
| `--button-primary-label` | `button` | `#ffffff` |
| `--button-secondary-background-active` | `button` | `#99bced` |
| `--button-secondary-background-default` | `button` | `#e8f0fb` |
| `--button-secondary-background-hover` | `button` | `#ccdef6` |
| `--button-secondary-icon` | `button` | `#0058d2` |
| `--button-secondary-label` | `button` | `#0058d2` |
| `--button-strict-background-active` | `button` | `#444444` |
| `--button-strict-background-default` | `button` | `#1e1e1e` |
| `--button-strict-background-hover` | `button` | `#383838` |
| `--button-strict-icon` | `button` | `#ffffff` |
| `--button-strict-label` | `button` | `#ffffff` |
| `--button-text-destructive-background-active` | `button` | `#fda19b` |
| `--button-text-destructive-background-default` | `button` | `transparent` |
| `--button-text-destructive-background-disabled` | `button` | `transparent` |
| `--button-text-destructive-background-hover` | `button` | `#fecdc9` |
| `--button-text-destructive-icon-active` | `button` | `#d92d20` |
| `--button-text-destructive-icon-default` | `button` | `#d92d20` |
| `--button-text-destructive-icon-disabled` | `button` | `#b2b2b2` |
| `--button-text-destructive-icon-hover` | `button` | `#d92d20` |
| `--button-text-destructive-label-active` | `button` | `#d92d20` |
| `--button-text-destructive-label-default` | `button` | `#d92d20` |
| `--button-text-destructive-label-disabled` | `button` | `#b2b2b2` |
| `--button-text-destructive-label-hover` | `button` | `#d92d20` |
| `--button-text-primary-background-active` | `button` | `#99bced` |
| `--button-text-primary-background-default` | `button` | `transparent` |
| `--button-text-primary-background-disabled` | `button` | `transparent` |
| `--button-text-primary-background-hover` | `button` | `#ccdef6` |
| `--button-text-primary-icon-active` | `button` | `#0058d2` |
| `--button-text-primary-icon-default` | `button` | `#0058d2` |
| `--button-text-primary-icon-disabled` | `button` | `#b2b2b2` |
| `--button-text-primary-icon-hover` | `button` | `#0058d2` |
| `--button-text-primary-label-active` | `button` | `#0058d2` |
| `--button-text-primary-label-default` | `button` | `#0058d2` |
| `--button-text-primary-label-disabled` | `button` | `#b2b2b2` |
| `--button-text-primary-label-hover` | `button` | `#0058d2` |
| `--button-text-strict-background-active` | `button` | `#b2b2b2` |
| `--button-text-strict-background-default` | `button` | `transparent` |
| `--button-text-strict-background-disabled` | `button` | `transparent` |
| `--button-text-strict-background-hover` | `button` | `#d9d9d9` |
| `--button-text-strict-icon-active` | `button` | `#121212` |
| `--button-text-strict-icon-default` | `button` | `#121212` |
| `--button-text-strict-icon-disabled` | `button` | `#b2b2b2` |
| `--button-text-strict-icon-hover` | `button` | `#121212` |
| `--button-text-strict-label-active` | `button` | `#121212` |
| `--button-text-strict-label-default` | `button` | `#121212` |
| `--button-text-strict-label-disabled` | `button` | `#b2b2b2` |
| `--button-text-strict-label-hover` | `button` | `#121212` |
| `--checkbox-container-border-radius-md` | `checkbox` | `6px` |
| `--checkbox-container-border-radius-sm` | `checkbox` | `6px` |
| `--checkbox-container-border-width` | `checkbox` | `2px` |
| `--checkbox-container-focus-ring-offset` | `checkbox` | `1px` |
| `--checkbox-container-focus-ring-width` | `checkbox` | `3px` |
| `--checkbox-container-gap-md` | `checkbox` | `12px` |
| `--checkbox-container-gap-sm` | `checkbox` | `6px` |
| `--checkbox-container-size-md` | `checkbox` | `24px` |
| `--checkbox-container-size-sm` | `checkbox` | `20px` |
| `--checkbox-container-touch-target-md` | `checkbox` | `36px` |
| `--checkbox-container-touch-target-sm` | `checkbox` | `36px` |
| `--checkbox-container-transition-duration` | `checkbox` | `150ms` |
| `--checkbox-container-transition-timing-function` | `checkbox` | `ease-out` |
| `--checkbox-default-background-checked` | `checkbox` | `#0058d2` |
| `--checkbox-default-background-unchecked` | `checkbox` | `#ffffff` |
| `--checkbox-default-border-checked` | `checkbox` | `#0058d2` |
| `--checkbox-default-border-unchecked` | `checkbox` | `#b2b2b2` |
| `--checkbox-default-focus-ring` | `checkbox` | `#3379db` |
| `--checkbox-destructive-background-checked` | `checkbox` | `#d92d20` |
| `--checkbox-destructive-background-unchecked` | `checkbox` | `#ffffff` |
| `--checkbox-destructive-border-checked` | `checkbox` | `#d92d20` |
| `--checkbox-destructive-border-unchecked` | `checkbox` | `#d92d20` |
| `--checkbox-destructive-focus-ring` | `checkbox` | `#3379db` |
| `--checkbox-destructive-label` | `checkbox` | `#d92d20` |
| `--checkbox-destructive-supporting` | `checkbox` | `#d92d20` |
| `--checkbox-disabled-background-checked` | `checkbox` | `#f1f1f1` |
| `--checkbox-disabled-background-unchecked` | `checkbox` | `#f1f1f1` |
| `--checkbox-disabled-border-checked` | `checkbox` | `#d9d9d9` |
| `--checkbox-disabled-border-unchecked` | `checkbox` | `#d9d9d9` |
| `--checkbox-icon-color-default` | `checkbox` | `#ffffff` |
| `--checkbox-icon-color-disabled` | `checkbox` | `#b2b2b2` |
| `--checkbox-icon-size-md` | `checkbox` | `16px` |
| `--checkbox-icon-size-sm` | `checkbox` | `12px` |
| `--checkbox-label-color-default` | `checkbox` | `#121212` |
| `--checkbox-label-color-disabled` | `checkbox` | `#b2b2b2` |
| `--checkbox-label-font-family` | `checkbox` | `Onest` |
| `--checkbox-label-font-size-md` | `checkbox` | `16px` |
| `--checkbox-label-font-size-sm` | `checkbox` | `14px` |
| `--checkbox-label-font-weight` | `checkbox` | `500` |
| `--checkbox-label-line-height-md` | `checkbox` | `24px` |
| `--checkbox-label-line-height-sm` | `checkbox` | `20px` |
| `--checkbox-supporting-color-default` | `checkbox` | `#757575` |
| `--checkbox-supporting-color-disabled` | `checkbox` | `#b2b2b2` |
| `--checkbox-supporting-font-family` | `checkbox` | `Onest` |
| `--checkbox-supporting-font-size-md` | `checkbox` | `14px` |
| `--checkbox-supporting-font-size-sm` | `checkbox` | `12px` |
| `--checkbox-supporting-font-weight` | `checkbox` | `400` |
| `--checkbox-supporting-line-height-md` | `checkbox` | `20px` |
| `--checkbox-supporting-line-height-sm` | `checkbox` | `16px` |
| `--checkbox-text-gap` | `checkbox` | `2px` |
| `--chip-avatar-size-md` | `chip` | `28px` |
| `--chip-avatar-size-sm` | `chip` | `22px` |
| `--chip-container-border-radius` | `chip` | `9999px` |
| `--chip-container-gap-icon-md` | `chip` | `4px` |
| `--chip-container-gap-icon-sm` | `chip` | `4px` |
| `--chip-container-gap-md` | `chip` | `8px` |
| `--chip-container-gap-sm` | `chip` | `6px` |
| `--chip-container-height-md` | `chip` | `36px` |
| `--chip-container-height-sm` | `chip` | `28px` |
| `--chip-container-min-width-md` | `chip` | `60px` |
| `--chip-container-min-width-sm` | `chip` | `52px` |
| `--chip-container-padding-inline-icon-md` | `chip` | `12px` |
| `--chip-container-padding-inline-icon-sm` | `chip` | `8px` |
| `--chip-container-padding-inline-md` | `chip` | `16px` |
| `--chip-container-padding-inline-remove-md` | `chip` | `12px` |
| `--chip-container-padding-inline-remove-sm` | `chip` | `8px` |
| `--chip-container-padding-inline-sm` | `chip` | `12px` |
| `--chip-container-touch-target-md` | `chip` | `44px` |
| `--chip-container-touch-target-sm` | `chip` | `40px` |
| `--chip-container-transition-duration` | `chip` | `150ms` |
| `--chip-container-transition-timing-function` | `chip` | `ease-out` |
| `--chip-count-font-size-md` | `chip` | `12px` |
| `--chip-count-font-size-sm` | `chip` | `12px` |
| `--chip-count-padding-inline-md` | `chip` | `4px` |
| `--chip-count-padding-inline-sm` | `chip` | `4px` |
| `--chip-count-size-md` | `chip` | `18px` |
| `--chip-count-size-sm` | `chip` | `16px` |
| `--chip-disabled-background` | `chip` | `#f1f1f1` |
| `--chip-disabled-icon` | `chip` | `#b2b2b2` |
| `--chip-disabled-label` | `chip` | `#b2b2b2` |
| `--chip-filter-background-default` | `chip` | `#f1f1f1` |
| `--chip-filter-background-hover` | `chip` | `#d9d9d9` |
| `--chip-filter-background-selected` | `chip` | `#1e1e1e` |
| `--chip-filter-background-selected-hover` | `chip` | `#383838` |
| `--chip-filter-icon-default` | `chip` | `#121212` |
| `--chip-filter-icon-selected` | `chip` | `#ffffff` |
| `--chip-filter-label-default` | `chip` | `#121212` |
| `--chip-filter-label-selected` | `chip` | `#ffffff` |
| `--chip-icon-size-md` | `chip` | `20px` |
| `--chip-icon-size-sm` | `chip` | `16px` |
| `--chip-input-background-default` | `chip` | `#f1f1f1` |
| `--chip-input-background-hover` | `chip` | `#d9d9d9` |
| `--chip-input-icon-default` | `chip` | `#121212` |
| `--chip-input-label-default` | `chip` | `#121212` |
| `--chip-input-min-width-md` | `chip` | `64px` |
| `--chip-input-min-width-sm` | `chip` | `56px` |
| `--chip-input-remove-background-default` | `chip` | `transparent` |
| `--chip-input-remove-background-hover` | `chip` | `#d9d9d9` |
| `--chip-input-remove-border-radius` | `chip` | `9999px` |
| `--chip-input-remove-color-default` | `chip` | `#121212` |
| `--chip-input-remove-color-disabled` | `chip` | `#b2b2b2` |
| `--chip-input-remove-color-hover` | `chip` | `#121212` |
| `--chip-input-remove-icon-size-md` | `chip` | `20px` |
| `--chip-input-remove-icon-size-sm` | `chip` | `16px` |
| `--chip-input-remove-size-md` | `chip` | `20px` |
| `--chip-input-remove-size-sm` | `chip` | `16px` |
| `--chip-label-font-family` | `chip` | `Onest` |
| `--chip-label-font-size-md` | `chip` | `14px` |
| `--chip-label-font-size-sm` | `chip` | `12px` |
| `--chip-label-font-weight` | `chip` | `500` |
| `--chip-label-line-height-md` | `chip` | `20px` |
| `--chip-label-line-height-sm` | `chip` | `16px` |
| `--color-background-alpha-large-surface` | `fundament/color` | `#44444408` |
| `--color-background-alpha-overlay-dark` | `fundament/color` | `#12121266` |
| `--color-background-alpha-overlay-light` | `fundament/color` | `#ffffff66` |
| `--color-background-base-default` | `fundament/color` | `#ffffff` |
| `--color-background-base-default-active` | `fundament/color` | `#d9d9d9` |
| `--color-background-base-default-hover` | `fundament/color` | `#f5f5f5` |
| `--color-background-base-inverse-default` | `fundament/color` | `#1e1e1e` |
| `--color-background-base-inverse-default-active` | `fundament/color` | `#444444` |
| `--color-background-base-inverse-default-hover` | `fundament/color` | `#383838` |
| `--color-background-base-secondary` | `fundament/color` | `#f5f5f5` |
| `--color-background-base-secondary-active` | `fundament/color` | `#d9d9d9` |
| `--color-background-base-secondary-hover` | `fundament/color` | `#f1f1f1` |
| `--color-background-base-tertiary` | `fundament/color` | `#f1f1f1` |
| `--color-background-base-tertiary-active` | `fundament/color` | `#b2b2b2` |
| `--color-background-base-tertiary-hover` | `fundament/color` | `#d9d9d9` |
| `--color-background-brand-default` | `fundament/color` | `#0058d2` |
| `--color-background-brand-default-active` | `fundament/color` | `#00357e` |
| `--color-background-brand-default-hover` | `fundament/color` | `#0046a8` |
| `--color-background-brand-focus-ring` | `fundament/color` | `#3379db` |
| `--color-background-brand-secondary` | `fundament/color` | `#e8f0fb` |
| `--color-background-brand-secondary-active` | `fundament/color` | `#99bced` |
| `--color-background-brand-secondary-hover` | `fundament/color` | `#ccdef6` |
| `--color-background-brand-tertiary` | `fundament/color` | `#00295a` |
| `--color-background-danger-default` | `fundament/color` | `#d92d20` |
| `--color-background-danger-default-active` | `fundament/color` | `#912018` |
| `--color-background-danger-default-hover` | `fundament/color` | `#b32318` |
| `--color-background-danger-secondary` | `fundament/color` | `#fee4e2` |
| `--color-background-danger-secondary-active` | `fundament/color` | `#fda19b` |
| `--color-background-danger-secondary-hover` | `fundament/color` | `#fecdc9` |
| `--color-background-disabled-default` | `fundament/color` | `#f1f1f1` |
| `--color-background-disabled-secondary` | `fundament/color` | `#d9d9d9` |
| `--color-background-positive-default` | `fundament/color` | `#039855` |
| `--color-background-positive-default-active` | `fundament/color` | `#05603a` |
| `--color-background-positive-default-hover` | `fundament/color` | `#027948` |
| `--color-background-positive-secondary` | `fundament/color` | `#e6f5ee` |
| `--color-background-positive-secondary-active` | `fundament/color` | `#cdeadd` |
| `--color-background-warning-accent` | `fundament/color` | `#fec84b` |
| `--color-background-warning-default` | `fundament/color` | `#fdb022` |
| `--color-background-warning-default-active` | `fundament/color` | `#dc6803` |
| `--color-background-warning-default-hover` | `fundament/color` | `#f79009` |
| `--color-background-warning-secondary` | `fundament/color` | `#feefc6` |
| `--color-background-warning-secondary-active` | `fundament/color` | `#fedf89` |
| `--color-border-base-default` | `fundament/color` | `#d9d9d9` |
| `--color-border-base-secondary` | `fundament/color` | `#b2b2b2` |
| `--color-border-base-strong` | `fundament/color` | `#121212` |
| `--color-border-base-subtle` | `fundament/color` | `#ffffff` |
| `--color-border-base-tertiary` | `fundament/color` | `#444444` |
| `--color-border-brand-default` | `fundament/color` | `#0058d2` |
| `--color-border-danger-default` | `fundament/color` | `#d92d20` |
| `--color-border-disabled-default` | `fundament/color` | `#d9d9d9` |
| `--color-border-positive-default` | `fundament/color` | `#027948` |
| `--color-border-warning-default` | `fundament/color` | `#dc6803` |
| `--color-icon-base-default` | `fundament/color` | `#121212` |
| `--color-icon-base-default-on-color` | `fundament/color` | `#121212` |
| `--color-icon-base-inverse-default` | `fundament/color` | `#ffffff` |
| `--color-icon-base-inverse-on-color` | `fundament/color` | `#ffffff` |
| `--color-icon-base-secondary` | `fundament/color` | `#444444` |
| `--color-icon-base-secondary-on-color` | `fundament/color` | `#444444` |
| `--color-icon-base-tertiary` | `fundament/color` | `#757575` |
| `--color-icon-brand-default` | `fundament/color` | `#0058d2` |
| `--color-icon-brand-on-secondary` | `fundament/color` | `#0058d2` |
| `--color-icon-brand-visited` | `fundament/color` | `#aa18ce` |
| `--color-icon-danger-default` | `fundament/color` | `#d92d20` |
| `--color-icon-danger-on-secondary` | `fundament/color` | `#b32318` |
| `--color-icon-disabled-default` | `fundament/color` | `#b2b2b2` |
| `--color-icon-disabled-on-disabled` | `fundament/color` | `#b2b2b2` |
| `--color-icon-positive-default` | `fundament/color` | `#039855` |
| `--color-icon-positive-on-secondary` | `fundament/color` | `#027948` |
| `--color-icon-warning-default` | `fundament/color` | `#dc6803` |
| `--color-icon-warning-on-secondary` | `fundament/color` | `#b54708` |
| `--color-text-base-default` | `fundament/color` | `#121212` |
| `--color-text-base-default-on-color` | `fundament/color` | `#121212` |
| `--color-text-base-inverse-default` | `fundament/color` | `#ffffff` |
| `--color-text-base-inverse-on-color` | `fundament/color` | `#ffffff` |
| `--color-text-base-secondary` | `fundament/color` | `#383838` |
| `--color-text-base-secondary-on-color` | `fundament/color` | `#444444` |
| `--color-text-base-tertiary` | `fundament/color` | `#757575` |
| `--color-text-brand-default` | `fundament/color` | `#0058d2` |
| `--color-text-brand-default-hover` | `fundament/color` | `#0058d2` |
| `--color-text-brand-on-secondary` | `fundament/color` | `#0058d2` |
| `--color-text-brand-visited` | `fundament/color` | `#aa18ce` |
| `--color-text-danger-default` | `fundament/color` | `#d92d20` |
| `--color-text-danger-on-secondary` | `fundament/color` | `#b32318` |
| `--color-text-disabled-default` | `fundament/color` | `#b2b2b2` |
| `--color-text-disabled-on-disabled` | `fundament/color` | `#b2b2b2` |
| `--color-text-positive-default` | `fundament/color` | `#039855` |
| `--color-text-positive-on-secondary` | `fundament/color` | `#027948` |
| `--color-text-warning-default` | `fundament/color` | `#b54708` |
| `--color-text-warning-on-secondary` | `fundament/color` | `#b54708` |
| `--date-input-assistive-color-default` | `date-input` | `#383838` |
| `--date-input-assistive-color-error` | `date-input` | `#d92d20` |
| `--date-input-assistive-font-family` | `date-input` | `Onest` |
| `--date-input-assistive-font-size` | `date-input` | `14px` |
| `--date-input-assistive-font-weight` | `date-input` | `400` |
| `--date-input-assistive-gap-default` | `date-input` | `6px` |
| `--date-input-assistive-gap-error` | `date-input` | `4px` |
| `--date-input-assistive-icon-size` | `date-input` | `20px` |
| `--date-input-assistive-line-height` | `date-input` | `20px` |
| `--date-input-container-border-radius` | `date-input` | `8px` |
| `--date-input-container-border-width-default` | `date-input` | `1px` |
| `--date-input-container-border-width-emphasized` | `date-input` | `2px` |
| `--date-input-container-focus-ring-width` | `date-input` | `2px` |
| `--date-input-container-gap-lg` | `date-input` | `8px` |
| `--date-input-container-gap-md` | `date-input` | `8px` |
| `--date-input-container-height-lg` | `date-input` | `48px` |
| `--date-input-container-height-md` | `date-input` | `40px` |
| `--date-input-container-padding-inline-lg` | `date-input` | `16px` |
| `--date-input-container-padding-inline-md` | `date-input` | `12px` |
| `--date-input-container-transition-duration` | `date-input` | `150ms` |
| `--date-input-container-transition-timing-function` | `date-input` | `ease-out` |
| `--date-input-control-caret-color` | `date-input` | `#0058d2` |
| `--date-input-control-font-family` | `date-input` | `Onest` |
| `--date-input-control-font-size-lg` | `date-input` | `16px` |
| `--date-input-control-font-size-md` | `date-input` | `14px` |
| `--date-input-control-font-weight` | `date-input` | `400` |
| `--date-input-control-line-height-lg` | `date-input` | `24px` |
| `--date-input-control-line-height-md` | `date-input` | `20px` |
| `--date-input-default-background-default` | `date-input` | `#ffffff` |
| `--date-input-default-background-disabled` | `date-input` | `#f1f1f1` |
| `--date-input-default-border-default` | `date-input` | `#d9d9d9` |
| `--date-input-default-border-disabled` | `date-input` | `#d9d9d9` |
| `--date-input-default-border-focus` | `date-input` | `#0058d2` |
| `--date-input-default-border-hover` | `date-input` | `#444444` |
| `--date-input-default-focus-ring` | `date-input` | `#ccdef6` |
| `--date-input-default-text-default` | `date-input` | `#121212` |
| `--date-input-default-text-disabled` | `date-input` | `#b2b2b2` |
| `--date-input-default-text-placeholder` | `date-input` | `#757575` |
| `--date-input-destructive-border-default` | `date-input` | `#d92d20` |
| `--date-input-destructive-border-focus` | `date-input` | `#d92d20` |
| `--date-input-destructive-border-hover` | `date-input` | `#d92d20` |
| `--date-input-destructive-focus-ring` | `date-input` | `#fecdc9` |
| `--date-input-field-gap` | `date-input` | `8px` |
| `--date-input-icon-color-default` | `date-input` | `#444444` |
| `--date-input-icon-color-disabled` | `date-input` | `#b2b2b2` |
| `--date-input-icon-size-lg` | `date-input` | `24px` |
| `--date-input-icon-size-md` | `date-input` | `20px` |
| `--date-input-label-color-default` | `date-input` | `#383838` |
| `--date-input-label-color-disabled` | `date-input` | `#b2b2b2` |
| `--date-input-label-font-family` | `date-input` | `Onest` |
| `--date-input-label-font-size` | `date-input` | `14px` |
| `--date-input-label-font-weight` | `date-input` | `400` |
| `--date-input-label-gap` | `date-input` | `4px` |
| `--date-input-label-line-height` | `date-input` | `20px` |
| `--date-input-label-required-mark-color` | `date-input` | `#d92d20` |
| `--date-input-label-required-mark-size` | `date-input` | `12px` |
| `--date-picker-container-background` | `date-picker` | `#ffffff` |
| `--date-picker-container-border-color` | `date-picker` | `#d9d9d9` |
| `--date-picker-container-border-radius` | `date-picker` | `12px` |
| `--date-picker-container-border-width` | `date-picker` | `1px` |
| `--date-picker-container-gap` | `date-picker` | `8px` |
| `--date-picker-container-padding-desktop` | `date-picker` | `12px` |
| `--date-picker-container-padding-docked` | `date-picker` | `8px` |
| `--date-picker-container-padding-mobile` | `date-picker` | `16px` |
| `--date-picker-container-shadow` | `date-picker` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` |
| `--date-picker-container-transition-duration` | `date-picker` | `150ms` |
| `--date-picker-container-transition-timing-function` | `date-picker` | `ease-out` |
| `--date-picker-container-width-desktop` | `date-picker` | `320px` |
| `--date-picker-container-width-docked` | `date-picker` | `320px` |
| `--date-picker-container-width-mobile` | `date-picker` | `100%` |
| `--date-picker-day-cell-border-radius` | `date-picker` | `6px` |
| `--date-picker-day-cell-border-width` | `date-picker` | `1.5px` |
| `--date-picker-day-cell-default-color` | `date-picker` | `#121212` |
| `--date-picker-day-cell-disabled-color` | `date-picker` | `#b2b2b2` |
| `--date-picker-day-cell-focus-ring` | `date-picker` | `#3379db` |
| `--date-picker-day-cell-focus-ring-offset` | `date-picker` | `1px` |
| `--date-picker-day-cell-focus-ring-width` | `date-picker` | `3px` |
| `--date-picker-day-cell-font-family` | `date-picker` | `Onest` |
| `--date-picker-day-cell-font-size` | `date-picker` | `14px` |
| `--date-picker-day-cell-font-weight` | `date-picker` | `500` |
| `--date-picker-day-cell-height` | `date-picker` | `40px` |
| `--date-picker-day-cell-hover-background` | `date-picker` | `#f5f5f5` |
| `--date-picker-day-cell-hover-color` | `date-picker` | `#121212` |
| `--date-picker-day-cell-in-range-background` | `date-picker` | `#e8f0fb` |
| `--date-picker-day-cell-in-range-color` | `date-picker` | `#0058d2` |
| `--date-picker-day-cell-line-height` | `date-picker` | `20px` |
| `--date-picker-day-cell-outside-month-color` | `date-picker` | `#757575` |
| `--date-picker-day-cell-selected-background` | `date-picker` | `#0058d2` |
| `--date-picker-day-cell-selected-color` | `date-picker` | `#ffffff` |
| `--date-picker-day-cell-selected-hover-background` | `date-picker` | `#0046a8` |
| `--date-picker-day-cell-selected-hover-color` | `date-picker` | `#ffffff` |
| `--date-picker-day-cell-today-border-color` | `date-picker` | `#0058d2` |
| `--date-picker-day-cell-today-color` | `date-picker` | `#0058d2` |
| `--date-picker-day-cell-today-hover-background` | `date-picker` | `#e8f0fb` |
| `--date-picker-day-cell-today-hover-color` | `date-picker` | `#0058d2` |
| `--date-picker-day-label-color` | `date-picker` | `#757575` |
| `--date-picker-day-label-font-family` | `date-picker` | `Onest` |
| `--date-picker-day-label-font-size` | `date-picker` | `14px` |
| `--date-picker-day-label-font-weight` | `date-picker` | `500` |
| `--date-picker-day-label-height` | `date-picker` | `32px` |
| `--date-picker-day-label-line-height` | `date-picker` | `20px` |
| `--date-picker-drag-handle-background` | `date-picker` | `#f1f1f1` |
| `--date-picker-drag-handle-border-radius` | `date-picker` | `9999px` |
| `--date-picker-drag-handle-height` | `date-picker` | `4px` |
| `--date-picker-drag-handle-width` | `date-picker` | `48px` |
| `--date-picker-header-gap` | `date-picker` | `8px` |
| `--date-picker-header-height` | `date-picker` | `40px` |
| `--date-picker-header-nav-button-background-active` | `date-picker` | `#b2b2b2` |
| `--date-picker-header-nav-button-background-default` | `date-picker` | `#f1f1f1` |
| `--date-picker-header-nav-button-background-hover` | `date-picker` | `#d9d9d9` |
| `--date-picker-header-nav-button-border-radius` | `date-picker` | `6px` |
| `--date-picker-header-nav-button-icon-color-default` | `date-picker` | `#121212` |
| `--date-picker-header-nav-button-icon-color-disabled` | `date-picker` | `#b2b2b2` |
| `--date-picker-header-nav-button-icon-size` | `date-picker` | `20px` |
| `--date-picker-header-nav-button-size` | `date-picker` | `40px` |
| `--date-picker-header-title-color` | `date-picker` | `#121212` |
| `--date-picker-header-title-font-family` | `date-picker` | `Onest` |
| `--date-picker-header-title-font-size` | `date-picker` | `16px` |
| `--date-picker-header-title-font-weight` | `date-picker` | `500` |
| `--date-picker-header-title-line-height` | `date-picker` | `24px` |
| `--date-picker-month-grid-cell-border-radius` | `date-picker` | `8px` |
| `--date-picker-month-grid-cell-height` | `date-picker` | `40px` |
| `--date-picker-month-grid-row-gap` | `date-picker` | `8px` |
| `--date-picker-year-grid-cell-border-radius` | `date-picker` | `8px` |
| `--date-picker-year-grid-cell-height` | `date-picker` | `40px` |
| `--date-picker-year-grid-row-gap` | `date-picker` | `8px` |
| `--drop-shadow-100` | `fundament/effects` | `0px 2px 8px 0px rgba(19, 22, 29, 0.06), 0px 4px 8px 1px rgba(19, 22, 29, 0.04)` |
| `--drop-shadow-200` | `fundament/effects` | `0px 4px 8px 2px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.04)` |
| `--drop-shadow-300` | `fundament/effects` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` |
| `--drop-shadow-400` | `fundament/effects` | `0px 12px 16px 6px rgba(40, 46, 55, 0.06), 0px 4px 6px 0px rgba(40, 46, 55, 0.06)` |
| `--drop-shadow-500` | `fundament/effects` | `0px 16px 20px 6px rgba(40, 46, 55, 0.06), 0px 4px 8px 0px rgba(40, 46, 55, 0.06)` |
| `--focus-ring-color-inner` | `fundament/focusRing` | `#ffffff` |
| `--focus-ring-color-outer` | `fundament/focusRing` | `#3379db` |
| `--focus-ring-width-inner` | `fundament/focusRing` | `2px` |
| `--focus-ring-width-outer` | `fundament/focusRing` | `3px` |
| `--font-family-primary` | `fundament/font` | `Onest` |
| `--font-size-10` | `fundament/font` | `10px` |
| `--font-size-12` | `fundament/font` | `12px` |
| `--font-size-14` | `fundament/font` | `14px` |
| `--font-size-16` | `fundament/font` | `16px` |
| `--font-size-18` | `fundament/font` | `18px` |
| `--font-size-20` | `fundament/font` | `20px` |
| `--font-size-22` | `fundament/font` | `22px` |
| `--font-size-24` | `fundament/font` | `24px` |
| `--font-size-28` | `fundament/font` | `28px` |
| `--font-size-32` | `fundament/font` | `32px` |
| `--font-size-40` | `fundament/font` | `40px` |
| `--font-size-48` | `fundament/font` | `48px` |
| `--font-size-56` | `fundament/font` | `56px` |
| `--font-size-64` | `fundament/font` | `64px` |
| `--font-weight-bold` | `fundament/font` | `700` |
| `--font-weight-medium` | `fundament/font` | `500` |
| `--font-weight-regular` | `fundament/font` | `400` |
| `--font-weight-semibold` | `fundament/font` | `600` |
| `--input-chip-assistive-color-default` | `input-chip` | `#383838` |
| `--input-chip-assistive-color-error` | `input-chip` | `#d92d20` |
| `--input-chip-assistive-font-family` | `input-chip` | `Onest` |
| `--input-chip-assistive-font-size` | `input-chip` | `14px` |
| `--input-chip-assistive-font-weight` | `input-chip` | `400` |
| `--input-chip-assistive-gap-default` | `input-chip` | `6px` |
| `--input-chip-assistive-gap-error` | `input-chip` | `4px` |
| `--input-chip-assistive-icon-size` | `input-chip` | `20px` |
| `--input-chip-assistive-line-height` | `input-chip` | `20px` |
| `--input-chip-chip-background-default` | `input-chip` | `#f5f5f5` |
| `--input-chip-chip-background-disabled` | `input-chip` | `#f1f1f1` |
| `--input-chip-chip-background-hover` | `input-chip` | `#f1f1f1` |
| `--input-chip-chip-border-radius` | `input-chip` | `6px` |
| `--input-chip-chip-focus-ring-color` | `input-chip` | `#ccdef6` |
| `--input-chip-chip-font-family` | `input-chip` | `Onest` |
| `--input-chip-chip-font-size` | `input-chip` | `14px` |
| `--input-chip-chip-font-weight` | `input-chip` | `500` |
| `--input-chip-chip-gap-lg` | `input-chip` | `6px` |
| `--input-chip-chip-gap-md` | `input-chip` | `4px` |
| `--input-chip-chip-height-lg` | `input-chip` | `32px` |
| `--input-chip-chip-height-md` | `input-chip` | `24px` |
| `--input-chip-chip-label-default` | `input-chip` | `#121212` |
| `--input-chip-chip-label-disabled` | `input-chip` | `#383838` |
| `--input-chip-chip-line-height` | `input-chip` | `20px` |
| `--input-chip-chip-max-inline-size` | `input-chip` | `100%` |
| `--input-chip-chip-padding-block-lg` | `input-chip` | `4px` |
| `--input-chip-chip-padding-block-md` | `input-chip` | `2px` |
| `--input-chip-chip-padding-inline-end-lg` | `input-chip` | `6px` |
| `--input-chip-chip-padding-inline-end-md` | `input-chip` | `4px` |
| `--input-chip-chip-padding-inline-start-lg` | `input-chip` | `12px` |
| `--input-chip-chip-padding-inline-start-md` | `input-chip` | `8px` |
| `--input-chip-chip-remove-background-focus` | `input-chip` | `#e8f0fb` |
| `--input-chip-chip-remove-background-hover` | `input-chip` | `#f1f1f1` |
| `--input-chip-chip-remove-border-radius` | `input-chip` | `9999px` |
| `--input-chip-chip-remove-color-default` | `input-chip` | `#444444` |
| `--input-chip-chip-remove-color-disabled` | `input-chip` | `#b2b2b2` |
| `--input-chip-chip-remove-color-hover` | `input-chip` | `#121212` |
| `--input-chip-chip-remove-icon-size-lg` | `input-chip` | `16px` |
| `--input-chip-chip-remove-icon-size-md` | `input-chip` | `12px` |
| `--input-chip-chip-remove-size-lg` | `input-chip` | `20px` |
| `--input-chip-chip-remove-size-md` | `input-chip` | `16px` |
| `--input-chip-container-border-radius` | `input-chip` | `8px` |
| `--input-chip-container-border-width-default` | `input-chip` | `1px` |
| `--input-chip-container-border-width-emphasized` | `input-chip` | `2px` |
| `--input-chip-container-focus-ring-width` | `input-chip` | `2px` |
| `--input-chip-container-gap-lg` | `input-chip` | `8px` |
| `--input-chip-container-gap-md` | `input-chip` | `6px` |
| `--input-chip-container-min-height-lg` | `input-chip` | `48px` |
| `--input-chip-container-min-height-md` | `input-chip` | `40px` |
| `--input-chip-container-padding-block-lg` | `input-chip` | `6px` |
| `--input-chip-container-padding-block-md` | `input-chip` | `4px` |
| `--input-chip-container-padding-inline-lg` | `input-chip` | `12px` |
| `--input-chip-container-padding-inline-md` | `input-chip` | `8px` |
| `--input-chip-container-transition-duration` | `input-chip` | `150ms` |
| `--input-chip-container-transition-timing-function` | `input-chip` | `ease-out` |
| `--input-chip-control-font-family` | `input-chip` | `Onest` |
| `--input-chip-control-font-size-lg` | `input-chip` | `16px` |
| `--input-chip-control-font-size-md` | `input-chip` | `14px` |
| `--input-chip-control-font-weight` | `input-chip` | `400` |
| `--input-chip-control-line-height-lg` | `input-chip` | `24px` |
| `--input-chip-control-line-height-md` | `input-chip` | `20px` |
| `--input-chip-control-min-width` | `input-chip` | `80px` |
| `--input-chip-default-background-default` | `input-chip` | `#ffffff` |
| `--input-chip-default-background-disabled` | `input-chip` | `#f1f1f1` |
| `--input-chip-default-border-default` | `input-chip` | `#d9d9d9` |
| `--input-chip-default-border-disabled` | `input-chip` | `#d9d9d9` |
| `--input-chip-default-border-focus` | `input-chip` | `#0058d2` |
| `--input-chip-default-border-hover` | `input-chip` | `#444444` |
| `--input-chip-default-focus-ring` | `input-chip` | `#ccdef6` |
| `--input-chip-default-text-default` | `input-chip` | `#121212` |
| `--input-chip-default-text-disabled` | `input-chip` | `#b2b2b2` |
| `--input-chip-default-text-placeholder` | `input-chip` | `#757575` |
| `--input-chip-destructive-border-default` | `input-chip` | `#d92d20` |
| `--input-chip-destructive-border-focus` | `input-chip` | `#d92d20` |
| `--input-chip-destructive-border-hover` | `input-chip` | `#d92d20` |
| `--input-chip-destructive-focus-ring` | `input-chip` | `#fecdc9` |
| `--input-chip-field-gap` | `input-chip` | `8px` |
| `--input-chip-label-color-default` | `input-chip` | `#383838` |
| `--input-chip-label-color-disabled` | `input-chip` | `#b2b2b2` |
| `--input-chip-label-font-family` | `input-chip` | `Onest` |
| `--input-chip-label-font-size` | `input-chip` | `14px` |
| `--input-chip-label-font-weight` | `input-chip` | `400` |
| `--input-chip-label-gap` | `input-chip` | `4px` |
| `--input-chip-label-line-height` | `input-chip` | `20px` |
| `--input-chip-label-required-mark-color` | `input-chip` | `#d92d20` |
| `--input-chip-label-required-mark-size` | `input-chip` | `12px` |
| `--line-height-12` | `fundament/font` | `12px` |
| `--line-height-16` | `fundament/font` | `16px` |
| `--line-height-20` | `fundament/font` | `20px` |
| `--line-height-24` | `fundament/font` | `24px` |
| `--line-height-26` | `fundament/font` | `26px` |
| `--line-height-28` | `fundament/font` | `28px` |
| `--line-height-30` | `fundament/font` | `30px` |
| `--line-height-32` | `fundament/font` | `32px` |
| `--line-height-36` | `fundament/font` | `36px` |
| `--line-height-40` | `fundament/font` | `40px` |
| `--line-height-48` | `fundament/font` | `48px` |
| `--line-height-56` | `fundament/font` | `56px` |
| `--line-height-64` | `fundament/font` | `64px` |
| `--menu-focus-ring-color` | `menu` | `#3379db` |
| `--menu-focus-ring-width` | `menu` | `2px` |
| `--menu-heading-color` | `menu` | `#757575` |
| `--menu-heading-font-family` | `menu` | `Onest` |
| `--menu-heading-font-size` | `menu` | `14px` |
| `--menu-heading-font-weight` | `menu` | `500` |
| `--menu-heading-gap` | `menu` | `4px` |
| `--menu-heading-line-height` | `menu` | `20px` |
| `--menu-heading-padding-block` | `menu` | `4px` |
| `--menu-heading-padding-inline` | `menu` | `16px` |
| `--menu-heading-separator-color` | `menu` | `#d9d9d9` |
| `--menu-item-background-active` | `menu` | `#d9d9d9` |
| `--menu-item-background-default` | `menu` | `#ffffff` |
| `--menu-item-background-hover` | `menu` | `#f5f5f5` |
| `--menu-item-background-selected` | `menu` | `#f5f5f5` |
| `--menu-item-border-radius` | `menu` | `8px` |
| `--menu-item-check-color` | `menu` | `#0058d2` |
| `--menu-item-check-size` | `menu` | `20px` |
| `--menu-item-font-family` | `menu` | `Onest` |
| `--menu-item-font-size` | `menu` | `14px` |
| `--menu-item-font-weight` | `menu` | `500` |
| `--menu-item-gap` | `menu` | `12px` |
| `--menu-item-icon-color-default` | `menu` | `#121212` |
| `--menu-item-icon-color-disabled` | `menu` | `#b2b2b2` |
| `--menu-item-icon-size` | `menu` | `20px` |
| `--menu-item-line-height` | `menu` | `20px` |
| `--menu-item-min-block-size` | `menu` | `40px` |
| `--menu-item-padding-block` | `menu` | `12px` |
| `--menu-item-padding-inline-end` | `menu` | `24px` |
| `--menu-item-padding-inline-end-trailing` | `menu` | `16px` |
| `--menu-item-padding-inline-start` | `menu` | `16px` |
| `--menu-item-text-default` | `menu` | `#383838` |
| `--menu-item-text-disabled` | `menu` | `#b2b2b2` |
| `--menu-item-text-selected` | `menu` | `#0058d2` |
| `--menu-panel-background` | `menu` | `#ffffff` |
| `--menu-panel-border-color` | `menu` | `#d9d9d9` |
| `--menu-panel-border-radius` | `menu` | `16px` |
| `--menu-panel-border-width` | `menu` | `0px` |
| `--menu-panel-gap` | `menu` | `4px` |
| `--menu-panel-max-block-size` | `menu` | `320px` |
| `--menu-panel-min-inline-size` | `menu` | `270px` |
| `--menu-panel-padding` | `menu` | `8px` |
| `--menu-panel-shadow` | `menu` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` |
| `--pagination-container-gap-md` | `pagination` | `24px` |
| `--pagination-container-gap-sm` | `pagination` | `4px` |
| `--pagination-container-inner-gap-md` | `pagination` | `16px` |
| `--pagination-container-inner-gap-sm` | `pagination` | `8px` |
| `--pagination-container-transition-duration` | `pagination` | `150ms` |
| `--pagination-container-transition-timing-function` | `pagination` | `ease-out` |
| `--pagination-ellipsis-label` | `pagination` | `#757575` |
| `--pagination-ellipsis-size-md` | `pagination` | `40px` |
| `--pagination-ellipsis-size-sm` | `pagination` | `32px` |
| `--pagination-item-border-radius` | `pagination` | `4px` |
| `--pagination-item-border-width-default` | `pagination` | `0` |
| `--pagination-item-border-width-focus` | `pagination` | `1.5px` |
| `--pagination-item-disabled-background` | `pagination` | `transparent` |
| `--pagination-item-disabled-label` | `pagination` | `#b2b2b2` |
| `--pagination-item-font-family` | `pagination` | `Onest` |
| `--pagination-item-font-size` | `pagination` | `14px` |
| `--pagination-item-font-weight` | `pagination` | `500` |
| `--pagination-item-line-height` | `pagination` | `20px` |
| `--pagination-item-selected-background-active` | `pagination` | `#00357e` |
| `--pagination-item-selected-background-default` | `pagination` | `#0058d2` |
| `--pagination-item-selected-background-hover` | `pagination` | `#0046a8` |
| `--pagination-item-selected-label-active` | `pagination` | `#ffffff` |
| `--pagination-item-selected-label-default` | `pagination` | `#ffffff` |
| `--pagination-item-selected-label-hover` | `pagination` | `#ffffff` |
| `--pagination-item-size-md` | `pagination` | `40px` |
| `--pagination-item-size-sm` | `pagination` | `32px` |
| `--pagination-item-unselected-background-active` | `pagination` | `#b2b2b2` |
| `--pagination-item-unselected-background-default` | `pagination` | `transparent` |
| `--pagination-item-unselected-background-hover` | `pagination` | `#f1f1f1` |
| `--pagination-item-unselected-border-default` | `pagination` | `transparent` |
| `--pagination-item-unselected-border-focus` | `pagination` | `#0058d2` |
| `--pagination-item-unselected-label-active` | `pagination` | `#121212` |
| `--pagination-item-unselected-label-default` | `pagination` | `#121212` |
| `--pagination-item-unselected-label-hover` | `pagination` | `#121212` |
| `--pagination-nav-background-active` | `pagination` | `#b2b2b2` |
| `--pagination-nav-background-default` | `pagination` | `transparent` |
| `--pagination-nav-background-hover` | `pagination` | `#f1f1f1` |
| `--pagination-nav-border-default` | `pagination` | `transparent` |
| `--pagination-nav-border-focus` | `pagination` | `#0058d2` |
| `--pagination-nav-border-radius` | `pagination` | `6px` |
| `--pagination-nav-gap` | `pagination` | `6px` |
| `--pagination-nav-height-md` | `pagination` | `40px` |
| `--pagination-nav-height-sm` | `pagination` | `32px` |
| `--pagination-nav-icon-default` | `pagination` | `#121212` |
| `--pagination-nav-icon-disabled` | `pagination` | `#b2b2b2` |
| `--pagination-nav-icon-size-md` | `pagination` | `20px` |
| `--pagination-nav-icon-size-sm` | `pagination` | `16px` |
| `--pagination-nav-label-default` | `pagination` | `#121212` |
| `--pagination-nav-label-disabled` | `pagination` | `#b2b2b2` |
| `--pagination-nav-min-width-md` | `pagination` | `56px` |
| `--pagination-nav-min-width-sm` | `pagination` | `32px` |
| `--pagination-nav-padding-inline-end-md` | `pagination` | `16px` |
| `--pagination-nav-padding-inline-end-sm` | `pagination` | `0px` |
| `--pagination-nav-padding-inline-start-md` | `pagination` | `12px` |
| `--pagination-nav-padding-inline-start-sm` | `pagination` | `0px` |
| `--pagination-overflow-menu-background` | `pagination` | `#ffffff` |
| `--pagination-overflow-menu-border-color` | `pagination` | `#d9d9d9` |
| `--pagination-overflow-menu-border-radius` | `pagination` | `6px` |
| `--pagination-overflow-menu-border-width` | `pagination` | `1px` |
| `--pagination-overflow-menu-gap` | `pagination` | `2px` |
| `--pagination-overflow-menu-item-background-active` | `pagination` | `#b2b2b2` |
| `--pagination-overflow-menu-item-background-default` | `pagination` | `transparent` |
| `--pagination-overflow-menu-item-background-hover` | `pagination` | `#f1f1f1` |
| `--pagination-overflow-menu-item-border-radius` | `pagination` | `4px` |
| `--pagination-overflow-menu-item-label` | `pagination` | `#121212` |
| `--pagination-overflow-menu-item-padding-inline` | `pagination` | `12px` |
| `--pagination-overflow-menu-item-size-md` | `pagination` | `32px` |
| `--pagination-overflow-menu-item-size-sm` | `pagination` | `32px` |
| `--pagination-overflow-menu-max-height` | `pagination` | `264px` |
| `--pagination-overflow-menu-min-width` | `pagination` | `56px` |
| `--pagination-overflow-menu-offset` | `pagination` | `4px` |
| `--pagination-overflow-menu-padding-block` | `pagination` | `4px` |
| `--pagination-overflow-menu-padding-inline` | `pagination` | `4px` |
| `--pagination-overflow-menu-shadow` | `pagination` | `0px 4px 8px 2px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.04)` |
| `--pagination-overflow-trigger-background-active` | `pagination` | `#b2b2b2` |
| `--pagination-overflow-trigger-background-default` | `pagination` | `transparent` |
| `--pagination-overflow-trigger-background-hover` | `pagination` | `#f1f1f1` |
| `--pagination-overflow-trigger-background-open` | `pagination` | `#f1f1f1` |
| `--pagination-overflow-trigger-border-default` | `pagination` | `transparent` |
| `--pagination-overflow-trigger-border-focus` | `pagination` | `#0058d2` |
| `--pagination-overflow-trigger-label` | `pagination` | `#757575` |
| `--palette-alpha-black-100-alpha` | `fundament/palette` | `#1212120d` |
| `--palette-alpha-black-200-alpha` | `fundament/palette` | `#1212121a` |
| `--palette-alpha-black-300-alpha` | `fundament/palette` | `#12121233` |
| `--palette-alpha-black-400-alpha` | `fundament/palette` | `#12121266` |
| `--palette-alpha-black-500-alpha` | `fundament/palette` | `#12121299` |
| `--palette-alpha-gray-alpha-100` | `fundament/palette` | `#44444408` |
| `--palette-alpha-gray-alpha-200` | `fundament/palette` | `#4444441a` |
| `--palette-alpha-gray-alpha-300` | `fundament/palette` | `#44444433` |
| `--palette-alpha-gray-alpha-400` | `fundament/palette` | `#44444466` |
| `--palette-alpha-gray-alpha-500` | `fundament/palette` | `#44444499` |
| `--palette-alpha-white-100-alpha` | `fundament/palette` | `#ffffff0d` |
| `--palette-alpha-white-200-alpha` | `fundament/palette` | `#ffffff1a` |
| `--palette-alpha-white-300-alpha` | `fundament/palette` | `#ffffff33` |
| `--palette-alpha-white-400-alpha` | `fundament/palette` | `#ffffff66` |
| `--palette-alpha-white-500-alpha` | `fundament/palette` | `#ffffff99` |
| `--palette-apricot-100` | `fundament/palette` | `#feefc6` |
| `--palette-apricot-200` | `fundament/palette` | `#fedf89` |
| `--palette-apricot-300` | `fundament/palette` | `#fec84b` |
| `--palette-apricot-400` | `fundament/palette` | `#fdb022` |
| `--palette-apricot-50` | `fundament/palette` | `#fef5dd` |
| `--palette-apricot-500` | `fundament/palette` | `#f79009` |
| `--palette-apricot-600` | `fundament/palette` | `#dc6803` |
| `--palette-apricot-700` | `fundament/palette` | `#b54708` |
| `--palette-apricot-800` | `fundament/palette` | `#93370d` |
| `--palette-apricot-900` | `fundament/palette` | `#792e0d` |
| `--palette-black-1000` | `fundament/palette` | `#121212` |
| `--palette-blue-sky-100` | `fundament/palette` | `#e8f0fb` |
| `--palette-blue-sky-150` | `fundament/palette` | `#d6e5f8` |
| `--palette-blue-sky-200` | `fundament/palette` | `#ccdef6` |
| `--palette-blue-sky-300` | `fundament/palette` | `#99bced` |
| `--palette-blue-sky-400` | `fundament/palette` | `#669be4` |
| `--palette-blue-sky-500` | `fundament/palette` | `#3379db` |
| `--palette-blue-sky-600` | `fundament/palette` | `#0058d2` |
| `--palette-blue-sky-700` | `fundament/palette` | `#0046a8` |
| `--palette-blue-sky-800` | `fundament/palette` | `#00357e` |
| `--palette-blue-sky-900` | `fundament/palette` | `#00295a` |
| `--palette-forest-green-100` | `fundament/palette` | `#e8f1f1` |
| `--palette-forest-green-150` | `fundament/palette` | `#d6e6e7` |
| `--palette-forest-green-200` | `fundament/palette` | `#cce0e1` |
| `--palette-forest-green-300` | `fundament/palette` | `#99c1c3` |
| `--palette-forest-green-400` | `fundament/palette` | `#66a1a5` |
| `--palette-forest-green-500` | `fundament/palette` | `#338287` |
| `--palette-forest-green-600` | `fundament/palette` | `#006369` |
| `--palette-forest-green-700` | `fundament/palette` | `#004f54` |
| `--palette-forest-green-800` | `fundament/palette` | `#003b3f` |
| `--palette-forest-green-900` | `fundament/palette` | `#00292c` |
| `--palette-gray-100` | `fundament/palette` | `#f5f5f5` |
| `--palette-gray-200` | `fundament/palette` | `#f1f1f1` |
| `--palette-gray-250` | `fundament/palette` | `#d9d9d9` |
| `--palette-gray-300` | `fundament/palette` | `#b2b2b2` |
| `--palette-gray-350` | `fundament/palette` | `#8a8a8a` |
| `--palette-gray-400` | `fundament/palette` | `#757575` |
| `--palette-gray-50` | `fundament/palette` | `#f7f7f7` |
| `--palette-gray-500` | `fundament/palette` | `#616161` |
| `--palette-gray-600` | `fundament/palette` | `#444444` |
| `--palette-gray-700` | `fundament/palette` | `#383838` |
| `--palette-gray-800` | `fundament/palette` | `#2c2c2c` |
| `--palette-gray-900` | `fundament/palette` | `#1e1e1e` |
| `--palette-green-100` | `fundament/palette` | `#e6f5ee` |
| `--palette-green-200` | `fundament/palette` | `#cdeadd` |
| `--palette-green-300` | `fundament/palette` | `#9ad6bb` |
| `--palette-green-400` | `fundament/palette` | `#68c199` |
| `--palette-green-50` | `fundament/palette` | `#ebf7f1` |
| `--palette-green-500` | `fundament/palette` | `#35ad77` |
| `--palette-green-600` | `fundament/palette` | `#039855` |
| `--palette-green-700` | `fundament/palette` | `#027948` |
| `--palette-green-800` | `fundament/palette` | `#05603a` |
| `--palette-green-900` | `fundament/palette` | `#054f31` |
| `--palette-lavender-100` | `fundament/palette` | `#efeafc` |
| `--palette-lavender-200` | `fundament/palette` | `#ddd2fa` |
| `--palette-lavender-300` | `fundament/palette` | `#bba5f5` |
| `--palette-lavender-400` | `fundament/palette` | `#9a79ef` |
| `--palette-lavender-500` | `fundament/palette` | `#784cea` |
| `--palette-lavender-600` | `fundament/palette` | `#561fe5` |
| `--palette-lavender-700` | `fundament/palette` | `#4519b7` |
| `--palette-lavender-800` | `fundament/palette` | `#341389` |
| `--palette-lavender-900` | `fundament/palette` | `#240c66` |
| `--palette-magenta-100` | `fundament/palette` | `#f7eafa` |
| `--palette-magenta-200` | `fundament/palette` | `#eed1f5` |
| `--palette-magenta-300` | `fundament/palette` | `#dda3eb` |
| `--palette-magenta-400` | `fundament/palette` | `#cc74e2` |
| `--palette-magenta-500` | `fundament/palette` | `#bb46d8` |
| `--palette-magenta-600` | `fundament/palette` | `#aa18ce` |
| `--palette-magenta-700` | `fundament/palette` | `#8813a5` |
| `--palette-magenta-800` | `fundament/palette` | `#660e7c` |
| `--palette-magenta-900` | `fundament/palette` | `#4a095a` |
| `--palette-purple-100` | `fundament/palette` | `#efeafc` |
| `--palette-purple-200` | `fundament/palette` | `#ddd2fa` |
| `--palette-purple-300` | `fundament/palette` | `#cbaffd` |
| `--palette-purple-400` | `fundament/palette` | `#b287fb` |
| `--palette-purple-500` | `fundament/palette` | `#985ffa` |
| `--palette-purple-600` | `fundament/palette` | `#7e37f9` |
| `--palette-purple-700` | `fundament/palette` | `#652cc7` |
| `--palette-purple-800` | `fundament/palette` | `#4c2195` |
| `--palette-purple-900` | `fundament/palette` | `#36166a` |
| `--palette-red-100` | `fundament/palette` | `#fee4e2` |
| `--palette-red-200` | `fundament/palette` | `#fecdc9` |
| `--palette-red-300` | `fundament/palette` | `#fda19b` |
| `--palette-red-400` | `fundament/palette` | `#f97066` |
| `--palette-red-50` | `fundament/palette` | `#feefee` |
| `--palette-red-500` | `fundament/palette` | `#f04438` |
| `--palette-red-600` | `fundament/palette` | `#d92d20` |
| `--palette-red-700` | `fundament/palette` | `#b32318` |
| `--palette-red-800` | `fundament/palette` | `#912018` |
| `--palette-red-900` | `fundament/palette` | `#7a271a` |
| `--palette-white-1000` | `fundament/palette` | `#ffffff` |
| `--screen-height-fixed-desktop` | `fundament/screen` | `1024px` |
| `--screen-height-fixed-laptop` | `fundament/screen` | `832px` |
| `--screen-height-fixed-mobile` | `fundament/screen` | `800px` |
| `--screen-height-fixed-tablet` | `fundament/screen` | `1024px` |
| `--screen-width-fixed-desktop` | `fundament/screen` | `1440px` |
| `--screen-width-fixed-laptop` | `fundament/screen` | `1280px` |
| `--screen-width-fixed-mobile` | `fundament/screen` | `360px` |
| `--screen-width-fixed-tablet` | `fundament/screen` | `768px` |
| `--screen-width-max-fluid-desktop` | `fundament/screen` | `1920px` |
| `--screen-width-max-fluid-laptop` | `fundament/screen` | `1440px` |
| `--screen-width-max-fluid-mobile` | `fundament/screen` | `480px` |
| `--screen-width-max-fluid-tablet` | `fundament/screen` | `1024px` |
| `--screen-width-min-fluid-desktop` | `fundament/screen` | `1440px` |
| `--screen-width-min-fluid-laptop` | `fundament/screen` | `1024px` |
| `--screen-width-min-fluid-mobile` | `fundament/screen` | `320px` |
| `--screen-width-min-fluid-tablet` | `fundament/screen` | `480px` |
| `--search-input-circular-assistive-color-default` | `search-input-circular` | `#383838` |
| `--search-input-circular-assistive-color-error` | `search-input-circular` | `#d92d20` |
| `--search-input-circular-assistive-font-family` | `search-input-circular` | `Onest` |
| `--search-input-circular-assistive-font-size` | `search-input-circular` | `14px` |
| `--search-input-circular-assistive-font-weight` | `search-input-circular` | `400` |
| `--search-input-circular-assistive-gap-default` | `search-input-circular` | `6px` |
| `--search-input-circular-assistive-gap-error` | `search-input-circular` | `4px` |
| `--search-input-circular-assistive-icon-size` | `search-input-circular` | `20px` |
| `--search-input-circular-assistive-line-height` | `search-input-circular` | `20px` |
| `--search-input-circular-container-border-radius` | `search-input-circular` | `9999px` |
| `--search-input-circular-container-border-width-default` | `search-input-circular` | `1px` |
| `--search-input-circular-container-border-width-emphasized` | `search-input-circular` | `2px` |
| `--search-input-circular-container-focus-ring-width` | `search-input-circular` | `2px` |
| `--search-input-circular-container-gap-lg` | `search-input-circular` | `8px` |
| `--search-input-circular-container-gap-md` | `search-input-circular` | `8px` |
| `--search-input-circular-container-height-lg` | `search-input-circular` | `48px` |
| `--search-input-circular-container-height-md` | `search-input-circular` | `40px` |
| `--search-input-circular-container-padding-inline-lg` | `search-input-circular` | `16px` |
| `--search-input-circular-container-padding-inline-md` | `search-input-circular` | `12px` |
| `--search-input-circular-container-transition-duration` | `search-input-circular` | `150ms` |
| `--search-input-circular-container-transition-timing-function` | `search-input-circular` | `ease-out` |
| `--search-input-circular-control-font-family` | `search-input-circular` | `Onest` |
| `--search-input-circular-control-font-size-lg` | `search-input-circular` | `16px` |
| `--search-input-circular-control-font-size-md` | `search-input-circular` | `14px` |
| `--search-input-circular-control-font-weight` | `search-input-circular` | `400` |
| `--search-input-circular-control-line-height-lg` | `search-input-circular` | `24px` |
| `--search-input-circular-control-line-height-md` | `search-input-circular` | `20px` |
| `--search-input-circular-default-background-default` | `search-input-circular` | `#ffffff` |
| `--search-input-circular-default-background-disabled` | `search-input-circular` | `#f1f1f1` |
| `--search-input-circular-default-border-default` | `search-input-circular` | `#d9d9d9` |
| `--search-input-circular-default-border-disabled` | `search-input-circular` | `#d9d9d9` |
| `--search-input-circular-default-border-focus` | `search-input-circular` | `#0058d2` |
| `--search-input-circular-default-border-hover` | `search-input-circular` | `#444444` |
| `--search-input-circular-default-focus-ring` | `search-input-circular` | `#ccdef6` |
| `--search-input-circular-default-text-default` | `search-input-circular` | `#121212` |
| `--search-input-circular-default-text-disabled` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-default-text-placeholder` | `search-input-circular` | `#757575` |
| `--search-input-circular-destructive-border-default` | `search-input-circular` | `#d92d20` |
| `--search-input-circular-destructive-border-focus` | `search-input-circular` | `#d92d20` |
| `--search-input-circular-destructive-border-hover` | `search-input-circular` | `#d92d20` |
| `--search-input-circular-destructive-focus-ring` | `search-input-circular` | `#fecdc9` |
| `--search-input-circular-field-gap` | `search-input-circular` | `8px` |
| `--search-input-circular-icon-end-clear-background-active` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-icon-end-clear-background-default` | `search-input-circular` | `#f1f1f1` |
| `--search-input-circular-icon-end-clear-background-hover` | `search-input-circular` | `#d9d9d9` |
| `--search-input-circular-icon-end-clear-border-radius` | `search-input-circular` | `9999px` |
| `--search-input-circular-icon-end-clear-color-active` | `search-input-circular` | `#121212` |
| `--search-input-circular-icon-end-clear-color-default` | `search-input-circular` | `#444444` |
| `--search-input-circular-icon-end-clear-color-disabled` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-icon-end-clear-color-hover` | `search-input-circular` | `#121212` |
| `--search-input-circular-icon-end-clear-padding` | `search-input-circular` | `4px` |
| `--search-input-circular-icon-end-clear-size-lg` | `search-input-circular` | `24px` |
| `--search-input-circular-icon-end-clear-size-md` | `search-input-circular` | `20px` |
| `--search-input-circular-icon-start-color-default` | `search-input-circular` | `#444444` |
| `--search-input-circular-icon-start-color-disabled` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-icon-start-size-lg` | `search-input-circular` | `24px` |
| `--search-input-circular-icon-start-size-md` | `search-input-circular` | `20px` |
| `--search-input-circular-label-color-default` | `search-input-circular` | `#383838` |
| `--search-input-circular-label-color-disabled` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-label-font-family` | `search-input-circular` | `Onest` |
| `--search-input-circular-label-font-size` | `search-input-circular` | `14px` |
| `--search-input-circular-label-font-weight` | `search-input-circular` | `400` |
| `--search-input-circular-label-gap` | `search-input-circular` | `4px` |
| `--search-input-circular-label-line-height` | `search-input-circular` | `20px` |
| `--search-input-circular-loading-spinner-color-default` | `search-input-circular` | `#0058d2` |
| `--search-input-circular-loading-spinner-color-disabled` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-loading-spinner-size-lg` | `search-input-circular` | `24px` |
| `--search-input-circular-loading-spinner-size-md` | `search-input-circular` | `20px` |
| `--search-input-circular-submit-button-background-active` | `search-input-circular` | `#00357e` |
| `--search-input-circular-submit-button-background-default` | `search-input-circular` | `#0058d2` |
| `--search-input-circular-submit-button-background-disabled` | `search-input-circular` | `#f1f1f1` |
| `--search-input-circular-submit-button-background-hover` | `search-input-circular` | `#0046a8` |
| `--search-input-circular-submit-button-border-radius` | `search-input-circular` | `9999px` |
| `--search-input-circular-submit-button-container-padding-inline-end` | `search-input-circular` | `4px` |
| `--search-input-circular-submit-button-focus-ring-offset` | `search-input-circular` | `2px` |
| `--search-input-circular-submit-button-icon-default` | `search-input-circular` | `#ffffff` |
| `--search-input-circular-submit-button-icon-disabled` | `search-input-circular` | `#b2b2b2` |
| `--search-input-circular-submit-button-icon-size-lg` | `search-input-circular` | `20px` |
| `--search-input-circular-submit-button-icon-size-md` | `search-input-circular` | `16px` |
| `--search-input-circular-submit-button-size-lg` | `search-input-circular` | `40px` |
| `--search-input-circular-submit-button-size-md` | `search-input-circular` | `32px` |
| `--search-input-rectangular-assistive-color-default` | `search-input-rectangular` | `#383838` |
| `--search-input-rectangular-assistive-color-error` | `search-input-rectangular` | `#d92d20` |
| `--search-input-rectangular-assistive-font-family` | `search-input-rectangular` | `Onest` |
| `--search-input-rectangular-assistive-font-size` | `search-input-rectangular` | `14px` |
| `--search-input-rectangular-assistive-font-weight` | `search-input-rectangular` | `400` |
| `--search-input-rectangular-assistive-gap-default` | `search-input-rectangular` | `6px` |
| `--search-input-rectangular-assistive-gap-error` | `search-input-rectangular` | `4px` |
| `--search-input-rectangular-assistive-icon-size` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-assistive-line-height` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-container-border-radius` | `search-input-rectangular` | `8px` |
| `--search-input-rectangular-container-border-width-default` | `search-input-rectangular` | `1px` |
| `--search-input-rectangular-container-border-width-emphasized` | `search-input-rectangular` | `2px` |
| `--search-input-rectangular-container-focus-ring-width` | `search-input-rectangular` | `2px` |
| `--search-input-rectangular-container-gap-lg` | `search-input-rectangular` | `8px` |
| `--search-input-rectangular-container-gap-md` | `search-input-rectangular` | `8px` |
| `--search-input-rectangular-container-height-lg` | `search-input-rectangular` | `48px` |
| `--search-input-rectangular-container-height-md` | `search-input-rectangular` | `40px` |
| `--search-input-rectangular-container-padding-inline-lg` | `search-input-rectangular` | `16px` |
| `--search-input-rectangular-container-padding-inline-md` | `search-input-rectangular` | `12px` |
| `--search-input-rectangular-container-transition-duration` | `search-input-rectangular` | `150ms` |
| `--search-input-rectangular-container-transition-timing-function` | `search-input-rectangular` | `ease-out` |
| `--search-input-rectangular-control-font-family` | `search-input-rectangular` | `Onest` |
| `--search-input-rectangular-control-font-size-lg` | `search-input-rectangular` | `16px` |
| `--search-input-rectangular-control-font-size-md` | `search-input-rectangular` | `14px` |
| `--search-input-rectangular-control-font-weight` | `search-input-rectangular` | `400` |
| `--search-input-rectangular-control-line-height-lg` | `search-input-rectangular` | `24px` |
| `--search-input-rectangular-control-line-height-md` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-default-background-default` | `search-input-rectangular` | `#ffffff` |
| `--search-input-rectangular-default-background-disabled` | `search-input-rectangular` | `#f1f1f1` |
| `--search-input-rectangular-default-border-default` | `search-input-rectangular` | `#d9d9d9` |
| `--search-input-rectangular-default-border-disabled` | `search-input-rectangular` | `#d9d9d9` |
| `--search-input-rectangular-default-border-focus` | `search-input-rectangular` | `#0058d2` |
| `--search-input-rectangular-default-border-hover` | `search-input-rectangular` | `#444444` |
| `--search-input-rectangular-default-focus-ring` | `search-input-rectangular` | `#ccdef6` |
| `--search-input-rectangular-default-text-default` | `search-input-rectangular` | `#121212` |
| `--search-input-rectangular-default-text-disabled` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-default-text-placeholder` | `search-input-rectangular` | `#757575` |
| `--search-input-rectangular-destructive-border-default` | `search-input-rectangular` | `#d92d20` |
| `--search-input-rectangular-destructive-border-focus` | `search-input-rectangular` | `#d92d20` |
| `--search-input-rectangular-destructive-border-hover` | `search-input-rectangular` | `#d92d20` |
| `--search-input-rectangular-destructive-focus-ring` | `search-input-rectangular` | `#fecdc9` |
| `--search-input-rectangular-field-gap` | `search-input-rectangular` | `8px` |
| `--search-input-rectangular-icon-end-clear-background-active` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-icon-end-clear-background-default` | `search-input-rectangular` | `#f1f1f1` |
| `--search-input-rectangular-icon-end-clear-background-hover` | `search-input-rectangular` | `#d9d9d9` |
| `--search-input-rectangular-icon-end-clear-border-radius` | `search-input-rectangular` | `9999px` |
| `--search-input-rectangular-icon-end-clear-color-active` | `search-input-rectangular` | `#121212` |
| `--search-input-rectangular-icon-end-clear-color-default` | `search-input-rectangular` | `#444444` |
| `--search-input-rectangular-icon-end-clear-color-disabled` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-icon-end-clear-color-hover` | `search-input-rectangular` | `#121212` |
| `--search-input-rectangular-icon-end-clear-padding` | `search-input-rectangular` | `4px` |
| `--search-input-rectangular-icon-end-clear-size-lg` | `search-input-rectangular` | `24px` |
| `--search-input-rectangular-icon-end-clear-size-md` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-icon-start-color-default` | `search-input-rectangular` | `#444444` |
| `--search-input-rectangular-icon-start-color-disabled` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-icon-start-size-lg` | `search-input-rectangular` | `24px` |
| `--search-input-rectangular-icon-start-size-md` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-label-color-default` | `search-input-rectangular` | `#383838` |
| `--search-input-rectangular-label-color-disabled` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-label-font-family` | `search-input-rectangular` | `Onest` |
| `--search-input-rectangular-label-font-size` | `search-input-rectangular` | `14px` |
| `--search-input-rectangular-label-font-weight` | `search-input-rectangular` | `400` |
| `--search-input-rectangular-label-gap` | `search-input-rectangular` | `4px` |
| `--search-input-rectangular-label-line-height` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-loading-spinner-color-default` | `search-input-rectangular` | `#0058d2` |
| `--search-input-rectangular-loading-spinner-color-disabled` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-loading-spinner-size-lg` | `search-input-rectangular` | `24px` |
| `--search-input-rectangular-loading-spinner-size-md` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-submit-button-background-active` | `search-input-rectangular` | `#00357e` |
| `--search-input-rectangular-submit-button-background-default` | `search-input-rectangular` | `#0058d2` |
| `--search-input-rectangular-submit-button-background-disabled` | `search-input-rectangular` | `#f1f1f1` |
| `--search-input-rectangular-submit-button-background-hover` | `search-input-rectangular` | `#0046a8` |
| `--search-input-rectangular-submit-button-border-radius` | `search-input-rectangular` | `6px` |
| `--search-input-rectangular-submit-button-container-padding-inline-end` | `search-input-rectangular` | `4px` |
| `--search-input-rectangular-submit-button-focus-ring-offset` | `search-input-rectangular` | `2px` |
| `--search-input-rectangular-submit-button-icon-default` | `search-input-rectangular` | `#ffffff` |
| `--search-input-rectangular-submit-button-icon-disabled` | `search-input-rectangular` | `#b2b2b2` |
| `--search-input-rectangular-submit-button-icon-size-lg` | `search-input-rectangular` | `20px` |
| `--search-input-rectangular-submit-button-icon-size-md` | `search-input-rectangular` | `16px` |
| `--search-input-rectangular-submit-button-size-lg` | `search-input-rectangular` | `40px` |
| `--search-input-rectangular-submit-button-size-md` | `search-input-rectangular` | `32px` |
| `--segmented-control-container-background` | `segmented-control` | `#f1f1f1` |
| `--segmented-control-container-background-disabled` | `segmented-control` | `#f1f1f1` |
| `--segmented-control-container-border-radius` | `segmented-control` | `9999px` |
| `--segmented-control-container-gap` | `segmented-control` | `6px` |
| `--segmented-control-container-padding` | `segmented-control` | `6px` |
| `--segmented-control-container-transition-duration` | `segmented-control` | `150ms` |
| `--segmented-control-container-transition-timing-function` | `segmented-control` | `ease-out` |
| `--segmented-control-disabled-background` | `segmented-control` | `#f1f1f1` |
| `--segmented-control-disabled-color` | `segmented-control` | `#b2b2b2` |
| `--segmented-control-focus-ring-inner-color` | `segmented-control` | `#ffffff` |
| `--segmented-control-focus-ring-inner-width` | `segmented-control` | `2px` |
| `--segmented-control-focus-ring-outer-color` | `segmented-control` | `#3379db` |
| `--segmented-control-focus-ring-outer-width` | `segmented-control` | `3px` |
| `--segmented-control-label-font-family` | `segmented-control` | `Onest` |
| `--segmented-control-label-font-size-md` | `segmented-control` | `14px` |
| `--segmented-control-label-font-size-sm` | `segmented-control` | `14px` |
| `--segmented-control-label-font-weight-selected` | `segmented-control` | `500` |
| `--segmented-control-label-font-weight-unselected` | `segmented-control` | `400` |
| `--segmented-control-label-line-height-md` | `segmented-control` | `20px` |
| `--segmented-control-label-line-height-sm` | `segmented-control` | `20px` |
| `--segmented-control-segment-border-radius` | `segmented-control` | `9999px` |
| `--segmented-control-segment-gap` | `segmented-control` | `6px` |
| `--segmented-control-segment-height-md` | `segmented-control` | `40px` |
| `--segmented-control-segment-height-sm` | `segmented-control` | `32px` |
| `--segmented-control-segment-min-width` | `segmented-control` | `68px` |
| `--segmented-control-segment-padding-inline-md` | `segmented-control` | `16px` |
| `--segmented-control-segment-padding-inline-sm` | `segmented-control` | `12px` |
| `--segmented-control-selected-background` | `segmented-control` | `#1e1e1e` |
| `--segmented-control-selected-background-hover` | `segmented-control` | `#383838` |
| `--segmented-control-selected-color` | `segmented-control` | `#ffffff` |
| `--segmented-control-selected-shadow` | `segmented-control` | `0px 0px 0.25px rgba(0, 0, 0, 0.30), 0px 1px 1.5px rgba(0, 0, 0, 0.16)` |
| `--segmented-control-separator-color` | `segmented-control` | `#d9d9d9` |
| `--segmented-control-separator-height` | `segmented-control` | `16px` |
| `--segmented-control-separator-width` | `segmented-control` | `1px` |
| `--segmented-control-unselected-background` | `segmented-control` | `#f1f1f1` |
| `--segmented-control-unselected-background-hover` | `segmented-control` | `#d9d9d9` |
| `--segmented-control-unselected-color` | `segmented-control` | `#383838` |
| `--segmented-control-unselected-color-hover` | `segmented-control` | `#121212` |
| `--select-input-assistive-color-default` | `select-input` | `#383838` |
| `--select-input-assistive-color-error` | `select-input` | `#d92d20` |
| `--select-input-assistive-font-family` | `select-input` | `Onest` |
| `--select-input-assistive-font-size` | `select-input` | `14px` |
| `--select-input-assistive-font-weight` | `select-input` | `400` |
| `--select-input-assistive-gap-default` | `select-input` | `6px` |
| `--select-input-assistive-gap-error` | `select-input` | `4px` |
| `--select-input-assistive-icon-size` | `select-input` | `20px` |
| `--select-input-assistive-line-height` | `select-input` | `20px` |
| `--select-input-chevron-color-default` | `select-input` | `#444444` |
| `--select-input-chevron-color-disabled` | `select-input` | `#b2b2b2` |
| `--select-input-chevron-size-lg` | `select-input` | `24px` |
| `--select-input-chevron-size-md` | `select-input` | `20px` |
| `--select-input-chevron-transition-duration` | `select-input` | `150ms` |
| `--select-input-chevron-transition-timing-function` | `select-input` | `ease-out` |
| `--select-input-container-border-radius` | `select-input` | `8px` |
| `--select-input-container-border-width-default` | `select-input` | `1px` |
| `--select-input-container-border-width-emphasized` | `select-input` | `2px` |
| `--select-input-container-focus-ring-width` | `select-input` | `2px` |
| `--select-input-container-gap-lg` | `select-input` | `8px` |
| `--select-input-container-gap-md` | `select-input` | `8px` |
| `--select-input-container-height-lg` | `select-input` | `48px` |
| `--select-input-container-height-md` | `select-input` | `40px` |
| `--select-input-container-padding-inline-lg` | `select-input` | `16px` |
| `--select-input-container-padding-inline-md` | `select-input` | `12px` |
| `--select-input-container-transition-duration` | `select-input` | `150ms` |
| `--select-input-container-transition-timing-function` | `select-input` | `ease-out` |
| `--select-input-control-font-family` | `select-input` | `Onest` |
| `--select-input-control-font-size-lg` | `select-input` | `16px` |
| `--select-input-control-font-size-md` | `select-input` | `14px` |
| `--select-input-control-font-weight` | `select-input` | `400` |
| `--select-input-control-line-height-lg` | `select-input` | `24px` |
| `--select-input-control-line-height-md` | `select-input` | `20px` |
| `--select-input-default-background-default` | `select-input` | `#ffffff` |
| `--select-input-default-background-disabled` | `select-input` | `#f1f1f1` |
| `--select-input-default-border-default` | `select-input` | `#d9d9d9` |
| `--select-input-default-border-disabled` | `select-input` | `#d9d9d9` |
| `--select-input-default-border-focus` | `select-input` | `#0058d2` |
| `--select-input-default-border-hover` | `select-input` | `#444444` |
| `--select-input-default-focus-ring` | `select-input` | `#ccdef6` |
| `--select-input-default-text-default` | `select-input` | `#121212` |
| `--select-input-default-text-disabled` | `select-input` | `#b2b2b2` |
| `--select-input-default-text-placeholder` | `select-input` | `#757575` |
| `--select-input-destructive-border-default` | `select-input` | `#d92d20` |
| `--select-input-destructive-border-focus` | `select-input` | `#d92d20` |
| `--select-input-destructive-border-hover` | `select-input` | `#d92d20` |
| `--select-input-destructive-focus-ring` | `select-input` | `#fecdc9` |
| `--select-input-field-gap` | `select-input` | `8px` |
| `--select-input-icon-color-default` | `select-input` | `#444444` |
| `--select-input-icon-color-disabled` | `select-input` | `#b2b2b2` |
| `--select-input-icon-size-lg` | `select-input` | `24px` |
| `--select-input-icon-size-md` | `select-input` | `20px` |
| `--select-input-label-color-default` | `select-input` | `#383838` |
| `--select-input-label-color-disabled` | `select-input` | `#383838` |
| `--select-input-label-font-family` | `select-input` | `Onest` |
| `--select-input-label-font-size` | `select-input` | `14px` |
| `--select-input-label-font-weight` | `select-input` | `400` |
| `--select-input-label-gap` | `select-input` | `4px` |
| `--select-input-label-line-height` | `select-input` | `20px` |
| `--select-input-label-required-mark-color` | `select-input` | `#d92d20` |
| `--select-input-label-required-mark-size` | `select-input` | `12px` |
| `--select-input-listbox-background` | `select-input` | `#ffffff` |
| `--select-input-listbox-border-color` | `select-input` | `#d9d9d9` |
| `--select-input-listbox-border-radius` | `select-input` | `16px` |
| `--select-input-listbox-border-width` | `select-input` | `0px` |
| `--select-input-listbox-gap` | `select-input` | `4px` |
| `--select-input-listbox-margin-block-start` | `select-input` | `4px` |
| `--select-input-listbox-max-block-size` | `select-input` | `320px` |
| `--select-input-listbox-padding` | `select-input` | `8px` |
| `--select-input-listbox-shadow` | `select-input` | `0px 8px 12px 4px rgba(40, 46, 55, 0.06), 0px 2px 4px 0px rgba(40, 46, 55, 0.06)` |
| `--select-input-option-background-active` | `select-input` | `#f5f5f5` |
| `--select-input-option-background-default` | `select-input` | `#ffffff` |
| `--select-input-option-background-hover` | `select-input` | `#f5f5f5` |
| `--select-input-option-background-selected` | `select-input` | `#f5f5f5` |
| `--select-input-option-border-radius` | `select-input` | `8px` |
| `--select-input-option-check-color` | `select-input` | `#0058d2` |
| `--select-input-option-font-family` | `select-input` | `Onest` |
| `--select-input-option-font-size-lg` | `select-input` | `14px` |
| `--select-input-option-font-size-md` | `select-input` | `14px` |
| `--select-input-option-font-weight-default` | `select-input` | `500` |
| `--select-input-option-font-weight-selected` | `select-input` | `500` |
| `--select-input-option-gap` | `select-input` | `12px` |
| `--select-input-option-icon-size-lg` | `select-input` | `24px` |
| `--select-input-option-icon-size-md` | `select-input` | `20px` |
| `--select-input-option-line-height-lg` | `select-input` | `20px` |
| `--select-input-option-line-height-md` | `select-input` | `20px` |
| `--select-input-option-min-block-size-lg` | `select-input` | `40px` |
| `--select-input-option-min-block-size-md` | `select-input` | `40px` |
| `--select-input-option-padding-block` | `select-input` | `12px` |
| `--select-input-option-padding-inline` | `select-input` | `16px` |
| `--select-input-option-text-default` | `select-input` | `#383838` |
| `--select-input-option-text-disabled` | `select-input` | `#757575` |
| `--select-input-option-text-selected` | `select-input` | `#0058d2` |
| `--spacing-0` | `fundament/sizes` | `0px` |
| `--spacing-12` | `fundament/sizes` | `12px` |
| `--spacing-120` | `fundament/sizes` | `120px` |
| `--spacing-16` | `fundament/sizes` | `16px` |
| `--spacing-2` | `fundament/sizes` | `2px` |
| `--spacing-20` | `fundament/sizes` | `20px` |
| `--spacing-24` | `fundament/sizes` | `24px` |
| `--spacing-32` | `fundament/sizes` | `32px` |
| `--spacing-4` | `fundament/sizes` | `4px` |
| `--spacing-40` | `fundament/sizes` | `40px` |
| `--spacing-48` | `fundament/sizes` | `48px` |
| `--spacing-56` | `fundament/sizes` | `56px` |
| `--spacing-6` | `fundament/sizes` | `6px` |
| `--spacing-64` | `fundament/sizes` | `64px` |
| `--spacing-8` | `fundament/sizes` | `8px` |
| `--spacing-80` | `fundament/sizes` | `80px` |
| `--spacing-96` | `fundament/sizes` | `96px` |
| `--table-cell-border-bottom` | `table` | `#d9d9d9` |
| `--table-cell-color` | `table` | `#383838` |
| `--table-cell-font-size` | `table` | `14px` |
| `--table-cell-font-weight` | `table` | `400` |
| `--table-cell-gap` | `table` | `6px` |
| `--table-cell-line-height` | `table` | `20px` |
| `--table-cell-min-height` | `table` | `48px` |
| `--table-cell-padding-block` | `table` | `8px` |
| `--table-cell-padding-inline-desktop` | `table` | `24px` |
| `--table-cell-padding-inline-mobile` | `table` | `16px` |
| `--table-container-background` | `table` | `#ffffff` |
| `--table-container-border-radius` | `table` | `0px` |
| `--table-container-font-family` | `table` | `Onest` |
| `--table-empty-color` | `table` | `#757575` |
| `--table-empty-font-size` | `table` | `14px` |
| `--table-empty-padding` | `table` | `48px` |
| `--table-focus-ring-color` | `table` | `#3379db` |
| `--table-focus-ring-offset` | `table` | `1px` |
| `--table-focus-ring-width` | `table` | `3px` |
| `--table-header-default-background` | `table` | `#f1f1f1` |
| `--table-header-default-border-bottom` | `table` | `#d9d9d9` |
| `--table-header-default-color` | `table` | `#121212` |
| `--table-header-font-size` | `table` | `14px` |
| `--table-header-font-weight` | `table` | `500` |
| `--table-header-gap` | `table` | `6px` |
| `--table-header-inverted-background` | `table` | `#1e1e1e` |
| `--table-header-inverted-border-bottom` | `table` | `#1e1e1e` |
| `--table-header-inverted-color` | `table` | `#ffffff` |
| `--table-header-line-height` | `table` | `20px` |
| `--table-header-min-height` | `table` | `48px` |
| `--table-header-padding-block` | `table` | `8px` |
| `--table-header-padding-inline-desktop` | `table` | `24px` |
| `--table-header-padding-inline-mobile` | `table` | `16px` |
| `--table-header-white-background` | `table` | `#ffffff` |
| `--table-header-white-border-bottom` | `table` | `#d9d9d9` |
| `--table-header-white-color` | `table` | `#121212` |
| `--table-mobile-breakpoint` | `table` | `640px` |
| `--table-row-background-default` | `table` | `#ffffff` |
| `--table-row-background-hover` | `table` | `#f5f5f5` |
| `--table-row-background-selected` | `table` | `#e8f0fb` |
| `--table-row-background-selected-hover` | `table` | `#ccdef6` |
| `--table-row-background-zebra` | `table` | `#f5f5f5` |
| `--table-row-transition-duration` | `table` | `120ms` |
| `--table-row-transition-timing-function` | `table` | `ease-out` |
| `--table-selection-column-width` | `table` | `56px` |
| `--table-sort-icon-color-active` | `table` | `#121212` |
| `--table-sort-icon-color-inactive` | `table` | `#757575` |
| `--table-sort-icon-color-inverted` | `table` | `#ffffff` |
| `--table-sort-icon-size` | `table` | `16px` |
| `--tabs-badge-background` | `tabs` | `#f1f1f1` |
| `--tabs-badge-border-radius` | `tabs` | `9999px` |
| `--tabs-badge-color` | `tabs` | `#121212` |
| `--tabs-badge-font-size-md` | `tabs` | `14px` |
| `--tabs-badge-font-size-sm` | `tabs` | `12px` |
| `--tabs-badge-font-weight` | `tabs` | `500` |
| `--tabs-badge-line-height-md` | `tabs` | `20px` |
| `--tabs-badge-line-height-sm` | `tabs` | `16px` |
| `--tabs-badge-min-size-md` | `tabs` | `24px` |
| `--tabs-badge-min-size-sm` | `tabs` | `20px` |
| `--tabs-badge-padding-inline-md` | `tabs` | `8px` |
| `--tabs-badge-padding-inline-sm` | `tabs` | `4px` |
| `--tabs-container-border-color` | `tabs` | `#d9d9d9` |
| `--tabs-container-border-width` | `tabs` | `1px` |
| `--tabs-container-gap` | `tabs` | `0px` |
| `--tabs-container-transition-duration` | `tabs` | `150ms` |
| `--tabs-container-transition-timing-function` | `tabs` | `ease-out` |
| `--tabs-disabled-color` | `tabs` | `#b2b2b2` |
| `--tabs-disabled-icon-color` | `tabs` | `#b2b2b2` |
| `--tabs-focus-ring-inner-color` | `tabs` | `#ffffff` |
| `--tabs-focus-ring-inner-width` | `tabs` | `2px` |
| `--tabs-focus-ring-outer-color` | `tabs` | `#3379db` |
| `--tabs-focus-ring-outer-width` | `tabs` | `3px` |
| `--tabs-icon-size-md` | `tabs` | `20px` |
| `--tabs-icon-size-sm` | `tabs` | `20px` |
| `--tabs-label-font-family` | `tabs` | `Onest` |
| `--tabs-label-font-size-md` | `tabs` | `16px` |
| `--tabs-label-font-size-sm` | `tabs` | `14px` |
| `--tabs-label-font-weight-selected` | `tabs` | `500` |
| `--tabs-label-font-weight-unselected` | `tabs` | `400` |
| `--tabs-label-line-height-md` | `tabs` | `24px` |
| `--tabs-label-line-height-sm` | `tabs` | `20px` |
| `--tabs-overflow-chevron-background` | `tabs` | `#ffffff` |
| `--tabs-overflow-chevron-color` | `tabs` | `#444444` |
| `--tabs-overflow-chevron-color-disabled` | `tabs` | `#b2b2b2` |
| `--tabs-overflow-chevron-color-hover` | `tabs` | `#121212` |
| `--tabs-overflow-chevron-padding-block-md` | `tabs` | `12px` |
| `--tabs-overflow-chevron-padding-block-sm` | `tabs` | `8px` |
| `--tabs-overflow-chevron-padding-inline-md` | `tabs` | `4px` |
| `--tabs-overflow-chevron-padding-inline-sm` | `tabs` | `4px` |
| `--tabs-overflow-chevron-size-md` | `tabs` | `24px` |
| `--tabs-overflow-chevron-size-sm` | `tabs` | `20px` |
| `--tabs-overflow-fade-width-md` | `tabs` | `48px` |
| `--tabs-overflow-fade-width-sm` | `tabs` | `32px` |
| `--tabs-panel-color` | `tabs` | `#121212` |
| `--tabs-panel-padding-block` | `tabs` | `16px` |
| `--tabs-selected-color` | `tabs` | `#0058d2` |
| `--tabs-selected-icon-color` | `tabs` | `#0058d2` |
| `--tabs-tab-gap` | `tabs` | `8px` |
| `--tabs-tab-height-md` | `tabs` | `48px` |
| `--tabs-tab-height-sm` | `tabs` | `40px` |
| `--tabs-tab-indicator-color` | `tabs` | `#0058d2` |
| `--tabs-tab-indicator-height` | `tabs` | `2px` |
| `--tabs-tab-inner-gap` | `tabs` | `6px` |
| `--tabs-tab-min-width` | `tabs` | `40px` |
| `--tabs-tab-padding-inline-md` | `tabs` | `16px` |
| `--tabs-tab-padding-inline-sm` | `tabs` | `12px` |
| `--tabs-unselected-color` | `tabs` | `#383838` |
| `--tabs-unselected-color-hover` | `tabs` | `#121212` |
| `--tabs-unselected-icon-color` | `tabs` | `#444444` |
| `--tabs-unselected-icon-color-hover` | `tabs` | `#121212` |
| `--tag-container-border-radius` | `tag` | `4px` |
| `--tag-container-border-width` | `tag` | `1px` |
| `--tag-container-gap-md` | `tag` | `4px` |
| `--tag-container-gap-sm` | `tag` | `4px` |
| `--tag-container-group-gap` | `tag` | `8px` |
| `--tag-container-height-md` | `tag` | `24px` |
| `--tag-container-height-sm` | `tag` | `20px` |
| `--tag-container-min-width-md` | `tag` | `32px` |
| `--tag-container-min-width-sm` | `tag` | `32px` |
| `--tag-container-padding-inline-icon-md` | `tag` | `6px` |
| `--tag-container-padding-inline-icon-sm` | `tag` | `4px` |
| `--tag-container-padding-inline-md` | `tag` | `8px` |
| `--tag-container-padding-inline-sm` | `tag` | `6px` |
| `--tag-icon-size-md` | `tag` | `16px` |
| `--tag-icon-size-sm` | `tag` | `12px` |
| `--tag-label-font-family` | `tag` | `Onest` |
| `--tag-label-font-size-md` | `tag` | `14px` |
| `--tag-label-font-size-sm` | `tag` | `12px` |
| `--tag-label-font-weight-info` | `tag` | `400` |
| `--tag-label-font-weight-status` | `tag` | `500` |
| `--tag-label-line-height-md` | `tag` | `20px` |
| `--tag-label-line-height-sm` | `tag` | `16px` |
| `--tag-outlined-background` | `tag` | `transparent` |
| `--tag-outlined-border-accent` | `tag` | `#dc6803` |
| `--tag-outlined-border-brand` | `tag` | `#0058d2` |
| `--tag-outlined-border-danger` | `tag` | `#d92d20` |
| `--tag-outlined-border-muted` | `tag` | `#d9d9d9` |
| `--tag-outlined-border-neutral` | `tag` | `#d9d9d9` |
| `--tag-outlined-border-success` | `tag` | `#027948` |
| `--tag-outlined-label-accent` | `tag` | `#b54708` |
| `--tag-outlined-label-brand` | `tag` | `#0058d2` |
| `--tag-outlined-label-danger` | `tag` | `#b32318` |
| `--tag-outlined-label-muted` | `tag` | `#757575` |
| `--tag-outlined-label-neutral` | `tag` | `#121212` |
| `--tag-outlined-label-success` | `tag` | `#027948` |
| `--tag-strong-background-accent` | `tag` | `#fec84b` |
| `--tag-strong-background-brand` | `tag` | `#0058d2` |
| `--tag-strong-background-danger` | `tag` | `#d92d20` |
| `--tag-strong-background-muted` | `tag` | `#444444` |
| `--tag-strong-background-neutral` | `tag` | `#1e1e1e` |
| `--tag-strong-background-success` | `tag` | `#027948` |
| `--tag-strong-label-accent` | `tag` | `#121212` |
| `--tag-strong-label-brand` | `tag` | `#ffffff` |
| `--tag-strong-label-danger` | `tag` | `#ffffff` |
| `--tag-strong-label-muted` | `tag` | `#ffffff` |
| `--tag-strong-label-neutral` | `tag` | `#ffffff` |
| `--tag-strong-label-success` | `tag` | `#ffffff` |
| `--tag-subtle-background-accent` | `tag` | `#feefc6` |
| `--tag-subtle-background-brand` | `tag` | `#e8f0fb` |
| `--tag-subtle-background-danger` | `tag` | `#fee4e2` |
| `--tag-subtle-background-muted` | `tag` | `#ffffff` |
| `--tag-subtle-background-neutral` | `tag` | `#f1f1f1` |
| `--tag-subtle-background-success` | `tag` | `#e6f5ee` |
| `--tag-subtle-label-accent` | `tag` | `#b54708` |
| `--tag-subtle-label-brand` | `tag` | `#0058d2` |
| `--tag-subtle-label-danger` | `tag` | `#b32318` |
| `--tag-subtle-label-muted` | `tag` | `#757575` |
| `--tag-subtle-label-neutral` | `tag` | `#121212` |
| `--tag-subtle-label-success` | `tag` | `#027948` |
| `--tooltip-arrow-edge-margin` | `tooltip` | `12px` |
| `--tooltip-arrow-size-lg` | `tooltip` | `12px` |
| `--tooltip-arrow-size-sm` | `tooltip` | `8px` |
| `--tooltip-arrow-width-lg` | `tooltip` | `24px` |
| `--tooltip-arrow-width-sm` | `tooltip` | `16px` |
| `--tooltip-close-background-default` | `tooltip` | `transparent` |
| `--tooltip-close-background-hover` | `tooltip` | `#383838` |
| `--tooltip-close-border-radius` | `tooltip` | `4px` |
| `--tooltip-close-color-default` | `tooltip` | `#ffffff` |
| `--tooltip-close-color-hover` | `tooltip` | `#ffffff` |
| `--tooltip-close-size` | `tooltip` | `16px` |
| `--tooltip-close-touch-target` | `tooltip` | `24px` |
| `--tooltip-container-background-default` | `tooltip` | `#1e1e1e` |
| `--tooltip-container-border-radius-lg` | `tooltip` | `6px` |
| `--tooltip-container-border-radius-sm` | `tooltip` | `4px` |
| `--tooltip-container-gap-lg` | `tooltip` | `12px` |
| `--tooltip-container-gap-sm` | `tooltip` | `8px` |
| `--tooltip-container-max-width` | `tooltip` | `200px` |
| `--tooltip-container-padding-block-lg` | `tooltip` | `12px` |
| `--tooltip-container-padding-block-sm` | `tooltip` | `8px` |
| `--tooltip-container-padding-inline-end-lg` | `tooltip` | `12px` |
| `--tooltip-container-padding-inline-lg` | `tooltip` | `16px` |
| `--tooltip-container-padding-inline-sm` | `tooltip` | `12px` |
| `--tooltip-container-shadow` | `tooltip` | `0px 2px 8px 0px rgba(19, 22, 29, 0.06), 0px 4px 8px 1px rgba(19, 22, 29, 0.04)` |
| `--tooltip-container-transition-duration` | `tooltip` | `150ms` |
| `--tooltip-container-transition-timing-function` | `tooltip` | `ease-out` |
| `--tooltip-container-z-index` | `tooltip` | `9999` |
| `--tooltip-hint-color-default` | `tooltip` | `#ffffff` |
| `--tooltip-hint-font-family` | `tooltip` | `Onest` |
| `--tooltip-hint-font-size` | `tooltip` | `12px` |
| `--tooltip-hint-font-weight` | `tooltip` | `400` |
| `--tooltip-hint-line-height` | `tooltip` | `16px` |
| `--tooltip-hint-margin-top` | `tooltip` | `6px` |
| `--tooltip-hint-opacity` | `tooltip` | `0.72` |
| `--tooltip-label-color-default` | `tooltip` | `#ffffff` |
| `--tooltip-label-font-family` | `tooltip` | `Onest` |
| `--tooltip-label-font-size` | `tooltip` | `14px` |
| `--tooltip-label-font-weight` | `tooltip` | `400` |
| `--tooltip-label-line-height` | `tooltip` | `20px` |
| `--tooltip-offset-trigger` | `tooltip` | `4px` |

