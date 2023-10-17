package no.nav.melosys.saksflyt.steg.behandling;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.Representant;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.FullmektigDto;
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
        var fullmektig = prosessinstans.getData(FULLMEKTIG);
        List<Fullmaktstype> fullmakter = prosessinstans.getData(FULLMAKTER, new TypeReference<>() {
        }, Collections.emptyList());
        var fullmektigKontaktperson = prosessinstans.getData(FULLMEKTIG_KONTAKTPERSON);
        var fullmektigKontaktOrgnr = prosessinstans.getData(FULLMEKTIG_KONTAKT_ORGNR);
        String initierendeJournalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String initierendeDokumentId = prosessinstans.getData(DOKUMENT_ID);
        LocalDate mottaksdato = prosessinstans.getData(MOTTATT_DATO, LocalDate.class);
        Behandlingsaarsaktyper behandlingsårsaktype = prosessinstans.getData(BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.class);
        String behandlingsårsakFritekst = prosessinstans.getData(BEHANDLINGSÅRSAK_FRITEKST);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);
        Sakstyper sakstype = prosessinstans.getData(SAKSTYPE, Sakstyper.class);
        Sakstemaer sakstema = prosessinstans.getData(SAKSTEMA, Sakstemaer.class);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(aktørID)
            .medVirksomhetOrgnr(virksomhetOrgnr)
            .medArbeidsgiver(arbeidsgiver)
            .medRepresentant(representant != null ? new Representant(representant, representantRepresenterer) : null)
            .medFullmektig(lagFullmektig(fullmektig, fullmakter))
            .medKontaktopplysninger(lagKontaktopplysning(representant, representantKontakperson, fullmektig, fullmektigKontaktperson, fullmektigKontaktOrgnr))
            .medSakstype(sakstype)
            .medSakstema(sakstema)
            .medBehandlingsårsaktype(behandlingsårsaktype)
            .medBehandlingsårsakFritekst(behandlingsårsakFritekst)
            .medBehandlingstema(behandlingstema)
            .medBehandlingstype(behandlingstype)
            .medMottaksdato(mottaksdato)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .build();
        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        Behandling behandling = fagsak.hentAktivBehandling();
        prosessinstans.setBehandling(behandling);
        log.info("Opprettet fagsak {} med behandling {}", fagsak.getSaksnummer(), behandling.getId());
    }

    private FullmektigDto lagFullmektig(String fullmektig, List<Fullmaktstype> fullmakter) {
        if (fullmektig == null) {
            return null;
        }
        if (erIDPersonident(fullmektig)) {
            return new FullmektigDto(null, fullmektig, fullmakter);
        } else {
            return new FullmektigDto(fullmektig, null, fullmakter);
        }
    }

    private boolean erIDPersonident(String id) {
        return id.length() == 11;
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

    private List<Kontaktopplysning> lagKontaktopplysning(String representant, String kontaktperson, String fullmektig, String fullmektigKontaktperson, String fullmektigKontaktOrgnr) {
        if (representant != null) {
            return kontaktperson != null
                ? Collections.singletonList(Kontaktopplysning.av(representant, kontaktperson, null, null))
                : Collections.emptyList();
        }
        if (fullmektig != null) {
            return fullmektigKontaktperson != null || fullmektigKontaktOrgnr != null
                ? Collections.singletonList(Kontaktopplysning.av(fullmektig, fullmektigKontaktperson, null, fullmektigKontaktOrgnr))
                : Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
