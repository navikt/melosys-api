package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.AbstraktDistribuerJournalpost;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.springframework.stereotype.Component;

@Component
public class UtpekLandDistribuerJournalpost extends AbstraktDistribuerJournalpost {

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
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(utpektLand);
        bestillDistribuering(journalpostId, utenlandskMyndighet);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
