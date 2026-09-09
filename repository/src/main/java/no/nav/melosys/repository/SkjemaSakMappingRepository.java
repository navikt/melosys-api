package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.SkjemaSakMapping;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkjemaSakMappingRepository extends JpaRepository<SkjemaSakMapping, UUID> {

    List<SkjemaSakMapping> findBySkjemaIdIn(Collection<UUID> skjemaIds);

    Optional<SkjemaSakMapping> findBySkjemaId(UUID skjemaId);

    List<SkjemaSakMapping> findByMottatteOpplysninger_Id(Long mottatteOpplysningerId);

    boolean existsByFagsak_Saksnummer(String saksnummer);

    /**
     * Projeksjon for saksstatus-synk: henter kun feltene synken trenger, i én spørring.
     * Unngår å laste hele SkjemaSakMapping (originalData-CLOB) og Fagsak med EAGER-samlinger.
     *
     * <p>Ren fagsak-mapping per produkteierbeslutning 2026-07-21: utledet skjema-status er en
     * funksjon av fagsakstatus alene — behandlingsnivået inngår ikke. At en revurdering ikke
     * resetter ferdigbehandlede innsendinger håndheves i mottaket i melosys-skjema-api
     * (monotoni: AVSLUTTET nedgraderes aldri). Se plan-dokumentet «faglige-avklaringer» for
     * art13- og gjenbrukssak-spørsmålene.
     */
    String SAKSSTATUS_SYNK_PROJEKSJON =
        "select m.skjemaId as skjemaId, f.saksnummer as saksnummer, f.status as saksstatus "
            + "from SkjemaSakMapping m join m.fagsak f";

    /** Sortert på saksnummer slik at rader for samme sak sjelden krysser batch-grenser i massesynk. */
    @Query(SAKSSTATUS_SYNK_PROJEKSJON + " order by f.saksnummer")
    List<SaksstatusSynkRad> finnAlleSaksstatusSynkRader();

    /** Som {@link #finnAlleSaksstatusSynkRader()}, men for én sak — brukes av løpende synk. */
    @Query(SAKSSTATUS_SYNK_PROJEKSJON + " where f.saksnummer = :saksnummer")
    List<SaksstatusSynkRad> finnSaksstatusSynkRaderForSaksnummer(@Param("saksnummer") String saksnummer);

    interface SaksstatusSynkRad {
        UUID getSkjemaId();

        String getSaksnummer();

        Saksstatuser getSaksstatus();
    }
}
