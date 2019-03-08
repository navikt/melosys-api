package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_AKTØR_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPDATER_JOURNALPOST;

/**
 * Journalføring av nytt dokument på eksisterende sak.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK:
 *  JFR_VURDER_JOURNALFOERINGSTYPE → JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 *  a) Saken har aktiv behandling (behandlingsstatus oppdateres til VURDER_DOKUMENT):
 *  JFR_VURDER_JOURNALFOERINGSTYPE → JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 *  b) Saken har ikke aktiv behandling og skal ikke behandles (behandlingstype null):
 *  JFR_VURDER_JOURNALFOERINGSTYPE → JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 *  c) Saken har ikke aktiv behandling og skal behandles:
 *  JFR_VURDER_JOURNALFOERINGSTYPE → JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 *  d) Saken har en avsluttet behandling og et fattet vedtak, men det er journalført inn et dokument
 *  som tilsier at perioden har blitt kortere enn den det tidligere har blitt innvilget vedtak på.
 *  JFR_KOPIER_BEHANDLINGSINFORMASJON
 */
@Component
public class VurderJournalfoeringstype extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(VurderJournalfoeringstype.class);

    private final FagsakRepository fagsakRepository;

    @Autowired
    public VurderJournalfoeringstype(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
        log.info("VurderJournalfoeringstype initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VURDER_JOURNALFOERINGSTYPE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        switch (prosessinstans.getType()) {
            case JFR_NY_SAK:
                prosessinstans.setSteg(JFR_AKTØR_ID);
                break;
            case JFR_KNYTT:
                knyttEllerNyBehandlingEllerReplikerBehandling(prosessinstans);
                break;
            default:
                String feilmelding = "Ukjent prosesstype: " + prosessinstans.getType();
                log.error("{}: {}", prosessinstans.getId(), feilmelding);
                håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
                return;
        }

        log.info("Prosessinstans {} har vurdert journalpost {}", prosessinstans.getId(), prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID));
        log.debug("Neste steg blir: {}", prosessinstans.getSteg());
    }

    private void knyttEllerNyBehandlingEllerReplikerBehandling(Prosessinstans prosessinstans) throws TekniskException {
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Behandlingstyper nyBehandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);

        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        Behandling aktivBehandling = fagsak.getAktivBehandling();
        Behandling tidligsteInaktiveBehandling = fagsak.getTidligsteInaktiveBehandling();

        if (nyBehandlingstype != null && nyBehandlingstype.equals(Behandlingstyper.ENDRET_PERIODE)) {
            if (aktivBehandling == null && tidligsteInaktiveBehandling != null) {
                prosessinstans.setSteg(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
            }
            else {
                String feilmelding = "Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som har en aktiv behandling eller mangler en inaktiv behandling";
                log.error("{}: {}", prosessinstans.getId(), feilmelding);
                håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);            }
        }
        else if (aktivBehandling == null && nyBehandlingstype != null) {
            // Ny behandling trenges.
            prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);
            prosessinstans.setSteg(ProsessSteg.JFR_AKTØR_ID);
        } else {
            // Dokumentet journalføres direkte.
            prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
        }
    }
}
