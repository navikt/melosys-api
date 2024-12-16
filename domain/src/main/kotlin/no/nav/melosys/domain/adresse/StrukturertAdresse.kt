package no.nav.melosys.domain.adresse

import no.nav.melosys.domain.kodeverk.Land_iso2
import org.apache.commons.lang3.StringUtils

class StrukturertAdresse : Adresse {

    var tilleggsnavn: String? = null
    var gatenavn: String? = null
    var husnummerEtasjeLeilighet: String? = null
    var postboks: String? = null
    var postnummer: String? = null
    var poststed: String? = null
    var region: String? = null
    private var landkode: String? = null

    constructor() {}

    constructor(
        gatenavn: String?,
        husnummerEtasjeLeilighet: String?,
        postnummer: String?,
        poststed: String?,
        region: String?,
        landkode: String?
    ) {
        this.gatenavn = gatenavn
        this.husnummerEtasjeLeilighet = husnummerEtasjeLeilighet
        this.region = region
        this.postnummer = postnummer
        this.poststed = poststed
        this.landkode = landkode
    }

    constructor(
        tilleggsnavn: String?, gatenavn: String?, husnummerEtasjeLeilighet: String?, postboks: String?,
        postnummer: String?, poststed: String?, region: String?, landkode: String?
    ) {
        this.gatenavn = gatenavn
        this.husnummerEtasjeLeilighet = husnummerEtasjeLeilighet
        this.tilleggsnavn = tilleggsnavn
        this.postboks = postboks
        this.region = region
        this.postnummer = postnummer
        this.poststed = poststed
        this.landkode = landkode
    }

    override fun getLandkode(): String? {
        return landkode
    }

    fun setLandkode(landkode: String?) {
        this.landkode = landkode
    }

    override fun toString(): String {
        return java.lang.String.join(", ", toList())
    }

    override fun toList(): List<String?> {
        if(!Land_iso2.values().any { it.kode == landkode }) {
            return listOf("Resident outside of Norway")
        }

        return mutableListOf(
            tilleggsnavn, Adresse.sammenslå(gatenavn, husnummerEtasjeLeilighet),
            postboks, postnummer, poststed, region,
            if (landkode != null) Land_iso2.valueOf(landkode!!).beskrivelse else ""
        )
            .filter { !it.isNullOrBlank() }
    }

    override fun erTom(): Boolean {
        return StringUtils.isAllEmpty(
            tilleggsnavn, gatenavn, husnummerEtasjeLeilighet, postboks, postnummer, poststed,
            region, landkode
        )
    }

    fun erGyldig(): Boolean {
        if (erTom()) return false
        if (landkode == null) return false
        return if (erNorsk()) {
            !postnummer.isNullOrBlank()
        } else {
            !StringUtils.isAllEmpty(
                tilleggsnavn, gatenavn, husnummerEtasjeLeilighet, postboks, postnummer, poststed,
                region
            )
        }
    }
}
