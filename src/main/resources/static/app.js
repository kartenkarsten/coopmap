const stompClient = new StompJs.Client({
    //brokerURL: 'ws://localhost:8082/socket'
    brokerURL: websocketProtocol+'://'+serverName+':'+websocketPort+'/socket'
});

const enableMultiDrag = false;
const map = L.map('map').setView([0, 0], 2);

//set the correct headline
$("#top span").text("Collaborative Map "+mapId);

if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(showPosition);
}
function showPosition(position) {
    map.flyTo(new L.LatLng(position.coords.latitude, position.coords.longitude), 10, {'animate':false});
}

stompClient.onConnect = (frame) => {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/'+mapId+'/markers', (message) => {
        ms=JSON.parse(message.body);
        ms.forEach((marker) => {
            showMarker(marker.lat, marker.lon, marker.id, marker.name, marker.description);
        });
    });
    stompClient.subscribe('/topic/'+mapId+'/markersToDelete', (message) => {
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
    stompClient.publish({destination: "/app/"+mapId+"/getMarkers"});
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

stompClient.activate();
updateMapHeight();

////////////////////////////////////////////////////////////////////////////
function publishMarker(lat, lon, id, name, desc) {
        stompClient.publish({
            destination: "/app/"+mapId+"/updateMarker",
            body: JSON.stringify({'id': id, 'name':name,'lat': lat, 'lon': lon, 'description': desc})
        });
}

function publishMarkerDeletion(id) {
        stompClient.publish({
            destination: "/app/"+mapId+"/deleteMarker",
            body: JSON.stringify({'id': id})
        });
}

function publishClearMap() {
        stompClient.publish({
            destination: "/app/"+mapId+"/clearMap",
            body: JSON.stringify({})
        });
}

////////////////////////////////////////////////////////////////////////////

const markers = {};
function deleteMarker(id) {
    markers[id].marker.removeFrom(map);
    markers[id] = null;
}

function showMarker(lat, lon, id, name, desc) {
    var marker = markers[id];

    if (null == marker) {
        // init marker
        var leafleatObject = L.marker([lat, lon], {'title': name, 'draggable': enableMultiDrag});
        marker={};
        marker.marker = leafleatObject;
        markers[id] = marker;

        leafleatObject.on('click', function (e) {
            L.DomEvent.stopPropagation(e);
            if ($("#markerDetails :input#id").val() == id) {
                // unselect this marker
                markerDetailsHide();

            }else{
                // select this marker
                markerDetailsShow(id);
                if (!markers[id].marker.dragging.enabled()) {
                    markers[id].marker.dragging.enable(); // Make the marker draggable

                    // Optionally, change the marker's icon or appearance
                    markers[id].marker.setIcon(L.icon({
                        iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png', // Change icon
                        iconSize: [50, 82],
                        iconAnchor: [12, 41],
                        popupAnchor: [1, -34],
                        shadowSize: [41, 41]
                    }));

                    console.log('Marker '+ id +' is now draggable!');
                }
            }
        });
        leafleatObject.on('dragend', function(e) {
          console.log('marker dragend event');
          publishMarker(e.target._latlng.lat, e.target._latlng.lng, id, markers[id].name, markers[id].desc);
        });

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

function markerDelete() {
    var id = $("#markerDetails :input#id").val();
    publishMarkerDeletion(id);
    markerDetailsHide();
}

function markerDetailsSave() {
    var id = $("#markerDetails :input#id").val();
    var name = $("#markerDetails :input#name").val();
    var desc = $("#markerDetails :input#description").val();
    publishMarker(markers[id].lat, markers[id].lon, id, name, desc);
    markerDetailsHide();
}

function markerDetailsHide() {
    $("#markerDetails").hide();
    var id = $("#markerDetails :input#id").val();

    // unselect
    if (markers && markers[id]) {
        if (markers[id].marker.dragging.enabled() && !enableMultiDrag) {
            markers[id].marker.dragging.disable(); // Make the marker not draggable
            console.log('Marker '+ id +' is now not draggable!');
        }

        // Optionally, change the marker's icon or appearance
        markers[id].marker.setIcon(L.icon({
            iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png', // Change icon back
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
        }));
    }


    $("#markerDetails :input#id").val("invalid")
    //map.fire('coopmap:markerunselected', { 'markerId': id });
    updateMapHeight();
}

function updateMapHeight() {
    var otherHeights = $("#top").height() + 16;
    $("#map").height("calc(100% - " + otherHeights + "px)");
    setTimeout(function(){ map.invalidateSize()}, 400);

}

function markerDetailsShow(id) {
    //map.fire('coopmap:markerselected', { 'markerId': id });
    $("#markerDetails :input#id").val(id);
    var marker = markers[id];

    $("#markerDetails p").text("Marker #"+id+" Details");
    $( "#name" ).val(marker.name);
    $( "#description" ).val(marker.desc);
    $("#markerDetails").show();
    var otherHeights = $("#markerDetails").height() +  $("#top").height() + 32;
    $("#map").height("calc(100% - "+otherHeights+"px)");
    setTimeout(function() {
      map.invalidateSize();
      map.panTo(new L.LatLng(marker.lat, marker.lon));
    }, 400);

}


const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
}).addTo(map);

map.clicked = 0;
function onMapClick(e) {
    // react only on single click not dblclick
    map.clicked = map.clicked + 1;
    setTimeout(function(){
        if(map.clicked == 1){
            if (stompClient.connected == true) {
                publishMarker(e.latlng.lat, e.latlng.lng);
            }else{
                console.log("can not create marker. Since Connection to server was not established now");
            }
            map.clicked = 0;
        }
     }, 300);
}

map.on('click', onMapClick);

map.on('dblclick', function(event){
    map.clicked = 0;
    map.zoomIn();
});