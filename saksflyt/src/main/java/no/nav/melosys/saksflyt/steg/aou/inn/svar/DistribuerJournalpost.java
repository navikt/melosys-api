package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.AbstraktDistribuerJournalpost;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//TODO: slettes og erstattes med DistribuerJournalpostUtland.java
@Component
public class DistribuerJournalpost extends AbstraktDistribuerJournalpost {
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    public DistribuerJournalpost(DoksysFasade doksysFasade, UtenlandskMyndighetService utenlandskMyndighetService) {
        super(doksysFasade);
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        bestillDistribuering(journalpostId, hentUtenlandskMyndighet(prosessinstans.getBehandling().getFagsak()));

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }

    private UtenlandskMyndighet hentUtenlandskMyndighet(Fagsak fagsak) throws MelosysException {
        Landkoder landkode = fagsak.hentMyndighetLandkode();
        return utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
    }
}
