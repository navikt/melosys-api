package no.nav.melosys.saksflyt.agent.au;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.jfr.OppdaterJournalpost;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.AU_OPPDATER_MEDL;
import static no.nav.melosys.domain.ProsessSteg.AU_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat med vedtaksdato og klagefrist.
 *
 * Transisjoner:
 * AU_OPPDATER_RESULTAT -> AU_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AnmodningUnntakOppdaterBehandlingsresultat extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterJournalpost.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public AnmodningUnntakOppdaterBehandlingsresultat(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        log.info("OppdaterBehandlingsresultat initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AU_OPPDATER_RESULTAT;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findOne(behandlingID);

        behandlingsresultat.setType(BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultatRepository.save(behandlingsresultat);

        prosessinstans.setSteg(AU_OPPDATER_MEDL);
        log.info("Oppdatert behandlingsresultat for prosessinstans {}.", prosessinstans.getId());
    }
}
