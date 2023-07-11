package org.acme.observability;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
//import io.quarkus.hibernate.orm.panache.PanacheEntity;
//import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@Table(name = "movie")
public class MovieEntity {

    @Id
    @SequenceGenerator(name = "moviesSequence", sequenceName = "known_movies_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "moviesSequence")
    private Integer id;

    public String title;
    public Integer year;


}
