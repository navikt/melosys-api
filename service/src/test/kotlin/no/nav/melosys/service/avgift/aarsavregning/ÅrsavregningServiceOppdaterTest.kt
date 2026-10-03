package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

internal class ÅrsavregningServiceOppdaterTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `tilFaktureringBeloep skal settes til diff mellom nytt totalbeloep og tidligere fakturert beloep`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                this.fagsak = fagsak
            }
            årsavregning {
                id = 1
                aar = 2023
                tidligereFakturertBeloep = BigDecimal.valueOf(12.4)
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
        every { behandlingsresultatService.hentBehandlingsresultatMedTrygdeavgiftsperioder(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(5.2))


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(-7.2)
    }

    @Test
    fun `tilFaktureringBeloep skal settes til beregnetAvgiftBelop hvis ikke tidligere avgift er satt`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                this.fagsak = fagsak
            }
            årsavregning {
                id = 1L
                aar = 2023
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
        every { behandlingsresultatService.hentBehandlingsresultatMedTrygdeavgiftsperioder(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.ONE, null, null)


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.ONE
    }

    @Test
    fun `tilFaktureringBeloep skal settes hvis innbetalt trygdeavgift og ny avgift ikke er null`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                this.fagsak = fagsak
            }
            årsavregning {
                id = 1L
                aar = 2023
                harInnbetaltTrygdeavgift = true
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
        every { behandlingsresultatService.hentBehandlingsresultatMedTrygdeavgiftsperioder(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(42.0), BigDecimal.valueOf(4.4))


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(37.6)
    }

    @Test
    fun `tilFaktureringBeloep skal settes til diff mellom beregnetAvgiftBelop og innbetalt trygdeavgift og melosys`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak { }
            }
            årsavregning {
                id = 1L
                aar = 2023
                tidligereFakturertBeloep = BigDecimal(37.0)
                harInnbetaltTrygdeavgift = true
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
        every { behandlingsresultatService.hentBehandlingsresultatMedTrygdeavgiftsperioder(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(42.0), BigDecimal.valueOf(4.4))


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(0.6)
    }

    @Test
    fun `harInnbetaltTrygdeavgift skal ikke settes hvis null`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak { }
            }
            årsavregning {
                id = 1L
                aar = 2023
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
        every { behandlingsresultatService.hentBehandlingsresultatMedTrygdeavgiftsperioder(1L) }.returns(behandlingsresultat)
        behandlingsresultat.hentÅrsavregning().harInnbetaltTrygdeavgift shouldBe null


        årsavregningService.oppdater(1L, 1L, null, BigDecimal.ONE)


        behandlingsresultat.hentÅrsavregning().harInnbetaltTrygdeavgift shouldBe null
    }

    @Test
    fun `oppdaterBeregnetAvgift - rører ikke manuelt fastsatt avgift`() {
        val årsavregning = Årsavregning.forTest {
            endeligAvgiftValg = EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT
            manueltAvgiftBeloep = BigDecimal("5000")
            tilFaktureringBeloep = BigDecimal("-100")
        }

        årsavregningService.oppdaterBeregnetAvgift(årsavregning, BigDecimal("9000"))

        årsavregning.beregnetAvgiftBelop shouldBe null
        årsavregning.tilFaktureringBeloep shouldBe BigDecimal("-100")
    }

    @Test
    fun `oppdaterBeregnetAvgift - uten totalavgift nullstilles beløp til fakturering`() {
        val årsavregning = Årsavregning.forTest {
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
            beregnetAvgiftBelop = BigDecimal("1000")
            tilFaktureringBeloep = BigDecimal("1000")
        }

        årsavregningService.oppdaterBeregnetAvgift(årsavregning, null)

        årsavregning.beregnetAvgiftBelop shouldBe null
        årsavregning.tilFaktureringBeloep shouldBe null
    }

    @Test
    fun `oppdaterBeregnetAvgift - setter beregnet avgift og regner ut beløp til fakturering`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                this.fagsak = fagsak
            }
            årsavregning {
                aar = 2023
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                tidligereFakturertBeloep = BigDecimal("1000")
            }
        }
        every { fagsakService.hentFagsak(any()) } returns fagsak

        årsavregningService.oppdaterBeregnetAvgift(behandlingsresultat.hentÅrsavregning(), BigDecimal("1500"))

        behandlingsresultat.hentÅrsavregning().beregnetAvgiftBelop shouldBe BigDecimal("1500")
        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal("500")
    }
}
