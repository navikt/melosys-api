package no.nav.melosys.saksflyt.agent.jfr;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.REPLIKER_BEHANDLING;


/**
 * Ferdigstiller en journalpost i Joark.
 *
 * Transisjoner:
 *     REPLIKER_BEHANDLING -> EN NY FLOTT GREIE eller FEILET_MASKINELT hvis feil
 */
@Component
public class ReplikerBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ReplikerBehandling.class);

    private FagsakRepository fagsakRepository;
    private BehandlingService behandlingService;

    @Autowired
    public ReplikerBehandling(FagsakRepository fagsakRepository, BehandlingService behandlingService) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        log.info("ReplikerBehandling initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return REPLIKER_BEHANDLING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);

        Behandling behandling;
        if (fagsak.getTidligsteInaktiveBehandling() != null) {
            try {
                behandling = behandlingService.replikerBehandlingOgBehandlingsresultat(fagsak.getTidligsteInaktiveBehandling(), Behandlingsstatus.OPPRETTET, Behandlingstyper.ENDRET_PERIODE);

            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                throw new TekniskException("Klarte ikke replikere tidligste inaktive behandling for fagsak: " + fagsak.getSaksnummer(), e);
            }

        } else {
            throw new FunksjonellException("Finner ingen avsluttet behandling på fagsak " + fagsak.getSaksnummer());
        }
        prosessinstans.setBehandling(behandling);

        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsakRepository.save(fagsak);

        prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        log.info("Prosessinstans {} har replikert behandling for {}", prosessinstans.getId(), saksnummer);
    }
}
