# Development Log

## 2026-08-05
- Scaffolded the ScalaFX 21 app: JFXApp3 shell with left-nav for four
  screens, immutable domain model (Household, Dependent, Asset), and
  the Household Profile screen (income entry, add/remove tables for
  dependents and assets).
- Added the eligibility engine: sealed trait SubsidyProgramme with
  four case objects, an abstract Programme class factoring out the
  shared income-threshold check, and a generic evaluate[T] method.
- Refactored EligibilityEngine so every programme's own evaluate call
  actually routes through the generic evaluate[T], not just the
  aggregator — confirmed no duplicated income-threshold logic remains
  across the four programmes.

## 2026-08-06
- Added file-based JSON persistence: HouseholdRepository, with every
  read/parse operation wrapped in Try/Either (no unguarded .get, no
  naked throw). Added reactive inline validation on the Household
  Profile form with bound StringProperty error labels.
- Traced through what happens when the saved JSON file is missing or
  malformed; fixed the specific path so it returns a safe
  Failure/Left instead of crashing. Factored the repeated read →
  parse → decode logic in both repositories into a generic
  loadAllFrom[T] helper, and tightened several members from protected
  to private once confirmed nothing outside the file used them.
- Added the History screen (live-filtering TableView over saved
  checks) and did a DRY pass across the model package, consolidating
  duplicated logic into shared helpers.
- Polished the UI across all four screens: consistent ≥8px section
  spacing, active-screen highlighting in the left-nav, logical Tab
  order through each form, and Enter-to-submit on the last field of
  each form.
- Fixed a keyboard-navigation bug where dependentRelationshipField
  was being skipped during Tab traversal in the Household Profile
  screen.
- Drafted unit test suggestions for EligibilityEngine and
  HouseholdRepository, plus a README setup/run outline.

## 2026-08-07
- Spent most of the day debugging a broken sbt project import:
  project/build.properties was pinned to a non-existent/incompatible
  sbt version, which was blocking sbt-assembly from loading and
  breaking IntelliJ's project structure extraction entirely.
- Diagnosed that the actual root cause was two version numbers
  (sbt's own version and the sbt-assembly plugin version) having been
  pasted into the wrong files. Corrected project/build.properties to
  sbt 1.10.5 and project/plugins.sbt to sbt-assembly 2.2.0, restoring
  a working build.

## 2026-08-09
- Attempted to replace the Checklist stub screen with a real
  ChecklistScreen plus a document-export helper. Generation stopped
  partway through, leaving ChecklistExporter.scala unfinished —
  picked back up in the next session.

## 2026-08-10
- Completed ChecklistScreen and ChecklistExporter, moving the export
  logic into the model package to match where the other repositories
  live. Fixed a Scala 3 indentation bug in a nested match inside the
  export logic. Built a small reusable HousePicker component to
  remove duplicated combo-box/hint code between ChecklistScreen and
  EligibilityCheckScreen, and wired up the final navigation routing.