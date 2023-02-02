package no.nav.melosys.service.brev.components;

import java.time.LocalDateTime;

import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.UtkastBrevRepository;
import org.springframework.stereotype.Component;

@Component
public class OppdaterUtkastComponent {

    private final UtkastBrevRepository utkastBrevRepository;

    public OppdaterUtkastComponent(UtkastBrevRepository utkastBrevRepository) {
        this.utkastBrevRepository = utkastBrevRepository;
    }


    public void oppdaterUtkast(RequestDto request) {
        if (!utkastBrevRepository.existsById(request.utkastBrevID())) {
            throw new FunksjonellException("Prøver å oppdatere brevutkast med ID %s som ikke finnes".formatted(request.utkastBrevID()));
        }

        var utkast = new UtkastBrev();
        utkast.setId(request.utkastBrevID());
        utkast.setBehandlingID(request.behandlingID());
        utkast.setLagretAvSaksbehandler(request.saksbehandlerIdent());
        utkast.setLagringsdato(LocalDateTime.now());
        utkast.setBrevbestillingUtkast(request.brevbestillingUtkast());
        utkastBrevRepository.save(utkast);
    }

    public record RequestDto(
        long utkastBrevID,
        long behandlingID,
        String saksbehandlerIdent,
        BrevbestillingUtkast brevbestillingUtkast
    ) {
    }
}
