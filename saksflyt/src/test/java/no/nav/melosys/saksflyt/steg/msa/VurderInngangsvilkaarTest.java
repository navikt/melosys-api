package no.nav.melosys.saksflyt.steg.msa;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VurderInngangsvilkaarTest {

    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private FagsakService fagsakService;

    private VurderInngangsvilkaar vurderInngangsvilkaar;

    @Before
    public void setup() {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, fagsakService);
    }

    @Test
    public void utfør() throws MelosysException {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.setFagsak(fagsak);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        BehandlingsgrunnlagData søknad = new SoeknadDokument();
        søknad.soeknadsland.landkoder = List.of(Landkoder.BE.getKode(), Landkoder.AT.getKode());
        søknad.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1));
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(søknad);

        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(anyLong(), any(), any())).thenReturn(Boolean.TRUE);

        vurderInngangsvilkaar.utfør(prosessinstans);

        verify(fagsakService).oppdaterType(eq(fagsak), eq(true));

    }
}