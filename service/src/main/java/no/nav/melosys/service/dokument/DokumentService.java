package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.DokumentType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
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
     */
    public byte[] hentDokument(String journalpostID, String dokumentID) throws SikkerhetsbegrensningException {
        return joarkFasade.hentDokument(journalpostID, dokumentID);
    }

    /**
     * Kaller Doksys for å produsere et dokumentutkast
     */
    public byte[] produserUtkast(long behandlingID, String dokumenttypeID) throws FunksjonellException, SikkerhetsbegrensningException {
        return produserDokument(behandlingID, dokumenttypeID, true);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, String dokumenttypeID) throws FunksjonellException, SikkerhetsbegrensningException {
        produserDokument(behandlingID, dokumenttypeID, false);
    }

    private byte[] produserDokument(long behandlingID, String dokumenttypeID, boolean erUtkast) throws FunksjonellException, SikkerhetsbegrensningException {
        DokumentType dokumentType = DokumentType.forKode(dokumenttypeID);
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new FunksjonellException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        DokumentbestillingMetadata request = brevDataService.lagBestillingMetadata(dokumentType, behandling);
        Object brevData = brevDataService.lagBrevXML(dokumentType, behandling);

        if (erUtkast) {
            return dokSysFasade.produserDokumentutkast(request, brevData);
        } else {
            dokSysFasade.produserIkkeredigerbartDokument(request, brevData);
            return null;
        }
    }
}
