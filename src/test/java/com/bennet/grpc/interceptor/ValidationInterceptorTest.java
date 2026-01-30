package com.bennet.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violations;
import com.bennet.grpc.service.HelloService;
import com.bennet.grpc.v1.gen.HelloServiceGrpc;
import com.bennet.grpc.v1.gen.SayHelloRequest;
import com.bennet.grpc.v1.gen.SayHelloResponse;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationInterceptorTest {

    private Server server;
    private ManagedChannel channel;
    private HelloServiceGrpc.HelloServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        Validator validator = ValidatorFactory.newBuilder().build();
        ValidationInterceptor validationInterceptor = new ValidationInterceptor(validator);

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(new HelloService(), validationInterceptor))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        stub = HelloServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void validator_shouldValidateDirectly() throws ValidationException {
        Validator validator = ValidatorFactory.newBuilder().build();

        SayHelloRequest validRequest = SayHelloRequest.newBuilder()
                .setName("World")
                .build();
        ValidationResult result = validator.validate(validRequest);
        assertThat(result.isSuccess()).isTrue();

        SayHelloRequest invalidRequest = SayHelloRequest.newBuilder()
                .setName("")
                .build();
        ValidationResult invalidResult = validator.validate(invalidRequest);
        assertThat(invalidResult.isSuccess()).isFalse();
    }

    @Test
    void validRequest_shouldSucceed() {
        SayHelloRequest request = SayHelloRequest.newBuilder()
                .setName("World")
                .build();

        SayHelloResponse response = stub.sayHello(request);

        assertThat(response.getMessage()).isEqualTo("Hello World");
    }

    @Test
    void emptyName_shouldFailValidation() {
        SayHelloRequest request = SayHelloRequest.newBuilder()
                .setName("")
                .build();

        assertThatThrownBy(() -> stub.sayHello(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> {
                    StatusRuntimeException sre = (StatusRuntimeException) e;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                });
    }

    @Test
    void nameTooLong_shouldFailValidation() {
        String longName = "a".repeat(51);
        SayHelloRequest request = SayHelloRequest.newBuilder()
                .setName(longName)
                .build();

        assertThatThrownBy(() -> stub.sayHello(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> {
                    StatusRuntimeException sre = (StatusRuntimeException) e;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                });
    }

    @Test
    void nameAtMaxLength_shouldSucceed() {
        String maxName = "a".repeat(50);
        SayHelloRequest request = SayHelloRequest.newBuilder()
                .setName(maxName)
                .build();

        SayHelloResponse response = stub.sayHello(request);

        assertThat(response.getMessage()).isEqualTo("Hello " + maxName);
    }

    @Test
    void emptyName_shouldContainViolationDetails() throws InvalidProtocolBufferException {
        SayHelloRequest request = SayHelloRequest.newBuilder()
                .setName("")
                .build();

        try {
            stub.sayHello(request);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);

            com.google.rpc.Status status = io.grpc.protobuf.StatusProto.fromThrowable(e);
            assertThat(status).isNotNull();
            assertThat(status.getDetailsCount()).isGreaterThan(0);

            Any detail = status.getDetails(0);
            assertThat(detail.is(Violations.class)).isTrue();

            Violations violations = detail.unpack(Violations.class);
            assertThat(violations.getViolationsCount()).isGreaterThan(0);

            var violation = violations.getViolations(0);
            assertThat(violation.getField().getElements(0).getFieldName()).isEqualTo("name");
        }
    }
}
