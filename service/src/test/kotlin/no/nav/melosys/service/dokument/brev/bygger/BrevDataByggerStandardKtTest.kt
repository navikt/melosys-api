package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Test

class BrevDataByggerStandardKtTest {

    @Test
    fun `lag brevData skal opprette standard brevdata`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            mottaker = Mottakerroller.BRUKER
            fritekst = "FRITEKST"
        }

        val brevDataByggerStandard = BrevDataByggerStandard(brevbestillingDto)
        val saksbehandler = "Z123456"


        val brevData = brevDataByggerStandard.lag(null, saksbehandler)


        brevData.run {
            shouldBeInstanceOf<BrevData>()
            this.saksbehandler shouldBe saksbehandler
            fritekst shouldBe brevbestillingDto.fritekst
            begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
        }
    }
} 