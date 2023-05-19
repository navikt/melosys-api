package no.nav.melosys.service.oppgave.migrering
interface Exporter {
    fun renderDocument(elements: List<Element>): String
    fun renderTable(table: Table): String
    fun renderTableRow(tableRow: TableRow): String
    fun renderTableCell(tabelCell: TabelCell): String = tabelCell.text.toString()
}

class ConfluenceWikiExporter : Exporter {
    override fun renderDocument(elements: List<Element>): String {
        return elements.joinToString { it: Element -> it.export(this) }
    }

    override fun renderTable(table: Table): String {
        val header = table.headers.cols.joinToString("") { it: TabelCell -> "|" + it.export(this) + "|" }
        val rows = table.rows.joinToString("\n") { it: TableRow -> "|" + it.export(this) + "|" }
        return "|$header|\n$rows"
    }

    override fun renderTableRow(tableRow: TableRow): String {
        return """
            ${tableRow.cols.joinToString("|") { it: TabelCell -> it.export(this) }}
        """.trimIndent()
    }

    override fun renderTableCell(tabelCell: TabelCell): String {
        val cellText = tabelCell.text.toString().ifBlank { " " }
        val text = when (tabelCell.font) {
            Font.NORMAL -> cellText
            Font.STRONG -> "*$cellText*"
            Font.ITALIC -> "_${cellText}_"
        }

        return if (tabelCell.fc != null) {
            "{color:${tabelCell.fc}}$text{color}"
        } else text
    }
}

class HtmlExporter(
    private val ignoreForegroundColor: Boolean = false,
    private val ignoreBackgroundColor: Boolean = false
) : Exporter {
    override fun renderDocument(elements: List<Element>): String {
        return """
        <!DOCTYPE html>
        <html>
        <style>
            table, th, td {
                border: 1px solid black;
                border-collapse: collapse;
            }
            th, td {
                padding: 2px;
            }
        </style>
            <body>
                <table>
                    ${elements.joinToString { it: Element -> it.export(this) }}
                </table>
            </body>
        </html>
    """.trimIndent()
    }

    private fun style(tabelCell: TabelCell): String = mutableListOf<String>().apply {
            if (!ignoreForegroundColor && tabelCell.fc != null) add("color:${tabelCell.fc}")
            if (!ignoreBackgroundColor && tabelCell.bc != null) add("background-color:${tabelCell.bc}")
        }.run {
            return if (isNotEmpty()) "style=\"${joinToString("; ")}\"" else ""
    }


    override fun renderTable(table: Table): String {
        return """
            <tr>
                ${table.headers.cols.joinToString(" ") { it: TabelCell -> "<th ${style(it)}>" + it.export(this) + "</th>" }}
            </tr>
             ${table.rows.joinToString("\n") { it: TableRow -> it.export(this) }}
        """.trimIndent()
    }

    override fun renderTableRow(tableRow: TableRow): String {
        return """
        <tr>
            ${tableRow.cols.joinToString(" ") { it: TabelCell -> "<td ${style(it)}>" + it.export(this) + "</td>" }}
        </tr>""".trimIndent()
    }
    override fun renderTableCell(tabelCell: TabelCell): String {
        val text = when (tabelCell.font) {
            Font.NORMAL -> tabelCell.text
            Font.STRONG -> "<b>${tabelCell.text}</b>"
            Font.ITALIC -> "<i>${tabelCell.text}</i>"
        }

        return text.toString()
    }
}
