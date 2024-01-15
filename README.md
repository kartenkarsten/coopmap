# CoOpMap

This project provides a cooperative interactive map. Every one sees the same view. Everyone can interact with the map.

implemented Features:
 - add markers
 - move markers

this can e.g. be used in online meetings to mark each ones location 

# Usage

run 
```
./gradlew bootBuildImage
docker run -p 8082:8082 docker.io/library/coopmap:0.0.1-SNAPSHOT
```
