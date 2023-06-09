package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.validering.Kontrollfeil;
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
    public Collection<Kontrollfeil> kontroller(long behandlingId, boolean skalRegisteropplysningerOppdateres, Behandlingsresultattyper behandlingsresultattype, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) throws ValideringException {
        if (skalRegisteropplysningerOppdateres) {
            return kontrollerKontrollMedRegisteropplysning.kontroller(behandlingId, behandlingsresultattype, kontrollerSomSkalIgnoreres);
        } else {
            return kontroll.kontroller(behandlingId, behandlingsresultattype, kontrollerSomSkalIgnoreres);
        }
    }

    @Transactional(readOnly = true)
    public Collection<Kontrollfeil> kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) throws ValideringException {
        return kontroll.kontroller(behandlingId, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }

    @Transactional
    public Collection<Kontrollfeil> kontrollerVedtakMedRegisteropplysninger(Behandling behandling,
                                                                            Behandlingsresultat behandlingsresultat,
                                                                            Sakstyper sakstype,
                                                                            Behandlingsresultattyper behandlingsresultattype,
                                                                            Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) throws ValideringException {
        return kontrollerKontrollMedRegisteropplysning.kontrollerVedtak(behandling, behandlingsresultat, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }
}
