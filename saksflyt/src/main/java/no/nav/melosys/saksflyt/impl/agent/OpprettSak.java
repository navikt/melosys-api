package no.nav.melosys.saksflyt.impl.agent;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_SAK;
import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;

@Component
public class OpprettSak extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(OpprettSak.class);

    FagsakService fagsakService;

    @Autowired
    public OpprettSak(Binge binge, ProsessinstansRepository prosessinstansRepo, FagsakService fagsakService) {
        super(binge, prosessinstansRepo);
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SAK;
    }

    @Override
    public void utfoerSteg(Prosessinstans prosessinstans) {
        String aktørId = prosessinstans.getData().getProperty(AKTØR_ID);
        ProsessType type = prosessinstans.getType(); //TODO Bruker vi ProsessType eller BehandlingType?

        Fagsak fagsak = null;
        try {
            fagsak = fagsakService.nyFagsakOgBehandling(aktørId, BehandlingType.SØKNAD, false);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg " + inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setSteg(JFR_OPPRETT_GSAK_SAK);
    }
}
