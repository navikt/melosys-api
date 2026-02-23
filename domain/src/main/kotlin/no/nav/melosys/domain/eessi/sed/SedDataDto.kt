package no.nav.melosys.domain.eessi.sed

import no.nav.melosys.domain.eessi.A008Formaal
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak

class SedDataDto : SedGrunnlagDto() {
    // Persondok.
    var bruker: Bruker? = null
    var kontaktadresse: Adresse? = null
    var oppholdsadresse: Adresse? = null
    var familieMedlem: MutableList<FamilieMedlem> = mutableListOf()
    var søknadsperiode: Periode? = null

    // Videresending av søknad
    var avklartBostedsland: String? = null

    var gsakSaksnummer: Long? = null

    // Lovvalg
    var tidligereLovvalgsperioder: MutableList<Lovvalgsperiode> = mutableListOf()

    var mottakerIder: List<String>? = null

    var svarAnmodningUnntak: SvarAnmodningUnntak? = null

    var utpekingAvvis: UtpekingAvvisDto? = null

    var vedtakDto: VedtakDto? = null

    var invalideringSedDto: InvalideringSedDto? = null

    // A008 CDM 4.4 - formål med A008 SED
    var a008Formaal: A008Formaal? = null

    // A001 CDM 4.4 - rammeavtale om fjernarbeid (TWFA)
    var erFjernarbeidTWFA: Boolean? = null
}
