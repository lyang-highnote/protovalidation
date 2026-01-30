package com.bennet.grpc.service;

import com.bennet.grpc.v1.gen.HelloServiceGrpc.HelloServiceImplBase;
import com.bennet.grpc.v1.gen.SayHelloRequest;
import com.bennet.grpc.v1.gen.SayHelloResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloService extends HelloServiceImplBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloService.class);

  @Override
  public void sayHello(SayHelloRequest request, StreamObserver<SayHelloResponse> responseObserver) {
    Thread thread = Thread.currentThread();
    LOGGER.info("[{}]: Received request from {}", thread, request.getName());
    String message = String.format("Hello %s", request.getName());
    SayHelloResponse response = SayHelloResponse.newBuilder().setMessage(message).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    LOGGER.info("[{}] Sending response for {}", thread, request.getName());
  }
}
