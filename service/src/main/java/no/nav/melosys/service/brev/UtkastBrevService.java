package no.nav.melosys.service.brev;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.UtkastBrevRepository;
import no.nav.melosys.service.brev.bestilling.OppdaterUtkastService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.springframework.stereotype.Service;

@Service
public class UtkastBrevService {


    private final OppdaterUtkastService oppdaterUtkastService;
    private final UtkastBrevRepository utkastBrevRepository;

    public UtkastBrevService(OppdaterUtkastService oppdaterUtkastService, UtkastBrevRepository utkastBrevRepository) {
        this.oppdaterUtkastService = oppdaterUtkastService;
        this.utkastBrevRepository = utkastBrevRepository;
    }

    public List<UtkastBrev> hentUtkast(long behandlingID) {
        return utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(behandlingID);
    }

    public void lagreUtkast(long behandlingID, String saksbehandler, BrevbestillingUtkast brevbestillingUtkast) {
        validerFinnesAlleredeUtkastForSammeBrev(behandlingID, brevbestillingUtkast.getTittel());

        var utkast = new UtkastBrev();
        utkast.setBehandlingID(behandlingID);
        utkast.setLagretAvSaksbehandler(saksbehandler);
        utkast.setLagringsdato(LocalDateTime.now());
        utkast.setBrevbestillingUtkast(brevbestillingUtkast);

        utkastBrevRepository.save(utkast);
    }

    public void oppdaterUtkast(OppdaterUtkastService.RequestDto request) {
        oppdaterUtkastService.oppdaterUtkast(request);
    }

    public void slettUtkast(long utkastBrevID) {
        utkastBrevRepository.deleteById(utkastBrevID);
    }


    public void slettTilhørendeUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        var tittel = BrevbestillingUtkast.getTittel(brevbestillingDto.getDokumentTittel(), brevbestillingDto.getProduserbardokument());
        hentBrevbestillingUtkastMedTittel(behandlingID, tittel).ifPresent((utkastBrev -> slettUtkast(utkastBrev.getId())));
    }

    private void validerFinnesAlleredeUtkastForSammeBrev(long behandlingID, String tittel) {
        if (hentBrevbestillingUtkastMedTittel(behandlingID, tittel).isPresent()) {
            throw new FunksjonellException("Behandling %s har allerede et brevutkast for samme brev".formatted(behandlingID));
        }
    }

    private Optional<UtkastBrev> hentBrevbestillingUtkastMedTittel(long behandlingID, String tittel) {
        return hentUtkast(behandlingID).stream().filter(utkast -> utkast.getBrevbestillingUtkast().getTittel().equals(tittel)).findFirst();
    }
}
