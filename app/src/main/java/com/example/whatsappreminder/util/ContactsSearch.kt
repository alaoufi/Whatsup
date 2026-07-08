package com.example.whatsappreminder.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/**
 * نتيجة بحث جهة اتصال: الاسم، الرقم، ونوع الرقم (جوال/منزل/عمل...).
 */
data class ContactResult(
    val name: String,
    val number: String,
    val label: String
)

/**
 * البحث في جهات اتصال الجهاز بالاسم أو الرقم.
 * يعيد كل الأرقام المطابقة (بما فيها أرقام الاسم الواحد المتعددة) ليختار المستخدم.
 * يتطلب صلاحية READ_CONTACTS.
 */
object ContactsSearch {

    fun search(context: Context, query: String, limit: Int = 12): List<ContactResult> {
        if (query.isBlank()) return emptyList()

        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        val results = mutableListOf<ContactResult>()
        val seenNumbers = mutableSetOf<String>()

        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = 0
                val numberIdx = 1
                val typeIdx = 2
                val labelIdx = 3
                while (cursor.moveToNext() && results.size < limit) {
                    val name = cursor.getString(nameIdx) ?: ""
                    val number = (cursor.getString(numberIdx) ?: "")
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "")
                    if (number.isBlank() || !seenNumbers.add(number)) continue

                    val type = cursor.getInt(typeIdx)
                    val customLabel = cursor.getString(labelIdx)
                    results.add(ContactResult(name, number, typeLabel(type, customLabel)))
                }
            }
        }
        return results
    }

    /** تحويل نوع الرقم إلى نص عربي مفهوم */
    private fun typeLabel(type: Int, customLabel: String?): String = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "جوال"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "المنزل"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "العمل"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "رئيسي"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "جوال العمل"
        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel?.takeIf { it.isNotBlank() } ?: "آخر"
        else -> "آخر"
    }
}
