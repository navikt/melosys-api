package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import no.nav.dok.melosysbrev.felles.melosys_felles.KjoennKode
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
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
        val behandlingsresultat = lagBehandlingsresultat(
            setOf(lagLovvalgsperiode()),
            setOf(lagAvklarteFakta())
        )


        testMapTilBrevXml(behandlingsresultat)
    }

    private fun testMapTilBrevXml(behandlingsresultat: Behandlingsresultat) =
        testMapTilBrevXml(lagBehandling(FagsakTestFactory.lagFagsak()), behandlingsresultat)

    private fun testMapTilBrevXml(behandling: Behandling, behandlingsresultat: Behandlingsresultat) {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevDataInnvilgelse = BrevDataInnvilgelse(BrevbestillingDto(), "Z123456").apply {
            arbeidsland = "Sverige"
            hovedvirksomhet = AvklartVirksomhet("Equinor", "987654321", null, Yrkesaktivitetstyper.LOENNET_ARBEID)
            lovvalgsperiode = lagLovvalgsperiode()
            personNavn = "For Etter"
        }

        val resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevDataInnvilgelse)

        resultat shouldMatch """(?s)\<\?xml version="\d\.\d+" .*>\n.*"""
        resultat shouldContain ":navn>For Etter</ns"
    }

    private fun lagBehandlingsresultat(perioder: Set<Lovvalgsperiode>, fakta: Set<Avklartefakta>) = Behandlingsresultat().apply {
        avklartefakta = fakta
        lovvalgsperioder = perioder
    }

    private fun lagLovvalgsperiode(): Lovvalgsperiode =
        lagLovvalgsperiode(LocalDate.now())

    private fun lagLovvalgsperiode(fom: LocalDate) = Lovvalgsperiode().apply {
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        this.fom = fom
        tom = LocalDate.now()
        lovvalgsland = Land_iso2.AT
    }

    private fun lagAvklarteFakta() = Avklartefakta().apply {
        type = Avklartefaktatyper.VIRKSOMHET
        fakta = "TRUE"
        subjekt = "123456789"
    }

    private fun lagBehandling(fagsak: Fagsak): Behandling {
        val pdok = PersonDokument().apply {
            kjønn = KjoennsType(KjoennKode.U.name)
            fornavn = "For"
            etternavn = "Etter"
            sammensattNavn = "For Etter"
            statsborgerskap = Land(Land.BELGIA)
            fødselsdato = LocalDate.ofYearDay(1900, 1)
        }
        return lagBehandling(fagsak, setOf(lagSaksopplysning(SaksopplysningType.PERSOPL, pdok)))
    }

    private fun lagSaksopplysning(type: SaksopplysningType, dokument: SaksopplysningDokument) = Saksopplysning().apply {
        this.type = type
        this.dokument = dokument
    }

    private fun lagBehandling(fagsak: Fagsak, saksopplysninger: Set<Saksopplysning>) = Behandling.forTest {
        type = Behandlingstyper.FØRSTEGANG
        this.fagsak = fagsak
        this.saksopplysninger = saksopplysninger.toMutableSet()
    }
}
