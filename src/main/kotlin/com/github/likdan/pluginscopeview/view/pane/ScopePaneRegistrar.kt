package com.github.likdan.pluginscopeview.view.pane

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.github.likdan.pluginscopeview.view.settings.ScopeSettings

class ScopePaneRegistrar : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val panes = mutableListOf<ScopeGroupPane>()

        fun refresh() {
            val projectView = ProjectView.getInstance(project)
            panes.forEach { projectView.removeProjectPane(it) }
            panes.clear()
            ScopeSettings.getInstance().groups.forEachIndexed { i, group ->
                val pane = ScopeGroupPane(project, group.name, group.scopes.toList(), i)
                projectView.addProjectPane(pane)
                panes.add(pane)
            }
        }

        ApplicationManager.getApplication().invokeLater { refresh() }
        ScopeSettings.getInstance().addChangeListener {
            ApplicationManager.getApplication().invokeLater { refresh() }
        }
    }
}
