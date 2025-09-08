package no.nav.melosys.saksflyt.steg.medl

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.medl.MedlPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvsluttTidligereMedlPeriodeTest {

    @MockK
    private lateinit var medlPeriodeService: MedlPeriodeService

    private lateinit var avsluttTidligereMedlPeriode: AvsluttTidligereMedlPeriode

    @BeforeEach
    fun setUp() {
        every { medlPeriodeService.avsluttTidligerMedlPeriode(any()) } just Runs

        avsluttTidligereMedlPeriode = AvsluttTidligereMedlPeriode(medlPeriodeService)
    }

    @Test
    fun `utfør ikke endring verifiser lagre lovvalgsperiode`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = 1L
            }
            medData(ProsessDataKey.ER_OPPDATERT_SED, false)
            medData(ProsessDataKey.BRUKER_ID, "12312322")
        }


        avsluttTidligereMedlPeriode.utfør(prosessinstans)
    }

    @Test
    fun `utfør er endring verifiser avslutt tidligere Medl periode`() {

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                fagsak { }
            }
            medData(ProsessDataKey.ER_OPPDATERT_SED, true)
            this.medData(ProsessDataKey.BRUKER_ID, "12312322")
        }


        avsluttTidligereMedlPeriode.utfør(prosessinstans)


        verify { medlPeriodeService.avsluttTidligerMedlPeriode(FagsakTestFactory.SAKSNUMMER) }
    }
}
