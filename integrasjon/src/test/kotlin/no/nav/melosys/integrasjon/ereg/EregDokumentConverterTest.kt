package no.nav.melosys.integrasjon.ereg

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.jpa.SaksopplysningDokumentConverter
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class EregDokumentConverterTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `Konvertering av virksomhet til json skal bli som forventet`() {
        val virksomhet = mapper.readValue<OrganisasjonResponse.Organisasjon>(hentRessurs("mock/organisasjon/901851573.json"))
        val forventetOrganisasjonDokumentJson = hentRessurs("mock/organisasjon/resultat/virksomhet-resultat.json")


        val organisasjonDokument =
            EregDtoTilSaksopplysningKonverter().lagSaksopplysning(virksomhet).dokument.shouldBeTypeOf<OrganisasjonDokument>()
        val organisasjonDokumentSomJson = SaksopplysningDokumentConverter().convertToDatabaseColumn(organisasjonDokument)


        organisasjonDokumentSomJson.shouldEqualJson(forventetOrganisasjonDokumentJson)
    }

    @Test
    fun `Konvertering av JuridiskEnhet til json skal bli som forventet`() {
        val juridiskEnhet = mapper.readValue<OrganisasjonResponse.Organisasjon>(hentRessurs("mock/organisasjon/928497704.json"))
        val forventetOrganisasjonDokumentJson = hentRessurs("mock/organisasjon/resultat/juridiskEnhet-resultat.json")


        val organisasjonDokument =
            EregDtoTilSaksopplysningKonverter().lagSaksopplysning(juridiskEnhet).dokument.shouldBeTypeOf<OrganisasjonDokument>()
        val organisasjonDokumentSomJson = SaksopplysningDokumentConverter().convertToDatabaseColumn(organisasjonDokument)


        organisasjonDokumentSomJson.shouldEqualJson(forventetOrganisasjonDokumentJson)
    }

    @Test
    fun `Konvertering av Organisasjonsledd til json skal bli som forventet`() {
        val organisasjonsledd = mapper.readValue<OrganisasjonResponse.Organisasjon>(hentRessurs("mock/organisasjon/974774577.json"))
        val forventetOrganisasjonDokumentJson = hentRessurs("mock/organisasjon/resultat/organisasjonsledd-resultat.json")


        val organisasjonDokument =
            EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjonsledd).dokument.shouldBeTypeOf<OrganisasjonDokument>()
        val organisasjonDokumentSomJson = SaksopplysningDokumentConverter().convertToDatabaseColumn(organisasjonDokument)


        organisasjonDokumentSomJson.shouldEqualJson(forventetOrganisasjonDokumentJson)
    }

    @Test
    fun `Konvertering av Organisasjons til json skal bli som forventet`() {
        val organisasjons = mapper.readValue<OrganisasjonResponse.Organisasjon>(hentRessurs("mock/organisasjon/928497705.json"))
        val forventetOrganisasjonDokumentJson = hentRessurs("mock/organisasjon/resultat/organisasjons-resultat.json")


        val organisasjonDokument =
            EregDtoTilSaksopplysningKonverter().lagSaksopplysning(organisasjons).dokument.shouldBeTypeOf<OrganisasjonDokument>()
        val organisasjonDokumentSomJson = SaksopplysningDokumentConverter().convertToDatabaseColumn(organisasjonDokument)


        organisasjonDokumentSomJson.shouldEqualJson(forventetOrganisasjonDokumentJson)
    }


    private fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")

}
