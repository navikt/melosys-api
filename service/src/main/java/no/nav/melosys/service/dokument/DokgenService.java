package no.nav.melosys.service.dokument;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalMapper;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidKlage;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer) {
        this.dokgenConsumer = dokgenConsumer;
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingId, BrevData brevData) {
        throw new NotImplementedException("Not implemented");
    }

    public void produserDokument(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Brevbestilling brevbestilling) throws FunksjonellException, TekniskException {
        byte[] produsertBrev = produserBrev(produserbartDokument, brevbestilling.getBehandling());

    }

    public byte[] produserBrev(Produserbaredokumenter produserbartDokument, Behandling behandling) throws TekniskException, FunksjonellException {
        String malnavn = DokgenMalMapper.hentMalnavn(produserbartDokument);

        return lagPdf(malnavn, mapBehandling(produserbartDokument, behandling));
    }

    public boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = DokgenMalMapper.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private DokgenDto mapBehandling(Produserbaredokumenter produserbartDokument, Behandling behandling) throws TekniskException, FunksjonellException {
        switch (produserbartDokument) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                return SaksbehandlingstidSoknad.av(behandling);
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                return SaksbehandlingstidKlage.av(behandling);
            default:
                throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet av melosys-dokgen", produserbartDokument));
        }
    }

    private byte[] lagPdf(String malNavn, DokgenDto dokgenDto) {
        return dokgenConsumer.lagPdf(malNavn, dokgenDto);
    }

}
