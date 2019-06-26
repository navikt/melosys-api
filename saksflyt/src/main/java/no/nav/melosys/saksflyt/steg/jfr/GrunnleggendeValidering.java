package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.JFR_VURDER_JOURNALFOERINGSTYPE;
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
    private static final String PID_MELDING = "{}: {}";

    private FagsakRepository fagsakRepository;

    public GrunnleggendeValidering() {
        log.info("GrunnleggendeValidering initialisert");
    }

    @Autowired
    public GrunnleggendeValidering(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
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
    public void utfør(Prosessinstans prosessinstans) throws TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.JFR_NY_SAK && prosessType != ProsessType.JFR_KNYTT) {
            String feilmelding = "ProsessType " + prosessType + " er ikke støttet";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        if (prosessType == ProsessType.JFR_NY_SAK) {
            Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
            if (periode == null || periode.getFom() == null) {
                log.error("Funksjonell feil for prosessinstans {}: Søknadsperioden er ikke oppgitt eller mangler fom.", prosessinstans.getId());
                håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Søknadsperioden er ikke oppgitt eller mangler fom.", null);
                return;
            }
        }

        if (prosessType == ProsessType.JFR_KNYTT) {
            String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
            Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
            if (StringUtils.isEmpty(saksnummer) || fagsak == null) {
                String feilmelding = "Det finnes ingen fagsak med saksnummer " + saksnummer;
                log.error(feilmelding);
                håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
                return;
            }

            Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
            if (behandlingstype != null && behandlingstype.equals(Behandlingstyper.ENDRET_PERIODE)) {
                Behandling aktivBehandling = fagsak.getAktivBehandling();
                Behandling tidligsteInaktiveBehandling = fagsak.getTidligsteInaktiveBehandling();
                if (aktivBehandling != null) {
                    String feilmelding = "Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som har en aktiv behandling";
                    log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
                    håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
                }
                if (tidligsteInaktiveBehandling == null) {
                    String feilmelding = "Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som mangler en inaktiv behandling";
                    log.error(PID_MELDING, prosessinstans.getId(), feilmelding);
                    håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
                }
            }
        }

        String brukerId = prosessinstans.getData(BRUKER_ID);
        if (brukerId == null) {
            log.error("Funksjonell feil for prosessinstans {}: Bruker id er ikke oppgitt.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Bruker id er ikke oppgitt.", null);
            return;
        }

        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        if (journalpostID == null) {
            log.error("Funksjonell feil for prosessinstans {}: Mangler journalpostID.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Mangler journalpostID.", null);
            return;
        }

        String hovdokTittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        if (hovdokTittel == null) {
            log.error("Funksjonell feil for prosessinstans {}: Mangler hoveddokument tittel.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Mangler hoveddokument tittel.", null);
            return;
        }

        String dokumentID = prosessinstans.getData(DOKUMENT_ID);
        if (dokumentID == null) {
            log.error("Funksjonell feil for prosessinstans {}: Mangler dokumentId.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Mangler dokumentId.", null);
            return;
        }

        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (saksbehandler == null) {
            log.error("Funksjonell feil for prosessinstans {}: Mangler saksbehandler.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Mangler saksbehandler.", null);
            return;
        }

        prosessinstans.setSteg(JFR_VURDER_JOURNALFOERINGSTYPE);

        log.info("Ferdig med grunnleggende validering av prosessinstans {}", prosessinstans.getId());
    }

}
