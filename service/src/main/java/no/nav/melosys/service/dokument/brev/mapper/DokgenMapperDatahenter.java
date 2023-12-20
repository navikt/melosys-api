package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.hasText;

@Component
public class DokgenMapperDatahenter {

    private final BehandlingsresultatService behandlingsresultatService;
    private final EregFasade eregFasade;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    protected DokgenMapperDatahenter(BehandlingsresultatService behandlingsresultatService,
                                     EregFasade eregFasade,
                                     PersondataFasade persondataFasade,
                                     KodeverkService kodeverkService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.eregFasade = eregFasade;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    String hentNorskPoststed(String postnr) {
        return kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postnr);
    }

    String hentLandnavnFraLandkode(String landkode) {
        var landnavn = "";
        if (hasText(landkode)) {
            landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, landkode);
            if (landnavn.equals("UKJENT")) {
                landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode);
            }
        }
        return landnavn.equals("UKJENT") ? "" : landnavn;
    }

    String hentFullmektigNavn(Fagsak fagsak, Fullmaktstype fullmaktstype) {
        return fagsak.finnFullmektig(fullmaktstype)
            .map(aktoer -> {
                if (StringUtils.hasText(aktoer.getOrgnr())) {
                    return eregFasade.hentOrganisasjonNavn(aktoer.getOrgnr());
                } else if (StringUtils.hasText(aktoer.getPersonIdent())) {
                    return persondataFasade.hentSammensattNavn(aktoer.getPersonIdent());
                }
                return null;
            })
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

    Persondata hentPersondata(Behandling behandling) {
        if (behandling.getFagsak().getHovedpartRolle() == Aktoersroller.VIRKSOMHET) {
            return null;
        }
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }

    Persondata hentPersonMottaker(Mottaker mottaker) {
        if (mottaker.erOrganisasjon()) {
            return null;
        }
        if (StringUtils.hasText(mottaker.getPersonIdent())) {
            return persondataFasade.hentPerson(mottaker.getPersonIdent());
        }
        if (StringUtils.hasText(mottaker.getAktørId())) {
            return persondataFasade.hentPerson(mottaker.getAktørId());
        }
        throw new FunksjonellException("PersonMottaker mangler aktørID og personIdent");
    }

    public Behandlingsresultat hentBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingId);
    }
}
