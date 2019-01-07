package no.nav.melosys.saksflyt.agent.au;

import java.util.Map;

import no.nav.melosys.domain.BehandlingsresultatType;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BEHANDLINGSRESULTATTYPE;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.AU_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.ProsessSteg.AU_VALIDERING;
import static no.nav.melosys.feil.Feilkategori.FUNKSJONELL_FEIL;

/**
 * Validerer opplysning bli brukt for anmodning om unntak.
 *
 * Transisjoner:
 *
 * ProsessType.ANMODNING_UNNTAK
 *  AU_VALIDERING -> AU_OPPDATER_RESULTAT eller FEILET_MASKINELT hvis feil
 */
@Component
public class AnmodningUnntakValidering extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakValidering.class);

    @Autowired
    public AnmodningUnntakValidering() {
        log.info("AnmodningUnntakValidering initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AU_VALIDERING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.ANMODNING_UNNTAK) {
            String feilmelding = "ProsessType " + prosessType + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        String saksbehandlerID = prosessinstans.getData(SAKSBEHANDLER);
        if (saksbehandlerID == null) {
            log.error("Funksjonell feil for prosessinstans {}: SaksbehandlerID er ikke oppgitt.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "saksbehandlerID er ikke oppgitt.", null);
            return;
        }

        BehandlingsresultatType behandlingsresultatType = prosessinstans.getData(BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.class);
        if (behandlingsresultatType != BehandlingsresultatType.ANMODNING_OM_UNNTAK) {
            log.error("Funksjonell feil for prosessinstans {}: behandlingsresultatType er ikke oppgitt.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "behandlingsresultatType er ikke oppgitt.", null);
            return;
        }

        prosessinstans.setSteg(AU_OPPDATER_RESULTAT);
    }
}
