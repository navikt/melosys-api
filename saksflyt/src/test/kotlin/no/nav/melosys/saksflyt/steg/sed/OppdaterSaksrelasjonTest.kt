package no.nav.melosys.saksflyt.steg.sed

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.fagsak
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppdaterSaksrelasjonTest {
    private lateinit var eessiService: EessiService
    private lateinit var joarkFasade: JoarkFasade
    private lateinit var fagsakService: FagsakService

    private lateinit var oppdaterSaksrelasjon: OppdaterSaksrelasjon

    @BeforeEach
    fun setup() {
        clearAllMocks()
        eessiService = mockk(relaxed = true)
        joarkFasade = mockk()
        fagsakService = mockk()
        oppdaterSaksrelasjon = OppdaterSaksrelasjon(joarkFasade, eessiService, fagsakService)
    }

    @Test
    fun `utfør journalpost er fra eessi verifiser oppdater saksrelasjon`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                fagsak {
                    medGsakSaksnummer()
                }
            }
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID)
        }


        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            mottaksKanal = "EESSI"
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost

        val melosysEessiMelding = MelosysEessiMelding().apply {
            bucType = "LA_BUC_04"
            rinaSaksnummer = "321323"
        }
        every { eessiService.hentSedTilknyttetJournalpost(JOURNALPOST_ID) } returns melosysEessiMelding


        oppdaterSaksrelasjon.utfør(prosessinstans)


        verify {
            eessiService.lagreSaksrelasjon(
                prosessinstans.hentBehandling.fagsak.gsakSaksnummer,
                melosysEessiMelding.rinaSaksnummer,
                melosysEessiMelding.bucType
            )
        }
    }

    @Test
    fun `utfør journalpost ikke fra eessi verifiser oppdaterer ikke saksrelasjon`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID)
        }

        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            mottaksKanal = "flaskepost"
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost


        oppdaterSaksrelasjon.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.lagreSaksrelasjon(any(), any(), any()) }
    }

    @Test
    fun `utfør eessi melding finnes i data verifiser oppdaterer saksrelasjon`() {
        val eessiMelding = MelosysEessiMelding().apply {
            rinaSaksnummer = "12312"
            bucType = "LA_BUC_06"
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                fagsak {
                    medGsakSaksnummer()
                }
            }
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, eessiMelding)
        }


        oppdaterSaksrelasjon.utfør(prosessinstans)


        verify {
            eessiService.lagreSaksrelasjon(
                prosessinstans.hentBehandling.fagsak.gsakSaksnummer,
                eessiMelding.rinaSaksnummer,
                eessiMelding.bucType
            )
        }
    }

    @Test
    fun `utfør ingen behandling ingen arkivsak ID i prosessinstans henter arkivsak ID fra saksnummer i fagsak service oppdaterer saksrelasjon`() {
        val eessiMelding = MelosysEessiMelding().apply {
            rinaSaksnummer = "12312"
            bucType = "LA_BUC_06"
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                fagsak {
                    medGsakSaksnummer()
                }
            }
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.EESSI_MELDING, eessiMelding)
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
        }

        every { fagsakService.hentFagsak(prosessinstans.hentBehandling.fagsak.saksnummer) } returns prosessinstans.hentBehandling.fagsak


        oppdaterSaksrelasjon.utfør(prosessinstans)


        verify {
            eessiService.lagreSaksrelasjon(
                prosessinstans.hentBehandling.fagsak.gsakSaksnummer,
                eessiMelding.rinaSaksnummer,
                eessiMelding.bucType
            )
        }
    }

    companion object {
        private const val JOURNALPOST_ID = "123"
    }
}
