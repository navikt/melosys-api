package no.nav.melosys.service.altinn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.MoreCollectors;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.Representant;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.FullmektigDto;
import no.nav.melosys.service.sak.OpprettSakRequest;
import no.nav.melosys.soknad_altinn.Kontaktperson;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class AltinnSoeknadService {
    private final SoknadMottakConsumer soknadMottakConsumer;
    private final FagsakService fagsakService;
    private final MottatteOpplysningerService mottatteOpplysningerService;
    private final PersondataFasade persondataFasade;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    public AltinnSoeknadService(SoknadMottakConsumer soknadMottakConsumer,
                                FagsakService fagsakService,
                                MottatteOpplysningerService mottatteOpplysningerService,
                                PersondataFasade persondataFasade,
                                AvklarteVirksomheterService avklarteVirksomheterService) {
        this.soknadMottakConsumer = soknadMottakConsumer;
        this.fagsakService = fagsakService;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
        this.persondataFasade = persondataFasade;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    public Behandling opprettFagsakOgBehandlingFraAltinnSøknad(String søknadReferanse) {
        final MedlemskapArbeidEOSM søknad = soknadMottakConsumer.hentSøknad(søknadReferanse);
        final LocalDate mottaksdato = hentMottaksdato(søknadReferanse);

        final Fagsak fagsak = fagsakService.nyFagsakOgBehandling(lagOpprettSakRequest(søknad, mottaksdato));
        final Behandling behandling = fagsak.hentAktivBehandling();

        mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
            behandling.getId(),
            getXml(søknad),
            SoeknadMapper.lagSoeknad(søknad),
            søknadReferanse
        );
        avklarteVirksomheterService.lagreVirksomhetSomAvklartfakta(hentArbeidsgiverID(søknad), behandling.getId());

        return behandling;
    }

    private OpprettSakRequest lagOpprettSakRequest(MedlemskapArbeidEOSM søknad, LocalDate mottaksdato) {
        return new OpprettSakRequest.Builder()
            .medSakstype(Sakstyper.EU_EOS)
            .medSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG)
            .medAktørID(hentAktørID(søknad))
            .medUtenlandskPersonId(hentUtenlandskPersonId(søknad))
            .medArbeidsgiver(hentArbeidsgiverID(søknad))
            .medRepresentant(hentRepresentant(søknad))
            .medFullmektig(hentFullmektig(søknad))
            .medKontaktopplysninger(hentKontaktopplysninger(søknad))
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.SØKNAD)
            .medMottaksdato(mottaksdato)
            .medBehandlingstema(avklarBehandlingstema(søknad))
            .medBehandlingstype(Behandlingstyper.FØRSTEGANG)
            .build();
    }

    private static String getXml(MedlemskapArbeidEOSM søknad) {
        String søknadXml;
        try {
            søknadXml = new XmlMapper().writeValueAsString(søknad);
        } catch (JsonProcessingException e) {
            throw new TekniskException(e);
        }
        return søknadXml;
    }

    private Behandlingstema avklarBehandlingstema(MedlemskapArbeidEOSM søknad) {
        if (Boolean.TRUE.equals(søknad.getInnhold().getArbeidsgiver().isOffentligVirksomhet())) {
            return Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY;
        } else {
            return Behandlingstema.UTSENDT_ARBEIDSTAKER;
        }
    }

    public Collection<AltinnDokument> hentDokumenterTilknyttetSoknad(String søknadReferanse) {
        return soknadMottakConsumer.hentDokumenter(søknadReferanse);
    }

    private LocalDate hentMottaksdato(String søknadReferanse) {
        return hentDokumenterTilknyttetSoknad(søknadReferanse).stream()
            .filter(AltinnDokument::erSøknad)
            .map(AltinnDokument::getInnsendtTidspunkt)
            .map(dokument -> LocalDate.ofInstant(dokument, ZoneId.systemDefault()))
            .collect(MoreCollectors.onlyElement());
    }

    private String hentAktørID(MedlemskapArbeidEOSM søknad) {
        if (StringUtils.isBlank(søknad.getInnhold().getArbeidstaker().getFoedselsnummer())) {
            throw new FunksjonellException("Søknader fra Altinn må inneholde fnr.");
        }
        return persondataFasade.hentAktørIdForIdent(søknad.getInnhold().getArbeidstaker().getFoedselsnummer());
    }

    private String hentUtenlandskPersonId(MedlemskapArbeidEOSM søknad) {
        return søknad.getInnhold().getArbeidstaker().getUtenlandskIDnummer();
    }

    private static String hentArbeidsgiverID(MedlemskapArbeidEOSM søknad) {
        return søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
    }

    private static Representant hentRepresentant(MedlemskapArbeidEOSM søknad) {
        if (rådgivningsfirmaErFullmektig(søknad)) {
            String fullmektigVirksomhetsnummer = søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
            return new Representant(fullmektigVirksomhetsnummer, hentRepresenterer(søknad));
        } else {
            return arbeidstakerHarGittFullmakt(søknad)
                ? new Representant(hentArbeidsgiverID(søknad), hentRepresenterer(søknad)) : null;
        }
    }

    private FullmektigDto hentFullmektig(MedlemskapArbeidEOSM søknad) {
        if (rådgivningsfirmaErFullmektig(søknad)) {
            String fullmektigVirksomhetsnummer = søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
            return new FullmektigDto(fullmektigVirksomhetsnummer, null, hentFullmakter(søknad));
        } else if (arbeidstakerHarGittFullmakt(søknad)) {
            return new FullmektigDto(hentArbeidsgiverID(søknad), null, hentFullmakter(søknad));
        } else {
            return null;
        }
    }

    private static boolean rådgivningsfirmaErFullmektig(MedlemskapArbeidEOSM søknad) {
        return StringUtils.isNotBlank(søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer());
    }

    private static Representerer hentRepresenterer(MedlemskapArbeidEOSM søknad) {
        if (arbeidstakerHarGittFullmakt(søknad)) {
            return Representerer.BEGGE;
        } else {
            return Representerer.ARBEIDSGIVER;
        }
    }

    private static List<Fullmaktstype> hentFullmakter(MedlemskapArbeidEOSM søknad) {
        if (arbeidstakerHarGittFullmakt(søknad)) {
            return List.of(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
        } else {
            return List.of(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
        }
    }

    private static boolean arbeidstakerHarGittFullmakt(MedlemskapArbeidEOSM søknad) {
        return Boolean.TRUE.equals(søknad.getInnhold().getFullmakt().isFullmaktFraArbeidstaker());
    }

    private static List<Kontaktopplysning> hentKontaktopplysninger(MedlemskapArbeidEOSM søknad) {
        Kontaktopplysning kontaktopplysning = hentKontaktopplysning(søknad);
        return kontaktopplysning != null ? List.of(kontaktopplysning) : Collections.emptyList();
    }

    private static Kontaktopplysning hentKontaktopplysning(MedlemskapArbeidEOSM søknad) {
        Kontaktperson kontaktperson = søknad.getInnhold().getArbeidsgiver().getKontaktperson();
        if (kontaktperson == null) {
            return null;
        }
        String kontaktpersonNavn = kontaktperson.getKontaktpersonNavn();
        String kontaktpersonTelefon = kontaktperson.getKontaktpersonTelefon();
        return StringUtils.isNotBlank(kontaktpersonNavn) || StringUtils.isNotBlank(kontaktpersonTelefon)
            ? Kontaktopplysning.av(hentKontaktVirksomhetsnummer(søknad), kontaktpersonNavn, kontaktpersonTelefon, null) : null;
    }

    private static String hentKontaktVirksomhetsnummer(MedlemskapArbeidEOSM søknad) {
        if (rådgivningsfirmaErFullmektig(søknad)) {
            return søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
        }
        return søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
    }
}
