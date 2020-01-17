package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

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

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService,
                                     BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        log.info("OpprettSak initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SAK_OG_BEH;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String arbeidsgiver = prosessinstans.getData(ARBEIDSGIVER);
        String representant = prosessinstans.getData(REPRESENTANT);
        String representantKontakperson = prosessinstans.getData(REPRESENTANT_KONTAKTPERSON);
        Representerer representantRepresenterer = prosessinstans.getData(REPRESENTANT_REPRESENTERER, Representerer.class);
        String initierendeJournalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String initierendeDokumentId = prosessinstans.getData(DOKUMENT_ID);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);

        if (prosessinstans.getType() == ProsessType.JFR_NY_BEHANDLING) {
            String saksnummer = prosessinstans.getData(SAKSNUMMER);
            Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
            Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.VURDER_DOKUMENT, behandlingstype, initierendeJournalpostId, initierendeDokumentId);
            prosessinstans.setBehandling(behandling);

            prosessinstans.setSteg(STATUS_BEH_OPPR);
            log.info("Opprettet behandling {} for prosessinstans {}", behandling.getId(), prosessinstans.getId());
        } else if (prosessinstans.getType() == ProsessType.JFR_NY_SAK || prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID(aktørId).medArbeidsgiver(arbeidsgiver)
                .medRepresentant(representant).medRepresentantKontaktperson(representantKontakperson)
                .medRepresentantRepresenterer(representantRepresenterer)
                .medBehandlingstype(behandlingstype).medInitierendeJournalpostId(initierendeJournalpostId)
                .medInitierendeDokumentId(initierendeDokumentId).build();
            Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
            prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
            prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

            prosessinstans.setSteg(JFR_OPPRETT_SØKNAD);
            log.info("Opprettet fagsak {} for prosessinstans {}", fagsak.getSaksnummer(), prosessinstans.getId());
        } else {
            throw new TekniskException("ProsessType " + prosessinstans.getType() + " er ikke støttet");
        }
    }
}
