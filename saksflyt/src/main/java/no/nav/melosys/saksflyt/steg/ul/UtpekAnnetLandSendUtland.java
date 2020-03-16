package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class UtpekAnnetLandSendUtland extends AbstraktSendUtland {

    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    protected UtpekAnnetLandSendUtland(BehandlingsresultatService behandlingsresultatService,
                                       EessiService eessiService,
                                       JoarkFasade joarkFasade,
                                       TpsFasade tpsFasade,
                                       UtenlandskMyndighetService utenlandskMyndighetService) {
        super(eessiService, behandlingsresultatService);
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.UL_SEND_UTLAND;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        SendUtlandStatus status = sendUtland(BucType.LA_BUC_02, prosessinstans);

        if (status == SendUtlandStatus.BREV_SENDT) {
            prosessinstans.setSteg(ProsessSteg.UL_DISTRIBUER_JOURNALPOST);
        } else {
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        }
    }

    protected void sendBrev(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(utpektLand);
        String institusjonsId = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);

        String fnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        byte[] pdf = eessiService.genererSedPdf(behandling.getId(), SedType.A003);
        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), fnr, SedType.A003, pdf,
            institusjonsId, utenlandskMyndighet.navn, utpektLand.getKode(), null
        );
        String journalpostId = joarkFasade.opprettJournalpost(opprettJournalpost, true);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return true;
    }
}
