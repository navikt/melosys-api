package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VurderInngangsvilkaarTest {
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private VurderInngangsvilkaar vurderInngangsvilkaar;

    private final long behandlingID = 143;
    private final String saksnummer = "MEL-432";
    private final Behandling behandling = new Behandling();

    @Before
    public void setUp() throws IkkeFunnetException {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, fagsakService, behandlingService);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
    }

    @Test
    public void utfoerSteg_funker() throws FunksjonellException, TekniskException {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1L));
        behandlingsgrunnlagData.soeknadsland.landkoder = List.of(Landkoder.NO.getKode(), Landkoder.SE.getKode());

        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            eq(behandlingID),
            eq(behandlingsgrunnlagData.soeknadsland.landkoder),
            eq(behandlingsgrunnlagData.periode)
        )).thenReturn(true);

        vurderInngangsvilkaar.utfør(prosessinstans);

        verify(fagsakService).oppdaterType(eq(prosessinstans.getBehandling().getFagsak()), eq(true));
    }

    @Test
    public void utfør_behandlingstemaBeslutningLovvalgAnnetLand_vurdererIkkeInngangsvilkår() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), any());
    }
}
