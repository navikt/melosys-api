package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MDCOperationsTest {

    @Test
    public void test_generateCallId() {

        String callId1 = MDCOperations.generateCallId();

        assertThat(callId1).isNotNull();

        String callId2 = MDCOperations.generateCallId();

        assertThat(callId2).isNotNull().isNotEqualTo(callId1);
    }

    @Test
    public void test_mdc() {
        MDCOperations.putToMDC("myKey", "myValue");

        assertThat(MDCOperations.getFromMDC("myKey")).isEqualTo("myValue");

        MDCOperations.remove("myKey");

        assertThat(MDCOperations.getFromMDC("myKey")).isNull();
    }
}