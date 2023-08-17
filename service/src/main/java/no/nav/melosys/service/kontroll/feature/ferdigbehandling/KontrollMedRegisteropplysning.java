package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
class KontrollMedRegisteropplysning {

    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;
    private final Kontroll kontroll;

    public KontrollMedRegisteropplysning(BehandlingService behandlingService,
                                         PersondataFasade persondataFasade,
                                         RegisteropplysningerService registeropplysningerService,
                                         Kontroll kontroll) {
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.kontroll = kontroll;
    }

    public Collection<Kontrollfeil> kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Sakstyper sakstype = behandling.getFagsak().getType();
        return kontrollerVedtak(behandling, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }

    public Collection<Kontrollfeil> kontrollerVedtak(Behandling behandling,
                                                     Sakstyper sakstype,
                                                     Behandlingsresultattyper behandlingsresultattype,
                                                     Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) {
        hentNyeRegisteropplysninger(behandling);
        return kontroll.kontrollerVedtak(behandling.getId(), sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }

    private void hentNyeRegisteropplysninger(Behandling behandling) {
        Periode søknadsperiode = behandling.getMottatteOpplysninger().getMottatteOpplysningerData().periode;
        String fnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(søknadsperiode.getFom())
                .tom(søknadsperiode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());
    }
}
