package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BEGRUNNELSEKODE;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendHenleggelsesbrevTest {

    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private SendHenleggelsesbrev sendHenleggelsesbrev;

    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final long behandlingID = 12314;

    @Before
    public void setUp() throws IkkeFunnetException {
        sendHenleggelsesbrev = new SendHenleggelsesbrev(brevBestiller, behandlingsresultatService);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
    }

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    public void utfør_sendHenleggelsesbrev_produserDokument() throws FunksjonellException, TekniskException {
        String saksbehandler = "Z097";
        Fagsak fagsak = new Fagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.HENLEGG_SAK);

        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);

        behandlingsresultat.setBegrunnelseFritekst("fritekst");
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);
        prosessinstans.setData(BEGRUNNELSEKODE, Henleggelsesgrunner.ANNET);
        prosessinstans.setData(BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, "fritekst");
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        sendHenleggelsesbrev.utfør(prosessinstans);

        ArgumentCaptor<DoksysBrevbestilling> brevbestillingCaptor = ArgumentCaptor.forClass(DoksysBrevbestilling.class);
        verify(brevBestiller).bestill(brevbestillingCaptor.capture());
        DoksysBrevbestilling brevbestilling = brevbestillingCaptor.getValue();

        assertThat(brevbestilling.getProduserbartdokument()).isEqualTo(Produserbaredokumenter.MELDING_HENLAGT_SAK);
        assertThat(brevbestilling.getMottakere().iterator().next().getRolle()).isEqualTo(Aktoersroller.BRUKER);
        assertThat(brevbestilling.getFritekst()).isEqualTo(behandlingsresultat.getBegrunnelseFritekst());
    }
}