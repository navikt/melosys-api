package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Component
public class DistribuerJournalpost implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpost.class);

    private final DoksysFasade doksysFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final BehandlingService behandlingService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final KodeverkService kodeverkService;

    public DistribuerJournalpost(@Qualifier("system") DoksysFasade doksysFasade,
                                 @Qualifier("system") EregFasade eregFasade,
                                 KontaktopplysningService kontaktopplysningService,
                                 BehandlingService behandlingService,
                                 UtenlandskMyndighetService utenlandskMyndighetService,
                                 KodeverkService kodeverkService) {
        this.doksysFasade = doksysFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.behandlingService = behandlingService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.kodeverkService = kodeverkService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.DISTRIBUER_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        if (prosessinstans.getBehandling() == null) {
            throw new FunksjonellException("Prosessinstans mangler behandling");
        }
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String journalpostId = prosessinstans.getData(DISTRIBUERBAR_JOURNALPOST_ID);
        var brevbestilling = prosessinstans.getData(BREVBESTILLING, DokgenBrevbestilling.class);
        Aktoersroller mottaker = prosessinstans.getData(MOTTAKER, Aktoersroller.class);
        String orgnr = prosessinstans.getData(ORGNR);
        String institusjonId = prosessinstans.getData(INSTITUSJON_ID);

        if (isEmpty(journalpostId)) {
            throw new FunksjonellException("JournalpostId mangler, kan ikke distribuere");
        }

        if (mottaker == null) {
            throw new FunksjonellException("Prosessinstans mangler mottaker");
        }

        OrganisasjonDokument org = null;
        Kontaktopplysning kontaktopplysning = null;

        if (mottaker != Aktoersroller.BRUKER && !hasText(institusjonId)) {
            kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
            String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
            org = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
        }

        String bestillingsId;
        if (org != null) {
            StrukturertAdresse orgAdresse = org.hentTilgjengeligAdresse();
            if (orgAdresse.erNorsk() && isEmpty(orgAdresse.getPoststed())) {
                orgAdresse.setPoststed(
                    kodeverkService.dekod(FellesKodeverk.POSTNUMMER, orgAdresse.getPostnummer()));
            }
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, orgAdresse, kontaktopplysning, brevbestilling.getKontaktpersonNavn());
        } else if (hasText(institusjonId)) {
            var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID(institusjonId);
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, utenlandskMyndighet.getAdresse());
        } else {
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId);
        }
        log.info("Distribuering av journalpostId {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
    }
}
