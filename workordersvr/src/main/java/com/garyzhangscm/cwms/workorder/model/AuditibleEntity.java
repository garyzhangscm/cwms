package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class AuditibleEntity<U> {

    private static final Logger logger = LoggerFactory.getLogger(AuditibleEntity.class);

    @Column(name = "created_time")
    //@CreatedDate
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime createdTime;

    @Column(name = "created_by")
    @CreatedBy
    private U createdBy;

    @Column(name = "last_modified_time")
    //@LastModifiedDate
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastModifiedTime;

    @Column(name = "last_modified_by")
    @LastModifiedBy
    private U lastModifiedBy;

    // temporary saved created time so that when we change the entity
    // we won't erase the value
    @Transient
    @JsonIgnore
    private ZonedDateTime bufferedCreatedTime;
    @Transient
    @JsonIgnore
    private U bufferedCreatedBy;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostLoad
    public void saveCreatedTime() {
        bufferedCreatedTime = getCreatedTime();
        bufferedCreatedBy = getCreatedBy();

    }
    @PrePersist
    public void onPrePersist() {
        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }

    @PreUpdate
    public void onPreUpdate() {

        if (Objects.nonNull(bufferedCreatedTime)) {
            setCreatedTime(bufferedCreatedTime);
        }
        else {
            setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        }
        if (Objects.nonNull(bufferedCreatedBy)) {
            setCreatedBy(bufferedCreatedBy);
        }
        else {
            setCreatedBy(getLastModifiedBy());
        }
        setLastModifiedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    public ZonedDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(ZonedDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public U getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(U createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(ZonedDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public U getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(U lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public ZonedDateTime getBufferedCreatedTime() {
        return bufferedCreatedTime;
    }

    public void setBufferedCreatedTime(ZonedDateTime bufferedCreatedTime) {
        this.bufferedCreatedTime = bufferedCreatedTime;
    }
}
