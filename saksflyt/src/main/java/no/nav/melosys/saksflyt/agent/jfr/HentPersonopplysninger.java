package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class HentPersonopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentPersonopplysninger.class);

    private TpsFasade tpsFasade;

    @Autowired
    public HentPersonopplysninger(@Qualifier("system")TpsFasade tpsFasade) {
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_HENT_PERS_OPPL;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        try {
            Behandling behandling = prosessinstans.getBehandling();
            Saksopplysning saksopplysning = tpsFasade.hentPersonMedAdresse(brukerId);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setRegistrertDato(LocalDateTime.now());
            behandling.getSaksopplysninger().add(saksopplysning);
        } catch (IkkeFunnetException | SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            // FIXME: MELOSYS-1316
            return;
        }

        prosessinstans.setSteg(JFR_VURDER_INNGANGSVILKÅR);
    }
}
