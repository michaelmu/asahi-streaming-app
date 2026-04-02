package ai.shieldtv.app.ui

import android.view.View
import java.util.WeakHashMap

class FocusMemory {
    private val lastFocusedByScreen = mutableMapOf<String, Int>()
    private val viewIds = WeakHashMap<View, Int>()
    private var nextId = 1

    fun remember(screenKey: String, view: View) {
        val id = viewIds.getOrPut(view) { nextId++ }
        lastFocusedByScreen[screenKey] = id
    }

    fun shouldRestore(screenKey: String, view: View): Boolean {
        val id = viewIds.getOrPut(view) { nextId++ }
        return lastFocusedByScreen[screenKey] == id
    }
}
