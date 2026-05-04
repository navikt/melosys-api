package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

internal class ÅrsavregningServiceOpprettTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `Ny årsavregning kaster feil når flere årsavregninger eksisterer for samme år på samme Fagsak`() {
        val eksisterendeBehandling = Behandling.forTest { id = 1L }
        val eksisterendeBehandlingsresultat = Behandlingsresultat.forTest {
            behandling = eksisterendeBehandling
        }
        val årsavregningEntity1 = Årsavregning.forTest {
            aar = 2023
            behandlingsresultat = eksisterendeBehandlingsresultat
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(årsavregningEntity1))
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(1, 2023) }.returns(1)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(eksisterendeBehandlingsresultat)

        shouldThrow<FunksjonellException> {
            årsavregningService.opprettÅrsavregning(1, 2023)
        }
    }

    @Test
    fun `opprettÅrsavregning - første gang uten tidligere årsavregning eller behandlinger`() {
        // Scenario 1: Ingen tidligere årsavregning
        val fagsak = Fagsak.forTest {
            saksnummer = "123456"
            behandling {
                id = 1L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
            }
            tema = Sakstemaer.UNNTAK
        }

        // Første behandling med medlemskap og avgift (NY_VURDERING)
        val nyVurderingBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling = fagsak.behandlinger[0]

            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        // Ny årsavregningsbehandling som skal opprettes
        val årsavregningBehandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling = fagsak.behandlinger[1]
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> nyVurderingBehandlingsresultat
                2L -> årsavregningBehandlingsresultat
                else -> null
            }.shouldNotBeNull()
        }

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(2, 2023) } returns 0
        every { behandlingsresultatService.lagreOgFlush(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning?.id = 50L
            }
        }

        val resultat = årsavregningService.opprettÅrsavregning(2, 2023)

        resultat.shouldNotBeNull().run {
            årsavregningID shouldBe 50L
            år shouldBe 2023

            // Verifiser at tidligere grunnlag er hentet fra NY_VURDERING
            tidligereTrygdeavgiftsGrunnlag.shouldNotBeNull().run {
                avgiftspliktigperioder.shouldHaveSize(1).single().run {
                    fom shouldBe LocalDate.of(2023, 1, 1)
                    tom shouldBe LocalDate.of(2023, 12, 31)
                }
            }

            // Verifiser at gjeldende medlemskapsperioder også er satt
            sisteGjeldendeAvgiftspliktigPerioder.shouldHaveSize(1).single().run {
                fom shouldBe LocalDate.of(2023, 1, 1)
                tom shouldBe LocalDate.of(2023, 12, 31)
            }

            // Verifiser at tidligereAvgift er hentet
            tidligereAvgift.isNotEmpty() shouldBe true

            // Siden det ikke finnes tidligere årsavregning, skal tidligereFakturertBeloep være beregnet
            tidligereFakturertBeloep shouldNotBe null
        }
    }

    @Test
    fun `Ny årsavregning med tidligere årsavregning og påfølgende ny vurdering - skal hente noe data fra tidligere årsavregning`() {
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.AVSLUTTET

            }
            behandling {
                id = 3L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.now().plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.OPPRETTET
            }
            tema = Sakstemaer.UNNTAK
        }
        val behandlingsresultatÅrsavregningEksisterende = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            årsavregning {
                id = 112
                aar = 2023
                innbetaltTrygdeavgift = BigDecimal("5000")
            }
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling = fagsak.behandlinger[0]

            medlemskapsperiode("2023-01-01", "2023-05-31")
        }

        val behandlingsresultatNyVurdering = Behandlingsresultat.forTest {
            id = 2L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling = fagsak.behandlinger[1]

            medlemskapsperiode("2023-01-01", "2023-05-31")
        }

        val behandlingsresultatÅrsavregningNy = Behandlingsresultat.forTest {
            id = 3L
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling = fagsak.behandlinger[2]
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> behandlingsresultatÅrsavregningEksisterende
                2L -> behandlingsresultatNyVurdering
                3L -> behandlingsresultatÅrsavregningNy
                else -> null
            }.shouldNotBeNull()
        }

        every { fagsakService.hentFagsak(any()) }.returns(fagsak)

        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning!!.id = 50L
            }
        }

        årsavregningService.opprettÅrsavregning(3, 2023) shouldBe ÅrsavregningModel(
            årsavregningID = 50L,
            år = 2023,
            tidligereTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(
                listOf(
                    MedlemskapsperiodeForAvgift(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 5, 31),
                        dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                        medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                        InnvilgelsesResultat.INNVILGET
                    )
                ),
                listOf(SkatteforholdTilNorgeForAvgift(lagSkatteforholdTilNorge("2023-01-01", "2023-05-31"))),
                listOf(InntektsperioderForAvgift(Inntektsperiode().apply {
                    fomDato = LocalDate.parse("2023-01-01")
                    tomDato = LocalDate.parse("2023-05-31")
                    avgiftspliktigMndInntekt = Penger(5000.0)
                    type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                }))
            ),
            sisteGjeldendeAvgiftspliktigPerioder = listOf(
                MedlemskapsperiodeForAvgift(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 5, 31),
                    dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                    medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                    InnvilgelsesResultat.INNVILGET
                )
            ),
            tidligereAvgift = behandlingsresultatNyVurdering.trygdeavgiftsperioder.filter { it.overlapperMedÅr(2023) },
            nyttTrygdeavgiftsGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = BigDecimal("25000.00"),
            beregnetAvgiftBelop = null,
            tilFaktureringBeloep = null,
            harInnbetaltTrygdeavgift = true,
            innbetaltTrygdeavgift = BigDecimal("5000"),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            manueltAvgiftBeloep = null,
            tidligereInnbetaltTrygdeavgift = BigDecimal("5000"),
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )
    }

    @Test
    fun `opprettÅrsavregning - ny vurdering som fjerner perioden for året replikerer ingen medlemskapsperioder`() {
        val fagsak = Fagsak.forTest {
            saksnummer = "123456"
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.of(2025, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                registrertDato = LocalDate.of(2025, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 3L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.of(2025, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.OPPRETTET
            }
            tema = Sakstemaer.UNNTAK
        }

        val tidligereÅrsavregningsresultat = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            årsavregning {
                id = 112
                aar = 2025
                trygdeavgiftFraAvgiftssystemet = BigDecimal("5000")
            }
            registrertDato = LocalDate.of(2025, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2025, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling = fagsak.behandlinger[0]

            medlemskapsperiode("2025-01-01", "2025-12-31")
        }

        val nyVurderingKun2026 = Behandlingsresultat.forTest {
            id = 2L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2025, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2025, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling = fagsak.behandlinger[1]

            medlemskapsperiode("2026-01-01", "2026-12-31", medTrygdeavgift = false)
        }

        val nyÅrsavregningBehandlingsresultat = Behandlingsresultat.forTest {
            id = 3L
            registrertDato = LocalDate.of(2025, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling = fagsak.behandlinger[2]
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            when (firstArg<Long>()) {
                1L -> tidligereÅrsavregningsresultat
                2L -> nyVurderingKun2026
                3L -> nyÅrsavregningBehandlingsresultat
                else -> null
            }.shouldNotBeNull()
        }

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(3, 2025) } returns 0
        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning?.id = 50L
            }
        }

        val resultat = årsavregningService.opprettÅrsavregning(3, 2025)

        resultat.shouldNotBeNull().run {
            årsavregningID shouldBe 50L
            år shouldBe 2025
            sisteGjeldendeAvgiftspliktigPerioder shouldHaveSize 0
            tidligereTrygdeavgiftsGrunnlag.shouldNotBeNull().avgiftspliktigperioder.shouldHaveSize(1)
            tidligereAvgift.shouldHaveSize(1)
            nyttTrygdeavgiftsGrunnlag shouldBe null
            beregnetAvgiftBelop shouldBe null
            tilFaktureringBeloep shouldBe null
            trygdeavgiftFraAvgiftssystemet shouldBe BigDecimal("5000")
        }

        nyÅrsavregningBehandlingsresultat.medlemskapsperioder.shouldHaveSize(0)
        nyÅrsavregningBehandlingsresultat.helseutgiftDekkesPeriode shouldBe null
        nyÅrsavregningBehandlingsresultat.årsavregning.shouldNotBeNull().run {
            tidligereBehandlingsresultat shouldBe nyVurderingKun2026
            tidligereFakturertBeloep shouldNotBe null
            beregnetAvgiftBelop shouldBe null
            tilFaktureringBeloep shouldBe null
            manueltAvgiftBeloep shouldBe null
        }
    }

    @Test
    fun `opprettÅrsavregning - EØS pensjonist med eksisterende årsavregning fjerner gammel helseutgiftDekkesPeriode ved årbytte`() {
        val fagsak = Fagsak.forTest {
            saksnummer = "123456"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.TRYGDEAVGIFT
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
            }
        }

        val førstegangBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling = fagsak.behandlinger[0]

            helseutgiftDekkesPeriode("2023-01-01", "2024-12-31")
        }

        val årsavregningBehandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling = fagsak.behandlinger[1]

            helseutgiftDekkesPeriode("2023-01-01", "2023-12-31", medTrygdeavgift = false)
        }

        val eksisterendeÅrsavregning = Årsavregning.forTest {
            aar = 2023
            behandlingsresultat = årsavregningBehandlingsresultat
        }
        årsavregningBehandlingsresultat.årsavregning = eksisterendeÅrsavregning

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> førstegangBehandlingsresultat
                2L -> årsavregningBehandlingsresultat
                else -> null
            }.shouldNotBeNull()
        }

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(2, 2024) } returns 0
        every { behandlingsresultatService.lagreOgFlush(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning?.id = 51L
            }
        }

        val resultat = årsavregningService.opprettÅrsavregning(2, 2024)

        resultat.shouldNotBeNull().run {
            årsavregningID shouldBe 51L
            år shouldBe 2024

            årsavregningBehandlingsresultat.helseutgiftDekkesPerioder.shouldNotBeEmpty()
            årsavregningBehandlingsresultat.helseutgiftDekkesPerioder.first().let { periode ->
                periode.fomDato shouldBe LocalDate.of(2024, 1, 1)
                periode.tomDato shouldBe LocalDate.of(2024, 12, 31)
                periode.behandlingsresultat shouldBe årsavregningBehandlingsresultat
                periode.trygdeavgiftsperioder shouldHaveSize 0
            }
        }
    }

    @Test
    fun `opprettÅrsavregning - EØS pensjonist med HelseutgiftDekkesPeriode replikerer helseutgiftperiode`() {
        val fagsak = Fagsak.forTest {
            saksnummer = "123456"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.TRYGDEAVGIFT
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
            }
        }

        // Første behandling med helseutgiftperiode (EØS pensjonist)
        val førstegangBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling = fagsak.behandlinger[0]

            helseutgiftDekkesPeriode("2023-01-01", "2023-12-31")
        }

        // Ny årsavregningsbehandling som skal opprettes
        val årsavregningBehandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling = fagsak.behandlinger[1]
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> førstegangBehandlingsresultat
                2L -> årsavregningBehandlingsresultat
                else -> null
            }.shouldNotBeNull()
        }

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(2, 2023) } returns 0
        every { behandlingsresultatService.lagreOgFlush(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning?.id = 50L
            }
        }

        val resultat = årsavregningService.opprettÅrsavregning(2, 2023)

        resultat.shouldNotBeNull().run {
            årsavregningID shouldBe 50L
            år shouldBe 2023

            // Verifiser at helseutgiftDekkesPeriode ble replikert til det nye behandlingsresultatet
            årsavregningBehandlingsresultat.helseutgiftDekkesPerioder.shouldNotBeEmpty()
            årsavregningBehandlingsresultat.helseutgiftDekkesPerioder.first().let { periode ->
                periode.fomDato shouldBe LocalDate.of(2023, 1, 1)
                periode.tomDato shouldBe LocalDate.of(2023, 12, 31)
                periode.behandlingsresultat shouldBe årsavregningBehandlingsresultat
                periode.trygdeavgiftsperioder shouldHaveSize 0
            }

            // Verifiser at sisteGjeldendeAvgiftspliktigPerioder inneholder HelseutgiftDekkesPeriodeForAvgift
            sisteGjeldendeAvgiftspliktigPerioder.shouldHaveSize(1).single().run {
                shouldBeInstanceOf<HelseutgiftDekkesPeriodeForAvgift>()
                fom shouldBe LocalDate.of(2023, 1, 1)
                tom shouldBe LocalDate.of(2023, 12, 31)
                dekning shouldBe Trygdedekninger.FULL_DEKNING_EOSFO
            }

            tidligereFakturertBeloep shouldNotBe null
        }
    }
}
