package no.nav.melosys.service.sak

import io.getunleash.FakeUnleash
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
internal class OpprettBehandlingForSakTest {
    @RelaxedMockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private val unleash = FakeUnleash()

    private lateinit var opprettBehandlingForSak: OpprettBehandlingForSak

    @BeforeEach
    fun setUp() {
        val lovligeKombinasjonerSaksbehandlingService = LovligeKombinasjonerSaksbehandlingService(
            fagsakService, behandlingService, behandlingsresultatService, unleash
        )
        val saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository, unleash)
        opprettBehandlingForSak = OpprettBehandlingForSak(
            fagsakService,
            prosessinstansService,
            saksbehandlingRegler,
            lovligeKombinasjonerSaksbehandlingService,
            behandlingService
        )

        every { behandlingService.avsluttBehandling(any()) } just Runs
    }

    @ParameterizedTest
    @EnumSource(value = Behandlingstyper::class, names = ["HENVENDELSE", "NY_VURDERING"])
    fun opprettBehandling_medAktivÅrsavregningVedBehandling_AvslutterIkkeAktivÅrsavregningBehandling(behandlingsType: Behandlingstyper) {
        unleash.enableAll()
        val behandlingId = 1L

        val førstegangsBehandling =
            lagBehandling(tema = Behandlingstema.YRKESAKTIV, type = Behandlingstyper.FØRSTEGANG, status = Behandlingsstatus.AVSLUTTET)
        val årsavregningBehandling =
            lagBehandling(tema = Behandlingstema.YRKESAKTIV, type = Behandlingstyper.ÅRSAVREGNING, status = Behandlingsstatus.UNDER_BEHANDLING)

        val fagsak = lagFagsak(førstegangsBehandling, årsavregningBehandling)
        fagsak.type = Sakstyper.FTRL
        fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG

        val eksisterendeResultat = Behandlingsresultat()
        eksisterendeResultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND

        every { behandlingsresultatRepository.findById(behandlingId) }.returns(Optional.of(eksisterendeResultat))


        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandlingId) }.returns(Behandlingsresultat())


        opprettBehandlingForSak.opprettBehandling(
            FagsakTestFactory.SAKSNUMMER,
            lagOpprettSakDto(Behandlingstema.YRKESAKTIV, behandlingsType)
        )


        verify(exactly = 0) { behandlingService.avsluttBehandling(behandlingId) }
    }

    @Test
    fun opprettBehandling_medAktivBehandlingVednyÅrsavregningBehandlingAvslutterIkkeAktivBehanding() {
        unleash.enableAll()
        val behandlingId = 1L

        val aktivBehandling = lagBehandling(status = Behandlingsstatus.UNDER_BEHANDLING)

        val fagsak = lagFagsak(aktivBehandling)
        fagsak.type = Sakstyper.FTRL
        fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandlingId) }.returns(Behandlingsresultat())


        opprettBehandlingForSak.opprettBehandling(
            FagsakTestFactory.SAKSNUMMER,
            lagOpprettSakDto(Behandlingstema.YRKESAKTIV, Behandlingstyper.ÅRSAVREGNING)
        )


        verify(exactly = 0) { behandlingService.avsluttBehandling(behandlingId) }
    }

    @Test
    fun opprettBehandling_medAktivBehandlingMenArtikkel16SendtAnmodningUtland_feilerIkke() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.NY_VURDERING)
        val aktivBehandling = lagBehandling()
        aktivBehandling.status = Behandlingsstatus.UNDER_BEHANDLING
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(aktivBehandling))

        val anmodningsperiode = Anmodningsperiode()
        anmodningsperiode.setSendtUtland(true)
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.anmodningsperioder.add(anmodningsperiode)
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(aktivBehandling.id) }.returns(behandlingsresultat)


        Assertions.assertThatNoException()
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
    }


    @Test
    fun opprettBehandling_utenBehandlingstema_feiler() {
        val opprettSakDto = lagOpprettSakDto(null, Behandlingstyper.NY_VURDERING)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
            .withMessageContaining("Behandlingstema mangler")
    }

    @Test
    fun opprettBehandling_utenBehandlingstype_feiler() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.UTSENDT_ARBEIDSTAKER, null)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
            .withMessageContaining("Behandlingstype mangler")
    }

    @Test
    fun opprettBehandling_utenMottaksdato_feiler() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.NY_VURDERING)
        opprettSakDto.mottaksdato = null
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
            .withMessageContaining("Mottaksdato")
    }

    @Test
    fun opprettBehandling_ugyldigBehandlingstype_feiler() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.FØRSTEGANG)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
            .withMessageContaining("Behandlingstype FØRSTEGANG er ikke lovlig for behandlingstema UTSENDT_ARBEIDSTAKER og saksnummer MEL-test")
    }

    @Test
    fun opprettBehandling_ugyldigBehandlingstema_feiler() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.REGISTRERING_UNNTAK, Behandlingstyper.NY_VURDERING)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
            .withMessageContaining("Behandlingstype NY_VURDERING er ikke lovlig for behandlingstema REGISTRERING_UNNTAK og saksnummer MEL-test")
    }

    @Test
    fun opprettBehandling_fritekstMenFeilType_feiler() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.NY_VURDERING)
        opprettSakDto.behandlingsaarsakFritekst = "Fritekst"
        opprettSakDto.behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto) }
            .withMessageContaining("Kan ikke lagre fritekst som årsak når årsakstype")
    }

    @Test
    fun opprettBehandling_opprettetBehandlingFårIngenFlyt_oppretterProsessSomIkkeReplikerer() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.PENSJONIST, Behandlingstyper.HENVENDELSE)

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(lagFagsak(lagBehandling()))


        opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto)


        verify { prosessinstansService.opprettNyBehandlingForSak(FagsakTestFactory.SAKSNUMMER, opprettSakDto.tilOpprettSakRequest()) }
    }

    @Test
    fun opprettBehandling_eksisterendeBehandlingKanReplikeres_oppretterProsessSomReplikerer() {
        val opprettSakDto = lagOpprettSakDto(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.NY_VURDERING)

        val eksisterendeBehandling = lagBehandling()
        eksisterendeBehandling.status = Behandlingsstatus.AVSLUTTET

        val fagsak = lagFagsak(eksisterendeBehandling)
        eksisterendeBehandling.fagsak = fagsak

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(fagsak)

        val eksisterendeResultat = Behandlingsresultat()
        eksisterendeResultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND

        every { behandlingsresultatRepository.findById(eksisterendeBehandling.id) }.returns(Optional.of(eksisterendeResultat))


        opprettBehandlingForSak.opprettBehandling(FagsakTestFactory.SAKSNUMMER, opprettSakDto)


        verify { prosessinstansService.opprettOgReplikerBehandlingForSak(FagsakTestFactory.SAKSNUMMER, opprettSakDto.tilOpprettSakRequest()) }
    }

    private fun lagOpprettSakDto(
        behandlingstema: Behandlingstema?,
        behandlingstyper: Behandlingstyper?
    ): OpprettSakDto {
        val opprettsakdto = OpprettSakDto()
        opprettsakdto.behandlingstema = behandlingstema
        opprettsakdto.behandlingstype = behandlingstyper
        opprettsakdto.mottaksdato = LocalDate.now()
        opprettsakdto.behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD

        return opprettsakdto
    }

    private fun lagBehandling(
        tema: Behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER,
        type: Behandlingstyper = Behandlingstyper.FØRSTEGANG,
        status: Behandlingsstatus = Behandlingsstatus.AVSLUTTET
    ): Behandling {
        val behandling = Behandling()

        behandling.id = 1L
        behandling.tema = tema
        behandling.type = type
        behandling.status = status

        return behandling
    }

    private fun lagFagsak(vararg behandlinger: Behandling): Fagsak {
        val fagsak = builder()
            .medBruker()
            .behandlinger(listOf(*behandlinger))
            .build()

        behandlinger.forEach { it.fagsak = fagsak }

        return fagsak
    }
}
