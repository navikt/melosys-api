package no.nav.melosys.tjenester.gui.config.jackson.serialize

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OrganisasjonSerializerTest {

    private lateinit var mapper: ObjectMapper
    private val kodeverkService = mockk<KodeverkService>()

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SimpleModule().apply {
                addSerializer(OrganisasjonSerializer(kodeverkService))
            })
            .build()
    }

    @Test
    fun `skal serialisere orgnummer og navn`() {
        val org = OrganisasjonDokumentTestFactory.builder()
            .orgnummer("987654321")
            .navn("Test AS")
            .build()

        val tree = mapper.readTree(mapper.writeValueAsString(org))

        tree.get("orgnr").asText() shouldBe "987654321"
        tree.get("navn").asText() shouldBe "Test AS"
    }

    @Test
    fun `skal serialisere enhetstype via kodeverk`() {
        every { kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, "AS") } returns "Aksjeselskap"

        val detaljer = mockk<OrganisasjonsDetaljer>(relaxed = true) {
            every { hentStrukturertForretningsadresse() } returns null
            every { hentStrukturertPostadresse() } returns null
        }
        val org = OrganisasjonDokument(
            orgnummer = "123456789",
            navn = "Test AS",
            enhetstype = "AS",
            organisasjonDetaljer = detaljer,
            sektorkode = "6500"
        )

        val tree = mapper.readTree(mapper.writeValueAsString(org))

        tree.get("organisasjonsform").asText() shouldBe "Aksjeselskap"
    }

    @Test
    fun `skal serialisere forretningsadresse og bruke ISO2-landkode`() {
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NOR") } returns "NO"
        every { kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0557") } returns "Oslo"

        val adresse = StrukturertAdresse(
            gatenavn = "Sannergata",
            husnummerEtasjeLeilighet = "2",
            postnummer = "0557",
            poststed = "Oslo",
            region = null,
            landkode = "NOR"
        )
        val detaljer = mockk<OrganisasjonsDetaljer>(relaxed = true) {
            every { hentStrukturertForretningsadresse() } returns adresse
            every { hentStrukturertPostadresse() } returns null
        }
        val org = OrganisasjonDokument(
            orgnummer = "123456789",
            navn = "Test AS",
            organisasjonDetaljer = detaljer,
            sektorkode = "6500"
        )

        val tree = mapper.readTree(mapper.writeValueAsString(org))
        val forretningsadresse = tree.get("forretningsadresse")

        forretningsadresse shouldNotBe null
        forretningsadresse.get("postnr").asText() shouldBe "0557"
        forretningsadresse.get("poststed").asText() shouldBe "Oslo"
        forretningsadresse.get("land").asText() shouldBe "NO"
    }

    @Test
    fun `skal falle tilbake til LANDKODER naar LANDKODER_ISO2 returnerer UKJENT`() {
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NOR") } returns KodeverkService.UKJENT
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER, "NOR") } returns "Norge"

        val adresse = StrukturertAdresse(
            gatenavn = "Testgate 1",
            postnummer = "0001",
            poststed = "Oslo",
            region = null,
            landkode = "NOR",
            husnummerEtasjeLeilighet = null
        )
        val detaljer = mockk<OrganisasjonsDetaljer>(relaxed = true) {
            every { hentStrukturertForretningsadresse() } returns adresse
            every { hentStrukturertPostadresse() } returns null
        }
        val org = OrganisasjonDokument(
            orgnummer = "123456789",
            navn = "Test AS",
            organisasjonDetaljer = detaljer,
            sektorkode = "6500"
        )

        val tree = mapper.readTree(mapper.writeValueAsString(org))

        tree.get("forretningsadresse").get("land").asText() shouldBe "Norge"
    }

    @Test
    fun `skal returnere tom adresse-dto naar adresse er null`() {
        val detaljer = mockk<OrganisasjonsDetaljer>(relaxed = true) {
            every { hentStrukturertForretningsadresse() } returns null
            every { hentStrukturertPostadresse() } returns null
        }
        val org = OrganisasjonDokumentTestFactory.builder()
            .organisasjonsDetaljer(detaljer)
            .build()

        val tree = mapper.readTree(mapper.writeValueAsString(org))

        // Tom AdresseDto returneres (ikke null) for å unngå null til frontend
        tree.get("forretningsadresse") shouldNotBe null
        tree.get("postadresse") shouldNotBe null
    }
}
