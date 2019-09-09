package no.nav.melosys.integrasjon.eessi;

import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerImplTest {

    @Mock
    private RestTemplate restTemplate;

    private EessiConsumer eessiConsumer;

    @Captor
    private ArgumentCaptor<HttpEntity> httpEntityCaptor;
    @Captor
    private ArgumentCaptor<ParameterizedTypeReference> parameterizedTypeReferenceCaptor;

    @Before
    public void setup() {
        eessiConsumer = new EessiConsumerImpl(restTemplate);
    }

    @Test
    public void genererSedForhåndsvisning() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenReturn(new ResponseEntity(PDF, HttpStatus.OK));

        SedDataDto sedDataDto = new SedDataDto();
        byte[] pdf = eessiConsumer.genererSedForhåndsvisning(sedDataDto, SedType.A001);

        verify(restTemplate).exchange(eq("/sed/A001/pdf"), eq(HttpMethod.POST), httpEntityCaptor.capture(), parameterizedTypeReferenceCaptor.capture());
        assertThat(pdf).isEqualTo(PDF);
        assertThat(httpEntityCaptor.getValue().getBody()).isEqualTo(sedDataDto);
        assertThat(parameterizedTypeReferenceCaptor.getValue().getType().getTypeName()).isEqualTo(byte[].class.getSimpleName());
    }
}