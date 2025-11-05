package no.nav.melosys.featuretoggle

import io.getunleash.FeatureToggle
import io.getunleash.MoreOperations
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.*

class DefaultEnabledUnleashTest {

    private val delegate: Unleash = mockk()
    private val moreOperations: MoreOperations = mockk()
    private val unleash = DefaultEnabledUnleash(delegate)

    @Test
    fun `should return true for unknown toggle`() {
        val toggleName = "unknown.toggle"
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.empty()

        val result = unleash.isEnabled(toggleName)

        result shouldBe true
        verify(exactly = 1) { moreOperations.getFeatureToggleDefinition(toggleName) }
        verify(exactly = 0) { delegate.isEnabled(any<String>(), any<Boolean>()) }
    }

    @Test
    fun `should return actual state when toggle is defined and enabled`() {
        val toggleName = "known.toggle.enabled"
        val featureToggle: FeatureToggle = mockk()
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.of(featureToggle)
        every { delegate.isEnabled(toggleName, false) } returns true

        val result = unleash.isEnabled(toggleName)

        result shouldBe true
        verify(exactly = 1) { moreOperations.getFeatureToggleDefinition(toggleName) }
        verify(exactly = 1) { delegate.isEnabled(toggleName, false) }
    }

    @Test
    fun `should return actual state when toggle is defined and disabled`() {
        val toggleName = "known.toggle.disabled"
        val featureToggle: FeatureToggle = mockk()
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.of(featureToggle)
        every { delegate.isEnabled(toggleName, false) } returns false

        val result = unleash.isEnabled(toggleName)

        result shouldBe false
        verify(exactly = 1) { moreOperations.getFeatureToggleDefinition(toggleName) }
        verify(exactly = 1) { delegate.isEnabled(toggleName, false) }
    }

    @Test
    fun `should support UnleashContext parameter for defined toggle`() {
        val toggleName = "context.toggle"
        val featureToggle: FeatureToggle = mockk()
        val context = UnleashContext.builder().userId("test-user").build()
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.of(featureToggle)
        every { delegate.isEnabled(toggleName, context, false) } returns true

        val result = unleash.isEnabled(toggleName, context)

        result shouldBe true
        verify(exactly = 1) { moreOperations.getFeatureToggleDefinition(toggleName) }
        verify(exactly = 1) { delegate.isEnabled(toggleName, context, false) }
    }

    @Test
    fun `should return true for unknown toggle with context`() {
        val toggleName = "unknown.context.toggle"
        val context = UnleashContext.builder().userId("test-user").build()
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.empty()

        val result = unleash.isEnabled(toggleName, context)

        result shouldBe true
        verify(exactly = 1) { moreOperations.getFeatureToggleDefinition(toggleName) }
        verify(exactly = 0) { delegate.isEnabled(any<String>(), any<UnleashContext>(), any<Boolean>()) }
    }

    @Test
    fun `should respect defaultSetting parameter for known toggle`() {
        val toggleName = "known.toggle"
        val featureToggle: FeatureToggle = mockk()
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.of(featureToggle)
        every { delegate.isEnabled(toggleName, true) } returns false

        val result = unleash.isEnabled(toggleName, true)

        result shouldBe false
        verify(exactly = 1) { delegate.isEnabled(toggleName, true) }
    }

    @Test
    fun `should ignore defaultSetting for unknown toggle and return true`() {
        val toggleName = "unknown.toggle"
        every { delegate.more() } returns moreOperations
        every { moreOperations.getFeatureToggleDefinition(toggleName) } returns Optional.empty()

        val result = unleash.isEnabled(toggleName, false)

        result shouldBe true
        verify(exactly = 0) { delegate.isEnabled(any<String>(), any<Boolean>()) }
    }
}
