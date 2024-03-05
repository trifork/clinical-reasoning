package org.opencds.cqf.fhir.utility.operation;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.Resources;

interface ParameterBinder {
    enum Type {
        ID,
        OPERATION,
        UNBOUND
    }

    // Extract the value from the parameters resource that matches the external name of the parameter.
    // And removes the value from the parameters resource. This is to ensure that all parameters get consumed
    Object bind(IBaseParameters parameters);

    Type type();

    String name();

    Parameter parameter();

    static ParameterBinder from(Parameter parameter) {
        Objects.requireNonNull(parameter, "parameter can not be null");

        var idParam = parameter.getAnnotation(IdParam.class);
        var operationParam = parameter.getAnnotation(OperationParam.class);
        var unboundParam = parameter.getAnnotation(UnboundParam.class);

        ensureOnlyOneOf(idParam, operationParam, unboundParam);

        if (idParam != null) {
            return new IdParameterBinder(parameter, idParam);
        } else if (operationParam != null) {
            return new OperationParameterBinder(parameter, operationParam);
        } else {
            return new UnboundParamBinder(parameter, unboundParam);
        }
    }

    static void ensureOnlyOneOf(Annotation... annotations) {
        var count = Arrays.stream(annotations).filter(Objects::nonNull).count();
        if (count == 0) {
            throw new IllegalArgumentException(
                    "Parameter must be annotated with @IdParam, @OperationParam, or @UnboundParam");
        } else if (count > 1) {
            throw new IllegalArgumentException(
                    "Parameter can only be annotated with one of @IdParam, @OperationParam, or @UnboundParam");
        }
    }

    static class IdParameterBinder implements ParameterBinder {
        private final Parameter parameter;
        private final IdParam idParam;

        public IdParameterBinder(Parameter parameter, IdParam idParam) {
            this.parameter = Objects.requireNonNull(parameter, "parameter can not be null");
            this.idParam = Objects.requireNonNull(idParam, "idParam can not be null");
        }

        @Override
        public Type type() {
            return Type.ID;
        }

        @Override
        public String name() {
            return "_id";
        }

        @Override
        public Object bind(IBaseParameters parameters) {
            throw new UnsupportedOperationException("bind is not supported for @IdParam");
        }

        @Override
        public Parameter parameter() {
            return parameter;
        }
    }

    static class OperationParameterBinder implements ParameterBinder {
        private final Parameter parameter;
        private final OperationParam operationParam;

        public OperationParameterBinder(Parameter parameter, OperationParam operationParam) {
            this.parameter = Objects.requireNonNull(parameter, "parameter can not be null");
            this.operationParam = Objects.requireNonNull(operationParam, "operationParam can not be null");
            Objects.requireNonNull(operationParam.name(), "@OperationParam must have a name defined");
        }

        @Override
        public Type type() {
            return Type.OPERATION;
        }

        @Override
        public String name() {
            return operationParam.name();
        }

        @Override
        public Object bind(IBaseParameters parameters) {
            // Extract the value from the parameters resource that matches the external name of the @OperationParam
            // And handle collection types like list.
            var context = parameters.getStructureFhirVersionEnum().newContextCached();

            var parts = Parameters.getPartsByName(context, parameters, this.name());

            // Check min and max, check types for target arguments, etc.

            return null;
        }

        @Override
        public Parameter parameter() {
            return parameter;
        }
    }

    static class UnboundParamBinder implements ParameterBinder {
        private final Parameter parameter;
        private final UnboundParam unboundParam;

        public UnboundParamBinder(Parameter parameter, UnboundParam unboundParam) {
            this.parameter = Objects.requireNonNull(parameter, "parameter can not be null");
            this.unboundParam = Objects.requireNonNull(unboundParam, "unboundParam can not be null");
        }

        @Override
        public Type type() {
            return Type.OPERATION;
        }

        @Override
        public String name() {
            return "<unbound>";
        }

        @Override
        public Object bind(IBaseParameters parameters) {
            var value = Resources.clone(parameters);

            // Remove all values from the parameters resource

            return value;
        }

        @Override
        public Parameter parameter() {
            return this.parameter;
        }
    }
}
