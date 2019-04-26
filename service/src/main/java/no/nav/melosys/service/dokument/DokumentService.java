package no.nav.melosys.service.dokument;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.aktoer.AvklarMyndighetService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
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
import org.w3c.dom.Element;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;

@Service
@Primary
public class DokumentService {

    private static final Set<Produserbaredokumenter> DOKUMENTER_TIL_BRUKER = Collections.unmodifiableSet(EnumSet.of(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK, MELDING_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK, INNVILGELSE_YRKESAKTIV));

    private static final String BEHANDLING_ID = "Behandling med ID ";
    private static final String FINNES_IKKE = " finnes ikke.";

    private final BehandlingRepository behandlingRepository;
    private final BrevDataService brevDataService;
    private final DoksysFasade dokSysFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final ProsessinstansService prosessinstansService;
    private final BrevDataByggerVelger brevDataByggerVelger;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final AvklarMyndighetService avklarMyndighetService;

    @Autowired
    public DokumentService(BehandlingRepository behandlingRepository,
                           BrevDataService brevDataService, DoksysFasade dokSysFasade,
                           KontaktopplysningService kontaktopplysningService,
                           ProsessinstansService prosessinstansService, BrevDataByggerVelger brevDataByggerVelger,
                           AvklarteVirksomheterService avklarteVirksomheterService, AvklarMyndighetService avklarMyndighetService) {
        this.behandlingRepository = behandlingRepository;
        this.brevDataService = brevDataService;
        this.dokSysFasade = dokSysFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.prosessinstansService = prosessinstansService;
        this.brevDataByggerVelger = brevDataByggerVelger;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarMyndighetService = avklarMyndighetService;
    }

    /**
     * Kaller Doksys for å produsere et dokumentutkast
     */
    // Bruker Transactional for å støtte lazy loading gjennom Hibernate,
    // selv om dataene som hentes ut egentlig er read-only. Det ser ut til å
    // være påkrevd for Hibernate å finne en sesjon via Spring-transaksjonen
    // for å kunne laste lazy collections i objektgrafen.
    @Transactional(readOnly = true)
    public byte[] produserUtkast(long behandlingID, Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto)
        throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException(BEHANDLING_ID + behandlingID + FINNES_IKKE);
        }

        BrevDataBygger bygger = brevDataByggerVelger.hent(produserbartDokument, brevbestillingDto);
        BrevData brevData = bygger.lag(behandling, SubjectHandler.getInstance().getUserID());

        Aktoersroller mottakerRolle = avklarMottakerRolleFraDokument(produserbartDokument);
        Aktoer mottaker = behandling.getFagsak().hentAktørMedRolleType(mottakerRolle);
        // Myndighet avklares ikke endelig før i Saksflyt
        if (mottaker == null && MYNDIGHET.equals(mottakerRolle)) {
            mottaker = avklarMyndighetService.hentMyndighetFraBehandling(behandling);
        }

        return dokSysFasade.produserDokumentutkast(lagDokumentbestilling(produserbartDokument, mottaker, behandling, brevData));
    }

    private Aktoersroller avklarMottakerRolleFraDokument(Produserbaredokumenter produserbartDokument) throws TekniskException {
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

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(Produserbaredokumenter produserbartDokument, Mottaker mottaker, long behandlingID, BrevData brevData)
        throws TekniskException, FunksjonellException {
        Assert.notNull(produserbartDokument, "Ingen gyldig produserbartDokument.");
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException(BEHANDLING_ID + behandlingID + FINNES_IKKE));

        Aktoersroller mottakerRolle = mottaker.getRolle();

        if (mottakerRolle == BRUKER) {
            sendTilBruker(produserbartDokument, behandling, brevData);
        } else if (mottakerRolle == ARBEIDSGIVER) {
            sendTilArbeidsgiver(produserbartDokument, behandling, brevData);
        } else if (mottakerRolle == MYNDIGHET) {
            sendTilMyndighet(produserbartDokument, mottaker, behandling, brevData);
        } else {
            throw new FunksjonellException(mottakerRolle + " støttes ikke.");
        }
    }

    // Dokumenter til bruker sendes til både bruker og representant.
    private void sendTilBruker(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws FunksjonellException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentAktørMedRolleType(BRUKER);
        if (bruker == null) {
            throw new FunksjonellException("Bruker er ikke registrert.");
        }
        produserIkkeredigerbartDokument(produserbartDokument, bruker, behandling, brevData);

        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
        if (representant.isPresent()) {
            produserIkkeredigerbartDokument(produserbartDokument, representant.get(), behandling, brevData);
        }
    }

    // Dokumenter til arbeidsgiver sendes bare til representant når representant finnes.
    private void sendTilArbeidsgiver(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws FunksjonellException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.ARBEIDSGIVER);
        if (representant.isPresent()) {
            produserIkkeredigerbartDokument(produserbartDokument, representant.get(), behandling, brevData);
        } else {
            produserIkkeredigerbartDokument(produserbartDokument, avklarArbeidsgiver(behandling), behandling, brevData);
        }
    }

    private Aktoer avklarArbeidsgiver(Behandling behandling) throws FunksjonellException, TekniskException {
        Aktoer arbeidsgiver = behandling.getFagsak().hentAktørMedRolleType(ARBEIDSGIVER);
        if (arbeidsgiver == null) {
            Set<String> arbeidsgivendeOrgnumre = avklarteVirksomheterService.hentArbeidsgivendeOrgnumre(behandling);
            if (arbeidsgivendeOrgnumre.isEmpty()) {
                throw new FunksjonellException("Arbeidsgiver er ikke registrert.");
            } else if (arbeidsgivendeOrgnumre.size() > 1) {
                throw new FunksjonellException("Flere arbeidsgivere er avklart.");
            } else {
                String orgnr = arbeidsgivendeOrgnumre.iterator().next();
                Aktoer avklartArbeidsgiver = new Aktoer();
                avklartArbeidsgiver.setRolle(ARBEIDSGIVER);
                avklartArbeidsgiver.setOrgnr(orgnr);
                return avklartArbeidsgiver;
            }
        }
        return arbeidsgiver;
    }

    private void sendTilMyndighet(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, BrevData brevData)
        throws FunksjonellException, TekniskException {
        Aktoer myndighet;
        if (mottaker.getAktør().getOrgnr() != null) {
            // Norsk myndighet har orgnummer.
            myndighet = mottaker.getAktør();
        } else {
            // Utenlandsk myndighet.
            myndighet = behandling.getFagsak().hentAktørMedRolleType(mottaker.getRolle());
        }
        produserIkkeredigerbartDokument(produserbartDokument, myndighet, behandling, brevData);
    }

    private Kontaktopplysning hentKontaktopplysning(String saksnumner, Aktoer mottaker) {
        if (mottaker == null) {
            return null;
        }

        Aktoersroller mottakerRolle = mottaker.getRolle();

        if (mottakerRolle == ARBEIDSGIVER || mottakerRolle == REPRESENTANT) {
            return kontaktopplysningService.hentKontaktopplysning(saksnumner, mottaker.getOrgnr()).orElse(null);
        } else {
            return null;
        }
    }

    private Dokumentbestilling lagDokumentbestilling(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData)
        throws FunksjonellException, TekniskException {
        Kontaktopplysning kontaktopplysning = hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker);
        DokumentbestillingMetadata metadata = brevDataService.lagBestillingMetadata(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        Element brevinnhold = brevDataService.lagBrevXML(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        return new Dokumentbestilling(metadata, brevinnhold);
    }

    private void produserIkkeredigerbartDokument(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData)
        throws FunksjonellException, TekniskException {
        dokSysFasade.produserIkkeredigerbartDokument(lagDokumentbestilling(produserbartDokument, mottaker, behandling, brevData));
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void produserDokumentISaksflyt(Produserbaredokumenter produserbartDokument, Aktoersroller mottaker, long behandlingID, BrevData brevdata)
        throws FunksjonellException {
        Assert.notNull(mottaker, "Dokument uten mottaker.");
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException(BEHANDLING_ID + behandlingID + FINNES_IKKE));

        if (produserbartDokument == MELDING_MANGLENDE_OPPLYSNINGER) {
            prosessinstansService.opprettProsessinstansMangelbrev(behandling, mottaker, brevdata);
        } else {
            throw new FunksjonellException("Produserbaredokumenter " + produserbartDokument + " er ikke støttet.");
        }
    }
}
