package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.OffsetDateTime


class Arbeidsforhold : HarPeriode {
    var arbeidsforholdID: String? = null
    var arbeidsforholdIDnav: Long = 0
    var ansettelsesPeriode: Periode? = null
    var arbeidsforholdstype: String? = null //"http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper"
    var arbeidsavtaler: List<Arbeidsavtale> = ArrayList()
    var permisjonOgPermittering: List<PermisjonOgPermittering> = ArrayList()
    var utenlandsopphold: List<Utenlandsopphold> = ArrayList()
    var arbeidsgivertype: Aktoertype? = null
    var arbeidsgiverID: String? = null
    var arbeidstakerID: String? = null
    var opplysningspliktigtype: Aktoertype? = null
    var opplysningspliktigID: String? = null

    @JsonProperty("Aordning")
    var arbeidsforholdInnrapportertEtterAOrdningen: Boolean? = null
    var opprettelsestidspunkt: OffsetDateTime? = null
    var sistBekreftet: OffsetDateTime? = null

    @JsonProperty("timerTimelonnet")
    var antallTimerForTimeloennet: List<AntallTimerIPerioden> = ArrayList()

    @JsonIgnore
    override fun getPeriode(): ErPeriode {
        return ansettelsesPeriode!!
    }

    fun hentOrgnumre(): List<String?> {
        val orgnr = ArrayList<String?>()
        if (arbeidsgivertype != Aktoertype.PERSON) {
            orgnr.add(arbeidsgiverID)
        }
        if (opplysningspliktigtype != Aktoertype.PERSON) {
            orgnr.add(opplysningspliktigID)
        }
        return orgnr
    }
}

