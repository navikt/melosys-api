package no.nav.melosys.saksflyt.steg.arsavregning

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
class OpprettArsavregningTest {
    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandslingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService

    @MockK
    private lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @MockK
    private lateinit var persondataService: PersondataService

    @MockK
    private lateinit var oppretteÅrsavregning: ÅrsavregningService


    @BeforeEach
    fun setUp() {
        val opprettArsavregning = OpprettArsavregning(
            fagsakService,
            persondataService,
            trygdeavgiftOppsummeringService,
            behandlingService,
            lovvalgsperiodeService,
            medlemskapsperiodeService,
            behandslingsresultatService,
            oppretteÅrsavregning
        )
    }


    @Test
    fun test() {

    }
}
