package no.nav.melosys.saksflyt.steg.aou.mottak;

import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
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

@Component("AnmodningUnntakMottakOpprettFagsakOgBehandling")
public class OpprettFagsakOgBehandling extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final FagsakOgBehandlingFelles opprettFagsakOgBehandlingFelles;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService, FagsakOgBehandlingFelles opprettFagsakOgBehandlingFelles) {
        this.fagsakService = fagsakService;
        this.opprettFagsakOgBehandlingFelles = opprettFagsakOgBehandlingFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_FAGSAK_OG_BEH;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        //Verifiser prosessType
        if (prosessinstans.getType() != ProsessType.ANMODNING_OM_UNNTAK_MOTTAK) {
            throw new TekniskException("Prosessinstans er ikke av type " + ProsessType.ANMODNING_OM_UNNTAK_MOTTAK);
        }

        Fagsak fagsak;
        Behandling behandling;
        Long gsakSaksnummer = prosessinstans.getData(GSAK_SAK_ID, Long.class);
        Optional<Fagsak> eksisterendeFagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer);

        if (eksisterendeFagsak.isPresent()) {
            fagsak = eksisterendeFagsak.get();
            behandling = opprettFagsakOgBehandlingFelles.opprettBehandlingPåEksisterendeFagsak(fagsak, Behandlingsstatus.UNDER_BEHANDLING,
                Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL, prosessinstans.getData(JOURNALPOST_ID), prosessinstans.getData(DOKUMENT_ID), gsakSaksnummer);
        } else {
            fagsak = opprettFagsakOgBehandlingFelles.opprettFagsakOgBehandling(prosessinstans.getData(AKTØR_ID), prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class),
                prosessinstans.getData(JOURNALPOST_ID), prosessinstans.getData(DOKUMENT_ID), gsakSaksnummer, Sakstyper.EU_EOS);
            behandling = fagsak.getAktivBehandling();
        }

        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());
        prosessinstans.setBehandling(behandling);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE);
    }
}
