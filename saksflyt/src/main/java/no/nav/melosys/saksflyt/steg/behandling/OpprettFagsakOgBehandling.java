package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fullmektig;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_SAK_OG_BEH;

@Component
public class OpprettFagsakOgBehandling implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final PersondataFasade persondataFasade;

    public OpprettFagsakOgBehandling(FagsakService fagsakService,
                                     PersondataFasade persondataFasade) {
        this.fagsakService = fagsakService;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_SAK_OG_BEH;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String aktørID = finnAktørID(prosessinstans).orElse(null);
        String virksomhetOrgnr = prosessinstans.getData(VIRKSOMHET_ORGNR);
        String arbeidsgiver = prosessinstans.getData(ARBEIDSGIVER);
        String representant = prosessinstans.getData(REPRESENTANT);
        String representantKontakperson = prosessinstans.getData(REPRESENTANT_KONTAKTPERSON);
        Representerer representantRepresenterer = prosessinstans.getData(REPRESENTANT_REPRESENTERER, Representerer.class);
        String initierendeJournalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String initierendeDokumentId = prosessinstans.getData(DOKUMENT_ID);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);
        Sakstyper sakstype = prosessinstans.getData(SAKSTYPE, Sakstyper.class);
        Sakstemaer sakstema = prosessinstans.getData(SAKSTEMA, Sakstemaer.class);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(aktørID)
            .medVirksomhetOrgnr(virksomhetOrgnr)
            .medArbeidsgiver(arbeidsgiver)
            .medFullmektig(representant != null ? new Fullmektig(representant, representantRepresenterer) : null)
            .medKontaktopplysninger(lagKontaktopplysningerForRepresentant(representant, representantKontakperson))
            .medSakstype(sakstype)
            .medSakstema(sakstema)
            .medBehandlingstema(behandlingstema)
            .medBehandlingstype(behandlingstype)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .build();
        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        Behandling behandling = fagsak.hentAktivBehandling();
        prosessinstans.setBehandling(behandling);
        log.info("Opprettet fagsak {} med behandling {}", fagsak.getSaksnummer(), behandling.getId());
    }

    private Optional<String> finnAktørID(Prosessinstans prosessinstans) {
        String aktørID = prosessinstans.getData(AKTØR_ID);
        if (StringUtils.isNotEmpty(aktørID)) {
            return Optional.of(aktørID);
        }

        String brukerID = prosessinstans.getData(BRUKER_ID);
        if (StringUtils.isNotEmpty(brukerID)) {
            return Optional.of(persondataFasade.hentAktørIdForIdent(brukerID));
        }

        return Optional.empty();
    }

    private List<Kontaktopplysning> lagKontaktopplysningerForRepresentant(String representant,
                                                                          String kontaktperson) {
        if (representant == null || kontaktperson == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(Kontaktopplysning.av(representant, kontaktperson, null));
    }
}
