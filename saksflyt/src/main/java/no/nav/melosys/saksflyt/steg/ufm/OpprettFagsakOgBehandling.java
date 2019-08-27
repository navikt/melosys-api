package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.felles.FagsakOgBehandlingFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;

@Component("RegistreringUnntakOpprettFagsakOgBehandling")
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final FagsakOgBehandlingFelles fagsakOgBehandlingFelles;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService, FagsakOgBehandlingFelles fagsakOgBehandlingFelles) {
        this.fagsakService = fagsakService;
        this.fagsakOgBehandlingFelles = fagsakOgBehandlingFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH;
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
        String journalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String dokumentId = prosessinstans.getData(DOKUMENT_ID);
        String aktørId = prosessinstans.getData(AKTØR_ID);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);

        if (eksisterendeFagsak.isPresent()) {
            fagsak = eksisterendeFagsak.get();
            behandling = fagsakOgBehandlingFelles.opprettBehandlingPåEksisterendeFagsak(fagsak, Behandlingsstatus.UNDER_BEHANDLING,
                Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD, journalpostId, dokumentId, gsakSaksnummer);
        } else {
            fagsak = fagsakOgBehandlingFelles.opprettFagsakOgBehandling(aktørId, behandlingstype, journalpostId,
                dokumentId, gsakSaksnummer, Sakstyper.EU_EOS);
            behandling = fagsak.getAktivBehandling();
        }

        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setBehandling(behandling);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET);
    }
}
