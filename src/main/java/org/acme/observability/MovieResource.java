package org.acme.observability;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    private static final Logger LOGGER = Logger.getLogger(MovieResource.class);

    private AtomicLong counter = new AtomicLong(0);

    @Inject
    Mutiny.SessionFactory sf;

    @Inject
    MovieProducer producer;

    // Metrics with MicroMeter
    //     Timed
    //     Counted
    // Tracing Span
    //     Span and tracing
    @POST
    @Path("/movies")
    @WithSpan("MovieResource.send")
    @Timed(value = "MovieResource.send")
    @Counted
    public Response send(MovieEntity movieEntity) {
        producer.sendMovieToKafka(movieEntity);
        // Return an 202 - Accepted response.
        return Response.accepted().build();
    }

    // Fault Tolerance
    //     Retry
    //     Fallback
    @GET
    @Path("/movies/{id}")
    @WithSpan("MoviesResource.getById")
    @Retry(retryOn = WebApplicationException.class, maxRetries = 3, delay = 2000)
    @Fallback(fallbackMethod = "MovieFallback") // Can also take a class
    public Uni<MovieEntity> getById(Integer id) {

        final Long invocationNumber = counter.getAndIncrement();

        maybeFail(String.format("MoviesResource#getById() invocation #%d failed", invocationNumber));


        LOGGER.infof("MoviesResource#getById() invocation #%d succeeded", invocationNumber);
        return sf.withTransaction((s,t) -> s.find(MovieEntity.class, id));
    }

    // Fault Tolerance
    //     Timeout
    @GET
    @Path("/movies")
    @WithSpan("MoviesResource.getAll")
    @Timeout(250)
    public Uni<List<MovieEntity>> getAll() {
        long started = System.currentTimeMillis();
        final long invocationNumber = counter.getAndIncrement();

        try {
            randomDelay();
            LOGGER.infof("MoviesResource#getAll() invocation #%d succeeded", invocationNumber);
            return sf.withTransaction((s,t) -> s
                    .createNamedQuery("Movies.findAll", MovieEntity.class)
                    .getResultList()
            );
        } catch (InterruptedException ex) {
            LOGGER.infof("MoviesResource#getById() invocation #%d timed out after %d ms",
                    invocationNumber, System.currentTimeMillis() - started);
            return null;
        }
        
    }

    @PUT
    @Path("/movies/{id}")
    public Uni<Response> update(Integer id, MovieEntity movieEntity) {
        if (movieEntity == null || movieEntity.title == null) {
            throw new WebApplicationException("Movie title was not set on request.", 422);
        }

        return sf.withTransaction((s,t) -> s.find(MovieEntity.class, id)
                        .onItem().ifNull().failWith(new WebApplicationException("Movie missing from database.", NOT_FOUND))
                        // If entity exists then update it
                        .invoke(entity -> {
                            entity.title = movieEntity.title;
                            entity.year = movieEntity.year;
                        }))
                .map(entity -> Response.ok(entity).build());
    }

    @DELETE
    @Path("/movies/{id}")
    public Uni<Response> delete(Integer id) {
        return sf.withTransaction((s,t) -> s.find(MovieEntity.class, id)
                        .onItem().ifNull().failWith(new WebApplicationException("Movie missing from database.", NOT_FOUND))
                        // If entity exists then delete it
                        .call(s::remove))
                .replaceWith(Response.ok().status(NO_CONTENT)::build);
    }

    public String MovieFallback(final Integer id) {
        return String.format("Unable to get Movie %d: ", id);
    }

    private void maybeFail(String failureLogMessage) {
        if (new Random().nextBoolean()) {
            LOGGER.error(failureLogMessage);
            throw new WebApplicationException("Resource failure.");
        }
    }

    private void randomDelay() throws InterruptedException {
        Thread.sleep(new Random().nextInt(500));
    }
}
