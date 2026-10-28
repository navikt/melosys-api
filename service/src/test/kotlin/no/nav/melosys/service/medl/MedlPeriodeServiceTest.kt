package no.nav.melosys.service.medl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl
import no.nav.melosys.integrasjon.medl.MedlService
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl
import no.nav.melosys.repository.AnmodningsperiodeRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.repository.UtpekingsperiodeRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
class MedlPeriodeServiceTest {
    @MockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var medlService: MedlService

    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var lovvalgsperiodeRepository: LovvalgsperiodeRepository

    @MockK
    lateinit var anmodningsperiodeRepository: AnmodningsperiodeRepository

    @RelaxedMockK
    lateinit var medlAnmodningsperiodeService: MedlAnmodningsperiodeService

    @MockK
    lateinit var utpekingsperiodeRepository: UtpekingsperiodeRepository

    @MockK
    lateinit var medlemskapsperiodeRepository: MedlemskapsperiodeRepository

    @MockK
    lateinit var fagsakService: FagsakService

    lateinit var medlPeriodeService: MedlPeriodeService


    @BeforeEach
    fun setUp() {
        medlPeriodeService = MedlPeriodeService(
            persondataFasade,
            medlService,
            behandlingsresultatService,
            behandlingService,
            lovvalgsperiodeRepository,
            medlAnmodningsperiodeService,
            utpekingsperiodeRepository,
            medlemskapsperiodeRepository,
            fagsakService
        )
    }

    @Test
    fun hentPeriodeListe() {
        val now = LocalDate.now()
        val plusMonths = LocalDate.now().plusMonths(2)

        medlPeriodeService.hentPeriodeListe(FNR, now, plusMonths)

        verify { medlService.hentPeriodeListe(FNR, now, plusMonths) }
    }

    @Test
    fun opprettPeriodeForeløpig() {
        setupHappyPathBehandling()
        val utpekingsperiode = utpekingsperiodeForTest()
        every { medlService.opprettPeriodeForeløpig(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { utpekingsperiodeRepository.save(any()) } returns utpekingsperiode

        medlPeriodeService.opprettPeriodeForeløpig(utpekingsperiode, 1)

        verify { medlService.opprettPeriodeForeløpig(FNR, any(), KildedokumenttypeMedl.SED) }
        verify { utpekingsperiodeRepository.save(utpekingsperiode) }
    }

    @Test
    fun opprettPeriodeUnderAvklaring() {
        setupHappyPathBehandling(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        every { medlService.opprettPeriodeUnderAvklaring(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { anmodningsperiodeRepository.save(any()) } returns anmodningsperiodeForTest()

        medlPeriodeService.opprettPeriodeUnderAvklaring(anmodningsperiodeForTest(), 1L)
        verify { medlService.opprettPeriodeUnderAvklaring(FNR, any(), KildedokumenttypeMedl.HENV_SOKNAD) }
        verify { medlAnmodningsperiodeService.lagreAnmodningsperiode(any()) }
    }

    @Test
    fun opprettPeriodeEndelig() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeEndelig(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { lovvalgsperiodeRepository.save(any()) } returns lovvalgsperiodeForTest()

        medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiodeForTest(), 1L)

        verify { medlService.opprettPeriodeEndelig(FNR, any(), KildedokumenttypeMedl.SED) }
        verify { lovvalgsperiodeRepository.save(any()) }
    }

    @Test
    fun opprettPeriodeEndeligFtrl_feiler() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeEndelig(FNR, any(), any()) } returns null

        shouldThrow<TekniskException> {
            medlPeriodeService.opprettPeriodeEndelig(1L, medlemskapsperiodeForTest())
        }.message.shouldContain("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID")

        verify(exactly = 0) { medlemskapsperiodeRepository.save(any()) }
    }

    @Test
    fun opprettPeriodeEndeligFtrl() {
        setupHappyPathBehandling()
        val medlemskapsperiode = medlemskapsperiodeForTest()
        every { medlemskapsperiodeRepository.save(any()) } returns medlemskapsperiode

        medlPeriodeService.opprettPeriodeEndelig(1L, medlemskapsperiode)

        verify { medlService.opprettPeriodeEndelig(FNR, any(), any()) }
        verify { medlemskapsperiodeRepository.save(medlemskapsperiode) }
    }

    @Test
    fun opprettOpphørtPeriode() {
        setupHappyPathBehandling()
        val medlemskapsperiode = medlemskapsperiodeForTest()
        every { medlemskapsperiodeRepository.save(any()) } returns medlemskapsperiode

        medlPeriodeService.opprettOpphørtPeriode(1L, medlemskapsperiode)

        verify { medlService.opprettOpphørtPeriode(FNR, any(), any()) }
        verify { medlemskapsperiodeRepository.save(medlemskapsperiode) }
    }


    @Test
    fun oppdaterPeriodeEndelig() {
        setupHappyPathBehandling(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER)

        val lovvalgsperiode = lovvalgsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun oppdaterOpphørtPeriode() {
        setupHappyPathBehandling(Sakstyper.FTRL, Behandlingstema.YRKESAKTIV)
        val medlemskapsperiode = medlemskapsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
        }


        medlPeriodeService.oppdaterOpphørtPeriode(1, medlemskapsperiode)


        verify { medlService.oppdaterOpphørtPeriode(medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun oppdaterPeriodeForeløpig() {
        setupHappyPathBehandling(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        val lovvalgsperiode = lovvalgsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun `opprettPeriodeEndelig bruker KildedokumenttypeMedl A1 ved registrering av unntak etter konvensjonen med Storbritannia`() {
        setupTrygdeavtaleBehandling(Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK)
        val lovvalgsperiode = lovvalgsperiodeForTest {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1
        }

        medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, 1L)

        verify { medlService.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.A1) }
    }

    @Test
    fun `opprettPeriodeEndelig bruker KildedokumenttypeMedl A1 ved registrering av unntak med tilleggsbestemmelse i konvensjonen`() {
        setupTrygdeavtaleBehandling(Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK)
        val lovvalgsperiode = lovvalgsperiodeForTest {
            bestemmelse = Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5
        }

        medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, 1L)

        verify { medlService.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.A1) }
    }

    @Test
    fun `opprettPeriodeEndelig bruker KildedokumenttypeMedl DOKUMENT ved registrering av unntak etter trygdeavtalen med Storbritannia`() {
        setupTrygdeavtaleBehandling(Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK)
        val lovvalgsperiode = lovvalgsperiodeForTest {
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        }

        medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, 1L)

        verify { medlService.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.DOKUMENT) }
    }

    @Test
    fun `opprettPeriodeEndelig bruker KildedokumenttypeMedl HENV_SOKNAD ved anmodning om unntak etter konvensjonen med Storbritannia`() {
        setupTrygdeavtaleBehandling(Sakstemaer.UNNTAK, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
        val lovvalgsperiode = lovvalgsperiodeForTest {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }

        medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, 1L)

        verify { medlService.opprettPeriodeEndelig(FNR, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun `oppdaterPeriodeEndelig bruker KildedokumenttypeMedl A1 ved registrering av unntak etter konvensjonen med Storbritannia`() {
        setupTrygdeavtaleBehandling(Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK)
        val lovvalgsperiode = lovvalgsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1
        }

        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode)

        verify { medlService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.A1) }
    }

    @Test
    fun `oppdaterPeriodeForeløpig bruker KildedokumenttypeMedl DOKUMENT når TRYGDEAVTALE og REGISTRERING_UNNTAK`() {
        every { behandlingService.hentBehandling(any()) } returns Behandling.forTest {
            tema = Behandlingstema.REGISTRERING_UNNTAK
            fagsak {
                type = Sakstyper.TRYGDEAVTALE
                medBruker()
            }
        }
        val lovvalgsperiode = lovvalgsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.DOKUMENT) }
    }

    @Test
    fun `oppdaterPeriodeForeløpig bruker KildedokumenttypeMedl HENV_SOKNAD når TRYGDEAVTALE og ANMODNING_OM_UNNTAK`() {
        every { behandlingService.hentBehandling(any()) } returns Behandling.forTest {
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            fagsak {
                type = Sakstyper.TRYGDEAVTALE
                medBruker()
            }
        }

        val lovvalgsperiode = lovvalgsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun `oppdaterPeriodeForeløpig bruker KildedokumenttypeMedl A1 når EU_EOS og A1_ANMODNING_OM_UNNTAK_PAPIR`() {
        every { behandlingService.hentBehandling(any()) } returns Behandling.forTest {
            tema = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
            fagsak { medBruker() }
        }
        val lovvalgsperiode = lovvalgsperiodeForTest {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.A1) }
    }

    @Test
    fun avvisPeriode() {
        medlPeriodeService.avvisPeriode(MEDL_PERIODE_ID)

        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun avvisPeriodeFeilregistrert() {
        medlPeriodeService.avvisPeriodeFeilregistrert(MEDL_PERIODE_ID)

        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.FEILREGISTRERT) }
    }

    @Test
    fun avvisPeriodeOpphørt() {
        medlPeriodeService.avvisPeriodeOpphørt(MEDL_PERIODE_ID)

        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.OPPHORT) }
    }

    @Test
    fun avsluttTidligereMedlPeriode_behandlingOgPeriodeFinnes_avviserPeriode() {
        val fagsak = Fagsak.forTest {
            behandling {
                status = Behandlingsstatus.AVSLUTTET
                id = 1L
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode { medlPeriodeID = MEDL_PERIODE_ID }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { fagsakService.hentFagsak("MEL-1") } returns fagsak

        medlPeriodeService.avsluttTidligerMedlPeriode("MEL-1")

        verify { behandlingsresultatService.hentBehandlingsresultat(1L) }
        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun avsluttTidligereMedlPeriode_ingenEksisterendePeriode_ingenPeriodeBlirAvvist() {
        val fagsak = Fagsak.forTest {
            behandling {
                status = Behandlingsstatus.AVSLUTTET
                id = 1L
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest()
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { fagsakService.hentFagsak("MEL-1") } returns fagsak

        medlPeriodeService.avsluttTidligerMedlPeriode("MEL-1")

        verify { behandlingsresultatService.hentBehandlingsresultat(1L) }
        verify(exactly = 0) { medlService.avvisPeriode(any(), any()) }
    }

    private fun setupTrygdeavtaleBehandling(sakstema: Sakstemaer, behandlingstema: Behandlingstema) {
        val behandling = Behandling.forTest {
            id = 1L
            tema = behandlingstema
            fagsak {
                type = Sakstyper.TRYGDEAVTALE
                tema = sakstema
                medBruker()
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns Behandlingsresultat.forTest {
            id = 1L
            this.behandling = behandling
        }
        every { persondataFasade.hentFolkeregisterident(any()) } returns FNR
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { medlService.opprettPeriodeEndelig(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { lovvalgsperiodeRepository.save(any()) } answers { firstArg() }
    }

    private fun setupHappyPathBehandling(
        sakstype: Sakstyper = Sakstyper.EU_EOS,
        behandlingstema: Behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
    ) {
        val behandlingResultat = lagBehandlingsResultat(sakstype, behandlingstema)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingResultat
        every { persondataFasade.hentFolkeregisterident(any()) } returns FNR
        every { behandlingService.hentBehandling(any()) } returns behandlingResultat.behandling
    }

    private fun lagBehandlingsResultat(
        sakstype: Sakstyper = Sakstyper.EU_EOS,
        behandlingstema: Behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        id = 1L
        behandling {
            tema = behandlingstema
            fagsak {
                type = sakstype
                medBruker()
            }
        }
    }

    companion object {
        private const val FNR = "12345678901"
        private const val MEDL_PERIODE_ID = 99L
    }
}
