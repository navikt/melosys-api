package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
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
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*
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
        val testMapTilBrevXml = testMapTilBrevXml(
            lagBehandlingsresultat(
                setOf(lagLovvalgsperiode()),
                setOf(lagAvklarteFakta(Avklartefaktatyper.VIRKSOMHET, "123456789"))
            ),
            false
        )


        val diff = createDiffIgnoreNameSpace(xmlFraFil, testMapTilBrevXml)


        diff.hasDifferences() shouldBe false
    }

    @Test
    fun `mapTilBrevXML maritimtArbeidInnenriks arbeidsland settes til territorialfarvannLand`() {
        val xmlFraFil = hentBrevXmlFraFil("innvilgelsesbrev/innvilgelsesbrev_territorialfarvann.xml")
        val testMapTilBrevXml = testMapTilBrevXml(
            lagBehandlingsresultat(
                setOf(lagLovvalgsperiode()),
                setOf(lagAvklarteFakta(Avklartefaktatyper.VIRKSOMHET, "123456789"))
            ),
            true
        )


        val diff = createDiffIgnoreNameSpace(xmlFraFil, testMapTilBrevXml)


        diff.hasDifferences() shouldBe false
    }

    private fun testMapTilBrevXml(behandlingsresultat: Behandlingsresultat, medFartsområde: Boolean): String =
        testMapTilBrevXml(lagBehandling(medFartsområde), behandlingsresultat)

    private fun hentBrevXmlFraFil(filnavn: String): String =
        javaClass.classLoader.getResourceAsStream(filnavn)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Kunne ikke lese XML fil: $filnavn")

    private fun testMapTilBrevXml(behandling: Behandling, behandlingsresultat: Behandlingsresultat): String {
        val fellesType = lagFellesType()
        val navFelles = lagNAVFelles()
        val brevdataA1 = BrevDataA1().apply {
            val virksomhet = AvklartVirksomhet("Virker ikke", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
            hovedvirksomhet = virksomhet
            bivirksomheter = listOf(virksomhet)
            bostedsadresse = lagStrukturertAdresse()
            yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL
            person = lagPersonopplysninger()
            arbeidssteder = ArrayList()
            arbeidsland = ArrayList()
        }

        val brevdataInnvilgelse = BrevDataInnvilgelse(BrevbestillingDto(), "SAKSBEHANDLER").apply {
            vedleggA1 = brevdataA1
            lovvalgsperiode = lagLovvalgsperiode()
            avklartMaritimType = Maritimtyper.SKIP
            turistskip = true
            hovedvirksomhet = brevdataA1.hovedvirksomhet
            arbeidsland = "Sverige"
            setAnmodningsperiodesvar(lagAnmodningsperiodeSvarInnvilgelse())
            trygdemyndighetsland = "Sverige"
            avklarteMedfolgendeBarn = lagAvklarteMedfølgendeBarn()
        }

        return instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse)
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

    private fun lagBehandlingsresultat(perioder: Set<Lovvalgsperiode>, fakta: Set<Avklartefakta>) = Behandlingsresultat().apply {
        avklartefakta = fakta
        lovvalgsperioder = perioder
    }

    private fun lagLovvalgsperiode(): Lovvalgsperiode = lagLovvalgsperiode(NOW)

    private fun lagLovvalgsperiode(fom: LocalDate): Lovvalgsperiode =
        Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            this.fom = fom
            tom = NOW
            lovvalgsland = Land_iso2.AT
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        }

    private fun lagAvklarteFakta(type: Avklartefaktatyper, verdi: String) =
        Avklartefakta().apply {
            this.type = type
            fakta = "TRUE"
            subjekt = verdi
        }

    private fun lagBehandling(medFartsområde: Boolean): Behandling =
        lagBehandling(FagsakTestFactory.lagFagsak(), lagSoeknadDokument(medFartsområde))

    private fun lagSoeknadDokument(medFartsområde: Boolean) = Soeknad().apply {
        val strukturertAdresse = StrukturertAdresse().apply {
            landkode = "AT"
        }
        val fysiskArbeidssted = FysiskArbeidssted(null, strukturertAdresse)
        arbeidPaaLand.fysiskeArbeidssteder = listOf(fysiskArbeidssted)
        maritimtArbeid.add(
            if (medFartsområde) lagMaritimtArbeidMedFartsområde() else lagMaritimtArbeidUtenFartsområde()
        )
    }

    private fun lagMaritimtArbeidUtenFartsområde() = MaritimtArbeid().apply {
        enhetNavn = "Dunfjæder"
        innretningLandkode = "NO"
    }

    private fun lagMaritimtArbeidMedFartsområde() = MaritimtArbeid().apply {
        enhetNavn = "Dunfjæder"
        innretningLandkode = "NO"
        territorialfarvannLandkode = "GB"
        fartsomradeKode = Fartsomrader.INNENRIKS
    }

    private fun lagBehandling(fagsak: Fagsak, mottatteOpplysningerData: MottatteOpplysningerData): Behandling {
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }

        return Behandling.forTest {
            type = Behandlingstyper.KLAGE
            this.fagsak = fagsak
            this.mottatteOpplysninger = mottatteOpplysninger
        }
    }

    companion object {
        private val NOW = LocalDate.parse("2022-02-13")
    }
}
