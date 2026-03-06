package com.github.likdan.pluginscopeview.view.pane

import com.intellij.icons.AllIcons
import com.intellij.ide.SelectInTarget
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.StructureTreeModel
import com.github.likdan.pluginscopeview.view.nodes.MultiScopeRootNode
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.tree.DefaultTreeModel

class ScopeGroupPane(
    project: Project,
    private val groupName: String,
    private val scopeNames: List<String>,
    private val order: Int = 0,
) : AbstractProjectViewPaneWithAsyncSupport(project) {

    override fun getTitle(): String = groupName
    override fun getIcon(): Icon = AllIcons.Ide.LocalScope
    override fun getId(): String = "ScopeGroup_$groupName"
    override fun getWeight(): Int = 200 + order

    override fun createComponent(): JComponent = super.createComponent()

    private fun invalidateTreeModel() {
        val asyncModel = myTree?.model as? com.intellij.ui.tree.AsyncTreeModel ?: return
        var structModel: StructureTreeModel<*>? = null
        var clazz: Class<*>? = asyncModel.javaClass
        outer@ while (clazz != null && structModel == null) {
            for (field in clazz.declaredFields) {
                try {
                    field.isAccessible = true
                    val value = field.get(asyncModel)
                    if (value is StructureTreeModel<*>) {
                        structModel = value
                        break@outer
                    }
                } catch (_: Exception) {}
            }
            clazz = clazz.superclass
        }
        structModel?.invalidate()
    }

    override fun updateFromRoot(isSortingChanged: Boolean): ActionCallback {
        ApplicationManager.getApplication().invokeLater {
            myTree?.revalidate()
            myTree?.repaint()
        }
        return ActionCallback.DONE
    }

    override fun select(element: Any?, file: VirtualFile?, requestFocus: Boolean) {}

    override fun createStructure(): AbstractTreeStructureBase {
        val scopes = scopeNames
        return object : ProjectTreeStructure(myProject, getId()) {
            override fun createRoot(project: Project, settings: ViewSettings) =
                MultiScopeRootNode(project, settings, scopes)
            override fun isShowLibraryContents() = false
            override fun getProviders(): List<TreeStructureProvider> = emptyList()
        }
    }

    override fun createTree(treeModel: DefaultTreeModel): DnDAwareTree {
        return object : ProjectViewTree(treeModel) {
            override fun toString(): String = title + " " + super.toString()
        }
    }

    override fun createSelectInTarget(): SelectInTarget = object : ProjectViewSelectInTarget(myProject) {
        override fun getMinorViewId(): String = getId()
        override fun toString(): String = getId()
    }
}
