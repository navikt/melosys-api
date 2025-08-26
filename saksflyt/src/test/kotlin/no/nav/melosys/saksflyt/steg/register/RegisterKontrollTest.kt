package no.nav.melosys.saksflyt.steg.register

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class RegisterKontrollTest {

    @MockK
    private lateinit var ufmKontrollService: UfmKontrollService

    private lateinit var registerKontroll: RegisterKontroll

    @BeforeEach
    fun setup() {
        every { ufmKontrollService.utførKontrollerOgRegistrerFeil(any()) } just Runs
        registerKontroll = RegisterKontroll(ufmKontrollService)
    }

    @Test
    fun utfør() {
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = 1L
            }
        }


        registerKontroll.utfør(prosessinstans)


        verify { ufmKontrollService.utførKontrollerOgRegistrerFeil(1L) }
    }
}
