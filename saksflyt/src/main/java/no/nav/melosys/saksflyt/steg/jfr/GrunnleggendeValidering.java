package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_VURDER_JOURNALFOERINGSTYPE;

/**
 * Utfører grunnleggende validering
 *
 * Transisjoner:
 * JFR_VALIDERING → JFR_AVSLUTT_OPPGAVE (eller til FEILET_MASKINELT hvis det blir oppdaget feil eller mangler)
 */
@Component
public class GrunnleggendeValidering extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(GrunnleggendeValidering.class);

    private FagsakService fagsakService;

    @Autowired
    public GrunnleggendeValidering(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VALIDERING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.JFR_NY_SAK && prosessType != ProsessType.JFR_KNYTT) {
            throw new TekniskException("ProsessType " + prosessType + " er ikke støttet");
        }

        if (prosessType == ProsessType.JFR_NY_SAK) {
            Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
            validerSøknadsperiode(periode);
            validerBehandlingstema(prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class));
            prosessinstans.setData(BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        }

        if (prosessType == ProsessType.JFR_KNYTT) {
            String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
            Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
            Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
            if (StringUtils.isEmpty(saksnummer) || fagsak == null) {
                throw new FunksjonellException("Det finnes ingen fagsak med saksnummer " + saksnummer);
            }

            if (behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
                Behandling aktivBehandling = fagsak.hentAktivBehandling();
                Behandling tidligsteInaktiveBehandling = fagsak.getTidligsteInaktiveBehandling();
                if (aktivBehandling != null) {
                    throw new FunksjonellException("Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som har en aktiv behandling");
                }
                if (tidligsteInaktiveBehandling == null) {
                    throw new FunksjonellException("Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som mangler en inaktiv behandling");
                }
            }
        }

        String brukerId = prosessinstans.getData(BRUKER_ID);
        if (brukerId == null) {
            throw new FunksjonellException("Bruker id er ikke oppgitt.");
        }

        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        if (journalpostID == null) {
            throw new FunksjonellException("Mangle journalpostID");
        }

        String hovdokTittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        if (hovdokTittel == null) {
            throw new FunksjonellException("Mangler hoveddokument tittel.");
        }

        String dokumentID = prosessinstans.getData(DOKUMENT_ID);
        if (dokumentID == null) {
            throw new FunksjonellException("Mangler dokumentID");
        }

        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (saksbehandler == null) {
            throw new FunksjonellException("Mangler saksbehandler");
        }

        prosessinstans.setSteg(JFR_VURDER_JOURNALFOERINGSTYPE);

        log.info("Ferdig med grunnleggende validering av prosessinstans {}", prosessinstans.getId());
    }

    private void validerBehandlingstema(Behandlingstema behandlingstema) throws FunksjonellException {
        if (!Behandling.erBehandlingAvSøknad(behandlingstema)) {
            throw new FunksjonellException("Behandlingstema gjelder ikke en søknad!");
        }
    }

    private void validerSøknadsperiode(Periode periode) throws FunksjonellException {
        if (periode == null || periode.getFom() == null) {
            throw new FunksjonellException("Søknadsperioden er ikke oppgitt eller mangler fom.");
        }
        if (periode.getTom() != null && periode.getFom().isAfter(periode.getTom())) {
            throw new FunksjonellException("Fra og med dato kan ikke være etter til og med dato.");
        }
    }
}
