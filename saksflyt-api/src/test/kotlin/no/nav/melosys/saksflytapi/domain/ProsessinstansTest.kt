package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

class ProsessinstansTest {

    @Test
    fun `should store and retrieve string data correctly`() {
        val problematiskStreng = "Problematisk streng med # og = skal tåles"
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, problematiskStreng)

        prosessinstans.getData(ProsessDataKey.AVSENDER_NAVN) shouldBe problematiskStreng
    }

    @Test
    fun `should store and retrieve Periode object correctly`() {
        val periode = Periode(LocalDate.now(), null)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SØKNADSPERIODE, periode)
        }

        val retrievedPeriode = prosessinstans.hentData<Periode>(ProsessDataKey.SØKNADSPERIODE)
        retrievedPeriode.shouldNotBeNull().run {
            fom shouldBe periode.fom
            tom shouldBe periode.tom
        }
    }

    @Test
    fun `should store and retrieve List object correctly`() {
        val oppholdsland = listOf("NOR", "SWE", "DNK")
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, oppholdsland)

        prosessinstans.finnData<List<String>>(ProsessDataKey.OPPHOLDSLAND)
            .shouldNotBeNull().run {
                shouldHaveSize(3)
                shouldContain("NOR")
                shouldContain("SWE")
                shouldContain("DNK")
            }
    }

    @Test
    fun `should store and retrieve MangelbrevBrevbestilling correctly`() {
        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MANGELBREV_BRUKER)
            .medBestillKopi(true)
            .medBestillUtkast(true)
            .build()
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling)

        val retrieved = prosessinstans.finnData<DokgenBrevbestilling>(ProsessDataKey.BREVBESTILLING)
        retrieved
            .shouldNotBeNull()
            .shouldBeInstanceOf<MangelbrevBrevbestilling>().run {
                produserbartdokument shouldBe Produserbaredokumenter.MANGELBREV_BRUKER
                isBestillKopi() shouldBe true
                isBestillUtkast() shouldBe true
            }
    }

    @Test
    fun `getData should return null for non-existent key`() {
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.getData(ProsessDataKey.AKTØR_ID) shouldBe null
        prosessinstans.finnData<String>(ProsessDataKey.AKTØR_ID) shouldBe null
    }

    @Test
    fun `hasData should return false for non-existent key`() {
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.hasData(ProsessDataKey.AKTØR_ID) shouldBe false
    }

    @Test
    fun `hasData should return true for existing key`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "12345678901")

        prosessinstans.hasData(ProsessDataKey.AKTØR_ID) shouldBe true
    }

    @Test
    fun `hentData should return value for existing key`() {
        val aktørId = "12345678901"
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørId)

        prosessinstans.hentData(ProsessDataKey.AKTØR_ID) shouldBe aktørId
    }

    @Test
    fun `hentData should throw IllegalStateException for non-existent key`() {
        val prosessinstans = Prosessinstans.forTest()

        val exception = assertThrows<IllegalStateException> {
            prosessinstans.hentData(ProsessDataKey.AKTØR_ID)
        }
        exception.message shouldContain "Data for key ${ProsessDataKey.AKTØR_ID.kode} is not set"
    }

    @Test
    fun `hentData with type should return typed value for existing key`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding())

        val retrieved = prosessinstans.hentData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING)

        retrieved.shouldBeInstanceOf<MelosysEessiMelding>()
    }

    //
    @Test
    fun `hentData with type should throw IllegalStateException for non-existent key`() {
        val prosessinstans = Prosessinstans.forTest()

        assertThrows<IllegalStateException> {
            prosessinstans.hentData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING)
        }.message shouldContain "Data for key ${ProsessDataKey.EESSI_MELDING.kode} is not set"
    }

    @Test
    fun `getData with default value should return default when key not found`() {
        val prosessinstans = Prosessinstans.forTest()
        val defaultValue = "default"

        val result = prosessinstans.finnData<String>(ProsessDataKey.AKTØR_ID, defaultValue)

        result shouldBe defaultValue
    }

    @Test
    fun `getData with default value should return default when key not with type inferred`() {
        val prosessinstans = Prosessinstans.forTest()
        val defaultValue = "default"

        val result = prosessinstans.finnData(ProsessDataKey.AKTØR_ID, defaultValue)

        result shouldBe defaultValue
    }

    @Test
    fun `finnData should return default when key not found`() {
        val prosessinstans = Prosessinstans.forTest()
        val defaultValue = "default"

        val result = prosessinstans.finnData(ProsessDataKey.AKTØR_ID, String::class.java, defaultValue)

        result shouldBe defaultValue
    }

    @Test
    fun `hentBehandling should return behandling when set`() {
        val behandling = Behandling.forTest { id = 123L }
        val prosessinstans = Prosessinstans.forTest {
            medBehandling(behandling)
        }

        prosessinstans.hentBehandling shouldBe behandling
    }

    @Test
    fun `hentBehandling should throw IllegalStateException when not set`() {
        val prosessinstans = Prosessinstans.forTest()

        assertThrows<IllegalStateException> {
            prosessinstans.hentBehandling
        }.message shouldContain "behandling er ikke satt for prosessinstans med ID"
    }

    @Test
    fun `hentLåsReferanse should return referanse when set`() {
        val låsReferanse = "test-referanse"
        val prosessinstans = Prosessinstans.forTest {
            this.låsReferanse = låsReferanse
        }

        prosessinstans.hentLåsReferanse shouldBe låsReferanse
    }

    @Test
    fun `hentLåsReferanse should throw IllegalStateException when not set`() {
        val prosessinstans = Prosessinstans.forTest()

        assertThrows<IllegalStateException> {
            prosessinstans.hentLåsReferanse
        }.message shouldContain "låsReferanse er ikke satt for prosessinstans med ID"
    }

    @Test
    fun `hentJournalpostID should return data value when set`() {
        val journalpostId = "12345"
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId)

        prosessinstans.hentJournalpostID() shouldBe journalpostId
    }

    @Test
    fun `hentJournalpostID should return behandling journalpost when data not set`() {
        val journalpostId = "67890"
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                initierendeJournalpostId = journalpostId
            }
        }

        prosessinstans.hentJournalpostID() shouldBe journalpostId
    }

    @Test
    fun `hentSaksbehandlerHvisTilordnes should return saksbehandler when skal tilordnes`() {
        val saksbehandler = "Z123456"
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
            medData(ProsessDataKey.SKAL_TILORDNES, true)
        }

        prosessinstans.hentSaksbehandlerHvisTilordnes() shouldBe saksbehandler
    }

    @Test
    fun `hentSaksbehandlerHvisTilordnes should return null when skal ikke tilordnes`() {
        val saksbehandler = "Z123456"
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
            medData(ProsessDataKey.SKAL_TILORDNES, false)
        }

        prosessinstans.hentSaksbehandlerHvisTilordnes() shouldBe null
    }

    @Test
    fun `hentAktørIDFraDataEllerSED should return aktør ID from data when available`() {
        val aktørId = "12345678901"
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.AKTØR_ID, aktørId)
        }

        prosessinstans.hentAktørIDFraDataEllerSED() shouldBe aktørId
    }

    @Test
    fun `hentAktørIDFraDataEllerSED should return aktør ID from SED when data not available`() {
        val aktørId = "98765432109"
        val eessiMelding = MelosysEessiMelding().apply { aktoerId = aktørId }
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, eessiMelding)
        }

        prosessinstans.hentAktørIDFraDataEllerSED() shouldBe aktørId
    }

    @Test
    fun `leggTilHendelse should add hendelse to list`() {
        val prosessinstans = Prosessinstans.forTest()
        val steg = ProsessSteg.OPPRETT_ARKIVSAK
        val exception = RuntimeException("Test exception")

        prosessinstans.leggTilHendelse(steg, exception)

        prosessinstans.hendelser.shouldHaveSize(1).single().run {
            steg shouldBe steg
            type shouldBe "RuntimeException"
            melding shouldContain "Test exception"
        }
    }

    @Test
    fun `erFerdig should return true when status is FERDIG`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.FERDIG
        }

        prosessinstans.erFerdig() shouldBe true
    }

    @Test
    fun `erFerdig should return false when status is not FERDIG`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erFerdig() shouldBe false
    }

    @Test
    fun `erFeilet should return true when status is FEILET`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.FEILET
        }

        prosessinstans.erFeilet() shouldBe true
    }

    @Test
    fun `erFeilet should return false when status is not FEILET`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erFeilet() shouldBe false
    }

    @Test
    fun `erPåVent should return true when status is PÅ_VENT`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.PÅ_VENT
        }

        prosessinstans.erPåVent() shouldBe true
    }

    @Test
    fun `erPåVent should return false when status is not PÅ_VENT`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erPåVent() shouldBe false
    }

    @Test
    fun `erUnderBehandling should return true when status is UNDER_BEHANDLING`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erUnderBehandling() shouldBe true
    }

    @Test
    fun `erUnderBehandling should return false when status is not UNDER_BEHANDLING`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.FERDIG
        }

        prosessinstans.erUnderBehandling() shouldBe false
    }

    @Test
    fun `equals should return true for same ID`() {
        val id = UUID.randomUUID()
        val prosessinstans1 = Prosessinstans.forTest { this.id = id }
        val prosessinstans2 = Prosessinstans.forTest { this.id = id }

        prosessinstans1 shouldBe prosessinstans2
    }

    @Test
    fun `equals should return false for different ID`() {
        val prosessinstans1 = Prosessinstans.forTest { id = UUID.randomUUID() }
        val prosessinstans2 = Prosessinstans.forTest { id = UUID.randomUUID() }

        prosessinstans1 shouldNotBe prosessinstans2
    }

    @Test
    fun `equals should return true for same instance`() {
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans shouldBe prosessinstans
    }



    @Test
    fun `toBuilder should create builder with current values`() {
        val originalId = UUID.randomUUID()
        val prosessinstans = Prosessinstans.forTest {
            id = originalId
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.UNDER_BEHANDLING
            låsReferanse = "test-referanse"
        }

        val builder = prosessinstans.toBuilder()
        val newProsessinstans = builder.build()

        newProsessinstans.run {
            id shouldBe originalId
            type shouldBe ProsessType.OPPRETT_SAK
            status shouldBe ProsessStatus.UNDER_BEHANDLING
            låsReferanse shouldBe "test-referanse"
        }
    }


    @Test
    fun `setData should handle null values gracefully`() {
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.AKTØR_ID, null as String?)
            medData(ProsessDataKey.EESSI_MELDING, null as Any?)
        }

        prosessinstans.hasData(ProsessDataKey.AKTØR_ID) shouldBe false
        prosessinstans.hasData(ProsessDataKey.EESSI_MELDING) shouldBe false
    }
}
