package no.nav.melosys.service.saksopplysninger

import io.mockk.Called
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingEndretStatusEvent
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockKExtension::class)
class SaksopplysningEventListenerTest {

    @RelaxedMockK
    private lateinit var personopplysningerLagrer: PersonopplysningerLagrer

    private lateinit var saksoppplysningEventListener: SaksoppplysningEventListener

    @BeforeEach
    fun setUp() {
        saksoppplysningEventListener = SaksoppplysningEventListener(
            personopplysningerLagrer
        )
    }

    @Test
    fun `lagrePersonopplysninger kaller personopplysningerLagrer når status er AVSLUTTET`() {
        val behandling = lagBehandling(status = Behandlingsstatus.AVSLUTTET)

        val event = BehandlingEndretStatusEvent(Behandlingsstatus.AVSLUTTET, behandling)
        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { personopplysningerLagrer.lagreHvisMangler(behandling.id) }
    }

    @Test
    fun `lagrePersonopplysninger kaller personopplysningerLagrer når status er MIDLERTIDIG_LOVVALGSBESLUTNING`() {
        val behandling = lagBehandling(status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)

        val event = BehandlingEndretStatusEvent(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING, behandling)
        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { personopplysningerLagrer.lagreHvisMangler(behandling.id) }
    }

    @ParameterizedTest
    @EnumSource(
        value = Behandlingsstatus::class,
        names = ["AVSLUTTET", "MIDLERTIDIG_LOVVALGSBESLUTNING"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `lagrePersonopplysninger kaller IKKE personopplysningerLagrer for andre statuser`(status: Behandlingsstatus) {
        val behandling = lagBehandling(status = status)

        val event = BehandlingEndretStatusEvent(status, behandling)
        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { personopplysningerLagrer wasNot Called }
    }

    private fun lagBehandling(
        status: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING
    ) = Behandling.forTest {
        id = 1L
        this.status = status
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
            medGsakSaksnummer()
        }
        mottatteOpplysninger { }
    }
}
