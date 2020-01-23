package no.nav.melosys.saksflyt.steg.ul;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class UtpekAnnetLandSendSed extends AbstraktStegBehandler {

    private final BehandlingService behandlingService;
    private final BrevBestiller brevBestiller;
    private final EessiService eessiService;
    private final FagsakService fagsakService;
    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final ProsessinstansRepository prosessinstansRepository;

    protected UtpekAnnetLandSendSed(EessiService eessiService, BehandlingService behandlingService,
                                    BrevBestiller brevBestiller, FagsakService fagsakService,
                                    JoarkFasade joarkFasade, TpsFasade tpsFasade,
                                    UtenlandskMyndighetService utenlandskMyndighetService,
                                    ProsessinstansRepository prosessinstansRepository) {
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.prosessinstansRepository = prosessinstansRepository;
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

        Set<String> mottakerinstitusjonerSendt = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKERE_SENDT, HashSet.class);
        if (mottakerinstitusjonerSendt == null) {
            mottakerinstitusjonerSendt = new HashSet<>();
        }

        if (!eessiService.landErEessiReady(BucType.LA_BUC_02.name(), utpektLand.getKode())) {
            sendBrev(prosessinstans);
            prosessinstans.setSteg(ProsessSteg.UL_DISTRIBUER_JOURNALPOST);
        } else {
            for (String mottakerinstitusjon : mottakerinstitusjoner) {
                if (mottakerinstitusjonerSendt.contains(mottakerinstitusjon)) {
                    continue;
                }
                try {
                    sendSed(prosessinstans, mottakerinstitusjon);
                    mottakerinstitusjonerSendt.add(mottakerinstitusjon);
                } catch (Exception e) {
                    prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE_SENDT, mottakerinstitusjonerSendt);
                    prosessinstansRepository.save(prosessinstans);
                    throw new TekniskException("Sending av SED feilet for behandlingID " + prosessinstans.getBehandling().getId()
                        + " til mottakerinstitusjon" + mottakerinstitusjon, e);
                }
            }
            prosessinstans.setSteg(ProsessSteg.FERDIG);
        }
    }

    private void sendSed(Prosessinstans prosessinstans, String mottakerInstitusjon) throws MelosysException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        if (mottakerInstitusjon == null) {
            mottakerInstitusjon = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKER);
        }
        if (StringUtils.isEmpty(mottakerInstitusjon)) {
            mottakerInstitusjon = eessiService.hentMottakerinstitusjonFraBuc(prosessinstans.getBehandling().getFagsak(), BucType.LA_BUC_02);
        }
        eessiService.opprettOgSendSed(behandlingID, mottakerInstitusjon, BucType.LA_BUC_02);
    }

    private void sendBrev(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Landkoder utpektLand = prosessinstans.getData(ProsessDataKey.UTPEKT_LAND, Landkoder.class);
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(utpektLand);
        String institusjonsId = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);

        String fnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        byte[] pdf = eessiService.genererPdfFraSed(behandling.getId(), SedType.A003);
        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), fnr, SedType.A003, pdf,
            institusjonsId, utenlandskMyndighet.navn, utpektLand.getKode(), null
        );
        String journalpostId = joarkFasade.opprettJournalpost(opprettJournalpost, true);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
    }
}
