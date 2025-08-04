package no.nav.melosys.service.dokument.brev.bygger

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Bostedsland
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrevDataByggerVideresendKtTest {

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    @MockK
    private lateinit var brevDataGrunnlag: BrevDataGrunnlag

    private lateinit var brevDataByggerVideresend: BrevDataByggerVideresend

    @BeforeEach
    fun setUp() {
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .build()
        every { brevDataGrunnlag.behandling } returns behandling
        every { brevDataGrunnlag.mottatteOpplysningerData } returns null

        brevDataByggerVideresend = BrevDataByggerVideresend(landvelgerService, utenlandskMyndighetService, BrevbestillingDto())
    }

    @Test
    fun lag_medBostedSverigeOgTrygdemyndighetslandSverige_girBrevdata() {
        every { landvelgerService.hentBostedsland(eq(1L), any()) } returns Bostedsland(Landkoder.SE)

        val utenlandskMyndighet = UtenlandskMyndighet()
        utenlandskMyndighet.navn = "Försäkringskassan"
        utenlandskMyndighet.gateadresse1 = "Box 1164"
        utenlandskMyndighet.postnummer = "SE-621 22"
        utenlandskMyndighet.poststed = "Visby"
        utenlandskMyndighet.land = "Sverige"
        utenlandskMyndighet.landkode = Land_iso2.SE
        every { utenlandskMyndighetService.hentUtenlandskMyndighet(eq(Land_iso2.SE)) } returns utenlandskMyndighet

        brevDataByggerVideresend.lag(brevDataGrunnlag, "Saksbehandler")
    }
}
