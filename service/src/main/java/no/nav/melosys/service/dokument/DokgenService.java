package no.nav.melosys.service.dokument;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalResolver;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokgenMalResolver dokgenMalResolver;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer, DokgenMalResolver dokgenMalResolver) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokgenMalResolver = dokgenMalResolver;
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingId, BrevData brevData) {
        throw new NotImplementedException("Not implemented");
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartDokument, Behandling behandling) throws TekniskException, FunksjonellException {
        String malnavn = dokgenMalResolver.hentMalnavn(produserbartDokument);

        return lagPdf(malnavn, dokgenMalResolver.mapBehandling(produserbartDokument, behandling));
    }

    public boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokgenMalResolver.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private byte[] lagPdf(String malNavn, DokgenDto dokgenDto) {
        return dokgenConsumer.lagPdf(malNavn, dokgenDto);
    }

}
