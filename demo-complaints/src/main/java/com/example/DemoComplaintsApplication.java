package com.example;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.CommandRouter;
import org.axonframework.commandhandling.distributed.DistributedCommandBus;
import org.axonframework.commandhandling.distributed.MetaDataRoutingStrategy;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.commandhandling.model.AggregateLifecycle;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.stereotype.Aggregate;
import org.axonframework.springcloud.commandhandling.SpringCloudCommandRouter;
import org.axonframework.springcloud.commandhandling.SpringHttpCommandBusConnector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import net.devk.complaints.api.ComplaintFileEvent;
import net.devk.complaints.api.FileComplaintCommand;

@EnableDiscoveryClient
@SpringBootApplication
public class DemoComplaintsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoComplaintsApplication.class, args);
	}

	@RestController
	public static class ComplaintsAPI {

		private final ComplaintQueryObjectRepository complaintQueryObjectRepository;
		private final CommandGateway commandGateway;

		public ComplaintsAPI(ComplaintQueryObjectRepository complaintQueryObjectRepository,
				CommandGateway commandGateway) {
			super();
			this.complaintQueryObjectRepository = complaintQueryObjectRepository;
			this.commandGateway = commandGateway;
		}

		@PostMapping
		public CompletableFuture<String> fileComplaint(@RequestBody Map<String, String> request) {
			String id = UUID.randomUUID().toString();
			return commandGateway
					.send(new FileComplaintCommand(id, request.get("company"), request.get("description")));
		}

		@GetMapping
		public List<ComplaintQueryObject> findAll() {
			return complaintQueryObjectRepository.findAll();
		}

		@GetMapping("/{id}")
		public ComplaintQueryObject findOne(@PathVariable String id) {
			return complaintQueryObjectRepository.findOne(id);
		}
	}

	@Aggregate
	public static class Complaint {
		@AggregateIdentifier
		private String identifier;

		@CommandHandler(routingKey = "123")
		public Complaint(FileComplaintCommand fileComplaintCommand) {
			Assert.hasLength(fileComplaintCommand.getCompany(), "invalid company name");
			AggregateLifecycle.apply(new ComplaintFileEvent(fileComplaintCommand.getId(),
					fileComplaintCommand.getCompany(), fileComplaintCommand.getDescription()));
		}

		@EventSourcingHandler
		public void on(ComplaintFileEvent complaintFileEvent) {
			identifier = complaintFileEvent.getId();
		}

	}

	@Component
	public static class ComplaintQueryModelUpdater {
		private final ComplaintQueryObjectRepository complaintQueryObjectRepository;

		public ComplaintQueryModelUpdater(ComplaintQueryObjectRepository complaintQueryObjectRepository) {
			super();
			this.complaintQueryObjectRepository = complaintQueryObjectRepository;
		}

		@EventHandler
		public void handle(ComplaintFileEvent event) {
			complaintQueryObjectRepository
					.save(new ComplaintQueryObject(event.getId(), event.getCompany(), event.getDescription()));
		}
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	// Example function providing a Spring Cloud Connector
	@Bean
	public CommandRouter springCloudCommandRouter(DiscoveryClient discoveryClient) {
		return new SpringCloudCommandRouter(discoveryClient, new MetaDataRoutingStrategy("complaints"));
	}

	@Bean
	public CommandBusConnector springHttpCommandBusConnector(@Qualifier("localSegment") CommandBus localCommandBus,
			RestTemplate restTemplate, Serializer serializer) {
		return new SpringHttpCommandBusConnector(localCommandBus, restTemplate, serializer);
	}

	@Primary
	@Bean
	public DistributedCommandBus springCloudDistributedCommandBus(CommandRouter commandRouter,
			CommandBusConnector commandBusConnector) {
		return new DistributedCommandBus(commandRouter, commandBusConnector);
	}

	// @Bean
	// public Exchange exchange() {
	// return ExchangeBuilder.fanoutExchange("ComplaintEvents").build();
	// }
	//
	// @Bean
	// public Queue queue() {
	// return QueueBuilder.durable("ComplaintEvents").build();
	// }
	//
	// @Bean
	// public Binding binding() {
	// return BindingBuilder.bind(queue()).to(exchange()).with("*").noargs();
	// }
	//
	// @Autowired
	// public void configure(AmqpAdmin amqpAdmin) {
	// amqpAdmin.declareExchange(exchange());
	// amqpAdmin.declareQueue(queue());
	// amqpAdmin.declareBinding(binding());
	// }

}
