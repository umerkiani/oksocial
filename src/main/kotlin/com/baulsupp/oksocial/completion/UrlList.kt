package com.baulsupp.oksocial.completion

import com.google.common.collect.Lists
import com.google.common.io.Files
import com.google.common.io.Resources
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

data class UrlList(val match: Match, val urls: List<String>) {
    enum class Match {
        EXACT, SITE, HOSTS
    }

    fun getUrls(prefix: String): List<String> {
        return urls.filter { u -> u.startsWith(prefix) }
    }

    fun replace(variable: String, replacements: List<String>, keepTemplate: Boolean): UrlList {
        if (replacements.isEmpty()) {
            return this
        }

        val regexToken = "\\{$variable\\}"
        val literalToken = "{$variable}"

        val replacementList: List<String>
        if (keepTemplate) {
            replacementList = Lists.newArrayList(replacements)
            if (keepTemplate) {
                replacementList.add(literalToken)
            }
        } else {
            replacementList = replacements
        }

        val newUrls = urls.flatMap { url ->
            if (url.contains("{"))
                replacementList.map { s -> url.replace(regexToken.toRegex(), s) }
            else
                listOf(url)
        }

        return UrlList(match, newUrls)
    }

    @Throws(IOException::class)
    fun toFile(file: File, strip: Int, prefix: String) {
        val content = regex(prefix) + "\n" + urls.joinToString("\n") { u -> u.substring(strip) }

        Files.write(content, file, StandardCharsets.UTF_8)
    }

    private fun regex(prefix: String): String {
        return when (match) {
            UrlList.Match.EXACT -> prefix
            UrlList.Match.HOSTS -> "[^/]*:?/?/?[^/]*"
            UrlList.Match.SITE -> prefix + ".*"
            else -> throw IllegalArgumentException()
        }
    }

    fun combine(b: UrlList): UrlList {
        val newUrls = Lists.newArrayList<String>()

        newUrls.addAll(urls)
        newUrls.addAll(b.urls)

        val newMatch: Match
        newMatch = if (match == b.match) {
            match
        } else {
            Match.EXACT
        }

        return UrlList(newMatch, newUrls)
    }

    override fun toString() = urls.joinToString("\n")

    companion object {
        @Throws(IOException::class)
        fun fromResource(serviceName: String): Optional<UrlList> {
            val url = UrlList::class.java.getResource("/urls/$serviceName.txt")
            return if (url != null) {
                Optional.of(
                        UrlList(Match.SITE, Resources.readLines(url, StandardCharsets.UTF_8)))
            } else {
                Optional.empty()
            }
        }
    }
}
