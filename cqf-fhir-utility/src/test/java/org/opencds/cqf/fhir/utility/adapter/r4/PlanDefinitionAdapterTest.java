package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class PlanDefinitionAdapterTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new PlanDefinitionAdapter(new Library()));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(PlanDefinitionAdapter.class), any(), any());
        IDomainResource planDef = new PlanDefinition();
        var adapter = new PlanDefinitionAdapter(planDef);
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(PlanDefinitionAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var planDef = new PlanDefinition();
        var name = "name";
        planDef.setName(name);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, planDef.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var planDef = new PlanDefinition();
        var url = "www.url.com";
        planDef.setUrl(url);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, planDef.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var planDef = new PlanDefinition();
        var version = "1.0.0";
        planDef.setVersion(version);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, planDef.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var planDef = new PlanDefinition();
        var status = PublicationStatus.DRAFT;
        planDef.setStatus(status);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var planDef = new PlanDefinition();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        planDef.setDate(date);
        planDef.setApprovalDate(approvalDate);
        planDef.setEffectivePeriod(effectivePeriod);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, planDef.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, planDef.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var planDef = new PlanDefinition();
        var experimental = true;
        planDef.setExperimental(experimental);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var planDef = new PlanDefinition();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = new PlanDefinitionAdapter(planDef);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, planDef.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var planDef = new PlanDefinition().setStatus(PublicationStatus.DRAFT);
        planDef.setId("plan-1");
        var adapter = new PlanDefinitionAdapter(planDef);
        var copy = adapter.copy();
        var adapterCopy = new PlanDefinitionAdapter(copy);
        adapterCopy.setId(new IdDt("PlanDefinition", "plan-2"));
        assertNotEquals(planDef.getId(), copy.getId());
        planDef.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "relatedArtifactRef",
                "libraryRef",
                "actionTriggerDataReqProfile",
                "actionTriggerDataReqCodeFilterValueSet",
                "actionInputProfile",
                "actionInputCodeFilterValueSet",
                "actionOutputProfile",
                "actionOutputCodeFilterValueSet",
                "actionDefinitionRef",
                "actionConditionExpressionReference",
                "actionDynamicValueExpressionRef",
                "cpgPartOfExtRef",
                "nestedActionDefinitionRef");
        var planDef = new PlanDefinition();
        planDef.getRelatedArtifactFirstRep().setResource(dependencies.get(0));
        planDef.getLibrary().add(new CanonicalType(dependencies.get(1)));
        var action = planDef.getActionFirstRep();
        action.getTriggerFirstRep()
                .getDataFirstRep()
                .setProfile(List.of(new CanonicalType(dependencies.get(2))))
                .getCodeFilterFirstRep()
                .setValueSet(dependencies.get(3));
        action.getInputFirstRep()
                .setProfile(List.of(new CanonicalType(dependencies.get(4))))
                .getCodeFilterFirstRep()
                .setValueSet(dependencies.get(5));
        action.getOutputFirstRep()
                .setProfile(List.of(new CanonicalType(dependencies.get(6))))
                .getCodeFilterFirstRep()
                .setValueSet(dependencies.get(7));
        action.setDefinition(new CanonicalType(dependencies.get(8)));
        action.getConditionFirstRep().getExpression().setReference(dependencies.get(9));
        action.getDynamicValueFirstRep().getExpression().setReference(dependencies.get(10));
        planDef.addExtension(new Extension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-partOf", new CanonicalType(dependencies.get(11))));
        action.addAction().setDefinition(new CanonicalType(dependencies.get(12)));
        var adapter = new PlanDefinitionAdapter(planDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }
}
