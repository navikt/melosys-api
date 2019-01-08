package no.nav.melosys.saksflyt.agent.aou;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningOmUnntakSendBrevTest {
    private class MockBrevDataBygger implements BrevDataBygger {
        @Override
        public BrevData lag(Behandling behandling, String saksbehandler) {
            return new BrevData();
        }
    }

    @Mock
    private DokumentSystemService dokService;

    @Mock
    private BrevDataByggerVelger byggerVelger;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private Prosessinstans p;
    private AnmodningOmUnntakSendBrev agent;

    @Before
    public void setUp() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setId(1L);

        when(behandlingRepository.findOneWithSaksopplysningerById(any())).thenReturn(behandling);
        when(byggerVelger.hent(any())).thenReturn(new MockBrevDataBygger());

        p = new Prosessinstans();
        p.setBehandling(behandling);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.ANMODNING_OM_UNNTAK.getKode());

        agent = new AnmodningOmUnntakSendBrev(dokService, byggerVelger, behandlingRepository, behandlingsresultatRepository);
    }

    @Test
    public void utfoerSteg() {
        agent.utførSteg(p);
        assertThat(p.getSteg()).isNull();
    }

    @Test
    public void utførStegAntallSendteBrev() throws FunksjonellException, TekniskException {
        agent.utførSteg(p);
        verify(dokService, times(1)).produserDokument(anyLong(), eq(ProduserbartDokument.ORIENTERING_ANMODNING_UNNTAK), any());
        verify(dokService, times(1)).produserDokument(anyLong(), eq(ProduserbartDokument.SED_A001), any());
    }
}