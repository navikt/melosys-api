package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.audit.AuditorProvider;
import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.datavarehus.BehandlingOpprettetEvent;
import no.nav.melosys.service.datavarehus.FagsakOpprettetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_SAK_OG_BEH;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_SØKNAD;

/**
 * Oppretter en sak og en behandling i Melosys.
 *
 * Transisjoner:
 * JFR_OPPRETT_SAK_OG_BEH -> JFR_OPPRETT_SOEKNAD eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final AuditorProvider auditorAware;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService, ApplicationEventPublisher applicationEventPublisher, AuditorProvider auditorAware) {
        this.fagsakService = fagsakService;
        this.applicationEventPublisher = applicationEventPublisher;
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
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String arbeidsgiver = prosessinstans.getData(ARBEIDSGIVER);
        String representant = prosessinstans.getData(REPRESENTANT);
        String endretAv = prosessinstans.getData(SAKSBEHANDLER);
        auditorAware.setSaksbehanlderID(endretAv);

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(aktørId, arbeidsgiver, representant, Behandlingstype.SØKNAD);
        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

        applicationEventPublisher.publishEvent(new FagsakOpprettetEvent(fagsak, endretAv));
        applicationEventPublisher.publishEvent(new BehandlingOpprettetEvent(fagsak.getBehandlinger().get(0), endretAv));

        prosessinstans.setSteg(JFR_OPPRETT_SØKNAD);
        log.info("Opprettet fagsak {} for prosessinstans {}", fagsak.getSaksnummer(), prosessinstans.getId());
    }
}
