const stompClient = new StompJs.Client({
    //brokerURL: 'ws://localhost:8082/socket'
    brokerURL: websocketProtocol+'://'+serverName+':'+websocketPort+'/socket'
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
    stompClient.subscribe('/topic/markers', (message) => {
        ms=JSON.parse(message.body);
        ms.forEach((marker) => {
            showMarker(marker.lat, marker.lon, marker.id, marker.name, marker.description);
        });
    });
    stompClient.subscribe('/topic/markersToDelete', (message) => {
        ms=JSON.parse(message.body);
        ms.forEach((marker) => {
            deleteMarker(marker.id);
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

function publishMarker(lat, lon, id, name, desc) {
        stompClient.publish({
            destination: "/app/updateMarker",
            body: JSON.stringify({'id': id, 'name':name,'lat': lat, 'lon': lon, 'description': desc})
        });
}

function publishMarkerDeletion(id) {
        stompClient.publish({
            destination: "/app/deleteMarker",
            body: JSON.stringify({'id': id})
        });
}

function publishClearMap() {
        stompClient.publish({
            destination: "/app/clearMap",
            body: JSON.stringify({})
        });
}

const markers = {};
function deleteMarker(id) {
    markers[id].marker.removeFrom(map);
    markers[id] = null;
}

function showMarker(lat, lon, id, name, desc) {
    var marker = markers[id];

    if (null == marker) {
        // init marker
        var leafleatObject = L.marker([lat, lon], {'title': name, 'draggable': true}).on('click', function (e) {
            L.DomEvent.stopPropagation(e);
            showMarkerDetails(id);
        });
        leafleatObject.on('dragend', function(e) {
          console.log('marker dragend event');
          publishMarker(e.target._latlng.lat, e.target._latlng.lng, id, markers[id].name, markers[id].desc);
        });
        marker={};
        marker.marker = leafleatObject;
        markers[id] = marker;
        leafleatObject.addTo(map);

    } else {
        // update marker
        marker.marker.setLatLng(new L.LatLng(lat, lon));
    }
    marker.lat = lat;
    marker.lon = lon;
    marker.name = name;
    marker.desc = desc;
}

function markerDetailsSave() {
    var id = $("#markerDetails :input#id").val();
    var name = $("#markerDetails :input#name").val();
    var desc = $("#markerDetails :input#description").val();
    publishMarker(markers[id].lat, markers[id].lon, id, name, desc);
    $("#markerDetails").hide();
}

function showMarkerDetails(id) {
    //TODO save metadata in markers list like name, desciption
    $("#markerDetails :input#id").val(id);
    var marker = markers[id];

    $( "#name" ).val(marker.name);
    $( "#description" ).val(marker.desc);
    $("#markerDetails").show();
}


const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
}).addTo(map);

function onMapClick(e) {
    map.panTo(e.latlng);
    if (stompClient.connected == true) {
        publishMarker(e.latlng.lat, e.latlng.lng);
    }else{
        console.log("can not create marker. Since Connection to server was not established now");
    }
}

map.on('click', onMapClick);