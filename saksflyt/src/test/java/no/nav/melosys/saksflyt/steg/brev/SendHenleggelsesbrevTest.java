package no.nav.melosys.saksflyt.steg.brev;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflytapi.domain.*;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.BEGRUNNELSE_FRITEKST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);

        behandlingsresultat.setBegrunnelseFritekst("fritekst");
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);

        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(behandlingID)
            .medFagsak(fagsak)
            .build();

        Prosessinstans prosessinstans = ProsessinstansTestFactory.builderWithDefaults()
            .medType(ProsessType.HENLEGG_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(BEGRUNNELSE_FRITEKST, "fritekst")
            .medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
            .build();

        sendHenleggelsesbrev.utfør(prosessinstans);

        verify(brevBestiller).bestill(eq(Produserbaredokumenter.MELDING_HENLAGT_SAK),
            eq(Collections.singleton(Mottaker.medRolle(Mottakerroller.BRUKER))),
            eq(behandlingsresultat.getBegrunnelseFritekst()), any(String.class),
            eq(Henleggelsesgrunner.ANNET.getKode()), eq(behandling));
    }
}
