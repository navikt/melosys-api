package no.nav.melosys.service.unntaksperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnntaksperiodeServiceTest {

    private final Periode PERIODE_OK = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
    private final Periode PERIODE_BAD = new Periode(LocalDate.now(), LocalDate.now().minusYears(2));
    private final Behandling behandling = new Behandling();
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private UnntaksperiodeKontrollService unntaksperiodeKontrollService;
    private UnntaksperiodeService unntaksperiodeService;

    @BeforeEach
    public void setUp() {
        unntaksperiodeService = new UnntaksperiodeService(behandlingService, behandlingsresultatService, lovvalgsperiodeService, oppgaveService, prosessinstansService, unntaksperiodeKontrollService);
        behandling.setId(1L);
        behandling.setFagsak(FagsakTestFactory.lagFagsak());
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
    }

    @Test
    void godkjennPeriode_behandlingAvsluttet_forventException() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        UnntaksperiodeGodkjenning unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder().build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L, unntaksperiodeGodkjenning))
            .withMessageContaining("er inaktiv");
    }

    @Test
    void godkjennPeriode_feilBehandlingstype_forventException() {
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        UnntaksperiodeGodkjenning unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder().build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L, unntaksperiodeGodkjenning))
            .withMessageContaining("ikke av tema");
    }

    @Test
    void godkjennPeriode_sedDokumentHarOppNedPeriode_forventException() {
        Saksopplysning sedSaksopplysning = new Saksopplysning();
        sedSaksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(PERIODE_BAD);
        sedSaksopplysning.setDokument(sedDokument);
        behandling.getSaksopplysninger().add(sedSaksopplysning);
        UnntaksperiodeGodkjenning unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder().build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L, unntaksperiodeGodkjenning))
            .withMessageContaining("har feil i perioden");
    }

    @Test
    void godkjennPeriode_endretPeriodeUtenFeil_verifiserKall() {
        Saksopplysning sedSaksopplysning = new Saksopplysning();
        sedSaksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(PERIODE_BAD);
        sedSaksopplysning.setDokument(sedDokument);
        behandling.getSaksopplysninger().add(sedSaksopplysning);
        Unntaksperiode unntaksperiode = new Unntaksperiode(LocalDate.of(2000, 1, 1), LocalDate.of(2001, 1, 1));
        UnntaksperiodeGodkjenning endretUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .varsleUtland(false)
            .fritekst(null)
            .endretPeriode(unntaksperiode)
            .lovvalgsbestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
            .build();

        unntaksperiodeService.godkjennPeriode(1L, endretUnntaksperiodeGodkjenning);

        Lovvalgsperiode forventetLovvalgsperiode = new Lovvalgsperiode();
        forventetLovvalgsperiode.setFom(LocalDate.of(2000, 1, 1));
        forventetLovvalgsperiode.setTom(LocalDate.of(2001, 1, 1));
        forventetLovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        forventetLovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        forventetLovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
        forventetLovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);
        Collection<Lovvalgsperiode> forventedeLovvalgsperioder = Collections.singleton(forventetLovvalgsperiode);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(1L, forventedeLovvalgsperioder);
        verify(prosessinstansService).opprettProsessinstansGodkjennUnntaksperiode(any(), eq(false), eq(null), eq(null));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
        verify(unntaksperiodeKontrollService).kontrollPeriode(sedDokument, new Periode(unntaksperiode.fom(),
            unntaksperiode.tom()));
    }

    @Test
    void godkjennPeriode_endretPeriodeErOppNed_forventException() {
        Unntaksperiode unntaksperiode = new Unntaksperiode(LocalDate.of(2001, 1, 1), LocalDate.of(2000, 1, 1));
        UnntaksperiodeGodkjenning endretUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .endretPeriode(unntaksperiode)
            .build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L, endretUnntaksperiodeGodkjenning));
    }

    @Test
    void godkjennPeriode_tomEndretPeriode_forventException() {
        Unntaksperiode unntaksperiode = new Unntaksperiode(null, null);
        UnntaksperiodeGodkjenning endretUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .endretPeriode(unntaksperiode)
            .build();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L, endretUnntaksperiodeGodkjenning));
    }

    @Test
    void ikkeGodkjennPeriode_oppNedPeriode_forventIngenException() {
        Saksopplysning sedSaksopplysning = new Saksopplysning();
        sedSaksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(PERIODE_BAD);
        sedSaksopplysning.setDokument(sedDokument);
        behandling.getSaksopplysninger().add(sedSaksopplysning);
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());

        assertThatCode(() -> unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null))
            .doesNotThrowAnyException();
    }

    @Test
    void ikkeGodkjennPeriode_medBegrunnelser_ingenFeil() throws Exception {
        leggTilNødvendigeSaksopplysninger();
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null);
        verify(prosessinstansService).opprettProsessinstansUnntaksperiodeAvvist(any(), any());
    }

    @Test
    void ikkeGodkjennPeriode_ingenBegrunnelser_forventException() {
        leggTilNødvendigeSaksopplysninger();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.ikkeGodkjennPeriode(1L, Set.of(), null))
            .withMessageContaining("Ingen begrunnelser");
    }

    @Test
    void ikkeGodkjennPeriode_begrunnelseAnnetIngenFritekst_forventException() {
        leggTilNødvendigeSaksopplysninger();
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        begrunnelser.add(Ikke_godkjent_begrunnelser.ANNET.getKode());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null))
            .withMessageContaining("krever fritekst");
    }

    private void leggTilNødvendigeSaksopplysninger() {
        Saksopplysning sedSaksopplysning = new Saksopplysning();
        sedSaksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(PERIODE_OK);
        sedSaksopplysning.setDokument(sedDokument);
        behandling.getSaksopplysninger().add(sedSaksopplysning);
    }

}
