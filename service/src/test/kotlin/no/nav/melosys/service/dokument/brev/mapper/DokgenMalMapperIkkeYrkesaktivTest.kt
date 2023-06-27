package no.nav.melosys.service.dokument.brev.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.brev.IkkeYrkesaktivBrevbestilling
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca_qc
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.SoeknadIkkeYrkesaktiv
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.dokument.DokgenTestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class DokgenMalMapperIkkeYrkesaktivTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockTrygdeavtaleMapper: TrygdeavtaleMapper

    @MockK
    private lateinit var mockInnvilgelseFtrlMapper: InnvilgelseFtrlMapper

    private lateinit var dokgenMalMapper: DokgenMalMapper

    @BeforeEach
    fun beforEach() {
        dokgenMalMapper = DokgenMalMapper(
            mockDokgenMapperDatahenter,
            mockInnvilgelseFtrlMapper,
            mockTrygdeavtaleMapper
        )
    }

    @Test
    fun test() {
        val behandling = Behandling().apply behandling@{
            id = 1L
            tema = Behandlingstema.IKKE_YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            mottatteOpplysninger = MottatteOpplysninger().apply {
                setMottatteOpplysningerdata(SoeknadIkkeYrkesaktiv().apply {
                    ikkeYrkesaktivSituasjontype = Ikkeyrkesaktivsituasjontype.STUDENT
                    soeknadsland = Soeknadsland(listOf(Land_iso2.CA.kode), false)
                })
            }
            fagsak = Fagsak().apply {
                saksnummer = "MEL-1"
                type = Sakstyper.EU_EOS
                behandlinger = listOf(this@behandling)
            }
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            type = Behandlingsresultattyper.IKKE_FASTSATT
            this.behandling = behandling
            lovvalgsperioder = setOf(Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2
            })
            innledningFritekst = "innledningFritekst"
            begrunnelseFritekst = "begrunnelseFritekst"
        }

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val vedtaksbrev = dokgenMalMapper.lagIkkeYrkesaktivVedtaksbrev(
            IkkeYrkesaktivBrevbestilling.Builder()
                .medPersonMottaker(DokgenTestData.lagPersondata())
                .medPersonDokument(DokgenTestData.lagPersondata())
                .medBehandling(behandling)
                .medNyVurderingBakgrunn("nyVurderingBakgrunn")
                .build()
        )


        vedtaksbrev.toJsonNode.apply {
            get("oppholdsland").asText().shouldBe("Canada")
            get("nyVurderingBakgrunn").asText().shouldBe("nyVurderingBakgrunn")
            get("sakstype").asText().shouldBe("EU_EOS")
            get("artikkel").asText().shouldBe("Rfo. 883/2004 art.11(2)")
            get("bestemmelse").asText().shouldBe("FO_883_2004_ART11_2")
            get("periode").apply {
                get("fom").asText().shouldBe("2020-01-01")
                get("tom").asText().shouldBe("2021-02-01")
            }
            get("innvilgelse").apply {
                get("innledningFritekst").asText().shouldBe("innledningFritekst")
                get("begrunnelseFritekst").asText().shouldBe("begrunnelseFritekst")
            }
            get("ikkeYrkesaktivSituasjontype").asText().shouldBe("STUDENT")
        }

        // vedtaksbrev.toJsonNode.toPrettyString().also(::println)
    }


    @Test
    fun `test at artikkel blir splittet opp riktig fra bestemmelsesbeskrivelsen`() {
        val behandling = Behandling().apply behandling@{
            id = 2L
            tema = Behandlingstema.IKKE_YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            mottatteOpplysninger = MottatteOpplysninger().apply {
                setMottatteOpplysningerdata(Soeknad().apply {
                    soeknadsland = Soeknadsland(listOf(Land_iso2.CA_QC.kode), false)
                })
            }
            fagsak = Fagsak().apply {
                saksnummer = "MEL-2"
                type = Sakstyper.TRYGDEAVTALE
                behandlinger = listOf(this@behandling)
            }
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 2L
            type = Behandlingsresultattyper.IKKE_FASTSATT
            this.behandling = behandling
            lovvalgsperioder = setOf(Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7_3
            })
        }

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val vedtaksbrev = dokgenMalMapper.lagIkkeYrkesaktivVedtaksbrev(
            IkkeYrkesaktivBrevbestilling.Builder()
                .medPersonMottaker(DokgenTestData.lagPersondata())
                .medPersonDokument(DokgenTestData.lagPersondata())
                .medBehandling(behandling)
                .build()
        )


        vedtaksbrev.toJsonNode.apply {
            get("artikkel").asText().shouldBe("artikkel 7 nr. 3")
        }

        // vedtaksbrev.toJsonNode.toPrettyString().also(::println)
    }


    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
