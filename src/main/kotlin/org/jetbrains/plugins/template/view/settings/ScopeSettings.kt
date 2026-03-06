package org.jetbrains.plugins.template.view.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(name = "ScopeViewSettings", storages = [Storage("ScopeViewSettings.xml")])
class ScopeSettings : PersistentStateComponent<ScopeSettings.State> {

    data class State(
        var groups: MutableList<ScopeGroup> = mutableListOf(
            ScopeGroup("Default", mutableListOf("Plugin", "Tests"))
        )
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    var groups: MutableList<ScopeGroup>
        get() = myState.groups
        set(value) { myState.groups = value }

    private val changeListeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun notifyChanged() {
        changeListeners.forEach { it() }
    }

    companion object {
        fun getInstance(): ScopeSettings =
            ApplicationManager.getApplication().getService(ScopeSettings::class.java)
    }
}
