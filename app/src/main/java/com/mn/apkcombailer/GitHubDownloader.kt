package com.mn.apkcombailer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object GitHubDownloader {
    suspend fun download(context: Context, repositoryUrl: String, token: String): File =
        withContext(Dispatchers.IO) {
            val (owner, repo) = parseRepository(repositoryUrl)
            val apiUrl = URL("https://api.github.com/repos/$owner/$repo/zipball")
            val first = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "MN-APK-COMBAILER")
                if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                instanceFollowRedirects = false
            }

            val code = first.responseCode
            val downloadUrl = when (code) {
                HttpURLConnection.HTTP_OK -> apiUrl
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_MOVED_PERM,
                307, 308 -> URL(first.getHeaderField("Location")
                    ?: error("GitHub did not provide a download URL."))
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN -> error("GitHub authentication failed. Check the token and repository access.")
                HttpURLConnection.HTTP_NOT_FOUND -> error("Repository not found, or you do not have permission to access it.")
                else -> error("GitHub returned HTTP $code.")
            }
            first.disconnect()

            val connection = (downloadUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "MN-APK-COMBAILER")
                // The redirect URL is a temporary signed URL; do not forward the PAT.
            }
            if (connection.responseCode !in 200..299) {
                val result = connection.responseCode
                connection.disconnect()
                error("Repository download failed (HTTP $result).")
            }

            val base = File(context.filesDir, "repositories").apply { mkdirs() }
            val destination = File(base, "${owner}_${repo}").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val zipFile = File(base, "repository.zip")
            connection.inputStream.use { input ->\n                zipFile.outputStream().use { output -> input.copyTo(output) }\n            }
            connection.disconnect()

            try {
                ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val relative = entry.name.substringAfter('/', "")
                        if (relative.isBlank()) {
                            zip.closeEntry()
                            continue
                        }
                        val output = File(destination, relative)
                        val canonicalRoot = destination.canonicalPath + File.separator
                        if (!output.canonicalPath.startsWith(canonicalRoot)) {
                            error("Archive contains an unsafe file path.")
                        }
                        if (entry.isDirectory) output.mkdirs()
                        else {
                            output.parentFile?.mkdirs()
                            output.outputStream().buffered().use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                    }
                }
            } finally {
                zipFile.delete()
            }
            destination
        }

    private fun parseRepository(value: String): Pair<String, String> {
        val uri = runCatching { java.net.URI(value.trim()) }
            .getOrElse { error("Enter a valid GitHub repository URL.") }
        if (uri.scheme != "https" || uri.host?.lowercase() != "github.com") {
            error("Only https://github.com/owner/repository URLs are supported.")
        }
        val parts = uri.path.trim('/').split('/')
        if (parts.size < 2) error("Repository URL must include owner and repository.")
        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        val valid = Regex("[A-Za-z0-9_.-]+")
        if (!valid.matches(owner) || !valid.matches(repo)) error("Invalid repository owner or name.")
        return owner to repo
    }
}
