package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.behandling.Behandlingsresultat;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VilkaarsresultatRepository extends JpaRepository<Vilkaarsresultat, Long> {

    List<Vilkaarsresultat> findByBehandlingsresultatId(long ID);

    Optional<Vilkaarsresultat> findByBehandlingsresultatIdAndVilkaar(long ID, Vilkaar vilkaar);

    boolean existsByBehandlingsresultatIdAndVilkaarAndOppfyltTrue(long ID, Vilkaar vilkaar);

    void deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);

    void deleteByBehandlingsresultatAndVilkaarNotIn(Behandlingsresultat behandlingsresultat, Collection<Vilkaar> vilkaar);
}
