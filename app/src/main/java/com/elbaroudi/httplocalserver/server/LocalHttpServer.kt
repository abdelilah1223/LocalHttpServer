package com.elbaroudi.httplocalserver.server

import android.content.Context
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class FileInfo(val name: String, val size: Long)

object LocalHttpServer {
    private var server: ApplicationEngine? = null

    fun start(context: Context, port: Int = 8080) {
        if (server != null) return

        val rootDir = File(context.getExternalFilesDir(null), "uploads")
        if (!rootDir.exists()) rootDir.mkdirs()

        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(StatusPages) {
                exception<Throwable> { call: ApplicationCall, cause: Throwable ->
                    call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            routing {
                get("/") {
                    val html = withContext(Dispatchers.IO) {
                        try {
                            context.assets.open("index.html").bufferedReader().use { it.readText() }
                        } catch (e: Exception) {
                            "<html><body><h1>Error loading index.html</h1><p>${e.message}</p></body></html>"
                        }
                    }
                    call.respondText(html, ContentType.Text.Html)
                }

                post("/upload") {
                    val multipart = call.receiveMultipart()
                    try {
                        withContext(Dispatchers.IO) {
                            while (true) {
                                val part = multipart.readPart() ?: break
                                if (part is PartData.FileItem) {
                                    val fileName = part.originalFileName ?: "uploaded_file_${System.currentTimeMillis()}"
                                    val file = File(rootDir, fileName)
                                    part.streamProvider().use { input ->
                                        file.outputStream().buffered().use { output ->
                                            input.copyTo(output, 4 * 1024 * 1024)
                                        }
                                    }
                                }
                                part.dispose()
                            }
                        }
                        call.respondText("Upload completed")
                    } catch (e: Exception) {
                        call.respondText("Upload failed: ${e.message}", status = HttpStatusCode.InternalServerError)
                    }
                }

                get("/data") {
                    val files = withContext(Dispatchers.IO) {
                        rootDir.listFiles()?.map {
                            FileInfo(it.name, it.length())
                        } ?: emptyList()
                    }
                    call.respond(files)
                }

                get("/download/{name}") {
                    val name = call.parameters["name"] ?: return@get
                    val file = File(rootDir, name)
                    if (file.exists() && file.isFile) {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
                        )
                        call.respondFile(file)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                delete("/file/{name}") {
                    val name = call.parameters["name"] ?: return@delete
                    val file = File(rootDir, name)
                    if (file.exists() && file.isFile) {
                        if (file.delete()) {
                            call.respond(HttpStatusCode.OK, "File deleted")
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "Could not delete file")
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/download-app") {
                    val apkFile = File(context.packageCodePath)
                    if (apkFile.exists()) {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "HttpLocalServer.apk").toString()
                        )
                        call.respondFile(apkFile)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun isRunning(): Boolean = server != null
}
