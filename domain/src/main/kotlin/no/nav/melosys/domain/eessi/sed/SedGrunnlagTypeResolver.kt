package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import no.nav.melosys.domain.eessi.SedType

/**
 * Jackson TypeResolver for polymorphic deserialisering av SED grunnlag-typer.
 *
 * Denne resolveren håndterer dynamisk type-mapping basert på SED-type streng,
 * og returnerer riktig DTO-klasse for Jackson deserialisering.
 *
 * Brukes av [SedGrunnlagDto] via @JsonTypeIdResolver-annotasjon.
 */
class SedGrunnlagTypeResolver : TypeIdResolverBase() {

    private var baseType: JavaType? = null

    override fun init(javaType: JavaType) {
        this.baseType = javaType
    }

    override fun idFromValue(o: Any?): String = "N/A"

    override fun idFromValueAndType(o: Any?, aClass: Class<*>?): String? = null

    override fun idFromBaseType(): String? = null

    /**
     * Løser konkret Java-type basert på SED-type streng.
     *
     * @param databindContext Jackson databind-kontekst for type-konstruksjon
     * @param typeId SED-type streng (f.eks. "A003", "A001")
     * @return JavaType for riktig DTO-klasse
     * @throws IllegalStateException hvis baseType ikke er initialisert
     */
    override fun typeFromId(databindContext: DatabindContext, typeId: String): JavaType {
        val resolvedType = baseType ?: error("BaseType er ikke initialisert for ${this::class.simpleName}")

        val targetClass = resolveTargetClass(typeId)
        return databindContext.constructSpecializedType(resolvedType, targetClass)
    }

    private fun resolveTargetClass(typeId: String): Class<out SedGrunnlagDto> {
        // Sjekk om typeId er en gyldig SED-type
        if (!SED_TYPE_NAMES.contains(typeId)) {
            return DEFAULT_CLASS
        }

        val sedType = SedType.valueOf(typeId)
        return SED_GRUNNLAG_TYPE_MAPPING[sedType] ?: DEFAULT_CLASS
    }

    override fun getDescForKnownTypeIds(): String? = null

    override fun getMechanism(): JsonTypeInfo.Id = JsonTypeInfo.Id.NAME

    companion object {
        private val DEFAULT_CLASS: Class<out SedGrunnlagDto> = SedGrunnlagDto::class.java
        private val SED_TYPE_NAMES: Set<String> by lazy {
            SedType.entries.mapTo(HashSet()) { it.name }
        }

        private val SED_GRUNNLAG_TYPE_MAPPING: Map<SedType, Class<out SedGrunnlagDto>> = mapOf(
            SedType.A003 to SedGrunnlagA003Dto::class.java,
            SedType.A001 to SedGrunnlagDto::class.java
        )
    }
}
