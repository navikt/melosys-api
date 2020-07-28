package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettBrevOgJournalpost implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettBrevOgJournalpost.class);

    private final EessiService eessiService;
    private final SedSomBrevService sedSomBrevService;

    @Autowired
    public OpprettBrevOgJournalpost(@Qualifier("system") EessiService eessiService,
                                    SedSomBrevService sedSomBrevService) {
        this.eessiService = eessiService;
        this.sedSomBrevService = sedSomBrevService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final Behandling behandling = prosessinstans.getBehandling();
        final SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(behandling.getId());
        String journalpostId = sedSomBrevService
            .lagJournalpostForSendingAvSedSomBrev(sedType, behandling.getFagsak().hentMyndighetLandkode(), behandling);

        log.info("Opprettet journalpost {} for behandling {}", journalpostId, prosessinstans.getBehandling().getId());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST);
    }
}
