package de.ichsagnurweb.coopmap;

import org.springframework.data.repository.CrudRepository;

public interface MarkerRepository extends CrudRepository<Marker, Long> {

    Marker findById(long id);
}
