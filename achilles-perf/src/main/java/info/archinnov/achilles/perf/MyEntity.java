package info.archinnov.achilles.perf;

import info.archinnov.achilles.annotations.Column;
import info.archinnov.achilles.annotations.Entity;
import info.archinnov.achilles.annotations.PartitionKey;
import info.archinnov.achilles.annotations.Strategy;
import info.archinnov.achilles.type.NamingStrategy;

@Entity(keyspace = "test_ks",table = "my_table")
@Strategy(naming = NamingStrategy.SNAKE_CASE)
public class MyEntity {

    @PartitionKey
    private Long id;

    @Column
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



}
