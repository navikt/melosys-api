package no.nav.melosys.saksflyt.steg.brev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.doksys.DoksysFasade
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DistribuerJournalpostUtlandTest {

    private lateinit var doksysFasade: DoksysFasade
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService
    private lateinit var distribuerJournalpostUtland: DistribuerJournalpostUtland


    @BeforeEach
    fun settOpp() {
        doksysFasade = mockk(relaxed = true)
        utenlandskMyndighetService = mockk()
        distribuerJournalpostUtland = DistribuerJournalpostUtland(doksysFasade, utenlandskMyndighetService)
    }

    @Test
    fun `utfør distribuerbar journalpost og mottaker satt distribuerer journalpost`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {}
            medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "12345")
            medData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, Landkoder.SE)
            medData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VEDTAK)
        }
        every { utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.SE) } returns UtenlandskMyndighet().apply {
            institusjonskode = "123456"
            landkode = Land_iso2.SE
            navn = "Svenska myndighetan"
            gateadresse1 = "Svenskegatan 38"
            poststed = "Svenska stan"
            postnummer = "8080"
        }


        distribuerJournalpostUtland.utfør(prosessinstans)


        val slot = slot<StrukturertAdresse>()
        verify { doksysFasade.distribuerJournalpost("12345", capture(slot), Distribusjonstype.VEDTAK) }

        slot.captured.shouldNotBeNull().run {
            gatenavn shouldBe "Svenskegatan 38"
            postnummer shouldBe "8080"
            landkode shouldBe Landkoder.SE.kode
        }
    }

    @Test
    fun `utfør distribuer journalpost satt mottaker ikke satt kaster feil`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "123")
        }


        val exception = assertThrows<IkkeFunnetException> {
            distribuerJournalpostUtland.utfør(prosessinstans)
        }


        exception.message!! shouldContain "mottakerland ikke er satt"
    }

    @Test
    fun `utfør distribuer journalpost ikke satt distribuerer ikke journalpost`() {
        val prosessinstans = Prosessinstans.forTest { }


        distribuerJournalpostUtland.utfør(prosessinstans)


        verify(exactly = 0) { doksysFasade.distribuerJournalpost(any(), any()) }
    }
}
