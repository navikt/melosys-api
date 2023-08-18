package no.nav.melosys.domain.person

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.brev.Postadresse
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.person.adresse.Adressebeskyttelse
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.person.familie.Familiemedlem
import java.time.LocalDate
import java.util.*

data class Personopplysninger(
    @JsonProperty("adressebeskyttelser") var adressebeskyttelser: Collection<Adressebeskyttelse>,
    @JsonProperty("bostedsadresse") var bostedsadresse: Bostedsadresse?,
    @JsonProperty("dødsfall") var dødsfall: Doedsfall?,
    @JsonProperty("familiemedlemmer") var familiemedlemmer: Set<Familiemedlem>?,
    @JsonProperty("fødsel") var fødsel: Foedsel?,
    @JsonProperty("folkeregisteridentifikator") var folkeregisteridentifikator: Folkeregisteridentifikator?,
    @JsonProperty("kjønn") var kjønn: KjoennType?,
    @JsonProperty("kontaktadresser") var kontaktadresser: Collection<Kontaktadresse>,
    @JsonProperty("navn") var navn: Navn?,
    @JsonProperty("oppholdsadresser") var oppholdsadresser: Collection<Oppholdsadresse>,
    @JsonProperty("statsborgerskap") var statsborgerskap: Collection<Statsborgerskap>
) : Persondata {

    override fun erPersonDød(): Boolean = dødsfall?.dødsdato() != null

    override fun harStrengtAdressebeskyttelse(): Boolean = adressebeskyttelser.any { it.erStrengtFortrolig() }

    override fun manglerGyldigRegistrertAdresse(): Boolean {
        return hentGjeldendePostadresse() != null
    }


    override fun manglerBostedsadresse(): Boolean = finnBostedsadresse().isEmpty
    override fun manglerPostnummer(): Boolean = bostedsadresse?.strukturertAdresse?.postnummer?.isEmpty() ?: true

    override fun hentFolkeregisterident(): String? = folkeregisteridentifikator?.identifikasjonsnummer()

    override fun hentAlleStatsborgerskap(): Set<Land> = statsborgerskap.map { s: Statsborgerskap ->
        Land.av(s.landkode())
    }.toSet()

    override fun hentKjønnType(): KjoennType? = kjønn

    @JsonIgnore
    override fun getFornavn(): String? = navn?.fornavn()

    @JsonIgnore
    override fun getMellomnavn(): String? = navn?.mellomnavn()

    @JsonIgnore
    override fun getEtternavn(): String? = navn?.etternavn()

    @JsonIgnore
    override fun getSammensattNavn(): String? = navn?.tilSammensattNavn()

    override fun hentFamiliemedlemmer(): Set<Familiemedlem>? = familiemedlemmer

    @JsonIgnore
    override fun getFødselsdato(): LocalDate? = fødsel?.fødselsdato()

    override fun finnBostedsadresse(): Optional<Bostedsadresse> = Optional.ofNullable(bostedsadresse)

    override fun finnKontaktadresse(): Optional<Kontaktadresse> =
        kontaktadresser
            .maxByOrNull { it.registrertDato() }
            .let { Optional.ofNullable(it) }

    override fun finnOppholdsadresse(): Optional<Oppholdsadresse> =
        oppholdsadresser
            .maxByOrNull { it.registrertDato() }
            .let { Optional.ofNullable(it) }

    /*
     * Vi følger anbefaling fra PDL om følgende prioritering:
     * Kontaktadresse med master PDL
     * Kontaktadresse fra Freg med nyeste fregGyldighetstidspunkt
     * Oppholdsadresse med master PDL
     * Oppholdsadresse med master Freg
     * Bostedsadresse
     */
    override fun hentGjeldendePostadresse(): Postadresse? =
        lagPostadresseFraKontaktadresser()
            .or { lagPostadresseFraOppholdsadresser() }
            .or { lagPostadresseFraBostedsadresse() }
            .orElse(null)

    private fun lagPostadresseFraKontaktadresser(): Optional<Postadresse?> =
        hentGjeldendeKontaktadresseFraMaster(Master.PDL)
            .or { hentGjeldendeKontaktadresseFraMaster(Master.FREG) }
            .map { kontaktadresse: Kontaktadresse -> lagPostadresseFraKontaktadresse(kontaktadresse) }

    private fun hentGjeldendeKontaktadresseFraMaster(master: Master): Optional<Kontaktadresse> =
        kontaktadresser
            .filter { master.name.equals(it.master(), ignoreCase = true) }
            .filter { it.erGyldig() }
            .maxByOrNull { it.registrertDato() }
            .let { Optional.ofNullable(it) }

    private fun lagPostadresseFraOppholdsadresser(): Optional<Postadresse?> =
        hentGjeldendeOppholdsadresseFraMaster(Master.PDL)
            .or { hentGjeldendeOppholdsadresseFraMaster(Master.FREG) }
            .map { oppholdsadresse: Oppholdsadresse -> lagPostadresseFraOppholdsadresse(oppholdsadresse) }

    private fun hentGjeldendeOppholdsadresseFraMaster(master: Master): Optional<Oppholdsadresse> =
        oppholdsadresser
            .filter { master.name.equals(it.master(), ignoreCase = true) }
            .filter { it.erGyldig() }
            .maxByOrNull { it.registrertDato() }
            .let { Optional.ofNullable(it) }

    private fun lagPostadresseFraBostedsadresse(): Optional<Postadresse?> = finnBostedsadresse()
        .map { bostedsadresse: Bostedsadresse ->
            Postadresse.lagPostadresse(
                bostedsadresse.coAdressenavn(),
                bostedsadresse.strukturertAdresse()
            )
        }

    private fun lagPostadresseFraKontaktadresse(kontaktadresse: Kontaktadresse): Postadresse? {
        if (kontaktadresse.strukturertAdresse() != null) {
            return Postadresse.lagPostadresse(kontaktadresse.coAdressenavn(), kontaktadresse.strukturertAdresse())
        } else if (kontaktadresse.semistrukturertAdresse() != null) {
            return Postadresse.lagPostadresse(kontaktadresse.coAdressenavn(), kontaktadresse.semistrukturertAdresse())
        }
        return null
    }

    private fun lagPostadresseFraOppholdsadresse(oppholdsadresse: Oppholdsadresse): Postadresse? {
        return if (oppholdsadresse.strukturertAdresse() != null) {
            Postadresse.lagPostadresse(
                oppholdsadresse.coAdressenavn(),
                oppholdsadresse.strukturertAdresse()
            )
        } else null
    }
}
