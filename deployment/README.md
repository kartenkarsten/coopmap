it bases on code from this [website](https://www.heyvaldemar.com/install-nextcloud-using-docker-compose/).

```bash
# use this commands manually - some needs to be executed only once
DOCKER_HOST=ssh://1.2.3.4
docker -H ${DOCKER_HOST} network create traefik-network
# first check if server has ports accessable used by acme validation to avoid blocking acme request for your hostname
#  ( e.g. disable fail2ban and ufw for testing on destination host)
docker -H ${DOCKER_HOST} compose -f coopmap-traefik-letsencrypt-docker-compose.yml -p coopmap up -d

#docker -H ${DOCKER_HOST} compose -f coopmap-traefik-letsencrypt-docker-compose.yml -p coopmap down
```
