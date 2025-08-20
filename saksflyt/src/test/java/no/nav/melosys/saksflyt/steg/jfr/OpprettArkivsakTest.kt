package no.nav.melosys.saksflyt.steg.jfr

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.sak.ArkivsakService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OpprettArkivsakTest {

    private val arkivsakService: ArkivsakService = mockk()

    private val fagsakService: FagsakService = mockk()

    private lateinit var opprettArkivsak: OpprettArkivsak

    private val oppgaveFactory = no.nav.melosys.service.oppgave.OppgaveFactory()

    @BeforeEach
    fun setUp() {
        opprettArkivsak = OpprettArkivsak(fagsakService, arkivsakService, oppgaveFactory)
    }

    @Test
    fun utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet() {
        val forventetArkivsakID = 1234432L
        val behandling = Behandling.forTest {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                medBruker()
            }
        }
        val fagsak = behandling.fagsak
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }
        every {
            arkivsakService.opprettSakForBruker(
                fagsak.saksnummer,
                oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type),
                FagsakTestFactory.BRUKER_AKTØR_ID
            )
        } returns forventetArkivsakID
        every { fagsakService.lagre(any()) } returns Unit


        opprettArkivsak.utfør(prosessinstans)


        fagsak.gsakSaksnummer shouldBe forventetArkivsakID
        verify { fagsakService.lagre(fagsak) }
    }

    @Test
    fun utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet_brukFagsakTema() {
        val forventetArkivsakID = 1234432L
        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                medBruker()
            }
        }
        val fagsak = behandling.fagsak
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }
        every {
            arkivsakService.opprettSakForBruker(
                fagsak.saksnummer, oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type),
                FagsakTestFactory.BRUKER_AKTØR_ID
            )
        } returns forventetArkivsakID
        every { fagsakService.lagre(any()) } returns Unit


        opprettArkivsak.utfør(prosessinstans)


        fagsak.gsakSaksnummer shouldBe forventetArkivsakID
        verify { fagsakService.lagre(fagsak) }
    }

    @Test
    fun utfør_virksomhetErHovedpart_oppretterSakForVirksomhet() {
        val forventetArkivsakID = 1234432L
        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                medVirksomhet()
            }
        }
        val fagsak = behandling.fagsak
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }
        every {
            arkivsakService.opprettSakForVirksomhet(
                fagsak.saksnummer,
                oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type),
                FagsakTestFactory.ORGNR
            )
        } returns forventetArkivsakID
        every { fagsakService.lagre(any()) } returns Unit


        opprettArkivsak.utfør(prosessinstans)


        fagsak.gsakSaksnummer shouldBe forventetArkivsakID
        verify { fagsakService.lagre(fagsak) }
    }

    @Test
    fun utfør_arkivsakIDEksisterer_kasterException() {
        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                gsakSaksnummer = 1234432L
            }
        }
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }


        val exception = shouldThrow<FunksjonellException> {
            opprettArkivsak.utfør(prosessinstans)
        }


        exception.message shouldContain "allerede knyttet til"
    }

    @Test
    fun utfør_harVerkenBrukerIDEllerVirksomhetOrgnr_kasterException() {
        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                // No bruker or virksomhet - default fagsak
            }
        }
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }


        val exception = shouldThrow<FunksjonellException> {
            opprettArkivsak.utfør(prosessinstans)
        }


        exception.message shouldContain "Finner verken bruker eller virksomhet tilknyttet fagsak MEL-test"
    }
}
