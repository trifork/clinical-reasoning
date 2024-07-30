package org.opencds.cqf.fhir.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hl7.fhir.instance.model.api.IBase;

/**
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface OperationParam {

    /**
     * Value for {@link OperationParam#max()} indicating no maximum
     */
    int MAX_UNLIMITED = -1;

    /**
     * Value for {@link OperationParam#max()} indicating that the maximum will be inferred
     * from the type. If the type is a single parameter type (e.g. <code>StringDt</code>,
     * <code>TokenParam</code>, <code>IBaseResource</code>) the maximum will be
     * <code>1</code>.
     * <p>
     * If the type is a collection, e.g.
     * <code>List&lt;StringDt&gt;</code> or <code>List&lt;TokenOrListParam&gt;</code>
     * the maximum will be set to <code>*</code>. If the param is a search parameter
     * "and" type, such as <code>TokenAndListParam</code> the maximum will also be
     * set to <code>*</code>
     * </p>
     *
     * @since 1.5
     */
    int MAX_DEFAULT = -2;

    /**
     * The name of the parameter
     */
    String name() default "";

    /**
     * The type of the parameter. This will only have effect on <code>@OperationParam</code>
     * annotations specified as values for {@link Operation#returnParameters()}, otherwise the
     * value will be ignored. Value should be one of:
     * <ul>
     * <li>A resource type, e.g. <code>Patient.class</code></li>
     * <li>A datatype, e.g. <code>{@link StringDt}.class</code> or </code>CodeableConceptDt.class</code>
     * <li>A RESTful search parameter type, e.g. <code>{@link StringParam}.class</code>
     * </ul>
     */
    Class<? extends IBase> type() default IBase.class;

    /**
     * Optionally specifies the type of the parameter as a string, such as <code>Coding</code> or
     * <code>base64Binary</code>. This can be useful if you want to use a generic interface type
     * on the actual method,such as {@link org.hl7.fhir.instance.model.api.IPrimitiveType} or
     * {@link @org.hl7.fhir.instance.model.api.ICompositeType}.
     */
    String typeName() default "";

    /**
     * The minimum number of repetitions allowed for this child (default is 0)
     */
    int min() default 0;

    /**
     * The maximum number of repetitions allowed for this child. Should be
     * set to {@link #MAX_UNLIMITED} if there is no limit to the number of
     * repetitions. See {@link #MAX_DEFAULT} for a description of the default
     * behaviour.
     */
    int max() default MAX_DEFAULT;
}
