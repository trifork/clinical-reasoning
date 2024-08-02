package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeArtifactReleaseVisitor implements KnowledgeArtifactVisitor {
    private Logger log = LoggerFactory.getLogger(KnowledgeArtifactReleaseVisitor.class);

    @SuppressWarnings("unchecked")
    @Override
    public IBase visit(LibraryAdapter rootLibraryAdapter, Repository repository, IBaseParameters operationParameters) {
        // boolean latestFromTxServer = operationParameters.getParameterBool("latestFromTxServer");
        Optional<Boolean> latestFromTxServer = VisitorHelper.getParameter(
                        "latestFromTxServer", operationParameters, IPrimitiveType.class)
                .map(t -> (Boolean) t.getValue());
        // TODO: This check is to avoid partial releases and should be removed once the argument is supported.
        if (latestFromTxServer.isPresent()) {
            throw new NotImplementedOperationException("Support for 'latestFromTxServer' is not yet implemented.");
        }
        var version = VisitorHelper.getParameter("version", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue())
                .orElseThrow(() -> new UnprocessableEntityException("Version must be present"));
        var releaseLabel = VisitorHelper.getParameter("releaseLabel", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue())
                .orElse("");
        var versionBehavior = VisitorHelper.getParameter("versionBehavior", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
        var requireNonExpermimental = VisitorHelper.getParameter(
                        "requireNonExperimental", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
        checkReleaseVersion(version, versionBehavior);
        checkReleasePreconditions(rootLibraryAdapter, rootLibraryAdapter.getApprovalDate());
        var rootLibrary = rootLibraryAdapter.get();
        updateReleaseLabel(rootLibrary, releaseLabel);
        var fhirVersion = rootLibrary.getStructureFhirVersionEnum();

        // Determine which version should be used.
        var existingVersion = rootLibraryAdapter.hasVersion()
                ? rootLibraryAdapter.getVersion().replace("-draft", "")
                : null;
        var releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion, fhirVersion)
                .orElseThrow(
                        () -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));
        var rootEffectivePeriod = rootLibraryAdapter.getEffectivePeriod();
        // if the root artifact is experimental then we don't need to check for experimental children
        if (rootLibraryAdapter.getExperimental()) {
            requireNonExpermimental = Optional.of("none");
        }
        var releasedResources = internalRelease(
                rootLibraryAdapter,
                releaseVersion,
                rootEffectivePeriod,
                latestFromTxServer.orElse(false),
                requireNonExpermimental,
                repository,
                new Date());
        var rootArtifactOriginalDependencies = new ArrayList<IDependencyInfo>(rootLibraryAdapter.getDependencies());
        // Get list of extensions which need to be preserved
        var originalDependenciesWithExtensions = rootArtifactOriginalDependencies.stream()
                .filter(dep -> dep.getExtension() != null && dep.getExtension().size() > 0)
                .collect(Collectors.toList());
        // once iteration is complete, delete all depends-on RAs in the root artifact
        rootLibraryAdapter.getRelatedArtifact().removeIf(ra -> KnowledgeArtifactAdapter.getRelatedArtifactType(ra)
                .equalsIgnoreCase("depends-on"));
        var expansionParameters = rootLibraryAdapter.getExpansionParameters();
        var systemVersionParams = expansionParameters
                .map(p -> VisitorHelper.getListParameter("system-version", p, IPrimitiveType.class)
                        .orElse(null))
                .map(versions ->
                        versions.stream().map(v -> (String) v.getValue()).collect(Collectors.toList()))
                .orElse(new ArrayList<String>());
        var canonicalVersionParams = expansionParameters
                .map(p -> VisitorHelper.getListParameter("canonical-version", p, IPrimitiveType.class)
                        .orElse(null))
                .map(versions ->
                        versions.stream().map(v -> (String) v.getValue()).collect(Collectors.toList()))
                .orElse(new ArrayList<String>());

        // Report all dependencies, resolving unversioned dependencies to the latest known version, recursively
        gatherDependencies(
                rootLibraryAdapter,
                rootLibraryAdapter,
                releasedResources,
                fhirVersion,
                repository,
                new HashMap<String, IDomainResource>(),
                systemVersionParams,
                canonicalVersionParams);
        rootLibraryAdapter.setExpansionParameters(systemVersionParams, canonicalVersionParams);
        // removed duplicates and add
        var relatedArtifacts = rootLibraryAdapter.getRelatedArtifact();
        var distinctResolvedRelatedArtifacts = new ArrayList<>(relatedArtifacts);
        distinctResolvedRelatedArtifacts.clear();
        for (var resolvedRelatedArtifact : relatedArtifacts) {
            var relatedArtifactReference =
                    KnowledgeArtifactAdapter.getRelatedArtifactReference(resolvedRelatedArtifact);
            boolean isDistinct = !distinctResolvedRelatedArtifacts.stream().anyMatch(distinctRelatedArtifact -> {
                boolean referenceNotInArray = relatedArtifactReference.equals(
                        KnowledgeArtifactAdapter.getRelatedArtifactReference(distinctRelatedArtifact));
                boolean typeMatches = KnowledgeArtifactAdapter.getRelatedArtifactType(distinctRelatedArtifact)
                        .equals(KnowledgeArtifactAdapter.getRelatedArtifactType(resolvedRelatedArtifact));
                return referenceNotInArray && typeMatches;
            });
            if (isDistinct) {
                distinctResolvedRelatedArtifacts.add(resolvedRelatedArtifact);
                // preserve Extensions if found
                originalDependenciesWithExtensions.stream()
                        .filter(originalDep -> Canonicals.getUrl(originalDep.getReference())
                                        .equals(Canonicals.getUrl(relatedArtifactReference))
                                && KnowledgeArtifactAdapter.getRelatedArtifactType(resolvedRelatedArtifact)
                                        .equalsIgnoreCase("depends-on"))
                        .findFirst()
                        .ifPresent(dep -> {
                            ((List<IBaseExtension<?, ?>>) resolvedRelatedArtifact.getExtension())
                                    .addAll((List<IBaseExtension<?, ?>>) dep.getExtension());
                            originalDependenciesWithExtensions.removeIf(
                                    ra -> ra.getReference().equals(relatedArtifactReference));
                        });
            }
        }

        // Add all updated resources to a transaction bundle for the result
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");
        for (var artifact : releasedResources) {
            var entry = PackageHelper.createEntry(artifact, true);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        // update ArtifactComments referencing the old Canonical Reference
        findArtifactCommentsToUpdate(rootLibrary, releaseVersion, repository).forEach(entry -> {
            BundleHelper.addEntry(transactionBundle, entry);
        });
        rootLibraryAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);

        // return transactionBundle;
        return repository.transaction(transactionBundle);
    }

    private static void updateMetadata(
            KnowledgeArtifactAdapter artifactAdapter,
            String version,
            ICompositeType rootEffectivePeriod,
            Date current) {
        artifactAdapter.setDate(current == null ? new Date() : current);
        artifactAdapter.setStatus("active");
        artifactAdapter.setVersion(version);
        propagageEffectivePeriod(rootEffectivePeriod, artifactAdapter);
    }

    private List<IDomainResource> internalRelease(
            KnowledgeArtifactAdapter artifactAdapter,
            String version,
            ICompositeType rootEffectivePeriod,
            boolean latestFromTxServer,
            Optional<String> experimentalBehavior,
            Repository repository,
            Date current)
            throws NotImplementedOperationException, ResourceNotFoundException {
        var resourcesToUpdate = new ArrayList<IDomainResource>();
        // Step 1: Update the Date, version and propagate effectivePeriod if it doesn't exist
        updateMetadata(artifactAdapter, version, rootEffectivePeriod, current);
        // Step 2: add the resource to the list of released resources
        resourcesToUpdate.add(artifactAdapter.get());
        // Step 3 : Go through all the components, update them and add them to root dependencies
        for (var component : artifactAdapter.getComponents()) {
            final var preReleaseReference = KnowledgeArtifactAdapter.getRelatedArtifactReference(component);
            if (!StringUtils.isBlank(preReleaseReference)) {
                // For composed-of references, if a version is NOT specified in the reference
                // then the latest version of the referenced artifact should be used.

                //  If a version IS specified then `tryGetLatestVersion`
                //  will return that version.
                var alreadyUpdated = checkIfReferenceInList(preReleaseReference, resourcesToUpdate);
                if (KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component) && !alreadyUpdated.isPresent()) {
                    // get the latest version regardless of status because it's owned and we're releasing it
                    var latest = tryGetLatestVersion(preReleaseReference, repository);
                    if (latest.isPresent()) {
                        checkNonExperimental(latest.get().get(), experimentalBehavior, repository);
                                        // release components recursively
                                        resourcesToUpdate.addAll(internalRelease(
                                                latest.get(),
                                                version,
                                                rootEffectivePeriod,
                                                latestFromTxServer,
                                                experimentalBehavior,
                                                repository,
                                                current));
                    } else {
                        // if missing throw because it's an owned resource
                        throw new ResourceNotFoundException(String.format(
                            "Resource with URL '%s' is Owned by this repository and referenced by resource '%s', but no active version was found on the server.",
                            preReleaseReference, artifactAdapter.getUrl()));
                    }
                } else if (!alreadyUpdated.isPresent()) {
                    // if it's a not-owned component just try to get the latest active version
                    tryGetLatestVersionWithStatus(preReleaseReference, repository, "active")
                            .ifPresent(latestActive ->
                                    // check if it's experimental
                                    checkNonExperimental(latestActive.get(), experimentalBehavior, repository));
                }
            }
        }
        return resourcesToUpdate;
    }

    private void gatherDependencies(
            LibraryAdapter rootLibraryAdapter,
            KnowledgeArtifactAdapter artifactAdapter,
            List<IDomainResource> releasedResources,
            FhirVersionEnum fhirVersion,
            Repository repository,
            Map<String, IDomainResource> alreadyUpdatedDependencies,
            List<String> systemVersionExpansionParameters,
            List<String> canonicalVersionExpansionParameters) {

        for (var component : artifactAdapter.getComponents()) {
            // all components are already updated to latest as part of release
            var preReleaseReference = KnowledgeArtifactAdapter.getRelatedArtifactReference(component);
            var updatedReference = preReleaseReference;
            Optional<KnowledgeArtifactAdapter> res = Optional.empty();
            if (KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component)) {
                res = checkIfReferenceInList(preReleaseReference, releasedResources);
                if (!res.isPresent()) {
                    // should never happen since we check all references as part of `internalRelease`
                    throw new InternalErrorException(
                            "Owned resource reference not found during release: " + preReleaseReference);
                }
            } else {
                res = tryGetLatestVersion(preReleaseReference, repository);
            }
            if (res.isPresent()) {
                if (!alreadyUpdatedDependencies.containsKey(Canonicals.getUrl(preReleaseReference))) {
                    alreadyUpdatedDependencies.put(
                            Canonicals.getUrl(preReleaseReference), res.get().get());
                }
                // update the reference to latest
                if (!res.get().hasVersion()) {
                    var s = res.get().getUrl();
                }
                updatedReference = res.get().hasVersion()
                        ? String.format("%s|%s", res.get().getUrl(), res.get().getVersion())
                        : res.get().getUrl();
                KnowledgeArtifactAdapter.setRelatedArtifactReference(
                        component, updatedReference, res.get().getDescriptor());
            }
            var componentToDependency = KnowledgeArtifactAdapter.newRelatedArtifact(
                    fhirVersion,
                    "depends-on",
                    updatedReference,
                    res.map(a -> a.getDescriptor()).orElse(null));
            rootLibraryAdapter.getRelatedArtifact().add(componentToDependency);
        }
        var dependencies = artifactAdapter.getDependencies();
        for (var dependency : dependencies) {
            KnowledgeArtifactAdapter dependencyAdapter = null;
            if (alreadyUpdatedDependencies.containsKey(Canonicals.getUrl(dependency.getReference()))) {
                dependencyAdapter = AdapterFactory.forFhirVersion(fhirVersion)
                        .createKnowledgeArtifactAdapter(
                                alreadyUpdatedDependencies.get(Canonicals.getUrl(dependency.getReference())));
                String versionedReference = addVersionToReference(dependency.getReference(), dependencyAdapter);
                dependency.setReference(versionedReference);

                // the dependency is already updated we just need to recurse into it
                gatherDependencies(
                        rootLibraryAdapter,
                        dependencyAdapter,
                        releasedResources,
                        fhirVersion,
                        repository,
                        alreadyUpdatedDependencies,
                        systemVersionExpansionParameters,
                        canonicalVersionExpansionParameters);
            } else {
                // try to get versions from expansion parameters if they are available
                var resourceType = Canonicals.getResourceType(dependency.getReference());
                if (StringUtils.isBlank(Canonicals.getVersion(dependency.getReference()))) {
                    // TODO: update when we support requireVersionedDependencies
                    Optional<String> expansionParametersVersion = Optional.empty();
                    if (resourceType.equals("CodeSystem")) {
                        expansionParametersVersion = systemVersionExpansionParameters.stream()
                                .filter(canonical -> !StringUtils.isBlank(Canonicals.getUrl(canonical)))
                                .filter(canonical ->
                                        Canonicals.getUrl(canonical).equals(dependency.getReference()))
                                .findAny();
                    } else if (resourceType.equals("ValueSet")) {
                        expansionParametersVersion = canonicalVersionExpansionParameters.stream()
                                .filter(canonical ->
                                        Canonicals.getUrl(canonical).equals(dependency.getReference()))
                                .findAny();
                    }
                    expansionParametersVersion
                            .map(canonical -> Canonicals.getVersion(canonical))
                            .ifPresent(version -> dependency.setReference(dependency.getReference() + "|" + version));
                }
                Optional<KnowledgeArtifactAdapter> maybeAdapter = Optional.empty();
                // if not then try to find the latest version and update the dependency
                if (StringUtils.isBlank(Canonicals.getVersion(dependency.getReference()))) {
                    maybeAdapter = tryGetLatestVersionWithStatus(dependency.getReference(), repository, "active")
                            .map(adapter -> {
                                String versionedReference = addVersionToReference(dependency.getReference(), adapter);
                                dependency.setReference(versionedReference);
                                if (resourceType.equals("CodeSystem")) {
                                    systemVersionExpansionParameters.add(versionedReference);
                                } else if (resourceType.equals("ValueSet")) {
                                    canonicalVersionExpansionParameters.add(versionedReference);
                                }
                                alreadyUpdatedDependencies.put(
                                        Canonicals.getUrl(dependency.getReference()), adapter.get());
                                return adapter;
                            });
                } else {
                    // This is a versioned reference, just get the dependency
                    maybeAdapter = Optional.ofNullable(getArtifactByCanonical(dependency.getReference(), repository));
                }
                // if the dependency is resolvable then recurse into it
                if (maybeAdapter.isPresent()) {
                    dependencyAdapter = maybeAdapter.get();
                    gatherDependencies(
                            rootLibraryAdapter,
                            dependencyAdapter,
                            releasedResources,
                            fhirVersion,
                            repository,
                            alreadyUpdatedDependencies,
                            systemVersionExpansionParameters,
                            canonicalVersionExpansionParameters);
                }
            }
            // only add the dependency to the manifest if it is from a leaf artifact
            if (!artifactAdapter.getUrl().equals(rootLibraryAdapter.getUrl())) {
                if (Canonicals.getVersion(dependency.getReference()) == null) {
                    var d = dependency;
                }
                var newDep = KnowledgeArtifactAdapter.newRelatedArtifact(
                        fhirVersion,
                        "depends-on",
                        dependency.getReference(),
                        dependencyAdapter != null ? dependencyAdapter.getDescriptor() : null);
                rootLibraryAdapter.getRelatedArtifact().add(newDep);
            }
        }
    }

    private void checkNonExperimental(
            IDomainResource resource, Optional<String> experimentalBehavior, Repository repository)
            throws UnprocessableEntityException {
        if (resource instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.utility.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.utility.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.dstu3.model.MetadataResource) resource, code, repository, log);
        } else if (resource instanceof org.hl7.fhir.r4.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.utility.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.utility.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.r4.model.MetadataResource) resource, code, repository, log);
        } else if (resource instanceof org.hl7.fhir.r5.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.utility.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.utility.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.r5.model.MetadataResource) resource, code, repository, log);
        } else {
            throw new UnprocessableEntityException(resource.getClass().getName() + " not supported");
        }
    }

    private static void propagageEffectivePeriod(
            ICompositeType rootEffectivePeriod, KnowledgeArtifactAdapter artifactAdapter) {
        if (rootEffectivePeriod instanceof org.hl7.fhir.dstu3.model.Period) {
            org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.propagageEffectivePeriod(
                    (org.hl7.fhir.dstu3.model.Period) rootEffectivePeriod, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r4.model.Period) {
            org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.propagageEffectivePeriod(
                    (org.hl7.fhir.r4.model.Period) rootEffectivePeriod, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r5.model.Period) {
            org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.propagageEffectivePeriod(
                    (org.hl7.fhir.r5.model.Period) rootEffectivePeriod, artifactAdapter);
        } else {
            throw new UnprocessableEntityException(
                    rootEffectivePeriod.getClass().getName() + " not supported");
        }
    }

    private KnowledgeArtifactAdapter getArtifactByCanonical(String inputReference, Repository repository) {
        List<KnowledgeArtifactAdapter> matchingResources = VisitorHelper.getMetadataResourcesFromBundle(
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, inputReference))
                .stream()
                .map(r -> AdapterFactory.forFhirVersion(r.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(r))
                .collect(Collectors.toList());
        if (matchingResources.isEmpty()) {
            return null;
        } else if (matchingResources.size() == 1) {
            return matchingResources.get(0);
        } else {
            // TODO: Log that multiple resources matched by url and version...
            return matchingResources.get(0);
        }
    }

    private Optional<KnowledgeArtifactAdapter> tryGetLatestVersionWithStatus(
            String inputReference, Repository repository, String status) {
        return KnowledgeArtifactAdapter.findLatestVersion(SearchHelper.searchRepositoryByCanonicalWithPagingWithParams(
                        repository, inputReference, Searches.byStatus(status)))
                .map(res -> AdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }

    private Optional<KnowledgeArtifactAdapter> tryGetLatestVersion(String inputReference, Repository repository) {
        return KnowledgeArtifactAdapter.findLatestVersion(
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, inputReference))
                .map(res -> AdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }

    private String addVersionToReference(String inputReference, KnowledgeArtifactAdapter adapter) {
        if (adapter != null) {
            String versionedReference = adapter.hasVersion()
                    ? String.format("%s|%s", adapter.getUrl(), adapter.getVersion())
                    : adapter.getUrl();
            return versionedReference;
        }

        return inputReference;
    }

    private Optional<String> getReleaseVersion(
            String version, Optional<String> versionBehavior, String existingVersion, FhirVersionEnum fhirVersion)
            throws UnprocessableEntityException {
        switch (fhirVersion) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case R4:
                return org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case R5:
                return org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    private void updateReleaseLabel(IBaseResource artifact, String releaseLabel) throws IllegalArgumentException {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseLabel);
        } else {
            throw new UnprocessableEntityException(artifact.getClass().getName() + " not supported");
        }
    }

    private Optional<KnowledgeArtifactAdapter> checkIfReferenceInList(
            String referenceToCheck, List<IDomainResource> resourceList) {
        for (var resource : resourceList) {
            String referenceURL = Canonicals.getUrl(referenceToCheck);
            String currentResourceURL = AdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                    .createKnowledgeArtifactAdapter(resource)
                    .getUrl();
            if (referenceURL.equals(currentResourceURL)) {
                return Optional.of(resource).map(res -> AdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
            }
        }
        return Optional.empty();
    }

    private void checkReleasePreconditions(KnowledgeArtifactAdapter artifact, Date approvalDate)
            throws PreconditionFailedException {
        if (artifact == null) {
            throw new ResourceNotFoundException("Resource not found.");
        }

        if (!artifact.getStatus().equals("draft")) {
            throw new PreconditionFailedException(String.format(
                    "Resource with ID: '%s' does not have a status of 'draft'.",
                    artifact.get().getIdElement().getIdPart()));
        }
        if (approvalDate == null) {
            throw new PreconditionFailedException(String.format(
                    "The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
        }
        if (approvalDate.before(artifact.getDate())) {
            throw new PreconditionFailedException(String.format(
                    "The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update.",
                    approvalDate, artifact.getDate()));
        }
    }

    private List<IBaseBackboneElement> findArtifactCommentsToUpdate(
            IBaseResource artifact, String releaseVersion, Repository repository) {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            return org
                    .opencds
                    .cqf
                    .fhir
                    .utility
                    .visitor
                    .dstu3
                    .KnowledgeArtifactReleaseVisitor
                    .findArtifactCommentsToUpdate(
                            (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else {
            throw new UnprocessableEntityException("Version not supported");
        }
    }

    private void checkReleaseVersion(String version, Optional<String> versionBehavior)
            throws UnprocessableEntityException {
        if (!versionBehavior.isPresent()) {
            throw new UnprocessableEntityException(
                    "'versionBehavior' must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
        }
        checkVersionValidSemver(version);
    }

    private void checkVersionValidSemver(String version) throws UnprocessableEntityException {
        if (version == null || version.isEmpty()) {
            throw new UnprocessableEntityException("The version argument is required");
        }
        if (version.contains("draft")) {
            throw new UnprocessableEntityException("The version cannot contain 'draft'");
        }
        if (version.contains("/") || version.contains("\\") || version.contains("|")) {
            throw new UnprocessableEntityException("The version contains illegal characters");
        }
        var pattern = Pattern.compile("^(\\d+\\.)(\\d+\\.)(\\*|\\d+)$", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(version);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH");
        }
    }

    @Override
    public IBase visit(PlanDefinitionAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(
            KnowledgeArtifactAdapter knowledgeArtifactAdapter,
            Repository repository,
            IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }
}
