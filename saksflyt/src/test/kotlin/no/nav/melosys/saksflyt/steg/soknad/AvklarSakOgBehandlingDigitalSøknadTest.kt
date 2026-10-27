package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class AvklarSakOgBehandlingDigitalSøknadTest {

    @MockK lateinit var skjemaSakMappingService: SkjemaSakMappingService
    @MockK lateinit var nySakOppretter: DigitalSøknadNySakOppretter
    @MockK lateinit var eksisterendeSakHåndterer: DigitalSøknadEksisterendeSakHåndterer

    private lateinit var steg: AvklarSakOgBehandlingDigitalSøknad

    private val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto()
    private val relaterteSkjemaIder = listOf(UUID.randomUUID())

    private val prosessinstans = Prosessinstans.forTest {
        medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
        medData(ProsessDataKey.DIGITAL_SØKNAD_RELATERTE_SKJEMA_IDER, relaterteSkjemaIder)
    }

    @BeforeEach
    fun setup() {
        steg = AvklarSakOgBehandlingDigitalSøknad(skjemaSakMappingService, nySakOppretter, eksisterendeSakHåndterer)
        every { nySakOppretter.opprett(any()) } just Runs
        every { eksisterendeSakHåndterer.håndter(any()) } just Runs
    }

    @Test
    fun `inngangsSteg returnerer AVKLAR_SAK_OG_BEHANDLING_DIGITAL_SØKNAD`() {
        steg.inngangsSteg() shouldBe ProsessSteg.AVKLAR_SAK_OG_BEHANDLING_DIGITAL_SØKNAD
    }

    @Test
    fun `oppretter ny sak når ingen relaterte skjemaer har gyldig sak`() {
        val skjemaIder = slot<Collection<UUID>>()
        every { skjemaSakMappingService.harMappingMedGyldigSaksnummerForSkjemaId(capture(skjemaIder)) } returns false

        steg.utfør(prosessinstans)

        skjemaIder.captured shouldContainExactlyInAnyOrder relaterteSkjemaIder + søknadsdata.skjema.id
        verify { nySakOppretter.opprett(prosessinstans) }
        verify(exactly = 0) { eksisterendeSakHåndterer.håndter(any()) }
        prosessinstans.gjelderNySakForDigitalSøknad() shouldBe true
    }

    @Test
    fun `bruker eksisterende sak når et relatert skjema har gyldig sak`() {
        every { skjemaSakMappingService.harMappingMedGyldigSaksnummerForSkjemaId(any()) } returns true

        steg.utfør(prosessinstans)

        verify { eksisterendeSakHåndterer.håndter(prosessinstans) }
        verify(exactly = 0) { nySakOppretter.opprett(any()) }
        prosessinstans.gjelderNySakForDigitalSøknad() shouldBe false
    }
}
