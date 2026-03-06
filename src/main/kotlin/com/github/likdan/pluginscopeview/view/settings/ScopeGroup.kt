package com.github.likdan.pluginscopeview.view.settings

data class ScopeGroup(
    var name: String = "",
    var scopes: MutableList<String> = mutableListOf()
)
