package com.intuit.player.plugins.devtools

import com.facebook.flipper.android.AndroidFlipperClient
import com.intuit.player.android.AndroidPlayer
import com.intuit.player.android.AndroidPlayerPlugin
import com.intuit.player.jvm.core.player.state.CompletedState
import com.intuit.player.jvm.core.plugins.JSPluginWrapper
import com.intuit.player.jvm.core.plugins.PlayerPluginException

private var count = 0

/**
 * [AndroidPlayerPlugin] responsible for proxying Player events and registering to receive Flipper method requests.
 * Configuration with this plugin requires the [AndroidFlipperClient] to be initialized by the containing app.
 */
public class AndroidDevtoolsPlugin private constructor(public var playerID: String, private val devtoolsPlugin: DevtoolsPlugin) : AndroidPlayerPlugin, JSPluginWrapper by devtoolsPlugin, DevtoolsMethodHandler by devtoolsPlugin {

    private val flipperPlugin = (AndroidFlipperClient
        .getInstanceIfInitialized() ?: throw PlayerPluginException(this::class.java.simpleName, "AndroidFlipperClient not initialized. Ensure your app is initializing the AndroidFlipperClient before this plugin is instantiated.\nhttps://fbflipper.com/docs/getting-started/android-native/"))
        .getPluginByClass(DevtoolsFlipperPlugin::class.java) ?: throw PlayerPluginException(this::class.java.simpleName, "${DevtoolsFlipperPlugin::class.java.simpleName} not found. Ensure the AndroidFlipperClient is registering the ${DevtoolsFlipperPlugin::class.java.simpleName} plugin.")

    public constructor(playerID: String = "player-${count++}") : this(playerID, DevtoolsPlugin(playerID))

    init {
        devtoolsPlugin.onEvent = DevtoolsEventPublisher(flipperPlugin::publishAndroidMessage)
    }

    override fun apply(androidPlayer: AndroidPlayer) {
        flipperPlugin.register(this)

        androidPlayer.hooks.state.tap { state ->
            // TODO: Should probably be on ReleasedState?
            if (state is CompletedState) {
                flipperPlugin.remove(this)
            }
        }
    }
}