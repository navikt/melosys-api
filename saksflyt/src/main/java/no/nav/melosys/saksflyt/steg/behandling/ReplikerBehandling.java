package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.REPLIKER_BEHANDLING;


@Component
public class ReplikerBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ReplikerBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;

    public ReplikerBehandling(FagsakService fagsakService, BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return REPLIKER_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        if (fagsak.hentTidligsteInaktiveBehandling() == null) {
            throw new FunksjonellException("Finner ingen avsluttet behandling på fagsak " + fagsak.getSaksnummer());
        }

        Behandling tidligstInaktiveBehandling = fagsak.hentTidligsteInaktiveBehandling();
        Behandling nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            tidligstInaktiveBehandling,
            Behandlingsstatus.OPPRETTET,
            prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)
        );

        prosessinstans.setBehandling(nyBehandling);

        fagsakService.lagre(fagsak);

        log.info("Behandling {} replikert og behandling {} har blitt opprettet for {}",
            tidligstInaktiveBehandling.getId(), nyBehandling.getId(), saksnummer);
    }
}
