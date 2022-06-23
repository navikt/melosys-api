package no.nav.melosys.saksflyt.steg.brev;

import java.util.Optional;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.brev.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DistribuerJournalpostUtland implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpostUtland.class);

    private final DoksysFasade doksysFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public DistribuerJournalpostUtland(DoksysFasade doksysFasade,
                                       UtenlandskMyndighetService utenlandskMyndighetService) {
        this.doksysFasade = doksysFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.DISTRIBUER_JOURNALPOST_UTLAND;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        String distribuerbarJournalpost = prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID);

        if (StringUtils.isNotEmpty(distribuerbarJournalpost)) {

            Landkoder mottakerLand = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, Landkoder.class))
                .orElseThrow(() -> new IkkeFunnetException("Kan ikke distribuere journalpost da mottakerland ikke er satt"));
            UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(mottakerLand);
            Distribusjonstype distribusjonstype =  prosessinstans.getData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.class);
            log.info("Bestiller distribuering av journalpost {} til land {} i behandling {} med distribusjonstype {}",
                distribuerbarJournalpost, mottakerLand, prosessinstans.getBehandling().getId(), distribusjonstype);

            bestillDistribuering(distribuerbarJournalpost, utenlandskMyndighet, distribusjonstype);
        }
    }

    private void bestillDistribuering(String journalpostId, UtenlandskMyndighet utenlandskMyndighet, Distribusjonstype distribusjonstype) {
        String bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, utenlandskMyndighet.getAdresse(), distribusjonstype);
        log.info("Distribuering av journalpost {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
    }
}
