package no.nav.melosys.saksflyt.steg.saksopplysninger

import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.saksopplysninger.PersonopplysningerLagrer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class LagrePersonopplysningerTest {

    @RelaxedMockK
    lateinit var personopplysningerLagrer: PersonopplysningerLagrer

    private lateinit var lagrePersonopplysninger: LagrePersonopplysninger

    private val behandlingId = 123L
    private val prosessinstans = Prosessinstans.forTest()
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        lagrePersonopplysninger = LagrePersonopplysninger(
            personopplysningerLagrer
        )

        behandling = Behandling.forTest {
            id = behandlingId
            fagsak = Fagsak.forTest { medBruker() }
        }

        prosessinstans.behandling = behandling
    }

    @Test
    fun `inngangsSteg returnerer LAGRE_PERSONOPPLYSNINGER`() {
        lagrePersonopplysninger.inngangsSteg() shouldBe ProsessSteg.LAGRE_PERSONOPPLYSNINGER
    }

    @Test
    fun `utfør kaller personopplysningerLagrer med behandlingId`() {
        lagrePersonopplysninger.utfør(prosessinstans)

        verify { personopplysningerLagrer.lagreHvisMangler(behandlingId) }
    }
}
