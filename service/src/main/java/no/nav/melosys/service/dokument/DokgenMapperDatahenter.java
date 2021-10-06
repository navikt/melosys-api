package no.nav.melosys.service.dokument;

import java.time.Instant;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMapperDatahenter {

    private final BehandlingsresultatService behandlingsresultatService;
    private final EregFasade eregFasade;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    protected DokgenMapperDatahenter(BehandlingsresultatService behandlingsresultatService,
                                     @Qualifier("system") EregFasade eregFasade,
                                     @Qualifier("system") PersondataFasade persondataFasade,
                                     KodeverkService kodeverkService,
                                     Unleash unleash) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    String hentPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr);
    }

    String hentLandnavn(String landkode) {
        var landnavn = "";
        if (hasText(landkode)) {
            landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode);
            if (landnavn.equals("UKJENT")) {
                landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, landkode);
            }
        }
        return landnavn.equals("UKJENT") ? "" : landnavn;
    }

    String hentFullmektigNavn(Fagsak fagsak, Representerer representerer) {
        return fagsak.finnRepresentant(representerer)
            .map(aktoer -> eregFasade.hentOrganisasjonNavn(aktoer.getOrgnr()))
            .orElse(null);
    }

    Instant hentVedtaksdato(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }

    Behandlingsresultat hentBehandlingsresultat(long behandlingId) {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingId);
    }

    Persondata hentPersondata(DokgenBrevbestilling brevbestilling) {
        final var behandling = brevbestilling.getBehandling();
        if (unleash.isEnabled("melosys.brev.adresser.pdl")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        String fnr = behandling.hentPersonDokument().hentFolkeregisterident();
        return (Persondata) persondataFasade.hentPersonFraTps(fnr, Informasjonsbehov.STANDARD).getDokument();
    }

    String hentSammensattNavn(String fnr) {
        return persondataFasade.hentSammensattNavn(fnr);
    }
}
