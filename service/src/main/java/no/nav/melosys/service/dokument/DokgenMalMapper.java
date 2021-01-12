package no.nav.melosys.service.dokument;

import java.time.LocalDate;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidKlage;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class DokgenMalMapper {

    private final KodeverkService kodeverkService;

    @Autowired
    public DokgenMalMapper(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    public DokgenDto mapBehandling(DokgenBrevbestilling brevbestilling) throws TekniskException, FunksjonellException {
        DokgenDto dto;
        switch (brevbestilling.getProduserbartdokument()) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                dto = SaksbehandlingstidSoknad.av(brevbestilling);
                break;
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                dto = SaksbehandlingstidKlage.av(brevbestilling);
                break;
            default:
                throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet av melosys-dokgen", brevbestilling.getProduserbartdokument()));
        }

        dto.setPoststed(hentPoststed(dto.getPostnr()));
        return dto;
    }

    private String hentPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr, LocalDate.now());
    }
}
