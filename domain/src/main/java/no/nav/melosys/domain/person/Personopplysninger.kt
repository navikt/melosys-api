package no.nav.melosys.domain.person

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.brev.Postadresse
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.person.adresse.Adressebeskyttelse
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.person.familie.Familiemedlem
import java.time.LocalDate
import java.util.*
import java.util.function.Predicate

data class Personopplysninger(
    var adressebeskyttelser: Collection<Adressebeskyttelse>,
    var bostedsadresse: Bostedsadresse?,
    var dødsfall: Doedsfall?,
    var familiemedlemmer: Set<Familiemedlem>?,
    var fødsel: Foedsel?,
    var folkeregisteridentifikator: Folkeregisteridentifikator?,
    var kjønn: KjoennType?,
    var kontaktadresser: Collection<Kontaktadresse>,
    var navn: Navn?,
    var oppholdsadresser: Collection<Oppholdsadresse>,
    var statsborgerskap: Collection<Statsborgerskap>
) : Persondata {

    override fun erPersonDød(): Boolean {
        return dødsfall?.dødsdato() != null
    }

    override fun harStrengtAdressebeskyttelse(): Boolean {
        return adressebeskyttelser.stream()
            .anyMatch(Predicate { obj: Adressebeskyttelse -> obj.erStrengtFortrolig() })
    }

    override fun manglerRegistrertAdresse(): Boolean {
        return bostedsadresse == null && kontaktadresser.isEmpty() && oppholdsadresser.isEmpty()
    }

    override fun manglerBostedsadresse(): Boolean {
        return finnBostedsadresse().isEmpty
    }

    override fun hentFolkeregisterident(): String? {
        return folkeregisteridentifikator?.identifikasjonsnummer()
    }

    override fun hentAlleStatsborgerskap(): Set<Land> {
        return statsborgerskap.map { s: Statsborgerskap -> Land.av(s.landkode()) }.toSet()
    }

    override fun hentKjønnType(): KjoennType? {
        return kjønn
    }

    @JsonIgnore
    override fun getFornavn(): String? {
        return navn?.fornavn()
    }

    @JsonIgnore
    override fun getMellomnavn(): String? {
        return navn?.mellomnavn()
    }

    @JsonIgnore
    override fun getEtternavn(): String? {
        return navn?.etternavn()
    }

    @JsonIgnore
    override fun getSammensattNavn(): String? {
        return navn?.tilSammensattNavn();
    }

    override fun hentFamiliemedlemmer(): Set<Familiemedlem>? {
        return familiemedlemmer
    }

    @JsonIgnore
    override fun getFødselsdato(): LocalDate? {
        return fødsel?.fødselsdato()
    }

    override fun finnBostedsadresse(): Optional<Bostedsadresse> {
        return Optional.ofNullable(bostedsadresse)
    }

    override fun finnKontaktadresse(): Optional<Kontaktadresse> {
        return kontaktadresser.stream().max(Comparator.comparing { obj: Kontaktadresse -> obj.registrertDato() })
    }

    override fun finnOppholdsadresse(): Optional<Oppholdsadresse> {
        return oppholdsadresser.stream().max(Comparator.comparing { obj: Oppholdsadresse -> obj.registrertDato() })
    }

    /*
     * Vi følger anbefaling fra PDL om følgende prioritering:
     * Kontaktadresse med master PDL
     * Kontaktadresse fra Freg med nyeste fregGyldighetstidspunkt
     * Oppholdsadresse med master PDL
     * Oppholdsadresse med master Freg
     * Bostedsadresse
     */  override fun hentGjeldendePostadresse(): Postadresse? {
        return lagPostadresseFraKontaktadresser()
            .or { lagPostadresseFraOppholdsadresser() }
            .or { lagPostadresseFraBostedsadresse() }
            .orElse(null)
    }

    private fun lagPostadresseFraKontaktadresser(): Optional<Postadresse?> {
        return hentGjeldendeKontaktadresseFraMaster(Master.PDL)
            .or { hentGjeldendeKontaktadresseFraMaster(Master.FREG) }
            .map { kontaktadresse: Kontaktadresse -> lagPostadresseFraKontaktadresse(kontaktadresse) }
    }

    private fun hentGjeldendeKontaktadresseFraMaster(master: Master): Optional<Kontaktadresse> {
        return kontaktadresser.stream()
            .filter { a: Kontaktadresse -> master.name.equals(a.master(), ignoreCase = true) }
            .max(Comparator.comparing { obj: Kontaktadresse -> obj.registrertDato() })
    }

    private fun lagPostadresseFraOppholdsadresser(): Optional<Postadresse?> {
        return hentGjeldendeOppholdsadresseFraMaster(Master.PDL)
            .or { hentGjeldendeOppholdsadresseFraMaster(Master.FREG) }
            .map { oppholdsadresse: Oppholdsadresse -> lagPostadresseFraOppholdsadresse(oppholdsadresse) }
    }

    private fun hentGjeldendeOppholdsadresseFraMaster(master: Master): Optional<Oppholdsadresse> {
        return oppholdsadresser.stream()
            .filter(Predicate { a: Oppholdsadresse -> master.name.equals(a.master(), ignoreCase = true) })
            .max(Comparator.comparing { obj: Oppholdsadresse -> obj.registrertDato() })
    }

    private fun lagPostadresseFraBostedsadresse(): Optional<Postadresse?> {
        return finnBostedsadresse()
            .map { bostedsadresse: Bostedsadresse ->
                Postadresse.lagPostadresse(
                    bostedsadresse.coAdressenavn(),
                    bostedsadresse.strukturertAdresse()
                )
            }
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
