package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import io.mockk.spyk
import no.nav.dok.melosysbrev._000081.Fag
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.begrunnelse
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Avslag_anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.vilkaarsresultat
import no.nav.melosys.domain.vilkaarsresultatForTest
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarAvslag
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvslagYrkesaktivMapperTest {

    // Brev-types use .apply since they don't have forTest DSL
    private val fellesType = FellesType().apply {
        fagsaksnummer = "MELTEST-1"
    }

    private val navFelles = lagNAVFelles().apply {
        mottaker.mottakeradresse = lagNorskPostadresse()
        kontaktinformasjon = lagKontaktInformasjon()
    }

    @Test
    fun `mapTilBrevXML genererer gyldig XML`() {
        val behandling = Behandling.forTest {
            mottatteOpplysninger {
                soeknad {
                    fysiskeArbeidssted {
                        landkode = "NO"
                    }
                }
            }
        }

        val vilkaarsresultat16_1 = vilkaarsresultatForTest {
            vilkaar = Vilkaar.FO_883_2004_ART16_1
            isOppfylt = false
            begrunnelse(Avslag_anmodning_begrunnelser.OVER_5_AAR.kode)
            begrunnelse(Avslag_anmodning_begrunnelser.SOEKT_FOR_SENT.kode)
            begrunnelse(Avslag_anmodning_begrunnelser.SAERLIG_AVSLAGSGRUNN.kode)
            begrunnelseFritekst = "Fritekst"
        }

        val resultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                lovvalgsland = Land_iso2.NO
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
            vilkaarsresultat {
                vilkaar = Vilkaar.FO_883_2004_ART12_1
                isOppfylt = false
                begrunnelse(Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.kode)
            }
            vilkaarsresultat {
                vilkaar = Vilkaar.FO_883_2004_ART12_2
                isOppfylt = false
            }
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
        val behandling = Behandling.forTest()
        val spy = spyk(AvslagYrkesaktivMapper())

        val vilkaar16_1_oppfylt = vilkaarsresultatForTest {
            vilkaar = Vilkaar.FO_883_2004_ART16_1
            isOppfylt = true
            begrunnelse(Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR.kode)
        }

        val brevData = BrevDataAvslagYrkesaktiv(BrevbestillingDto(), "Z999999").apply {
            arbeidsland = Landkoder.ES.beskrivelse
            hovedvirksomhet = AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID)
            anmodningsperiodeSvar = lagAnmodningsperiodeSvarAvslag()
            yrkesaktivitet = Yrkesaktivitetstyper.LOENNET_ARBEID
            art16Vilkaar = vilkaar16_1_oppfylt
        }

        val resultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                lovvalgsland = Land_iso2.NO
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
            vilkaarsresultat {
                vilkaar = Vilkaar.FO_883_2004_ART12_1
                isOppfylt = false
                begrunnelse(Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.kode)
            }
        }


        val xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData)


        xml shouldMatch """(?s)\<\?xml version="\d\.\d+" .*>\n.*"""
    }

    @Test
    fun `mapTilBrevXml kan mappe alle kodeverksverdier for Art16_1 avslag`() {
        val spy = spyk(AvslagYrkesaktivMapper())
        val brevdata = BrevDataAvslagYrkesaktiv(BrevbestillingDto(), "")
        val begrunnelser = VilkaarbegrunnelseFactoryTest().lagAlleVilkaarBegrunnelser(Avslag_anmodning_begrunnelser::class)


        for (begrunnelse in begrunnelser) {
            val vilkaarsresultat = vilkaarsresultatForTest {
                begrunnelseKoder.add(begrunnelse.kode)
                begrunnelseFritekst = "Fritekst"
            }
            brevdata.art16Vilkaar = vilkaarsresultat
            spy.mapArt161Avslag(Fag(), brevdata)
        }
    }
}
