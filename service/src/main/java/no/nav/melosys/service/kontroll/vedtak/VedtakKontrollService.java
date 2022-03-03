package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void kontrollerVedtak(long behandlingID, Vedtakstyper vedtakstype, boolean skalRegisteropplysningerOppdateres) throws ValideringException {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        var sakstype = behandling.getFagsak().getType();
        if (skalRegisteropplysningerOppdateres) {
            var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
            kontrollerInnvilgelse(behandling, behandlingsresultat, vedtakstype, sakstype);
        } else {
            kontrollerInnvilgelse(behandlingID, sakstype);
        }
    }

    public void kontrollerInnvilgelse(Behandling behandling,
                                      Behandlingsresultat behandlingsresultat,
                                      Vedtakstyper vedtakstype,
                                      Sakstyper sakstype) throws ValideringException {
        hentNyeRegisteropplysninger(behandlingsresultat, behandling);
        kontrollerInnvilgelse(behandling.getId(), sakstype);
    }

    private void hentNyeRegisteropplysninger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
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
    }

    private void kontrollerInnvilgelse(long behandlingID, Sakstyper sakstype) throws ValideringException {
        Collection<Kontrollfeil> kontrollfeil = utførKontroller(behandlingID, sakstype);
        if (!kontrollfeil.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                kontrollfeil.stream().map(Kontrollfeil::tilDto).toList());
        }
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID, Sakstyper sakstype) {
        return utførKontroller(
            behandlingService.hentBehandlingMedSaksopplysninger(behandlingID),
            lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID),
            sakstype);
    }

    private Collection<Kontrollfeil> utførKontroller(Behandling behandling, Lovvalgsperiode lovvalgsperiode,
                                                     Sakstyper sakstype) {
        Set<Function<VedtakKontrollData, Kontrollfeil>> vedtakKontroller =
            VedtakKontrollFactory.hentKontrollerForVedtak(sakstype);
        var vedtakKontrollData = hentVedtakKontrollData(behandling, lovvalgsperiode);
        return vedtakKontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private VedtakKontrollData hentVedtakKontrollData(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        BehandlingsgrunnlagData behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        Persondata persondata = hentPersondata(behandling);
        return new VedtakKontrollData(medlemskapDokument, persondata, behandlingsgrunnlagData,
            lovvalgsperiode);
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (unleash.isEnabled("melosys.pdl.aktiv")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }
}
