package se.nackademin.messaging.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class AuditEvent {
    @JsonProperty
    public long accountId;
    @JsonProperty
    public String data;
    @JsonProperty
    public String type;
    @JsonProperty
    public String timestamp;

    @JsonCreator
    public AuditEvent(@JsonProperty("accountId") long accountId, @JsonProperty("data") String data, @JsonProperty("type") String type, @JsonProperty("timestamp") String timestamp) {
        this.accountId = accountId;
        this.data = data;
        this.type = type;
        this.timestamp = timestamp;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    //Here to convert to Audit Event to Audit Entry, first the type.
    public AuditEntry.AuditType toAuditType(String type){
        if(type.equals("OPEN_ACCOUNT")){
            return AuditEntry.AuditType.Open;
        } else if (type.equals("DEPOSIT")){
            return AuditEntry.AuditType.Deposit;
        }
        throw new RuntimeException("type not found");
    }
    //THen the whole object
    public AuditEntry toDomainObject(){
        return new AuditEntry(toAuditType(type), accountId, Instant.parse(timestamp), data);
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "accountId=" + accountId +
                ", data='" + data + '\'' +
                ", type='" + type + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
