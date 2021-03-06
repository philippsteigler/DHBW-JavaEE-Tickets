package org.dhbw.mosbach.ai.tickets.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class Entry {

    //Persistente Attribute:
    @Id
    @GeneratedValue
    private long id;

    @Column
    private long creatorId;

    @Column
    private String content;

    @Column
    private Date createDate;

    //Konstruktor:
    public Entry() { super(); }

    public Entry (long creatorId, String content, Date createDate) {
        this.creatorId = creatorId;
        this.content = content;
        this.createDate = createDate;
    }

    //Zugriffsmethoden:
    public long getCreatorID() {
        return creatorId;
    }

    public String getContent() {
        return content;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public long getId() {
        return id;
    }
}

