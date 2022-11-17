package no.nav.melosys.service.vedtak.publisering;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadFtrl;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.vedtak.publisering.dto.Fullmektig;
import no.nav.melosys.service.vedtak.publisering.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.time.LocalDate.ofInstant;
import static java.util.Collections.emptyList;
import static no.nav.melosys.service.vedtak.publisering.dto.IdentifikatorType.BRUKER;
import static no.nav.melosys.service.vedtak.publisering.dto.IdentifikatorType.ORGANISASJON;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Service
public class FattetVedtakService {

    private final FattetVedtakProducer fattetVedtakProducer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;

    public FattetVedtakService(FattetVedtakProducer fattetVedtakProducer, BehandlingService behandlingService,
                               BehandlingsresultatService behandlingsresultatService,
                               PersondataFasade persondataFasade) {
        this.fattetVedtakProducer = fattetVedtakProducer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
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
            lagSoeknad(behandling.getMottatteOpplysninger()),
            lagSaksopplysninger(persondata),
            null,
            lagPerioder(behandlingsresultat),
            lagFullmektig(behandling.getFagsak()),
            lagRepresentantAvgift(behandlingsresultat)
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

    private Soeknad lagSoeknad(MottatteOpplysninger mottatteOpplysninger) {
        SoeknadFtrl mottatteOpplysningerData = (SoeknadFtrl) mottatteOpplysninger.getMottatteOpplysningerData();
        return new Soeknad(
            mottatteOpplysningerData.getTrygdedekning(),
            mottatteOpplysningerData.loennOgGodtgjoerelse,
            mottatteOpplysningerData.juridiskArbeidsgiverNorge,
            mottatteOpplysningerData.foretakUtland,
            mottatteOpplysninger.getMottaksdato(),
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

    private Collection<LovvalgOgMedlemskapsperiode> lagPerioder(Behandlingsresultat behandlingsresultat) {
        Optional<MedlemAvFolketrygden> medlemAvFolketrygden = behandlingsresultat.finnMedlemAvFolketrygden();

        //NOTE Ikke støtte for EØS foreløpig
        return medlemAvFolketrygden.map(this::lagMedlemskapsperioder).orElse(emptyList());
    }

    private Collection<LovvalgOgMedlemskapsperiode> lagMedlemskapsperioder(MedlemAvFolketrygden medlemAvFolketrygden) {
        var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();

        return medlemAvFolketrygden.getMedlemskapsperioder().stream().map(m -> {

            var avgiftForNorsk = m.getTrygdeavgift().stream()
                .filter(no.nav.melosys.domain.avgift.Trygdeavgift::erAvgiftForNorskInntekt).findFirst();

            var avgiftForUtenlandsk = m.getTrygdeavgift().stream()
                .filter(t -> !t.erAvgiftForNorskInntekt()).findFirst();

            Aktoer betalesAv = fastsattTrygdeavgift.getBetalesAv();

            return new LovvalgOgMedlemskapsperiode(
                m.getBestemmelse(),
                null,
                null,
                new Periode(m.getFom(), m.getTom()),
                m.getInnvilgelsesresultat(),
                m.getDekning(),
                m.getMedlemskapstype(),
                new FastsattTrygdeavgift(
                    new BetalesAv(betalesAv.getOrgnr(), betalesAv.getRolle()),
                    avgiftForNorsk.map(n -> new Trygdeavgift(
                        fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
                        n.getTrygdeavgiftsbeløpMd(),
                        n.getTrygdesats(),
                        n.getAvgiftskode()
                    )).orElse(null),
                    avgiftForUtenlandsk.map(u -> new Trygdeavgift(
                        fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
                        u.getTrygdeavgiftsbeløpMd(),
                        u.getTrygdesats(),
                        u.getAvgiftskode()
                    )).orElse(null)
                )
            );
        }).toList();
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

    private RepresentantAvgift lagRepresentantAvgift(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.finnMedlemAvFolketrygden().map(m -> {
            var fastsattTrygdeavgift = m.getFastsattTrygdeavgift();
            Aktoer betalesAv = fastsattTrygdeavgift.getBetalesAv();

            String fnr = null;

            if (betalesAv.getRolle() == Aktoersroller.BRUKER) {
                fnr = persondataFasade.hentFolkeregisterident(betalesAv.getAktørId());
            }

            return new RepresentantAvgift(
                new Identifikator(
                    hasText(fnr) ? fnr : betalesAv.getOrgnr(),
                    hasText(fnr) ? BRUKER : ORGANISASJON
                ),
                fastsattTrygdeavgift.getRepresentantNr()
            );
        }).orElse(null);
    }
}
