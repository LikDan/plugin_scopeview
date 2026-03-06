package com.github.likdan.pluginscopeview.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.scope.packageSet.NamedScope
import com.intellij.psi.search.scope.packageSet.NamedScopeManager
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder

class ScopeNode(
    project: Project,
    private val settings: ViewSettings,
    private val scopeName: String,
) : AbstractTreeNode<String>(project, scopeName) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = scopeName
        presentation.setIcon(AllIcons.Ide.LocalScope)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val proj = project ?: return emptyList()
        val scopeFiles = resolveScopeFiles(proj)
        val psiManager = PsiManager.getInstance(proj)

        return ProjectRootManager.getInstance(proj).contentRoots
            .filter { root: VirtualFile -> hasMatchingFiles(root, scopeFiles) }
            .flatMap { root: VirtualFile ->
                root.children
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                    .mapNotNull { child ->
                        when {
                            child.isDirectory && hasMatchingFiles(child, scopeFiles) ->
                                psiManager.findDirectory(child)?.let {
                                    ScopeDirectoryNode(proj, it, settings, scopeFiles)
                                }
                            !child.isDirectory && child in scopeFiles ->
                                psiManager.findFile(child)?.let { PsiFileNode(proj, it, settings) }
                            else -> null
                        }
                    }
            }
    }

    private fun resolveScopeFiles(proj: Project): Set<VirtualFile> {
        val sm = NamedScopeManager.getInstance(proj)
        val dm = DependencyValidationManager.getInstance(proj)
        val scope: NamedScope = sm.getScope(scopeName) ?: dm.getScope(scopeName) ?: return emptySet()
        val holder: NamedScopesHolder = if (sm.getScope(scopeName) != null) sm else dm
        val packageSet = scope.value ?: return emptySet()
        val psiManager = PsiManager.getInstance(proj)
        val result = mutableSetOf<VirtualFile>()
        ProjectFileIndex.getInstance(proj).iterateContent { file ->
            if (!file.isDirectory) {
                val psiFile = psiManager.findFile(file)
                if (psiFile != null && packageSet.contains(psiFile, holder)) result.add(file)
            }
            true
        }
        return result
    }
}
