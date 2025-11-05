package no.nav.melosys.featuretoggle

import io.getunleash.*
import mu.KotlinLogging
import java.util.function.BiPredicate

private val log = KotlinLogging.logger {}

/**
 * Unleash implementation that wraps DefaultUnleash and provides default-enabled behavior
 * for unknown/unconfigured feature toggles.
 *
 * This is useful for local development with Unleash running in Docker Compose where not all
 * feature flags may be configured. Unknown flags will default to ENABLED, allowing new features
 * to work immediately without manual Unleash configuration.
 *
 * For flags that ARE configured in Unleash, their explicit enable/disable state is respected.
 *
 * @param delegate The actual DefaultUnleash instance to delegate to
 */
class DefaultEnabledUnleash(private val delegate: Unleash) : Unleash {

    override fun isEnabled(toggleName: String): Boolean {
        return isEnabled(toggleName, false)
    }

    override fun isEnabled(toggleName: String, defaultSetting: Boolean): Boolean {
        // Check if this toggle is defined/registered in Unleash
        val toggleDefinition = delegate.more().getFeatureToggleDefinition(toggleName)

        return if (toggleDefinition.isPresent) {
            // Toggle is defined - use actual Unleash state
            val enabled = delegate.isEnabled(toggleName, defaultSetting)
            log.info { "Toggle '$toggleName' is defined in Unleash: enabled=$enabled" }
            enabled
        } else {
            // Toggle is NOT defined - default to enabled for local development
            log.info { "Toggle '$toggleName' is NOT defined in Unleash, defaulting to ENABLED" }
            true
        }
    }

    override fun isEnabled(toggleName: String, context: UnleashContext): Boolean {
        return isEnabled(toggleName, context, false)
    }

    override fun isEnabled(toggleName: String, context: UnleashContext, defaultSetting: Boolean): Boolean {
        // Check if this toggle is defined/registered in Unleash
        val toggleDefinition = delegate.more().getFeatureToggleDefinition(toggleName)

        return if (toggleDefinition.isPresent) {
            // Toggle is defined - use actual Unleash state
            val enabled = delegate.isEnabled(toggleName, context, defaultSetting)
            log.debug { "Toggle '$toggleName' (with context) is defined in Unleash: enabled=$enabled" }
            enabled
        } else {
            // Toggle is NOT defined - default to enabled for local development
            log.debug { "Toggle '$toggleName' (with context) is NOT defined in Unleash, defaulting to ENABLED" }
            true
        }
    }

    override fun isEnabled(toggleName: String, fallbackAction: BiPredicate<String, UnleashContext>): Boolean {
        return delegate.isEnabled(toggleName, fallbackAction)
    }

    override fun isEnabled(
        toggleName: String,
        context: UnleashContext,
        fallbackAction: BiPredicate<String, UnleashContext>
    ): Boolean {
        return delegate.isEnabled(toggleName, context, fallbackAction)
    }

    override fun getVariant(toggleName: String): Variant {
        return delegate.getVariant(toggleName)
    }

    override fun getVariant(toggleName: String, defaultValue: Variant): Variant {
        return delegate.getVariant(toggleName, defaultValue)
    }

    override fun getVariant(toggleName: String, context: UnleashContext): Variant {
        return delegate.getVariant(toggleName, context)
    }

    override fun getVariant(toggleName: String, context: UnleashContext, defaultValue: Variant): Variant {
        return delegate.getVariant(toggleName, context, defaultValue)
    }

    override fun getFeatureToggleNames(): List<String> {
        return delegate.featureToggleNames
    }

    override fun more(): MoreOperations {
        return delegate.more()
    }

    override fun shutdown() {
        delegate.shutdown()
    }
}
