package de.ichsagnurweb.coopmap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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


	@GetMapping("/")
	public String yourPage(Model model) {
		// Pass a value to the Thymeleaf template
		model.addAttribute("serverName", serverName);
		model.addAttribute("websocketPort", websocketPort);
		model.addAttribute("websocketProtocol", websocketProtocol);

		return "index";
	}

	@MessageMapping("/deleteMarker")
	@SendTo("/topic/markersToDelete")
	public List<Marker> deleteMarker(Marker marker) throws Exception {
		Marker toDelete = null;
		Long id = marker.getId();
		if (null == id) {
			//should not happen
		}else{
			// update marker in map
			toDelete = markerRepository.findById(id);
			markerRepository.deleteById(id);
		}
		return Arrays.asList(toDelete);
	}

	@MessageMapping("/updateMarker")
	@SendTo("/topic/markers")
	public List<Marker> updateMarker(Marker marker) throws Exception {
		Marker savedMarker = markerRepository.save(marker);
		return Arrays.asList(savedMarker);
	}

	@MessageMapping("/getMarkers")
	@SendTo("/topic/markers")
	public Iterable<Marker> getMarker() throws Exception {
		return markerRepository.findAll();
	}

	@MessageMapping("/clearMap")
	@SendTo("/topic/markersToDelete")
	public Iterable<Marker> clearMap() throws Exception {
		Iterable<Marker> markersToDelete = markerRepository.findAll();
		markerRepository.deleteAll();
		return markersToDelete;
	}


}
