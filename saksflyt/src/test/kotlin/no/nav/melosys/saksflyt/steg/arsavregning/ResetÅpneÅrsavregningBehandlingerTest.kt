package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ResetÅpneÅrsavregningBehandlingerTest {

    @RelaxedMockK
    private lateinit var årsavregningService: ÅrsavregningService

    private lateinit var resetÅpneÅrsavregningBehandlinger: ResetÅpneÅrsavregningBehandlinger

    @BeforeEach
    fun setup() {
        resetÅpneÅrsavregningBehandlinger = ResetÅpneÅrsavregningBehandlinger(årsavregningService)
    }

    @Nested
    inner class InngangsSteg {
        @Test
        fun `returnerer riktig prosess steg`() {
            resetÅpneÅrsavregningBehandlinger.inngangsSteg() shouldBe ProsessSteg.RESET_ÅPNE_ÅRSAVREGNINGER
        }
    }

    @Nested
    inner class Utfør {

        @Test
        fun `gjør ingenting hvis behandlingen ikke er en ny vurdering`() {
            val prosessinstans = Prosessinstans.forTest {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.FØRSTEGANG
                }
            }

            resetÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 0) { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) }
            verify(exactly = 0) { årsavregningService.resetEksisterendeÅrsavregning(any()) }
        }

        @Test
        fun `oppdaterer alle åpne årsavregninger på fagsak`() {
            val saksnummer = "123456789"
            val prosessinstans = Prosessinstans.forTest {
                type = ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING
                status = ProsessStatus.KLAR
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.NY_VURDERING
                    fagsak {
                        this.saksnummer = saksnummer
                    }
                }
            }


            val årsavregning1 = Årsavregning.forTest {
                id = 1L
                aar = 2022
            }
            val årsavregning2 = Årsavregning.forTest {
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

            every { årsavregningService.resetEksisterendeÅrsavregning(any()) } returns ÅrsavregningModel(
                årsavregningID = 1L,
                år = 2022,
                tidligereTrygdeavgiftsGrunnlag = null,
                tidligereAvgift = emptyList(),
                nyttTrygdeavgiftsGrunnlag = null,
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

            resetÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 1) {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            }
            verify(exactly = 1) { årsavregningService.resetEksisterendeÅrsavregning(1L) }
            verify(exactly = 1) { årsavregningService.resetEksisterendeÅrsavregning(2L) }
        }

        @Test
        fun `utfører ingenting når ingen åpne årsavregninger finnes`() {
            val saksnummer = "123456789"
            val fagsak = Fagsak.forTest { this.saksnummer = saksnummer }
            val prosessinstans = Prosessinstans.forTest {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.NY_VURDERING
                    this.fagsak = fagsak
                }
            }

            every {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            } returns emptyList()

            resetÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 1) {
                årsavregningService.finnÅrsavregningerPåFagsak(
                    saksnummer,
                    null,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
            }
            verify(exactly = 0) { årsavregningService.resetEksisterendeÅrsavregning(any()) }
        }

        @Test
        fun `håndterer årsavregninger med forskjellige år`() {
            val saksnummer = "987654321"
            val fagsak = Fagsak.forTest { this.saksnummer = saksnummer }
            val prosessinstans = Prosessinstans.forTest {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.NY_VURDERING
                    this.fagsak = fagsak
                }
            }

            val årsavregning2021 = Årsavregning.forTest {
                id = 10L
                aar = 2021
            }
            val årsavregning2022 = Årsavregning.forTest {
                id = 20L
                aar = 2022
            }
            val årsavregning2023 = Årsavregning.forTest {
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

            every { årsavregningService.resetEksisterendeÅrsavregning(any()) } returns ÅrsavregningModel(
                årsavregningID = 1L,
                år = 2022,
                tidligereTrygdeavgiftsGrunnlag = null,
                tidligereAvgift = emptyList(),
                nyttTrygdeavgiftsGrunnlag = null,
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

            resetÅpneÅrsavregningBehandlinger.utfør(prosessinstans)

            verify(exactly = 1) { årsavregningService.resetEksisterendeÅrsavregning(10L) }
            verify(exactly = 1) { årsavregningService.resetEksisterendeÅrsavregning(20L) }
            verify(exactly = 1) { årsavregningService.resetEksisterendeÅrsavregning(30L) }
        }
    }
}
