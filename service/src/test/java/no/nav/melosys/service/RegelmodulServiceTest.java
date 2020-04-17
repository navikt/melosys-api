package no.nav.melosys.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class RegelmodulServiceTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private RegelmodulService regelmodulService;

    @Before
    public void setUp() throws ParserConfigurationException {
        regelmodulService = new RegelmodulService("", behandlingRepository, saksopplysningerService);
    }

    @Test
    public void fastsettLovvalg_behandlingIkkeFunnet() {
        when(behandlingRepository.findWithSaksopplysningerById(0L)).thenReturn(null);

        FastsettLovvalgReply fastsettLovvalgReply = regelmodulService.fastsettLovvalg(0L);
        assertThat(fastsettLovvalgReply).isNull();
    }

    @Test
    public void lagRequest() {
        Behandling behandling = new Behandling();

        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning arbeidsforhold = new Saksopplysning();
        arbeidsforhold.setType(SaksopplysningType.ARBFORH);
        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument();
        arbeidsforhold.setDokument(arbeidsforholdDokument);
        saksopplysninger.add(arbeidsforhold);

        Saksopplysning inntekt = new Saksopplysning();
        inntekt.setType(SaksopplysningType.INNTK);
        InntektDokument inntektDokument = new InntektDokument();
        inntekt.setDokument(inntektDokument);
        saksopplysninger.add(inntekt);

        Saksopplysning medl = new Saksopplysning();
        medl.setType(SaksopplysningType.MEDL);
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        medl.setDokument(medlemskapDokument);
        saksopplysninger.add(medl);

        Saksopplysning org = new Saksopplysning();
        org.setType(SaksopplysningType.ORG);
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        org.setDokument(organisasjonDokument);
        saksopplysninger.add(org);

        Saksopplysning person = new Saksopplysning();
        person.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        person.setDokument(personDokument);
        saksopplysninger.add(person);

        behandling.setSaksopplysninger(saksopplysninger);

        FastsettLovvalgRequest fastsettLovvalgRequest = lagRequest(behandling);

        assertThat(fastsettLovvalgRequest.arbeidsforholdDokumenter.get(0)).isEqualTo(arbeidsforholdDokument);
        assertThat(fastsettLovvalgRequest.inntektDokumenter.get(0)).isEqualTo(inntektDokument);
        assertThat(fastsettLovvalgRequest.medlemskapDokumenter.get(0)).isEqualTo(medlemskapDokument);
        assertThat(fastsettLovvalgRequest.organisasjonDokumenter.get(0)).isEqualTo(organisasjonDokument);
        assertThat(fastsettLovvalgRequest.personopplysningDokument).isEqualTo(personDokument);
    }

    private FastsettLovvalgRequest lagRequest(Behandling behandling) {
        FastsettLovvalgRequest fastsettLovvalgRequest = new FastsettLovvalgRequest();
        fastsettLovvalgRequest.arbeidsforholdDokumenter = new ArrayList<>();
        fastsettLovvalgRequest.inntektDokumenter = new ArrayList<>();
        fastsettLovvalgRequest.medlemskapDokumenter = new ArrayList<>();
        fastsettLovvalgRequest.organisasjonDokumenter = new ArrayList<>();

        for (Saksopplysning saksopplysning : behandling.getSaksopplysninger()) {
            SaksopplysningType type = saksopplysning.getType();
            SaksopplysningDokument dokument = saksopplysning.getDokument();

            switch (type) {
                case ARBFORH:
                    fastsettLovvalgRequest.arbeidsforholdDokumenter.add((ArbeidsforholdDokument)dokument);
                    break;
                case INNTK:
                    fastsettLovvalgRequest.inntektDokumenter.add((InntektDokument)dokument);
                    break;
                case MEDL:
                    fastsettLovvalgRequest.medlemskapDokumenter.add((MedlemskapDokument)dokument);
                    break;
                case ORG:
                    fastsettLovvalgRequest.organisasjonDokumenter.add((OrganisasjonDokument)dokument);
                    break;
                case PERSOPL:
                    fastsettLovvalgRequest.personopplysningDokument = (PersonDokument) dokument;
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type.getKode() + " ikke støttet.");
            }
        }

        return fastsettLovvalgRequest;
    }

    @Test
    @SuppressWarnings("ALL")
    public void unmarshallFastsettLovvalgReply() throws JAXBException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream("fastsett-lovvalg-reply.xml");

        JAXBContext context = JAXBContext.newInstance(FastsettLovvalgReply.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        FastsettLovvalgReply reply = (FastsettLovvalgReply) unmarshaller.unmarshal(kilde);

        assertThat(reply).isNotNull();
        assertThat(reply.feilmeldinger).isNotEmpty();
        assertThat(reply.lovvalgsbestemmelser).isNotEmpty();
    }
}