package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class ValiderVedtakService {
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;
    private final VedtakKontrollService vedtakKontrollService;

    public ValiderVedtakService(PersondataFasade persondataFasade, RegisteropplysningerService registeropplysningerService, VedtakKontrollService vedtakKontrollService) {
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.vedtakKontrollService = vedtakKontrollService;
    }

    void validerInnvilgelse(Behandling behandling, Behandlingsresultat behandlingsresultat, Vedtakstyper vedtakstype, Sakstyper sakstype) throws ValideringException {
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
        Collection<Kontrollfeil> feilValideringer = vedtakKontrollService.utførKontroller(behandlingID, vedtakstype, sakstype);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                feilValideringer.stream().map(Kontrollfeil::tilDto).collect(Collectors.toList()));
        }
    }
}
