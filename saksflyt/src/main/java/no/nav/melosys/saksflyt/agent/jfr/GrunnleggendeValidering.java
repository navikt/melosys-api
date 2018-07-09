package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.feil.Feilkategori.FUNKSJONELL_FEIL;

/**
 * Utfører grunnleggende validering
 *
 * Transisjoner:
 * JFR_VALIDERING → JFR_AVSLUTT_OPPGAVE (eller til FEILET_MASKINELT hvis det blir oppdaget feil eller mangler)
 */
@Component
public class GrunnleggendeValidering extends AbstraktStegBehandler {
    
    private static final Logger log = LoggerFactory.getLogger(GrunnleggendeValidering.class);

    public GrunnleggendeValidering() {
        log.info("GrunnleggendeValidering initialisert");
    }

    
    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VALIDERING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void utfør(Prosessinstans prosessinstans) throws TekniskException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        if (periode == null || periode.getFom() == null) {
            log.error("Funksjonell feil for {}: Søknadsperioden er ikke oppgitt eller mangler fom.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Søknadsperioden er ikke oppgitt eller mangler fom.", null);
            return;
        }

        String brukerId = prosessinstans.getData(BRUKER_ID);
        if (brukerId == null) {
            log.error("Funksjonell feil for {}: Bruker id er ikke oppgitt.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Bruker id er ikke oppgitt.", null);
            return;
        }

        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        if (journalpostID == null) {
            log.error("Funksjonell feil for {}: Mangler journalpostID.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Mangler journalpostID.", null);
            return;
        }

        // FIXME: Flyttet fra OpprettSak. Ok?
        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.JFR_NY_SAK) {
            String feilmelding = "ProsessType " + prosessType + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        prosessinstans.setSteg(ProsessSteg.JFR_AVSLUTT_OPPGAVE);
        log.info("Ferdig med grunnleggende validering av {}", prosessinstans.getId());
    }

}
