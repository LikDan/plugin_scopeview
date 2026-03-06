package org.jetbrains.plugins.template.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

class MultiScopeRootNode(
    project: Project,
    private val settings: ViewSettings,
    private val scopeNames: List<String>,
) : AbstractTreeNode<String>(project, "root") {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = "root"
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> =
        scopeNames.map { ScopeNode(project, settings, it) }
}
