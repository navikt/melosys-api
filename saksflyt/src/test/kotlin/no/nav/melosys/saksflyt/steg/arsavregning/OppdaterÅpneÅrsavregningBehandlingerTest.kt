package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OppdaterÅpneÅrsavregningBehandlingerTest {

    @RelaxedMockK
    private lateinit var årsavregningService: ÅrsavregningService

    private lateinit var oppdaterÅpneÅrsavregningBehandlinger: OppdaterÅpneÅrsavregningBehandlinger

    @BeforeEach
    fun setup() {
        oppdaterÅpneÅrsavregningBehandlinger = OppdaterÅpneÅrsavregningBehandlinger(årsavregningService)
    }

    @Nested
    inner class InngangsSteg {
        @Test
        fun `returnerer riktig prosess steg`() {
            oppdaterÅpneÅrsavregningBehandlinger.inngangsSteg() shouldBe ProsessSteg.OPPDATER_ÅPNE_ÅRSAVREGNINGER
        }
    }

    @Nested
    inner class Utfør {

        @Test
        fun `gjør ingenting hvis behandlingen ikke er en ny vurdering`() {
            val prosessinstans = Prosessinstans().apply {
                behandling = Behandling().apply {
                    id = 1L
                    type = Behandlingstyper.FØRSTEGANG
                }
            }

            oppdaterÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 0) { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) }
            verify(exactly = 0) { årsavregningService.oppdaterEksisterendeÅrsavregning(any()) }
        }

        @Test
        fun `oppdaterer alle åpne årsavregninger på fagsak`() {
            val saksnummer = "123456789"
            val prosessinstans = Prosessinstans().apply {
                setData(ProsessDataKey.SAKSNUMMER, saksnummer)
                behandling = Behandling().apply {
                    id = 1L
                    type = Behandlingstyper.NY_VURDERING
                }
            }

            val årsavregning1 = Årsavregning().apply {
                id = 1L
                aar = 2022
            }
            val årsavregning2 = Årsavregning().apply {
                id = 2L
                aar = 2023
            }

            val årsavregninger = listOf(årsavregning1, årsavregning2)

            every {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            } returns årsavregninger

            every { årsavregningService.oppdaterEksisterendeÅrsavregning(any()) } returns ÅrsavregningModel(
                årsavregningID = 1L,
                år = 2022,
                tidligereGrunnlag = null,
                tidligereAvgift = emptyList(),
                nyttGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = null,
                beregnetAvgiftBelop = null,
                tilFaktureringBeloep = null,
                harTrygdeavgiftFraAvgiftssystemet = null,
                trygdeavgiftFraAvgiftssystemet = null,
                manueltAvgiftBeloep = null,
                tidligereTrygdeavgiftFraAvgiftssystemet = null,
                tidligereÅrsavregningmanueltAvgiftBeloep = null,
                harSkjoennsfastsattInntektsgrunnlag = false
            )

            oppdaterÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 1) {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            }
            verify(exactly = 1) { årsavregningService.oppdaterEksisterendeÅrsavregning(1L) }
            verify(exactly = 1) { årsavregningService.oppdaterEksisterendeÅrsavregning(2L) }
        }

        @Test
        fun `utfører ingenting når ingen åpne årsavregninger finnes`() {
            val saksnummer = "123456789"
            val prosessinstans = Prosessinstans().apply {
                setData(ProsessDataKey.SAKSNUMMER, saksnummer)
                behandling = Behandling().apply {
                    id = 1L
                    type = Behandlingstyper.NY_VURDERING
                }
            }

            every {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            } returns emptyList()

            oppdaterÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 1) {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            }
            verify(exactly = 0) { årsavregningService.oppdaterEksisterendeÅrsavregning(any()) }
        }

        @Test
        fun `håndterer årsavregninger med forskjellige år`() {
            val saksnummer = "987654321"
            val prosessinstans = Prosessinstans().apply {
                setData(ProsessDataKey.SAKSNUMMER, saksnummer)
                behandling = Behandling().apply {
                    id = 1L
                    type = Behandlingstyper.NY_VURDERING
                }
            }

            val årsavregning2021 = Årsavregning().apply {
                id = 10L
                aar = 2021
            }
            val årsavregning2022 = Årsavregning().apply {
                id = 20L
                aar = 2022
            }
            val årsavregning2023 = Årsavregning().apply {
                id = 30L
                aar = 2023
            }

            val årsavregninger = listOf(årsavregning2021, årsavregning2022, årsavregning2023)

            every {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            } returns årsavregninger

            every { årsavregningService.oppdaterEksisterendeÅrsavregning(any()) } returns ÅrsavregningModel(
                årsavregningID = 1L,
                år = 2022,
                tidligereGrunnlag = null,
                tidligereAvgift = emptyList(),
                nyttGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = null,
                beregnetAvgiftBelop = null,
                tilFaktureringBeloep = null,
                harTrygdeavgiftFraAvgiftssystemet = null,
                trygdeavgiftFraAvgiftssystemet = null,
                manueltAvgiftBeloep = null,
                tidligereTrygdeavgiftFraAvgiftssystemet = null,
                tidligereÅrsavregningmanueltAvgiftBeloep = null,
                harSkjoennsfastsattInntektsgrunnlag = false
            )

            oppdaterÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 1) { årsavregningService.oppdaterEksisterendeÅrsavregning(10L) }
            verify(exactly = 1) { årsavregningService.oppdaterEksisterendeÅrsavregning(20L) }
            verify(exactly = 1) { årsavregningService.oppdaterEksisterendeÅrsavregning(30L) }
        }
    }
}
