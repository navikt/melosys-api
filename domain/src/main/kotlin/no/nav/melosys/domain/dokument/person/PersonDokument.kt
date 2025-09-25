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


/**
 * Representerer svar fra personregisteret (TPS)
 */
data class PersonDokument(
    var fnr: String? = null,
    var sivilstand: Sivilstand? = null,
    var sivilstandGyldighetsperiodeFom: LocalDate? = null,
    /**
     * Kodeverk: Landkoder
     */
    var statsborgerskap: Land? = null,
    /**
     * Kodeverk: Kjønnstyper
     */
    @JsonProperty("kjoenn")
    var kjønn: KjoennsType? = null,
    @JsonProperty("foedselsdato")
    override var fødselsdato: LocalDate? = null,
    var dødsdato: LocalDate? = null,
    var diskresjonskode: Diskresjonskode? = null,
    var personstatus: Personstatus? = null,
    var statsborgerskapDato: LocalDate? = null,
    var bostedsadresse: Bostedsadresse? = Bostedsadresse(),
    var postadresse: UstrukturertAdresse? = UstrukturertAdresse(),
    var midlertidigPostadresse: MidlertidigPostadresse? = MidlertidigPostadresse(),
    var gjeldendePostadresse: UstrukturertAdresse = UstrukturertAdresse(),
    @JsonProperty(defaultValue = "false")
    private var erEgenAnsatt: Boolean = false
) : Persondata {

    override var fornavn: String? = null
    override var mellomnavn: String? = null
    override var etternavn: String? = null
    override var sammensattNavn: String? = null
    var familiemedlemmer: List<Familiemedlem> = ArrayList()

    override fun erPersonDød(): Boolean = dødsdato != null

    override fun manglerGyldigRegistrertAdresse(): Boolean = false

    override fun finnBostedsadresse(): Optional<no.nav.melosys.domain.person.adresse.Bostedsadresse> =
        if (bostedsadresse == null || bostedsadresse!!.erTom()) {
            Optional.empty()
        } else Optional.of(
            no.nav.melosys.domain.person.adresse.Bostedsadresse(
                bostedsadresse!!.tilStrukturertAdresse(), null, null,
                null, Master.TPS.name, Master.TPS.name, false
            )
        )

    override fun finnKontaktadresse(): Optional<Kontaktadresse> =
        if (postadresse == null || postadresse!!.erTom()) {
            Optional.empty()
        } else Optional.of(
            Kontaktadresse(
                null, lagSemistrukturertAdresse(postadresse!!), null,
                null, null, Master.TPS.name, Master.TPS.name, null,
                false
            )
        )

    override fun finnOppholdsadresse(): Optional<Oppholdsadresse> = Optional.empty()

    override fun harStrengtAdressebeskyttelse(): Boolean = diskresjonskode?.erKode6() ?: false

    override fun hentAlleStatsborgerskap(): Set<Land> = listOfNotNull(statsborgerskap).toSet()

    override fun hentKjønnType(): KjoennType = KjoennType.avKode(kjønn?.kode)

    override fun hentFolkeregisterident(): String? = fnr


    override fun hentFamiliemedlemmer(): Set<no.nav.melosys.domain.person.familie.Familiemedlem> =
        familiemedlemmer.map { it.tilDomene() }.toSet()


    override fun hentGjeldendePostadresse() = Postadresse(
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
