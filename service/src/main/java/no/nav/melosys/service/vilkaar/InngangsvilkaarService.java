package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
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

/**
 * Service som kaller regelmodulen.
 */
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
                                               Soeknadsland søknadsland,
                                               Periode søknadsperiode) throws IkkeFunnetException {
        boolean erEF_883_2004 = false;
        Kodeverk begrunnelseKode = null;
        try {
            erEF_883_2004 = vurderInngangsvilkår(behandlingID, søknadsland, søknadsperiode);
        } catch (IkkeFunnetException e) {
            begrunnelseKode = Inngangsvilkaar.NORDISK_UTENFOR_EOS; // FIXME erstattes med kode om manglende statsborgerskap
        } catch (MelosysException e) {
            begrunnelseKode = Inngangsvilkaar.TREDJELANDSBORGER; // FIXME erstattes med kode om teknisk feil
        }
        log.info("Kall til regelmodul for behandling {} returnerte {}", behandlingID, erEF_883_2004);
        vilkaarsresultatService.oppdaterVilkaarsresultat(behandlingID, FO_883_2004_INNGANGSVILKAAR, erEF_883_2004, begrunnelseKode);
        return erEF_883_2004;
    }

    private boolean vurderInngangsvilkår(long behandlingID, Soeknadsland søknadsland, Periode søknadsperiode)
        throws FunksjonellException, TekniskException {
        Land statsborgerskap = hentStatsborgerskapForPerioden(behandlingID, søknadsperiode);
        if (statsborgerskap == null) {
            throw new IkkeFunnetException("Finner ingen informasjon om statsborgerskap");
        }

        var landkoderISO3 = tilIso3(søknadsland.landkoder);
        VurderInngangsvilkaarReply res = regelmodulFasade.vurderInngangsvilkår(statsborgerskap, landkoderISO3, søknadsperiode);

        List<String> feilmeldinger = res.feilmeldinger.stream()
            .filter(feilmelding -> feilmelding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL)
            .map(feilmelding -> feilmelding.melding)
            .collect(Collectors.toList());

        if (!feilmeldinger.isEmpty()) {
            throw new FunksjonellException("Vurdering av inngangsvilkår feilet: " + String.join(System.lineSeparator(), feilmeldinger));
        }
        return res.kvalifisererForEf883_2004;
    }

    Land hentStatsborgerskapForPerioden(long behandlingID, Periode periode) throws IkkeFunnetException {
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
