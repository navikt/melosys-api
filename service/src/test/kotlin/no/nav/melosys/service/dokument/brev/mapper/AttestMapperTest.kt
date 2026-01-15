package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagMaritimtArbeidssted
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AttestMapperTest {

    private val mapper = AttestMapper()
    private val easyRandom: EasyRandom = EasyRandomConfigurer.randomForDokProd()

    @Test
    fun `mapTilBrevXML should generate XML successfully`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
        }

        val boAdresse = StrukturertAdresse(
            gatenavn = "HjemmeGata",
            husnummerEtasjeLeilighet = "25",
            postnummer = "0165",
            poststed = "Poststed",
            region = "Region",
            landkode = Landkoder.NO.kode
        )

        val behandling = Behandling.forTest {
            id = 1L
            fagsak { }
        }

        val virksomhetAdresse = StrukturertAdresse(
            gatenavn = "Gatenavn",
            husnummerEtasjeLeilighet = "25",
            postnummer = "0165",
            poststed = "Poststed",
            region = "Region",
            landkode = Landkoder.NO.kode
        )

        val virksomhet = AvklartVirksomhet(
            "JARLSBERG INTERNATIONAL",
            "123456789",
            virksomhetAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )

        val utenlandskVirksomhet = AvklartVirksomhet(
            "Jarlsberg",
            "123456789",
            virksomhetAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )

        val fysiskArbeidssted = FysiskArbeidssted(
            "JARLSBERG INTERNATIONAL",
            "123456789",
            virksomhetAdresse
        )

        val ikkeFysiskArbeidssted = lagMaritimtArbeidssted()

        val a1Data = BrevDataA1(
            yrkesgruppe = Yrkesgrupper.ORDINAER,
            bostedsadresse = boAdresse,
            arbeidssteder = listOf(fysiskArbeidssted, ikkeFysiskArbeidssted),
            arbeidsland = listOf(Land_iso2.NO, Land_iso2.BG, Land_iso2.AT, Land_iso2.AX),
            person = PersonopplysningerObjectFactory.lagPersonopplysninger(),
            hovedvirksomhet = virksomhet,
            bivirksomheter = mutableListOf(utenlandskVirksomhet)
        )

        val brevData = BrevDataVedlegg("Z1234567").also {
            it.brevDataA1 = a1Data
        }

        // FellesType is a generated class from dokgen - using .apply for property setting
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-1"
        }

        // EasyRandom generates random test data - needs post-construction overrides
        val navFelles = easyRandom.nextObject(MelosysNAVFelles::class.java).apply {
            mottaker.mottakeradresse = lagNorskPostadresse()
            kontaktinformasjon = lagKontaktInformasjon()
        }

        val xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData)

        xml.shouldNotBeNull()
    }
}
