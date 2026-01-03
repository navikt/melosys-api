package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.string.shouldMatch
import io.mockk.spyk
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Vesentlig_virksomhet_begrunnelser
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.lovvalgsperiodeForTest
import no.nav.melosys.domain.vilkaarsresultatForTest
import no.nav.melosys.domain.begrunnelse
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvslagArbeidsgiverMapperTest {

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
            person = personDokumentForTest {
                sammensattNavn = "Gunnar Granskau"
            }
            arbeidsland = "Danmark"
            hovedvirksomhet = AvklartVirksomhet("Test AS", "123456789", null, Yrkesaktivitetstyper.SELVSTENDIG)
            lovvalgsperiode = lovvalgsperiodeForTest {
                lovvalgsland = Land_iso2.DE
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
            vilkårbegrunnelser121 = vilkaarsresultatForTest {
                vilkaar = Vilkaar.FO_883_2004_ART12_1
                begrunnelse(Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.kode)
            }.begrunnelser
            vilkårbegrunnelser121VesentligVirksomhet = vilkaarsresultatForTest {
                vilkaar = Vilkaar.VESENTLIG_VIRKSOMHET
                begrunnelse(Vesentlig_virksomhet_begrunnelser.FOR_LITE_KONTRAKTER_NORGE.kode)
            }.begrunnelser
        }

        val spy = spyk(AvslagArbeidsgiverMapper())
        val xml = spy.mapTilBrevXML(fellesType, navFelles, null, null, brevData)

        xml shouldMatch "(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\\n.*"
    }
}
