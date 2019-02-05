package no.nav.melosys.saksflyt.agent.hs;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataHenleggelse;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProduserbartDokument.MELDING_HENLAGT_SAK;
import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendHenleggelsesbrevTest {
    private SendHenleggelsesbrev sendHenleggelsesbrev;

    @Mock
    DokumentSystemService dokumentService;

    @Mock
    BrevDataByggerVelger brevDataByggerVelger;

    @Mock
    BrevDataBygger brevDataBygger;

    @Mock
    BrevDataHenleggelse brevDataHenleggelse;

    @Before
    public void setUp() {
        sendHenleggelsesbrev = new SendHenleggelsesbrev(dokumentService, brevDataByggerVelger);
        doReturn(brevDataBygger).when(brevDataByggerVelger).hent(MELDING_HENLAGT_SAK);
    }

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    public void utførSteg_sendHenleggelsesbrev_produserDokument() throws FunksjonellException, TekniskException {
        long behandlingId = 234234L;
        String saksbehandler = "Z097";
        Fagsak fagsak = new Fagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.HENLEGG_SAK);
        Behandling behandling = new Behandling();
        behandling.setId(behandlingId);
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);
        doReturn(brevDataHenleggelse).when(brevDataBygger).lag(behandling, saksbehandler);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        sendHenleggelsesbrev.utfør(prosessinstans);

        assertThat(brevDataHenleggelse.mottaker).isEqualTo(RolleType.BRUKER);
        verify(dokumentService).produserDokument(behandlingId, MELDING_HENLAGT_SAK, brevDataHenleggelse);
        assertThat(prosessinstans.getSteg()).isEqualTo(IV_STATUS_BEH_AVSL);
    }
}