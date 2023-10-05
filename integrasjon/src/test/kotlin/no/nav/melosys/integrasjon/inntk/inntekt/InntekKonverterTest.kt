package no.nav.melosys.integrasjon.inntk.inntekt

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Loennsinntekt
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Naeringsinntekt
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.BonusFraForsvaret
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.ReiseKostOgLosji
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Svalbardinntekt
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntekKonverterTest {

    @Test
    fun test() {
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
                        .first().shouldBeInstanceOf<Loennsinntekt>().run {
                            beloep.shouldBe(BigDecimal(50000))
                            opptjeningsperiode.shouldNotBeNull()
                                .run {
                                    fom.shouldBe(LocalDate.of(2022, 1, 1))
                                    tom.shouldBe(LocalDate.of(2022, 1, 10))
                                }
                            antall.shouldBe(1)
                            tilleggsinformasjon
                                .tilleggsinformasjonDetaljer
                                .shouldBeTypeOf<BonusFraForsvaret>()
                                .åretUtbetalingenGjelderFor.shouldBe(Year.of(1980))
                        }
                }

                get(1).run {
                    aarMaaned.shouldBe(YearMonth.parse("2022-02"))
                    arbeidsInntektInformasjon.inntektListe.shouldHaveSize(1)
                        .first().shouldBeInstanceOf<YtelseFraOffentlige>().run {
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
                        .first().shouldBeInstanceOf<Naeringsinntekt>().run {
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

    fun hentRessurs(fil: String): String = InntekKonverterTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}



