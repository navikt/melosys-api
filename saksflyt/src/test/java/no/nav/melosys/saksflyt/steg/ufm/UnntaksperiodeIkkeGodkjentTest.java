package no.nav.melosys.saksflyt.steg.ufm;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeIkkeGodkjentTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private UnntaksperiodeIkkeGodkjent unntaksperiodeIkkeGodkjent;

    private Behandling behandling = new Behandling();
    private Fagsak fagsak = new Fagsak();
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Before
    public void setup() throws IkkeFunnetException {
        unntaksperiodeIkkeGodkjent = new UnntaksperiodeIkkeGodkjent(fagsakService, behandlingsresultatService, medlPeriodeService);

        fagsak.setSaksnummer("MEL-111111");
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandlingsresultat.getLovvalgsperioder().add(new Lovvalgsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utfør_medBegrunnelser_blirLagret() throws Exception {

        List<Ikke_godkjent_begrunnelser> ikkeGodkjentBegrunnelser = new ArrayList<>();
        ikkeGodkjentBegrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
        ikkeGodkjentBegrunnelser.add(Ikke_godkjent_begrunnelser.ANNET);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, ikkeGodkjentBegrunnelser);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, "fritekst");

        unntaksperiodeIkkeGodkjent.utfør(prosessinstans);

        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.AVSLUTTET));
        verify(behandlingsresultatService).oppdaterBegrunnelser(eq(behandling.getId()), anySet(), eq("fritekst"));

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }

    @Test(expected = TekniskException.class)
    public void utfør_utenBegrunnelser_kasterException() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        unntaksperiodeIkkeGodkjent.utfør(prosessinstans);
    }
}