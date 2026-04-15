package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Maritimtyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.*
import java.time.LocalDate

class InnvilgelsesbrevMapperTest {

    private val instans = InnvilgelsesbrevMapper()

    @Test
    fun `mapArbeidslandFraSøknad til brevXml gir ikke tom XML streng`() {
        val xmlFraFil = hentBrevXmlFraFil("innvilgelsesbrev/innvilgelsesbrev.xml")
        val behandlingsresultat = lagBehandlingsresultat {
            lovvalgsperiode { konfigurer(NOW) }
            avklartefakta {
                type = Avklartefaktatyper.VIRKSOMHET
                subjekt = "123456789"
                fakta = "TRUE"
            }
        }
        val testMapTilBrevXml = testMapTilBrevXml(
            lagBehandling(medFartsområde = false),
            behandlingsresultat
        )

        val diff = createDiffIgnoreNameSpace(xmlFraFil, testMapTilBrevXml)

        withClue(diff.differences) {
            diff.hasDifferences() shouldBe false
        }
    }

    @Test
    fun `mapTilBrevXML maritimtArbeidInnenriks arbeidsland settes til territorialfarvannLand`() {
        val xmlFraFil = hentBrevXmlFraFil("innvilgelsesbrev/innvilgelsesbrev_territorialfarvann.xml")
        val behandlingsresultat = lagBehandlingsresultat {
            lovvalgsperiode { konfigurer(NOW) }
            avklartefakta {
                type = Avklartefaktatyper.VIRKSOMHET
                subjekt = "123456789"
                fakta = "TRUE"
            }
        }
        val testMapTilBrevXml = testMapTilBrevXml(
            lagBehandling(medFartsområde = true),
            behandlingsresultat
        )

        val diff = createDiffIgnoreNameSpace(xmlFraFil, testMapTilBrevXml)

        withClue(diff.differences) {
            diff.hasDifferences() shouldBe false
        }
    }

    @Test
    fun `fritekst med linjeskift konverteres til Metaforce-format`() {
        val behandlingsresultat = lagBehandlingsresultat {
            lovvalgsperiode { konfigurer(NOW) }
            avklartefakta {
                type = Avklartefaktatyper.VIRKSOMHET
                subjekt = "123456789"
                fakta = "TRUE"
            }
        }
        val brevdataA1 = lagBrevDataA1()
        val brevdataInnvilgelse = lagBrevDataInnvilgelse(brevdataA1).apply {
            fritekst = "Linje 1\nLinje 2\nLinje 3"
        }

        val brevXml = instans.mapTilBrevXML(
            lagFellesType(), lagNAVFelles(),
            lagBehandling(medFartsområde = false), behandlingsresultat, brevdataInnvilgelse
        )

        brevXml shouldContain "Linje 1[_¶_]Linje 2[_¶_]Linje 3"
    }

    private fun hentBrevXmlFraFil(filnavn: String): String =
        javaClass.classLoader.getResourceAsStream(filnavn)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Kunne ikke lese XML fil: $filnavn")

    private fun testMapTilBrevXml(behandling: Behandling, behandlingsresultat: Behandlingsresultat): String {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevdataA1 = lagBrevDataA1()
        val brevdataInnvilgelse = lagBrevDataInnvilgelse(brevdataA1)

        return instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse)
    }

    private fun lagBrevDataA1(): BrevDataA1 {
        val virksomhet = no.nav.melosys.domain.avklartefakta.AvklartVirksomhet(
            "Virker ikke",
            "123456789",
            BrevDataTestUtils.lagStrukturertAdresse(),
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )
        return BrevDataA1().apply {
            hovedvirksomhet = virksomhet
            bivirksomheter = listOf(virksomhet)
            bostedsadresse = BrevDataTestUtils.lagStrukturertAdresse()
            yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL
            person = lagPersonopplysninger()
            arbeidssteder = ArrayList()
            arbeidsland = ArrayList()
        }
    }

    private fun lagBrevDataInnvilgelse(brevdataA1: BrevDataA1): BrevDataInnvilgelse {
        val lovvalgsperiode = lagLovvalgsperiode()
        return BrevDataInnvilgelse(BrevbestillingDto(), "SAKSBEHANDLER").apply {
            vedleggA1 = brevdataA1
            this.lovvalgsperiode = lovvalgsperiode
            avklartMaritimType = Maritimtyper.SKIP
            turistskip = true
            hovedvirksomhet = brevdataA1.hovedvirksomhet
            arbeidsland = "Sverige"
            setAnmodningsperiodesvar(BrevDataTestUtils.lagAnmodningsperiodeSvarInnvilgelse())
            trygdemyndighetsland = "Sverige"
            avklarteMedfolgendeBarn = BrevDataTestUtils.lagAvklarteMedfølgendeBarn()
        }
    }

    private fun createDiffIgnoreNameSpace(expectedXml: String, testMapTilBrevXml: String): Diff =
        DiffBuilder.compare(Input.fromString(expectedXml))
            .withTest(Input.fromString(testMapTilBrevXml))
            .ignoreWhitespace()
            .withDifferenceEvaluator(
                DifferenceEvaluators.chain(
                    DifferenceEvaluators.Default,
                    DifferenceEvaluator { comparison, outcome ->
                        if (comparison.type == ComparisonType.NAMESPACE_URI) {
                            val controlNode = comparison.controlDetails.target
                            val testNode = comparison.testDetails.target
                            if (controlNode != null && testNode != null &&
                                controlNode.nodeType == Node.ELEMENT_NODE &&
                                testNode.nodeType == Node.ELEMENT_NODE
                            ) {
                                return@DifferenceEvaluator ComparisonResult.EQUAL
                            }
                        }
                        outcome
                    }
                )
            )
            .withNodeFilter { node ->
                !node.nodeName.endsWith(":opprettelsesDato") && node.nodeName != "opprettelsesDato"
            }
            .checkForSimilar()
            .build()

    private fun lagBehandlingsresultat(init: BehandlingsresultatTestFactory.Builder.() -> Unit) =
        Behandlingsresultat.forTest(init)

    private fun LovvalgsperiodeTestFactory.Builder.konfigurer(fom: LocalDate) {
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        this.fom = fom
        tom = NOW
        lovvalgsland = Land_iso2.AT
        tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
    }

    private fun lagLovvalgsperiode(): Lovvalgsperiode = lovvalgsperiodeForTest { konfigurer(NOW) }

    private fun lagBehandling(medFartsområde: Boolean): Behandling {
        val mottatteOpplysninger = mottatteOpplysningerForTest {
            soeknad {
                fysiskeArbeidssted { landkode = "AT" }
                if (medFartsområde) {
                    maritimtArbeid {
                        enhetNavn = "Dunfjæder"
                        innretningLandkode = "NO"
                        territorialfarvannLandkode = "GB"
                        fartsomradeKode = Fartsomrader.INNENRIKS
                    }
                } else {
                    maritimtArbeid {
                        enhetNavn = "Dunfjæder"
                        innretningLandkode = "NO"
                    }
                }
            }
        }

        return Behandling.forTest {
            type = Behandlingstyper.KLAGE
            fagsak { }
            this.mottatteOpplysninger = mottatteOpplysninger
        }
    }

    companion object {
        private val NOW = LocalDate.parse("2022-02-13")
    }
}
