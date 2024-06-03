package de.ichsagnurweb.coopmap;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleDataRestController {

    @Autowired
    private MarkerRepository markerRepository;

    @PostMapping("/api/map/{mapId}/sampledata")
    void createSampleData(@PathVariable(value="mapId") String mapId) {

        Marker bahnhof = new Marker(mapId, "Bahnhof Lüneburg", 10.41954, 53.24989);
        markerRepository.save(bahnhof);
    }
}
