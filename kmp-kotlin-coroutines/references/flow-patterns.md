# Flow Patterns

## Flow Operator Reference

### Transformation Operators

| Operator | Purpose | When to use |
|----------|---------|-------------|
| `map` | Transform each emitted value | Data mapping, type conversion |
| `mapLatest` | Transform, cancelling previous if new value arrives | Search, API calls triggered by user input |
| `transform` | Emit zero or more values per input | Conditional emission, expanding |
| `flatMapLatest` | Switch to a new Flow on each emission, cancel previous | Dependent API calls, Flow-of-Flows |
| `flatMapConcat` | Collect inner Flows sequentially | Ordered dependent operations |
| `flatMapMerge` | Collect inner Flows concurrently | Parallel independent operations |

### Filtering Operators

| Operator | Purpose | When to use |
|----------|---------|-------------|
| `filter` | Keep values matching predicate | Conditional processing |
| `filterNotNull` | Drop null values | Nullable Flow cleanup |
| `distinctUntilChanged` | Drop consecutive duplicates | Avoid redundant processing |
| `debounce` | Wait for pause in emissions | Search input, scroll events |
| `take` | Take first N values | Limited collection |
| `drop` | Skip first N values | Ignore initial values |

### Combining Operators

| Operator | Purpose | When to use |
|----------|---------|-------------|
| `combine` | Merge latest values from multiple Flows | UI state from multiple sources |
| `zip` | Pair values 1-to-1 from two Flows | Synchronized streams |
| `merge` | Interleave emissions from multiple Flows | Multiple event sources |

### Side-Effect Operators

| Operator | Purpose | When to use |
|----------|---------|-------------|
| `onEach` | Perform action on each value without transforming | Logging, analytics |
| `onStart` | Emit initial value or perform action at collection start | Loading states |
| `onCompletion` | Perform action when Flow completes | Cleanup, hide loading |
| `catch` | Handle upstream exceptions | Error recovery |

### Buffering Operators

| Operator | Purpose | When to use |
|----------|---------|-------------|
| `buffer` | Buffer emissions when collector is slow | Backpressure handling |
| `conflate` | Drop intermediate values, keep latest | Fast producer, slow consumer |
| `flowOn` | Change upstream dispatcher | Move work off main thread |

### Terminal Operators

| Operator | Purpose | When to use |
|----------|---------|-------------|
| `collect` | Consume all values | Standard collection |
| `first` / `firstOrNull` | Get first value then cancel | One-shot queries |
| `toList` / `toSet` | Collect all into collection | Finite Flows |
| `launchIn` | Collect in given scope | ViewModel, lifecycle-aware collection |
| `stateIn` | Convert to StateFlow | Expose to UI layer |
| `shareIn` | Convert to SharedFlow | Share upstream among collectors |
