package no.nav.melosys.integrasjon.inntk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.integrasjon.inntk.inntekt.*
import org.springframework.stereotype.Service
import java.time.YearMonth

private val log = KotlinLogging.logger { }

@Service
class InntektRestService(
    private val inntektRestConsumer: InntektRestConsumer
) : InntektFasade {
    private val inntektKonverter = InntektKonverter()

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }

    override fun hentInntektListe(personID: String, fom: YearMonth, tom: YearMonth): Saksopplysning {
        val inntekt = hentInntekt(personID, fom, tom)
        return inntektKonverter.lagSaksopplysning(inntekt).apply {
            leggTilKildesystemOgMottattDokument(
                SaksopplysningKildesystem.INNTK, inntekt.tilJsonString()
            )
            type = SaksopplysningType.INNTK
            versjon = INNTEKT_VERSJON
        }
    }

    private fun hentInntekt(personID: String, fom: YearMonth, tom: YearMonth): InntektResponse {
        if (fom.isBefore(JANUAR_2015) && tom.isBefore(JANUAR_2015)) {
            log.info("Hele perioden($fom -> $tom) er fra før $JANUAR_2015 som inntektskomponenten ikke støtter. Lager en tom respons")
            return InntektResponse(ident = Aktoer(personID, AktoerType.AKTOER_ID))
        }
        return inntektRestConsumer.hentInntektListe(
            InntektRequest(
                ainntektsfilter = AINNTEKTSFILTER,
                formaal = FORMAAL,
                ident = Aktoer(personID, AktoerType.AKTOER_ID),
                maanedFom = begrensFom(fom),
                maanedTom = tom
            )
        )
    }

    private fun begrensFom(fom: YearMonth): YearMonth =
        if (fom.isBefore(JANUAR_2015)) {
            log.info("Periode har fom dato $fom som inntektskomponent ikke støtter, henter inntekt med fom $JANUAR_2015")
            JANUAR_2015
        } else fom

    companion object {
        private val JANUAR_2015 = YearMonth.of(2015, 1)
        const val INNTEKT_VERSJON = "REST 1.0"
        const val AINNTEKTSFILTER = "MedlemskapA-inntekt"
        const val FORMAAL = "Medlemskap"
    }
}
