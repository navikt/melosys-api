package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagMaritimtArbeidssted
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AttestMapperTest {

    private lateinit var mapper: AttestMapper
    private lateinit var easyRandom: EasyRandom
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var behandling: Behandling
    private lateinit var brevData: BrevDataVedlegg

    @BeforeEach
    fun setUp() {
        mapper = AttestMapper()
        easyRandom = EasyRandomConfigurer.randomForDokProd()

        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
            fom = LocalDate.now()
            tom = LocalDate.now()
        }

        behandlingsresultat = mockk {
            every { registrertDato } returns Instant.now()
            every { lovvalgsperioder } returns setOf(lovvalgsperiode)
            every { hentLovvalgsperiode() } returns lovvalgsperiode
        }

        val boAdresse = StrukturertAdresse().apply {
            gatenavn = "HjemmeGata"
            husnummerEtasjeLeilighet = "25"
            postnummer = "0165"
            poststed = "Poststed"
            region = "Region"
            landkode = Landkoder.NO.kode
        }

        behandling = mockk {
            every { registrertDato } returns Instant.now()
            every { fagsak } returns Fagsak.forTest()
            every { id } returns 1L
        }

        val strukturertAdresse = StrukturertAdresse().apply {
            husnummerEtasjeLeilighet = "25"
            gatenavn = "Gatenavn"
            postnummer = "0165"
            poststed = "Poststed"
            region = "Region"
            landkode = Landkoder.NO.kode
        }


        val virksomhet = AvklartVirksomhet(
            "JARLSBERG INTERNATIONAL",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )

        val utenlandskVirksomhet = AvklartVirksomhet(
            "Jarlsberg",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )

        val fysiskArbeidssted = no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted(
            "JARLSBERG INTERNATIONAL",
            "123456789",
            strukturertAdresse
        )

        val ikkeFysiskArbeidssted = lagMaritimtArbeidssted()

        val a1Data = BrevDataA1().apply {
            yrkesgruppe = Yrkesgrupper.ORDINAER
            bostedsadresse = boAdresse
            arbeidssteder = listOf(fysiskArbeidssted, ikkeFysiskArbeidssted)
            arbeidsland = listOf(Land_iso2.NO, Land_iso2.BG, Land_iso2.AT, Land_iso2.AX)
            person = PersonopplysningerObjectFactory.lagPersonopplysninger()
            hovedvirksomhet = virksomhet
            bivirksomheter = mutableListOf(utenlandskVirksomhet)
        }

        brevData = BrevDataVedlegg("Z1234567").apply {
            brevDataA1 = a1Data
        }
    }

    @Test
    fun `mapTilBrevXML should generate XML successfully`() {
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-1"
        }

        val navFelles = easyRandom.nextObject(MelosysNAVFelles::class.java).apply {
            mottaker.mottakeradresse = lagNorskPostadresse()
            kontaktinformasjon = lagKontaktInformasjon()
        }

        val xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData)

        xml.shouldNotBeNull()
    }
}
