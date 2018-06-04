package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.DokumentType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokumentService {

    private BehandlingRepository behandlingRepository;

    private BrevService brevService;

    private DokSysFasade dokSysFasade;

    private JoarkFasade joarkFasade;

    @Autowired
    DokumentService(BehandlingRepository behandlingRepository, BrevService brevService, DokSysFasade dokSysFasade, JoarkFasade joarkFasade) {
        this.behandlingRepository = behandlingRepository;
        this.brevService = brevService;
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
    public byte[] produserUtkast(long behandlingID, String typeID) throws FunksjonellException, SikkerhetsbegrensningException {
        return produserDokument(behandlingID, typeID, true);
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(long behandlingID, String typeID) throws FunksjonellException, SikkerhetsbegrensningException {
        produserDokument(behandlingID, typeID, false);
    }

    private byte[] produserDokument(long behandlingID, String typeID, boolean erUtkast) throws FunksjonellException, SikkerhetsbegrensningException {
        DokumentType dokumentType = DokumentType.forKode(typeID);
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new FunksjonellException("Behandling med ID " + behandlingID + " finnes ikke");
        }

        DokumentbestillingRequest request = lagDokumentRequest(dokumentType, behandling);
        Object brevData = brevService.lagBrevData(dokumentType, behandling);

        if (erUtkast) {
            return dokSysFasade.produserDokumentutkast(request, brevData);
        } else {
            dokSysFasade.produserIkkeredigerbartDokument(request, brevData);
            return null;
        }
    }

    private DokumentbestillingRequest lagDokumentRequest(DokumentType dokumentType, Behandling behandling) {
        DokumentbestillingRequest dokumentbestillingRequest = new DokumentbestillingRequest();
        // FIXME MELOSYS-1011
        return dokumentbestillingRequest;
    }
}
