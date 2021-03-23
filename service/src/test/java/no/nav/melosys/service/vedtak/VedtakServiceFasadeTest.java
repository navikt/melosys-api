package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode.ENDRINGER_ARBEIDSSITUASJON;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VedtakServiceFasadeTest {

    private static long behandlingID = 1L;

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private EosVedtakService mockEosVedtakService;

    @Mock
    private EosVedtakSystemService mockEosVedtakSystemService;

    private VedtakServiceFasade vedtakServiceFasade;

    private Behandling behandling;

    @BeforeEach
    void init() {
        vedtakServiceFasade = new VedtakServiceFasade(mockBehandlingService, mockEosVedtakService, mockEosVedtakSystemService);
        behandling = lagBehandling();
    }

    @Test
    void fattVedtak_feilBehandlingstype_kasterException() throws Exception {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtakServiceFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND,
            null, null, null, FØRSTEGANGSVEDTAK, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Kan ikke fatte vedtak ved behandlingstema UFM: Melding om utstasjonering – A009");
    }

    @Test
    void fattVedtak_EU_EOS_skalKalleEosVedtakService() throws Exception {
        setFagsakPåBehandling(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND,
            null, null, null, FØRSTEGANGSVEDTAK, null);

        verify(mockEosVedtakService).fattVedtak(eq(behandlingID), eq(FASTSATT_LOVVALGSLAND), isNull(), isNull(), isNull(), eq(FØRSTEGANGSVEDTAK), isNull());
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void fattVedtak_delvisAutomatisert_skalKalleEosVedtakSystemService() throws Exception {
        vedtakServiceFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND);

        verify(mockEosVedtakSystemService).fattVedtak(eq(behandlingID), eq(FASTSATT_LOVVALGSLAND), isNull(), isNull(), isNull(), eq(FØRSTEGANGSVEDTAK), isNull());
        verifyNoInteractions(mockEosVedtakService);
    }

    @Test
    void fattVedtak_FTRL_kasterException() throws Exception {
        setFagsakPåBehandling(Sakstyper.FTRL);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtakServiceFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND,
            null, null, null, FØRSTEGANGSVEDTAK, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Vedtaksfatting for sakstype FTRL er ikke støttet.");

        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void fattVedtak_TRYGDEAVTALER_kasterException() throws Exception {
        setFagsakPåBehandling(Sakstyper.TRYGDEAVTALE);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        assertThatThrownBy(() -> vedtakServiceFasade.fattVedtak(behandlingID, FASTSATT_LOVVALGSLAND,
            null, null, null, FØRSTEGANGSVEDTAK, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Vedtaksfatting for sakstype TRYGDEAVTALE er ikke støttet.");

        verifyNoInteractions(mockEosVedtakService);
        verifyNoInteractions(mockEosVedtakSystemService);
    }

    @Test
    void endreVedtak_EU_EOS_skalKalleEosVedtakService() throws Exception {
        setFagsakPåBehandling(Sakstyper.EU_EOS);
        when(mockBehandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        vedtakServiceFasade.endreVedtak(behandlingID, ENDRINGER_ARBEIDSSITUASJON, null, null);

        verify(mockEosVedtakService).endreVedtak(eq(behandlingID), eq(ENDRINGER_ARBEIDSSITUASJON), isNull(), isNull());
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
}