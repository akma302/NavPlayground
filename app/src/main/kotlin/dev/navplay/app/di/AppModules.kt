package dev.navplay.app.di

import dev.navplay.feature.auth.impl.AuthNavigationModule
import dev.navplay.feature.feed.impl.FeedNavigationModule
import dev.navplay.feature.media.impl.MediaNavigationModule
import dev.navplay.feature.profile.impl.ProfileNavigationModule
import org.koin.core.module.Module

/** The composition root explicitly chooses the feature modules in this build. */
val appModules: List<Module> = listOf(
    AppNavigationModule.module,
    AuthNavigationModule.module,
    FeedNavigationModule.module,
    ProfileNavigationModule.module,
    MediaNavigationModule.module,
)
