package no.nav.melosys.service.dokument.sed.bygger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigArbeid;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.SedData;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SedDataByggerTest {

    private SedDataBygger dataBygger;
    private Behandling behandling;

    class StubSedDataBygger extends SedDataBygger {

        StubSedDataBygger(KodeverkService kodeverkService,
                          RegisterOppslagService registerOppslagService,
                          LovvalgsperiodeService lovvalgsperiodeService,
                          AvklartefaktaService avklartefaktaService) {
            super(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
        }
    }

    private class SedDataImpl extends SedData{}

    @Before
    @SuppressWarnings("unchecked")
    public void setup()
        throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landKode = "Land";

        HashSet<OrganisasjonDokument> orgDokumentHashSet = new HashSet<>();

        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.organisasjonDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonDokument.organisasjonDetaljer.hentStrukturertForretningsadresse()).thenReturn(strukturertAdresse);
        orgDokumentHashSet.add(organisasjonDokument);
        when(registerOppslagService.hentOrganisasjoner(any(Set.class))).thenReturn(orgDokumentHashSet);

        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        behandling = new Behandling();
        behandling.setSaksopplysninger(saksopplysninger);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.adresse = strukturertAdresse;
        foretakUtland.orgnr = "orgnr";
        foretakUtland.navn = "navn foretak";

        Saksopplysning saksopplysning = new Saksopplysning();
        SoeknadDokument søknadDokument = new SoeknadDokument();
        søknadDokument.selvstendigArbeid = new SelvstendigArbeid();
        søknadDokument.foretakUtland = Lists.newArrayList(foretakUtland);
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = "12312312";
        søknadDokument.selvstendigArbeid.selvstendigForetak = Collections.singletonList(selvstendigForetak);
        søknadDokument.selvstendigArbeid.erSelvstendig = true;
        saksopplysning.setDokument(søknadDokument);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysninger.add(saksopplysning);

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBEIDSFORHOLD);
        saksopplysning.setDokument(new ArbeidsforholdDokument());
        saksopplysninger.add(saksopplysning);

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning.setDokument(new PersonDokument());
        saksopplysninger.add(saksopplysning);

        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);

        dataBygger = new StubSedDataBygger(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak()
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        SedData sedData = dataBygger.lag(behandling, new SedDataImpl());

        assertNotNull(sedData);
        assertNotNull(sedData.getPersonDokument());
        assertNotNull(sedData.getArbeidsgivendeVirkomsheter());
        assertNotNull(sedData.getArbeidssteder());
        assertNotNull(sedData.getBostedsadresse());
        assertNotNull(sedData.getSelvstendigeVirksomheter());
        assertNotNull(sedData.getSøknadDokument());
        assertNotNull(sedData.getUtenlandskeVirksomheter());

        assertFalse(sedData.getArbeidsgivendeVirkomsheter().isEmpty());
        assertEquals("Land", sedData.getArbeidsgivendeVirkomsheter().get(0).adresse.landKode);
    }
}
