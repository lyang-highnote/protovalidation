import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import com.bennet.grpc.interceptor.ValidationInterceptor;
import com.bennet.grpc.service.HelloService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloApplication.class);
  public static final int PORT = 9901;

  public static void main(String[] args) throws IOException, InterruptedException {
    Validator validator = ValidatorFactory.newBuilder().build();
    ValidationInterceptor validationInterceptor = new ValidationInterceptor(validator);

    Server server =
        ServerBuilder.forPort(PORT)
            .addService(ServerInterceptors.intercept(new HelloService(), validationInterceptor))
            .addService(ProtoReflectionService.newInstance())
            .build();
    server.start();
    LOGGER.info("Server started, listening on " + server.getPort());
    server.awaitTermination();
  }
}