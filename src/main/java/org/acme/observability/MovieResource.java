package org.acme.observability;
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
    public Response send(MovieEntity movieEntity) {
        producer.sendMovieToKafka(movieEntity);
        // Return an 202 - Accepted response.
        return Response.accepted().build();
    }

    @GET
    @Path("/movies")
    public Uni<List<MovieEntity>> get() {
        return sf.withTransaction((s,t) -> s
                .createNamedQuery("Movies.findAll", MovieEntity.class)
                .getResultList()
        );
    }
}
