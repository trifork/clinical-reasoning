package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class LibraryAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.LibraryAdapter {

    private Library library;

    public LibraryAdapter(IDomainResource library) {
        super(library);

        if (!(library instanceof Library)) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }

        this.library = (Library) library;
    }

    protected Library getLibrary() {
        return this.library;
    }

    @Override
    public Library get() {
        return this.library;
    }

    @Override
    public Library copy() {
        return this.get().copy();
    }

    @Override
    public String getName() {
        return this.getLibrary().getName();
    }

    @Override
    public boolean hasTitle() {
        return this.getLibrary().hasTitle();
    }

    @Override
    public String getTitle() {
        return this.getLibrary().getTitle();
    }

    @Override
    public String getPurpose() {
        return this.getLibrary().getPurpose();
    }

    @Override
    public void setName(String name) {
        this.getLibrary().setName(name);
    }

    @Override
    public void setTitle(String title) {
        this.getLibrary().setTitle(title);
    }

    @Override
    public String getUrl() {
        return this.getLibrary().getUrl();
    }

    @Override
    public boolean hasUrl() {
        return this.getLibrary().hasUrl();
    }

    @Override
    public void setUrl(String url) {
        this.getLibrary().setUrl(url);
    }

    @Override
    public String getVersion() {
        return this.getLibrary().getVersion();
    }

    @Override
    public boolean hasVersion() {
        return this.getLibrary().hasVersion();
    }

    @Override
    public void setVersion(String version) {
        this.getLibrary().setVersion(version);
    }

    @Override
    public boolean hasContent() {
        return this.getLibrary().hasContent();
    }

    @Override
    public List<Attachment> getContent() {
        return this.getLibrary().getContent().stream().collect(Collectors.toList());
    }

    @Override
    public void setContent(List<? extends ICompositeType> attachments) {
        var castAttachments = attachments.stream().map(x -> (Attachment) x).collect(Collectors.toList());
        this.getLibrary().setContent(castAttachments);
    }

    @Override
    public Attachment addContent() {
        return this.getLibrary().addContent();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> retval = new ArrayList<IDependencyInfo>();
        final String referenceSource =
                this.hasVersion() ? this.getUrl() + "|" + this.getLibrary().getVersion() : this.getUrl();
        this.getRelatedArtifact().stream()
                .filter(ra -> ra.hasResource())
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .forEach(dep -> retval.add(dep));
        this.getLibrary().getDataRequirement().stream().forEach(dr -> {
            dr.getProfile().stream()
                    .filter(profile -> profile.hasValue())
                    .forEach(profile -> retval.add(new DependencyInfo(
                            referenceSource,
                            profile.getValue(),
                            profile.getExtension(),
                            (reference) -> profile.setValue(reference))));
            dr.getCodeFilter().stream()
                    .filter(cf -> cf.hasValueSet())
                    .forEach(cf -> retval.add(new DependencyInfo(
                            referenceSource,
                            cf.getValueSet(),
                            cf.getExtension(),
                            (reference) -> cf.setValueSet(reference))));
        });
        return retval;
    }

    @Override
    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    @Override
    public Date getApprovalDate() {
        return this.library.getApprovalDate();
    }

    @Override
    public Date getDate() {
        return this.getLibrary().getDate();
    }

    @Override
    public void setDate(Date date) {
        this.getLibrary().setDate(date);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.getLibrary().setDateElement((DateTimeType) date);
    }

    @Override
    public Period getEffectivePeriod() {
        return this.getLibrary().getEffectivePeriod();
    }

    @Override
    public boolean hasRelatedArtifact() {
        return this.getLibrary().hasRelatedArtifact();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getLibrary().getRelatedArtifact();
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be org.hl7.fhir.r5.model.Period");
        }
        this.getLibrary().setEffectivePeriod((Period) effectivePeriod);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifactsOfType(String codeString) {
        RelatedArtifactType type;
        try {
            type = RelatedArtifactType.fromCode(codeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid related artifact code");
        }
        return this.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        this.getLibrary().setApprovalDate(approvalDate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getComponents() {
        return this.getRelatedArtifactsOfType("composed-of");
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts)
            throws UnprocessableEntityException {
        this.getLibrary()
                .setRelatedArtifact(relatedArtifacts.stream()
                        .map(ra -> {
                            try {
                                return (RelatedArtifact) ra;
                            } catch (ClassCastException e) {
                                throw new UnprocessableEntityException(
                                        "All related artifacts must be of type " + RelatedArtifact.class.getName());
                            }
                        })
                        .collect(Collectors.toList()));
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus status;
        try {
            status = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getLibrary().setStatus(status);
    }

    @Override
    public String getStatus() {
        return this.getLibrary().getStatus() == null
                ? null
                : this.getLibrary().getStatus().toCode();
    }

    @Override
    public boolean getExperimental() {
        return this.getLibrary().getExperimental();
    }

    @Override
    public void setExtension(List<IBaseExtension<?, ?>> extensions) {
        this.get().setExtension(extensions.stream().map(e -> (Extension) e).collect(Collectors.toList()));
    }

    @Override
    public Optional<IBaseParameters> getExpansionParameters() {
        return getLibrary().getExtension().stream()
                .filter(ext -> ext.getUrl().equals(Constants.EXPANSION_PARAMETERS_URL))
                .findAny()
                .map(ext -> ((Reference) ext.getValue()).getReference())
                .map(ref -> {
                    if (getLibrary().hasContained()) {
                        return getLibrary().getContained().stream()
                                .filter(r -> r.getId().equals(ref))
                                .findFirst()
                                .map(r -> (IBaseParameters) r)
                                .orElse(null);
                    }
                    return null;
                });
    }
}
