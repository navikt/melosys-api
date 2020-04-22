package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.Soeknadsland;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.regelmodul.RegelmodulFasade;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;

/**
 * Service som kaller regelmodulen.
 */
@Service
public class RegelmodulService {
    private static final Logger log = LoggerFactory.getLogger(RegelmodulService.class);

    private final BehandlingRepository behandlingRepo;
    private final SaksopplysningerService saksopplysningerService;
    private final RegelmodulFasade regelmodulFasade;

    @Autowired
    public RegelmodulService(BehandlingRepository repository,
                             SaksopplysningerService saksopplysningerService,
                             RegelmodulFasade regelmodulFasade) {
        this.behandlingRepo = repository;
        this.saksopplysningerService = saksopplysningerService;
        this.regelmodulFasade = regelmodulFasade;
    }

    /**
     * Kall til regelmodulen med opplysninger knyttet til behandlingen med ID {@code behandlingID}.
     *
     * @param behandlingID Database ID til den behandlingen som brukes for å konstruere requesten til regelmodulen.
     */
    FastsettLovvalgReply fastsettLovvalg(long behandlingID) {
        Behandling behandling = behandlingRepo.findWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            // Ikke funnet
            return null;
        }

        return regelmodulFasade.fastsettLovvalg(behandling.getSaksopplysninger());
    }

    public boolean kvalifisererForEf883_2004(Long behandlingID, Soeknadsland søknadsland, Periode periode) throws FunksjonellException {
        Land statsborgerskap = null;
        if (periode.getFom().isBefore(LocalDate.now())) {
            statsborgerskap = avgjørStatsborgerskapPåStartDato(
                saksopplysningerService.hentPersonhistorikk(behandlingID).statsborgerskapListe, periode.getFom());
        } else {
            statsborgerskap = saksopplysningerService.hentPersonOpplysninger(behandlingID).statsborgerskap;
        }
        VurderInngangsvilkaarReply res = vurderInngangsvilkår(statsborgerskap, tilIso3(søknadsland.landkoder), periode);

        List<Feilmelding> feilmeldinger = res.feilmeldinger.stream()
            .filter(feilmelding -> feilmelding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL)
            .collect(Collectors.toList());

        if (!feilmeldinger.isEmpty()) {
            throw new FunksjonellException("Vurdering av inngangsvilkår feilet.");
        }
        return Boolean.TRUE.equals(res.kvalifisererForEf883_2004);
     }

    /**
     * Kaller regelmodulen for å kjøre inngangsvilkårsvurdering
     *
     * @throws RuntimeException Hvis request- eller reply-prosessering feiler, hvis IO-feil ved kommunikasjon med regelmodulen, eller hvis regelmodulen returnerer noe annet enn HTTP 2xx
     */
    public VurderInngangsvilkaarReply vurderInngangsvilkår(Land brukersStatsborgerskap, List<String> søknadsland, Periode søknadsperiode) {
        return regelmodulFasade.vurderInngangsvilkår(brukersStatsborgerskap, søknadsland, søknadsperiode);
    }

    public static Land avgjørStatsborgerskapPåStartDato(List<StatsborgerskapPeriode> statsborgerskapListe, LocalDate startDato) {
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
