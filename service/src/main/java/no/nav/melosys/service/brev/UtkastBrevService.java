package no.nav.melosys.service.brev;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.repository.UtkastBrevRepository;
import no.nav.melosys.service.brev.components.OppdaterUtkastComponent;
import org.springframework.stereotype.Service;

@Service
public class UtkastBrevService {


    private final OppdaterUtkastComponent oppdaterUtkastComponent;
    private final UtkastBrevRepository utkastBrevRepository;

    public UtkastBrevService(OppdaterUtkastComponent oppdaterUtkastComponent, UtkastBrevRepository utkastBrevRepository) {
        this.oppdaterUtkastComponent = oppdaterUtkastComponent;
        this.utkastBrevRepository = utkastBrevRepository;
    }

    public List<UtkastBrev> hentUtkast(long behandlingID) {
        return utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(behandlingID);
    }

    public void lagreUtkast(long behandlingID, String saksbehandler, BrevbestillingUtkast brevbestillingUtkast) {
        var utkast = new UtkastBrev();
        utkast.setBehandlingID(behandlingID);
        utkast.setLagretAvSaksbehandler(saksbehandler);
        utkast.setLagringsdato(LocalDateTime.now());
        utkast.setBrevbestillingUtkast(brevbestillingUtkast);

        utkastBrevRepository.save(utkast);
    }

    public void oppdaterUtkast(OppdaterUtkastComponent.RequestDto request) {
        oppdaterUtkastComponent.oppdaterUtkast(request);
    }

    public void slettUtkast(long utkastBrevID) {
        utkastBrevRepository.deleteById(utkastBrevID);
    }
}
