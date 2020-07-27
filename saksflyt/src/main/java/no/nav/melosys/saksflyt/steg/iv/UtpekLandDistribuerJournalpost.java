package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.AbstraktDistribuerJournalpost;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UtpekLandDistribuerJournalpost extends AbstraktDistribuerJournalpost {
    private static final Logger log = LoggerFactory.getLogger(UtpekLandDistribuerJournalpost.class);

    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public UtpekLandDistribuerJournalpost(DoksysFasade doksysFasade,
                                          UtenlandskMyndighetService utenlandskMyndighetService) {
        super(doksysFasade);
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.UL_DISTRIBUER_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(utpektLand);
        log.info("Bestiller distribuering av journalpost {} for å sende A003 som brev i behandling {}",
            journalpostId, prosessinstans.getBehandling().getId());
        bestillDistribuering(journalpostId, utenlandskMyndighet);
        prosessinstans.setSteg(ProsessSteg.IV_OPPDATER_RESULTAT);
    }
}
