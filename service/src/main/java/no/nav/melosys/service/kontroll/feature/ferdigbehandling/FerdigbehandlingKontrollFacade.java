package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FerdigbehandlingKontrollFacade {

    private final MedRegisterOpplysningService kontrollerMedRegisterOpplysningService;
    private final FerdigbehandlingKontrollService ferdigbehandlingKontrollService;

    public FerdigbehandlingKontrollFacade(MedRegisterOpplysningService KontrollerMedRegisterOpplysningService,
                                          FerdigbehandlingKontrollService ferdigbehandlingKontrollService) {
        this.kontrollerMedRegisterOpplysningService = KontrollerMedRegisterOpplysningService;
        this.ferdigbehandlingKontrollService = ferdigbehandlingKontrollService;
    }

    @Transactional
    public void kontroller(long behandlingId, boolean skalRegisteropplysningerOppdateres, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        if (skalRegisteropplysningerOppdateres) {
            kontrollerMedRegisterOpplysningService.kontroller(behandlingId, behandlingsresultattype);
        } else {
            ferdigbehandlingKontrollService.kontroller(behandlingId, behandlingsresultattype);
        }
    }
}
