package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.AbstraktDistribuerJournalpost;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.LandvelgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("vsDistribuerJournalpost")
public class DistribuerJournalpost extends AbstraktDistribuerJournalpost {
    private final LandvelgerService landvelgerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    public DistribuerJournalpost(DoksysFasade doksysFasade, LandvelgerService landvelgerService, UtenlandskMyndighetService utenlandskMyndighetService) {
        super(doksysFasade);
        this.landvelgerService = landvelgerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.VS_DISTRIBUER_JOURNALPOST;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        bestillDistribuering(journalpostId, hentUtenlandskMyndighet(prosessinstans.getBehandling()));

        prosessinstans.setSteg(ProsessSteg.IV_STATUS_BEH_AVSL);
    }

    private UtenlandskMyndighet hentUtenlandskMyndighet(Behandling behandling) throws MelosysException {
        Landkoder landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId()).stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Fant ikke trygdemyndighetsland for behandling " + behandling.getId()));
        return utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
    }
}
