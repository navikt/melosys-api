package no.nav.melosys.integrasjon.inntekt

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.InntektType
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.BonusFraForsvaret
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.ReiseKostOgLosji
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Svalbardinntekt
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektKonverterTest {

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
            .shouldHaveSize(4)
            .toList()
            .run {
                get(0).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-01"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            type.shouldBe(InntektType.Loennsinntekt)
                            beloep.shouldBe(BigDecimal(50000))
                            opptjeningsperiode.shouldNotBeNull()
                                .run {
                                    fom.shouldBe(LocalDate.of(2022, 1, 1))
                                    tom.shouldBe(LocalDate.of(2022, 1, 10))
                                }
                            antall.shouldBe(1)
                            tilleggsinformasjon.shouldNotBeNull().apply {
                                kategori.shouldBe("bla")
                                tilleggsinformasjonDetaljer.shouldBeNull()
                            }
                        }
                }

                get(1).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-02"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            type.shouldBe(InntektType.YtelseFraOffentlige)
                            beloep.shouldBe(BigDecimal(50000))
                            tilleggsinformasjon
                                ?.tilleggsinformasjonDetaljer
                                .shouldBeTypeOf<ReiseKostOgLosji>()
                                .persontype.shouldBe("norskPendler")
                        }
                }
                get(2).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-03"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            type.shouldBe(InntektType.Naeringsinntekt)
                            beloep.shouldBe(BigDecimal(50000))
                            tilleggsinformasjon
                                ?.tilleggsinformasjonDetaljer
                                .shouldBeTypeOf<Svalbardinntekt>().run {
                                    antallDager.shouldBe(40)
                                    betaltTrygdeavgift.shouldBe(BigDecimal(50000))
                                }


                        }
                }
                get(3).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-04"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().run {
                            type.shouldBe(InntektType.Naeringsinntekt)
                            tilleggsinformasjon.shouldBeNull()
                        }
                }
            }
    }

    @Test
    fun `skal feil om beskrivelse er null`() {
        val aktoer = Aktoer("123456789", AktoerType.AKTOER_ID)
        val inntektResponse = InntektResponse(
            lagArbeidsInntektMaaned(
                YearMonth.of(2022, 1),
                YearMonth.of(2022, 1),
                TestDataInntekt(
                    beskrivelse = null
                ),
            ), aktoer
        )
        shouldThrow<TekniskException> {
            InntektKonverter().lagSaksopplysning(inntektResponse)
        }
    }

    data class TestDataInntekt(
        var inntektType: InntektResponse.InntektType? = InntektResponse.InntektType.LOENNSINNTEKT,
        var beloep: BigDecimal = BigDecimal(50000),
        var fordel: String = "kontantytelse",
        var inntektskilde: String = "A-ordningen",
        var inntektsperiodetype: String = "Maaned",
        var inntektsstatus: String = "LoependeInnrapportert",
        var leveringstidspunkt: YearMonth = YearMonth.now(),
        var opptjeningsland: String? = null,
        var skattemessigBosattLand: String? = null,
        var opplysningspliktig: Aktoer? = Aktoer("974761076", AktoerType.ORGANISASJON),
        var virksomhet: Aktoer? = Aktoer("888888888", AktoerType.ORGANISASJON),
        var inntektsmottaker: Aktoer? = Aktoer("123456789", AktoerType.AKTOER_ID),
        var inngaarIGrunnlagForTrekk: Boolean? = true,
        var utloeserArbeidsgiveravgift: Boolean? = true,
        var informasjonsstatus: String? = "InngaarAlltid",
        var beskrivelse: String? = "fastloenn",
        var skatteOgAvgiftsregel: String? = null,
        var antall: Int? = null,
        var tilleggsinformasjon: InntektResponse.Tilleggsinformasjon? = InntektResponse.Tilleggsinformasjon(
            kategori = "bla",
            tilleggsinformasjonDetaljer = InntektResponse.BonusFraForsvaret(
                aaretUtbetalingenGjelderFor = Year.of(1980)
            )
        )
    )

    private fun lagArbeidsInntektMaaned(
        fom: YearMonth,
        tom: YearMonth,
        inntekt: TestDataInntekt = TestDataInntekt()
    ): List<InntektResponse.ArbeidsInntektMaaned> = List(ChronoUnit.MONTHS.between(fom, tom).toInt() + 1) {
        fom.plusMonths(it.toLong())
    }.map { yearMonth ->
        InntektResponse.ArbeidsInntektMaaned(
            aarMaaned = yearMonth,
            arbeidsInntektInformasjon = InntektResponse.ArbeidsInntektInformasjon(
                arbeidsforholdListe = listOf(InntektResponse.ArbeidsforholdFrilanser()),
                inntektListe = listOf(
                    InntektResponse.Inntekt(
                        inntektType = inntekt.inntektType!!,
                        beloep = inntekt.beloep,
                        fordel = inntekt.fordel,
                        inntektskilde = inntekt.inntektskilde,
                        inntektsperiodetype = inntekt.inntektsperiodetype,
                        inntektsstatus = inntekt.inntektsstatus,
                        leveringstidspunkt = inntekt.leveringstidspunkt,
                        utbetaltIMaaned = yearMonth,
                        opplysningspliktig = inntekt.opplysningspliktig,
                        virksomhet = inntekt.virksomhet,
                        inntektsmottaker = inntekt.inntektsmottaker,
                        inngaarIGrunnlagForTrekk = inntekt.inngaarIGrunnlagForTrekk,
                        utloeserArbeidsgiveravgift = inntekt.utloeserArbeidsgiveravgift,
                        informasjonsstatus = inntekt.informasjonsstatus,
                        beskrivelse = inntekt.beskrivelse,
                        tilleggsinformasjon = inntekt.tilleggsinformasjon,
                        skattemessigBosattLand = inntekt.skattemessigBosattLand,
                    )
                )
            )
        )
    }

    fun hentRessurs(fil: String): String = InntektKonverterTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
