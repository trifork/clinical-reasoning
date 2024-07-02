package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class StructureDefinitionAdapterTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(StructureDefinitionAdapter.class), any(), any());
        IDomainResource structureDef = new StructureDefinition();
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(structureDef, adapter.get());
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(StructureDefinitionAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var structureDef = new StructureDefinition();
        var name = "name";
        structureDef.setName(name);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, structureDef.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var structureDef = new StructureDefinition();
        var url = "www.url.com";
        structureDef.setUrl(url);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertTrue(adapter.hasUrl());
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertTrue(adapter.hasUrl());
        assertEquals(newUrl, structureDef.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var structureDef = new StructureDefinition();
        var version = "1.0.0";
        structureDef.setVersion(version);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, structureDef.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {}

    @Test
    void adapter_get_and_set_dates() {}

    @Test
    void adapter_get_and_set_related() {}

    @Test
    void adapter_get_experimental() {
        var structureDef = new StructureDefinition();
        var experimental = true;
        structureDef.setExperimental(experimental);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_copy() {}

    @Test
    void adapter_get_all_dependencies() {}
}
