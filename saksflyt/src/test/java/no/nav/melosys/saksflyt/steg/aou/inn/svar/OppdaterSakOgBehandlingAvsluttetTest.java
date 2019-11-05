package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSakOgBehandlingAvsluttetTest {

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private TpsService tpsService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private FagsakService fagsakService;

    private OppdaterSakOgBehandlingAvsluttet oppdaterSakOgBehandlingAvsluttet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingAvsluttet = new OppdaterSakOgBehandlingAvsluttet(sakOgBehandlingFasade, tpsService, behandlingService, fagsakService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "22");

        Saksopplysning saksopplysning = new Saksopplysning();
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99";
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        oppdaterSakOgBehandlingAvsluttet.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(behandling.getFagsak()), eq(Saksstatuser.LOVVALG_AVKLART), eq(behandling));
        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(any());
        verify(tpsService, never()).hentAktørIdForIdent(anyString());
        verify(behandlingService, never()).hentBehandling(anyLong());
    }
}