package no.nav.melosys.service.avgift.aarsavregning

import ch.qos.logback.classic.Level
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.LoggingTestUtils.withLogAppender
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal class ÅrsavregningServiceHentSisteBehandlingsresultatTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `henter nyeste behandlingsresultat med grunnlag og riktig år for opprettelse av ny årsavregning`() {
        val eldreBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }
        val aktivFagsak = eldreBehandlingsresultat.hentBehandling().fagsak

        val nyesteBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-01-01", "2023-08-31", medTrygdeavgift = false)
        }


        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyesteBehandlingsresultat


        // Med ny logikk: nyesteBehandlingsresultat har medlemskapsperioder, men ingen har avgiftsgrunnlag
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedAvgiftspliktigPeriode = nyesteBehandlingsresultat,
                    sisteBehandlingsresultatMedAvgift = null
                )
            )
        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter nyeste behandlingsresultat med manuellAvgift satt og uten medlemskapsperioder ved opprettelse av årsavregning`() {

        val eldreBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }

        val aktivFagsak = eldreBehandlingsresultat.hentBehandling().fagsak

        val behandlingsresultatMedManuelAvgift = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            årsavregning {
                id = 2
                aar = 2023
                manueltAvgiftBeloep = BigDecimal.valueOf(1000.0)
            }
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
        }


        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingsresultatMedManuelAvgift


        // Med ny logikk: eldreBehandlingsresultat har medlemskapsperioder, men ingen behandling har avgiftsgrunnlag
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedAvgiftspliktigPeriode = eldreBehandlingsresultat,
                    sisteBehandlingsresultatMedAvgift = null,
                    sisteÅrsavregning = behandlingsresultatMedManuelAvgift
                )
            )
        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @ParameterizedTest
    @EnumSource(Behandlingsresultattyper::class, names = ["FERDIGBEHANDLET", "HENLEGGELSE_BORTFALT"])
    fun `ekskluderer årsavregninger uten vedtak`() {
        val eldreForstegangsbehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }
        val aktivFagsak = eldreForstegangsbehandlingsresultat.hentBehandling().fagsak

        val nyttÅrsavregningsbehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.FERDIGBEHANDLET
            behandling {
                id = 1
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreForstegangsbehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyttÅrsavregningsbehandlingsresultat


        // Med ny logikk: eldreForstegangsbehandlingsresultat har medlemskapsperioder, men ingen avgift
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedAvgiftspliktigPeriode = eldreForstegangsbehandlingsresultat,
                    sisteBehandlingsresultatMedAvgift = null
                )
            )
        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter årsavregning med resulttatype FASTSATT_TRYGDEAVGIFT`() {
        val forstegangsbehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }
        val aktivFagsak = forstegangsbehandlingsresultat.hentBehandling().fagsak

        val vedtattAarsavregningsresultat = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling {
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }

        val ferdigbehandletAarsavregningsresultat = lagTidligereBehandlingsresultat {
            id = 3
            type = Behandlingsresultattyper.FERDIGBEHANDLET
            behandling {
                id = 3
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false)
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forstegangsbehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns vedtattAarsavregningsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(3) } returns ferdigbehandletAarsavregningsresultat


        // Med ny logikk: vedtattAarsavregningsresultat har medlemskapsperioder, men ingen avgift
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldBe(
                GjeldendeBehandlingsresultaterForÅrsavregning(
                    sisteBehandlingsresultatMedAvgiftspliktigPeriode = vedtattAarsavregningsresultat,
                    sisteBehandlingsresultatMedAvgift = null
                )
            )
        verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter separate behandlinger for medlemskapsperiode og avgiftsgrunnlag når de er forskjellige`() {
        // Scenario 4: Tidligere årsavregning med senere ny vurdering med ulikt medlemskapsperiode

        // Første behandling med medlemskap og avgift
        val forsteBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2023-01-01", "2023-12-31")
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 1, 11).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val aktivFagsak = forsteBehandlingsresultat.hentBehandling().fagsak

        // Årsavregning basert på første behandling
        val aarsavregningsresultat = Behandlingsresultat.forTest {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                aar = 2023
                manueltAvgiftBeloep = null
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        // Ny vurdering med endret medlemskapsperiode (kortere periode)
        val nyVurderingMedEndretMedlemskap = Behandlingsresultat.forTest {
            id = 3
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 3
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2023-01-01", "2023-06-30", medTrygdeavgift = false) // Endret periode, ingen avgift
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forsteBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns aarsavregningsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(3) } returns nyVurderingMedEndretMedlemskap

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe nyVurderingMedEndretMedlemskap
            sisteBehandlingsresultatMedAvgift shouldBe aarsavregningsresultat
        }

        verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `henter samme behandling for medlemskap og avgift når medlemskapsperiode ikke er endret`() {
        // Scenario 3: Tidligere årsavregning med senere ny vurdering med likt medlemskapsperiode
        val aarsavregningsresultat = Behandlingsresultat.forTest {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                aar = 2023
                manueltAvgiftBeloep = null
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = aarsavregningsresultat.hentBehandling().fagsak

        // Ny vurdering med samme medlemskapsperiode - ingen trygdeavgift siden det er ny vurdering
        val nyVurderingSammeMedlemskap = Behandlingsresultat.forTest {
            id = 2
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2023-01-01", "2023-12-31", medTrygdeavgift = false) // Samme periode, men uten avgift
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns aarsavregningsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyVurderingSammeMedlemskap

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe nyVurderingSammeMedlemskap
            sisteBehandlingsresultatMedAvgift shouldBe aarsavregningsresultat
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `håndterer behandling uten trygdeavgiftsperioder korrekt`() {
        // Behandling med medlemskap men uten trygdeavgiftsperioder
        val behandlingUtenAvgift = Behandlingsresultat.forTest {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode("2023-01-01", "2023-12-31", medTrygdeavgift = false)
        }
        val aktivFagsak = behandlingUtenAvgift.hentBehandling().fagsak

        // Behandling med både medlemskap og avgift
        val behandlingMedAvgift = Behandlingsresultat.forTest {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingUtenAvgift
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingMedAvgift

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe behandlingMedAvgift
            sisteBehandlingsresultatMedAvgift shouldBe behandlingMedAvgift
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `velger behandling basert på vedtaksdato og ikke registrertDato når disse er forskjellige`() {
        val behandlingMedTidligVedtaksdato = Behandlingsresultat.forTest {
            id = 1
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.of(2023, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                id = 100
                aar = 2023
            }
            behandling {
                id = 1
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = behandlingMedTidligVedtaksdato.hentBehandling().fagsak

        val behandlingMedSenVedtaksdato = Behandlingsresultat.forTest {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                id = 200
                aar = 2023
            }
            behandling {
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingMedTidligVedtaksdato
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingMedSenVedtaksdato

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe behandlingMedSenVedtaksdato
            sisteBehandlingsresultatMedAvgift shouldBe behandlingMedSenVedtaksdato
            sisteÅrsavregning shouldBe behandlingMedSenVedtaksdato
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `filtrerer bort behandlingsresultater med vedtaksdato etter gitt førVedtaksdato`() {
        val tidligBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.FTRL
                    saksnummer = "123456"
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = tidligBehandlingsresultat.hentBehandling().fagsak

        val senBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling {
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                aar = 2023
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns tidligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns senBehandlingsresultat

        // Når vi henter med førVedtaksdato som er før den andre behandlingen
        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            "123456",
            2023,
            førVedtaksdato = LocalDate.of(2023, 6, 15).atStartOfDay().toInstant(ZoneOffset.UTC)
        )

        resultat.shouldNotBeNull()
        with(resultat) {
            // Kun første behandlingen skal være med siden den andre har vedtaksdato 2023-07-01
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe tidligBehandlingsresultat
            sisteBehandlingsresultatMedAvgift shouldBe tidligBehandlingsresultat
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `inkluderer alle behandlingsresultater når førVedtaksdato er null`() {
        val tidligBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.FTRL
                    saksnummer = "123456"
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-06-30")
        }
        val aktivFagsak = tidligBehandlingsresultat.hentBehandling().fagsak

        val senBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling {
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                aar = 2023
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns tidligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns senBehandlingsresultat

        // Når vi henter uten førVedtaksdato skal alle behandlinger inkluderes
        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            "123456",
            2023,
            førVedtaksdato = null
        )

        resultat.shouldNotBeNull()
        with(resultat) {
            // Begge behandlinger skal være med
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe senBehandlingsresultat
            sisteBehandlingsresultatMedAvgift shouldBe senBehandlingsresultat
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `filtrerer bort alle behandlingsresultater når alle har vedtaksdato etter førVedtaksdato`() {
        val behandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                }
            }
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = behandlingsresultat.hentBehandling().fagsak

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingsresultat

        // Når førVedtaksdato er før alle behandlinger
        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            "123456",
            2023,
            førVedtaksdato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        )

        // Skal returnere null når alle behandlinger filtreres bort
        resultat shouldBe null

        verify(exactly = 1) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `filtrerer bort behandlingsresultat når vedtaksdato er lik førVedtaksdato`() {
        val behandlingsresultat = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                }
            }
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 6, 15).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = behandlingsresultat.hentBehandling().fagsak

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingsresultat

        // Når førVedtaksdato er lik vedtaksdato
        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            "123456",
            2023,
            førVedtaksdato = LocalDate.of(2023, 6, 15).atStartOfDay().toInstant(ZoneOffset.UTC)
        )

        // Med < operator skal behandlinger med samme vedtaksdato filtreres bort
        resultat shouldBe null

        verify(exactly = 1) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `ny vurdering som fjerner perioden for året gir nyeste behandling som avgiftspliktig periode`() {
        // Behandling 1: Medlemskap 2025-2026
        val behandlingMedToÅr = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2025, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2025, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2025-01-01", "2026-12-31", medTrygdeavgift = false)
        }
        val aktivFagsak = behandlingMedToÅr.hentBehandling().fagsak

        // Behandling 2 (ny vurdering): Kun 2026, perioden for 2025 er fjernet
        val nyVurderingKun2026 = lagTidligereBehandlingsresultat {
            id = 2
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            registrertDato = LocalDate.of(2025, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2025, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2026-01-01", "2026-12-31", medTrygdeavgift = false)
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingMedToÅr
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyVurderingKun2026

        val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2025)

        // Skal bruke nyeste behandling selv om den ikke overlapper 2025,
        // slik at årsavregningen korrekt gjenspeiler at perioden er fjernet
        resultat.shouldNotBeNull()
        with(resultat) {
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe nyVurderingKun2026
            sisteBehandlingsresultatMedAvgift shouldBe null
            sisteÅrsavregning shouldBe null
        }

        verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    /**
     * En sak avsluttet fra behandlingsmenyen med «Søknaden er innvilget» får resultattype uten at det
     * fattes vedtak i Melosys. Raden i vedtak_metadata finnes da ikke, og [Behandlingsresultat.harVedtak]
     * finnes nettopp fordi tilstanden er lovlig. Oppslaget skal sortere en slik behandling som eldst, ikke feile.
     */
    @Test
    fun `velger behandlingen med vedtak når en annen avsluttet behandling mangler vedtaksmetadata`() {
        val behandlingMedVedtak = lagTidligereBehandlingsresultat {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = behandlingMedVedtak.hentBehandling().fagsak

        // Avsluttet uten vedtak i Melosys, og for et annet år enn det som avregnes
        val behandlingUtenVedtak = Behandlingsresultat.forTest {
            id = 2
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            registrertDato = LocalDate.of(2024, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2024-01-01", "2024-12-31", medTrygdeavgift = false)
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingMedVedtak
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingUtenVedtak

        withLogAppender<ÅrsavregningService> { logger ->
            val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

            resultat.shouldNotBeNull()
            with(resultat) {
                sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe behandlingMedVedtak
                sisteBehandlingsresultatMedAvgift shouldBe behandlingMedVedtak
                sisteÅrsavregning shouldBe null
            }

            logger.list.filter { it.level == Level.INFO }.map { it.formattedMessage } shouldContain
                "1 behandling(er) uten vedtaksdato i sak 123456 ved oppslag for årsavregning"
        }

        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            "123456", 2023, førVedtaksdato = Instant.parse("2025-01-01T00:00:00Z")
        ).shouldNotBeNull().sisteBehandlingsresultatMedAvgift shouldBe behandlingMedVedtak

        verify(exactly = 4) { behandlingsresultatService.hentBehandlingsresultat(any()) }
    }

    @Test
    fun `behandling uten vedtaksdato brukes når den er eneste, men ikke når grunnlaget skal være fra før en vedtaksdato`() {
        val behandlingUtenVedtak = Behandlingsresultat.forTest {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        every { fagsakService.hentFagsak("123456") } returns behandlingUtenVedtak.hentBehandling().fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingUtenVedtak

        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
            .shouldNotBeNull().sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe behandlingUtenVedtak
        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            "123456", 2023, førVedtaksdato = Instant.parse("2025-01-01T00:00:00Z")
        ) shouldBe null
    }

    @Test
    fun `flere behandlinger uten vedtaksdato sorteres på registrert dato`() {
        val nyest = Behandlingsresultat.forTest {
            id = 1
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2024, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling {
                id = 1
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }
        val aktivFagsak = nyest.hentBehandling().fagsak

        val eldst = Behandlingsresultat.forTest {
            id = 2
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            behandling {
                id = 2
                status = Behandlingsstatus.AVSLUTTET
                fagsak = aktivFagsak
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        every { fagsakService.hentFagsak("123456") } returns aktivFagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1) } returns nyest
        every { behandlingsresultatService.hentBehandlingsresultat(2) } returns eldst

        with(årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023).shouldNotBeNull()) {
            sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe nyest
            sisteBehandlingsresultatMedAvgift shouldBe nyest
        }
    }
}
