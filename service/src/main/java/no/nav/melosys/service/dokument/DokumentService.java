package no.nav.melosys.service.dokument;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;

@Service
@Primary
public class DokumentService {

    private static final Set<Produserbaredokumenter> DOKUMENTER_TIL_BRUKER = Collections.unmodifiableSet(EnumSet.of(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK, MELDING_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK, INNVILGELSE_YRKESAKTIV));

    private final BehandlingRepository behandlingRepository;

    private final FagsakRepository fagsakRepository;

    private final BrevDataService brevDataService;

    private final DoksysFasade dokSysFasade;

    private final JoarkFasade joarkFasade;

    private final KontaktopplysningService kontaktopplysningService;

    private final ProsessinstansService prosessinstansService;

    private final BrevDataByggerVelger brevDataByggerVelger;

    @Autowired
    public DokumentService(BehandlingRepository behandlingRepository,
                           FagsakRepository fagsakRepository,
                           BrevDataService brevDataService,
                           DoksysFasade dokSysFasade, JoarkFasade joarkFasade,
                           KontaktopplysningService kontaktopplysningService,
                           ProsessinstansService prosessinstansService, BrevDataByggerVelger brevDataByggerVelger) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.brevDataService = brevDataService;
        this.joarkFasade = joarkFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.dokSysFasade = dokSysFasade;
        this.prosessinstansService = prosessinstansService;
        this.brevDataByggerVelger = brevDataByggerVelger;
    }

    /**
     * Henter et dokument fra Joark
     */
    public byte[] hentDokument(String journalpostID, String dokumentID) throws IkkeFunnetException, SikkerhetsbegrensningException {
        return joarkFasade.hentDokument(journalpostID, dokumentID);
    }

    /**
     * Henter dokumenter knyttet til en sak med et gitt saksnummer
     */
    public List<Journalpost> hentDokumenter(String saksnummer) throws IkkeFunnetException, IntegrasjonException, SikkerhetsbegrensningException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            throw new IkkeFunnetException("Fagsak med saksnummer " + saksnummer + " finnes ikke");
        }

        return joarkFasade.hentKjerneJournalpostListe(fagsak.getGsakSaksnummer());
    }

    /**
     * Kaller Doksys for å produsere et dokumentutkast
     */
    // Bruker Transactional for å støtte lazy loading gjennom Hibernate,
    // selv om dataene som hentes ut egentlig er read-only. Det ser ut til å
    // være påkrevd for Hibernate å finne en sesjon via Spring-transaksjonen
    // for å kunne laste lazy collections i objektgrafen.
    @Transactional
    public byte[] produserUtkast(long behandlingID, Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto)
        throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        BrevDataBygger bygger = brevDataByggerVelger.hent(produserbartDokument, brevbestillingDto);
        BrevData brevData = bygger.lag(behandling, SubjectHandler.getInstance().getUserID());

        Aktoersroller mottakerRolle = brevData.mottakerRolle != null ? brevData.mottakerRolle : avklarMottakerRolleFraProduserbartDokument(produserbartDokument);
        Aktoer mottaker = behandling.getFagsak().hentAktørMedRolleType(mottakerRolle);

        return dokSysFasade.produserDokumentutkast(lagDokumentbestilling(produserbartDokument, mottaker, behandling, brevData));
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, Produserbaredokumenter produserbartDokument, BrevData brevData)
        throws TekniskException, FunksjonellException {
        Assert.notNull(produserbartDokument, "Ingen gyldig produserbartDokument");
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke"));

        Aktoersroller mottakerRolle = brevData.mottakerRolle != null ? brevData.mottakerRolle : avklarMottakerRolleFraProduserbartDokument(produserbartDokument);
        Fagsak fagsak = behandling.getFagsak();
        if (mottakerRolle == BRUKER) {
            // Dokumenter sendes til både bruker og representant
            Aktoer bruker = fagsak.hentAktørMedRolleType(BRUKER);
            produserIkkeredigerbartDokument(produserbartDokument, bruker, behandling, brevData);

            Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
            if (representant.isPresent()) {
                produserIkkeredigerbartDokument(produserbartDokument, representant.get(), behandling, brevData);
            }
        } else if (mottakerRolle == ARBEIDSGIVER) {
            Aktoer arbeidsgiver = fagsak.hentAktørMedRolleType(ARBEIDSGIVER);
            Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.ARBEIDSGIVER);
            if (representant.isPresent()) {
                produserIkkeredigerbartDokument(produserbartDokument, representant.get(), behandling, brevData);
            } else {
                produserIkkeredigerbartDokument(produserbartDokument, arbeidsgiver, behandling, brevData);
            }
        } else {
            Aktoer mottaker = fagsak.hentAktørMedRolleType(mottakerRolle);
            produserIkkeredigerbartDokument(produserbartDokument, mottaker, behandling, brevData);
        }
    }

    private Aktoersroller avklarMottakerRolleFraProduserbartDokument(Produserbaredokumenter produserbartDokument) throws TekniskException {
        Aktoersroller mottakerRolle;
        if (DOKUMENTER_TIL_BRUKER.contains(produserbartDokument)) {
            mottakerRolle = BRUKER;
        } else if (produserbartDokument == INNVILGELSE_ARBEIDSGIVER || produserbartDokument == AVSLAG_ARBEIDSGIVER) {
            mottakerRolle = ARBEIDSGIVER;
        } else if (produserbartDokument == ANMODNING_UNNTAK || produserbartDokument == ATTEST_A1) {
            mottakerRolle = MYNDIGHET;
        } else {
            throw new TekniskException("Valg av mottakerRolle støtter ikke " + produserbartDokument);
        }
        return mottakerRolle;
    }

    private Kontaktopplysning hentKontaktopplysning(String saksnumner, Aktoer mottaker) {
        Aktoersroller mottakerRolle = mottaker.getRolle();

        if (mottakerRolle == ARBEIDSGIVER || mottakerRolle == REPRESENTANT) {
            return kontaktopplysningService.hentKontaktopplysning(saksnumner, mottaker.getOrgnr()).orElse(null);
        } else {
            return null;
        }
    }

    private Dokumentbestilling lagDokumentbestilling(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData) throws FunksjonellException, TekniskException {
        Kontaktopplysning kontaktopplysning = hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker);
        DokumentbestillingMetadata metadata = brevDataService.lagBestillingMetadata(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        Object brevinnhold = brevDataService.lagBrevXML(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        return new Dokumentbestilling(metadata, brevinnhold);
    }

    private void produserIkkeredigerbartDokument(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData)
        throws FunksjonellException, TekniskException {
        dokSysFasade.produserIkkeredigerbartDokument(lagDokumentbestilling(produserbartDokument, mottaker, behandling, brevData));
    }

    @Transactional
    public void produserDokumentISaksflyt(long behandlingID, Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto) throws FunksjonellException {
        Assert.notNull(brevbestillingDto, "BrevbestillingDto brukes til å bestille brev i saksflyt.");
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke."));

        if (produserbartDokument == MELDING_MANGLENDE_OPPLYSNINGER) {
            prosessinstansService.opprettProsessinstansMangelbrev(behandling, new BrevData(brevbestillingDto));
        } else {
            throw new FunksjonellException("Produserbaredokumenter " + produserbartDokument + " er ikke støttet.");
        }
    }
}
