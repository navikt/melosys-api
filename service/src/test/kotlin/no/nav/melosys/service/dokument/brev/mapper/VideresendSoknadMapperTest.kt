package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.service.dokument.brev.BrevDataVideresend
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import org.junit.jupiter.api.Test

class VideresendSoknadMapperTest {

    private val instans = VideresendSoknadMapper()

    @Test
    fun mapTilBrevXML() {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevdata = lagBrevDataVideresend()
        val behandling = Behandling.forTest { }


        val resultat = instans.mapTilBrevXML(
            fellesType, navFelles, behandling, Behandlingsresultat(), brevdata
        )


        resultat shouldMatch "(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\\n.*"
    }

    private fun lagBrevDataVideresend() =
        BrevDataVideresend(BrevbestillingDto(), "Saksbehandler").apply {
            bostedsland = Landkoder.NO.beskrivelse
            trygdemyndighet = UtenlandskMyndighet().apply {
                navn = "Försäkringskassan"
                gateadresse1 = "Box 1164"
                postnummer = "SE-621 22"
                poststed = "Visby"
                land = "Sverige"
                landkode = Land_iso2.SE
            }
        }
}
