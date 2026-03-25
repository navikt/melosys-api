package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.dok.melosysbrev._000084.BestemmelseDetSoekesUnntakFraKode
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse
import no.nav.melosys.service.dokument.brev.mapper.AnmodningUnntakMapper.BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP
import no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest
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

    private val mapper = AnmodningUnntakMapper()

    @Test
    fun `mapTilBrevXML genererer korrekt XML`() {
        val fellesType = lagFellesType()
        val navFelles = lagMelosysNAVFelles()
        val behandling = lagBehandling()
        val resultat = lagBehandlingsresultat {
            anmodningsperiode {
                fom = LocalDate.of(2000, 1, 1)
                tom = LocalDate.of(2001, 1, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = null
                tilleggsbestemmelse = null
                unntakFraLovvalgsland = Land_iso2.DK
                unntakFraBestemmelse = null
            }
        }

        val brevData = lagBrevData()

        val xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData)
        val expectedXml = hentBrevXmlFraFil()

        val diff = createDiffIgnoreNameSpace(expectedXml, xml)

        diff.hasDifferences() shouldBe false
    }

    @Test
    fun `mapTilBrevXML kodeverkAnmodning begrunnelser validerer`() {
        val behandling = lagBehandling()
        val resultat = lagBehandlingsresultat {
            anmodningsperiode {
                fom = LocalDate.of(2000, 1, 1)
                tom = LocalDate.of(2001, 1, 1)
                lovvalgsland = Land_iso2.NO
                unntakFraLovvalgsland = Land_iso2.DK
            }
        }
        val begrunnelser = VilkaarbegrunnelseFactoryTest().lagAlleVilkaarBegrunnelser(Anmodning_begrunnelser::class)

        for (begrunnelse in begrunnelser) {
            val brevdata = lagBrevData().apply {
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
        val behandlingsresultat = Behandlingsresultat.forTest {
            anmodningsperiode {
                fom = LocalDate.of(2000, 1, 1)
                tom = LocalDate.of(2001, 1, 1)
                lovvalgsland = Land_iso2.NO
                unntakFraLovvalgsland = Land_iso2.DK
                unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3
            }
        }
        val brevData = lagBrevData()

        val fag = mapper.mapFag(behandling, behandlingsresultat, brevData)

        fag.bestemmelseDetSoekesUnntakFra.shouldNotBeNull()
    }

    @Test
    fun `mapFag ikke direkteArt16 forvent null`() {
        val behandling = lagBehandling()
        val behandlingsresultat = lagBehandlingsresultat {
            anmodningsperiode {
                fom = LocalDate.of(2000, 1, 1)
                tom = LocalDate.of(2001, 1, 1)
                lovvalgsland = Land_iso2.NO
                unntakFraLovvalgsland = Land_iso2.DK
                unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3
            }
        }
        val brevData = lagBrevData()

        val fag = mapper.mapFag(behandling, behandlingsresultat, brevData)

        fag.bestemmelseDetSoekesUnntakFra.shouldBeNull()
    }

    @Test
    fun `mapFag alle bestemmelser det soekes unntak fra brukes`() {
        val bestemmelseDetSoekesUnntakFraBrev = BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP.inverse()

        BestemmelseDetSoekesUnntakFraKode.values().forEach { b ->
            bestemmelseDetSoekesUnntakFraBrev[b].shouldNotBeNull()
        }
    }

    @Test
    fun `fritekst med linjeskift konverteres til Metaforce-format`() {
        val behandling = lagBehandling()
        val resultat = lagBehandlingsresultat {
            anmodningsperiode {
                fom = LocalDate.of(2000, 1, 1)
                tom = LocalDate.of(2001, 1, 1)
                lovvalgsland = Land_iso2.NO
                unntakFraLovvalgsland = Land_iso2.DK
            }
        }
        val brevData = lagBrevData(anmodningFritekst = "Avsnitt A\nAvsnitt B").apply {
            fritekst = "Linje 1\nLinje 2"
        }

        val xml = mapper.mapTilBrevXML(lagFellesType(), lagMelosysNAVFelles(), behandling, resultat, brevData)

        xml shouldContain "Linje 1[_¶_]Linje 2"
        xml shouldContain "Avsnitt A[_¶_]Avsnitt B"
    }

    private fun lagBrevData(anmodningFritekst: String? = null): BrevDataAnmodningUnntak = BrevDataAnmodningUnntak(
        "Z999999",
        Landkoder.AT.beskrivelse,
        AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.SELVSTENDIG),
        Yrkesaktivitetstyper.SELVSTENDIG,
        emptySet(),
        emptySet(),
        anmodningFritekst
    )

    // FellesType er ekstern brev-type (ikke domain entity), derfor brukes .apply her
    private fun lagFellesType() = FellesType().apply {
        fagsaksnummer = "MELTEST-1"
    }

    // NAVFelles er ekstern brev-type (ikke domain entity), derfor brukes .apply her
    private fun lagMelosysNAVFelles() = lagNAVFelles().apply {
        mottaker.mottakeradresse = lagNorskPostadresse()
        kontaktinformasjon = lagKontaktInformasjon()
    }

    private fun lagBehandling() = Behandling.forTest {
        mottatteOpplysninger = mottatteOpplysningerForTest {
            soeknad {
                fysiskeArbeidssted {
                    landkode = "NO"
                }
            }
        }
    }

    private fun lagBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
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
            isOppfylt = true
        }
        init()
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
