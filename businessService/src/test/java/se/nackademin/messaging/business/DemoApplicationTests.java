package se.nackademin.messaging.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ContextConfiguration(initializers = DemoApplicationTests.Lab1ApplicationTestsContextInitializer.class)
@AutoConfigureMockMvc
class DemoApplicationTests {

    @Container
    private static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.9.5");

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Autowired
    ConnectionFactory connectionFactory;

    RabbitAdmin rabbitAdmin;

    @BeforeEach
    void setUp() {
        rabbitAdmin = new RabbitAdmin(connectionFactory);
        // TODO: uppfift 2.a
        // Dags att konfa v??r milj??, vi har skapat en exchange men vi beh??ver en queue s?? vi kan testa mot
        // Skapa en queue och en binding till den exchange vi skapade uppgift 1
        // Rabbit har ett verktyg som heter RabbitAdmin med bra hj??lpmetoder
        // Tex. rabbitAdmin.declareQueue och rabbitAdmin.declareBinding
        //rabbitAdmin.declareExchange(new FanoutExchange("business"));;
        // Skapa en Queue
        rabbitAdmin.declareQueue(new Queue("queue"));
        // Skapa en binding f??r queue till vardera exchange
        rabbitAdmin.declareBinding(new Binding("queue", Binding.DestinationType.QUEUE, "business", "just to fill the hole", Map.of()));

    }

    @Test
    void shouldSendCreatedOnPaymentCreated() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/openAccount/1234")).andExpect(status().is2xxSuccessful());
        /*
		TODO: Uppgift 2.b:
            Consume message fr??n din k?? du skapade i before.
            Testet kommer att fallera d?? vi inte har implemeterat v??r producer.
            rabbitTemplate har precis som v??r restTemplate massa bra metoder
            testa att anv??nda rabbitTemplate.receive(queue-name, 4000);
            d??refter kan du titta p?? meddelandet genom att k??ra message.getBody()
            Avsluta testet med att asserta att body inneh??ller "OPEN_ACCOUNT"
            */
        Message message = rabbitTemplate.receive("queue", 4000);
            assertNotNull(message);
            assertTrue(new String(message.getBody()).contains("OPEN_ACCOUNT"));
/*
            f??rhoppningsivs failar testet p?? att message ??r null.

            Leta efter uppgift 3.
		 */

    }

    public static class Lab1ApplicationTestsContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    configurableApplicationContext,
                    "spring.rabbitmq.host=" + rabbit.getContainerIpAddress(), "spring.rabbitmq.port=" + rabbit.getMappedPort(5672));

        }
    }
}
