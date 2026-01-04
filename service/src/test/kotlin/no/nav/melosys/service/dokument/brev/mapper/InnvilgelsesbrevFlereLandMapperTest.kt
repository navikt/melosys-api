package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import no.nav.dok.melosysbrev._000108.SakstypeKode
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.lovvalgsperiodeForTest
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.Companion.hentAlleVerdierFraKodeverk
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InnvilgelsesbrevFlereLandMapperTest {

    private val instans = InnvilgelsesbrevFlereLandMapper()

    @Test
    fun `test SakstypeKode`() {
        val koderSomIkkeErAktuelleForBrev = listOf(
            "UKJENT" // Det er ikke aktuelt med brev for denne
        )

        hentAlleVerdierFraKodeverk(Sakstyper::class)
            .filter { k -> !koderSomIkkeErAktuelleForBrev.contains(k) }
            .forEach { SakstypeKode.fromValue(it) }
    }

    @Test
    fun `mapTilBrevXml gir ikke tom XML streng`() {
        val lovvalgsperiodeEntity = lagLovvalgsperiode()
        val behandling = lagBehandling()
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
                fom = LocalDate.now()
                tom = LocalDate.now()
                lovvalgsland = Land_iso2.AT
                tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            }
        }
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevdataInnvilgelse = lagBrevdataInnvilgelse(lovvalgsperiodeEntity)

        val resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse)

        resultat shouldMatch """(?s)<\?xml version="\d\.\d+" .*>\n.*"""
    }

    private fun lagBrevdataInnvilgelse(lovvalgsperiode: no.nav.melosys.domain.Lovvalgsperiode): BrevDataInnvilgelseFlereLand {
        val norskeVirksomheter = listOf(
            AvklartVirksomhet("Telenor", "1234", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
        )

        return BrevDataInnvilgelseFlereLand(BrevbestillingDto(), "SAKSBEHANDLER").apply {
            this.lovvalgsperiode = lovvalgsperiode
            avklartMaritimTypeSkip = true
            avklartMaritimTypeSokkel = false
            arbeidsgivere = norskeVirksomheter
            bostedsland = "Norge"
            trydemyndighetsland = Landkoder.DE
            alleArbeidsland = listOf("Sverige", "Danmark", "Finland", "Spania")
            marginaltArbeid = true
            begrensetPeriode = true
            vedleggA1 = lagBrevdataA1(norskeVirksomheter)
        }
    }

    private fun lagBrevdataA1(virksomheter: List<AvklartVirksomhet>) = BrevDataA1().apply {
        person = lagPersonopplysninger()
        bostedsadresse = lagStrukturertAdresse()
        yrkesgruppe = Yrkesgrupper.ORDINAER
        hovedvirksomhet = virksomheter[0]
        val bivirksomheterList = ArrayList(virksomheter)
        bivirksomheterList.removeAt(0)
        bivirksomheter = bivirksomheterList
        arbeidssteder = ArrayList()
        arbeidsland = ArrayList()
    }

    private fun lagLovvalgsperiode(fom: LocalDate = LocalDate.now()) = lovvalgsperiodeForTest {
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
        this.fom = fom
        tom = LocalDate.now()
        lovvalgsland = Land_iso2.AT
        tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
    }

    private fun lagBehandling() = Behandling.forTest {
        type = Behandlingstyper.FØRSTEGANG
        fagsak { type = Sakstyper.EU_EOS }
    }
}
