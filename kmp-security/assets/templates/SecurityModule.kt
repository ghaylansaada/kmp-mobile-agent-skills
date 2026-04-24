package {your.package}.di

import {your.package}.security.DeviceIntegrityChecker
import {your.package}.security.SecureDataStoreWrapper
import {your.package}.security.SecureTokenStorage
import {your.package}.security.TokenMigration
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for security components.
 *
 * Platform-specific: use securityModuleAndroid or securityModuleIos
 * depending on the target.
 *
 * Gotcha: EncryptedSharedPreferences (Android) can throw KeyStoreException
 * if the device lock screen changes. The SecureTokenStorage implementation
 * handles this with a try/catch fallback that clears and recreates.
 *
 * Gotcha: iOS Keychain items with kSecAttrAccessibleWhenUnlockedThisDeviceOnly
 * are NOT included in backups and will be lost on device migration.
 */

// -- Android ---------------------------------------------------------------
val securityModuleAndroid: Module = module {
    // SecureTokenStorage requires Android Context
    single { SecureTokenStorage(context = get()) }

    // DeviceIntegrityChecker requires Android Context for package manager
    single { DeviceIntegrityChecker(context = get()) }

    // SecureDataStoreWrapper depends on DataStore and SecureTokenStorage
    single {
        SecureDataStoreWrapper(
            dataStore = get(),         // provided by your DataStore module
            secureTokenStorage = get()
        )
    }

    // Token migration from plain DataStore
    single {
        TokenMigration(
            plainDataStore = get(),
            secureTokenStorage = get()
        )
    }
}

// -- iOS -------------------------------------------------------------------
val securityModuleIos: Module = module {
    // iOS implementations have no constructor parameters
    single { SecureTokenStorage() }
    single { DeviceIntegrityChecker() }

    single {
        SecureDataStoreWrapper(
            dataStore = get(),
            secureTokenStorage = get()
        )
    }

    single {
        TokenMigration(
            plainDataStore = get(),
            secureTokenStorage = get()
        )
    }
}
