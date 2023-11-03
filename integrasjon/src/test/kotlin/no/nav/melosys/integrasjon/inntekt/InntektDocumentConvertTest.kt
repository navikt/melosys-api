package no.nav.melosys.integrasjon.inntekt

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer
import no.nav.melosys.domain.jpa.SaksopplysningDokumentConverter
import no.nav.melosys.domain.jpa.mixin.SaksopplysningDokumentMixIn
import no.nav.melosys.domain.jpa.mixin.TilleggsinformasjonDetaljerMixIn
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class InntektDocumentConvertTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `sjekk json resulat fra konvertering for lagring i databaase mot fasit fra fil`() {
        val inntektResponse = mapper.readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))
        val forventetInntektDokumentDatabaseJson = hentRessurs("mock/inntekt/InntektDocumentConverterResult.json")


        val inntektDokument = InntektKonverter().lagSaksopplysning(inntektResponse).dokument.shouldBeTypeOf<InntektDokument>()
        val convertToDatabaseColumn = SaksopplysningDokumentConverter().convertToDatabaseColumn(inntektDokument)


        convertToDatabaseColumn.shouldEqualJson(forventetInntektDokumentDatabaseJson)
    }

    @Test
    fun `sjekk json resulat fra konvertering for frontend mot fasit fra fil`() {
        val inntektResponse = mapper.readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))
        val forventetInntektDokumentrFrontEndJson = hentRessurs("mock/inntekt/InntektDocumentConverterFrontEndResult.json")
        val mapperWithView = mapper
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .addMixIn(SaksopplysningDokument::class.java, SaksopplysningDokumentMixIn::class.java)
            .addMixIn(TilleggsinformasjonDetaljer::class.java, TilleggsinformasjonDetaljerMixIn::class.java)
            .writerWithView(DokumentView.FrontendApi::class.java)


        val inntektDokument = InntektKonverter().lagSaksopplysning(inntektResponse).dokument.shouldBeTypeOf<InntektDokument>()
        val frontendExpectedDTOs = mapperWithView.writeValueAsString(inntektDokument)


        frontendExpectedDTOs.shouldEqualJson(forventetInntektDokumentrFrontEndJson)
    }

    fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
