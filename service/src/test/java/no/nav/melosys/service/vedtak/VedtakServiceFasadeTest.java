package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode.ENDRINGER_ARBEIDSSITUASJON;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VedtakServiceFasadeTest {

    private static final long behandlingID = 1L;

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private EosVedtakService mockEosVedtakService;

    @Mock
    private EosVedtakSystemService mockEosVedtakSystemService;

    @Mock
    private FtrlVedtakService mockFtrlVedtakService;

    @Mock
    private TrygdeavtaleVedtakService trygdeavtaleVedtakService;

    private VedtakServiceFasade vedtakServiceFasade;

    private Behandling behandling;

    @BeforeEach
    void init() {
        vedtakServiceFasade = new VedtakServiceFasade(mockBehandlingService, mockEosVedtakService, mockEosVedtakSystemService, mockFtrlVedtakService, trygdeavtaleVedtakService);
        behandling = lagBehandling();

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_feilBehandlingstype_kasterException() {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtakServiceFasade.fattVedtak(behandlingID, lagFattFtrlVedtakRequest(), SubjectHandler.getInstance().getUserID()))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Kan ikke fatte vedtak ved behandlingstema UFM: Melding om utstasjonering – A009");
    }

    @Test
    void fattVedtak_EU_EOS_skalKalleEosVedtakService() throws Exception {
        setFagsakPåBehandling(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.fattVedtak(behandlingID, lagFattEosVedtakRequest(), SubjectHandler.getInstance().getUserID());

        verify(mockEosVedtakService).fattVedtak(eq(behandling), any(FattEosVedtakRequest.class));
        verifyNoInteractions(mockEosVedtakSystemService);
        verifyNoInteractions(mockFtrlVedtakService);
    }

    @Test
    void fattVedtak_delvisAutomatisert_skalKalleEosVedtakSystemService() throws Exception {
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND);

        verify(mockEosVedtakSystemService).fattVedtak(behandling, FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK);
        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockFtrlVedtakService);
    }

    @Test
    void fattVedtak_FTRL_skalKalleFtrlVedtakService() throws Exception {
        setFagsakPåBehandling(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.fattVedtak(behandlingID, lagFattFtrlVedtakRequest(), SubjectHandler.getInstance().getUserID());

        verify(mockFtrlVedtakService).fattVedtak(eq(behandling), any(FattFtrlVedtakRequest.class), eq("Z990007"));
        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void fattVedtak_TRYGDEAVTALER_kasterException() throws Exception {
        setFagsakPåBehandling(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.fattVedtak(behandlingID, lagFattTrygdeavtaleVedtakRequest(), SubjectHandler.getInstance().getUserID());

        verify(trygdeavtaleVedtakService).fattVedtak(eq(behandling), any(FattTrygdeavtaleVedtakRequest.class));
        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void endreVedtak_EU_EOS_skalKalleEosVedtakService() throws Exception {
        setFagsakPåBehandling(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null);

        verify(mockEosVedtakService).endreVedtaksperiode(eq(behandling), eq(ENDRINGER_ARBEIDSSITUASJON), isNull(), isNull());
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void endreVedtak_FTRL_kasterException() throws Exception {
        setFagsakPåBehandling(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtakServiceFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Vedtaksendring for sakstype FTRL er ikke støttet.");

        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void endreVedtak_TRYGDEAVTALER_kasterException() throws Exception {
        setFagsakPåBehandling(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtakServiceFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Vedtaksendring for sakstype TRYGDEAVTALE er ikke støttet.");

        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setMedlPeriodeID(123L);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        return behandling;
    }

    private void setFagsakPåBehandling(Sakstyper sakstype) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstype);
        behandling.setFagsak(fagsak);
    }

    private FattEosVedtakRequest lagFattEosVedtakRequest() {
        return new FattEosVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekst("Fritekst")
            .build();
    }

    private FattFtrlVedtakRequest lagFattFtrlVedtakRequest() {
        return new FattFtrlVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekstBegrunnelse("Begrunnelse")
            .build();
    }

    private FattTrygdeavtaleVedtakRequest lagFattTrygdeavtaleVedtakRequest() {
        return new FattTrygdeavtaleVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekstBegrunnelse("Begrunnelse")
            .build();
    }
}
