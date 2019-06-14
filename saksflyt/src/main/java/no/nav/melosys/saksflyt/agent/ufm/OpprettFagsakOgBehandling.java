package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingService;
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
    private final BehandlingService behandlingService;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService, BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        //Verifiser prosessType
        if (prosessinstans.getType() != ProsessType.REGISTRERING_UNNTAK) {
            throw new TekniskException("Prosessinstans er ikke av type " + ProsessType.REGISTRERING_UNNTAK);
        }

        Long gsakSaksnummer = prosessinstans.getData(GSAK_SAK_ID, Long.class);
        Fagsak fagsak;
        Behandling behandling;

        Optional<Fagsak> eksisterendeFagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer);

        if (eksisterendeFagsak.isPresent()) {
            fagsak = eksisterendeFagsak.get();

            if (!Saksstatuser.OPPRETTET.equals(fagsak.getStatus())) {
                fagsak.setStatus(Saksstatuser.OPPRETTET);
            }

            avsluttTidligereBehandling(fagsak);
            behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP,
                prosessinstans.getData(JOURNALPOST_ID), prosessinstans.getData(DOKUMENT_ID));
            log.info("Opprettet ny behandling for fagsak {}", gsakSaksnummer);
        } else {

            OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
                .medAktørID(prosessinstans.getData(AKTØR_ID))
                .medBehandlingstype(Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP)
                .medInitierendeJournalpostId(prosessinstans.getData(JOURNALPOST_ID))
                .medInitierendeDokumentId(prosessinstans.getData(DOKUMENT_ID))
                .medGsakSaksnummer(gsakSaksnummer)
                .medSakstype(Sakstyper.EU_EOS)
                .build();

            fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
            behandling = fagsak.getAktivBehandling();
            log.info("Fagsak og behandling opprettet for EESSI-sak med gsakSaksnummer {}", gsakSaksnummer);
        }

        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setBehandling(behandling);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET);
    }

    private void avsluttTidligereBehandling(Fagsak fagsak) throws TekniskException, IkkeFunnetException {
        Behandling aktivBehandling = fagsak.getAktivBehandling();

        if (aktivBehandling != null) {
            behandlingService.avsluttBehandling(aktivBehandling.getId());
        }
    }
}
