package no.nav.melosys.service.oppgave.migrering

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class TabelDsl

@TabelDsl
fun document(builder: Document.() -> Unit) = Document().apply(builder)

@TabelDsl
fun Document.table(builder: Table.() -> Unit) = elements.add(Table().apply(builder))

@TabelDsl
fun Table.th(builder: TableRow.() -> Unit) {
    this.headers = TableRow().apply(builder)
}

@TabelDsl
fun Table.tr(builder: TableRow.() -> Unit) {
    this.rows.add(TableRow().apply(builder))
}

@TabelDsl
fun TableRow.item(
    fc: String? = null,
    bc: String? = null,
    font: Font = Font.NORMAL,
    builder: TableRow.() -> Any?
) = cols.add(TabelCell(builder.invoke(this) ?: "null", fc, bc, font))



