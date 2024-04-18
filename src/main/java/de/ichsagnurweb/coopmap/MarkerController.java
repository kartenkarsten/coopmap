package de.ichsagnurweb.coopmap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;

@Controller
public class MarkerController {

	Map<Long, Marker> markers = new HashMap();

	@Value("${server.name:localhost}")
	private String serverName;

	@Value("${websocket.port:8082}")
	private String websocketPort;

	@Value("${websocket.protocol:ws}")
	private String websocketProtocol;

	@Autowired
	private MarkerRepository markerRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;


	@GetMapping("/map/{map_id}")
	public String yourPage(Model model, @PathVariable(required = false) String map_id) {
		if (null == map_id || map_id.isEmpty()) {
			map_id="demo";
		}
		// Pass a value to the Thymeleaf template
		model.addAttribute("serverName", serverName);
		model.addAttribute("websocketPort", websocketPort);
		model.addAttribute("websocketProtocol", websocketProtocol);
		model.addAttribute("mapId", map_id);

		return "index";
	}

	@GetMapping("/")
	public String welcomePage(Model model) {
		// TODO: create a welcome page to generate map urls
		return yourPage(model,"demo");
	}

	@MessageMapping("/{mapId}/deleteMarker")
	public void deleteMarker(@DestinationVariable("mapId") String mapId, @Payload Marker marker) throws Exception {
		Marker toDelete = null;
		Long id = marker.getId();
		if (null == id) {
			//should not happen
		}else{
			// update marker in map
			toDelete = markerRepository.findById(id).orElse(null);
			if (!mapId.equals(toDelete.getMapId())) {
				throw new IllegalArgumentException("Marker with Id '"+id+"' belongs to map id '"+toDelete+"' but was tried to delete from map with id '"+mapId+"'");
			}
			markerRepository.deleteById(id);
		}
		String destination = "/topic/" + mapId + "/markersToDelete"; // Construct dynamic destination
		messagingTemplate.convertAndSend(destination, Arrays.asList(toDelete));
	}

	@MessageMapping("/{mapId}/updateMarker")
	public void updateMarker(@DestinationVariable("mapId") String mapId, @Payload Marker marker) throws Exception {
		if (mapId != null && marker.getMapId() != null && !mapId.equals(marker.getMapId())) {
			throw new IllegalArgumentException("marker with mapId '"+marker.getMapId()+"' is not allowed to be saved in map with id '"+mapId+"'" );
		}
		marker.setMapId(mapId); // ensure only markers on the current map are changed
		Marker savedMarker = markerRepository.save(marker);

		String destination = "/topic/" + mapId + "/markers"; // Construct dynamic destination
		messagingTemplate.convertAndSend(destination,  Arrays.asList(savedMarker));
	}

	@MessageMapping("/{mapId}/getMarkers")
	public void getMarker(@DestinationVariable("mapId") String mapId) throws Exception {
		String destination = "/topic/" + mapId + "/markers"; // Construct dynamic destination
		messagingTemplate.convertAndSend(destination,  markerRepository.findAllByMapId(mapId));
	}

	@MessageMapping("/{mapId}/clearMap")
	public void clearMap(@DestinationVariable("mapId") String mapId) throws Exception {
		Iterable<Marker> markersToDelete = markerRepository.findAllByMapId(mapId);
		markerRepository.deleteAll(markersToDelete);

		String destination = "/topic/" + mapId + "/markersToDelete"; // Construct dynamic destination
		messagingTemplate.convertAndSend(destination,  markersToDelete);
	}


}
