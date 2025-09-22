package no.nav.melosys.domain.eessi.sed

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
}
