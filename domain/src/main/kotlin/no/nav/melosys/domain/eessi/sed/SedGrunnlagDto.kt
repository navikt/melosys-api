package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import no.nav.melosys.domain.eessi.SedType

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "sedType", visible = true)
@JsonTypeIdResolver(SedGrunnlagTypeResolver::class)
open class SedGrunnlagDto {
    var sedType: String? = null

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var utenlandskIdent: MutableList<Ident> = mutableListOf()

    var bostedsadresse: Adresse? = null

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var arbeidsgivendeVirksomheter: MutableList<Virksomhet> = mutableListOf()

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var selvstendigeVirksomheter: MutableList<Virksomhet> = mutableListOf()

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var arbeidssteder: MutableList<Arbeidssted> = mutableListOf()

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var arbeidsland: MutableList<Arbeidsland> = mutableListOf()

    var harFastArbeidssted: Boolean? = null

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    var lovvalgsperioder: List<Lovvalgsperiode> = emptyList()

    var ytterligereInformasjon: String? = null

    fun erA003(): Boolean =
        SedType.A003.name.equals(sedType, ignoreCase = true) && this is SedGrunnlagA003Dto

    fun erA001(): Boolean =
        SedType.A001.name.equals(sedType, ignoreCase = true)
}
