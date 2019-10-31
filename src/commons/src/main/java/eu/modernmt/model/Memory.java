package eu.modernmt.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by davide on 06/09/16.
 */
public class Memory implements Serializable {

    private long id;
    private UUID owner;
    private String name;
    private boolean terminology;

    public Memory(long id) {
        this(id, null, null, false);
    }

    public Memory(long id, String name) {
        this(id, null, name, false);
    }

    public Memory(long id, UUID owner, String name) { this(id, owner, name, false); }

    public Memory(long id, boolean terminology) { this(id, null, null, terminology); }

    public Memory(long id, String name, boolean terminology) {
        this(id, null, name, terminology);
    }

    public Memory(long id, UUID owner, String name, boolean terminology) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.terminology = terminology;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTerminology() {
        return terminology;
    }

    public void setTerminology(boolean terminology) {
        this.terminology = terminology;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Memory memory = (Memory) o;

        return id == memory.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Memory{" +
                "id=" + id +
                ", owner=" + owner +
                ", name='" + name + '\'' +
                ", terminology='" + terminology + '\'' +
                '}';
    }

}
