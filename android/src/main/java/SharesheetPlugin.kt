// Copyright 2019-2023 Tauri Programme within The Commons Conservancy
// SPDX-License-Identifier: Apache-2.0
// SPDX-License-Identifier: MIT

package app.tauri.sharesheet

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.net.Uri
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Channel
import app.tauri.plugin.JSObject
import app.tauri.plugin.Invoke
import app.tauri.plugin.Plugin

@InvokeArg
class SetEventHandlerArgs {
    lateinit var handler: Channel
}

@InvokeArg
class ShareTextOptions {
    lateinit var text: String
    var mimeType: String = "text/plain"
    var title: String? = null
}


@TauriPlugin
class SharesheetPlugin(private val activity: Activity): Plugin(activity) {

    private var channel: Channel? = null
    // This command should not be added to the `build.rs` and exposed as it is only
    // used internally from the rust backend.
    @Command
    fun setShareEventHandler(invoke: Invoke) {
        val args = invoke.parseArgs(SetEventHandlerArgs::class.java)
        this.channel = args.handler
        invoke.resolve()
    }

    /**
     * Open the Sharesheet to share some text
     */
    @Command
    fun shareText(invoke: Invoke) {        
        val args = invoke.parseArgs(ShareTextOptions::class.java)

        val sendIntent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.type = args.mimeType
            this.putExtra(Intent.EXTRA_TEXT, args.text)
            this.putExtra(Intent.EXTRA_TITLE, args.title)
        }

        val shareIntent = Intent.createChooser(sendIntent, null);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.applicationContext?.startActivity(shareIntent);
    }

    override fun load(webView: WebView) {
      trigger("load", JSObject())
    }

    override fun onNewIntent(intent: Intent) {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/*" == intent.type) {
                    val event = JSObject()
                    event.put("mime_type", intent.type)
                    event.put("data", intent.data.toString())
                    this.channel?.send(event)
                    //trigger("newIntent", event)
                } else if (intent.type?.startsWith("image/") == true) {
                    println("send image unimplemented")
                    //handleSendImage(intent)
                }
            }
            intent?.action == Intent.ACTION_SEND_MULTIPLE
                    && intent.type?.startsWith("image/") == true -> {
                    println("send images unimplemented")
                    //handleSendMultipleImages(intent)
            }
        }
    }
}
