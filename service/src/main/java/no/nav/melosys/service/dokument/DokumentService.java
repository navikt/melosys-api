package no.nav.melosys.service.dokument;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
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

@Service
@Primary
public class DokumentService {

    private final BehandlingRepository behandlingRepository;

    private final FagsakRepository fagsakRepository;

    private final BrevDataService brevDataService;

    private final DoksysFasade dokSysFasade;

    private final JoarkFasade joarkFasade;

    private final ProsessinstansService prosessinstansService;

    private final BrevDataByggerVelger brevDataByggerVelger;

    @Autowired
    public DokumentService(BehandlingRepository behandlingRepository,
                           FagsakRepository fagsakRepository,
                           BrevDataService brevDataService,
                           DoksysFasade dokSysFasade, JoarkFasade joarkFasade,
                           ProsessinstansService prosessinstansService, BrevDataByggerVelger brevDataByggerVelger) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.brevDataService = brevDataService;
        this.joarkFasade = joarkFasade;
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

        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(produserbartDokument, behandling, brevData);
        Object brevinnhold = brevDataService.lagBrevXML(produserbartDokument, behandling, brevData);

        return dokSysFasade.produserDokumentutkast(request, brevinnhold);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, Produserbaredokumenter produserbartDokument, BrevData brevData)
        throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke"));

        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(produserbartDokument, behandling, brevData);
        Object brevinnhold = brevDataService.lagBrevXML(produserbartDokument, behandling, brevData);

        dokSysFasade.produserIkkeredigerbartDokument(request, brevinnhold);
    }

    @Transactional
    public void produserDokumentISaksflyt(long behandlingID, Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto) throws FunksjonellException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke"));

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        switch (produserbartDokument) {
            case MELDING_MANGLENDE_OPPLYSNINGER:
                prosessinstans.setType(ProsessType.MANGELBREV);
                prosessinstans.setSteg(ProsessSteg.MANGELBREV);
                break;
            default:
                throw new FunksjonellException("Produserbaredokumenter " + produserbartDokument + " er ikke støttet.");
        }

        if (brevbestillingDto != null) {
            BrevData brevData = new BrevData(brevbestillingDto);
            prosessinstans.setData(ProsessDataKey.BREVDATA, brevData);
        }

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstansService.lagre(prosessinstans);
    }
}
