package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.avklartefakta
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.lovvalgsperiodeForTest
import no.nav.melosys.domain.personDokument
import no.nav.melosys.domain.saksopplysning
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InnvilgelseArbeidsgiverBrevMapperTest {

    private val instans = InnvilgelseArbeidsgiverMapper()

    @Test
    fun `mapArbeidsLandSammensattNavnLovvalgsperiodeFraSøkandTilBrevXml gir ikke tom XML streng`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                fom = LocalDate.now()
                tom = LocalDate.now()
                lovvalgsland = Land_iso2.AT
            }
            avklartefakta {
                type = Avklartefaktatyper.VIRKSOMHET
                fakta = "TRUE"
                subjekt = "123456789"
            }
        }

        testMapTilBrevXml(behandlingsresultat)
    }

    private fun testMapTilBrevXml(behandlingsresultat: Behandlingsresultat) =
        testMapTilBrevXml(lagBehandling(), behandlingsresultat)

    private fun testMapTilBrevXml(behandling: Behandling, behandlingsresultat: Behandlingsresultat) {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevDataInnvilgelse = BrevDataInnvilgelse(BrevbestillingDto(), "Z123456").apply {
            arbeidsland = "Sverige"
            hovedvirksomhet = AvklartVirksomhet("Equinor", "987654321", null, Yrkesaktivitetstyper.LOENNET_ARBEID)
            lovvalgsperiode = lovvalgsperiodeForTest {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                fom = LocalDate.now()
                tom = LocalDate.now()
                lovvalgsland = Land_iso2.AT
            }
            personNavn = "For Etter"
        }

        val resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevDataInnvilgelse)

        resultat shouldMatch """(?s)\<\?xml version="\d\.\d+" .*>\n.*"""
        resultat shouldContain ":navn>For Etter</ns"
    }

    private fun lagBehandling() = Behandling.forTest {
        type = Behandlingstyper.FØRSTEGANG
        saksopplysning {
            type = SaksopplysningType.PERSOPL
            personDokument {
                kjønn = no.nav.melosys.domain.dokument.person.KjoennsType("U")
                fornavn = "For"
                etternavn = "Etter"
                sammensattNavn = "For Etter"
                statsborgerskap = Land(Land.BELGIA)
                fødselsdato = LocalDate.ofYearDay(1900, 1)
            }
        }
    }
}
