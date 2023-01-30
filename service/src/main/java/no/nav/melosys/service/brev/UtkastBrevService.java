package no.nav.melosys.service.brev;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.repository.UtkastBrevRepository;
import org.springframework.stereotype.Service;

@Service
public class UtkastBrevService {

    private final UtkastBrevRepository utkastBrevRepository;

    public UtkastBrevService(UtkastBrevRepository utkastBrevRepository) {
        this.utkastBrevRepository = utkastBrevRepository;
    }

    public List<BrevbestillingUtkast> hentUtkast(long behandlingID) {
        return utkastBrevRepository
            .findAllByBehandlingIDOrderByLagringsdatoDesc(behandlingID)
            .stream().map(UtkastBrev::getBrevbestillingUtkast)
            .toList();
    }

    public void lagreUtkast(long behandlingID, String saksbehandler, BrevbestillingUtkast brevbestillingUtkast) {
        var utkast = new UtkastBrev();
        utkast.setBehandlingID(behandlingID);
        utkast.setLagretAvSaksbehandler(saksbehandler);
        utkast.setLagringsdato(LocalDateTime.now());
        utkast.setBrevbestillingUtkast(brevbestillingUtkast);

        utkastBrevRepository.save(utkast);
    }
}
