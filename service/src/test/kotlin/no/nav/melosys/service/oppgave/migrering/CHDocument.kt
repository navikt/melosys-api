package no.nav.melosys.service.oppgave.migrering

class CHDocument {
    val elements = mutableListOf<CHElement>()

    fun export(exporter: Exporter): String  {
        return exporter.renderDocument(elements)
    }
}

