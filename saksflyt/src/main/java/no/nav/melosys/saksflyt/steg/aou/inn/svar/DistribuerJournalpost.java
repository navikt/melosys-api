package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DistribuerJournalpost extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpost.class);

    private final DoksysFasade doksysFasade;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Autowired
    public DistribuerJournalpost(DoksysFasade doksysFasade, UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.doksysFasade = doksysFasade;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        String bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, lagAdresseUtenlandskMyndighet(fagsak));

        log.info("Distribuering av journalpost {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }

    private StrukturertAdresse lagAdresseUtenlandskMyndighet(Fagsak fagsak) throws TekniskException {
        Landkoder landkode = fagsak.hentMyndighetLandkode();
        Optional<UtenlandskMyndighet> utenlandskMyndighet = utenlandskMyndighetRepository.findByLandkode(landkode);

        return utenlandskMyndighet.map(DistribuerJournalpost::tilStrukturertAdresse)
            .orElseThrow(() -> new TekniskException("Finner ingen myndighet for fagsak " + fagsak.getSaksnummer()));
    }

    private static StrukturertAdresse tilStrukturertAdresse(UtenlandskMyndighet utenlandskMyndighet) {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.poststed = utenlandskMyndighet.poststed;
        strukturertAdresse.postnummer = utenlandskMyndighet.postnummer;
        strukturertAdresse.gatenavn = utenlandskMyndighet.gateadresse;
        strukturertAdresse.landkode = utenlandskMyndighet.landkode.getKode();
        return strukturertAdresse;
    }
}
