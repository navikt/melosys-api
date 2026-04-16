package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldContain
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
            fellesType, navFelles, behandling, Behandlingsresultat.forTest { }, brevdata
        )


        resultat shouldMatch "(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\\n.*"
    }

    @Test
    fun `fritekst med linjeskift konverteres til Metaforce-format`() {
        val brevdata = lagBrevDataVideresend().apply {
            fritekst = "Linje 1\nLinje 2"
        }

        val xml = instans.mapTilBrevXML(
            lagFellesType(), lagNAVFelles(), Behandling.forTest { }, Behandlingsresultat.forTest { }, brevdata
        )

        xml shouldContain "Linje 1[_¶_]Linje 2"
    }

    private fun lagBrevDataVideresend() =
        BrevDataVideresend(BrevbestillingDto(), "Saksbehandler").apply {
            bostedsland = Landkoder.NO.beskrivelse
            trygdemyndighet = utenlandskMyndighetForTest {
                navn = "Försäkringskassan"
                gateadresse1 = "Box 1164"
                postnummer = "SE-621 22"
                poststed = "Visby"
                land = "Sverige"
                landkode = Land_iso2.SE
            }
        }

    /**
     * Simple DSL helper for creating UtenlandskMyndighet test instances.
     * UtenlandskMyndighet is a JPA entity without a dedicated TestFactory.
     */
    private fun utenlandskMyndighetForTest(
        init: UtenlandskMyndighet.() -> Unit
    ): UtenlandskMyndighet = UtenlandskMyndighet().apply(init)
}
