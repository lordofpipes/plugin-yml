/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Minecrell <https://github.com/Minecrell>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.minecrell.pluginyml.paper

import net.minecrell.pluginyml.InvalidPluginDescriptionException
import net.minecrell.pluginyml.PlatformPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

class PaperPlugin : PlatformPlugin<PaperPluginDescription>("Paper", "paper-plugin.yml") {

    companion object {
        @JvmStatic private val VALID_NAME = Regex("^[A-Za-z0-9 _.-]+$")
    }

    override fun createExtension(project: Project) = PaperPluginDescription(project)

    override fun setDefaults(project: Project, description: PaperPluginDescription) {
        description.name = description.name ?: project.name
        description.version = description.version ?: project.version.toString()
        description.description = description.description ?: project.description
        description.website = description.website ?: project.findProperty("url")?.toString()
        description.author = description.author ?: project.findProperty("author")?.toString()
    }


    override fun setLibraries(libraries: ResolvedComponentResult?, description: PaperPluginDescription) {
        val resolved = libraries?.let {
            it.dependencies.map { d -> (d as? ResolvedDependencyResult)?.selected?.moduleVersion?.toString() ?: error("No moduleVersion for $d") }
        }
        description.libraries = ((description.libraries ?: listOf()) + (resolved ?: listOf())).distinct()
    }

    override fun validate(description: PaperPluginDescription) {
        val name = description.name ?: throw InvalidPluginDescriptionException("Plugin name is not set")
        if (!VALID_NAME.matches(name)) throw InvalidPluginDescriptionException("Invalid plugin name: should match $VALID_NAME")

        if (description.version.isNullOrEmpty()) throw InvalidPluginDescriptionException("Plugin version is not set")
        description.apiVersion ?: throw InvalidPluginDescriptionException("Plugin API version is not set")
        description.apiVersion?.let { apiVersion ->
            if (apiVersion < "1.19") throw InvalidPluginDescriptionException("Plugin API version must be at least 1.19")
        }

        val main = description.main ?: throw InvalidPluginDescriptionException("Main class is not defined")
        if (main.isEmpty()) throw InvalidPluginDescriptionException("Main class cannot be empty")
        if (main.startsWith("org.bukkit.")) throw InvalidPluginDescriptionException("Main may not be within the org.bukkit namespace")
        if (description.bootstrapper?.startsWith("org.bukkit.") == true) throw InvalidPluginDescriptionException("Bootstrapper may not be within the org.bukkit namespace")
        if (description.bootstrapper?.startsWith("io.papermc.paper.plugin.") == true) throw InvalidPluginDescriptionException("Bootstrapper may not be within the io.papermc.paper.plugin namespace")
        if (description.loader?.startsWith("org.bukkit.") == true) throw InvalidPluginDescriptionException("Loader may not be within the org.bukkit namespace")
        if (description.loader?.startsWith("io.papermc.paper.plugin.") == true) throw InvalidPluginDescriptionException("Loader may not be within the io.papermc.paper.plugin namespace")

        for (command in description.commands) {
            if (command.name.contains(':')) throw InvalidPluginDescriptionException("Command '${command.name}' cannot contain ':'")
            command.aliases?.forEach { alias ->
                if (alias.contains(':')) throw InvalidPluginDescriptionException("Alias '$alias' of '${command.name}' cannot contain ':'")
            }
        }

        for (before in description.loadBefore) {
            if (before.name.isEmpty()) throw InvalidPluginDescriptionException("Plugin name in loadBefore can not be empty")
        }

        for (after in description.loadAfter) {
            if (after.name.isEmpty()) throw InvalidPluginDescriptionException("Plugin name in loadAfter can not be empty")
        }

        for (depend in description.depends) {
            if (depend.name.isEmpty()) throw InvalidPluginDescriptionException("Plugin name in depends can not be empty")
        }

        if (description.provides?.all(VALID_NAME::matches) == false) {
            throw InvalidPluginDescriptionException("Invalid plugin provides name: all should match $VALID_NAME")
        }
    }
}