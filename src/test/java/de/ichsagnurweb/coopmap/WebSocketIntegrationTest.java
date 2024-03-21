package de.ichsagnurweb.coopmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.repository.support.Repositories;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "port=0")
@ActiveProfiles(value = "test")
public class WebSocketIntegrationTest {


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebSocketStompClient stompClient;

    @Autowired
    private MarkerRepository markerRepository;


    @LocalServerPort
    private int port;


    @Test
    public void shouldAssignIdToNewMarker() throws Exception {
        BlockingQueue<Marker> blockingQueue = new LinkedBlockingDeque<>();

        String wsUrl = "ws://localhost:" + port + "/socket";
        StompSession session = stompClient.connect(
                        wsUrl, new StompSessionHandlerAdapter() {})
                .get(30, TimeUnit.SECONDS);

        session.subscribe("/topic/markers", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders stompHeaders) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders stompHeaders, Object payload) {
                try {
                    List<Marker> markers = objectMapper.readValue(new String((byte[]) payload, StandardCharsets.UTF_8), new TypeReference<List<Marker>>() {});
                    blockingQueue.addAll(markers);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Marker marker = new Marker("test", 0.0, 0.0);

        String message = objectMapper.writeValueAsString(marker);
        session.send("/app/updateMarker", message.getBytes());

        Marker receivedMessage = blockingQueue.poll(15, SECONDS);
        assertThat(markerRepository.findAll()).size().isEqualTo(1);
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getId()).isNotNull();
        assertThat(receivedMessage.getName()).isEqualTo(marker.getName());
        assertThat(receivedMessage.getLat()).isEqualTo(marker.getLat());
        assertThat(receivedMessage.getLon()).isEqualTo(marker.getLon());
    }

    @Test
    public void shouldGetAllMarkers() throws Exception {
        BlockingQueue<Marker> blockingQueue = new LinkedBlockingDeque<>();

        String wsUrl = "ws://localhost:" + port + "/socket";
        StompSession session = stompClient.connect(
                        wsUrl, new StompSessionHandlerAdapter() {})
                .get(30, TimeUnit.SECONDS);

        session.subscribe("/topic/markers", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders stompHeaders) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders stompHeaders, Object payload) {
                try {
                    List<Marker> markers = objectMapper.readValue(new String((byte[]) payload, StandardCharsets.UTF_8), new TypeReference<List<Marker>>() {});
                    blockingQueue.addAll(markers);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Marker marker = new Marker("test-getall", 0.0, 0.0);
        Marker savedmarker = markerRepository.save(marker);

        String message = objectMapper.writeValueAsString(marker);
        session.send("/app/getMarkers", null);

        Marker receivedMessage = blockingQueue.poll(15, SECONDS);

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getId()).isNotNull();
        assertThat(receivedMessage.getName()).isEqualTo(savedmarker.getName());
        assertThat(receivedMessage.getLat()).isEqualTo(savedmarker.getLat());
        assertThat(receivedMessage.getLon()).isEqualTo(savedmarker.getLon());
    }
}