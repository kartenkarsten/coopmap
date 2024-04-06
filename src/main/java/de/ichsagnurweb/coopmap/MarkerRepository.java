package de.ichsagnurweb.coopmap;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface MarkerRepository extends CrudRepository<Marker, Long> {

    Marker findById(long id);

    Iterable<Marker> findAllByMapId(String mapId);
}
