package no.nav.melosys.saksflyt.steg.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarMyndighetTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    private lateinit var avklarMyndighet: AvklarMyndighet

    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setUp() {
        avklarMyndighet = AvklarMyndighet(behandlingService, behandlingsresultatService, utenlandskMyndighetService)

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_EOS
            status = ProsessStatus.KLAR
            behandling {
                id = 1L
                fagsak = Fagsak.forTest()
                type = Behandlingstyper.FØRSTEGANG
                mottatteOpplysninger {
                    soeknad {
                        landkoder("BE")
                        fysiskeArbeidssted {
                            landkode = "HR"
                        }
                        bostedLandkode = "IT"
                    }
                }
            }
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.behandling

    }

    @Test
    fun `utfør utenMyndighet myndighetOpprettes`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            lovvalgsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingsresultatTestFactory.DEFAULT_ID) } returns behandlingsresultat
        every { utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(any<Behandling>()) } just Runs


        avklarMyndighet.utfør(prosessinstans)


        verify { utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(any<Behandling>()) }
    }
}
