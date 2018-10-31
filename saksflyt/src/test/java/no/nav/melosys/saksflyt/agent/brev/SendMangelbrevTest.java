package no.nav.melosys.saksflyt.agent.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Dokumenttype;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendMangelbrevTest {

    private BehandlingRepository behandlingRepo;

    private SendMangelbrev agent;

    private DokumentSystemService dokumentService;

    @Before
    public void setUp() throws Exception {
        behandlingRepo = mock(BehandlingRepository.class);
        dokumentService = mock(DokumentSystemService.class);
        agent = new SendMangelbrev(behandlingRepo, dokumentService);
    }

    @Test
    public void utførSteg() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());

        BrevDataDto brevDataDto = new BrevDataDto();
        brevDataDto.saksbehandler = "TEST";
        p.setData(ProsessDataKey.BREVDATA, brevDataDto);

        agent.utførSteg(p);

        verify(dokumentService, times(1)).produserDokument(anyLong(), any(Dokumenttype.class), any(BrevDataDto.class));
        verify(behandlingRepo, times(1)).save(any(Behandling.class));

        assertThat(p.getSteg()).isNull();
    }
}