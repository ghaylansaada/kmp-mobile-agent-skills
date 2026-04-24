# Project Structure Reference -- KMP Project Setup

## Source Set Directory Layout

```
composeApp/
  build.gradle.kts
  proguard-rules.pro
  roomSchemas/
  src/
    commonMain/kotlin/{your/package}/
      App.kt
      core/
        config/
        database/
        datastore/
        image/
        network/
          result/
          serializer/
        paging/
        platform/
        session/
        transfer/
      data/
        local/
          dao/
          entity/
        model/
        pagination/
        remote/
          dto/
            error/
          service/
        repository/
      di/
        modules/
      presentation/
      ui/
        components/
    commonTest/kotlin/{your/package}/
    androidMain/
      AndroidManifest.xml
      kotlin/{your/package}/
        MainActivity.kt
        core/
          database/
          datastore/
          platform/
          transfer/io/
        di/
      res/
    iosMain/kotlin/{your/package}/
      MainViewController.kt
      core/
        database/
        datastore/
        platform/
        transfer/io/
      di/
```

---

## Package Conventions

- `core/` -- infrastructure code shared across features (networking, database, platform abstractions)
- `data/` -- data layer (repositories, DAOs, DTOs, remote services)
- `di/` -- Koin dependency injection module definitions
- `presentation/` -- ViewModels and UI state holders
- `ui/` -- Composable screens and reusable components

Platform source sets (`androidMain`, `iosMain`) mirror the `core/` and `di/` packages
from `commonMain` to provide `actual` implementations of `expect` declarations.
