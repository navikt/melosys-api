package no.nav.melosys.integrasjon.inntk.inntekt

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.BonusFraForsvaret
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.ReiseKostOgLosji
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Svalbardinntekt
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntekKonverterTest {

    @Test
    fun `skal kunne konverter valid inntekt response`() {
        val inntektResponse = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))

        val saksopplysning = InntektKonverter().lagSaksopplysning(inntektResponse)

        saksopplysning.dokument
            .shouldNotBeNull()
            .shouldBeTypeOf<InntektDokument>()
            .arbeidsInntektMaanedListe
            .shouldHaveSize(3)
            .toList()
            .run {
                get(0).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-01"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            beloep.shouldBe(BigDecimal(50000))
                            tilleggsinformasjon
                                .tilleggsinformasjonDetaljer
                                .shouldBeTypeOf<BonusFraForsvaret>()
                                .åretUtbetalingenGjelderFor.shouldBe(Year.of(1980))
                        }
                }

                get(1).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-02"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            beloep.shouldBe(BigDecimal(50000))
                            tilleggsinformasjon
                                .tilleggsinformasjonDetaljer
                                .shouldBeTypeOf<ReiseKostOgLosji>()
                                .persontype.shouldBe("norskPendler")
                        }
                }
                get(2).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-03"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            beloep.shouldBe(BigDecimal(50000))
                            tilleggsinformasjon
                                .tilleggsinformasjonDetaljer
                                .shouldBeTypeOf<Svalbardinntekt>().run {
                                    antallDager.shouldBe(40)
                                    betaltTrygdeavgift.shouldBe(BigDecimal(50000))
                                }


                        }
                }
            }

    }

    @Test
    fun `skal feil om domene objekter får null hvor de er annotert med @NotNull`() {

        val aktoer = Aktoer("123456789", AktoerType.AKTOER_ID)
        val inntektResponse = InntektResponse(
            lagArbeidsInntektMaaned(
                YearMonth.of(2022, 1),
                YearMonth.of(2022, 2),
                aktoer.identifikator
            ), aktoer
        )
        val saksopplysning = InntektKonverter().lagSaksopplysning(inntektResponse)

        saksopplysning.dokument
            .shouldNotBeNull()
            .shouldBeTypeOf<InntektDokument>()
            .arbeidsInntektMaanedListe
            .shouldHaveSize(2)
            .toList()

    }

    private fun lagArbeidsInntektMaaned(
        fom: YearMonth,
        tom: YearMonth,
        identifikator: String
    ): List<InntektResponse.ArbeidsInntektMaaned> = List(ChronoUnit.MONTHS.between(fom, tom).toInt() + 1) {
        fom.plusMonths(it.toLong())
    }.map { yearMonth ->
        InntektResponse.ArbeidsInntektMaaned(
            aarMaaned = yearMonth,
            arbeidsInntektInformasjon = InntektResponse.ArbeidsInntektInformasjon(
                arbeidsforholdListe = listOf(InntektResponse.ArbeidsforholdFrilanser()),
                inntektListe = listOf(
                    InntektResponse.Inntekt(
                        inntektType = InntektResponse.InntektType.LOENNSINNTEKT,
                        beloep = BigDecimal(50000),
                        fordel = "kontantytelse",
                        inntektskilde = "A-ordningen",
                        inntektsperiodetype = "Maaned",
                        inntektsstatus = "LoependeInnrapportert",
                        leveringstidspunkt = YearMonth.now(),
                        utbetaltIMaaned = yearMonth,
                        opplysningspliktig = Aktoer("974761076", AktoerType.ORGANISASJON),
                        virksomhet = Aktoer("888888888", AktoerType.ORGANISASJON),
                        inntektsmottaker = Aktoer(identifikator, AktoerType.AKTOER_ID),
                        inngaarIGrunnlagForTrekk = true,
                        utloeserArbeidsgiveravgift = true,
                        informasjonsstatus = "InngaarAlltid",
                        beskrivelse = "fastloenn",
                        tilleggsinformasjon = InntektResponse.Tilleggsinformasjon(
                            kategori = "bla",
                            tilleggsinformasjonDetaljer = InntektResponse.BonusFraForsvaret(
                                aaretUtbetalingenGjelderFor = Year.of(1980)
                            )
                        )
                    )
                )
            )
        )
    }



    fun hentRessurs(fil: String): String = InntekKonverterTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}



