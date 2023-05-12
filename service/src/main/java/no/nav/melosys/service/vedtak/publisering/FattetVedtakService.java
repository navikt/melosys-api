package no.nav.melosys.service.vedtak.publisering;

import java.time.ZoneId;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.VedtakMetadata;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadFtrl;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.vedtak.publisering.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.time.LocalDate.ofInstant;
import static java.util.Collections.emptyList;
import static no.nav.melosys.service.vedtak.publisering.dto.IdentifikatorType.BRUKER;
import static no.nav.melosys.service.vedtak.publisering.dto.IdentifikatorType.ORGANISASJON;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

// TODO Slett klassen
@Service
public class FattetVedtakService {

    private final FattetVedtakProducer fattetVedtakProducer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;
    private final UtledMottaksdato utledMottaksdato;

    public FattetVedtakService(FattetVedtakProducer fattetVedtakProducer, BehandlingService behandlingService,
                               BehandlingsresultatService behandlingsresultatService,
                               PersondataFasade persondataFasade, UtledMottaksdato utledMottaksdato) {
        this.fattetVedtakProducer = fattetVedtakProducer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.utledMottaksdato = utledMottaksdato;
    }

    @Transactional
    public void publiserFattetVedtak(long behandlingId) throws IkkeFunnetException {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        fattetVedtakProducer.produserMelding(lagMelding(behandling));
    }

    private FattetVedtak lagMelding(Behandling behandling) throws IkkeFunnetException {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        final var persondata = hentPersondata(behandling);
        return new FattetVedtak(
            lagSak(behandling, behandling.getFagsak(), persondata),
            lagVedtak(behandlingsresultat.getVedtakMetadata()),
            lagSoeknad(behandling),
            lagSaksopplysninger(persondata),
            null,
            emptyList(),
            lagFullmektig(behandling.getFagsak())
        );
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }

    private Sak lagSak(Behandling behandling, Fagsak fagsak, Persondata persondata) {
        return new Sak(persondata.hentFolkeregisterident(),
            behandling.getId(),
            fagsak.getSaksnummer(),
            fagsak.getType().getKode(),
            ofInstant(fagsak.getRegistrertDato(), ZoneId.of("Europe/Paris"))
        );
    }

    private Vedtak lagVedtak(VedtakMetadata vedtakMetadata) {
        return new Vedtak(
            ofInstant(vedtakMetadata.getVedtaksdato(), ZoneId.of("Europe/Paris")),
            vedtakMetadata.getVedtakKlagefrist(),
            vedtakMetadata.getVedtakstype().getKode(),
            vedtakMetadata.getRegistrertAv(),
            null
        );
    }

    private Soeknad lagSoeknad(Behandling behandling) {
        SoeknadFtrl mottatteOpplysningerData = (SoeknadFtrl) behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        return new Soeknad(
            mottatteOpplysningerData.getTrygdedekning(),
            mottatteOpplysningerData.loennOgGodtgjoerelse,
            mottatteOpplysningerData.juridiskArbeidsgiverNorge,
            mottatteOpplysningerData.foretakUtland,
            utledMottaksdato.getMottaksdato(behandling),
            mottatteOpplysningerData.periode
        );
    }

    private Saksopplysninger lagSaksopplysninger(Persondata persondata) {
        return new Saksopplysninger(
            new Person(persondata.hentFolkeregisterident(),
                new Navn(persondata.getFornavn(), persondata.getMellomnavn(), persondata.getEtternavn(),
                    null
                ),
                new Statsborgerskap(
                    persondata.hentAlleStatsborgerskap().stream().findFirst().map(Land::getKode).orElse(null),
                    null,
                    null,
                    null,
                    null
                ),
                null,
                null
            ),
            null,
            null
        );
    }


    private Fullmektig lagFullmektig(Fagsak fagsak) {
        return fagsak.finnRepresentant(Representerer.BRUKER)
            .map(f -> {
                String fnr = null;
                if (isEmpty(f.getOrgnr())) {
                    fnr = persondataFasade.hentFolkeregisterident(f.getAktørId());
                }
                return new Fullmektig(new Identifikator(hasText(fnr) ? fnr : f.getOrgnr(), hasText(fnr) ? BRUKER : ORGANISASJON));
            }).orElse(null);
    }
}
