package com.example.usconstitution

import android.content.Context

data class Part(val title: String, val body: String)

data class Group(val name: String, val parts: List<Part>)

private val ASSET_FILES = listOf(
    "constitution.txt",
    "documents.txt",
    "documents2.txt",
    "federalist.txt",
    "more-documents.txt",
    "scotus.txt",
    "ratification.txt",
    "speeches.txt",
)

/**
 * Parses the text files in assets/.
 *
 *   @group Name      starts a group shown as a list header
 *   # Title          starts a part (article / amendment / document)
 *   anything else    is the part's body text
 *
 * A group name that appears in more than one file is one group: its entries are joined, in file
 * order. A group whose entry titles ("Name – March 4, 1801") all carry a year is then listed in
 * date order; the sort is stable, so entries from the same date keep their file order.
 */
fun loadConstitution(context: Context): List<Group> {
    val groups = mutableListOf<Group>()
    var groupName: String? = null
    var parts = mutableListOf<Part>()
    var title: String? = null
    val body = StringBuilder()

    fun finishPart() {
        title?.let { parts.add(Part(it, body.toString().trim())) }
        title = null
        body.clear()
    }

    fun finishGroup() {
        finishPart()
        groupName?.let { groups.add(Group(it, parts)) }
        parts = mutableListOf()
    }

    for (file in ASSET_FILES) {
        context.assets.open(file).bufferedReader().useLines { lines ->
            for (line in lines) {
                when {
                    line.startsWith("@group ") -> {
                        finishGroup()
                        groupName = line.removePrefix("@group ").trim()
                    }
                    line.startsWith("# ") -> {
                        finishPart()
                        title = line.removePrefix("# ").trim()
                    }
                    else -> body.appendLine(line)
                }
            }
        }
        finishGroup()
        groupName = null
    }
    return mergeAndSort(groups)
}

/**
 * The order the groups appear in the list. A group that is not named here (a new one) is listed
 * after these, in the order it was first loaded.
 */
private val GROUP_ORDER = listOf(
    "The Constitution",
    "The Bill of Rights",
    "Later Amendments",
    "American DNA",
    "The First Constitution",
    "Founding Era",
    "The Federalist Papers",
    "Presidential Addresses",
    "Older Roots",
    "Supreme Court: Power of Government",
    "Supreme Court: Race and Equality",
    "Supreme Court: Speech, Religion and the Press",
    "Supreme Court: Criminal Justice",
    "Supreme Court: Privacy and Liberty",
    "Supreme Court: The Second Amendment",
    "State Ratifications",
)

private fun mergeAndSort(groups: List<Group>): List<Group> {
    val merged = LinkedHashMap<String, MutableList<Part>>()
    for (group in groups) merged.getOrPut(group.name) { mutableListOf() }.addAll(group.parts)
    val ordered = merged.entries.sortedBy { entry ->
        GROUP_ORDER.indexOf(entry.key).let { if (it < 0) Int.MAX_VALUE else it }
    }
    return ordered.map { (name, parts) ->
        val keys = parts.map { dateKey(it.title) }
        if (keys.all { it != null }) {
            Group(name, parts.indices.sortedWith(compareBy<Int>({ keys[it]!!.first }, { keys[it]!!.second }, { keys[it]!!.third }))
                .map { parts[it] })
        } else {
            Group(name, parts)
        }
    }
}

private val yearRegex = Regex("\\b(1[0-9]{3}|20[0-9]{2})\\b")
private val monthDayRegex = Regex(
    "(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{1,2})",
)
private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/** (year, month, day) from the part of a title after " – ", or null if it has no year. */
private fun dateKey(title: String): Triple<Int, Int, Int>? {
    val subtitle = title.substringAfter(" – ", "")
    val year = yearRegex.find(subtitle)?.value?.toInt() ?: return null
    val md = monthDayRegex.find(subtitle)
    val month = md?.groupValues?.get(1)?.let { monthNames.indexOf(it) + 1 } ?: 0
    val day = md?.groupValues?.get(2)?.toInt() ?: 0
    return Triple(year, month, day)
}
