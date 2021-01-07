package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class DistribuerJournalpost implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpost.class);

    private final DoksysFasade doksysFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final BehandlingService behandlingService;

    public DistribuerJournalpost(DoksysFasade doksysFasade, EregFasade eregFasade,
                                 KontaktopplysningService kontaktopplysningService, BehandlingService behandlingService) {
        this.doksysFasade = doksysFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.DISTRIBUER_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        if (prosessinstans.getBehandling() == null) {
            throw new FunksjonellException("Prosessinstans mangler behandling");
        }
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String journalpostId = prosessinstans.getData(DISTRIBUERBAR_JOURNALPOST_ID);
        Aktoersroller mottaker = prosessinstans.getData(MOTTAKER, Aktoersroller.class);
        String orgnr = prosessinstans.getData(ORGNR, String.class, null);

        if (isEmpty(journalpostId)) {
            throw new FunksjonellException("JournalpostId mangler, kan ikke distribuere");
        }

        if (mottaker == null) {
            throw new FunksjonellException("Prosessinstans mangler mottaker");
        }

        OrganisasjonDokument org = null;
        Kontaktopplysning kontaktopplysning = null;

        if (!Aktoersroller.BRUKER.equals(mottaker)) {
            org = (OrganisasjonDokument) eregFasade.hentOrganisasjon(orgnr).getDokument();
            kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        }

        String bestillingsId;
        if (org != null) {
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, org.getPostadresse(), kontaktopplysning);
        } else {
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId);
        }
        log.info("Distribuering av journalpostId {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
    }
}
