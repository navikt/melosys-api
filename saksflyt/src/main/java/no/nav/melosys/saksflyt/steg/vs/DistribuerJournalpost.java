package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.AbstraktDistribuerJournalpost;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("VideresendSoknadDistribuerJournalpost")
public class DistribuerJournalpost extends AbstraktDistribuerJournalpost {
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final LandvelgerService landvelgerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    public DistribuerJournalpost(DoksysFasade doksysFasade,
                                 BehandlingsgrunnlagService behandlingsgrunnlagService, LandvelgerService landvelgerService,
                                 UtenlandskMyndighetService utenlandskMyndighetService) {
        super(doksysFasade);
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.landvelgerService = landvelgerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.VS_DISTRIBUER_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        bestillDistribuering(journalpostId, hentUtenlandskMyndighet(prosessinstans.getBehandling()));

        prosessinstans.setSteg(ProsessSteg.IV_STATUS_BEH_AVSL);
    }

    private UtenlandskMyndighet hentUtenlandskMyndighet(Behandling behandling) throws MelosysException {
        BehandlingsgrunnlagData grunnlagData = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandling.getId()).getBehandlingsgrunnlagdata();
        Landkoder landkode = landvelgerService.hentBostedsland(behandling.getId(), grunnlagData);
        return utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
    }
}
