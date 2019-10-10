package no.nav.melosys.saksflyt.steg.jfr.sed;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {

    @Mock
    private FagsakService fagsakService;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    @Before
    public void setUp() throws Exception {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(fagsakService);
        when(fagsakService.nyFagsakOgBehandling(any())).thenReturn(hentFagsak());
    }

    @Test
    public void utførSteg_prosessTypeAnmodningsUnntak_verifiserNyFagsakOgBehandlingBlirOpprettet() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(fagsakService).nyFagsakOgBehandling(any(OpprettSakRequest.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_OPPRETT_SAK);
    }

    @Test(expected = TekniskException.class)
    public void utførSteg_ikkeProsessTypeMottakSed_kasterException() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        opprettFagsakOgBehandling.utfør(prosessinstans);
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, "321");
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer("123rina");

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        return prosessinstans;
    }

    private Fagsak hentFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);

        fagsak.setBehandlinger(Lists.newArrayList(behandling));
        return fagsak;
    }
}