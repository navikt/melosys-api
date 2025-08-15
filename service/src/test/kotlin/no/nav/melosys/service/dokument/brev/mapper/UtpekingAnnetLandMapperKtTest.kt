package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtpekingAnnetLandMapperKtTest {
    private lateinit var utpekingAnnetLandMapper: UtpekingAnnetLandMapper

    @BeforeEach
    fun setUp() {
        utpekingAnnetLandMapper = UtpekingAnnetLandMapper()
    }

    @Test
    fun mapTilBrevXML() {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevDataUtpekingAnnetLand = lagDataUtpekingAnnetLand()
        val behandling = Behandling.forTest { }


        val brevXML = utpekingAnnetLandMapper.mapTilBrevXML(
            fellesType, navFelles, behandling, Behandlingsresultat(), brevDataUtpekingAnnetLand
        )


        brevXML.run {
            shouldContain(Landkoder.EE.beskrivelse)
            shouldContain(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3.kode)
        }
    }

    private fun lagDataUtpekingAnnetLand() =
        BrevDataUtpekingAnnetLand(BrevbestillingDto(), "Saksbehandler").apply {
            utpekingsperiode = Utpekingsperiode(
                LocalDate.now(), null, Land_iso2.EE,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, null
            )
        }
}
