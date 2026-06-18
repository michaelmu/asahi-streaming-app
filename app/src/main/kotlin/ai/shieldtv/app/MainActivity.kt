package ai.codexa.app

import ai.codexa.app.ui.CodexRoute
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = CodexAppContainer(this)
        val savedConnection = container.loadSavedConnection()

        setContent {
            MaterialTheme {
                CodexRoute(
                    viewModel = container.viewModel,
                    savedConnection = savedConnection,
                    onSaveConnection = container::saveConnection,
                )
            }
        }
    }
}
