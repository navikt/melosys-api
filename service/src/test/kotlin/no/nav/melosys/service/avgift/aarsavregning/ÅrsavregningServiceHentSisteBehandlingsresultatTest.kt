package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

internal class ÅrsavregningServiceHentSisteBehandlingsresultatTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `henter nyeste behandlingsresultat med grunnlag og riktig år for opprettelse av ny årsavregning`() {
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        val eldreBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        val nyesteBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-08-31", medTrygdeavgift = false))
        }


        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyesteBehandlingsresultat


        // Med ny logikk: nyesteBehandlingsresultat har medlemskapsperioder, men ingen har avgiftsgrunnlag
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedMedlemskapsperiode = nyesteBehandlingsresultat,
                    sisteBehandlingsresultatMedAvgift = null
                )
            )
        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter nyeste behandlingsresultat med manuellAvgift satt og uten medlemskapsperioder ved opprettelse av årsavregning`() {
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        val eldreBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        val behandlingsresultatMedManuelAvgift = lagTidligereBehandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            årsavregning = Årsavregning.forTest {
                id = 2
                aar = 2023
                manueltAvgiftBeloep = BigDecimal.valueOf(1000.0)
            }
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf()
        }


        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingsresultatMedManuelAvgift


        // Med ny logikk: eldreBehandlingsresultat har medlemskapsperioder, men ingen behandling har avgiftsgrunnlag
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedMedlemskapsperiode = eldreBehandlingsresultat,
                    sisteBehandlingsresultatMedAvgift = null,
                    sisteÅrsavregning = behandlingsresultatMedManuelAvgift
                )
            )
        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @ParameterizedTest
    @EnumSource(Behandlingsresultattyper::class, names = ["FERDIGBEHANDLET", "HENLEGGELSE_BORTFALT"])
    fun `ekskluderer årsavregninger uten vedtak`() {
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        val eldreForstegangsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        val nyttÅrsavregningsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.FERDIGBEHANDLET
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreForstegangsbehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyttÅrsavregningsbehandlingsresultat


        // Med ny logikk: eldreForstegangsbehandlingsresultat har medlemskapsperioder, men ingen avgift
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedMedlemskapsperiode = eldreForstegangsbehandlingsresultat,
                    sisteBehandlingsresultatMedAvgift = null
                )
            )
        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter årsavregning med resulttatype FASTSATT_TRYGDEAVGIFT`() {
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        val forstegangsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        val vedtattAarsavregningsresultat = lagTidligereBehandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        val ferdigbehandletAarsavregningsresultat = lagTidligereBehandlingsresultat().apply {
            id = 3
            type = Behandlingsresultattyper.FERDIGBEHANDLET
            behandling = Behandling.forTest().apply behandling@{
                id = 3
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forstegangsbehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns vedtattAarsavregningsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(3) } returns ferdigbehandletAarsavregningsresultat


        // Med ny logikk: vedtattAarsavregningsresultat har medlemskapsperioder, men ingen avgift
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedMedlemskapsperiode = vedtattAarsavregningsresultat,
                    sisteBehandlingsresultatMedAvgift = null
                )
            )
        verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter separate behandlinger for medlemskapsperiode og avgiftsgrunnlag når de er forskjellige`() {
        // Scenario 4: Tidligere årsavregning med senere ny vurdering med ulikt medlemskapsperiode
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        // Første behandling med medlemskap og avgift
        val forsteBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 1, 11).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        // Årsavregning basert på første behandling
        val aarsavregningsresultat = Behandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            årsavregning = Årsavregning.forTest {
                aar = 2023
                manueltAvgiftBeloep = null
            }
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        // Ny vurdering med endret medlemskapsperiode (kortere periode)
        val nyVurderingMedEndretMedlemskap = Behandlingsresultat().apply {
            id = 3
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 3
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder =
                mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-06-30", medTrygdeavgift = false)) // Endret periode, ingen avgift
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forsteBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns aarsavregningsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(3) } returns nyVurderingMedEndretMedlemskap

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedMedlemskapsperiode shouldBe nyVurderingMedEndretMedlemskap
            sisteBehandlingsresultatMedAvgift shouldBe aarsavregningsresultat
        }

        verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter samme behandling for medlemskap og avgift når medlemskapsperiode ikke er endret`() {
        // Scenario 3: Tidligere årsavregning med senere ny vurdering med likt medlemskapsperiode
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        val aarsavregningsresultat = Behandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            årsavregning = Årsavregning.forTest {
                aar = 2023
                manueltAvgiftBeloep = null
            }
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        // Ny vurdering med samme medlemskapsperiode - ingen trygdeavgift siden det er ny vurdering
        val nyVurderingSammeMedlemskap = Behandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder =
                mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31", medTrygdeavgift = false)) // Samme periode, men uten avgift
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns aarsavregningsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyVurderingSammeMedlemskap

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedMedlemskapsperiode shouldBe nyVurderingSammeMedlemskap
            sisteBehandlingsresultatMedAvgift shouldBe aarsavregningsresultat
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `håndterer behandling uten trygdeavgiftsperioder korrekt`() {
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        // Behandling med medlemskap men uten trygdeavgiftsperioder
        val behandlingUtenAvgift = Behandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31", medTrygdeavgift = false))
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        // Behandling med både medlemskap og avgift
        val behandlingMedAvgift = Behandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingUtenAvgift
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingMedAvgift

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedMedlemskapsperiode shouldBe behandlingMedAvgift
            sisteBehandlingsresultatMedAvgift shouldBe behandlingMedAvgift
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `velger behandling basert på vedtaksdato og ikke registrertDato når disse er forskjellige`() {
        val aktivFagsak = Fagsak.forTest {
            saksnummer = "123456"
        }

        val behandlingMedTidligVedtaksdato = Behandlingsresultat().apply {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = Behandling.forTest().apply behandling@{
                id = 1
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            årsavregning = Årsavregning.forTest {
                id = 100
                aar = 2023
            }
        }

        val behandlingMedSenVedtaksdato = Behandlingsresultat().apply {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = Behandling.forTest().apply behandling@{
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
            }
            registrertDato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.of(2023, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            årsavregning = Årsavregning.forTest {
                id = 200
                aar = 2023
            }
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingMedTidligVedtaksdato
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingMedSenVedtaksdato

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedMedlemskapsperiode shouldBe behandlingMedSenVedtaksdato
            sisteBehandlingsresultatMedAvgift shouldBe behandlingMedSenVedtaksdato
            sisteÅrsavregning shouldBe behandlingMedSenVedtaksdato
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }
}
