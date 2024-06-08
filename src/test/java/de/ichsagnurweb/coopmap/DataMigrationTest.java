package de.ichsagnurweb.coopmap;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

// integration test
@ActiveProfiles(value = "integrationtest")
@SpringBootTest
public class DataMigrationTest {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationTest.class);

    // Create a custom network
    static Network network = Network.newNetwork();

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:15-alpine"
    )
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(5432)
            .withNetworkAliases("postgres")
            .withNetwork(network);

    private RestTemplate restTemplate = new RestTemplate();


    @BeforeAll
    static void setUp() {
        postgres.start();
    }

    @AfterAll
    static void tearDown() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MarkerRepository myEntityRepository;

    @Test
    void shouldMigrateData() {
        String mapId = "test-shouldMigrateData";

        // run the old release - is a precondition
        DockerImageName appImage = DockerImageName.parse("ghcr.io/kartenkarsten/coopmap:latest");
        String jdbcUrl = "jdbc:postgresql://postgres:5432/"+postgres.getDatabaseName();
        GenericContainer<?> oldReleaseContainer = new GenericContainer<>(appImage)
            //    .withImagePullPolicy(PullPolicy.ageBased(Duration.ofMinutes(10)))
                .withEnv("spring.datasource.url", jdbcUrl)
                .withEnv("spring.datasource.username", postgres.getUsername())
                .withEnv("spring.datasource.password", postgres.getPassword())
                .withEnv("spring.profiles.active", "default,debug")
                .dependsOn(postgres)
                .withExposedPorts(8082)
                .withNetwork(network)
                .waitingFor(Wait.forLogMessage(".*Started .*Application in.*", 1).withStartupTimeout(Duration.ofSeconds(30)));

//        oldReleaseContainer.waitingFor(Wait.forHttp("/").forStatusCode(200));
       oldReleaseContainer.start();

       // assert empty db
        Iterable<Marker> allMarkers = myEntityRepository.findAllByMapId(mapId);
        assertThat(allMarkers).isEmpty();

        //create sample data with the old release
        String migrationUrl = "http://"+oldReleaseContainer.getHost()+":" + oldReleaseContainer.getFirstMappedPort() + "/api/map/"+mapId+"/sampledata";
        restTemplate.postForObject(migrationUrl, null, Void.class);

        // assert data existing
        allMarkers = myEntityRepository.findAllByMapId(mapId);
        assertThat(allMarkers).isNotEmpty();

        // stop the old release
        oldReleaseContainer.stop();

        // run current build
        DockerImageName currentAppImage = DockerImageName.parse("ghcr.io/kartenkarsten/coopmap:dev-tag");
        GenericContainer<?> currentBuildAppContainer = new GenericContainer<>(currentAppImage)
                //    .withImagePullPolicy(PullPolicy.ageBased(Duration.ofMinutes(10)))
                .withEnv("spring.datasource.url", jdbcUrl)
                .withEnv("spring.datasource.username", postgres.getUsername())
                .withEnv("spring.datasource.password", postgres.getPassword())
                .withEnv("spring.profiles.active", "default,debug")
                .dependsOn(postgres)
                .withExposedPorts(8082)
                .withNetwork(network)
                .waitingFor(Wait.forLogMessage(".*Started .*Application in.*", 1).withStartupTimeout(Duration.ofSeconds(120)));

        // Use a LogConsumer to capture container logs
        currentBuildAppContainer.start();

        // assert data existing
        allMarkers = myEntityRepository.findAllByMapId(mapId);
        assertThat(allMarkers).isNotEmpty();

        String logs = currentBuildAppContainer.getLogs();
        // ensure liquibase run was successfull
        assertThat(logs).contains("liquibase.util");
        assertThat(logs).contains("Successfully released change log lock");
        assertThat(logs).contains("Command execution complete");
//        2024-06-08T06:42:00.887Z  INFO 1 --- [           main] liquibase.util                           : Update summary generated
//        2024-06-08T06:42:00.950Z  INFO 1 --- [           main] liquibase.lockservice                    : Successfully released change log lock
//        2024-06-08T06:42:00.954Z  INFO 1 --- [           main] liquibase.command                        : Command execution complete

        //clean up
        currentBuildAppContainer.stop();
    }
}
