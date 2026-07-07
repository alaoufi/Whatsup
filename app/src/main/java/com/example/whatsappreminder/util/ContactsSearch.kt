package com.example.whatsappreminder.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/** نتيجة بحث جهة اتصال: الاسم والرقم */
data class ContactResult(val name: String, val number: String)

/**
 * البحث في جهات اتصال الجهاز بالاسم أو الرقم.
 * يستخدم CONTENT_FILTER_URI الذي يطابق الاسم والرقم معاً.
 * يتطلب صلاحية READ_CONTACTS.
 */
object ContactsSearch {

    fun search(context: Context, query: String, limit: Int = 8): List<ContactResult> {
        if (query.isBlank()) return emptyList()

        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val results = mutableListOf<ContactResult>()
        val seenNumbers = mutableSetOf<String>()

        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext() && results.size < limit) {
                    val name = cursor.getString(0) ?: ""
                    // تنظيف الرقم مع الإبقاء على رمز الدولة (+)
                    val number = (cursor.getString(1) ?: "")
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "")
                    // تجنّب تكرار نفس الرقم
                    if (number.isNotBlank() && seenNumbers.add(number)) {
                        results.add(ContactResult(name, number))
                    }
                }
            }
        }
        return results
    }
}
