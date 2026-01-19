package no.nav.melosys.service

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.saksopplysning
import no.nav.melosys.domain.sedDokument
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.vilkaar.InngangsvilkaarService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppfriskSaksopplysningerServiceTest {

    @RelaxedMockK
    lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var ufmKontrollService: UfmKontrollService

    @RelaxedMockK
    lateinit var inngangsvilkaarService: InngangsvilkaarService

    @RelaxedMockK
    lateinit var registeropplysningerService: RegisteropplysningerService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @RelaxedMockK
    lateinit var årsavregningService: ÅrsavregningService

    @RelaxedMockK
    lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    private lateinit var oppfriskSaksopplysningerService: OppfriskSaksopplysningerService
    private lateinit var fakeUnleash: FakeUnleash

    @BeforeEach
    fun setUp() {
        fakeUnleash = FakeUnleash()
        val registeropplysningerFactory = RegisteropplysningerFactory(saksbehandlingRegler, fakeUnleash)
        oppfriskSaksopplysningerService = OppfriskSaksopplysningerService(
            anmodningsperiodeService,
            behandlingService,
            behandlingsresultatService,
            ufmKontrollService,
            inngangsvilkaarService,
            registeropplysningerService,
            persondataFasade,
            registeropplysningerFactory,
            årsavregningService,
            helseutgiftDekkesPeriodeService
        )
    }

    @Test
    fun `oppfrisk saksopplysning`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { behandlingsresultatService.tømBehandlingsresultat(any()) }
        verify { registeropplysningerService.slettRegisterOpplysninger(BEHANDLING_ID) }
        verify { registeropplysningerService.hentOgLagreOpplysninger(any<RegisteropplysningerRequest>()) }
    }

    @Test
    fun `oppfrisk saksopplysning virksomhet ingen flyt`() {
        val behandling = lagBehandlingMedVirksomhetOgType(Behandlingstyper.HENVENDELSE)
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns true
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { behandlingsresultatService.tømBehandlingsresultat(any()) }
        verify { registeropplysningerService.slettRegisterOpplysninger(BEHANDLING_ID) }
        verify(exactly = 0) { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) }
    }

    @Test
    fun `oppfrisk saksopplysning anmodning om unntak sendt feiler`() {
        // Note: The test checks that when erUtsending() is true and anmodningsperiode is sent,
        // it throws FunksjonellException. The default tema (UTSENDT_ARBEIDSTAKER) makes erUtsending() true.
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        every { anmodningsperiodeService.harSendtAnmodningsperiode(BEHANDLING_ID) } returns true

        val exception = shouldThrow<FunksjonellException> {
            oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)
        }
        exception.message shouldContain "Anmodning om unntak er sendt"
    }

    @Test
    fun `oppfrisk saksopplysning med SED kaller kontroller`() {
        val behandling = lagBehandlingMedSED()
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID) }
    }

    @Test
    fun `oppfrisk saksopplysning har ikke oppfylt inngangsvilkår oppdaterer type`() {
        val behandling = lagBehandlingMedFagsakType(Sakstyper.EU_EOS)

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns true
        every { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) } returns true

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { inngangsvilkaarService.vurderOgLagreInngangsvilkår(eq(behandling.id), eq(listOf("SE")), eq(false), any<Periode>()) }
    }

    @Test
    fun `oppfrisk saksopplysning skal ikke hente inngangsvilkår henter ikke inngangsvilkår`() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify(exactly = 0) { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) }
    }

    @Test
    fun `oppfrisk saksopplysning utleder periode for årsavregning`() {
        val behandling = lagBehandlingMedType(Behandlingstyper.ÅRSAVREGNING)
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(any()) } returns 2023

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { registeropplysningerService.hentOgLagreOpplysninger(any<RegisteropplysningerRequest>()) }
    }

    private fun lagBehandling() = Behandling.forTest {
        id = BEHANDLING_ID
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
        }
        saksopplysning {
            type = SaksopplysningType.PERSOPL
        }
        mottatteOpplysninger {
            soeknad {
                fysiskeArbeidssted { }
                periode(LocalDate.now(), LocalDate.now().plusYears(2))
                landkoder("SE")
            }
        }
    }

    private fun lagBehandlingMedVirksomhetOgType(behandlingstype: Behandlingstyper) = Behandling.forTest {
        id = BEHANDLING_ID
        type = behandlingstype
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
            medVirksomhet()
        }
        saksopplysning {
            type = SaksopplysningType.PERSOPL
        }
        mottatteOpplysninger {
            soeknad {
                fysiskeArbeidssted { }
                periode(LocalDate.now(), LocalDate.now().plusYears(2))
                landkoder("SE")
            }
        }
    }

    private fun lagBehandlingMedType(behandlingstype: Behandlingstyper) = Behandling.forTest {
        id = BEHANDLING_ID
        type = behandlingstype
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
        }
        saksopplysning {
            type = SaksopplysningType.PERSOPL
        }
        mottatteOpplysninger {
            soeknad {
                fysiskeArbeidssted { }
                periode(LocalDate.now(), LocalDate.now().plusYears(2))
                landkoder("SE")
            }
        }
    }

    private fun lagBehandlingMedSED() = Behandling.forTest {
        id = BEHANDLING_ID
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
        fagsak {
            medBruker()
        }
        saksopplysning {
            type = SaksopplysningType.PERSOPL
        }
        saksopplysning {
            type = SaksopplysningType.SEDOPPL
            sedDokument {
                lovvalgsperiode(LocalDate.MIN, LocalDate.MAX)
            }
        }
        mottatteOpplysninger {
            soeknad {
                fysiskeArbeidssted { }
                periode(LocalDate.now(), LocalDate.now().plusYears(2))
                landkoder("SE")
            }
        }
    }

    private fun lagBehandlingMedFagsakType(sakstype: Sakstyper) = Behandling.forTest {
        id = BEHANDLING_ID
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
            type = sakstype
        }
        saksopplysning {
            type = SaksopplysningType.PERSOPL
        }
        mottatteOpplysninger {
            soeknad {
                fysiskeArbeidssted { }
                periode(LocalDate.now(), LocalDate.now().plusYears(2))
                landkoder("SE")
            }
        }
    }

    companion object {
        private const val BEHANDLING_ID = 11L
    }
}
