package no.nav.melosys.integrasjon.inntk.inntekt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer
import no.nav.melosys.domain.jpa.SaksopplysningDokumentConverter
import no.nav.melosys.domain.jpa.mixin.InntektMixin
import no.nav.melosys.domain.jpa.mixin.SaksopplysningDokumentMixIn
import no.nav.melosys.domain.jpa.mixin.TilleggsinformasjonDetaljerMixIn
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class InntektDocumentConvertTest {

    @Test
    fun `sjekk json resulat fra konvertering for lagring i databaase mot fasit fra fil`() {
        val inntektResponse = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))

        val saksopplysning = InntektKonverter().lagSaksopplysning(inntektResponse)
        val convertToDatabaseColumn = SaksopplysningDokumentConverterForTest().convertToDatabaseColumn(saksopplysning.dokument)

        convertToDatabaseColumn.trim().shouldBe(hentRessurs("mock/inntekt/InntektDocumentConverterResult.json").trim())
    }

    @Test
    fun `sjekk json resulat fra konvertering for frontend mot fasit fra fil`() {
        val inntektResponse = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))

        val saksopplysning = InntektKonverter().lagSaksopplysning(inntektResponse)

        val frontendExpectedDTOs = jacksonObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .addMixIn(Inntekt::class.java, InntektMixin::class.java)
            .addMixIn(SaksopplysningDokument::class.java, SaksopplysningDokumentMixIn::class.java)
            .addMixIn(TilleggsinformasjonDetaljer::class.java, TilleggsinformasjonDetaljerMixIn::class.java)
            .registerModule(JavaTimeModule())
            .writerWithView(DokumentView.FrontendApi::class.java)
            .writeValueAsString(saksopplysning.dokument)

        val frontendResultDTOs = jacksonObjectMapper()
            .readTree(hentRessurs("mock/inntekt/InntektDocumentConverterResult.json")).let {
                it["arbeidsInntektMaanedListe"].forEach { node ->
                    (node as ObjectNode).remove("avvikListe")
                    (node["arbeidsInntektInformasjon"]["inntektListe"]
                        .first() as ObjectNode).remove("antall")
                }
                it.toPrettyString()
            }


        frontendResultDTOs.trim().shouldBe(frontendExpectedDTOs.trim())
    }


    fun hentRessurs(fil: String): String = InntekKonverterTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")

    class SaksopplysningDokumentConverterForTest : SaksopplysningDokumentConverter() {
        override fun getObjectMapper(): ObjectMapper {
            return super.getObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true)
        }
    }
}
