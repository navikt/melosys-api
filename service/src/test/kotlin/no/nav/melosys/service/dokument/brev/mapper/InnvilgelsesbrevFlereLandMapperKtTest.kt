package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import no.nav.dok.melosysbrev._000108.SakstypeKode
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InnvilgelsesbrevFlereLandMapperKtTest {

    private val instans = InnvilgelsesbrevFlereLandMapper()

    @Test
    fun `test SakstypeKode`() {
        val koderSomIkkeErAktuelleForBrev = listOf(
            "UKJENT" // Det er ikke aktuelt med brev for denne
        )

        hentAlleVerdierFraKodeverk(Sakstyper::class.java)
            .filter { k -> !koderSomIkkeErAktuelleForBrev.contains(k) }
            .forEach { SakstypeKode.fromValue(it) }
    }

    @Test
    fun `mapTilBrevXml gir ikke tom XML streng`() {
        val behandling = lagBehandling(lagFagsak())
        val behandlingsresultat = lagBehandlingsresultat(setOf(lagLovvalgsperiode()))
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevdataInnvilgelse = lagBrevdataInnvilgelse()

        val resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse)
        
        resultat shouldMatch """(?s)<\?xml version="\d\.\d+" .*>\n.*"""
    }

    private fun lagBrevdataInnvilgelse(): BrevDataInnvilgelseFlereLand {
        val norskeVirksomheter = listOf(
            AvklartVirksomhet("Telenor", "1234", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
        )

        return BrevDataInnvilgelseFlereLand(BrevbestillingDto(), "SAKSBEHANDLER").apply {
            lovvalgsperiode = lagLovvalgsperiode()
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

    companion object {
        private fun lagBrevdataA1(virksomheter: List<AvklartVirksomhet>): BrevDataA1 {
            return BrevDataA1().apply {
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
        }

        private fun lagBehandlingsresultat(perioder: Set<Lovvalgsperiode>): Behandlingsresultat {
            return Behandlingsresultat().apply {
                lovvalgsperioder = perioder
            }
        }

        private fun lagLovvalgsperiode(): Lovvalgsperiode {
            return lagLovvalgsperiode(LocalDate.now())
        }

        private fun lagLovvalgsperiode(fom: LocalDate): Lovvalgsperiode {
            return Lovvalgsperiode().apply {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
                this.fom = fom
                tom = LocalDate.now()
                lovvalgsland = Land_iso2.AT
                tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            }
        }

        private fun lagFagsak(): Fagsak {
            return Fagsak(
                "MEL-test",
                123L,
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Saksstatuser.OPPRETTET,
                null,
                emptySet(),
                emptyList()
            )
        }

        private fun lagBehandling(fagsak: Fagsak): Behandling {
            return BehandlingTestFactory.builderWithDefaults()
                .medType(Behandlingstyper.FØRSTEGANG)
                .medFagsak(fagsak)
                .build()
        }
    }
}