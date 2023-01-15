package no.nav.melosys.itest

import io.mockk.impl.annotations.RelaxedMockK
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.MottatteOpplysningerRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(FakeUnleash::class)
class MottatteOpplysningerHeleContextSimpleIT(
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val mottatteOpplysningerRepository: MottatteOpplysningerRepository,
    @Autowired private val unleash: FakeUnleash,
) : ComponentTestBase() {

    @RelaxedMockK
    private lateinit var joarkFasade: JoarkFasade

    private val mottatteOpplysningerService by lazy {
        MottatteOpplysningerService(mottatteOpplysningerRepository, behandlingService, joarkFasade, unleash)
    }

    private val behandlingService by lazy {
        BehandlingService(
            behandlingRepository, null, null,
            null, null, null,
            UtledMottaksdato(joarkFasade), unleash
        )
    }

    @Test
    fun `legg til mottate opplysinger på eksisterende behanding`() {
        unleash.enable(ToggleName.FOLKETRYGDEN_MVP)

        val behandlingID: Long = 61 // Velg en id som du alt har i databasen - lag med MottatteOpplysningerIT

        val behandling = behandlingRepository.findById(behandlingID).get()
        // kaster InvalidDataAccessApiUsageException: detached entity passed to persist
        mottatteOpplysningerService.opprettSøknad(behandling, Periode(), Soeknadsland())
    }
}
