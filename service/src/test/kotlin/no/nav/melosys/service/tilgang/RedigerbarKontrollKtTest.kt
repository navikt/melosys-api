package no.nav.melosys.service.tilgang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedigerbarKontrollKtTest {

    private lateinit var behandling: Behandling
    private val behandlingsresultat = Behandlingsresultat()

    private val behandlingsresultatService: BehandlingsresultatService = mockk()

    private lateinit var redigerbarKontroll: RedigerbarKontroll

    @BeforeEach
    fun setup() {
        behandling = Behandling.forTest {
            id = 11111L
        }
        redigerbarKontroll = RedigerbarKontroll(behandlingsresultatService)
    }

    @Test
    fun sjekkRessursRedigerbarOgTilgang_ukjentRessursBehandlingIkkeRedigerbar_kasterFeil() {
        behandling.status = Behandlingsstatus.AVSLUTTET
        val exception = shouldThrow<FunksjonellException> {
            redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.UKJENT)
        }
        exception.message shouldContain "ikke-redigerbar"
    }

    @Test
    fun sjekkRessursRedigerbarOgTilgang_behandlingRedigerbar_kasterIkkeFeil() {
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        // No exception should be thrown
        redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.UKJENT)
    }

    @Test
    fun sjekkRessursRedigerbarOgTilgang_endringAvklarteFaktaIkkeSendtAnmodningOmUnntak_kasterIkkeFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat

        // No exception should be thrown
        redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.AVKLARTE_FAKTA)
    }

    @Test
    fun sjekkRessursRedigerbarOgTilgang_endringAvklarteFaktaErSendtAnmodningOmUnntak_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat

        val anmodningsperiode = Anmodningsperiode().apply {
            setSendtUtland(true)
        }
        behandlingsresultat.anmodningsperioder.add(anmodningsperiode)

        val exception = shouldThrow<FunksjonellException> {
            redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.AVKLARTE_FAKTA)
        }
        exception.message shouldContain "Kan ikke endre"
    }
}
