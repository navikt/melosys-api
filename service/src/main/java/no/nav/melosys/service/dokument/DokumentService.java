package no.nav.melosys.service.dokument;

import java.time.LocalDateTime;
import javax.transaction.Transactional;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
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

    private final BrevDataService brevDataService;

    private final DokSysFasade dokSysFasade;

    private final JoarkFasade joarkFasade;

    private final Binge binge;

    private final ProsessinstansRepository prosessinstansRepo;

    @Autowired
    DokumentService(BehandlingRepository behandlingRepository, BrevDataService brevDataService,
                    DokSysFasade dokSysFasade, JoarkFasade joarkFasade,
                    Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.behandlingRepository = behandlingRepository;
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
     * Kaller Doksys for å produsere et dokumentutkast
     * @throws TekniskException 
     */
    public byte[] produserUtkast(long behandlingID, DokumentType dokumentType, BrevDataDto brevDataDto)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        return produserDokument(behandlingID, dokumentType, brevDataDto, true);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, DokumentType dokumentType, BrevDataDto brevDataDto)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        produserDokument(behandlingID, dokumentType, brevDataDto, false);
    }

    private byte[] produserDokument(long behandlingID, DokumentType dokumentType, BrevDataDto brevDataDto, boolean erUtkast)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
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
    public void produserDokumentISaksflyt(long behandlingID, DokumentType dokumentType, BrevDataDto brevDataDto) throws FunksjonellException {
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        switch (dokumentType) {
            case MELDING_MANGLENDE_OPPLYSNINGER:
                prosessinstans.setType(ProsessType.MANGELBREV);
                prosessinstans.setSteg(ProsessSteg.MANGELBREV);
                break;
            default:
                throw new FunksjonellException("DokumentType " + dokumentType + " er ikke støttet.");
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
