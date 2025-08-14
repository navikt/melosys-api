package no.nav.melosys.saksflytapi.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.instanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class ProsessinstansTest {

    @Test
    fun `skal lagre og hente streng data korrekt`() {
        val problematiskStreng = "Problematisk streng med # og = skal tåles"
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, problematiskStreng)

        prosessinstans.getData(ProsessDataKey.AVSENDER_NAVN) shouldBe problematiskStreng
    }

    @Test
    fun `skal lagre og hente Periode objekt korrekt`() {
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
    fun `skal lagre og hente Liste objekt korrekt`() {
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
    fun `skal lagre og hente MangelbrevBrevbestilling korrekt`() {
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
    fun `getData skal returnere null for ikke-eksisterende nøkkel`() {
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.getData(ProsessDataKey.AKTØR_ID) shouldBe null
        prosessinstans.finnData<String>(ProsessDataKey.AKTØR_ID) shouldBe null
    }

    @Test
    fun `hasData skal returnere false for ikke-eksisterende nøkkel`() {
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans.hasData(ProsessDataKey.AKTØR_ID) shouldBe false
    }

    @Test
    fun `hasData skal returnere true for eksisterende nøkkel`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "12345678901")

        prosessinstans.hasData(ProsessDataKey.AKTØR_ID) shouldBe true
    }

    @Test
    fun `hentData skal returnere verdi for eksisterende nøkkel`() {
        val aktørId = "12345678901"
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørId)

        prosessinstans.hentData(ProsessDataKey.AKTØR_ID) shouldBe aktørId
    }

    @Test
    fun `hentData skal kaste IllegalStateException for ikke-eksisterende nøkkel`() {
        val prosessinstans = Prosessinstans.forTest()

        shouldThrow<IllegalStateException> {
            prosessinstans.hentData(ProsessDataKey.AKTØR_ID)
        }.message shouldContain "Data for key ${ProsessDataKey.AKTØR_ID.kode} is not set"
    }

    @Test
    fun `hentData med type skal returnere typet verdi for eksisterende nøkkel`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding())

        val retrieved = prosessinstans.hentData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING)

        retrieved.shouldBeInstanceOf<MelosysEessiMelding>()
    }

    @Test
    fun `hentData med type skal kaste IllegalStateException for ikke-eksisterende nøkkel`() {
        val prosessinstans = Prosessinstans.forTest()

        shouldThrow<IllegalStateException> {
            prosessinstans.hentData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING)
        }.message shouldContain "Data for key ${ProsessDataKey.EESSI_MELDING.kode} is not set"
    }

    @Test
    fun `getData med standardverdi skal returnere standard når nøkkel ikke finnes`() {
        val prosessinstans = Prosessinstans.forTest()
        val defaultValue = "default"

        val result = prosessinstans.finnData<String>(ProsessDataKey.AKTØR_ID, defaultValue)

        result shouldBe defaultValue
    }

    @Test
    fun `getData med standardverdi skal returnere standard når nøkkel ikke finnes med type utledet`() {
        val prosessinstans = Prosessinstans.forTest()
        val defaultValue = "default"

        val result = prosessinstans.finnData(ProsessDataKey.AKTØR_ID, defaultValue)

        result shouldBe defaultValue
    }

    @Test
    fun `finnData skal returnere standard når nøkkel ikke finnes`() {
        val prosessinstans = Prosessinstans.forTest()
        val defaultValue = "default"

        val result = prosessinstans.finnData(ProsessDataKey.AKTØR_ID, String::class.java, defaultValue)

        result shouldBe defaultValue
    }

    @Test
    fun `hentBehandling skal returnere behandling når satt`() {
        val behandling = Behandling.forTest { id = 123L }
        val prosessinstans = Prosessinstans.forTest {
            medBehandling(behandling)
        }

        prosessinstans.hentBehandling shouldBe behandling
    }

    @Test
    fun `hentBehandling skal kaste IllegalStateException når ikke satt`() {
        val prosessinstans = Prosessinstans.forTest()

        shouldThrow<IllegalStateException> {
            prosessinstans.hentBehandling
        }.message shouldContain "behandling er ikke satt for prosessinstans med ID"
    }

    @Test
    fun `hentLåsReferanse skal returnere referanse når satt`() {
        val låsReferanse = "test-referanse"
        val prosessinstans = Prosessinstans.forTest {
            this.låsReferanse = låsReferanse
        }

        prosessinstans.hentLåsReferanse shouldBe låsReferanse
    }

    @Test
    fun `hentLåsReferanse skal kaste IllegalStateException når ikke satt`() {
        val prosessinstans = Prosessinstans.forTest()

        shouldThrow<IllegalStateException> {
            prosessinstans.hentLåsReferanse
        }.message shouldContain "låsReferanse er ikke satt for prosessinstans med ID"
    }

    @Test
    fun `hentJournalpostID skal returnere dataverdi når satt`() {
        val journalpostId = "12345"
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId)

        prosessinstans.hentJournalpostID() shouldBe journalpostId
    }

    @Test
    fun `hentJournalpostID skal returnere behandling journalpost når data ikke satt`() {
        val journalpostId = "67890"
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                initierendeJournalpostId = journalpostId
            }
        }

        prosessinstans.hentJournalpostID() shouldBe journalpostId
    }

    @Test
    fun `hentSaksbehandlerHvisTilordnes skal returnere saksbehandler når skal tilordnes`() {
        val saksbehandler = "Z123456"
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
            medData(ProsessDataKey.SKAL_TILORDNES, true)
        }

        prosessinstans.hentSaksbehandlerHvisTilordnes() shouldBe saksbehandler
    }

    @Test
    fun `hentSaksbehandlerHvisTilordnes skal returnere null når skal ikke tilordnes`() {
        val saksbehandler = "Z123456"
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
            medData(ProsessDataKey.SKAL_TILORDNES, false)
        }

        prosessinstans.hentSaksbehandlerHvisTilordnes() shouldBe null
    }

    @Test
    fun `hentAktørIDFraDataEllerSED skal returnere aktør ID fra data når tilgjengelig`() {
        val aktørId = "12345678901"
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.AKTØR_ID, aktørId)
        }

        prosessinstans.hentAktørIDFraDataEllerSED() shouldBe aktørId
    }

    @Test
    fun `hentAktørIDFraDataEllerSED skal returnere aktør ID fra SED når data ikke tilgjengelig`() {
        val aktørId = "98765432109"
        val eessiMelding = MelosysEessiMelding().apply { aktoerId = aktørId }
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, eessiMelding)
        }

        prosessinstans.hentAktørIDFraDataEllerSED() shouldBe aktørId
    }

    @Test
    fun `leggTilHendelse skal legge til hendelse i listen`() {
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
    fun `erFerdig skal returnere true når status er FERDIG`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.FERDIG
        }

        prosessinstans.erFerdig() shouldBe true
    }

    @Test
    fun `erFerdig skal returnere false når status ikke er FERDIG`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erFerdig() shouldBe false
    }

    @Test
    fun `erFeilet skal returnere true når status er FEILET`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.FEILET
        }

        prosessinstans.erFeilet() shouldBe true
    }

    @Test
    fun `erFeilet skal returnere false når status ikke er FEILET`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erFeilet() shouldBe false
    }

    @Test
    fun `erPåVent skal returnere true når status er PÅ_VENT`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.PÅ_VENT
        }

        prosessinstans.erPåVent() shouldBe true
    }

    @Test
    fun `erPåVent skal returnere false når status ikke er PÅ_VENT`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erPåVent() shouldBe false
    }

    @Test
    fun `erUnderBehandling skal returnere true når status er UNDER_BEHANDLING`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.UNDER_BEHANDLING
        }

        prosessinstans.erUnderBehandling() shouldBe true
    }

    @Test
    fun `erUnderBehandling skal returnere false når status ikke er UNDER_BEHANDLING`() {
        val prosessinstans = Prosessinstans.forTest {
            status = ProsessStatus.FERDIG
        }

        prosessinstans.erUnderBehandling() shouldBe false
    }

    @Test
    fun `equals skal returnere true for samme ID`() {
        val id = UUID.randomUUID()
        val prosessinstans1 = Prosessinstans.forTest { this.id = id }
        val prosessinstans2 = Prosessinstans.forTest { this.id = id }

        prosessinstans1 shouldBe prosessinstans2
    }

    @Test
    fun `equals skal returnere false for forskjellig ID`() {
        val prosessinstans1 = Prosessinstans.forTest { id = UUID.randomUUID() }
        val prosessinstans2 = Prosessinstans.forTest { id = UUID.randomUUID() }

        prosessinstans1 shouldNotBe prosessinstans2
    }

    @Test
    fun `equals skal returnere true for samme instans`() {
        val prosessinstans = Prosessinstans.forTest()

        prosessinstans shouldBe prosessinstans
    }

    @Test
    fun `toBuilder skal opprette builder med nåværende verdier`() {
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
    fun `setData skal håndtere null-verdier ved å retunere false`() {
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.AKTØR_ID, null as String?)
            medData(ProsessDataKey.EESSI_MELDING, null as Any?)
        }

        prosessinstans.hasData(ProsessDataKey.AKTØR_ID) shouldBe false
        prosessinstans.hasData(ProsessDataKey.EESSI_MELDING) shouldBe false
    }

    @Test
    fun `getData skal kaste IllegalStateException ved ugyldig JSON`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, "ugyldig json {")

        shouldThrow<IllegalStateException> {
            prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java)
        }.run {
            message shouldContain "Ugyldig JSON for ${ProsessDataKey.EESSI_MELDING}"
            message shouldContain "ved deserialisering til MelosysEessiMelding"
            cause.shouldBeInstanceOf<com.fasterxml.jackson.core.JsonParseException>()
        }
    }

    @Test
    fun `getData skal kaste IllegalStateException ved mapping-feil`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, "{\"ugyldigFelt\": \"verdi\"}")

        shouldThrow<IllegalStateException> {
            prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java)
        }.run {
            message shouldContain "Mapping-feil for ${ProsessDataKey.EESSI_MELDING}"
            message shouldContain "ved deserialisering til MelosysEessiMelding"
            cause shouldBe instanceOf<com.fasterxml.jackson.databind.JsonMappingException>()
        }

    }

    @Test
    fun `hentData skal kaste IllegalStateException ved ugyldig JSON`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, "ugyldig json {")

        shouldThrow<IllegalStateException> {
            prosessinstans.hentData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING)
        }.run {
            message shouldContain "Ugyldig JSON for ${ProsessDataKey.EESSI_MELDING}"
            message shouldContain "ved deserialisering til MelosysEessiMelding"
            cause shouldBe instanceOf<com.fasterxml.jackson.core.JsonParseException>()
        }
    }

    @Test
    fun `finnData skal kaste IllegalStateException ved ugyldig JSON`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, "ugyldig json {")

        shouldThrow<IllegalStateException> {
            prosessinstans.finnData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING)
        }.run {
            message shouldContain "Ugyldig JSON for ${ProsessDataKey.EESSI_MELDING}"
            message shouldContain "ved deserialisering til MelosysEessiMelding"
            cause shouldBe instanceOf<com.fasterxml.jackson.core.JsonParseException>()
        }
    }

    @Test
    fun `getData med TypeReference skal kaste IllegalStateException ved ugyldig JSON`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, "ugyldig json {")

        shouldThrow<IllegalStateException> {
            prosessinstans.getData(ProsessDataKey.OPPHOLDSLAND, object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
        }.run {
            message shouldContain "Ugyldig JSON for ${ProsessDataKey.OPPHOLDSLAND}"
            message shouldContain "ved deserialisering til"
            cause shouldBe instanceOf<com.fasterxml.jackson.core.JsonParseException>()
        }
    }
}
