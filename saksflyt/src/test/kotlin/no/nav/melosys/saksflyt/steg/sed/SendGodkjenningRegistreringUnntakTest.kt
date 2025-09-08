package no.nav.melosys.saksflyt.steg.sed

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.dokument.sed.EessiService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SendGodkjenningRegistreringUnntakTest {

    @MockK(relaxUnitFun = true)
    private lateinit var eessiService: EessiService

    private lateinit var sendGodkjenningRegistreringUnntak: SendGodkjenningRegistreringUnntak

    @BeforeEach
    fun setUp() {
        sendGodkjenningRegistreringUnntak = SendGodkjenningRegistreringUnntak(eessiService)
    }

    @Test
    fun `varsleUtland skalVarsles og rett behandlingstema forvent sed sendt`() {
        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.VARSLE_UTLAND, true)
        }


        sendGodkjenningRegistreringUnntak.utfør(prosessinstans)


        verify { eessiService.sendGodkjenningArbeidFlereLand(any(), FRITEKST) }
    }

    @Test
    fun `varsleUtland send A012 ikke valgt av saksbehandler forvent ingen sed sendt`() {
        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.VARSLE_UTLAND, false)
        }


        sendGodkjenningRegistreringUnntak.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.sendGodkjenningArbeidFlereLand(any(), FRITEKST) }
    }

    @Test
    fun `varsleUtland utland ikke utpekt forvent ingen sed sendt`() {
        val prosessinstans = lagProsessinstans()
        prosessinstans.hentBehandling.tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING


        sendGodkjenningRegistreringUnntak.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.sendGodkjenningArbeidFlereLand(any(), FRITEKST) }
    }

    private fun lagProsessinstans() = Prosessinstans.forTest {
        behandling {
            id = 1L
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        }
        medData(ProsessDataKey.YTTERLIGERE_INFO_SED, FRITEKST)
    }

    companion object {
        private const val FRITEKST = "Fritekst her"
    }
}
