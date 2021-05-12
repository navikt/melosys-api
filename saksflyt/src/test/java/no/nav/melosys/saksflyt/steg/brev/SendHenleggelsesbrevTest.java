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
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendHenleggelsesbrevTest {

    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private SendHenleggelsesbrev sendHenleggelsesbrev;

    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final long behandlingID = 12314;

    @BeforeEach
    public void setUp() {
        sendHenleggelsesbrev = new SendHenleggelsesbrev(brevBestiller, behandlingsresultatService);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_sendHenleggelsesbrev_produserDokument() {
        String saksbehandler = "Z097";
        Fagsak fagsak = new Fagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.HENLEGG_SAK);

        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);

        behandlingsresultat.setBegrunnelseFritekst("fritekst");
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);
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
