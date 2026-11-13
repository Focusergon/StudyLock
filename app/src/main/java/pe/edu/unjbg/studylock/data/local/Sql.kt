package pe.edu.unjbg.studylock.data.local

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

internal fun <T> SQLiteDatabase.list(
    sql: String,
    args: Array<String> = emptyArray(),
    mapper: (Cursor) -> T,
): List<T> = rawQuery(sql, args).use { c ->
    val out = ArrayList<T>(c.count)
    while (c.moveToNext()) out.add(mapper(c))
    out
}

internal fun <T> SQLiteDatabase.one(
    sql: String,
    args: Array<String> = emptyArray(),
    mapper: (Cursor) -> T,
): T? = rawQuery(sql, args).use { c -> if (c.moveToFirst()) mapper(c) else null }

internal fun SQLiteDatabase.count(sql: String, args: Array<String> = emptyArray()): Int =
    one(sql, args) { it.getInt(0) } ?: 0

internal fun Cursor.str(name: String): String = getString(getColumnIndexOrThrow(name)) ?: ""
internal fun Cursor.strOrNull(name: String): String? {
    val index = getColumnIndexOrThrow(name)
    return if (isNull(index)) null else getString(index)
}
internal fun Cursor.i(name: String): Int = getInt(getColumnIndexOrThrow(name))
internal fun Cursor.l(name: String): Long = getLong(getColumnIndexOrThrow(name))
internal fun Cursor.d(name: String): Double = getDouble(getColumnIndexOrThrow(name))
internal fun Cursor.b(name: String): Boolean = getInt(getColumnIndexOrThrow(name)) != 0
