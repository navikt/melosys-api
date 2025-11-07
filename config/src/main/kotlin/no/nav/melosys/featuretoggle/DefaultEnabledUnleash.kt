package no.nav.melosys.featuretoggle

import io.getunleash.*
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Unleash-implementasjon som wrapper DefaultUnleash og gir default-enabled oppførsel
 * for ukjente/ukonfigurerte feature toggles i lokal utvikling.
 *
 * Ukjente flags blir ENABLED som standard, mens konfigurerte flags respekterer sin
 * eksplisitte enable/disable-status. Denne oppførselen gjelder kun i lokal utvikling
 * (!nais & !test profiler).
 *
 * Se [FEATURE_TOGGLES.md](../../../FEATURE_TOGGLES.md) for fullstendig dokumentasjon
 * om løsningsvalg, oppsett og bruk.
 *
 * @param wrappedUnleash Den faktiske DefaultUnleash-instansen som delegeres til
 */
class DefaultEnabledUnleash(private val wrappedUnleash: Unleash) : Unleash by wrappedUnleash {

    override fun isEnabled(toggleName: String): Boolean {
        return isEnabled(toggleName, false)
    }

    override fun isEnabled(toggleName: String, defaultSetting: Boolean): Boolean {
        val toggleDefinition = wrappedUnleash.more().getFeatureToggleDefinition(toggleName)

        return if (toggleDefinition.isPresent) {
            val enabled = wrappedUnleash.isEnabled(toggleName, defaultSetting)
            log.debug { "Toggle '$toggleName' er definert i Unleash: enabled=$enabled" }
            enabled
        } else {
            log.debug { "Toggle '$toggleName' er IKKE definert i Unleash, defaulter til ENABLED" }
            true
        }
    }

    override fun isEnabled(toggleName: String, context: UnleashContext): Boolean =
        isEnabled(toggleName, context, false)

    override fun isEnabled(toggleName: String, context: UnleashContext, defaultSetting: Boolean): Boolean {
        val toggleDefinition = wrappedUnleash.more().getFeatureToggleDefinition(toggleName)

        return if (toggleDefinition.isPresent) {
            val enabled = wrappedUnleash.isEnabled(toggleName, context, defaultSetting)
            log.debug { "Toggle '$toggleName' (med context) er definert i Unleash: enabled=$enabled" }
            enabled
        } else {
            log.debug { "Toggle '$toggleName' (med context) er IKKE definert i Unleash, defaulter til ENABLED" }
            true
        }
    }

    // Override BiPredicate-variant for å sikre korrekt delegering
    override fun isEnabled(
        toggleName: String,
        fallbackAction: java.util.function.BiPredicate<String, UnleashContext>
    ): Boolean = wrappedUnleash.isEnabled(toggleName, fallbackAction)

    // Override default-metoder for å sikre at de delegerer korrekt
    // (Disse metodene brukes ikke i kodebasen, men vi overrider dem
    // for å unngå IntelliJ-varsel om Java default-metoder som ikke blir delegert)

    override fun getVariant(toggleName: String): Variant = wrappedUnleash.getVariant(toggleName)

    override fun getVariant(toggleName: String, defaultValue: Variant): Variant = wrappedUnleash.getVariant(toggleName, defaultValue)

    override fun shutdown() {
        wrappedUnleash.shutdown()
    }
}
