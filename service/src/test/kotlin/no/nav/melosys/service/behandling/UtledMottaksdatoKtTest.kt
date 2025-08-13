package no.nav.melosys.service.behandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.integrasjon.joark.JoarkFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
class UtledMottaksdatoKtTest {

    @RelaxedMockK
    lateinit var joarkFasade: JoarkFasade

    private lateinit var utledMottaksdato: UtledMottaksdato

    @BeforeEach
    fun setUp() {
        utledMottaksdato = UtledMottaksdato(joarkFasade)
    }

    @Test
    fun `getMottaksdato behandlingsårsak finnes returnerer mottaksdato`() {
        val behandling = Behandling.forTest {
            behandlingsårsak = Behandlingsaarsak()
        }
        behandling.behandlingsårsak?.mottaksdato = MOTTAKSDATO


        val utledetDato = utledMottaksdato.getMottaksdato(behandling)


        utledetDato shouldBe MOTTAKSDATO
        verify(exactly = 0) { joarkFasade.hentJournalpost(any()) }
    }

    @Test
    fun `getMottaksdato behandlingsårsak finnes ikke journalpost har dato returnerer forsendelse mottatt`() {
        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            forsendelseMottatt = FORSENDELSE_MOTTATT
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost
        val behandling = Behandling.forTest {
            initierendeJournalpostId = JOURNALPOST_ID
        }


        val utledetDato = utledMottaksdato.getMottaksdato(behandling)


        utledetDato shouldBe LocalDate.ofInstant(FORSENDELSE_MOTTATT, ZoneId.systemDefault())
    }

    @Test
    fun `getMottaksdato behandlingsårsak finnes ikke journalpost har ikke dato returnerer registrert dato`() {
        val journalpost = Journalpost(JOURNALPOST_ID)
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost
        val behandling = Behandling.forTest {
            registrertDato = REGISTRERT_DATO
            initierendeJournalpostId = JOURNALPOST_ID
        }


        val utledetDato = utledMottaksdato.getMottaksdato(behandling)


        utledetDato shouldBe REGISTRERT_DATO_LOCALDATE
    }

    @Test
    fun `getMottaksdato behandlingsårsak finnes ikke har ikke initierende journalpost returnerer registrert dato`() {
        val behandling = Behandling.forTest {
            registrertDato = REGISTRERT_DATO
        }


        val utledetDato = utledMottaksdato.getMottaksdato(behandling)


        utledetDato shouldBe REGISTRERT_DATO_LOCALDATE
        verify(exactly = 0) { joarkFasade.hentJournalpost(any()) }
    }

    @Test
    fun `getMottaksdato med journalpost behandlingsårsak finnes returnerer mottaksdato`() {
        val journalpost = Journalpost(JOURNALPOST_ID)
        val behandling = Behandling.forTest {
            behandlingsårsak = Behandlingsaarsak()
        }
        behandling.behandlingsårsak?.mottaksdato = MOTTAKSDATO


        val utledetDato = utledMottaksdato.getMottaksdato(behandling, journalpost)


        utledetDato shouldBe MOTTAKSDATO
    }

    @Test
    fun `getMottaksdato med journalpost behandlingsårsak finnes ikke journalpost har dato returnerer forsendelse mottatt`() {
        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            forsendelseMottatt = FORSENDELSE_MOTTATT
        }
        val behandling = Behandling.forTest()


        val utledetDato = utledMottaksdato.getMottaksdato(behandling, journalpost)


        utledetDato shouldBe LocalDate.ofInstant(FORSENDELSE_MOTTATT, ZoneId.systemDefault())
    }

    @Test
    fun `getMottaksdato med journalpost behandlingsårsak finnes ikke journalpost har ikke dato returnerer registrert dato`() {
        val journalpost = Journalpost(JOURNALPOST_ID)
        val behandling = Behandling.forTest {
            registrertDato = REGISTRERT_DATO
        }


        val utledetDato = utledMottaksdato.getMottaksdato(behandling, journalpost)


        utledetDato shouldBe REGISTRERT_DATO_LOCALDATE
    }

    @Test
    fun `getMottaksdato behandlingsårsak finnes ikke mottatte opplysninger har dato returnerer mottatte opplysninger mottaksdato`() {
        val mottatteOpplysningerMedDato = MottatteOpplysninger().apply {
            mottaksdato = MOTTAKSDATO
        }
        val behandling = Behandling.forTest {
            mottatteOpplysninger = mottatteOpplysningerMedDato
        }


        val utledetDato = utledMottaksdato.getMottaksdato(behandling)


        utledetDato shouldBe MOTTAKSDATO
    }

    companion object {
        private val MOTTAKSDATO = LocalDate.of(2022, 12, 1)
        private val REGISTRERT_DATO = Instant.parse("2021-12-01T12:00:00.00Z")
        private val REGISTRERT_DATO_LOCALDATE = LocalDate.ofInstant(REGISTRERT_DATO, ZoneId.systemDefault())
        private val FORSENDELSE_MOTTATT = Instant.parse("2020-12-01T12:00:00.00Z")
        private const val JOURNALPOST_ID = "journalpostId"
    }
}
