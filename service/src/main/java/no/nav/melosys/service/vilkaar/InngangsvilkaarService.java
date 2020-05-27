package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.regelmodul.RegelmodulFasade;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.service.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_INNGANGSVILKAAR;
import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;
import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerPeriodeFactory.REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR;

@Service
public class InngangsvilkaarService {
    private static final Logger log = LoggerFactory.getLogger(InngangsvilkaarService.class);

    private final SaksopplysningerService saksopplysningerService;
    private final RegelmodulFasade regelmodulFasade;
    private final VilkaarsresultatService vilkaarsresultatService;

    @Autowired
    public InngangsvilkaarService(SaksopplysningerService saksopplysningerService,
                                  RegelmodulFasade regelmodulFasade,
                                  VilkaarsresultatService vilkaarsresultatService) {
        this.saksopplysningerService = saksopplysningerService;
        this.regelmodulFasade = regelmodulFasade;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    public boolean vurderOgLagreInngangsvilkår(long behandlingID,
                                               List<String> søknadsland,
                                               ErPeriode søknadsperiode) throws FunksjonellException, TekniskException {
        final InngangsvilkaarVurdering vurderingEF_883_2004 = vurderInngangsvilkår(behandlingID, søknadsland, søknadsperiode);
        final boolean erEF_883_2004 = vurderingEF_883_2004.isOppfylt();

        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR,
            erEF_883_2004, vurderingEF_883_2004.getBegrunnelseKode());
        return erEF_883_2004;
    }

    private InngangsvilkaarVurdering vurderInngangsvilkår(long behandlingID, List<String> søknadsland, ErPeriode søknadsperiode)
        throws FunksjonellException, TekniskException {
        Land statsborgerskap = hentStatsborgerskapForPerioden(behandlingID, søknadsperiode);
        if (statsborgerskap == null) {
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.MANGLER_STATSBORGERSKAP);
        }
        if (søknadsperiode.getTom() == null) {
            søknadsperiode = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusYears(REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR));
        }

        var landkoderISO3 = tilIso3(søknadsland);
        VurderInngangsvilkaarReply res = regelmodulFasade.vurderInngangsvilkår(statsborgerskap, landkoderISO3, søknadsperiode);

        List<String> feilmeldinger = res.feilmeldinger.stream()
            .filter(feilmelding -> feilmelding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL)
            .map(feilmelding -> feilmelding.melding)
            .collect(Collectors.toList());

        if (!feilmeldinger.isEmpty()) {
            if (log.isErrorEnabled()) {
                log.error("Vurdering av inngangsvilkår feilet: {}", String.join(System.lineSeparator(), feilmeldinger));
            }
            return new InngangsvilkaarVurdering(false, Inngangsvilkaar.TEKNISK_FEIL);
        } else {
            // Vurdering fra regelmodul gir ikke begrunnelser så langt.
            return new InngangsvilkaarVurdering(res.kvalifisererForEf883_2004, null);
        }
    }

    Land hentStatsborgerskapForPerioden(long behandlingID, ErPeriode periode) throws IkkeFunnetException {
        // Hent statsborgerskap fra saksopplysningene...
        // Ved søknad tilbake i tid brukes historisk statsborgerskap
        if (periode.getFom().isBefore(LocalDate.now())) {
            return avgjørStatsborgerskapPåStartDato(
                saksopplysningerService.hentPersonhistorikk(behandlingID).statsborgerskapListe, periode.getFom());
        } else {
            return saksopplysningerService.hentPersonOpplysninger(behandlingID).statsborgerskap;
        }
    }

    Land avgjørStatsborgerskapPåStartDato(List<StatsborgerskapPeriode> statsborgerskapListe, LocalDate startDato) {
        if (statsborgerskapListe.isEmpty()) {
            return null;
        }
        List<StatsborgerskapPeriode> gyldigeStasborgerskap = statsborgerskapListe.stream()
            .filter(p -> p.getPeriode().inkluderer(startDato))
            .collect(Collectors.toList());
        if (gyldigeStasborgerskap.isEmpty()) {
            return null;
        } else if (gyldigeStasborgerskap.size() == 1) {
            return gyldigeStasborgerskap.get(0).statsborgerskap;
        } else {
            // Hvis det finnes flere kilder for samme dato så ønsker vi å se bort fra det som kommer fra Skattedirektoratet
            // pga. dårlig datakvalitet. Vi filterer også ukjent statsborgerskap siden det ikke hjelper å vurdere inngangsvilkår.
            return gyldigeStasborgerskap.stream().filter(p -> !p.erFraSkattedirektoratet())
                .filter(p -> !p.statsborgerskap.erUkjent())
                .max(Comparator.comparing(p -> p.endringstidspunkt))
                .map(p -> p.statsborgerskap).orElse(null);
        }
    }
}
