package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FerdigbehandlingKontrollFacade {

    private final KontrollMedRegisteropplysning kontrollerKontrollMedRegisteropplysning;
    private final Kontroll kontroll;

    public FerdigbehandlingKontrollFacade(KontrollMedRegisteropplysning kontrollerKontrollMedRegisteropplysning,
                                          Kontroll kontroll) {
        this.kontrollerKontrollMedRegisteropplysning = kontrollerKontrollMedRegisteropplysning;
        this.kontroll = kontroll;
    }

    @Transactional
    public void kontroller(long behandlingId, boolean skalRegisteropplysningerOppdateres, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        if (skalRegisteropplysningerOppdateres) {
            kontrollerKontrollMedRegisteropplysning.kontroller(behandlingId, behandlingsresultattype);
        } else {
            kontroll.kontroller(behandlingId, behandlingsresultattype);
        }
    }

    @Transactional(readOnly = true)
    public void kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        kontroll.kontroller(behandlingId, behandlingsresultattype);
    }

    @Transactional
    public void kontrollerVedtakMedRegisteropplysninger(Behandling behandling,
                                                        Behandlingsresultat behandlingsresultat,
                                                        Sakstyper sakstype,
                                                        Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        kontrollerKontrollMedRegisteropplysning.kontrollerVedtak(behandling, behandlingsresultat, sakstype, behandlingsresultattype);
    }
}
