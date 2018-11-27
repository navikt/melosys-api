package no.nav.melosys.tjenester.gui.config;

import javax.ws.rs.ApplicationPath;

import no.nav.melosys.tjenester.gui.*;
import no.nav.melosys.tjenester.gui.unntakshandtering.FunksjonellExceptionMapper;
import no.nav.melosys.tjenester.gui.unntakshandtering.IkkeFunnetExceptionMapper;
import no.nav.melosys.tjenester.gui.unntakshandtering.SikkerhetsbegrensningExceptionMapper;
import no.nav.melosys.tjenester.gui.unntakshandtering.TekniskExceptionMapper;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        setApplicationName("melosys-api");
        property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);

        // N.B. alfabetisk rekkefølge
        register(AvklartefaktaTjeneste.class);
        register(ArbeidsforholdHistorikkTjeneste.class);
        register(DokumentTjeneste.class);
        register(FagsakTjeneste.class);
        register(FrontendLoggingTjeneste.class);
        register(InngangTjeneste.class);
        register(JournalfoeringTjeneste.class);
        register(KodeverkTjeneste.class);
        register(LovvalgsperiodeTjeneste.class);
        register(OppgaveTjeneste.class);
        register(OrganisasjonTjeneste.class);
        register(PersonTjeneste.class);
        register(RyddOppgaverTjeneste.class);
        register(SaksbehandlerTjeneste.class);
        register(SaksopplysningTjeneste.class);
        register(SoeknadTjeneste.class);
        register(VedtakTjeneste.class);
        register(VilkaarTjeneste.class);

        // Globale ExceptionMappers (implisitt unntakshåndtering for hyppig
        // forekommende unntak i REST-grensesnittet).
        register(IkkeFunnetExceptionMapper.class);
        register(SikkerhetsbegrensningExceptionMapper.class);
        register(TekniskExceptionMapper.class);
        register(FunksjonellExceptionMapper.class);
    }
}