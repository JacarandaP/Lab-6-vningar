package se.nackademin.messaging.business;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.amqp.core.Binding.*;

@Testcontainers
public class RabbitTest {

    @Container
    private static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.9.5");

    // RabbitAdmin är ett bra hjälpverktyg för tex tester där vi kan programatiskt skapa köer etc.
    RabbitAdmin rabbitAdmin;

    // RabbitTemplate är precis som RestTemplate vi har använt tidigare. Ett enkelt sätt att interagera med rabbit
    // Du kan tex. använda .receive för att läsa meddelanden och .convertAndSend för att serialisera och skicka meddelanden.
    RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        // En connection factory är ett sätt att beskriva hur vi ska connecta till rabbit. I detta fall behöver vi
        // bara tillhandahålla en ip-address och en port.
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbit.getContainerIpAddress(), rabbit.getMappedPort(5672));
        rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitTemplate = new RabbitTemplate(connectionFactory);
    }
/*
    @AfterEach
    void tearDown(){
        rabbitAdmin.deleteExchange("namn");
        rabbitAdmin.declareQueue("namn");//por att återanvända

    }
*/
    @Test
    void uppgift_1_skicka_och_ta_emot_ett_meddelande() {
        // Kommer ni ihåg från föreläsningen. En exchange är dit vi publicerar saker. På en exchange kan vi koppla en
        // eller flera Queues, köer som consumers kan beta av. För att koppla ihop en Queue med en Exchange skapar vi
        // en Binding. Låt oss skapa dessa i detta test!

        // Skapa en exhange
       rabbitAdmin.declareExchange(new FanoutExchange("my-exchange-1"));
        // Skapa en queue
        rabbitAdmin.declareQueue(new Queue("for-test-only-1"));
        // Skapa en binding
        rabbitAdmin.declareBinding(new Binding("for-test-only-1", DestinationType.QUEUE, "my-exchange-1", "routing-key-is-not-used-for-fanout-but-required", Map.of()));
        // Produce message på exchange
        rabbitTemplate.convertAndSend("my-exchange-1", "", "Hej Hej");
        // Consume message på queue
        Message message = rabbitTemplate.receive("for-test-only-1", 4000);
        assertEquals(new String(message.getBody()), "Hej Hej");
    }

    @Test
    void uppgift_2_skicka_och_ta_emot_ett_meddelande_på_fler_köer() {
        // Vi använder oss av en FanoutExchange dvs alla köer vi kopplar på får samma meddelanden.
        // Vi ska testa det genom att koppla två köer till samma exchage och säkerställa att meddelandet kommer
        // fram till båda köerna

        // Skapa en FanoutExchange
        rabbitAdmin.declareExchange(new FanoutExchange("my-exchange-2"));
        // Skapa två Queues med olika namn
        rabbitAdmin.declareQueue(new Queue("queue-1"));
        rabbitAdmin.declareQueue(new Queue("queue-2"));
        // Skapa en binding för varje queue till exchangen
        rabbitAdmin.declareBinding(new Binding("queue-1", DestinationType.QUEUE, "my-exchange-2", "just to fill the hole", Map.of()));
        rabbitAdmin.declareBinding(new Binding("queue-2", DestinationType.QUEUE, "my-exchange-2", "just to fill the hole", Map.of()));

        // Skicka ett meddelande
        rabbitTemplate.convertAndSend("my-exchange-2", "", "Hello you two");
        // ta emot ett på varje kö och se att de är samma.
        Message message1 = rabbitTemplate.receive("queue-1", 4000);
        Message message2 = rabbitTemplate.receive("queue-2", 4000);
        // asserta att meddelandet har kommit fram till båda köerna
        assertEquals(new String(message1.getBody()), "Hello you two");
        assertNotNull(message1);
        assertEquals(new String(message2.getBody()), "Hello you two");
        // asserta att meddelandet är det samma som skickades
        assertNotNull(message2);
    }

    @Test
    void uppgift_3_skicka_och_ta_emot_ett_meddelande_på_olika_köer() {
        // En kö ska endast få de meddelanden som den är ämnad för. Vi ska testa det genom att
        // skapa två exchanges och två köer och koppla en kö till vardera exchange. Nu kan vi säkerställa
        // att om vi skickar ett meddelande till en exchange så ska det bara dyka upp i en kö.

        // Skapa två FanoutExchange med olika namn
        rabbitAdmin.declareExchange(new FanoutExchange("my-exchange-a"));
        rabbitAdmin.declareExchange(new FanoutExchange("my-exchange-b"));
        // Skapa två Queues med olika namn
        rabbitAdmin.declareQueue(new Queue("queue-a"));
        rabbitAdmin.declareQueue(new Queue("queue-b"));
        // Skapa en binding för varje queue till vardera exchange
        rabbitAdmin.declareBinding(new Binding("queue-a", DestinationType.QUEUE, "my-exchange-a", "just to fill the hole", Map.of()));
        rabbitAdmin.declareBinding(new Binding("queue-b", DestinationType.QUEUE, "my-exchange-b", "just to fill the hole", Map.of()));

        // Skicka ett meddelande på vardera exchange
        rabbitTemplate.convertAndSend("my-exchange-a", "", "Hello a");
        rabbitTemplate.convertAndSend("my-exchange-b", "", "Hello b");
        // ta emot ett på varje kö och se att de är olika.
        Message message1 = rabbitTemplate.receive("queue-a", 4000);
        Message message2 = rabbitTemplate.receive("queue-b", 4000);
        // asserta att detta är sant
        assertNotEquals(message1, message2);

        //rabbitAdmin.purgeQueue("my-exchange");//to återanvända samma namn på exchange
    }

    @Test
    void uppgift_4_ta_emot_meddelanden_från_flera_exchanger() {
        // En kö kan få meddelanden från flera exchanges. Vi ska testa det genom att skapa två exchanges och en kö
        // sen ska vi koppla denna kö till båda exchangesarna. Vi kan nu säkerställa att om jag skickar ett meddelande
        // till vardera exchange så ska jag ha fått bägge på min kö.

        // Skapa två FanoutExchange med olika namn
        rabbitAdmin.declareExchange(new FanoutExchange("my-exchange-x"));
        rabbitAdmin.declareExchange(new FanoutExchange("my-exchange-y"));
        // Skapa en Queue
        rabbitAdmin.declareQueue(new Queue("queue-xy"));
        // Skapa en binding för queue till vardera exchange
        rabbitAdmin.declareBinding(new Binding("queue-xy", DestinationType.QUEUE, "my-exchange-x", "just to fill the hole", Map.of()));
        rabbitAdmin.declareBinding(new Binding("queue-xy", DestinationType.QUEUE, "my-exchange-y", "just to fill the hole", Map.of()));
        // Skicka ett meddelande på vardera exchange
        rabbitTemplate.convertAndSend("my-exchange-x", "", "Hello xy");

        rabbitTemplate.convertAndSend("my-exchange-y", "", "Hi there xy");
        // ta emot ett meddelande och se att det var första som skickades

        Message message1 = rabbitTemplate.receive("queue-xy", 4000);
        assertEquals("Hello xy", new String(message1.getBody()));
        // ta emot ett meddelande och se att det var andra som skickades
        Message message2 = rabbitTemplate.receive("queue-xy", 4000);
        assertEquals("Hi there xy", new String(message2.getBody()));
        // asserta att detta är sant.
    }
}
