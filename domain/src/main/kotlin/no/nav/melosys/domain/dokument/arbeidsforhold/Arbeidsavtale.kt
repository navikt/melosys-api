package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.felles.Periode
import java.math.BigDecimal
import java.time.LocalDate

class Arbeidsavtale(
    val arbeidstidsordning: Arbeidstidsordning,
    val avloenningstype: String? = null,
    val yrke: Yrke = Yrke(),
    val gyldighetsperiode: Periode = Periode(),
    val avtaltArbeidstimerPerUke: BigDecimal? = null,
    val stillingsprosent: BigDecimal? = null,
    @JsonView(DokumentView.Database::class) val sisteLoennsendringsdato: LocalDate? = null,
    val beregnetAntallTimerPrUke: BigDecimal? = null,
    val endringsdatoStillingsprosent: LocalDate? = null,
    val skipsregister: Skipsregister? = null,
    val skipstype: Skipstype? = null,
    val maritimArbeidsavtale: Boolean? = null,
    val beregnetStillingsprosent: BigDecimal? = null,
    val antallTimerGammeltAa: BigDecimal? = null,
    val fartsomraade: Fartsomraade? = null
) : HarPeriode {

    @JsonIgnore
    override fun getPeriode(): ErPeriode = gyldighetsperiode
}

