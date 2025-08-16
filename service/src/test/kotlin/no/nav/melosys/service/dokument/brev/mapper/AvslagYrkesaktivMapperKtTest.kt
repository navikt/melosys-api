package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import io.mockk.spyk
import no.nav.dok.melosysbrev._000081.Fag
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Avslag_anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarAvslag
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest.lagAlleVilkaarBegrunnelser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvslagYrkesaktivMapperKtTest {

    private lateinit var fellesType: FellesType
    private lateinit var behandling: Behandling
    private lateinit var navFelles: MelosysNAVFelles

    @BeforeEach
    fun setUp() {
        fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-1"
        }

        navFelles = lagNAVFelles().apply {
            mottaker.mottakeradresse = lagNorskPostadresse()
            kontaktinformasjon = lagKontaktInformasjon()
        }

        behandling = Behandling.forTest()
    }

    @Test
    fun `mapTilBrevXML genererer gyldig XML`() {
        val fysiskArbeidssted = FysiskArbeidssted("NO", StrukturertAdresse())

        behandling.mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = Soeknad().apply {
                arbeidPaaLand.fysiskeArbeidssteder = mutableListOf(fysiskArbeidssted)
            }
        }

        val resultat = Behandlingsresultat().apply {
            val lovvalgsperiode = Lovvalgsperiode().apply {
                lovvalgsland = Land_iso2.NO
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
            lovvalgsperioder = setOf(lovvalgsperiode)
            vilkaarsresultater = hashSetOf()
        }

        val vilkaarsresultat12_1 = lagVilkaarsresultat(
            Vilkaar.FO_883_2004_ART12_1,
            false,
            Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET
        )
        resultat.vilkaarsresultater.add(vilkaarsresultat12_1)

        val vilkaarsresultat12_2 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false)
        resultat.vilkaarsresultater.add(vilkaarsresultat12_2)

        val vilkaarsresultat16_1 = lagVilkaarsresultat(
            Vilkaar.FO_883_2004_ART16_1,
            false,
            Avslag_anmodning_begrunnelser.OVER_5_AAR,
            Avslag_anmodning_begrunnelser.SOEKT_FOR_SENT,
            Avslag_anmodning_begrunnelser.SAERLIG_AVSLAGSGRUNN
        ).apply {
            begrunnelseFritekst = "Fritekst"
        }

        val brevData = BrevDataAvslagYrkesaktiv(BrevbestillingDto(), "Z999999").apply {
            arbeidsland = Landkoder.AT.beskrivelse
            hovedvirksomhet = AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID)
            anmodningsperiodeSvar = AnmodningsperiodeSvar()
            yrkesaktivitet = Yrkesaktivitetstyper.LOENNET_ARBEID
            art16Vilkaar = vilkaarsresultat16_1
        }
        val spy = spyk(AvslagYrkesaktivMapper())


        val xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData)


        xml shouldMatch """(?s)\<\?xml version="\d\.\d+" .*>\n.*"""
    }

    @Test
    fun `mapTilBrevXML med oppfylt Art16 og anmodningsperiode bruker anmodningsperiode`() {
        val spy = spyk(AvslagYrkesaktivMapper())

        val brevData = BrevDataAvslagYrkesaktiv(BrevbestillingDto(), "Z999999").apply {
            arbeidsland = Landkoder.ES.beskrivelse
            hovedvirksomhet = AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID)
            anmodningsperiodeSvar = lagAnmodningsperiodeSvarAvslag()
            yrkesaktivitet = Yrkesaktivitetstyper.LOENNET_ARBEID
        }

        val vilkaar16_1_oppfylt = lagVilkaarsresultat(
            Vilkaar.FO_883_2004_ART16_1,
            true,
            Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR
        )
        brevData.art16Vilkaar = vilkaar16_1_oppfylt

        val resultat = lagBehandlingsresultat()
        val vilkaar12_1_avslatt = lagVilkaarsresultat(
            Vilkaar.FO_883_2004_ART12_1,
            false,
            Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET
        )
        resultat.vilkaarsresultater.add(vilkaar12_1_avslatt)


        val xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData)


        xml shouldMatch """(?s)\<\?xml version="\d\.\d+" .*>\n.*"""
    }

    @Test
    fun `mapTilBrevXml kan mappe alle kodeverksverdier for Art16_1 avslag`() {
        val spy = spyk(AvslagYrkesaktivMapper())
        val brevdata = BrevDataAvslagYrkesaktiv(BrevbestillingDto(), "")
        val begrunnelser = lagAlleVilkaarBegrunnelser(Avslag_anmodning_begrunnelser::class.java)


        for (begrunnelse in begrunnelser) {
            val vilkaarsresultat = Vilkaarsresultat().apply {
                this.begrunnelser = setOf(begrunnelse)
                begrunnelseFritekst = "Fritekst"
            }
            brevdata.art16Vilkaar = vilkaarsresultat
            spy.mapArt161Avslag(Fag(), brevdata)
        }
    }

    private fun lagVilkaarsresultat(vilkaar: Vilkaar, oppfylt: Boolean, vararg vilkaarbegrunnelser: Kodeverk): Vilkaarsresultat =
        Vilkaarsresultat().apply {
            this.setOppfylt(oppfylt)
            this.vilkaar = vilkaar
            this.begrunnelser = hashSetOf()
            for (begrunnelseKode in vilkaarbegrunnelser) {
                val begrunnelse = VilkaarBegrunnelse().apply {
                    kode = begrunnelseKode.kode
                }
                this.begrunnelser.add(begrunnelse)
            }
        }

    private fun lagBehandlingsresultat(): Behandlingsresultat =
        Behandlingsresultat().apply {
            val lovvalgsperiode = Lovvalgsperiode().apply {
                lovvalgsland = Land_iso2.NO
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
            lovvalgsperioder = setOf(lovvalgsperiode)
            vilkaarsresultater = hashSetOf()
        }
}
