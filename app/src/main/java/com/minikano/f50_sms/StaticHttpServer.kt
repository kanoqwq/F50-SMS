package com.minikano.f50_sms

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class StaticHttpServer(port: Int, private val rootDir: String) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri
        if (uri == "/") {
            uri = "/index.html" // 默认文件
        }

        val file = File(rootDir + uri)
        if (!file.exists() || file.isDirectory) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "404 Not Found"
            )
        }

        try {
            val fis = FileInputStream(file)
            val mimeType = getMimeTypeForFile(uri)
            return newChunkedResponse(Response.Status.OK, mimeType, fis)
        } catch (e: IOException) {
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "500 Internal Server Error"
            )
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val port = 2333
            val wwwRoot = "www" // 本地静态文件目录

            try {
                val server = StaticHttpServer(port, wwwRoot)
                server.start(SOCKET_READ_TIMEOUT, false)
                println("HTTP 静态服务器已启动：http://localhost:$port")
            } catch (ioe: IOException) {
                System.err.println("无法启动服务器: " + ioe.message)
            }
        }
    }
}