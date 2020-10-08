package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SendMangelbrevTest {

    private BehandlingRepository behandlingRepo;

    private SendMangelbrev agent;

    private BrevBestiller brevBestiller;

    @Before
    public void setUp() {
        behandlingRepo = mock(BehandlingRepository.class);
        brevBestiller = mock(BrevBestiller.class);
        agent = new SendMangelbrev(behandlingRepo, brevBestiller);
    }

    @Test
    public void utfør() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);

        Aktoersroller mottaker = Aktoersroller.ARBEIDSGIVER;
        p.setData(ProsessDataKey.MOTTAKER, mottaker);
        BrevData brevData = new BrevData("Z123456");
        p.setData(ProsessDataKey.BREVDATA, brevData);

        agent.utfør(p);

        ArgumentCaptor<Brevbestilling> brevbestillingArgumentCaptor = ArgumentCaptor.forClass(Brevbestilling.class);
        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getDokumentType()).isEqualTo(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER);
        verify(behandlingRepo).save(any(Behandling.class));
    }

    @Test
    public void testSetGetData() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());

        BrevData brevData = new BrevData("Z123456");
        brevData.fritekst = "Fritekst";

        p.setData(ProsessDataKey.BREVDATA, brevData);

        BrevData hentetBrevData = p.getData(ProsessDataKey.BREVDATA, BrevData.class);
        assertThat(hentetBrevData.fritekst).isEqualTo(brevData.fritekst);
        assertThat(hentetBrevData.saksbehandler).isEqualTo(brevData.saksbehandler);

    }
}