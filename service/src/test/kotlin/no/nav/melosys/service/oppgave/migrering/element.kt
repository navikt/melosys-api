package no.nav.melosys.service.oppgave.migrering

interface Element {
    fun export(exporter: Exporter): String
}

class Table : Element {
    internal var headers: TableRow = TableRow()
    internal val rows = mutableListOf<TableRow>()

    override fun export(exporter: Exporter): String = exporter.renderTable(this)
}

class TabelCell(
    internal val text: Any,
    internal val fc: String? = null,
    internal val bc: String? = null,
    internal val font: Font = Font.NORMAL
) : Element {
    override fun export(exporter: Exporter): String = exporter.renderTableCell(this)
}

class TableRow : Element {
    internal val cols = mutableListOf<TabelCell>()

    override fun export(exporter: Exporter): String = exporter.renderTableRow(this)
}

enum class Font {
    NORMAL,
    STRONG,
    ITALIC
}
