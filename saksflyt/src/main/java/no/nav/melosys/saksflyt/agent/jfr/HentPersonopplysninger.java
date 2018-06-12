package no.nav.melosys.saksflyt.agent.jfr;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.StandardAbstraktAgent;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_HENT_PERS_OPPL;
import static no.nav.melosys.domain.ProsessSteg.JFR_VURDER_INNGANGSVILKÅR;

/**
 * Steget sørger for å hente personinfo fra TPS
 * 
 * Transisjoner: 
 * JFR_HENT_PERS_OPPL → JFR_VURDER_INNGANGSVILKÅR hvis alt ok
 * JFR_HENT_PERS_OPPL → FEILET_MASKINELT hvis personen ikke finnes i TPS
 */
@Component
public class HentPersonopplysninger extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(HentPersonopplysninger.class);

    private TpsFasade tpsFasade;

    private FagsakService fagsakService;

    @Autowired
    public HentPersonopplysninger(Binge binge, ProsessinstansRepository prosessinstansRepo, TpsFasade tpsFasade, FagsakService fagsakService) {
        super(binge, prosessinstansRepo);
        this.tpsFasade = tpsFasade;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_HENT_PERS_OPPL;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        try {
            Saksopplysning saksopplysning = tpsFasade.hentPerson(brukerId);
            prosessinstans.getBehandling().getSaksopplysninger().add(saksopplysning);

            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            fagsakService.lagre(fagsak);
        } catch (IkkeFunnetException | SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
            return;
        }

        prosessinstans.setSteg(JFR_VURDER_INNGANGSVILKÅR);
    }
}
