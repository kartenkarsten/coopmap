# CoOpMap

This project provides a cooperative interactive map. Every one sees the same view. Everyone can interact with the map.

implemented Features:
 - add markers
 - move markers

this can e.g. be used in online meetings to mark each ones location 

# Usage

run
TODO this command does not work - fix it
```
docker run -p 8082:8082 -e server.name=1.2.3.4 -e spring.profiles.active=default,debug,local-db ghcr.io/kartenkarsten/coopmap:latest
```

# Build

run 
```
./gradlew bootBuildImage
```
