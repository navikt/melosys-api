package no.nav.melosys.saksflyt.steg.sed.jfr;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;

@Component("RegistreringUnntakOpprettFagsakOgBehandling")
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(EESSI_MELDING, MelosysEessiMelding.class);

        //Verifiser prosessType
        if (prosessinstans.getType() == ProsessType.MOTTAK_SED || prosessinstans.getType() == ProsessType.MOTTAK_SED_JOURNALFØRING) {
            throw new TekniskException("Prosessinstans er av type " + prosessinstans.getType());
        }

        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(prosessinstans.getData(AKTØR_ID))
            .medBehandlingstype(behandlingstype)
            .medInitierendeJournalpostId(prosessinstans.getData(JOURNALPOST_ID))
            .medInitierendeDokumentId(prosessinstans.getData(DOKUMENT_ID))
            .medSakstype(Sakstyper.EU_EOS)
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());

        Behandling behandling = fagsak.getAktivBehandling();
        prosessinstans.setBehandling(behandling);

        log.info("Fagsak {} opprettet med behandling {} for RINA-sak {}",
            fagsak.getSaksnummer(), behandling.getId(), melosysEessiMelding.getRinaSaksnummer());

        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_SAK);
    }
}
