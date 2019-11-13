package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpostUtils;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettBrevOgJournalpost extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettBrevOgJournalpost.class);

    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    public OpprettBrevOgJournalpost(EessiService eessiService, JoarkFasade joarkFasade, TpsFasade tpsFasade, UtenlandskMyndighetService utenlandskMyndighetService) {
        this.eessiService = eessiService;
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = joarkFasade.opprettJournalpost(lagJournalpost(prosessinstans), true);

        log.info("Opprettet journalpost {} for behandling {}", journalpostId, prosessinstans.getBehandling().getId());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST);
    }

    private OpprettJournalpost lagJournalpost(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();

        String brukerFnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(behandling.getId());
        byte[] sedPdf = eessiService.genererSedForhåndsvisning(behandling.getId(), sedType);

        Landkoder landkode = fagsak.hentMyndighetLandkode();
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
        String institusjonsId = utenlandskMyndighetService.lagInstitusjonsId(landkode);

        return OpprettJournalpostUtils.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), brukerFnr, sedType, sedPdf, institusjonsId, utenlandskMyndighet.navn, landkode.getKode(), Collections.emptyList()
        );
    }
}
