package no.nav.melosys.integrasjon.aareg

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdKonverter
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdQuery
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AaregService(
    private val arbeidsforholdRestConsumer: ArbeidsforholdRestConsumer,
    private val kodeOppslag: KodeOppslag
) : AaregFasade {
    override fun finnArbeidsforholdPrArbeidstaker(ident: String, fom: LocalDate?, tom: LocalDate?): Saksopplysning {
        return finnArbeidsforholdPrArbeidstakerRest(ident, fom, tom)
    }

    private fun finnArbeidsforholdPrArbeidstakerRest(ident: String, fom: LocalDate?, tom: LocalDate?): Saksopplysning {
        val arbeidsforholdQuery = ArbeidsforholdQuery(
            regelverk = ArbeidsforholdQuery.Regelverk.A_ORDNINGEN,
            arbeidsforholdType = ArbeidsforholdQuery.ArbeidsforholdType.ALLE,
            ansettelsesperiodeFom = fom,
            ansettelsesperiodeTom = tom
        )

        val response = arbeidsforholdRestConsumer.finnArbeidsforholdPrArbeidstaker(ident, arbeidsforholdQuery)
        val arbeidsforholdKonverter = ArbeidsforholdKonverter(response, kodeOppslag)

        val saksopplysning = arbeidsforholdKonverter.createSaksopplysning()
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.AAREG, response.tilSaksopplysning()
        )
        saksopplysning.type = SaksopplysningType.ARBFORH
        saksopplysning.versjon = ARBEIDSFORHOLD_REST_VERSJON

        return saksopplysning
    }

    companion object {
        private const val ARBEIDSFORHOLD_REST_VERSJON = "REST 1.0"
    }
}
