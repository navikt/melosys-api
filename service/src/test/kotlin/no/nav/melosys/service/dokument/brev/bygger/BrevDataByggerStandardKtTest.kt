package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Test

class BrevDataByggerStandardKtTest {

    @Test
    fun `lagBrevData`() {
        val brevbestillingDto = BrevbestillingDto()
        brevbestillingDto.mottaker = Mottakerroller.BRUKER
        brevbestillingDto.fritekst = "FRITEKST"

        val brevDataByggerStandard = BrevDataByggerStandard(brevbestillingDto)

        val saksbehandler = "Z123456"
        val brevData = brevDataByggerStandard.lag(null, saksbehandler)
        brevData.shouldBeInstanceOf<BrevData>()
        brevData.saksbehandler shouldBe saksbehandler
        brevData.fritekst shouldBe brevbestillingDto.fritekst
        brevData.begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
    }
} 