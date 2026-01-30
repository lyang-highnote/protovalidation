package com.bennet.grpc.interceptor;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.StatusProto;

public class ValidationInterceptor implements ServerInterceptor {

    private final Validator validator;

    public ValidationInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new RequestValidationServerCallListener<>(validator, call, listener);
    }

    private static class RequestValidationServerCallListener<ReqT, RespT>
            extends SimpleForwardingServerCallListener<ReqT> {

        private final Validator validator;
        private final ServerCall<ReqT, RespT> call;

        protected RequestValidationServerCallListener(
                Validator validator, ServerCall<ReqT, RespT> call, ServerCall.Listener<ReqT> delegate) {
            super(delegate);
            this.validator = validator;
            this.call = call;
        }

        @Override
        public void onHalfClose() {
            if (this.call.isReady()) {
                super.onHalfClose();
            }
        }

        @Override
        public void onMessage(ReqT message) {
            if (!(message instanceof Message)) {
                throw new IllegalArgumentException(
                        "Message is of type " + message.getClass() + ", not a " + Message.class.getName());
            }

            try {
                ValidationResult validationResult = validator.validate((Message) message);

                if (validationResult.isSuccess()) {
                    super.onMessage(message);
                } else {
                    Status status = Status.newBuilder()
                            .setCode(Code.INVALID_ARGUMENT.getNumber())
                            .setMessage(Code.INVALID_ARGUMENT.getValueDescriptor().getName())
                            .addDetails(Any.pack(validationResult.toProto()))
                            .build();

                    var sre = StatusProto.toStatusRuntimeException(status);
                    call.close(sre.getStatus(), sre.getTrailers());
                }

            } catch (ValidationException e) {
                Status status = Status.newBuilder()
                        .setCode(Code.INTERNAL.getNumber())
                        .setMessage(e.getMessage())
                        .build();

                throw StatusProto.toStatusRuntimeException(status);
            }
        }
    }
}
