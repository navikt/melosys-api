package no.nav.melosys.saksflyt.steg.hs;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendHenleggelsesbrevTest {
    private SendHenleggelsesbrev sendHenleggelsesbrev;

    @Mock
    BrevBestiller brevBestiller;

    @Before
    public void setUp() {
        sendHenleggelsesbrev = new SendHenleggelsesbrev(brevBestiller);
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
        prosessinstans.setData(BEGRUNNELSEKODE, Henleggelsesgrunner.ANNET);
        prosessinstans.setData(BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, "fritekst");
        Behandling behandling = new Behandling();
        behandling.setId(behandlingId);
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        sendHenleggelsesbrev.utfør(prosessinstans);

        ArgumentCaptor<Brevbestilling> brevbestillingCaptor = ArgumentCaptor.forClass(Brevbestilling.class);
        verify(brevBestiller).bestill(brevbestillingCaptor.capture());
        Brevbestilling brevbestilling = brevbestillingCaptor.getValue();

        assertThat(brevbestilling.getDokumentType()).isEqualTo(Produserbaredokumenter.MELDING_HENLAGT_SAK);
        assertThat(brevbestilling.getMottakere().iterator().next().getRolle()).isEqualTo(Aktoersroller.BRUKER);

        assertThat(prosessinstans.getSteg()).isEqualTo(IV_STATUS_BEH_AVSL);
    }
}