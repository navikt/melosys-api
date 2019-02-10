package no.nav.melosys.saksflyt.agent.brev;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class SendMangelbrevTest {

    private BehandlingRepository behandlingRepo;

    private SendMangelbrev agent;

    private DokumentSystemService dokumentService;

    @Before
    public void setUp() {
        behandlingRepo = mock(BehandlingRepository.class);
        dokumentService = mock(DokumentSystemService.class);
        agent = new SendMangelbrev(behandlingRepo, dokumentService);
    }

    @Test
    public void utførSteg() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);

        BrevData brevData = new BrevData("Z123456");
        p.setData(ProsessDataKey.BREVDATA, brevData);

        agent.utførSteg(p);

        verify(dokumentService).produserDokument(anyLong(), any(ProduserbartDokument.class), any(BrevData.class));
        verify(behandlingRepo).save(any(Behandling.class));

        assertThat(p.getSteg()).isNull();
    }

    @Test
    public void testSetGetData() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());

        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = RolleType.MYNDIGHET;
        brevData.fritekst = "Fritekst";

        p.setData(ProsessDataKey.BREVDATA, brevData);

        BrevData hentetBrevData = p.getData(ProsessDataKey.BREVDATA, BrevData.class);
        assertThat(hentetBrevData.mottaker).isEqualTo(brevData.mottaker);
        assertThat(hentetBrevData.fritekst).isEqualTo(brevData.fritekst);
        assertThat(hentetBrevData.saksbehandler).isEqualTo(brevData.saksbehandler);

    }
}