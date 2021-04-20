package no.nav.melosys.service.altinn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fullmektig;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import no.nav.melosys.soknad_altinn.Kontaktperson;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AltinnSoeknadService {
    private final SoknadMottakConsumer soknadMottakConsumer;
    private final FagsakService fagsakService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final PersondataFasade persondataFasade;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    public AltinnSoeknadService(SoknadMottakConsumer soknadMottakConsumer,
                                FagsakService fagsakService,
                                BehandlingsgrunnlagService behandlingsgrunnlagService,
                                @Qualifier("system") PersondataFasade persondataFasade,
                                AvklarteVirksomheterService avklarteVirksomheterService) {
        this.soknadMottakConsumer = soknadMottakConsumer;
        this.fagsakService = fagsakService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.persondataFasade = persondataFasade;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    public Behandling opprettFagsakOgBehandlingFraAltinnSøknad(String søknadReferanse) throws FunksjonellException, TekniskException {
        final MedlemskapArbeidEOSM søknad = soknadMottakConsumer.hentSøknad(søknadReferanse);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medSakstype(Sakstyper.EU_EOS)
            .medAktørID(hentAktørID(søknad))
            .medUtenlandskPersonId(hentUtenlandskPersonId(søknad))
            .medArbeidsgiver(hentArbeidsgiverID(søknad))
            .medFullmektig(hentFullmektig(søknad))
            .medKontaktopplysninger(hentKontaktopplysninger(søknad))
            .medBehandlingstema(avklarBehandlingstema(søknad))
            .medBehandlingstype(Behandlingstyper.SOEKNAD)
            .build();

        final Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        final Behandling behandling = fagsak.hentAktivBehandling();
        String søknadXml;
        try {
            søknadXml = new XmlMapper().writeValueAsString(søknad);
        } catch (JsonProcessingException e) {
            throw new TekniskException(e);
        }
        behandlingsgrunnlagService.opprettSøknadUtsendteArbeidstakereEøs(
            behandling.getId(),
            søknadXml,
            SoeknadMapper.lagSoeknad(søknad),
            søknadReferanse
        );
        avklarteVirksomheterService.lagreVirksomhetSomAvklartfakta(hentArbeidsgiverID(søknad), behandling.getId());

        return behandling;
    }

    private static Behandlingstema avklarBehandlingstema(MedlemskapArbeidEOSM søknad) {
        if (Boolean.TRUE.equals(søknad.getInnhold().getArbeidsgiver().isOffentligVirksomhet())) {
            return Behandlingstema.ARBEID_ETT_LAND_ØVRIG;
        } else {
            return Behandlingstema.UTSENDT_ARBEIDSTAKER;
        }
    }

    public Collection<AltinnDokument> hentDokumenterTilknyttetSoknad(String søknadReferanse) {
        return soknadMottakConsumer.hentDokumenter(søknadReferanse);
    }

    private String hentAktørID(MedlemskapArbeidEOSM søknad) throws FunksjonellException {
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

    private static Fullmektig hentFullmektig(MedlemskapArbeidEOSM søknad) {
        if (rådgivningsfirmaErFullmektig(søknad)) {
            String fullmektigVirksomhetsnummer = søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
            return new Fullmektig(fullmektigVirksomhetsnummer, hentRepresenterer(søknad));
        } else {
            return arbeidstakerHarGittFullmakt(søknad)
                ? new Fullmektig(hentArbeidsgiverID(søknad), hentRepresenterer(søknad)) : null;
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
            ? Kontaktopplysning.av(hentKontaktVirksomhetsnummer(søknad), kontaktpersonNavn, kontaktpersonTelefon) : null;
    }

    private static String hentKontaktVirksomhetsnummer(MedlemskapArbeidEOSM søknad){
        if (rådgivningsfirmaErFullmektig(søknad)) {
            return søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
        }
        return søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
    }
}
