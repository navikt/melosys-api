package no.nav.melosys.service.dokument;

import java.time.LocalDateTime;
import java.util.List;
import javax.transaction.Transactional;

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
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

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

    @Autowired
    DokumentService(BehandlingRepository behandlingRepository,
                    FagsakRepository fagsakRepository,
                    BrevDataService brevDataService,
                    DokSysFasade dokSysFasade, JoarkFasade joarkFasade,
                    Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.brevDataService = brevDataService;
        this.joarkFasade = joarkFasade;
        this.dokSysFasade = dokSysFasade;
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    /**
     * Henter et dokument fra Joark
     * @throws IkkeFunnetException 
     */
    public byte[] hentDokument(String journalpostID, String dokumentID) throws SikkerhetsbegrensningException, IntegrasjonException, IkkeFunnetException {
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
     * @throws TekniskException 
     */
    public byte[] produserUtkast(long behandlingID, Dokumenttype dokumenttype, BrevDataDto brevDataDto)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        return produserDokument(behandlingID, dokumenttype, brevDataDto, true);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, Dokumenttype dokumenttype, BrevDataDto brevDataDto)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        produserDokument(behandlingID, dokumenttype, brevDataDto, false);
    }

    private byte[] produserDokument(long behandlingID, Dokumenttype dokumenttype, BrevDataDto brevDataDto, boolean erUtkast)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        if (dokumenttype == null) {
            throw new TekniskException("Ingen gyldig dokumenttype");
        }

        DokumentType dokumentType;
        try {
            dokumentType = DokumentType.valueOf(dokumenttype.name());
        } catch (IllegalArgumentException e) {
            throw new TekniskException("Fant ikke dokumenttypeId for dokumenttype " + dokumenttype);
        }

        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(dokumentType, behandling, brevDataDto);
        Object brevData = brevDataService.lagBrevXML(dokumentType, behandling, brevDataDto);

        if (erUtkast) {
            return dokSysFasade.produserDokumentutkast(request, brevData);
        } else {
            dokSysFasade.produserIkkeredigerbartDokument(request, brevData);
            return null;
        }
    }

    @Transactional
    public void produserDokumentISaksflyt(long behandlingID, Dokumenttype dokumenttype, BrevDataDto brevDataDto) throws FunksjonellException {
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

        if (brevDataDto != null) {
            prosessinstans.setData(ProsessDataKey.BREVDATA, brevDataDto);
        }
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
