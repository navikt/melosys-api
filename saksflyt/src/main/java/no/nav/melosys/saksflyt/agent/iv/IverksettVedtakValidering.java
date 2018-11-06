package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.ProsessSteg.IV_VALIDERING;
import static no.nav.melosys.feil.Feilkategori.FUNKSJONELL_FEIL;

/**
 * Validerer opplysning bli brukt for iverksette vedtak.
 *
 * Transisjoner:
 *
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_VALIDERING -> IV_OPPDATER_RESULTAT eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksettVedtakValidering extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakValidering.class);

    @Autowired
    public IverksettVedtakValidering() {
        log.info("IverksetteVedtakValidering initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_VALIDERING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.IVERKSETT_VEDTAK) {
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

        prosessinstans.setSteg(IV_OPPDATER_RESULTAT);
    }
}
