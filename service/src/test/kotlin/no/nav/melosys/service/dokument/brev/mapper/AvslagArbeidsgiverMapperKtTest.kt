package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import io.mockk.spyk
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Vesentlig_virksomhet_begrunnelser
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvslagArbeidsgiverMapperKtTest {

    @Test
    fun `mapTilBrevXML skal generere korrekt XML`() {
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-2"
        }

        val navFelles = lagNAVFelles().apply {
            mottaker.mottakeradresse = lagNorskPostadresse()
            kontaktinformasjon = lagKontaktInformasjon()
        }

        val brevData = BrevDataAvslagArbeidsgiver("Z12345").apply {
            person = PersonDokument().apply {
                sammensattNavn = "Gunnar Granskau"
            }
            arbeidsland = "Danmark"
            hovedvirksomhet = AvklartVirksomhet("Test AS", "123456789", null, Yrkesaktivitetstyper.SELVSTENDIG)
            lovvalgsperiode = Lovvalgsperiode().apply {
                lovvalgsland = Land_iso2.DE
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
            vilkårbegrunnelser121 = Vilkaarsresultat().apply {
                vilkaar = Vilkaar.FO_883_2004_ART12_1
                begrunnelser = setOf(VilkaarBegrunnelse().apply {
                    kode = Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.kode
                })
            }.begrunnelser
            vilkårbegrunnelser121VesentligVirksomhet = Vilkaarsresultat().apply {
                vilkaar = Vilkaar.VESENTLIG_VIRKSOMHET
                begrunnelser = setOf(VilkaarBegrunnelse().apply {
                    kode = Vesentlig_virksomhet_begrunnelser.FOR_LITE_KONTRAKTER_NORGE.kode
                })
            }.begrunnelser
        }

        val spy = spyk(AvslagArbeidsgiverMapper())
        val xml = spy.mapTilBrevXML(fellesType, navFelles, null, null, brevData)

        xml shouldMatch "(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\\n.*"
    }
}
