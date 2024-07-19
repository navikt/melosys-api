package no.nav.melosys.integrasjon.aareg

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdKonverter
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdQuery
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AaregService(
    private val arbeidsforholdConsumer: ArbeidsforholdConsumer,
    private val kodeOppslag: KodeOppslag
) : AaregFasade {
    override fun finnArbeidsforholdPrArbeidstaker(ident: String, fom: LocalDate?, tom: LocalDate?): Saksopplysning {
        val arbeidsforholdQuery = ArbeidsforholdQuery(
            regelverk = ArbeidsforholdQuery.Regelverk.A_ORDNINGEN,
            arbeidsforholdType = ArbeidsforholdQuery.ArbeidsforholdType.ALLE,
            ansettelsesperiodeFom = fom,
            ansettelsesperiodeTom = tom
        )

        val response = arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(ident, arbeidsforholdQuery)

        return ArbeidsforholdKonverter(response, kodeOppslag).createSaksopplysning().apply {
            leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.AAREG, response.tilSaksopplysning())
            type = SaksopplysningType.ARBFORH
            versjon = ARBEIDSFORHOLD_REST_VERSJON
        }
    }

    companion object {
        private const val ARBEIDSFORHOLD_REST_VERSJON = "REST 1.0"
    }
}
