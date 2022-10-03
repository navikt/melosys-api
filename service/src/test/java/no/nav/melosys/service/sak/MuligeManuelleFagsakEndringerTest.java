package no.nav.melosys.service.sak;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER;
import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static no.nav.melosys.service.SaksbehandlingDataFactory.lagFagsak;

@ExtendWith(MockitoExtension.class)
class MuligeManuelleFagsakEndringerTest {
    @Mock
    private LovligeKombinasjonerService lovligeKombinasjonerService;

    private MuligeManuelleFagsakEndringer muligeManuelleFagsakEndringer;

    @Test
    @Disabled
    void hentMuligeSakstyper_med_behandlingstema_med_behandlingstema_lovlig() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagBehandling();
        behandling.setTema(UTSENDT_ARBEIDSTAKER);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setTema(MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        Set<Sakstemaer> muligeSakstemaer = muligeManuelleFagsakEndringer.hentMuligeSakstema(behandling, fagsak.getType());
    }

    @Test
    @Disabled
    void hentMuligeSakstema_med_behandlingstema_med_behandlingstema_lovlig() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagBehandling();
        behandling.setTema(UTSENDT_ARBEIDSTAKER);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setTema(MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        Set<Sakstemaer> muligeSakstemaer = muligeManuelleFagsakEndringer.hentMuligeSakstema(behandling, fagsak.getType());
    }

}
