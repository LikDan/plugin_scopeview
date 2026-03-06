package com.github.likdan.pluginscopeview.view.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.search.scope.packageSet.NamedScopeManager
import com.github.likdan.pluginscopeview.MyBundle
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel

class ScopeSettingsConfigurable(private val project: Project) : Configurable {

    private val groups = mutableListOf<Pair<String, MutableList<String>>>()
    private val groupsModel = DefaultListModel<String>()
    private val groupsList = JBList(groupsModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }
    private val scopesModel = DefaultListModel<String>()
    private val scopesList = JBList(scopesModel)
    private var currentGroup = -1
    private var suppressListener = false
    private lateinit var panel: JPanel

    override fun getDisplayName(): String = MyBundle.message("scopeView.settings.displayName")

    override fun createComponent(): JComponent {
        groupsList.addListSelectionListener {
            if (!it.valueIsAdjusting && !suppressListener) {
                commitCurrentScopes()
                loadScopesForGroup(groupsList.selectedIndex)
            }
        }

        reset()

        val groupsDecorator = ToolbarDecorator.createDecorator(groupsList)
            .setAddAction { addGroup() }
            .setRemoveAction {
                val i = groupsList.selectedIndex; if (i < 0) return@setRemoveAction
                suppressListener = true
                commitCurrentScopes()
                groups.removeAt(i)
                groupsModel.remove(i)
                val newIdx = if (groups.isEmpty()) -1 else minOf(i, groups.size - 1)
                if (newIdx >= 0) groupsList.selectedIndex = newIdx
                loadScopesForGroup(newIdx)
                suppressListener = false
            }
            .setEditAction { renameGroup() }
            .setMoveUpAction { moveGroup(-1) }
            .setMoveDownAction { moveGroup(1) }

        val scopesDecorator = ToolbarDecorator.createDecorator(scopesList)
            .setAddAction { addScope() }
            .setRemoveAction {
                scopesList.selectedIndices.reversed().forEach { scopesModel.remove(it) }
            }
            .setMoveUpAction {
                val i = scopesList.selectedIndex
                if (i > 0) { val item = scopesModel.remove(i); scopesModel.add(i - 1, item); scopesList.selectedIndex = i - 1 }
            }
            .setMoveDownAction {
                val i = scopesList.selectedIndex
                if (i < scopesModel.size - 1) { val item = scopesModel.remove(i); scopesModel.add(i + 1, item); scopesList.selectedIndex = i + 1 }
            }

        val leftPanel = JPanel(BorderLayout()).apply {
            add(JBLabel(MyBundle.message("scopeView.label.groups")), BorderLayout.NORTH)
            add(groupsDecorator.createPanel(), BorderLayout.CENTER)
        }
        val rightPanel = JPanel(BorderLayout()).apply {
            add(JBLabel(MyBundle.message("scopeView.label.scopes")), BorderLayout.NORTH)
            add(scopesDecorator.createPanel(), BorderLayout.CENTER)
        }

        panel = JPanel(BorderLayout())
        panel.add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel).apply {
            dividerLocation = 200
        })
        return panel
    }

    private fun commitCurrentScopes() {
        if (currentGroup >= 0 && currentGroup < groups.size) {
            groups[currentGroup] = groups[currentGroup].first to scopesModel.elements().toList().toMutableList()
        }
    }

    private fun loadScopesForGroup(index: Int) {
        currentGroup = index
        scopesModel.clear()
        if (index >= 0 && index < groups.size) {
            groups[index].second.forEach { scopesModel.addElement(it) }
        }
    }

    private fun addGroup() {
        val name = Messages.showInputDialog(panel, MyBundle.message("scopeView.dialog.groupName.prompt"), MyBundle.message("scopeView.dialog.addGroup.title"), null, "", null) ?: return
        if (name.isBlank()) return
        suppressListener = true
        commitCurrentScopes()
        groups.add(name to mutableListOf())
        groupsModel.addElement(name)
        val newIdx = groups.size - 1
        groupsList.selectedIndex = newIdx
        loadScopesForGroup(newIdx)
        suppressListener = false
    }

    private fun renameGroup() {
        val i = groupsList.selectedIndex; if (i < 0) return
        val newName = Messages.showInputDialog(panel, MyBundle.message("scopeView.dialog.groupName.prompt"), MyBundle.message("scopeView.dialog.renameGroup.title"), null, groups[i].first, null) ?: return
        if (newName.isBlank()) return
        groups[i] = newName to groups[i].second
        groupsModel.set(i, newName)
    }

    private fun moveGroup(delta: Int) {
        val i = groupsList.selectedIndex; if (i < 0) return
        val j = i + delta; if (j < 0 || j >= groups.size) return
        suppressListener = true
        commitCurrentScopes()
        val tmp = groups[i]; groups[i] = groups[j]; groups[j] = tmp
        val tmpName = groupsModel.remove(i); groupsModel.add(j, tmpName)
        groupsList.selectedIndex = j
        loadScopesForGroup(j)
        suppressListener = false
    }

    private fun addScope() {
        if (currentGroup < 0) return
        val alreadyAdded = scopesModel.elements().toList().toSet()
        val available = availableScopeNames() - alreadyAdded
        if (available.isEmpty()) return
        val dialog = ScopeChooserDialog(project, available.sorted())
        if (dialog.showAndGet()) {
            dialog.selected.forEach { if (!scopesModel.contains(it)) scopesModel.addElement(it) }
        }
    }

    private fun availableScopeNames(): Set<String> {
        val names = mutableSetOf<String>()
        NamedScopeManager.getInstance(project).scopes.mapTo(names) { it.scopeId }
        DependencyValidationManager.getInstance(project).scopes.mapTo(names) { it.scopeId }
        return names
    }

    override fun isModified(): Boolean {
        val saved = ScopeSettings.getInstance().groups
        if (saved.size != groups.size) return true
        return saved.zip(groups).any { (g, w) ->
            val effectiveScopes = if (groups.indexOf(w) == currentGroup) scopesModel.elements().toList() else w.second
            g.name != w.first || g.scopes != effectiveScopes
        }
    }

    override fun apply() {
        commitCurrentScopes()
        ScopeSettings.getInstance().groups = groups
            .map { (name, scopes) -> ScopeGroup(name, scopes.toMutableList()) }
            .toMutableList()
        ScopeSettings.getInstance().notifyChanged()
    }

    override fun reset() {
        suppressListener = true
        currentGroup = -1
        scopesModel.clear()
        groups.clear()
        ScopeSettings.getInstance().groups.forEach { g ->
            groups.add(g.name to g.scopes.toMutableList())
        }
        groupsModel.clear()
        groups.forEach { (name, _) -> groupsModel.addElement(name) }
        if (groups.isNotEmpty()) {
            groupsList.selectedIndex = 0
            loadScopesForGroup(0)
        }
        suppressListener = false
    }
}

private class ScopeChooserDialog(
    project: Project,
    private val scopes: List<String>
) : DialogWrapper(project, true) {

    private val chooserModel = DefaultListModel<String>().also { m -> scopes.forEach { m.addElement(it) } }
    private val chooserList = JBList(chooserModel).also {
        it.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }

    val selected: List<String> get() = chooserList.selectedValuesList

    init {
        title = MyBundle.message("scopeView.dialog.chooseScopes.title")
        init()
    }

    override fun createCenterPanel(): JComponent =
        ScrollPaneFactory.createScrollPane(chooserList).also {
            it.preferredSize = java.awt.Dimension(300, 200)
        }

    override fun createNorthPanel(): JComponent =
        JBLabel(MyBundle.message("scopeView.dialog.chooseScopes.label"))
}
