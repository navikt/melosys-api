package no.nav.melosys.service.mottatteopplysninger

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.*
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.MottatteOpplysningerRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class MottatteOpplysningerServiceTest {
    @Mock
    private val mottatteOpplysningerRepository: MottatteOpplysningerRepository? = null

    @Mock
    private val behandlingService: BehandlingService? = null

    @Mock
    private val joarkFasade: JoarkFasade? = null
    private var mottatteOpplysningerService: MottatteOpplysningerService? = null

    @Captor
    private val mottatteOpplysningerArgumentCaptor: ArgumentCaptor<MottatteOpplysninger>? = null
    private val behandlingID: Long = 123332211
    private val unleash = FakeUnleash()
    @BeforeEach
    fun setup() {
        val utledMottaksdato = UtledMottaksdato(joarkFasade!!)
        mottatteOpplysningerService = MottatteOpplysningerService(
            mottatteOpplysningerRepository!!,
            behandlingService!!,
            utledMottaksdato,
            unleash
        )
        unleash.enableAll()
    }

    @Test
    fun hentMottatteOpplysningerForBehandlingID_finnes_returnerMottatteOpplysninger() {
        whenever(mottatteOpplysningerRepository!!.findByBehandling_Id(behandlingID))
            .thenReturn(Optional.of(MottatteOpplysninger()))
        Assertions.assertThat(mottatteOpplysningerService!!.hentMottatteOpplysninger(behandlingID)).isNotNull
    }

    @Test
    fun hentMottatteOpplysningerForBehandlingID_finnesIkke_kastException() {
        Assertions.assertThatExceptionOfType(IkkeFunnetException::class.java)
            .isThrownBy { mottatteOpplysningerService!!.hentMottatteOpplysninger(1) }
            .withMessageContaining("Finner ikke mottatteOpplysninger for behandling 1")
    }

    @Test
    fun opprettSøknadEllerAnmodningEllerAttest_toggleErAv_lagerIkkeAnmodningEllerAttest() {
        unleash.disableAll()
        val behandling =
            lagBehandling(Sakstyper.EU_EOS, Sakstemaer.UNNTAK, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        mottatteOpplysningerService!!.opprettSøknadEllerAnmodningEllerAttest(prosessinstans)
        Mockito.verify(mottatteOpplysningerRepository, Mockito.never())!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
    }

    @Test
    fun opprettSøknadEllerAnmodningEllerAttest_erAnmodningOmUnntakEllerRegistreringUnntak_lagerAnmodningEllerAttest() {
        val behandling =
            lagBehandling(Sakstyper.EU_EOS, Sakstemaer.UNNTAK, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        mottatteOpplysningerService!!.opprettSøknadEllerAnmodningEllerAttest(prosessinstans)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST)
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            AnmodningEllerAttest::class.java
        )
    }

    @Test
    fun opprettSøknadEllerAnmodningEllerAttest_erIkkeAnmodningOmUnntakEllerRegistreringUnntak_lagerIkkeAnmodningEllerAttest() {
        val behandling =
            lagBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        mottatteOpplysningerService!!.opprettSøknadEllerAnmodningEllerAttest(prosessinstans)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.type).isNotEqualTo(Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST)
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isNotInstanceOf(
            AnmodningEllerAttest::class.java
        )
    }

    @Test
    fun opprettEøsSøknadGrunnlag_finnesIkkeFraFør_blirOpprettet() {
        val behandling =
            lagBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        whenever(joarkFasade!!.hentJournalpost(behandling.initierendeJournalpostId))
            .thenReturn(lagJournalpost(behandling))
        val periode = Periode()
        val soeknadsland = Soeknadsland()
        mottatteOpplysningerService!!.opprettSøknad(behandling, periode, soeknadsland)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            Soeknad::class.java
        )
        Assertions.assertThat(opprettet.mottatteOpplysningerData.periode).isEqualTo(periode)
        Assertions.assertThat(opprettet.mottatteOpplysningerData.soeknadsland).isEqualTo(soeknadsland)
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS)
        Assertions.assertThat(opprettet.behandling).isEqualTo(behandling)
        Assertions.assertThat(opprettet.mottaksdato).isNotNull
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun oppdaterMottatteOpplysningerJson_mottatteopplysningerEksisterer_oppdatererMottatteOpplysningerData() {
        val objectMapper = ObjectMapper()
        val mottatteOpplysninger = MottatteOpplysninger()
        val originalData = MottatteOpplysningerData()
        val originalJsonData = objectMapper.writeValueAsString(originalData)
        mottatteOpplysninger.jsonData = originalJsonData
        mottatteOpplysninger.setMottatteOpplysningerdata(MottatteOpplysningerData())
        whenever(mottatteOpplysningerRepository!!.findByBehandling_Id(behandlingID))
            .thenReturn(Optional.of(mottatteOpplysninger))
        val nyData: MottatteOpplysningerData = Soeknad()
        val jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(nyData))
        mottatteOpplysningerService!!.oppdaterMottatteOpplysninger(behandlingID, jsonNode)
        Mockito.verify(mottatteOpplysningerRepository).saveAndFlush(
            ArgumentMatchers.any(
                MottatteOpplysninger::class.java
            )
        )
        Assertions.assertThat(mottatteOpplysninger.jsonData).isNotEqualTo(originalJsonData)
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun oppdaterMottatteOpplysninger_mottatteopplysningerJsonDataIkkeSatt_setterJsonDataOgLagrerMottatteOpplysninger() {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.periode = Periode(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2010, 1, 1)
        )
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData)
        mottatteOpplysningerService!!.oppdaterMottatteOpplysninger(mottatteOpplysninger)
        Mockito.verify(mottatteOpplysningerRepository)!!.saveAndFlush(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val jsonNode = ObjectMapper().readTree(
            mottatteOpplysningerArgumentCaptor.value.jsonData
        )
        val periode = jsonNode["periode"].toString()
        Assertions.assertThat(periode)
            .isEqualTo(
                "{" +
                        "\"fom\":[2000,1,1]," +
                        "\"tom\":[2010,1,1]" +
                        "}"
            )
    }

    @Test
    fun oppdaterMottatteOpplysningerPeriodeOgLand_eksisterer_oppdatererPeriodeOgLand() {
        val captor = ArgumentCaptor.forClass(
            MottatteOpplysninger::class.java
        )
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.setMottatteOpplysningerdata(MottatteOpplysningerData())
        whenever(mottatteOpplysningerRepository!!.findByBehandling_Id(behandlingID))
            .thenReturn(Optional.of(mottatteOpplysninger))
        val periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val soeknadsland = Soeknadsland(listOf("UK"), false)
        mottatteOpplysningerService!!.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingID, periode, soeknadsland)
        Mockito.verify(mottatteOpplysningerRepository).saveAndFlush(captor.capture())
        Assertions.assertThat(captor.value.mottatteOpplysningerData.periode).isEqualTo(periode)
        Assertions.assertThat(captor.value.mottatteOpplysningerData.soeknadsland).isEqualTo(soeknadsland)
    }

    @Test
    fun opprettSedGrunnlag_harRettType() {
        val behandling = lagBehandling(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        whenever(joarkFasade!!.hentJournalpost(behandling.initierendeJournalpostId))
            .thenReturn(lagJournalpost(behandling))
        val sedGrunnlag = SedGrunnlag()
        mottatteOpplysningerService!!.opprettSedGrunnlag(behandlingID, sedGrunnlag)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            SedGrunnlag::class.java
        )
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.SED)
        Assertions.assertThat(opprettet.behandling).isEqualTo(behandling)
        Assertions.assertThat(opprettet.mottaksdato).isNotNull
    }

    @Test
    fun opprettSøknadFolketrygden_harPeriodeOgLand_setterPeriodeOgLandOgHarRettType() {
        val behandling = lagBehandling(Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.ARBEID_I_UTLANDET)
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        whenever(joarkFasade!!.hentJournalpost(behandling.initierendeJournalpostId))
            .thenReturn(lagJournalpost(behandling))
        val periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val soeknadsland = Soeknadsland(listOf("UK"), false)
        mottatteOpplysningerService!!.opprettSøknad(behandling, periode, soeknadsland)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            SoeknadFtrl::class.java
        )
        Assertions.assertThat(opprettet.mottatteOpplysningerData.periode).isEqualTo(periode)
        Assertions.assertThat(opprettet.mottatteOpplysningerData.soeknadsland).isEqualTo(soeknadsland)
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.SØKNAD_FOLKETRYGDEN)
        Assertions.assertThat(opprettet.behandling).isEqualTo(behandling)
        Assertions.assertThat(opprettet.mottaksdato).isNotNull
    }

    @Test
    fun opprettSøknadTrygdeavtale_harPeriodeOgLand_setterPeriodeOgLandOgHarRettType() {
        val behandling =
            lagBehandling(Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV)
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        whenever(joarkFasade!!.hentJournalpost(behandling.initierendeJournalpostId))
            .thenReturn(lagJournalpost(behandling))
        val periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val soeknadsland = Soeknadsland(listOf("UK"), false)
        mottatteOpplysningerService!!.opprettSøknad(behandling, periode, soeknadsland)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            SoeknadTrygdeavtale::class.java
        )
        Assertions.assertThat(opprettet.mottatteOpplysningerData.periode).isEqualTo(periode)
        Assertions.assertThat(opprettet.mottatteOpplysningerData.soeknadsland).isEqualTo(soeknadsland)
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.SØKNAD_TRYGDEAVTALE)
        Assertions.assertThat(opprettet.behandling).isEqualTo(behandling)
        Assertions.assertThat(opprettet.mottaksdato).isNotNull
    }

    @Test
    fun opprettSøknad_tomFlyt_mottatteOpplysningerBlirIkkeOpprettet() {
        val behandling = lagBehandling(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        mottatteOpplysningerService!!.opprettSøknad(behandling, null, null)
        Mockito.verifyNoInteractions(behandlingService)
        Mockito.verifyNoInteractions(mottatteOpplysningerRepository)
    }

    @Test
    fun opprettSøknad_mottatteOpplysningerBlirOpprettet() {
        val behandling = lagBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV)
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        whenever(joarkFasade!!.hentJournalpost(behandling.initierendeJournalpostId))
            .thenReturn(lagJournalpost(behandling))
        mottatteOpplysningerService!!.opprettSøknad(behandling, null, null)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            Soeknad::class.java
        )
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS)
        Assertions.assertThat(opprettet.behandling).isEqualTo(behandling)
        Assertions.assertThat(opprettet.mottaksdato).isNotNull
    }

    @Test
    fun opprettSøknad_FTRL_brukbBehandlingsårsak() {
        val behandling = lagBehandling(Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV)
        val behandlingsaarsak = Behandlingsaarsak()
        behandlingsaarsak.mottaksdato = LocalDate.now()
        behandling.behandlingsårsak = behandlingsaarsak
        whenever(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling)
        mottatteOpplysningerService!!.opprettSøknad(behandling, null, null)
        Mockito.verify(mottatteOpplysningerRepository)!!.save(
            mottatteOpplysningerArgumentCaptor!!.capture()
        )
        val opprettet = mottatteOpplysningerArgumentCaptor.value
        Assertions.assertThat(opprettet).isNotNull
        Assertions.assertThat(opprettet.mottatteOpplysningerData).isInstanceOf(
            Soeknad::class.java
        )
        Assertions.assertThat(opprettet.type).isEqualTo(Mottatteopplysningertyper.SØKNAD_FOLKETRYGDEN)
        Assertions.assertThat(opprettet.behandling).isEqualTo(behandling)
        Assertions.assertThat(opprettet.mottaksdato).isNotNull
    }

    private fun lagJournalpost(behandling: Behandling): Journalpost {
        val journalpost = Journalpost(behandling.initierendeJournalpostId)
        journalpost.forsendelseMottatt = LocalDateTime.now().toInstant(ZoneOffset.UTC)
        return journalpost
    }

    private fun lagBehandling(sakstype: Sakstyper, sakstemaer: Sakstemaer, tema: Behandlingstema): Behandling {
        val behandling = Behandling()
        behandling.fagsak = lagFagsak(sakstype, sakstemaer)
        behandling.id = behandlingID
        behandling.initierendeJournalpostId = "123321"
        behandling.tema = tema
        behandling.type = Behandlingstyper.FØRSTEGANG
        return behandling
    }

    private fun lagFagsak(sakstype: Sakstyper, sakstemaer: Sakstemaer): Fagsak {
        val fagsak = Fagsak()
        fagsak.type = sakstype
        fagsak.tema = sakstemaer
        return fagsak
    }
}
