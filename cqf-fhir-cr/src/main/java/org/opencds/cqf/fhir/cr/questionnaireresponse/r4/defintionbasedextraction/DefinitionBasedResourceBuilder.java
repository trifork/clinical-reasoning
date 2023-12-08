package org.opencds.cqf.fhir.cr.questionnaireresponse.r4.defintionbasedextraction;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cr.questionnaireresponse.common.ResourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DefinitionBasedResourceBuilder {
    protected static final Logger logger = LoggerFactory.getLogger(DefinitionBasedResourceBuilder.class);
    private static final String PROPERTY_SETTING_ERROR_MESSAGE = "Error encountered attempting to set property (%s) on resource type (%s): %s";
//    ModelResolver modelResolver;
    private IBaseResource baseResource;
    private String resourceType;
    private IBaseResource questionnaireResponseItem;
    private IPrimitiveType<Date> authoredDate;
    private IBaseDatatype id;
    private IBaseMetaType meta;
    private Map<String, IBaseReference> subject;
    private Map<String, IBaseReference> author;
    private List<Property> dateProperties;
    ResourceBuilder resourceBuilder;

    @Nonnull
    DefinitionBasedResourceBuilder baseResource(IBaseResource resource) {
        this.baseResource = resource;
        return this;
    }

    @Nonnull
    DefinitionBasedResourceBuilder resourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    @Nonnull
    DefinitionBasedResourceBuilder questionnaireResponseItem(IBaseResource questionnaireResponseItem) {
        this.questionnaireResponseItem = questionnaireResponseItem;
        return this;
    }

    @Nonnull
    DefinitionBasedResourceBuilder authoredDate(IPrimitiveType<Date> authoredDate) {
        this.authoredDate = authoredDate;
        return this;
    }

    @Nonnull
    DefinitionBasedResourceBuilder id(IBaseDatatype id) {
        this.id = id;
        return this;
    }

    @Nonnull
    DefinitionBasedResourceBuilder meta(IBaseMetaType meta) {
        this.meta = meta;
        return this;
    }

    @Nonnull
    DefinitionBasedResourceBuilder subject(Map<String, IBaseReference> subject) {
        this.subject = subject;
        return this;
    }
    @Nonnull
    DefinitionBasedResourceBuilder author(Map<String, IBaseReference> author) {
        this.author = author;
        return this;
    }

    public IBaseResource getBaseResource() {
        return baseResource;
    }

    public String getResourceType() {
        return resourceType;
    }

    public IBaseResource getQuestionnaireResponseItem() {
        return questionnaireResponseItem;
    }

    public IPrimitiveType<Date> getAuthoredDate() {
        return authoredDate;
    }

    public IBaseDatatype getId() {
        return id;
    }

    public IBaseMetaType getMeta() {
        return meta;
    }

    public Map<String, IBaseReference> getSubject() {
        return subject;
    }

    public Map<String, IBaseReference> getAuthor() {
        return author;
    }

    public List<Property> getDateProperties() {
        return dateProperties;
    }

    @Nonnull
    IBaseResource build()
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return resourceBuilder.makeDefinitionBasedResource(this);
    }
//    @Nonnull
//    DefinitionBasedResourceBuilder dateProperties(List<Property> dateProperties) {
//        this.dateProperties = dateProperties;
//        return this;
//    }

//    @Nonnull
//    public Resource build() {
//        this.baseResource.setId(this.id);
//        this.baseResource.setMeta(this.meta);
//        if (this.subjectProperty != null) {
//            this.baseResource.setProperty(
//                this.subjectProperty.getName(),
//                this.subjectPropertyValue
//            );
//        }
//        if (this.authorProperty != null) {
//            this.baseResource.setProperty(
//                this.authorProperty.getName(),
//                this.authorPropertyValue
//            );
//        }
//        this.setDatePropertiesUsingModelResolver();
//        this.setAnswerValuePropertiesUsingModelResolver();
//        return this.baseResource;
//    }

//    void setAnswerValuePropertiesUsingModelResolver() {
//        questionnaireResponseItem.getItem().forEach(childItem -> {
//            if (childItem.hasDefinition()) {
//                // First element is always the resource type, so it can be ignored
//                final Type answerValue = childItem.getAnswerFirstRep().getValue();
//                setPropertyValueWithModelResolver(getAnswerValuePath(childItem), answerValue);
//            }
//        });
//    }
//
//    private String getAnswerValuePath(QuestionnaireResponseItemComponent childItem) {
//        final String[] definition = childItem.getDefinition().split("#");
//        return definition[1].replace(resourceType + ".", "");
//    }
//
//    void setDatePropertiesUsingModelResolver() {
//        if (dateProperties != null && !dateProperties.isEmpty() && authoredDate != null) {
//            dateProperties.forEach(property -> {
//                try {
//                    final Object datePropertyValue = getDatePropertyValue(property);
//                    setPropertyValueWithModelResolver(property.getName(), datePropertyValue);
//                } catch (Exception ex) {
//                    logger.error(String.format(
//                        PROPERTY_SETTING_ERROR_MESSAGE,
//                        property.getName(), baseResource.fhirType(), ex.getMessage())
//                    );
//                }
//            });
//        }
//    }
//
//    private Object getDatePropertyValue(Property property)
//        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        final BaseRuntimeElementDefinition propertyDef = getPropertyDefinition(property);
//        return propertyDef
//            .getImplementingClass()
//            .getConstructor(Date.class)
//            .newInstance(authoredDate);
//    }
//
//    private BaseRuntimeElementDefinition getPropertyDefinition(Property property) {
//        return repository.fhirContext().getElementDefinition(
//            property.getTypeCode().contains("|")
//                ? property.getTypeCode().split("\\|")[0]
//                : property.getTypeCode());
//    }
//
//    void setPropertyValueWithModelResolver(String propertyName, Object propertyValue) {
//        if (propertyValue != null) {
//            modelResolver.setValue(
//                baseResource,
//                propertyName,
//                propertyValue
//            );
//        }
//    }
}
