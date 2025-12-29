package no.nav.melosys.service.sak

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatTestFactory
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.helseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class TrygdeavgiftServiceTest {

    private val BEHANDLING_ID = 123L
    private val BEHANDLINGSRESULTAT_ID = 123L

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var trygdeavgiftService: TrygdeavgiftService

    @BeforeEach
    fun setup() {
        val trygdeavgiftMottakerService = TrygdeavgiftMottakerService(behandlingsresultatService)
        trygdeavgiftService = TrygdeavgiftService(fagsakService, behandlingsresultatService, trygdeavgiftMottakerService)
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, uten avgiftsperioder, returnerer false`() {
        val fagsak = lagFagsak()
        val behandlingsresultat = Behandlingsresultat.forTest { id = BEHANDLINGSRESULTAT_ID }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, med avgiftsperioder men uten mndBeløp, returnerer false`() {
        val fagsak = lagFagsak()
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal.ZERO
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.now()
                    periodeTil = LocalDate.now()
                }
            }
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeAvgift_returnererFalse() {
        val fagsak = lagFagsak()
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal.ZERO
                    trygdesats = BigDecimal.ZERO
                    periodeFra = LocalDate.now()
                    periodeTil = LocalDate.now()
                }
            }
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, fagsak har ugyldig status, returnerer false`() {
        val fagsak = lagFagsak { status = Saksstatuser.HENLAGT }
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(30000.0)
                    trygdesats = BigDecimal(3.56)
                    periodeFra = LocalDate.now()
                    periodeTil = LocalDate.now()
                }
            }
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift med sjekk av fakturaserie, trygedavgiftsperioder men ikke bestilt faktura, returnerer false`() {
        val fagsak = lagFagsak()
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(2345.56)
                    trygdesats = BigDecimal(3.56)
                    periodeFra = LocalDate.now()
                    periodeTil = LocalDate.now()
                }
            }
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER, true).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperioderBådeMedOgUtenAvgift_returnererTrue() {
        val fagsak = lagFagsak()
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal.ZERO
                    trygdesats = BigDecimal.ZERO
                }
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(2345.56)
                    trygdesats = BigDecimal(3.56)
                }
            }
        }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeTrue()
    }

    @Test
    fun `harFakturerbarTrygdeavgift, trygdeavgift + betaler til NAV, true`() {
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagSkatteforholdTilNorge { skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG }
                    grunnlagInntekstperiode { avgiftspliktigMndInntekt = Penger(5000.0) }
                }
            }
        }

        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat).shouldBeTrue()
    }

    @Test
    fun `harFakturerbarTrygdeavgift, ingen trygdeavgift, false`() {
        val behandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal.ZERO
                    trygdesats = BigDecimal.ZERO
                    periodeFra = LocalDate.now()
                    periodeTil = LocalDate.now()
                }
            }
        }

        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat).shouldBeFalse()
    }

    @Test
    fun `harFakturerbarTrygdeavgift - EØS pensjonist, trygdeavgift + betaler til NAV, true`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLINGSRESULTAT_ID
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2023, 1, 1)
                tomDato = LocalDate.of(2023, 5, 1)
                bostedLandkode = Land_iso2.NO
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2023, 1, 1)
                    periodeTil = LocalDate.of(2023, 5, 1)
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagSkatteforholdTilNorge { skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG }
                    grunnlagInntekstperiode { avgiftspliktigMndInntekt = Penger(5000.0) }
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSRESULTAT_ID) } returns behandlingsresultat

        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat).shouldBeTrue()
    }

    private fun lagFagsak(
        init: FagsakTestFactory.Builder.() -> Unit = {}
    ): Fagsak = Fagsak.forTest {
        behandling { id = BEHANDLING_ID }
        init()
    }

    private fun lagBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        id = BEHANDLINGSRESULTAT_ID
        init()
    }
}
