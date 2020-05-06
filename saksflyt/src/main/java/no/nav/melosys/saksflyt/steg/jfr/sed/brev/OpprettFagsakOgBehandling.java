package no.nav.melosys.saksflyt.steg.jfr.sed.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import no.nav.melosys.service.sak.SakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("JournalførAouBrevOpprettFagsakOgBehandling")
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final TpsFasade tpsFasade;
    private final SakService sakService;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService,
                                     TpsFasade tpsFasade,
                                     SakService sakService) {
        this.fagsakService = fagsakService;
        this.tpsFasade = tpsFasade;
        this.sakService = sakService;
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

        long gsakSaksnummer = opprettGsakSak(fagsak, behandling.getTema(), aktørId);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);

        lagreAvsender(
            fagsak.getSaksnummer(),
            prosessinstans.getData(ProsessDataKey.AVSENDER_ID),
            prosessinstans.getData(ProsessDataKey.AVSENDER_TYPE, Avsendertyper.class)
        );

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

    private long opprettGsakSak(Fagsak fagsak, Behandlingstema behandlingstema, String aktørId) throws FunksjonellException, TekniskException {
        Long gsakSaksnummer = sakService.opprettSak(fagsak.getSaksnummer(), behandlingstema, aktørId);
        fagsak.setGsakSaksnummer(gsakSaksnummer);
        fagsakService.lagre(fagsak);

        log.info("NAV-Sak {} opprettet for fagsak {}", gsakSaksnummer, fagsak.getSaksnummer());
        return gsakSaksnummer;
    }

    private void lagreAvsender(String saksnummer, String avsenderID, Avsendertyper avsenderType) throws FunksjonellException {
        Aktoersroller aktørrolle;
        switch (avsenderType) {
            case PERSON:
                return;
            case ORGANISASJON:
                aktørrolle = Aktoersroller.ARBEIDSGIVER;
                break;
            case UTENLANDSK_TRYGDEMYNDIGHET:
                aktørrolle = Aktoersroller.MYNDIGHET;
                break;
            default:
                throw new FunksjonellException("Kan ikke legge til aktør for avsenderType " + avsenderType);
        }

        fagsakService.leggTilAktør(saksnummer, aktørrolle, avsenderID);
    }
}
