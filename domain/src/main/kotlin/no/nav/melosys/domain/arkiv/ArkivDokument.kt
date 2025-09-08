package no.nav.melosys.domain.arkiv

open class ArkivDokument(
    var dokumentId: String? = null,
    var tittel: String? = null,
    var navSkjemaID: String? = null
) {
    var logiskeVedlegg: List<LogiskVedlegg> = emptyList() // Til sammensatte dokumenter der vedlegg er scannet inn i ett dokument.
    var dokumentVarianter: List<DokumentVariant> = emptyList()

    fun hentLogiskeVedleggTitler(): List<String> = logiskeVedlegg.map { it.tittel }

    fun setLogiskeVedleggTitler(vedlegg: List<LogiskVedlegg>) {
        logiskeVedlegg = logiskeVedlegg + vedlegg
    }

    fun addDokumentVariant(variant: DokumentVariant) {
        dokumentVarianter = dokumentVarianter + variant
    }

    fun harTilgangTilArkivVariant(): Boolean =
        dokumentVarianter
            .filter { it.erVariantArkiv() }
            .all { it.saksbehandlerHarTilgang }
}
