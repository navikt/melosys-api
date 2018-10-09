package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.DokumentType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingResponse;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DokumentService {

    private BehandlingRepository behandlingRepository;

    private BrevDataService brevDataService;

    private DokSysFasade dokSysFasade;

    private JoarkFasade joarkFasade;

    @Autowired
    DokumentService(BehandlingRepository behandlingRepository, BrevDataService brevDataService, DokSysFasade dokSysFasade, JoarkFasade joarkFasade) {
        this.behandlingRepository = behandlingRepository;
        this.brevDataService = brevDataService;
        this.joarkFasade = joarkFasade;
        this.dokSysFasade = dokSysFasade;
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
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return (byte[]) produserDokument(behandlingID, dokumentType, brevDataDto, true);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public DokumentbestillingResponse produserDokument(long behandlingID, DokumentType dokumentType, BrevDataDto brevDataDto) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        return (DokumentbestillingResponse) produserDokument(behandlingID, dokumentType, brevDataDto, false);
    }

    private Object produserDokument(long behandlingID, DokumentType dokumentType, BrevDataDto brevDataDto, boolean erUtkast)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(dokumentType, behandling, brevDataDto);
        Object brevData = brevDataService.lagBrevXML(dokumentType, behandling, brevDataDto);

        if (erUtkast) {
            return dokSysFasade.produserDokumentutkast(request, brevData);
        } else {
            return dokSysFasade.produserIkkeredigerbartDokument(request, brevData);
        }
    }
}
