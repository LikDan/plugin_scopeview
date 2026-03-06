package org.jetbrains.plugins.template.view.settings

data class ScopeGroup(
    var name: String = "",
    var scopes: MutableList<String> = mutableListOf()
)
