package no.nav.melosys.integrasjon.inntekt

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.InntektType
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer
import no.nav.melosys.domain.jpa.SaksopplysningDokumentConverter
import no.nav.melosys.domain.jpa.mixin.SaksopplysningDokumentMixIn
import no.nav.melosys.domain.jpa.mixin.TilleggsinformasjonDetaljerMixIn
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class InntektDokumentConverterTest {

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

    @Test
    fun `sjekk at vi kan lese inn med type Inntekt`() {
        val saksopplysningDokument: SaksopplysningDokument =
            SaksopplysningDokumentConverter().convertToEntityAttribute(INNTEKTDOKUMENT_MED_TYPEI_NNTEKT)


        saksopplysningDokument.shouldBeInstanceOf<InntektDokument>()
            .arbeidsInntektMaanedListe.shouldHaveSize(1)
            .first().arbeidsInntektInformasjon.inntektListe.first().type
            .shouldBe(InntektType.Inntekt)
    }

    @Test
    fun `sjekk at vi kan lese inn inntektListe uten kategori i tilleggsinformasjon`() {
        val saksopplysningDokument: SaksopplysningDokument =
            SaksopplysningDokumentConverter().convertToEntityAttribute(INNTEKT_DOKUMENT_UTEN_KATEGORI)


        saksopplysningDokument.shouldBeInstanceOf<InntektDokument>()
            .arbeidsInntektMaanedListe.shouldHaveSize(1)
            .first().arbeidsInntektInformasjon.inntektListe.first().type
            .shouldBe(InntektType.Inntekt)
    }


    fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")

    companion object {
        private const val INNTEKTDOKUMENT_MED_TYPEI_NNTEKT = """{
          "type" : "InntektDokument",
          "arbeidsInntektMaanedListe" : [ {
            "arbeidsInntektInformasjon" : {
              "inntektListe" : [ {
                "type" : "Inntekt",
                "beloep" : 50000,
                "fordel" : "kontantytelse",
                "inntektskilde" : "A-ordningen",
                "inntektsperiodetype" : "Maaned",
                "inntektsstatus" : "LoependeInnrapportert",
                "utbetaltIPeriode" : [ 2022, 1 ]
              } ]
            }
          } ]}"""

        private const val INNTEKT_DOKUMENT_UTEN_KATEGORI = """{
          "type" : "InntektDokument",
          "arbeidsInntektMaanedListe" : [ {
            "arbeidsInntektInformasjon" : {
              "inntektListe" : [ {
                "type" : "Inntekt",
                "beloep" : 50000,
                "fordel" : "kontantytelse",
                "inntektskilde" : "A-ordningen",
                "inntektsperiodetype" : "Maaned",
                "inntektsstatus" : "LoependeInnrapportert",
                "utbetaltIPeriode" : [ 2022, 1 ],
                    "tilleggsinformasjon" : {
                }
              } ]
            }
          } ]}"""
    }
}
