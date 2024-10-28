package ru.telproject.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.telproject.model.RabbitQueue.*;

@Configuration
public class RabbitConfiguration {

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper){
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Queue docMessageQueue(){
        return new Queue(DOC_MESSAGE_UPDATE);
    }
    @Bean
    public Queue textMessageQueue(){
        return new Queue(TEXT_MESSAGE_UPDATE);
    }
    @Bean
    public Queue photoMessageQueue(){
        return new Queue(PHOTO_MESSAGE_UPDATE);
    }
    @Bean
    public Queue answerMessageQueue(){
        return new Queue(ANSWER_MESSAGE_UPDATE);
    }

    @Bean
    public Queue recordUserPush(){return new Queue(RECORD_USER_PUSH);}
}
