package no.nav.melosys.service.dokument;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.dokument.brev.*;
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

    private final DokSysFasade dokSysFasade;

    private final JoarkFasade joarkFasade;

    private final Binge binge;

    private final ProsessinstansRepository prosessinstansRepo;

    private final BrevDataByggerVelger brevDataByggerVelger;

    @Autowired
    public DokumentService(BehandlingRepository behandlingRepository,
                    FagsakRepository fagsakRepository,
                    BrevDataService brevDataService,
                    DokSysFasade dokSysFasade, JoarkFasade joarkFasade,
                    Binge binge, ProsessinstansRepository prosessinstansRepo,
                    BrevDataByggerVelger brevDataByggerVelger) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.brevDataService = brevDataService;
        this.joarkFasade = joarkFasade;
        this.dokSysFasade = dokSysFasade;
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
        this.brevDataByggerVelger = brevDataByggerVelger;
    }

    /**
     * Henter et dokument fra Joark
     * 
     * @throws IkkeFunnetException
     * @throws SikkerhetsbegrensningException
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
     * 
     * @throws TekniskException
     */
    // Bruker Transactional for å støtte lazy loading gjennom Hibernate,
    // selv om dataene som hentes ut egentlig er read-only. Det ser ut til å
    // være påkrevd for Hibernate å finne en sesjon via Spring-transaksjonen
    // for å kunne laste lazy collections i objektgrafen.
    @Transactional
    public byte[] produserUtkast(long behandlingID, Dokumenttype dokumenttype, BrevbestillingDto brevbestillingDto)
            throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findOneWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        DokumentType dokumentType = lagDokumentType(dokumenttype);
        BrevDataBygger bygger = brevDataByggerVelger.hent(dokumentType, brevbestillingDto);
        BrevData brevData = bygger.lag(behandling, SubjectHandler.getInstance().getUserID());

        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(dokumentType, behandling, brevData);
        Object brevinnhold = brevDataService.lagBrevXML(dokumentType, behandling, brevData);

        return dokSysFasade.produserDokumentutkast(request, brevinnhold);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, Dokumenttype dokumenttype, BrevData brevData)
        throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        DokumentType dokumentType = lagDokumentType(dokumenttype);
        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(dokumentType, behandling, brevData);
        Object brevinnhold = brevDataService.lagBrevXML(dokumentType, behandling, brevData);

        dokSysFasade.produserIkkeredigerbartDokument(request, brevinnhold);
    }

    private DokumentType lagDokumentType(Dokumenttype dokumenttype) throws TekniskException {
        if (dokumenttype == null) {
            throw new TekniskException("Ingen gyldig dokumenttype");
        }

        // NB: Kan ved første øyekast se ut som meningsløs kode, men er en
        // mapping til en enum i melosys-service med samme navn.
        // FIXME: Dette er vel symptom på uønsket kodeduplisering? Service-laget
        // kan vel koples til domene-laget og gjenbruke typen derfra?
        DokumentType dokumentType;
        try {
            dokumentType = DokumentType.valueOf(dokumenttype.name());
        } catch (IllegalArgumentException e) {
            throw new TekniskException("Fant ikke dokumenttypeId for dokumenttype " + dokumenttype);
        }
        return dokumentType;
    }

    @Transactional
    public void produserDokumentISaksflyt(long behandlingID, Dokumenttype dokumenttype, BrevbestillingDto brevbestillingDto) throws FunksjonellException {
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        switch (dokumenttype) {
            case MELDING_MANGLENDE_OPPLYSNINGER:
                prosessinstans.setType(ProsessType.MANGELBREV);
                prosessinstans.setSteg(ProsessSteg.MANGELBREV);
                break;
            default:
                throw new FunksjonellException("Dokumenttype " + dokumenttype + " er ikke støttet.");
        }

        if (brevbestillingDto != null) {
            BrevData brevData = new BrevData(brevbestillingDto);
            prosessinstans.setData(ProsessDataKey.BREVDATA, brevData);
        }

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
