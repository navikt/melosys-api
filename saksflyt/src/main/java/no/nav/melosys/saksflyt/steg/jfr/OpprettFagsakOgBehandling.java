package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fullmektig;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_OPPRETT_SAK_OG_BEH;

/**
 * Oppretter en sak og en behandling i Melosys.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_KNYTT:
 * JFR_OPPRETT_SAK_OG_BEH -> STATUS_BEH_OPPR eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_NY_SAK:
 * JFR_OPPRETT_SAK_OG_BEH -> JFR_OPPRETT_SOEKNAD eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettFagsakOgBehandling implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final TpsFasade tpsFasade;

    @Autowired
    public OpprettFagsakOgBehandling(FagsakService fagsakService,
                                     @Qualifier("system") TpsFasade tpsFasade) {
        this.fagsakService = fagsakService;
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SAK_OG_BEH;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        String aktørID = hentAktørID(prosessinstans);
        String arbeidsgiver = prosessinstans.getData(ARBEIDSGIVER);
        String representant = prosessinstans.getData(REPRESENTANT);
        String representantKontakperson = prosessinstans.getData(REPRESENTANT_KONTAKTPERSON);
        Representerer representantRepresenterer = prosessinstans.getData(REPRESENTANT_REPRESENTERER, Representerer.class);
        String initierendeJournalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String initierendeDokumentId = prosessinstans.getData(DOKUMENT_ID);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(aktørID)
            .medArbeidsgiver(arbeidsgiver)
            .medFullmektig(representant != null ? new Fullmektig(representant, representantRepresenterer) : null)
            .medKontaktopplysninger(lagKontaktopplysningerForRepresentant(representant, representantKontakperson))
            .medBehandlingstema(behandlingstema)
            .medBehandlingstype(behandlingstype)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .build();
        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        prosessinstans.setBehandling(fagsak.hentAktivBehandling());
        log.info("Opprettet fagsak {} for prosessinstans {}", fagsak.getSaksnummer(), prosessinstans.getId());
    }

    private String hentAktørID(Prosessinstans prosessinstans) throws IkkeFunnetException {
        String aktørID = prosessinstans.getData(AKTØR_ID);
        if (StringUtils.isNotEmpty(aktørID)) {
            return aktørID;
        }

        return tpsFasade.hentAktørIdForIdent(prosessinstans.getData(BRUKER_ID));
    }

    private List<Kontaktopplysning> lagKontaktopplysningerForRepresentant(String representant,
                                                                          String kontaktperson) {
        if (representant == null || kontaktperson == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(Kontaktopplysning.av(representant, kontaktperson));
    }
}
