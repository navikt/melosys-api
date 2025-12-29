package no.nav.melosys.saksflyt.steg.sed

import io.getunleash.FakeUnleash
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.integrasjon.eessi.EessiConsumer
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.dokument.sed.SedDataGrunnlagFactory
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SendAvslagUtpekingTest {
    @MockK
    private lateinit var sedDataBygger: SedDataBygger

    @MockK
    private lateinit var sedDataGrunnlagFactory: SedDataGrunnlagFactory

    @MockK
    private lateinit var eessiConsumer: EessiConsumer

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var sendAvslagUtpeking: SendAvslagUtpeking
    private lateinit var eessiService: EessiService

    private val fakeUnleash = FakeUnleash()

    @BeforeEach
    fun settOpp() {
        eessiService = EessiService(
            behandlingService, behandlingsresultatService, eessiConsumer, joarkFasade,
            sedDataBygger, sedDataGrunnlagFactory, fakeUnleash
        )
        sendAvslagUtpeking = SendAvslagUtpeking(eessiService)
    }

    @Test
    fun utfør() {
        val behandling = lagBehandling()

        every { sedDataBygger.lagUtkast(any(), any(), any()) } returns SedDataDto()
        every { sedDataGrunnlagFactory.av(any()) } returns mockk()
        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat.forTest { }
        every { eessiConsumer.sendSedPåEksisterendeBuc(any(), any(), any()) } returns Unit

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = 1L }
            medData(
                ProsessDataKey.UTPEKING_AVVIS, UtpekingAvvis(
                    "begrunnelse", true,
                    "DK", "fritekst"
                )
            )
        }

        sendAvslagUtpeking.utfør(prosessinstans)

        verify { eessiConsumer.sendSedPåEksisterendeBuc(any(), any(), any()) }
    }

    private fun lagBehandling() = Behandling.forTest {
        id = 1L
        fagsak {
            medGsakSaksnummer()
        }
        saksopplysning {
            type = SaksopplysningType.SEDOPPL
            sedDokument {
                this@sedDokument.lovvalgsperiode(LocalDate.now(), LocalDate.now())
                this@sedDokument.rinaSaksnummer = "rinaSaksnummer"
            }
        }
    }
}
