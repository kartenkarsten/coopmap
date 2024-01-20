package de.ichsagnurweb.coopmap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
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

	@Value("${server.port}")
	private String serverPort;

	@GetMapping("/")
	public String yourPage(Model model) {
		// Pass a value to the Thymeleaf template
		model.addAttribute("serverName", serverName);
		model.addAttribute("serverPort", serverPort);

		return "index";
	}

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/updateMarker")
//	@SendTo("/topic/counts")
	public void updateMarker(Marker marker) throws Exception {
		Integer id = marker.getId();
		if (null == id) {
			id = calcNewId();
			marker.setId(id);
			markers.put(id, marker);
		}else{
			// update marker in map
			markers.put(id, marker);
		}
		Marker payload = markers.get(id);
		messagingTemplate.convertAndSend("/topic/marker", payload);
	}

	@MessageMapping("/getMarkers")
//	@SendTo("/topic/counts")
	public void getMarker() throws Exception {
//		Markers payload = new Markers(this.markers.values());
		List<Marker> payload = this.markers.values().stream().toList();
		messagingTemplate.convertAndSend("/topic/markers", payload);
	}


	private Integer calcNewId() {
		Optional<Integer> max = markers.keySet().stream().reduce(Integer::max);
		return max.orElse(-1)+1;
	}

}
