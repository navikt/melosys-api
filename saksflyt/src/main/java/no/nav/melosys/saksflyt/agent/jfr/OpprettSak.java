package no.nav.melosys.saksflyt.agent.jfr;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.StandardAbstraktAgent;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_SAK_OG_BEH;

/**
 * Oppretter en oppgave i GSAK.
 *
 * Transisjoner:
 * JFR_OPPRETT_SAK_OG_BEH -> JFR_OPPRETT_GSAK_SAK eller FEILET_MASKINELT hvis feil
 */
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
        return JFR_OPPRETT_SAK_OG_BEH;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String aktørId = prosessinstans.getData(AKTØR_ID);
        ProsessType prosessType = prosessinstans.getType();
        BehandlingType behandlingType = null;
        if (ProsessType.JFR_NY_SAK.equals(prosessType)) {
            behandlingType = BehandlingType.SØKNAD;
        } else  {
            throw new TekniskException("ProsessType " + prosessType + " er ikke støttet");
        }

        Fagsak fagsak = null;
        try {
            fagsak = fagsakService.nyFagsakOgBehandling(aktørId, behandlingType, false);
            prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
            return;
        }

        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setSteg(JFR_OPPRETT_GSAK_SAK);
    }
}
