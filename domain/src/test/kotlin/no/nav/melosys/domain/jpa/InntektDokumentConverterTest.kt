package no.nav.melosys.domain.jpa

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.InntektType
import org.junit.jupiter.api.Test

class InntektDokumentConverterTest {

    @Test
    fun `sjekk at vi kan lese inn med type Inntekt`() {
        val saksopplysningDokument: SaksopplysningDokument = SaksopplysningDokumentConverter().convertToEntityAttribute(inntektDokumentMedTypeInntekt)

        saksopplysningDokument.shouldBeInstanceOf<InntektDokument>()
            .arbeidsInntektMaanedListe.shouldHaveSize(1)
            .first().arbeidsInntektInformasjon.inntektListe.first().type
            .shouldBe(InntektType.Inntekt)
    }

    @Test
    fun `sjekk at vi kan lese inn inntektListe uten kategori i tilleggsinformasjon`() {
        val saksopplysningDokument: SaksopplysningDokument = SaksopplysningDokumentConverter().convertToEntityAttribute(inntektDokumentUtenKategori)

        saksopplysningDokument.shouldBeInstanceOf<InntektDokument>()
            .arbeidsInntektMaanedListe.shouldHaveSize(1)
            .first().arbeidsInntektInformasjon.inntektListe.first().type
            .shouldBe(InntektType.Inntekt)
    }

    private val inntektDokumentMedTypeInntekt = """{
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
          } ]}""".trimIndent()

    private val inntektDokumentUtenKategori = """{
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
          } ]}""".trimIndent()
}
