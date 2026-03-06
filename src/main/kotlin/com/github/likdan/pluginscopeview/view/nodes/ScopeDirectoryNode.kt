package com.github.likdan.pluginscopeview.view.nodes

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager

class ScopeDirectoryNode(
    project: Project,
    directory: PsiDirectory,
    settings: ViewSettings,
    private val scopeFiles: Set<VirtualFile>,
) : PsiDirectoryNode(project, directory, settings) {

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
        val proj = project ?: return emptyList()
        val psiManager = PsiManager.getInstance(proj)
        return (virtualFile?.children ?: return emptyList())
            .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            .mapNotNull { child ->
                when {
                    child.isDirectory && hasMatchingFiles(child, scopeFiles) ->
                        psiManager.findDirectory(child)?.let {
                            ScopeDirectoryNode(proj, it, settings!!, scopeFiles)
                        }
                    !child.isDirectory && child in scopeFiles ->
                        psiManager.findFile(child)?.let { PsiFileNode(proj, it, settings!!) }
                    else -> null
                }
            }
    }
}

fun hasMatchingFiles(dir: VirtualFile, scopeFiles: Set<VirtualFile>): Boolean {
    if (dir.children.any { !it.isDirectory && it in scopeFiles }) return true
    return dir.children.any { it.isDirectory && hasMatchingFiles(it, scopeFiles) }
}
