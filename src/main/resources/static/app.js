const stompClient = new StompJs.Client({
    //brokerURL: 'ws://localhost:8082/socket'
    brokerURL: 'ws://'+serverName+':'+serverPort+'/socket'
});

const map = L.map('map').setView([0, 0], 2);

if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(showPosition);
}
function showPosition(position) {
    map.flyTo(new L.LatLng(position.coords.latitude, position.coords.longitude), 10, {'animate':false});
}

stompClient.activate();

stompClient.onConnect = (frame) => {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/marker', (markerRaw) => {
        marker=JSON.parse(markerRaw.body);
        showMarker(marker.lat, marker.lon, marker.id, marker.name);
    });
    stompClient.subscribe('/topic/markers', (message) => {
        ms=JSON.parse(message.body);
        ms.forEach((marker) => {
            showMarker(marker.lat, marker.lon, marker.id, marker.name);
        });
    });
    stompClient.subscribe('/topic/*', function(message) {
            // Handle the received message
            const payload = JSON.parse(message.body);
            console.log('[debug] Received message:', payload);
    });

    // load already published markers
    stompClient.publish({destination: "/app/getMarkers"});
};



stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function publishMarker(lat, lon, id, name,) {
        stompClient.publish({
            destination: "/app/updateMarker",
            body: JSON.stringify({'id': id, 'name':name,'lat': lat, 'lon': lon})
        });
}

const markers = {};
function showMarker(lat, lon, id, name) {
    var marker = markers[id];

    if (null == marker) {
        // init marker
        marker = L.marker([lat, lon], {'title': name, 'draggable': true});
        marker.on('dragend', function(e) {
          console.log('marker dragend event');
          publishMarker(e.target._latlng.lat, e.target._latlng.lng, id, name);
        });
        markers[id] = marker;
        marker.addTo(map);
    } else {
        // update marker
        marker.setLatLng(new L.LatLng(lat, lon));
    }
}

const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
}).addTo(map);

function onMapClick(e) {
    if (stompClient.connected == true) {
        publishMarker(e.latlng.lat, e.latlng.lng);
    }else{
        console.log("can not create marker. Since Connection to server was not established now");
    }
}

map.on('click', onMapClick);