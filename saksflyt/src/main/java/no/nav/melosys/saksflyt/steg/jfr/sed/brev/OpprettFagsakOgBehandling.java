package no.nav.melosys.saksflyt.steg.jfr.sed.brev;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("JournalførAouBrevOpprettFagsakOgBehandling")
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final TpsFasade tpsFasade;
    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService,
                                     TpsFasade tpsFasade,
                                     @Qualifier("system") GsakFasade gsakFasade) {
        this.fagsakService = fagsakService;
        this.tpsFasade = tpsFasade;
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String aktørId = hentAktørId(prosessinstans);
        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(new OpprettSakRequest.Builder()
            .medAktørID(aktørId)
            .medSakstype(Sakstyper.EU_EOS)
            .medBehandlingstype(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class))
            .medInitierendeDokumentId(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID))
            .medInitierendeJournalpostId(prosessinstans.getData(ProsessDataKey.DOKUMENT_ID))
            .build());

        Behandling behandling = fagsak.getAktivBehandling();
        log.info("Fagsak {} opprettet med behandling {}", fagsak.getSaksnummer(), behandling.getId());
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setBehandling(behandling);

        long gsakSaksnummer = opprettGsakSak(fagsak, behandling.getType(), aktørId);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);

        prosessinstans.setSteg(ProsessSteg.JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST);
    }

    private String hentAktørId(Prosessinstans prosessinstans) throws IkkeFunnetException {
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        if (aktørId == null) {
            aktørId = hentAktørIdFraTps(prosessinstans.getData(ProsessDataKey.BRUKER_ID));
            prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørId);
        }

        return aktørId;
    }

    private String hentAktørIdFraTps(String brukerId) throws IkkeFunnetException {
        return tpsFasade.hentAktørIdForIdent(brukerId);
    }

    private long opprettGsakSak(Fagsak fagsak, Behandlingstyper behandlingstype, String aktørId) throws FunksjonellException, TekniskException {
        Long gsakSaksnummer = gsakFasade.opprettSak(fagsak.getSaksnummer(), behandlingstype, aktørId);
        fagsak.setGsakSaksnummer(gsakSaksnummer);
        fagsakService.lagre(fagsak);

        log.info("Sak {} opprettet i gsak for fagsak {}", gsakSaksnummer, fagsak.getSaksnummer());
        return gsakSaksnummer;
    }
}
