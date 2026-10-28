# TasteIndia (Android)

Indian-cuisine discovery app built on TheMealDB public V1 API (test key `1`).
Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Retrofit/OkHttp · kotlinx.serialization · Coroutines/Flow · DataStore · Coil.

## Toolchain
| | |
|---|---|
| Android Studio | Ladybug (2024.2) or newer |
| JDK | 17 |
| AGP / Gradle / Kotlin | 8.7.3 / 8.9 / 2.0.21 |
| minSdk / targetSdk / compileSdk | 24 / 35 / 35 |

## Setup
1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator/device (needs internet for live data).
3. Unit tests (offline, use fixtures in `app/src/test/resources/fixtures`): `./gradlew testDebugUnitTest`

## Architecture
`ui` (Compose screens + ViewModels) → `data` (MealRepository, DedupLoader, FavouritesStore) → `data.remote` (Retrofit API, parser/mappers, MealService) ; pure logic in `domain`.
- DTOs (`MealSummaryDto`, detail key/value map), domain models (`MealSummary`, `MealDetail`, `Ingredient`), filter state (`FilterState`) and persisted favourites (`FavouriteEntity`) are separate types.
- DI is manual (`AppContainer`); ViewModels take `MealRepository` / `FavouritesStore`, so tests pass fakes (`FakeMealService`, `InMemoryFavouritesStore`).
- One ViewModel per screen; state exposed as immutable `StateFlow`, collected with `collectAsStateWithLifecycle`.

## Route map
| Route | Screen |
|---|---|
| `recipes` | Recipes list + search/filters/sort |
| `favourites` | Saved meals |
| `details/{mealId}` | Details (shared by both tabs; only the id is passed) |

## Endpoints used
`filter.php?a=Indian` (authoritative base set), `filter.php?c=` and `filter.php?i=` (membership sets), `lookup.php?i=` (details), `list.php?c=list`, `list.php?i=list` (filter choices, loaded lazily for ingredients).

## Filtering / intersection strategy
The Indian base set is the only list ever iterated (`domain/FilterLogic.kt`). Category and ingredient responses are converted to ID sets and used purely as membership tests, so non-Indian IDs cannot leak in. Search, favourites-only and sort are applied locally (no stale network results). Category/ingredient fetches use `flatMapLatest`, so only the latest selection updates the UI. "Clear all" resets filters but keeps sort order.

## Cache policy
In-memory only (`DedupLoader`): successful lookups cached per process; concurrent callers share one request; failures are not cached; detail lookups are capped at 4 concurrent calls (`Semaphore`). Row category labels come from a single ViewModel-driven enrichment pass over cached/bounded detail lookups, never from row composables.

## State restoration
Filters/search/sort are held in the ViewModel (scoped to the back-stack entry) and mirrored into `SavedStateHandle` (process death). Scroll position uses saveable `LazyListState`. Favourites (id + name + thumbnail) are persisted in DataStore and never need the network.

## Details fields
Rendered when non-empty: name, image, category, area, ingredient/measure rows, instructions (paragraphs), tags, source link, YouTube link, image-source link, alternate drink. Intentionally omitted: `idMeal` (identity only), `strCreativeCommonsConfirmed`, `dateModified` (not user-facing).

## Assumptions, trade-offs, known issues
- Unit tests only (no instrumented UI tests). Fixtures are used by tests; the running app makes real TheMealDB GET requests.
- No offline cache for the Indian list: with no network the list shows an error + Retry (favourites still work).
- Ingredient picker loads the full ingredient list (one request) the first time it is opened.
- Fixtures are hand-trimmed, representative responses (IDs/names are illustrative).

## Time spent, AI/tool-use disclosure
- Time spent: ____ hours over ____ days (fill in honestly).
- AI tools: Claude (Anthropic) assisted with scaffolding, DTO/parser, filter logic, Compose screens, unit tests and this README. The candidate must review, run and be able to explain/modify every file.
- Candidate notes: ____ (what you changed/debugged yourself).

## Recording
2-4 min screen recording (list, search, combined filters, details, favourites, error/retry, state restoration): add link/file here.

## Attribution
Recipe data and images © TheMealDB (https://www.themealdb.com), used under its educational test-key terms. Libraries: AndroidX/Jetpack Compose, Retrofit, OkHttp, kotlinx.serialization, kotlinx.coroutines, Coil (all Apache-2.0).


## Commands
`./gradlew testDebugUnitTest` (offline) | `./gradlew assembleDebug` (APK)
