package no.nav.melosys.service.medl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
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
        val utpekingsperiode = Utpekingsperiode()
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
        every { anmodningsperiodeRepository.save(any()) } returns Anmodningsperiode()

        medlPeriodeService.opprettPeriodeUnderAvklaring(Anmodningsperiode(), 1L)
        verify { medlService.opprettPeriodeUnderAvklaring(FNR, any(), KildedokumenttypeMedl.HENV_SOKNAD) }
        verify { medlAnmodningsperiodeService.lagreAnmodningsperiode(any()) }
    }

    @Test
    fun opprettPeriodeEndelig() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeEndelig(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { lovvalgsperiodeRepository.save(any()) } returns Lovvalgsperiode()

        medlPeriodeService.opprettPeriodeEndelig(Lovvalgsperiode(), 1L)

        verify { medlService.opprettPeriodeEndelig(FNR, any(), KildedokumenttypeMedl.SED) }
        verify { lovvalgsperiodeRepository.save(any()) }
    }

    @Test
    fun opprettPeriodeEndeligFtrl_feiler() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeEndelig(FNR, any(), any()) } returns null

        shouldThrow<FunksjonellException> {
            medlPeriodeService.opprettPeriodeEndelig(1L, Medlemskapsperiode())
        }.message.shouldContain("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID")

        verify(exactly = 0) { medlemskapsperiodeRepository.save(any()) }
    }

    @Test
    fun opprettPeriodeEndeligFtrl() {
        setupHappyPathBehandling()
        val medlemskapsperiode = Medlemskapsperiode()
        every { medlemskapsperiodeRepository.save(any()) } returns medlemskapsperiode

        medlPeriodeService.opprettPeriodeEndelig(1L, medlemskapsperiode)

        verify { medlService.opprettPeriodeEndelig(FNR, any(), any()) }
        verify { medlemskapsperiodeRepository.save(medlemskapsperiode) }
    }

    @Test
    fun oppdaterPeriodeEndelig() {
        setupHappyPathBehandling(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER)

        val lovvalgsperiode = Lovvalgsperiode().apply {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun oppdaterPeriodeForeløpig() {
        setupHappyPathBehandling(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        val lovvalgsperiode = Lovvalgsperiode().apply {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun `oppdaterPeriodeForeløpig bruker KildedokumenttypeMedl DOKUMENT når TRYGDEAVTALE og REGISTRERING_UNNTAK`() {
        every { behandlingService.hentBehandling(any()) } returns Behandling().apply {
            tema = Behandlingstema.REGISTRERING_UNNTAK
            fagsak = Fagsak().apply {
                type = Sakstyper.TRYGDEAVTALE
                aktører.add(Aktoer().apply {
                    rolle = Aktoersroller.BRUKER
                    aktørId = "456"
                })
            }
        }
        val lovvalgsperiode = Lovvalgsperiode().apply {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.DOKUMENT) }
    }

    @Test
    fun `oppdaterPeriodeForeløpig bruker KildedokumenttypeMedl HENV_SOKNAD når TRYGDEAVTALE og ANMODNING_OM_UNNTAK`() {
        every { behandlingService.hentBehandling(any()) } returns Behandling().apply {
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            fagsak = Fagsak().apply {
                type = Sakstyper.TRYGDEAVTALE
                aktører.add(Aktoer().apply {
                    rolle = Aktoersroller.BRUKER
                    aktørId = "456"
                })
            }
        }
        val lovvalgsperiode = Lovvalgsperiode().apply {
            medlPeriodeID = MEDL_PERIODE_ID
            behandlingsresultat = lagBehandlingsResultat()
        }


        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)


        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun `oppdaterPeriodeForeløpig bruker KildedokumenttypeMedl A1 når EU_EOS og A1_ANMODNING_OM_UNNTAK_PAPIR`() {
        every { behandlingService.hentBehandling(any()) } returns Behandling().apply {
            tema = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
            fagsak = Fagsak().apply {
                type = Sakstyper.EU_EOS
                aktører.add(Aktoer().apply {
                    rolle = Aktoersroller.BRUKER
                    aktørId = "456"
                })
            }
        }
        val lovvalgsperiode = Lovvalgsperiode().apply {
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
        val fagsak = Fagsak().apply {
            behandlinger = listOf(Behandling().apply {
                status = Behandlingsstatus.AVSLUTTET
                id = 1L
            })
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            lovvalgsperioder =
                setOf(Lovvalgsperiode().apply { medlPeriodeID = MEDL_PERIODE_ID })
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { fagsakService.hentFagsak("MEL-1") } returns fagsak

        medlPeriodeService.avsluttTidligerMedlPeriode("MEL-1")

        verify { behandlingsresultatService.hentBehandlingsresultat(1L) }
        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun avsluttTidligereMedlPeriode_ingenEksisterendePeriode_ingenPeriodeBlirAvvist() {
        val fagsak = Fagsak().apply {
            behandlinger = listOf(Behandling().apply {
                status = Behandlingsstatus.AVSLUTTET
                id = 1L
            })
        }
        val behandlingsresultat = Behandlingsresultat().apply { lovvalgsperioder = setOf() }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { fagsakService.hentFagsak("MEL-1") } returns fagsak

        medlPeriodeService.avsluttTidligerMedlPeriode("MEL-1")

        verify { behandlingsresultatService.hentBehandlingsresultat(1L) }
        verify(exactly = 0) { medlService.avvisPeriode(any(), any()) }
    }

    @Test
    fun erstattMedlemskapsperioder_skalKunOppretteNyePerioder_nårGammelListeErTom() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        every { medlService.opprettPeriodeEndelig(any(), any(), any()) } returns MEDL_PERIODE_ID
        val medlemskapsperiode1 = Medlemskapsperiode().apply { fom = LocalDate.now().minusDays(1) }
        val medlemskapsperiode2 = Medlemskapsperiode().apply { fom = LocalDate.now().plusDays(1) }
        val nyListe = listOf(medlemskapsperiode1, medlemskapsperiode2);

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat()

        medlPeriodeService.erstattMedlemskapsperioder(nyListe, 1L, 2L)

        verify(exactly = 1) { medlService.opprettPeriodeEndelig("12345678901", medlemskapsperiode1, KildedokumenttypeMedl.HENV_SOKNAD) }
        verify(exactly = 1) { medlService.opprettPeriodeEndelig("12345678901", medlemskapsperiode2, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun erstattMedlemskapsperioder_skalKunAvviseGamlePerioder_nårNyListeErTom() {
        setupHappyPathBehandling()
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply { medlPeriodeID = 1L }
        val gammelListe = listOf(gammelMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

        medlPeriodeService.erstattMedlemskapsperioder(emptyList(), 1L, 2L)

        verify(exactly = 1) { medlService.avvisPeriode(1L, StatusaarsakMedl.OPPHORT) }
        verify(exactly = 0) { medlService.opprettPeriodeEndelig(any(), any(), any()) }
    }

    @Test
    fun erstattMedlemskapsperioder_skalOppretteNyeOgAvviseGamle_nårBeggeListerErIkkeTomme_menIngenFellesElementer() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply { medlPeriodeID = 1L }
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = listOf(gammelMedlemskapsperiode) }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }
        val nyMedlemskapsperiode = Medlemskapsperiode()

        medlPeriodeService.erstattMedlemskapsperioder(listOf(nyMedlemskapsperiode), 1L, 2L)

        verify(exactly = 1) { medlService.avvisPeriode(gammelMedlemskapsperiode.medlPeriodeID, StatusaarsakMedl.OPPHORT) }
        verify(exactly = 1) { medlService.opprettPeriodeEndelig("12345678901", nyMedlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun erstattMedlemskapsperioder_skalKunBehandleForskjellene_nårBeggeListerHarFellesElementer() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val fellesMedlemskapsperiode = Medlemskapsperiode().apply { medlPeriodeID = 1L }
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply  {medlPeriodeID = 2L }
        val gammelListe = listOf(fellesMedlemskapsperiode, gammelMedlemskapsperiode)
        val nyMedlemskapsperiode = Medlemskapsperiode()
        val nyListe = listOf(fellesMedlemskapsperiode, nyMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

        medlPeriodeService.erstattMedlemskapsperioder(nyListe, 1L, 2L)

        verify(exactly = 1) { medlService.avvisPeriode(gammelMedlemskapsperiode.medlPeriodeID, StatusaarsakMedl.OPPHORT) }
        verify(exactly = 1) { medlService.opprettPeriodeEndelig("12345678901", nyMedlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
        verify(exactly = 0) { medlService.opprettPeriodeEndelig("12345678901", fellesMedlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
        verify(exactly = 0) { medlService.avvisPeriode(fellesMedlemskapsperiode.medlPeriodeID, StatusaarsakMedl.OPPHORT) }
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
    ):
        Behandlingsresultat = Behandlingsresultat().apply {
        id = 1L
        behandling = Behandling().apply {
            tema = behandlingstema
            fagsak = Fagsak().apply {
                aktører.add(Aktoer().apply {
                    rolle = Aktoersroller.BRUKER
                    aktørId = "456"
                    type = sakstype
                })
            }
        }
    }

    companion object {
        private const val FNR = "12345678901"
        private const val MEDL_PERIODE_ID = 99L
    }
}
