package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dok.melosysbrev._000084.BestemmelseDetSoekesUnntakFraKode
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.mapper.AnmodningUnntakMapper.BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.ComparisonType
import org.xmlunit.diff.Diff
import org.xmlunit.diff.DifferenceEvaluators
import java.time.LocalDate

class AnmodningUnntakMapperTest {

    private lateinit var mapper: AnmodningUnntakMapper

    @BeforeEach
    fun setUp() {
        mapper = AnmodningUnntakMapper()
    }

    @Test
    fun `mapTilBrevXML genererer korrekt XML`() {
        val fellesType = lagFellesType()
        val navFelles = lagMelosysNAVFelles()
        val behandling = lagBehandling()
        val resultat = lagBehandlingsresultat()

        val brevData = lagBrevData(resultat)

        val xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData)
        val expectedXml = hentBrevXmlFraFil()

        val diff = createDiffIgnoreNameSpace(expectedXml, xml)

        diff.hasDifferences() shouldBe false
    }

    @Test
    fun `mapTilBrevXML kodeverkAnmodning begrunnelser validerer`() {
        val behandling = lagBehandling()
        val resultat = lagBehandlingsresultat()
        val begrunnelser = VilkaarbegrunnelseFactoryTest().lagAlleVilkaarBegrunnelser(Anmodning_begrunnelser::class)

        for (begrunnelse in begrunnelser) {
            val brevdata = lagBrevData(resultat).apply {
                anmodningBegrunnelser = setOf(begrunnelse)
            }
            shouldNotThrow<Exception> {
                mapper.mapFag(behandling, resultat, brevdata)
            }
        }
    }

    @Test
    fun `mapFag direkteArt16 forvent ikke null`() {
        val behandling = lagBehandling()
        val behandlingsresultat = SaksbehandlingDataFactory.lagBehandlingsresultat()
        val brevData = lagBrevData(behandlingsresultat, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3)

        val fag = mapper.mapFag(behandling, behandlingsresultat, brevData)

        fag.bestemmelseDetSoekesUnntakFra.shouldNotBeNull()
    }

    @Test
    fun `mapFag ikke direkteArt16 forvent null`() {
        val behandling = lagBehandling()
        val behandlingsresultat = lagBehandlingsresultat()
        val brevData = lagBrevData(behandlingsresultat, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3)

        val fag = mapper.mapFag(behandling, behandlingsresultat, brevData)

        fag.bestemmelseDetSoekesUnntakFra.shouldBeNull()
    }

    @Test
    fun `mapFag alle bestemmelser det s�kes unntak fra brukes`() {
        val bestemmelseDetSoekesUnntakFraBrev = BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP.inverse()

        BestemmelseDetSoekesUnntakFraKode.values().forEach { b ->
            bestemmelseDetSoekesUnntakFraBrev[b].shouldNotBeNull()
        }
    }

    private fun lagBrevData(resultat: Behandlingsresultat, unntakFraBestemmelse: LovvalgBestemmelse? = null): BrevDataAnmodningUnntak {
        val fom = LocalDate.of(2000, 1, 1)
        val tom = LocalDate.of(2001, 1, 1)
        val anmodningsperiode = Anmodningsperiode(
            fom,
            tom,
            Land_iso2.NO,
            null,
            null,
            Land_iso2.DK,
            unntakFraBestemmelse,
            null
        )
        resultat.anmodningsperioder = hashSetOf(anmodningsperiode)

        return BrevDataAnmodningUnntak(
            "Z999999",
            Landkoder.AT.beskrivelse,
            AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.SELVSTENDIG),
            Yrkesaktivitetstyper.SELVSTENDIG,
            emptySet(),
            emptySet(),
            null
        )
    }

    private fun lagFellesType() = FellesType().apply {
        fagsaksnummer = "MELTEST-1"
    }

    private fun lagMelosysNAVFelles() = lagNAVFelles().apply {
        mottaker.mottakeradresse = lagNorskPostadresse()
        kontaktinformasjon = lagKontaktInformasjon()
    }

    private fun lagBehandling() = Behandling.forTest {
        this.mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = Soeknad().apply {
                arbeidPaaLand.fysiskeArbeidssteder = mutableListOf(FysiskArbeidssted(null, StrukturertAdresse().apply {
                    landkode = "NO"
                }))
            }
        }
    }

    private fun lagBehandlingsresultat() = Behandlingsresultat().apply {
        lovvalgsperioder = mutableSetOf(Lovvalgsperiode().apply {
            this.lovvalgsland = Land_iso2.NO
            this.fom = LocalDate.now()
            this.tom = LocalDate.now()
        })

        vilkaarsresultater = hashSetOf()

        vilkaarsresultater.add(Vilkaarsresultat().apply {
            this.vilkaar = Vilkaar.FO_883_2004_ART12_1
            isOppfylt = false
            this.begrunnelser = setOf(VilkaarBegrunnelse().apply {
                this.kode = Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.kode
            })
        })

        vilkaarsresultater.add(Vilkaarsresultat().apply {
            this.vilkaar = Vilkaar.FO_883_2004_ART12_2
            isOppfylt = true
        })
    }

    private fun hentBrevXmlFraFil(): String = javaClass.classLoader.getResourceAsStream("unntakbrev/unntakbrev.xml")?.bufferedReader()?.readText()
        ?: throw IllegalStateException("Kunne ikke lese XML fil")

    private fun createDiffIgnoreNameSpace(expectedXml: String, testMapTilBrevXml: String): Diff {
        return DiffBuilder.compare(Input.fromString(expectedXml))
            .withTest(Input.fromString(testMapTilBrevXml))
            .ignoreWhitespace()
            .withDifferenceEvaluator(
                DifferenceEvaluators.chain(
                    DifferenceEvaluators.Default,
                    { comparison, outcome ->
                        if (comparison.type == ComparisonType.NAMESPACE_URI) {
                            val controlNode = comparison.controlDetails.target
                            val testNode = comparison.testDetails.target
                            if (controlNode != null && testNode != null &&
                                controlNode.nodeType == Node.ELEMENT_NODE &&
                                testNode.nodeType == Node.ELEMENT_NODE
                            ) {
                                ComparisonResult.EQUAL
                            } else {
                                outcome
                            }
                        } else {
                            outcome
                        }
                    }
                )
            )
            .checkForSimilar()
            .build()
    }
}
