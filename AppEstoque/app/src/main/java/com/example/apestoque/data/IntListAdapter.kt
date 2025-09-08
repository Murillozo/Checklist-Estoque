package com.example.apestoque.data

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

class IntListAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): List<Int> {
        return when (reader.peek()) {
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Int>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(reader.nextInt())
                }
                reader.endArray()
                list
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                emptyList()
            }
            else -> listOf(reader.nextInt())
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: List<Int>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value(it) }
        writer.endArray()
    }
}
