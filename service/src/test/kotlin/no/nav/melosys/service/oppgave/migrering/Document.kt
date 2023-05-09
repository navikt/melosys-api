package no.nav.melosys.service.oppgave.migrering

class Document {
    val elements = mutableListOf<Element>()

    fun export(exporter: Exporter): String  {
        return exporter.renderDocument(elements)
    }
}

