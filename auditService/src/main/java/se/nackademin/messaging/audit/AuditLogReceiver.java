package se.nackademin.messaging.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditLogReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogReceiver.class);


    @Autowired
    AuditLogRepository auditLogRepository;

    @RabbitListener(queues = "audit-log")
    public void receiveMessage(AuditEvent event) {
        LOG.info("Received message! {}", event);
        /* TODO: Uppgift 2: Spara eventet!
            För att lyssna på events räcker det med @RabbitListner! Annotera denna metod med:
             @RabbitListener(queues = "audit-log") så kommer den automagiskt att ropas på
             när ett message hamnar på den queuen!

            Om ni nu kör testet borde ni få en utskrift i loggen "Received message" även to testet failar.

            När vi får in ett event så vill vi spara det i auditLogRepository.

            Översätt DTOn AuditEvent till vårat domänobjekt AuditEntry och spara det i databasen.
            Finns ett auditLogRepository här ni kan använda er av för att spara AuditEntries.

            För att se att allt fungerar kör testet AuditApplicationTest
         */
        auditLogRepository.add(event.toDomainObject());


    }
}
