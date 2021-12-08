package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class VedtakKontrollService {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;
    private final Unleash unleash;

    public VedtakKontrollService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService, LovvalgsperiodeService lovvalgsperiodeService,
                                 PersondataFasade persondataFasade, RegisteropplysningerService registeropplysningerService, Unleash unleash) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.unleash = unleash;
    }

    public void validerInnvilgelse(long behandlingId, Vedtakstyper vedtakstype, boolean oppdaterRegisteropplysninger) throws ValideringException {
        var behandling = behandlingService.hentBehandling(behandlingId);
        var sakstype = behandling.getFagsak().getType();
        if (oppdaterRegisteropplysninger) {
            var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
            validerInnvilgelse(behandling, behandlingsresultat, vedtakstype, sakstype);
        } else {
            kontrollerFattVedtak(behandlingId, vedtakstype, sakstype);
        }
    }

    public void validerInnvilgelse(Behandling behandling, Behandlingsresultat behandlingsresultat, Vedtakstyper vedtakstype, Sakstyper sakstype) throws ValideringException {
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        String fnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentAktørID());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(lovvalgsperiode.getFom())
                .tom(lovvalgsperiode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());

        kontrollerFattVedtak(behandling.getId(), vedtakstype, sakstype);
    }

    private void kontrollerFattVedtak(long behandlingID, Vedtakstyper vedtakstype, Sakstyper sakstype) throws ValideringException {
        Collection<Kontrollfeil> feilValideringer = utførKontroller(behandlingID, vedtakstype, sakstype);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                feilValideringer.stream().map(Kontrollfeil::tilDto).collect(Collectors.toList()));
        }
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID, Vedtakstyper vedtakstype, Sakstyper sakstype) {
        return utførKontroller(
            behandlingService.hentBehandling(behandlingID),
            lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID),
            VedtakKontrollFactory.hentKontrollerForVedtakstype(vedtakstype, sakstype)
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Lovvalgsperiode lovvalgsperiode,
        Set<Function<VedtakKontrollData, Kontrollfeil>> kontroller
    ) {
        BehandlingsgrunnlagData behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        Persondata persondata = hentPersondata(behandling);
        VedtakKontrollData vedtakKontrollData = new VedtakKontrollData(medlemskapDokument, persondata, behandlingsgrunnlagData, lovvalgsperiode);
        return kontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (unleash.isEnabled("melosys.pdl.aktiv")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }
}
