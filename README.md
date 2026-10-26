# Vialcount

Cycle-count workbench for pharmacy stock. The variance rules live in a pure Kotlin module, so they can be tested without an emulator.

I am Anna Brackat, a pharmacy technician and an SNHU computer science student. Counts on the shelf go wrong in ordinary ways: a bottle in the wrong bin, a damaged package, a quantity the computer still believes. Vialcount is a training workbench for that job. It is not a live inventory system, and it is not a prescription-fill app.

The shelf lines are synthetic. SKUs look like `VC-1042`. There are no patient names, no dates of birth, and no manufacturer NDCs.

## Resume bullet

Built Vialcount, an Android cycle-count workbench in Kotlin and Jetpack Compose. Modeled blind counts, recounts, and lead-only variance acceptance in a pure JVM module, and unit-tested the rules on GitHub Actions without an emulator.

## What to run

Domain tests do not need the Android SDK or a device.

```bash
# JDK 17. Android Studio's embedded JDK is enough.
gradle :domain:test
```

GitHub Actions runs that command on every push. See [`.github/workflows/domain-tests.yml`](.github/workflows/domain-tests.yml).

Open this folder in current Android Studio. Studio installs the Gradle wrapper if it asks. Then run the `app` configuration.

## Rules

A line starts `OPEN`. The expected quantity stays hidden until the first count is submitted.

The allowance is the larger of 2 units and 5 percent of expected. The comparison is integer math. A count is outside the percent band when `abs(counted - expected) * 100 > expected * 5`. It is outside the allowance only when it also exceeds 2 units. A difference of exactly 2, or an exact 5 percent, is still inside.

- A first count inside the allowance is `ACCEPTED` and stores `firstCount`. A first count outside it becomes `NEEDS_RECOUNT`.
- A quantity below zero is rejected. The error text does not include the expected quantity.
- A technician or a lead may submit the recount. Copying the first number is not a recount (`same_count`), and the line stays `NEEDS_RECOUNT`.
- A recount inside the allowance is `ACCEPTED`. A recount that is still outside stays `NEEDS_RECOUNT` for a lead.
- Only a lead can accept that variance, and only with a reason: damaged, found in another bin, or data error. Only a lead can reject the count.
- Expected failures return `DomainResult.Err` with a code: `negative`, `state`, `role`, `same_count`, or `reason`. They are not exceptions.

The screen calls these functions. It does not recompute the threshold.

## Layout

| Module | Responsibility |
| --- | --- |
| `domain` | Lines, the blind expected quantity, the allowance, recounts, and lead acceptance. No Android imports. |
| `app` | Jetpack Compose list, a count field, and a Tech/Lead toggle. Displays the domain error string. |

## Stack

- Kotlin 2.2, JDK 17
- Android Gradle Plugin 9.4, Gradle 9.6, `compileSdk` 37
- Jetpack Compose BOM 2026.09, Material 3
- Classic Android DSL, with `android.newDsl=false` and `android.builtInKotlin=false`

The Gradle wrapper jar is not committed. Android Studio creates it on open. CI uses `gradle/actions/setup-gradle` and does not need the jar.
