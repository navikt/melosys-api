package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.service.dokument.brev.BrevDataVideresend
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import org.junit.jupiter.api.Test

class VideresendSoknadMapperKtTest {

    private val instans = VideresendSoknadMapper()

    @Test
    fun mapTilBrevXML() {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()

        val brevdata = lagBrevDataVideresend()
        val resultat = instans.mapTilBrevXML(
            fellesType, navFelles,
            BehandlingTestFactory.builderWithDefaults().build(),
            Behandlingsresultat(), brevdata
        )
        resultat shouldMatch "(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\\n.*"
    }

    private fun lagBrevDataVideresend(): BrevDataVideresend {
        val brevDataVideresend = BrevDataVideresend(BrevbestillingDto(), "Saksbehandler")
        brevDataVideresend.bostedsland = Landkoder.NO.beskrivelse

        val utenlandskMyndighet = UtenlandskMyndighet()
        utenlandskMyndighet.navn = "Försäkringskassan"
        utenlandskMyndighet.gateadresse1 = "Box 1164"
        utenlandskMyndighet.postnummer = "SE-621 22"
        utenlandskMyndighet.poststed = "Visby"
        utenlandskMyndighet.land = "Sverige"
        utenlandskMyndighet.landkode = Land_iso2.SE
        brevDataVideresend.trygdemyndighet = utenlandskMyndighet
        return brevDataVideresend
    }
}
