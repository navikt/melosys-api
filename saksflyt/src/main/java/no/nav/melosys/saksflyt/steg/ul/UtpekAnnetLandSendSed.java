package no.nav.melosys.saksflyt.steg.ul;

import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Component;

@Component
public class UtpekAnnetLandSendSed extends AbstraktSendUtland {

    private final BehandlingService behandlingService;
    private final BrevBestiller brevBestiller;
    private final FagsakService fagsakService;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    protected UtpekAnnetLandSendSed(EessiService eessiService, BehandlingsresultatService behandlingsresultatService,
                                    LandvelgerService landvelgerService, BehandlingService behandlingService,
                                    BrevBestiller brevBestiller, FagsakService fagsakService, TpsFasade tpsFasade,
                                    UtenlandskMyndighetService utenlandskMyndighetService) {
        super(eessiService, behandlingsresultatService, landvelgerService);
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
        this.fagsakService = fagsakService;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.UL_SEND_BREV;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        List<String> mottakerinstitusjoner = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKERE, List.class);
        Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();

        fagsakService.oppdaterMyndigheter(saksnummer, mottakerinstitusjoner);

        for (String mottakerinstitusjon : mottakerinstitusjoner) {
            // FIXME håndter feilede
            SendUtlandStatus sendtStatus = sendUtland(BucType.LA_BUC_03, prosessinstans, utpektLand.getKode(), mottakerinstitusjon, null);
        }
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(utpektLand);
        String institusjonsId = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);

        String fnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        byte[] pdf = null; // FIXME lag pdf
        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), fnr, SedType.A008, pdf,
            institusjonsId, utenlandskMyndighet.navn, utpektLand.getKode(), null
        );
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return true;
    }
}
