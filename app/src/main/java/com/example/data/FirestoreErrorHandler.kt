package com.example.data

import android.util.Log

enum class OperationType {
    GET, LIST, CREATE, UPDATE, DELETE
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    val errorJson = """
        {
            "event": "firestore_error",
            "operation": "${operation.name}",
            "path": "$path",
            "message": "${exception.localizedMessage?.replace("\"", "\\\"") ?: "Unknown error"}",
            "class": "${exception.javaClass.simpleName}"
        }
    """.trimIndent()
    Log.e("FirestoreError", errorJson, exception)
}
