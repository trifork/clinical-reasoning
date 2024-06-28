package org.opencds.cqf.fhir.utility.adapter.r5;

import static java.util.Optional.ofNullable;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Resource;

class ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.ResourceAdapter {

    public ResourceAdapter(IBaseResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource can not be null");
        }

        if (!resource.getStructureFhirVersionEnum().equals(FhirVersionEnum.R5)) {
            throw new IllegalArgumentException("resource is incorrect fhir version for this adapter");
        }

        this.resource = (Resource) resource;
    }

    private Resource resource;

    protected Resource getResource() {
        return this.resource;
    }

    protected boolean isDomainResource() {
        return getDomainResource().isPresent();
    }

    protected Optional<DomainResource> getDomainResource() {
        return ofNullable(resource instanceof DomainResource ? (DomainResource) resource : null);
    }

    public IBaseResource get() {
        return this.resource;
    }

    @Override
    public IBase setProperty(String name, IBase value) throws FHIRException {
        return this.getResource().setProperty(name, (Base) value);
    }

    @Override
    public IBase addChild(String name) throws FHIRException {
        return this.getResource().addChild(name);
    }

    @Override
    public IBase getSingleProperty(String name) throws FHIRException {
        IBase[] values = this.getProperty(name, true);

        if (values == null || values.length == 0) {
            return null;
        }

        if (values.length > 1) {
            throw new IllegalArgumentException(String.format("more than one value found for property: %s", name));
        }

        return values[0];
    }

    @Override
    public IBase[] getProperty(String name) throws FHIRException {
        return this.getProperty(name, true);
    }

    @Override
    public IBase[] getProperty(String name, boolean checkValid) throws FHIRException {
        return this.getResource().getProperty(name.hashCode(), name, checkValid);
    }

    @Override
    public IBase makeProperty(String name) throws FHIRException {
        return this.getResource().makeProperty(name.hashCode(), name);
    }

    @Override
    public String[] getTypesForProperty(String name) throws FHIRException {
        return this.getResource().getTypesForProperty(name.hashCode(), name);
    }

    @Override
    public IBaseResource copy() {
        return this.getResource().copy();
    }

    @Override
    public void copyValues(IBaseResource dst) {
        this.getResource().copyValues((Resource) dst);
    }

    @Override
    public boolean equalsDeep(IBase other) {
        return this.getResource().equalsDeep((Base) other);
    }

    @Override
    public boolean equalsShallow(IBase other) {
        return this.getResource().equalsShallow((Base) other);
    }

    @Override
    public void setExtension(List<IBaseExtension<?, ?>> extensions) {
        getDomainResource()
                .ifPresent(r -> r.setExtension(
                        extensions.stream().map(e -> (Extension) e).collect(Collectors.toList())));
    }

    @Override
    public <T extends IBaseExtension<?, ?>> void addExtension(T extension) {
        getDomainResource().ifPresent(r -> r.addExtension((Extension) extension));
    }

    @Override
    public List<Extension> getExtension() {
        return isDomainResource() ? getDomainResource().get().getExtension() : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Extension getExtensionByUrl(String url) {
        return isDomainResource()
                ? getDomainResource().get().getExtension().stream()
                        .filter(e -> e.getUrl().equals(url))
                        .findFirst()
                        .orElse(null)
                : null;
    }

    @Override
    public List<Extension> getExtensionsByUrl(String url) {
        return isDomainResource()
                ? getDomainResource().get().getExtension().stream()
                        .filter(e -> e.getUrl().equals(url))
                        .collect(Collectors.toList())
                : new ArrayList<>();
    }

    @Override
    public List<Resource> getContained() {
        return isDomainResource() ? getDomainResource().get().getContained() : new ArrayList<>();
    }
}
