package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.felles.Periode
import java.math.BigDecimal
import java.time.LocalDate

class Arbeidsavtale : HarPeriode {
    var arbeidstidsordning: Arbeidstidsordning? = null //"http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger"
    var avloenningstype: String? = null //"http://nav.no/kodeverk/Kodeverk/Avl_c3_b8nningstyper"
    var yrke: Yrke? = null //"http://nav.no/kodeverk/Kodeverk/Yrker"
    var gyldighetsperiode: Periode? = null
    var avtaltArbeidstimerPerUke: BigDecimal? = null
    var stillingsprosent: BigDecimal? = null

    @JsonView(DokumentView.Database::class)
    var sisteLoennsendringsdato: LocalDate? = null
    var beregnetAntallTimerPrUke: BigDecimal? = null
    var endringsdatoStillingsprosent: LocalDate? = null

    @JsonProperty("fartsomraade")
    var fartsområde: Fartsomraade? = null //"http://nav.no/kodeverk/Kodeverk/Fartsområder"
    var skipsregister: Skipsregister? = null
    var skipstype: Skipstype? = null
    var maritimArbeidsavtale: Boolean? = null
    var beregnetStillingsprosent: BigDecimal? = null
    var antallTimerGammeltAa: BigDecimal? = null

    @JsonIgnore
    override fun getPeriode(): ErPeriode {
        return gyldighetsperiode!!
    }
}

