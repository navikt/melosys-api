package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProsessinstansFerdigListenerTest {
    private lateinit var prosessinstansRepository: ProsessinstansRepository
    private lateinit var prosessinstansDispatcher: ProsessinstansDispatcher
    private lateinit var prosessinstansFerdigListener: ProsessinstansFerdigListener

    @BeforeEach
    fun setup() {
        prosessinstansRepository = mockk<ProsessinstansRepository>()
        prosessinstansDispatcher = mockk<ProsessinstansDispatcher>()
        prosessinstansFerdigListener = ProsessinstansFerdigListener(prosessinstansRepository, prosessinstansDispatcher)
    }

    @Test
    fun prosessinstansFerdig_harIngenLås_gjørIngenting() {
        val ferdigProsessinstans: Prosessinstans = lagProsessInstans()

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))

        verify {
            prosessinstansRepository wasNot Called
            prosessinstansDispatcher wasNot Called
        }
    }

    @Test
    fun prosesssinstansFerdig_harLåsFinnesIngenPåVent_gjørIngenting() {
        val ferdigProsessinstans = lagProsessInstans { låsReferanse = "12_12_1" }
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns emptySet()

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))

        verify {
            prosessinstansDispatcher wasNot Called
        }
    }

    @Test
    fun prosessinstansFerdig_harLåsIngenAktiveReferanser_starterTidligstOpprettetProsessinstans() {
        val ferdigProsessinstans = lagProsessInstans { låsReferanse = "12_12_1" }

        val prosessinstansUlikReferanse = lagProsessInstans {
            låsReferanse = "13_12_1"
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = "12_13_1"
            registrertDato = LocalDateTime.now().minusDays(1)
        }
        val senestOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = "12_14_1"
            registrertDato = LocalDateTime.now()
        }
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit
        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            prosessinstansUlikReferanse,
            tidligstOpprettetProsessinstans,
            senestOpprettetProsessinstans
        )

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))

        verify { prosessinstansDispatcher.dispatch(tidligstOpprettetProsessinstans) }
        tidligstOpprettetProsessinstans.status.shouldBe(ProsessStatus.KLAR)
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `start eldste sub-prosesser først`() {
        val lås1 = "12_13_1"
        val lås2 = "12_14_1"

        val rootProsessinstans = lagProsessInstans {
            låsReferanse = lås1
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås2
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val subProsessinstansEldst = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now().minusDays(1)
        }

        val subProsessinstansNy = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now()
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            subProsessinstansEldst,
            subProsessinstansNy

        )
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(rootProsessinstans))

        verify(exactly = 1) { prosessinstansDispatcher.dispatch(subProsessinstansEldst) }
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `start sub-prosesser før root-prosessers`() {
        val lås1 = "12_13_1"
        val lås2 = "12_14_1"

        val rootProsessinstans1 = lagProsessInstans {
            låsReferanse = lås1
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås2
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val subProsessinstansEldst = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans1.id)
            registrertDato = LocalDateTime.now().minusDays(1)
        }
        val subProsessinstansNy = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans1.id)
            registrertDato = LocalDateTime.now()
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            subProsessinstansNy,
        )
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(subProsessinstansEldst))

        verify(exactly = 1) { prosessinstansDispatcher.dispatch(subProsessinstansNy) }
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `start eldste sub-prosesser først når duplikat`() {
        val lås = "12_13_1"

        val rootProsessinstans = lagProsessInstans {
            låsReferanse = lås
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val subProsessinstansEldst = lagProsessInstans {
            låsReferanse = lås
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now().minusDays(1)
        }

        val subProsessinstansNy = lagProsessInstans {
            låsReferanse = lås
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now()
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            subProsessinstansEldst,
            subProsessinstansNy

        )
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(rootProsessinstans))

        verify(exactly = 1) { prosessinstansDispatcher.dispatch(subProsessinstansEldst) }
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `start neste sibling på vent når vi har duplikat låp på root prosesser`() {
        val lås = "12_13_1"

        val rootProsessinstans1 = lagProsessInstans {
            registrertDato = LocalDateTime.now().minusDays(2)
            låsReferanse = lås
        }
        val rootProsessinstans2 = lagProsessInstans {
            registrertDato = LocalDateTime.now().minusDays(1)
            låsReferanse = lås
        }
        val subProsessinstans1 = lagProsessInstans {
            låsReferanse = lås
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans1.id)
            registrertDato = LocalDateTime.now().minusHours(2)
        }
        val subProsessinstans2 = lagProsessInstans {
            låsReferanse = lås
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans1.id)
            registrertDato = LocalDateTime.now().minusHours(1)
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            rootProsessinstans2,
            subProsessinstans2,

        )
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(subProsessinstans1))

        verify(exactly = 1) { prosessinstansDispatcher.dispatch(subProsessinstans2) }
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `start eldste prosesser først`() {
        val lås = "12_13_1"

        val rootProsessinstans = lagProsessInstans {
            låsReferanse = lås
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val nyesteOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås
            registrertDato = LocalDateTime.now()
        }
        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            nyesteOpprettetProsessinstans,
        )
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(rootProsessinstans))

        verify(exactly = 1) { prosessinstansDispatcher.dispatch(tidligstOpprettetProsessinstans) }
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `prosessinstansFeilet med SØKNAD-låsreferanse slipper fram neste i gruppen`() {
        // Digital søknad serialiseres per søknadsgruppe (MELOSYS-8151). Feiler én del, må de øvrige
        // delene slippes fram — ellers står de PÅ_VENT til neste oppstart.
        val gruppeId = UUID.randomUUID()
        val feiletProsessinstans = lagProsessInstans { låsReferanse = "${gruppeId}_${UUID.randomUUID()}" }
        val nesteIGruppen = lagProsessInstans {
            låsReferanse = "${gruppeId}_${UUID.randomUUID()}"
            registrertDato = LocalDateTime.now().minusMinutes(1)
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(nesteIGruppen)
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFeilet(ProsessinstansFeiletEvent(feiletProsessinstans))

        verify(exactly = 1) { prosessinstansDispatcher.dispatch(nesteIGruppen) }
        nesteIGruppen.status.shouldBe(ProsessStatus.KLAR)
        confirmVerified(prosessinstansDispatcher)
    }

    @Test
    fun `prosessinstansFeilet med annen låsreferansetype slipper ikke fram noe`() {
        // Den viktigste avgrensningen: opplåsing ved FEILET er bevisst begrenset til SØKNAD.
        // For andre prosesstyper (her SED, formatet {tall}_{alfanumerisk}_{tall}) er rekkefølgen
        // mellom instanser i samme gruppe faglig viktigere enn framdrift — en feilet prosess skal
        // fortsatt blokkere gruppen og håndteres av restart/gjenoppretting. Uten denne testen ville
        // en fjerning av type-sjekken endret oppførsel for alle prosesstyper uten at noe feilet.
        val feiletProsessinstans = lagProsessInstans { låsReferanse = "12_abc_1" }
        val nesteISammeGruppe = lagProsessInstans {
            låsReferanse = "12_def_1"
            registrertDato = LocalDateTime.now().minusMinutes(1)
        }

        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(nesteISammeGruppe)

        prosessinstansFerdigListener.prosessinstansFeilet(ProsessinstansFeiletEvent(feiletProsessinstans))

        verify { prosessinstansDispatcher wasNot Called }
        nesteISammeGruppe.status.shouldBe(ProsessStatus.PÅ_VENT)
    }

    @Test
    fun `prosessinstansFeilet uten låsreferanse gjør ingenting`() {
        val feiletProsessinstans = lagProsessInstans()

        prosessinstansFerdigListener.prosessinstansFeilet(ProsessinstansFeiletEvent(feiletProsessinstans))

        verify {
            prosessinstansRepository wasNot Called
            prosessinstansDispatcher wasNot Called
        }
    }

    @Test
    fun `prosessinstans i gammelt låsreferanse-format velter ikke opplåsingen`() {
        // Regresjonsvern (MELOSYS-8151): før gruppe-serialiseringen var SØKNAD-låsreferansen en bar
        // skjemaId-UUID. Slike rader lever videre i databasen etter deploy, og denne lytteren parser
        // låsreferansen til ALLE prosessinstanser som står PÅ_VENT ved hvert ferdig-event. Godtok
        // ikke LåsReferanseType.SØKNAD det gamle formatet, ville den gamle raden kastet
        // IllegalArgumentException og stanset opplåsingen for HELE køen — også for andre
        // prosesstyper, siden lista traverseres i én filter-operasjon.
        val gruppeId = UUID.randomUUID()
        val ferdigProsessinstans = lagProsessInstans {
            låsReferanse = "${gruppeId}_${UUID.randomUUID()}"
            status = ProsessStatus.FERDIG
        }
        val gammelBarUuidPåVent = lagProsessInstans {
            låsReferanse = UUID.randomUUID().toString()
            registrertDato = LocalDateTime.now().minusMinutes(2)
        }
        val nesteISammeGruppe = lagProsessInstans {
            låsReferanse = "${gruppeId}_${UUID.randomUUID()}"
            registrertDato = LocalDateTime.now().minusMinutes(1)
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns
            setOf(gammelBarUuidPåVent, nesteISammeGruppe)
        every { prosessinstansDispatcher.dispatch(any()) } returns Unit

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))

        // Den gamle raden parses uten å kaste, og riktig prosessinstans slippes fram.
        verify(exactly = 1) { prosessinstansDispatcher.dispatch(nesteISammeGruppe) }
        gammelBarUuidPåVent.status shouldBe ProsessStatus.PÅ_VENT
    }

    private fun lagProsessInstans(block: Prosessinstans.() -> Unit = {}) = Prosessinstans.forTest {
        registrertDato = LocalDateTime.now()
        status = ProsessStatus.PÅ_VENT
        id = UUID.randomUUID()
    }.apply {
        block()
    }
}
