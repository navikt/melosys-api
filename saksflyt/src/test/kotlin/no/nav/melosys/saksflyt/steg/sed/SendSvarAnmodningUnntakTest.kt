package no.nav.melosys.saksflyt.steg.sed

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.dokument.sed.EessiService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SendSvarAnmodningUnntakTest {

    @MockK(relaxUnitFun = true)
    private lateinit var eessiService: EessiService

    private lateinit var sendSvarAnmodningUnntak: SendSvarAnmodningUnntak

    @BeforeEach
    fun setup() {
        sendSvarAnmodningUnntak = SendSvarAnmodningUnntak(eessiService)
    }

    @Test
    fun utfør() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = BEHANDLING_ID
            }
            medData(ProsessDataKey.YTTERLIGERE_INFO_SED, YTTERLIGERE_INFO)
        }


        sendSvarAnmodningUnntak.utfør(prosessinstans)


        verify { eessiService.sendAnmodningUnntakSvar(BEHANDLING_ID, YTTERLIGERE_INFO) }
    }

    companion object {
        private const val BEHANDLING_ID = 1L
        private const val YTTERLIGERE_INFO = "Fritekst her"
    }
}
