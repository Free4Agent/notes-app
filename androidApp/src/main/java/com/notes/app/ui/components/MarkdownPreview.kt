package com.notes.app.ui.components

import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        color = MaterialTheme.colorScheme.surface
    ) {
        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    setTextIsSelectable(true)
                    setPadding(32, 32, 32, 32)
                    
                    val markwon = Markwon.builder(context)
                        .usePlugin(StrikethroughPlugin.create())
                        .usePlugin(TablePlugin.create(context))
                        .usePlugin(TaskListPlugin.create(context))
                        .usePlugin(HtmlPlugin.create())
                        .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES))
                        .usePlugin(SyntaxHighlightPlugin.create(Prism4jThemeDefault.create()))
                        .build()
                    
                    tag = markwon
                }
            },
            update = { view ->
                val markwon = view.tag as Markwon
                markwon.setMarkdown(view, markdown)
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
