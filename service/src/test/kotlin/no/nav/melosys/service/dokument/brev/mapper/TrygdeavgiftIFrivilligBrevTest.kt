package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel.MINSTEBELØP
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel.ORDINÆR
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
import no.nav.melosys.domain.avgift.Avgiftsdel
import no.nav.melosys.domain.avgift.Avgiftsdel.HELSE
import no.nav.melosys.domain.avgift.Avgiftsdel.PENSJON
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
import no.nav.melosys.domain.kodeverk.Inntektskildetype.INNTEKT_FRA_UTLANDET
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftPerAvgiftsdelDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftssatsDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.ÅrsinntektDto
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TrygdeavgiftIFrivilligBrevTest {

    private val medlemskapsperiode = medlemskapsperiodeForTest {
        fom = LocalDate.of(2026, 1, 1)
        tom = LocalDate.of(2027, 12, 31)
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
    }

    @Test
    fun `bare ordinær sats viser ordinær beregningstabell`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                periode(2026, ORDINÆR, sats = "26.2", beløp = 3000),
                periode(2027, ORDINÆR, sats = "26.2", beløp = 3000),
            )
        )

        trygdeavgift.visOrdinærBeregningstabell shouldBe true
        trygdeavgift.tjuefemProsentRegel.shouldBeNull()
        trygdeavgift.ordinærSatsPåfølgendeÅr.shouldBeNull()
        trygdeavgift.årsinntektUnderMinstebeløp.shouldBeEmpty()
    }

    @Test
    fun `25 prosent-regelen for pensjonsdelen i år og ordinær sats neste år`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                periode(2026, ORDINÆR, sats = "6.8", beløp = 1000, avgiftsdel = HELSE, inntektskildetype = ARBEIDSINNTEKT_FRA_NORGE),
                periode(2026, ORDINÆR, sats = "9.1", beløp = 500, avgiftsdel = HELSE, inntektskildetype = INNTEKT_FRA_UTLANDET),
                periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 700, avgiftsdel = PENSJON),
                periode(2027, ORDINÆR, sats = "26.2", beløp = 3000, inntektskildetype = ARBEIDSINNTEKT_FRA_NORGE),
            )
        )

        trygdeavgift.visOrdinærBeregningstabell shouldBe false
        trygdeavgift.tjuefemProsentRegel.shouldNotBeNull().run {
            avgiftssatser.shouldContainExactly(
                sats(2026, "6.8", ORDINÆR, ARBEIDSINNTEKT_FRA_NORGE, HELSE),
                sats(2026, "9.1", ORDINÆR, INNTEKT_FRA_UTLANDET, HELSE),
                sats(2026, null, TJUEFEM_PROSENT_REGEL, INNTEKT_FRA_UTLANDET, PENSJON),
            )
            avgiftPerAvgiftsdel.shouldContainExactly(
                AvgiftPerAvgiftsdelDto(2026, PENSJON, TJUEFEM_PROSENT_REGEL, BigDecimal(700)),
                AvgiftPerAvgiftsdelDto(2026, HELSE, ORDINÆR, BigDecimal(1500)),
            )
        }
        trygdeavgift.ordinærSatsPåfølgendeÅr.shouldNotBeNull().run {
            etterÅr shouldBe 2026
            år shouldBe 2027
            avgiftssatser.shouldContainExactly(sats(2027, "26.2", ORDINÆR, ARBEIDSINNTEKT_FRA_NORGE, null))
        }
    }

    @Test
    fun `25 prosent-regelen begge år gir ingen tabell for påfølgende år`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 700, avgiftsdel = PENSJON),
                periode(2027, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 800, avgiftsdel = PENSJON),
            )
        )

        trygdeavgift.ordinærSatsPåfølgendeÅr.shouldBeNull()
        trygdeavgift.tjuefemProsentRegel.shouldNotBeNull().avgiftPerAvgiftsdel.map { it.år } shouldBe listOf(2026, 2027)
    }

    @Test
    fun `avgift per måned for del med ordinær sats er gjennomsnittet når avgiften endrer seg i løpet av året`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                periode(2026, ORDINÆR, sats = "6.8", beløp = 1000, avgiftsdel = HELSE, fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 6, 30)),
                periode(2026, ORDINÆR, sats = "6.8", beløp = 2000, avgiftsdel = HELSE, fom = LocalDate.of(2026, 7, 1), tom = LocalDate.of(2026, 12, 31)),
                periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 700, avgiftsdel = PENSJON),
            )
        )

        val tjuefemProsentRegel = trygdeavgift.tjuefemProsentRegel.shouldNotBeNull()
        tjuefemProsentRegel.avgiftPerAvgiftsdel.first { it.avgiftsdel == HELSE }.avgiftPerMd shouldBe BigDecimal(1500)
        tjuefemProsentRegel.avgiftssatser.shouldContainExactly(
            sats(2026, "6.8", ORDINÆR, INNTEKT_FRA_UTLANDET, HELSE),
            sats(2026, null, TJUEFEM_PROSENT_REGEL, INNTEKT_FRA_UTLANDET, PENSJON),
        )
    }

    @Test
    fun `inntekt under minstebeløpet i år og ordinær sats neste år`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                periode(2026, MINSTEBELØP, sats = null, beløp = 0, månedsinntekt = 5000),
                periode(2027, ORDINÆR, sats = "26.2", beløp = 3000),
            )
        )

        trygdeavgift.visOrdinærBeregningstabell shouldBe false
        trygdeavgift.tjuefemProsentRegel.shouldBeNull()
        trygdeavgift.beregningsregelIÅr shouldBe MINSTEBELØP
        trygdeavgift.beregningsregelNesteÅr shouldBe ORDINÆR
        trygdeavgift.årsinntektUnderMinstebeløp.shouldContainExactly(ÅrsinntektDto(2026, BigDecimal(60000)))
        trygdeavgift.ordinærSatsPåfølgendeÅr.shouldNotBeNull().run {
            etterÅr shouldBe 2026
            år shouldBe 2027
        }
    }

    @Test
    fun `bare inntekt under minstebeløpet i år gir ingen regel for neste år`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(listOf(periode(2026, MINSTEBELØP, sats = null, beløp = 0)))

        trygdeavgift.beregningsregelIÅr shouldBe MINSTEBELØP
        trygdeavgift.beregningsregelNesteÅr.shouldBeNull()
        trygdeavgift.visOrdinærBeregningstabell shouldBe false
        trygdeavgift.tjuefemProsentRegel.shouldBeNull()
        trygdeavgift.ordinærSatsPåfølgendeÅr.shouldBeNull()
    }

    @Test
    fun `årsinntekt under minstebeløpet holder utenfor inntekt der arbeidsgiveravgift betales til Skatteetaten`() {
        val minstebeløpsperiode = periode(2026, MINSTEBELØP, sats = null, beløp = 0).apply {
            leggTilGrunnlag(grunnlag(this, INNTEKT_FRA_UTLANDET, Skatteplikttype.IKKE_SKATTEPLIKTIG, arbeidsgiveravgiftTilSkatt = false))
            leggTilGrunnlag(grunnlag(this, ARBEIDSINNTEKT_FRA_NORGE, Skatteplikttype.SKATTEPLIKTIG, arbeidsgiveravgiftTilSkatt = true))
            leggTilGrunnlag(grunnlag(this, Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE, Skatteplikttype.SKATTEPLIKTIG, arbeidsgiveravgiftTilSkatt = true))
        }

        TrygdeavgiftIFrivilligBrev(listOf(minstebeløpsperiode)).årsinntektUnderMinstebeløp
            .shouldContainExactly(ÅrsinntektDto(2026, BigDecimal(120000)))
    }

    @Test
    fun `årsinntekt under minstebeløpet teller inntekt bare én gang når helse- og pensjonsdelen har ulik periode`() {
        val kunPensjon = medlemskapsperiodeForTest {
            fom = LocalDate.of(2026, 6, 1)
            tom = LocalDate.of(2026, 10, 20)
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        }
        val helseOgPensjon = medlemskapsperiodeForTest {
            fom = LocalDate.of(2026, 10, 21)
            tom = LocalDate.of(2026, 12, 31)
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        }
        val skatteforhold = skatteforholdForTest {
            fomDato = LocalDate.of(2026, 6, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
        val inntekter = listOf(ARBEIDSINNTEKT_FRA_NORGE to 10000.0, INNTEKT_FRA_UTLANDET to 1000.0).map { (type, beløp) ->
            inntektForTest {
                fomDato = LocalDate.of(2026, 6, 1)
                tomDato = LocalDate.of(2026, 12, 31)
                this.type = type
                avgiftspliktigMndInntekt = Penger(beløp)
                arbeidsgiversavgiftBetalesTilSkatt = type == ARBEIDSINNTEKT_FRA_NORGE
            }
        }
        fun minstebeløpsperiode(avgiftsdel: Avgiftsdel, fom: LocalDate, medlemskapsperioder: List<Medlemskapsperiode>) =
            periode(2026, MINSTEBELØP, sats = null, beløp = 0, avgiftsdel = avgiftsdel, fom = fom).apply {
                medlemskapsperioder.forEach { medlemskapsperiode ->
                    inntekter.forEach { inntekt ->
                        leggTilGrunnlag(
                            TrygdeavgiftsperiodeGrunnlag(
                                trygdeavgiftsperiode = this,
                                medlemskapsperiode = medlemskapsperiode,
                                inntektsperiode = inntekt,
                                skatteforhold = skatteforhold,
                            )
                        )
                    }
                }
            }

        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                minstebeløpsperiode(PENSJON, LocalDate.of(2026, 6, 1), listOf(kunPensjon, helseOgPensjon)),
                minstebeløpsperiode(HELSE, LocalDate.of(2026, 10, 21), listOf(helseOgPensjon)),
            )
        )

        // 11 000 kr/mnd i 7 måneder. Inntekten 21.10–31.12 ligger i grunnlaget til begge delene.
        trygdeavgift.årsinntektUnderMinstebeløp.shouldContainExactly(ÅrsinntektDto(2026, BigDecimal(77000)))
    }

    @Test
    fun `25 prosent-regelen viser inntektskilden når det er én, og markerer sammenslåtte inntektskilder`() {
        val énInntektskilde = periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 700, avgiftsdel = HELSE).apply {
            leggTilGrunnlag(grunnlag(this, ARBEIDSINNTEKT_FRA_NORGE, Skatteplikttype.IKKE_SKATTEPLIKTIG, arbeidsgiveravgiftTilSkatt = true))
        }
        val flereInntektskilder = periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 500, avgiftsdel = PENSJON).apply {
            leggTilGrunnlag(grunnlag(this, ARBEIDSINNTEKT_FRA_NORGE, Skatteplikttype.IKKE_SKATTEPLIKTIG, arbeidsgiveravgiftTilSkatt = true))
            leggTilGrunnlag(grunnlag(this, INNTEKT_FRA_UTLANDET, Skatteplikttype.IKKE_SKATTEPLIKTIG, arbeidsgiveravgiftTilSkatt = false))
        }

        TrygdeavgiftIFrivilligBrev(listOf(énInntektskilde, flereInntektskilder)).tjuefemProsentRegel.shouldNotBeNull()
            .avgiftssatser.shouldContainExactly(
                sats(2026, null, TJUEFEM_PROSENT_REGEL, ARBEIDSINNTEKT_FRA_NORGE, HELSE),
                sats(2026, null, TJUEFEM_PROSENT_REGEL, null, PENSJON, harSammenslåtteInntektskilder = true),
            )
    }

    @Test
    fun `år med ordinær sats før år med 25 prosent-regelen vises i tabellen for 25 prosent-regelen`() {
        val trygdeavgift = TrygdeavgiftIFrivilligBrev(
            listOf(
                periode(2026, ORDINÆR, sats = "26.2", beløp = 3000),
                periode(2027, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 800, avgiftsdel = PENSJON),
            )
        )

        trygdeavgift.visOrdinærBeregningstabell shouldBe false
        trygdeavgift.ordinærSatsPåfølgendeÅr.shouldBeNull()
        trygdeavgift.tjuefemProsentRegel.shouldNotBeNull().run {
            avgiftssatser.map { it.år } shouldBe listOf(2026, 2027)
            avgiftPerAvgiftsdel.map { it.år } shouldBe listOf(2027)
        }
    }

    @Test
    fun `harUlikBeregningsregelPerÅr er sann bare når årene beregnes etter ulik regel`() {
        TrygdeavgiftIFrivilligBrev.harUlikBeregningsregelPerÅr(
            listOf(
                periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 700, avgiftsdel = PENSJON),
                periode(2026, ORDINÆR, sats = "6.8", beløp = 1000, avgiftsdel = HELSE),
                periode(2027, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 800, avgiftsdel = PENSJON),
            )
        ) shouldBe false
        TrygdeavgiftIFrivilligBrev.harUlikBeregningsregelPerÅr(
            listOf(periode(2026, MINSTEBELØP, sats = null, beløp = 0), periode(2027, MINSTEBELØP, sats = null, beløp = 0))
        ) shouldBe false
        TrygdeavgiftIFrivilligBrev.harUlikBeregningsregelPerÅr(
            listOf(periode(2026, ORDINÆR, sats = "26.2", beløp = 3000), periode(2027, ORDINÆR, sats = "26.2", beløp = 3000))
        ) shouldBe false
        TrygdeavgiftIFrivilligBrev.harUlikBeregningsregelPerÅr(
            listOf(periode(2026, MINSTEBELØP, sats = null, beløp = 0), periode(2027, ORDINÆR, sats = "26.2", beløp = 3000))
        ) shouldBe true
        TrygdeavgiftIFrivilligBrev.harUlikBeregningsregelPerÅr(
            listOf(
                periode(2026, TJUEFEM_PROSENT_REGEL, sats = null, beløp = 700, avgiftsdel = PENSJON),
                periode(2027, ORDINÆR, sats = "26.2", beløp = 3000),
            )
        ) shouldBe true
    }

    private fun periode(
        år: Int,
        beregningsregel: Avgiftsberegningsregel,
        sats: String?,
        beløp: Int,
        avgiftsdel: Avgiftsdel? = null,
        inntektskildetype: Inntektskildetype = INNTEKT_FRA_UTLANDET,
        månedsinntekt: Int = 50000,
        fom: LocalDate = LocalDate.of(år, 1, 1),
        tom: LocalDate = LocalDate.of(år, 12, 31),
    ): Trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
        periodeFra = fom
        periodeTil = tom
        trygdesats = sats?.let(::BigDecimal)
        trygdeavgiftsbeløpMd = BigDecimal(beløp)
        this.beregningsregel = beregningsregel
        this.avgiftsdel = avgiftsdel
        medlemskapsperiode = this@TrygdeavgiftIFrivilligBrevTest.medlemskapsperiode
        grunnlagInntekstperiode {
            type = inntektskildetype
            avgiftspliktigMndInntekt = Penger(månedsinntekt.toDouble())
        }
    }

    private fun grunnlag(
        trygdeavgiftsperiode: Trygdeavgiftsperiode,
        inntektskildetype: Inntektskildetype,
        skatteplikttype: Skatteplikttype,
        arbeidsgiveravgiftTilSkatt: Boolean,
    ) = TrygdeavgiftsperiodeGrunnlag(
        trygdeavgiftsperiode = trygdeavgiftsperiode,
        medlemskapsperiode = medlemskapsperiode,
        inntektsperiode = inntektForTest {
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            type = inntektskildetype
            avgiftspliktigMndInntekt = Penger(5000.0)
            arbeidsgiversavgiftBetalesTilSkatt = arbeidsgiveravgiftTilSkatt
        },
        skatteforhold = skatteforholdForTest {
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            this.skatteplikttype = skatteplikttype
        },
    )

    private fun sats(
        år: Int,
        sats: String?,
        beregningsregel: Avgiftsberegningsregel,
        inntektskildetype: Inntektskildetype?,
        avgiftsdel: Avgiftsdel?,
        harSammenslåtteInntektskilder: Boolean = false,
    ) = AvgiftssatsDto(
        år = år,
        avgiftssats = sats?.let(::BigDecimal),
        beregningsregel = beregningsregel,
        inntektskildetype = inntektskildetype,
        harSammenslåtteInntektskilder = harSammenslåtteInntektskilder,
        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON.name,
        avgiftsdel = avgiftsdel,
    )
}
