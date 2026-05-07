package com.example.tooltipguide.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import com.example.tooltipguide.R

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MainScreenPreview() {
    AndroidView(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.activity_main, null)
        }
    )
}
