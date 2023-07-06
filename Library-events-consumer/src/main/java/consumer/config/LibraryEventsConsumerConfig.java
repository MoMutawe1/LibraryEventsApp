package consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

/*
factory.setConcurrency(3) :
    here the  .setConcurrency() is a method which we can use in order to configure multiple Kafka listeners from the same application itself,
    in our Kafka Topic we have 3 partitions, so I'm going to provide the value as three So it is going to spawn three threads with the same instance of the Kafka listener.
*/

@Configuration
//@EnableKafka  // this annotation was required in previous version of kafka, to make sure the consumer that
// we are going to build is going to be automatically spun up when you start up the application.
@Slf4j
public class LibraryEventsConsumerConfig {

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(3);

        return factory;
    }
}
