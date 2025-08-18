package no.nav.melosys.service.brev.bestilling

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.melosys.domain.brev.NorskMyndighet.*
import org.junit.jupiter.api.Test

class HentTilgjengeligeNorskeMyndigheterServiceTest {

    private val hentTilgjengeligeNorskeMyndigheterService = HentTilgjengeligeNorskeMyndigheterService()

    @Test
    fun `hentTilgjengeligeNorskeMyndigheter skal inneholde bare støttede norske myndigheter`() {
        val tilgjengeligeNorskeMyndigheter = hentTilgjengeligeNorskeMyndigheterService.hentTilgjengeligeNorskeMyndigheter()


        tilgjengeligeNorskeMyndigheter shouldContainExactly listOf(
            SKATTEETATEN, SKATTEINNKREVER_UTLAND, HELFO
        )
    }
}
