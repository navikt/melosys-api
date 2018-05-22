package no.nav.melosys.saksflyt.impl.agent;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.felles.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_AKTOER_ID;
import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;

@Component
public class HentAktoerId extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(HentAktoerId.class);

    private TpsFasade tpsFasade;

    @Autowired
    public HentAktoerId(Binge binge, ProsessinstansRepository prosessinstansRepo, TpsFasade tpsFasade) {
        super(binge, prosessinstansRepo);
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_AKTOER_ID;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        String aktørId = null;
        try {
            aktørId = tpsFasade.hentAktørIdForIdent(brukerId);
        } catch (IkkeFunnetException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setData(AKTØR_ID, aktørId);
        prosessinstans.setSteg(ProsessSteg.JFR_OPPRETT_SAK);
    }
}
