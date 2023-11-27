package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING;

@Service
@Primary
public class AvklartFullstendigManglendeInnbetalingService {

    protected final AvklartefaktaService avklartefaktaService;

    public AvklartFullstendigManglendeInnbetalingService(AvklartefaktaService avklartefaktaService) {
        this.avklartefaktaService = avklartefaktaService;
    }

    public void lagreFullstendigManglendeInnbetalingSomAvklartFakta(long behandlingID, Boolean fullstendigManglendeInnbetaling) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, FULLSTENDIG_MANGLENDE_INNBETALING);

        avklartefaktaService.leggTilAvklarteFakta(behandlingID, FULLSTENDIG_MANGLENDE_INNBETALING, FULLSTENDIG_MANGLENDE_INNBETALING.getKode(),
            fullstendigManglendeInnbetaling.toString(), Avklartefakta.VALGT_FAKTA);
    }
}
