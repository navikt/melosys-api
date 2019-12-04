package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.AbstraktDistribuerJournalpost;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.LandvelgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("VideresendSoknadDistribuerJournalpost")
public class DistribuerJournalpost extends AbstraktDistribuerJournalpost {
    private final SoeknadService soeknadService;
    private final LandvelgerService landvelgerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    public DistribuerJournalpost(DoksysFasade doksysFasade,
                                 SoeknadService soeknadService,
                                 LandvelgerService landvelgerService,
                                 UtenlandskMyndighetService utenlandskMyndighetService) {
        super(doksysFasade);
        this.soeknadService = soeknadService;
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
        SoeknadDokument søknad = soeknadService.hentSøknad(behandling.getId());
        Landkoder landkode = landvelgerService.hentBostedsland(behandling.getId(), søknad);
        return utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
    }
}
