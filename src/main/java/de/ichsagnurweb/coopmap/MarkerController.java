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

	Map<Integer, Marker> markers = new HashMap();

	@Value("${server.name:localhost}")
	private String serverName;

	@Value("${websocket.port:8082}")
	private String websocketPort;

	@Value("${websocket.protocol:ws}")
	private String websocketProtocol;

	@GetMapping("/")
	public String yourPage(Model model) {
		// Pass a value to the Thymeleaf template
		model.addAttribute("serverName", serverName);
		model.addAttribute("websocketPort", websocketPort);
		model.addAttribute("websocketProtocol", websocketProtocol);

		return "index";
	}

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/deleteMarker")
	@SendTo("/topic/markersToDelete")
	public List<Marker> deleteMarker(Marker marker) throws Exception {
		Marker toDelete = null;
		Integer id = marker.getId();
		if (null == id) {
			//should not happen
		}else{
			// update marker in map
			toDelete = markers.remove(id);
		}
		return Arrays.asList(toDelete);
	}

	@MessageMapping("/updateMarker")
	@SendTo("/topic/markers")
	public List<Marker> updateMarker(Marker marker) throws Exception {
		Integer id = marker.getId();
		if (null == id) {
			id = calcNewId();
			marker.setId(id);
			markers.put(id, marker);
		}else{
			// update marker in map
			markers.put(id, marker);
		}
		return Arrays.asList(markers.get(id));
	}

	@MessageMapping("/getMarkers")
	@SendTo("/topic/markers")
	public List<Marker> getMarker() throws Exception {
		return this.markers.values().stream().toList();
	}

	@MessageMapping("/clearMap")
	@SendTo("/topic/markersToDelete")
	public List<Marker> clearMap() throws Exception {
		List<Marker> markersToDelete = this.markers.values().stream().toList();
		this.markers.clear();
		return markersToDelete;
	}


	private Integer calcNewId() {
		Optional<Integer> max = markers.keySet().stream().reduce(Integer::max);
		return max.orElse(-1)+1;
	}

}
