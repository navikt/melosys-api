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
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Before
    public void setUp() {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, fagsakService, behandlingService);
    }

    @Test
    public void utfoerSteg_funker() throws FunksjonellException, TekniskException {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1L));
        behandlingsgrunnlagData.soeknadsland.landkoder = List.of(Landkoder.NO.getKode(), Landkoder.SE.getKode());

        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            eq(behandlingID),
            eq(behandlingsgrunnlagData.soeknadsland.landkoder),
            eq(behandlingsgrunnlagData.periode)
        )).thenReturn(true);

        vurderInngangsvilkaar.utfør(prosessinstans);

        verify(fagsakService).oppdaterType(eq(prosessinstans.getBehandling().getFagsak()), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.VURDER_GJENBRUK_OPPGAVE);
    }
}
