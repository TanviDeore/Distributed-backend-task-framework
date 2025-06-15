package com.api.apiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {
	@Value("${aws.region}")
	private String awsRegion;
	
	@Bean
    public DynamoDbClient dynamoDbClient() {
        // Configure the basic client, e.g., with your region
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion)) 
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        // Create the enhanced client from the basic client
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
    
    @Bean
    public SqsClient sqsClient() {
      return SqsClient.builder()
                      .region(Region.of(awsRegion))
                      .build();
    }

}
