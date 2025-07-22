package no.nav.melosys.domain.adresse

import no.nav.melosys.domain.kodeverk.Land_iso2
import org.apache.commons.lang3.StringUtils

class StrukturertAdresse(
    var tilleggsnavn: String? = null,
    var gatenavn: String? = null,
    var husnummerEtasjeLeilighet: String? = null,
    var postboks: String? = null,
    var postnummer: String? = null,
    var poststed: String? = null,
    var region: String? = null,
    override var landkode: String? = null
) : Adresse {

    // Java-compatible secondary constructor (without tilleggsnavn and postboks)
    constructor(
        gatenavn: String?,
        husnummerEtasjeLeilighet: String?,
        postnummer: String?,
        poststed: String?,
        region: String?,
        landkode: String?
    ) : this(
        tilleggsnavn = null,
        gatenavn = gatenavn,
        husnummerEtasjeLeilighet = husnummerEtasjeLeilighet,
        postboks = null,
        postnummer = postnummer,
        poststed = poststed,
        region = region,
        landkode = landkode
    )


    override fun toString(): String = toList().joinToString(", ")

    override fun toList(): List<String?> {
        return listOf(
            tilleggsnavn,
            Adresse.sammenslå(gatenavn, husnummerEtasjeLeilighet),
            postboks,
            postnummer,
            poststed,
            region,
            landkode?.let { Land_iso2.valueOf(it).beskrivelse } ?: ""
        ).filterNot { it.isNullOrBlank() }
    }

    override fun erTom(): Boolean =
        StringUtils.isAllEmpty(
            tilleggsnavn, gatenavn, husnummerEtasjeLeilighet,
            postboks, postnummer, poststed, region, landkode
        )

    fun erGyldig(): Boolean =
        !erTom() && landkode != null && (
            if (erNorsk()) !postnummer.isNullOrBlank()
            else !StringUtils.isAllEmpty(
                tilleggsnavn, gatenavn, husnummerEtasjeLeilighet,
                postboks, postnummer, poststed, region
            )
            )
}
