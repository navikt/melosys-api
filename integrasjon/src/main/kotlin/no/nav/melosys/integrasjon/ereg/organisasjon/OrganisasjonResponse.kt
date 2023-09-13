package no.nav.melosys.integrasjon.ereg.organisasjon

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.melosys.exception.TekniskException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class OrganisasjonResponse {

    open class OrganisasjonBase(
        val organisasjonsnummer: String? = null,
        val navn: Navn? = null
    )

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = JuridiskEnhet::class, name = "JuridiskEnhet"),
        JsonSubTypes.Type(value = Organisasjonsledd::class, name = "Organisasjonsledd"),
        JsonSubTypes.Type(value = Virksomhet::class, name = "Virksomhet"),
    )
    open class Organisasjon(
        val organisasjonDetaljer: OrganisasjonDetaljer? = null,
        val type: String? = null
    ) : OrganisasjonBase() {

        fun tilJsonString(): String = try {
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .writeValueAsString(this)
        } catch (e: JsonProcessingException) {
            throw TekniskException("Kunne ikke konvertere organisasjon til json string", e)
        }
    }

    class Virksomhet : Organisasjon() {
        val bestaarAvOrganisasjonsledd: List<BestaarAvOrganisasjonsledd>? = null
        val inngaarIJuridiskEnheter: List<InngaarIJuridiskEnhet>? = null
        val virksomhetDetaljer: VirksomhetDetaljer? = null
    }

    class VirksomhetDetaljer {
        val oppstartsdato: LocalDate? = null
        val eierskiftedato: LocalDate? = null
        val nedleggelsesdato: LocalDate? = null
        val enhetstype: String? = null
        val ubemannetVirksomhet: Boolean? = null
    }

    open class VirksomhetNoekkelinfo : OrganisasjonBase()

    data class DriverVirksomhet(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null
    ) : VirksomhetNoekkelinfo()

        data class JuridiskEnhetFisjon(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null,
        val virkningsdato: LocalDate? = null,
        val juridiskEnhet: JuridiskEnhet? = null
    )

    data class JuridiskEnhetFusjon(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null,
        val virkningsdato: LocalDate? = null,
        val juridiskEnhet: JuridiskEnhet? = null
    )

    data class JuridiskEnhetKnytning(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null,
        val knytning: String? = null,
        val juridiskEnhet: JuridiskEnhet? = null
    )

    data class Kapitalopplysninger(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null,
        val valuta: String? = null,
        val kapital: BigDecimal? = null,
        val kapitalInnbetalt: BigDecimal? = null,
        val kapitalBundetKs: String? = null,
        val fritekst: String? = null
    )

    data class JuridiskEnhetDetaljer(
        val sektorkode: String? = null,
        val enhetstype: String? = null,
        val registrertStiftelsesregisteret: Boolean? = null,
        val harAnsatte: Boolean? = null,
        val kapitalopplysninger: List<Kapitalopplysninger>? = mutableListOf(),
        val foretaksregisterRegistreringer: List<Foretaksregister>? = mutableListOf()
    )


    data class JuridiskEnhet(
        val bestaarAvOrganisasjonsledd: List<BestaarAvOrganisasjonsledd> = listOf(),
        val driverVirksomheter: List<DriverVirksomhet> = listOf(),
        val fisjoner: List<JuridiskEnhetFisjon> = listOf(),
        val fusjoner: List<JuridiskEnhetFusjon> = listOf(),
        val knytninger: List<JuridiskEnhetKnytning> = listOf(),
        val juridiskEnhetDetaljer: JuridiskEnhetDetaljer? = null
    ) : Organisasjon(type = "JuridiskEnhet")

    data class BestaarAvOrganisasjonsledd(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null,
        val organisasjonsledd: Organisasjonsledd? = null

    )

    data class Foretaksregister(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null,
        val registrert: Boolean? = null
    )

    data class OrganisasjonsleddDetaljer(
        val enhetstype: String? = null,
        val sektorkode: String? = null
    )

    abstract class JuridiskEnhetNoekkelinfo : OrganisasjonBase()

    data class InngaarIJuridiskEnhet(
        val bruksperiode: Bruksperiode? = null,
        val gyldighetsperiode: Gyldighetsperiode? = null
    ) : JuridiskEnhetNoekkelinfo()

    data class Organisasjonsledd(
        val organisasjonsleddUnder: List<BestaarAvOrganisasjonsledd> = emptyList(),
        val organisasjonsleddOver: List<BestaarAvOrganisasjonsledd> = emptyList(),
        val driverVirksomheter: List<DriverVirksomhet> = emptyList(),
        val inngaarIJuridiskEnheter: List<InngaarIJuridiskEnhet> = emptyList(),
        val organisasjonsleddDetaljer: OrganisasjonsleddDetaljer? = null
    ) : Organisasjon(type = "Organisasjonsledd") // Assuming Organisasjon is also converted to Kotlin


    data class Navn(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val sammensattnavn: String? = null,
        val navnelinje1: String? = null,
        val navnelinje2: String? = null,
        val navnelinje3: String? = null,
        val navnelinje4: String? = null,
        val navnelinje5: String? = null
    )

    data class Bruksperiode(
        val fom: LocalDateTime,
        val tom: LocalDateTime? = null
    )

    data class Gyldighetsperiode(
        val fom: LocalDate,
        val tom: LocalDate? = null
    )

    data class OrganisasjonDetaljer(
        var registreringsdato: LocalDateTime? = null,
        var sistEndret: LocalDate? = null,
        var maalform: String? = null,
        var opphoersdato: LocalDate? = null,
        var dublettAv: Organisasjon? = null,
        var dubletter: List<Organisasjon>? = null,
        var registrertMVA: List<MVA>? = null,
        var telefaksnummer: List<Telefonnummer>? = null,
        var telefonnummer: List<Telefonnummer>? = null,
        var statuser: List<Status>? = null,
        var forretningsadresser: List<Adresse>? = null,
        var postadresser: List<Adresse>? = null,
        var navSpesifikkInformasjon: NAVSpesifikkInformasjon? = null,
        var internettadresser: List<Internettadresse>? = null,
        var epostadresser: List<Epostadresse>? = null,
        var naeringer: List<Naering>? = null,
        var underlagtHjemlandLovgivningForetaksform: List<UnderlagtHjemlandLovgivningForetaksform>? = null,
        var navn: List<Navn> = ArrayList(),
        var formaal: List<Formaal>? = null,
        var mobiltelefonnummer: List<Telefonnummer>? = null,
        var stiftelsesdato: LocalDate? = null,
        var hjemlandregistre: List<Hjemlandregister>? = null,
        var enhetstyper: List<Enhetstype>? = null,
        var ansatte: List<Ansatte>? = null
    )

    data class MVA(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val registrertIMVA: Boolean? = null
    )

    data class Telefonnummer(
        var bruksperiode: Bruksperiode,
        var gyldighetsperiode: Gyldighetsperiode,
        var nummer: String? = null,
        var telefontype: String? = null
    )

    data class Status(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val kode: String
    )

    data class Adresse(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val adresselinje1: String? = null,
        val adresselinje2: String? = null,
        val adresselinje3: String? = null,
        val postnummer: String? = null,
        val poststed: String? = null,
        val kommunenummer: String? = null,
        val landkode: String? = null
    )

    data class NAVSpesifikkInformasjon(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val erIA: Boolean
    )

    data class Internettadresse(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val adresse: String
    )

    data class Epostadresse(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val adresse: String
    )

    data class Naering(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val naeringskode: String? = null,
        val hjelpeenhet: Boolean? = null
    )

    data class UnderlagtHjemlandLovgivningForetaksform(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val landkode: String? = null,
        val foretaksform: String? = null,
        val beskrivelseHjemland: String? = null,
        val beskrivelseNorge: String? = null
    )

    data class Formaal(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val formaal: String? = null
    )

    data class Hjemlandregister(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val registernummer: String? = null,
        val navn1: String? = null,
        val navn2: String? = null,
        val navn3: String? = null,
        val postadresse: Adresse? = null
    )

    data class Enhetstype(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val enhetstype: String? = null
    )

    data class Ansatte(
        val bruksperiode: Bruksperiode,
        val gyldighetsperiode: Gyldighetsperiode,
        val antall: Long? = null
    )
}
