package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

internal class ÅrsavregningServiceOppdaterTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `tilFaktureringBeloep skal settes til diff mellom nytt totalbeloep og tidligere fakturert beloep`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                this.fagsak = fagsak
            }
            årsavregning = Årsavregning.forTest {
                id = 1
                aar = 2023
                tidligereFakturertBeloep = BigDecimal.valueOf(12.4)
                behandlingsresultat = this@resultat
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(5.2))


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(-7.2)
    }

    @Test
    fun `tilFaktureringBeloep skal settes til beregnetAvgiftBelop hvis ikke tidligere avgift er satt`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                this.fagsak = fagsak
            }
            årsavregning = Årsavregning.forTest {
                id = 1L
                aar = 2023
                behandlingsresultat = this@resultat
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.ONE, null, null)


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.ONE
    }

    @Test
    fun `tilFaktureringBeloep skal settes hvis avgift i avgiftssystemet og ny avgift ikke er null`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                this.fagsak = fagsak
            }
            årsavregning = Årsavregning.forTest {
                id = 1L
                aar = 2023
                behandlingsresultat = this@resultat
                harTrygdeavgiftFraAvgiftssystemet = true
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(42.0), BigDecimal.valueOf(4.4))


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(37.6)
    }

    @Test
    fun `tilFaktureringBeloep skal settes til diff mellom beregnetAvgiftBelop og avgift i avgiftssystemet og melosys`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                this.fagsak = fagsak
            }
            årsavregning = Årsavregning.forTest {
                id = 1L
                aar = 2023
                tidligereFakturertBeloep = BigDecimal(37.0)
                behandlingsresultat = this@resultat
                harTrygdeavgiftFraAvgiftssystemet = true
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


        årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(42.0), BigDecimal.valueOf(4.4))


        behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(0.6)
    }

    @Test
    fun `harTrygdeavgiftFraAvgiftssystemet skal ikke settes hvis null`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                this.fagsak = fagsak
            }
            årsavregning = Årsavregning.forTest {
                id = 1L
                aar = 2023
                behandlingsresultat = this@resultat
            }
        }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
        behandlingsresultat.hentÅrsavregning().harTrygdeavgiftFraAvgiftssystemet shouldBe null


        årsavregningService.oppdater(1L, 1L, null, BigDecimal.ONE)


        behandlingsresultat.hentÅrsavregning().harTrygdeavgiftFraAvgiftssystemet shouldBe null
    }
}
