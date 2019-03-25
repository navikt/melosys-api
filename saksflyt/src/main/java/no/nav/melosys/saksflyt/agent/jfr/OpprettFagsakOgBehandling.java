package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.audit.AuditorProvider;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.*;

/**
 * Oppretter en sak og en behandling i Melosys.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_KNYTT:
 * JFR_OPPRETT_SAK_OG_BEH -> STATUS_BEH_OPPR eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_NY_SAK:
 * JFR_OPPRETT_SAK_OG_BEH -> JFR_OPPRETT_SOEKNAD eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;

    private final BehandlingService behandlingService;

    private final AuditorProvider auditorAware;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService,
                                     BehandlingService behandlingService,
                                     AuditorProvider auditorAware) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.auditorAware = auditorAware;
        log.info("OpprettSak initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SAK_OG_BEH;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String arbeidsgiver = prosessinstans.getData(ARBEIDSGIVER);
        String representant = prosessinstans.getData(REPRESENTANT);
        String representantKontakperson = prosessinstans.getData(REPRESENTANT_KONTAKTPERSON);
        String endretAv = prosessinstans.getData(SAKSBEHANDLER);
        String initierendeJournalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String initierendeDokumentId = prosessinstans.getData(DOKUMENT_ID);
        String gsakSaksnummer = prosessinstans.getData(GSAK_SAK_ID);
        auditorAware.setSaksbehanlderID(endretAv);

        if (prosessinstans.getType() == ProsessType.JFR_NY_BEHANDLING) {
            String saksnummer = prosessinstans.getData(SAKSNUMMER);
            Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
            Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
            Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.VURDER_DOKUMENT, behandlingstype, initierendeJournalpostId, initierendeDokumentId);
            prosessinstans.setBehandling(behandling);

            prosessinstans.setSteg(STATUS_BEH_OPPR);
            log.info("Opprettet behandling {} for prosessinstans {}", behandling.getId(), prosessinstans.getId());
        } else if (prosessinstans.getType() == ProsessType.JFR_NY_SAK) {
            OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID(aktørId).medArbeidsgiver(arbeidsgiver)
                .medRepresentant(representant).medRepresentantKontaktperson(representantKontakperson)
                .medBehandlingstype(Behandlingstyper.SOEKNAD).medInitierendeJournalpostId(initierendeJournalpostId)
                .medInitierendeDokumentId(initierendeDokumentId).build();
            Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
            prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
            prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

            prosessinstans.setSteg(JFR_OPPRETT_SØKNAD);
            log.info("Opprettet fagsak {} for prosessinstans {}", fagsak.getSaksnummer(), prosessinstans.getId());
        } else if(prosessinstans.getType() == ProsessType.REGISTRERING_UNNTAK) {
            OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID(aktørId)
                .medBehandlingstype(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP)
                .medInitierendeJournalpostId(initierendeJournalpostId)
                .medInitierendeDokumentId(initierendeDokumentId)
                .medGsakSaksnummer(gsakSaksnummer)
                .build();

            Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
            prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
            prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

            prosessinstans.setSteg(STATUS_BEH_OPPR);
        } else {
            String feilmelding = "ProsessType " + prosessinstans.getType() + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
        }
    }
}
