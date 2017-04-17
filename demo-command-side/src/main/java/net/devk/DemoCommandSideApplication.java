package net.devk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.CommandRouter;
import org.axonframework.commandhandling.distributed.DistributedCommandBus;
import org.axonframework.commandhandling.distributed.MetaDataRoutingStrategy;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.serialization.Serializer;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import net.devk.complaints.api.FileComplaintCommand;

@EnableDiscoveryClient
@SpringBootApplication
public class DemoCommandSideApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoCommandSideApplication.class, args);
	}

	@RestController
	public static class ComplaintsAPI {

		private final CommandGateway commandGateway;

		public ComplaintsAPI(CommandGateway commandGateway) {
			this.commandGateway = commandGateway;
		}

		@PostMapping
		public CompletableFuture<String> fileComplaint(@RequestBody Map<String, String> request) {
			String id = UUID.randomUUID().toString();

			FileComplaintCommand command = new FileComplaintCommand(id, request.get("company"),
					request.get("description"));
			Map<String, String> metaData = new HashMap<>();
			metaData.put("complaints", "123");
			GenericCommandMessage<FileComplaintCommand> commandMessage = new GenericCommandMessage<FileComplaintCommand>(
					command, metaData);

			return commandGateway.send(commandMessage);
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

	@Primary // to make sure this CommandBus implementation is used for
				// autowiring
	@Bean
	public DistributedCommandBus springCloudDistributedCommandBus(CommandRouter commandRouter,
			CommandBusConnector commandBusConnector) {
		return new DistributedCommandBus(commandRouter, commandBusConnector);
	}
}
