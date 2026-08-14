# Personal Reflection

## OOP Concepts Applied

I built the application around a small set of deliberately chosen OOP concepts rather than one single design pattern. Abstraction and inheritance form the foundation of my eligibility engine. I used `SubsidyProgram` as a sealed trait to define what every programme must provide, while `Program` is an abstract class that implements the shared income-threshold logic once. This let each of the four concrete programmes focus only on its own specific behaviour.

I also applied subtype polymorphism through `EligibilityEngine.allPrograms`, which handles the four different programme types uniformly as `List[SubsidyProgram]`. Parametric polymorphism was used independently in three areas: `EligibilityEngine.evaluate[T]`, the `loadAllFrom[T]` helper I introduced to remove duplicated repository loading logic, and the generic `removeSelectedButton[T]` factory used in the UI layer. Using these concepts in different parts of the application showed me that polymorphism is a practical tool for reuse, not just a theoretical principle.

I refined encapsulation during development rather than deciding it all upfront. For example, I changed methods such as `meetsIncomeThreshold`, `ensureDataDir`, and `escapeJson`, together with several JSON value types, from `protected` to `private` after confirming they were not required outside their respective files. This reduced unnecessary exposure and clarified the internal structure of the application.

I also used immutability throughout the domain model. `Household`, `Dependent`, `Asset`, and `EligibilityCheckRecord` are immutable case classes, and I make changes using `.copy` rather than modifying existing objects directly. I deliberately restricted mutability to the UI boundary, particularly the `AppState` `ObservableBuffer`s. This kept the interface reactive while the domain model stayed predictable and consistent.

## Problems Encountered

The most time-consuming problems were not related to the OOP design itself, but to the surrounding development toolchain. I initially had an `sbt` version pinned to a non-existent release, `2.2.0`. I also encountered a separate version-string mismatch between `project/build.properties` and `project/plugins.sbt`. These took several rounds of debugging to trace.

At the code level, I encountered a Scala 3 significant-indentation issue that caused `.showAndWait()` to be applied to a string instead of the intended `Alert` object. I also had a mismatch between the resource filename the application referenced, `app.css`, and the actual file, `design.css`, which caused a runtime `NullPointerException` during startup.

Another issue involved a limitation in the ScalaFX API. I initially expected `scalafx.beans.binding.Bindings` to expose `isNull` and `isEmpty` in the same way as the corresponding JavaFX `Bindings` methods. I therefore had to switch to instance-level extension methods or access `javafx.beans.binding.Bindings` directly through a buffer's `.delegate`. This showed me that although ScalaFX gives a Scala-friendly interface to JavaFX, the two APIs are not always identical.

## Strengths and Weaknesses

**Strength:** Consistent error handling is one of the stronger aspects of my implementation. I used `Either` or `Try` for file I/O and JSON decoding operations, and avoided unguarded `.get` calls and bare `throw` statements in the persistence layer. This made errors explicit and reduced the risk of unexpected runtime failures.

**Strength:** My approach to reducing code duplication also improved throughout development. I introduced the `loadAllFrom[T]`, `errorLabel`, and `removeSelectedButton[T]` refactors after reviewing my own working code, rather than getting everything correct in the first implementation. I see this as a process strength — I was able to step back from just making the application work and consider maintainability too.

**Weakness:** The build and tooling configuration consumed a disproportionate amount of my development time. The `sbt` version issues required several rounds of debugging that were largely unrelated to the application's OOP design. More careful project configuration at the start could probably have avoided some of this.

I also identified a small inconsistency I have not fully resolved. `HouseholdRepository.save` and `HouseholdProfileScreen.upsertIntoAppState` implement the same general "replace if an existing record is found" behaviour using different approaches. The repository uses a filter-and-append mechanism, while the UI layer uses index-and-update. Both work correctly, but with more time I would unify them for consistency.