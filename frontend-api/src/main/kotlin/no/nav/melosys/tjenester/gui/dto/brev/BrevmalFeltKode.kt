package no.nav.melosys.tjenester.gui.dto.brev

enum class BrevmalFeltKode(@JvmField val beskrivelse: String) {
    BREV_TITTEL("Overskrift i brev"),
    DISTRIBUSJONSTYPE("Type brev"),
    FRITEKST("Tekst til brev"),
    FRITEKSTVEDLEGG("Legg til fritekstvedlegg"),
    MANGLER_FRITEKST("Hva skal mottakeren sende inn?"),
    DOKUMENT_TITTEL("Dokumenttittel (valgfritt)"),
    INNLEDNING_FRITEKST("Innledningstekst"),
    STANDARDTEKST_KONTAKTINFORMASJON("Legg til standardtekst med kontaktinformasjon nederst i brevet"),
    VEDLEGG("Vedlegg"),
    UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER("Trygdemyndighet");

    val kode: String
        get() = name
}
