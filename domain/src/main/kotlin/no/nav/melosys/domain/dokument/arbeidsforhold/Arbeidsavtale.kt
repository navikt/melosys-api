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

class Arbeidsavtale(var arbeidstidsordning: Arbeidstidsordning) : HarPeriode {
    var avloenningstype: String? = null
    var yrke: Yrke = Yrke()
    var gyldighetsperiode: Periode? = null
    var avtaltArbeidstimerPerUke: BigDecimal? = null
    var stillingsprosent: BigDecimal? = null

    @JsonView(DokumentView.Database::class)
    var sisteLoennsendringsdato: LocalDate? = null
    var beregnetAntallTimerPrUke: BigDecimal? = null
    var endringsdatoStillingsprosent: LocalDate? = null

    @JsonProperty("fartsomraade")
    var fartsområde: Fartsomraade? = null
    var skipsregister: Skipsregister? = null
    var skipstype: Skipstype? = null
    var maritimArbeidsavtale: Boolean? = null
    var beregnetStillingsprosent: BigDecimal? = null
    var antallTimerGammeltAa: BigDecimal? = null

    @JsonIgnore
    override fun getPeriode(): ErPeriode = gyldighetsperiode!!
}

