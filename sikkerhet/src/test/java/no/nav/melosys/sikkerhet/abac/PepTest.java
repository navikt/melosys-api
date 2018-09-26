package no.nav.melosys.sikkerhet.abac;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PepTest {

    @InjectMocks
    public AbacDefaultConfig abacDefaultConfig;

    @Mock
    SubjectHandler subjectHandler;

    @Test
    public void testExtractTokenBodyWhenEpmty() {
        assertEquals("", abacDefaultConfig.getOidcTokenBody());
    }
}
