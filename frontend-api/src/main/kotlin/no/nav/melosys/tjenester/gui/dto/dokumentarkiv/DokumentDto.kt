package no.nav.melosys.tjenester.gui.dto.dokumentarkiv

class DokumentDto {
    @JvmField val dokumentID: String?
    @JvmField val tittel: String
    @JvmField val logiskeVedlegg: List<String>

    constructor(tittel: String) {
        this.dokumentID = null
        this.tittel = tittel
        this.logiskeVedlegg = ArrayList()
    }

    constructor(dokumentID: String?, tittel: String, logiskeVedlegg: List<String>) {
        this.dokumentID = dokumentID
        this.tittel = tittel
        this.logiskeVedlegg = logiskeVedlegg
    }
}
