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
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    private static final Logger LOGGER = Logger.getLogger(MovieResource.class);

    @Inject
    Mutiny.SessionFactory sf;

    @Inject
    MovieProducer producer;

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

    @GET
    @Path("/movies/{id}")
    public Uni<MovieEntity> getById(Integer id) {
        return sf.withTransaction((s,t) -> s.find(MovieEntity.class, id));
    }

    @GET
    @Path("/movies")
    public Uni<List<MovieEntity>> get() {
        return sf.withTransaction((s,t) -> s
                .createNamedQuery("Movies.findAll", MovieEntity.class)
                .getResultList()
        );
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
}
