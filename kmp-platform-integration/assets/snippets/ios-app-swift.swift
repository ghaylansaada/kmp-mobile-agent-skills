// Template: SwiftUI App entry point with Kotlin framework integration
// Copy into: iosApp/iosApp/iOSApp.swift
// For ContentView hosting, see references/entry-points.md

import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        BackgroundTasksIosKt.registerAndScheduleBackgroundTasks()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
