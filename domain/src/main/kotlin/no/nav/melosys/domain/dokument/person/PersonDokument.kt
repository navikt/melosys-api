package no.nav.melosys.domain.dokument.person

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.brev.Postadresse
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse
import no.nav.melosys.domain.person.KjoennType
import no.nav.melosys.domain.person.Master
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.util.IsoLandkodeKonverterer
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors


/**
 * Representerer svar fra personregisteret (TPS)
 */
class PersonDokument : Persondata {
    var fnr: String? = null
    var sivilstand: Sivilstand? = null
    var sivilstandGyldighetsperiodeFom: LocalDate? = null

    /**
     * Kodeverk: Landkoder
     */
    var statsborgerskap: Land? = null

    /**
     * Kodeverk: Kjønnstyper
     */
    @JsonProperty("kjoenn")
    var kjønn: KjoennsType? = null
    private var fornavn: String? = null
    private var mellomnavn: String? = null
    private var etternavn: String? = null
    private var sammensattNavn: String? = null
    var familiemedlemmer: List<Familiemedlem> = ArrayList()

    @JsonProperty("foedselsdato")
    private var fødselsdato: LocalDate? = null
    var dødsdato: LocalDate? = null
    var diskresjonskode: Diskresjonskode? = null
    var personstatus: Personstatus? = null
    var statsborgerskapDato: LocalDate? = null
    var bostedsadresse: Bostedsadresse? = Bostedsadresse()
    var postadresse: UstrukturertAdresse? = UstrukturertAdresse()
    var midlertidigPostadresse: MidlertidigPostadresse? = MidlertidigPostadresse()
    var gjeldendePostadresse = UstrukturertAdresse()

    @JsonProperty(defaultValue = "false")
    private var erEgenAnsatt = false
    override fun erPersonDød(): Boolean {
        return dødsdato != null
    }

    override fun manglerGyldigRegistrertAdresse(): Boolean {
        return hentGjeldendePostadresse() == null
    }

    fun hentAnnenForelder(fnrGjeldendeForelder: String): Optional<Familiemedlem> {
        return familiemedlemmer.stream()
            .filter { obj: Familiemedlem -> obj.erForelder() }
            .filter { forelder: Familiemedlem -> fnrGjeldendeForelder != forelder.fnr }
            .findAny()
    }

    override fun finnBostedsadresse(): Optional<no.nav.melosys.domain.person.adresse.Bostedsadresse> {
        return if (bostedsadresse == null || bostedsadresse!!.erTom()) {
            Optional.empty()
        } else Optional.of(
            no.nav.melosys.domain.person.adresse.Bostedsadresse(
                bostedsadresse!!.tilStrukturertAdresse(), null, null,
                null, Master.TPS.name, Master.TPS.name, false
            )
        )
    }

    override fun finnKontaktadresse(): Optional<Kontaktadresse> {
        return if (postadresse == null || postadresse!!.erTom()) {
            Optional.empty()
        } else Optional.of(
            Kontaktadresse(
                null, lagSemistrukturertAdresse(postadresse!!), null,
                null, null, Master.TPS.name, Master.TPS.name, null,
                false
            )
        )
    }

    override fun finnOppholdsadresse(): Optional<Oppholdsadresse> {
        return Optional.empty()
    }

    override fun harStrengtAdressebeskyttelse(): Boolean {
        return diskresjonskode != null && diskresjonskode!!.erKode6()
    }

    override fun hentAlleStatsborgerskap(): MutableSet<Land?>? {
        return java.util.Set.of(statsborgerskap)
    }

    override fun hentKjønnType(): KjoennType? {
        return KjoennType.avKode(kjønn!!.getKode())
    }

    override fun hentFolkeregisterident(): String? {
        return fnr
    }

    override fun getFornavn(): String? {
        return fornavn
    }

    fun setFornavn(fornavn: String?) {
        this.fornavn = fornavn
    }

    override fun getMellomnavn(): String? {
        return mellomnavn
    }

    fun setMellomnavn(mellomnavn: String?) {
        this.mellomnavn = mellomnavn
    }

    override fun getEtternavn(): String? {
        return etternavn
    }

    fun setEtternavn(etternavn: String?) {
        this.etternavn = etternavn
    }

    override fun getSammensattNavn(): String? {
        return sammensattNavn
    }

    fun setSammensattNavn(sammensattNavn: String?) {
        this.sammensattNavn = sammensattNavn
    }

    override fun hentFamiliemedlemmer(): Set<no.nav.melosys.domain.person.familie.Familiemedlem> {
        return familiemedlemmer.stream().map { obj: Familiemedlem -> obj.tilDomene() }
            .collect(Collectors.toUnmodifiableSet())
    }

    override fun getFødselsdato(): LocalDate? {
        return fødselsdato
    }

    fun setFødselsdato(fødselsdato: LocalDate?) {
        this.fødselsdato = fødselsdato
    }

    override fun hentGjeldendePostadresse(): Postadresse? {
        return Postadresse(
            null,
            gjeldendePostadresse.adresselinje1,
            gjeldendePostadresse.adresselinje2,
            gjeldendePostadresse.adresselinje3,
            gjeldendePostadresse.adresselinje4,
            gjeldendePostadresse.postnr,
            gjeldendePostadresse.poststed,
            if (gjeldendePostadresse.land != null) IsoLandkodeKonverterer.tilIso2(gjeldendePostadresse.land!!.kode) else null,
            null
        )
    }

    fun setErEgenAnsatt(erEgenAnsatt: Boolean) {
        this.erEgenAnsatt = erEgenAnsatt
    }

    private fun lagSemistrukturertAdresse(ustrukturertAdresse: UstrukturertAdresse): SemistrukturertAdresse {
        return SemistrukturertAdresse(
            ustrukturertAdresse.adresselinje1,
            ustrukturertAdresse.adresselinje2,
            ustrukturertAdresse.adresselinje3,
            ustrukturertAdresse.adresselinje4,
            ustrukturertAdresse.postnr,
            ustrukturertAdresse.poststed,
            if (ustrukturertAdresse.land != null) IsoLandkodeKonverterer.tilIso2(ustrukturertAdresse.land!!.kode) else null
        )
    }
}
