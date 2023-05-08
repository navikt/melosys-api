package no.nav.melosys.service.oppgave.migrering

interface CHElement {
    fun export(exporter: Exporter): String
}

class Table : CHElement {
    internal var headers: TableRow = TableRow()
    internal val rows = mutableListOf<TableRow>()

    override fun export(exporter: Exporter): String = exporter.renderTable(this)
}

class TabelCell(
    internal val text: Any,
    internal val fc: String? = null,
    internal val bc: String? = null,
    internal val font: Font = Font.NORMAL
) : CHElement {
    override fun export(exporter: Exporter): String = exporter.renderTableCell(this)
}

class TableRow : CHElement {
    internal val cols = mutableListOf<TabelCell>()

    override fun export(exporter: Exporter): String = exporter.renderTableRow(this)
}

enum class Font {
    NORMAL,
    STRONG,
    ITALIC
}
