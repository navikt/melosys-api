package no.nav.melosys.saksflyt.steg.medl

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.medl.MedlAnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvsluttTidligereMedlAnmodningsperiodeTest {
    @MockK
    private lateinit var medlAnmodningsperiodeService: MedlAnmodningsperiodeService

    private lateinit var avsluttTidligereMedlAnmodningsperiode: AvsluttTidligereMedlAnmodningsperiode

    @BeforeEach
    fun setUp() {
        every { medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(any()) } just Runs

        avsluttTidligereMedlAnmodningsperiode = AvsluttTidligereMedlAnmodningsperiode(medlAnmodningsperiodeService)
    }

    @Test
    fun `utfør mottar oppdatert A001 kaller på avsluttTidligereAnmodningsperiode`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            }
            medData(ProsessDataKey.ER_OPPDATERT_SED, true)
        }


        avsluttTidligereMedlAnmodningsperiode.utfør(prosessinstans)


        verify { medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(any()) }
    }

    @Test
    fun `utfør mottar ny A001 kaller ikke på avsluttTidligereAnmodningsperiode`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            }
            medData(ProsessDataKey.ER_OPPDATERT_SED, false)
        }


        avsluttTidligereMedlAnmodningsperiode.utfør(prosessinstans)


        verify(exactly = 0) { medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(any()) }
    }

    @Test
    fun `utfør mottar oppdatert A009 kaller ikke på avsluttTidligereAnmodningsperiode`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
            }
            medData(ProsessDataKey.ER_OPPDATERT_SED, true)
        }


        avsluttTidligereMedlAnmodningsperiode.utfør(prosessinstans)


        verify(exactly = 0) { medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(any()) }
    }
}
